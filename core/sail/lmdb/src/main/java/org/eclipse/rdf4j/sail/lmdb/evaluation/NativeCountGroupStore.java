/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongUnaryOperator;

import org.eclipse.rdf4j.sail.lmdb.LmdbQueryMemoryManager;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelCancellation;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelRuntime;

/**
 * Bounded, authority-local COUNT / COUNT(DISTINCT id) grouping, also usable as a whole-row DISTINCT store with zero
 * count channels. No IR objects, RDF Values or native addresses are retained in spill records.
 *
 * The key function MUST return an exact stable equivalence representative, not just a hash. In particular equal hash
 * codes are not proof of term equality. The caller retains the authority until this store is closed. -1 denotes an
 * unbound value. An input weight is positive; a zero weight has no effect.
 *
 * A small primitive hash arena coalesces groups and distinct events. At its byte ceiling (or refused growth) it exports
 * sorted partial records to the shared native external sorter. Final merging adds ordinary counts and unions distinct
 * events. It never adds partial distinct cardinalities. A second bounded sort restores first-encounter group order,
 * including the original representative IDs; this preserves existing slice/tie behavior without retaining a map of all
 * groups. A hot single key therefore does not cause repartitioning without progress, and an arbitrarily large DISTINCT
 * set is merged as a stream.
 *
 * Reservations precede arena growth and spill-workspace allocation. The scope covers primitive hash/payload arrays,
 * peak copy/sort workspace and a conservative allowance for I/O/merge state. It does NOT cover runtime Values retained
 * by the authority, other operators or JVM overhead. Every owning caller must close on failure.
 */
public final class NativeCountGroupStore implements AutoCloseable {
	public static final String MAX_BYTES_PROPERTY = "rdf4j.lmdb.nativeGroup.maxBytes";
	public static final String ENABLED_PROPERTY = "rdf4j.lmdb.janinoCodegen.boundedGroups";
	private static final long DEFAULT_MAX_BYTES = 64L << 20;
	private static final int IO_BYTES = 4096;
	private static final long[] NO_LONGS = new long[0];
	private static final int[] NO_INTS = new int[0];
	private final int groups, outputs, keyWidth, recordWidth, originalOffset, ordinalOffset, countsOffset;
	private final boolean[] distinct;
	private final int distinctChannels;
	private final LongUnaryOperator canonicalKey;
	private final KernelCancellation cancellation;
	private final LmdbQueryMemoryManager.QueryLedger ledger;
	private final boolean ownsLedger;
	private final long localBytes;
	private final long[] scratch, readRow, groupRow;
	private LmdbQueryMemoryManager.Reservation arenaClaim, spillClaim;
	private long arenaBytes, spillAllowance, peakClaimed;
	private long[] records = NO_LONGS;
	private int[] slots = NO_INTS;
	private int capacity, size, mask, residentPosition;
	private long rowsSeen, partialRecords;
	private NativeSpillSort partials, ordered;
	private NativeSortedRows result, mergedInput;
	private boolean unorderedOutput, pendingPartial, mergedEof;
	private long currentOrdinal;
	private boolean finished, closed, emittedEmpty;
	private int ticks;

	public NativeCountGroupStore(int groupWidth, boolean[] distinct, LongUnaryOperator canonicalKey,
			KernelCancellation cancellation, LmdbQueryMemoryManager.QueryLedger ledger) {
		this(groupWidth, distinct, canonicalKey, cancellation, ledger, configuredMaxBytes());
	}

	public NativeCountGroupStore(int groupWidth, boolean[] distinct, LongUnaryOperator canonicalKey,
			KernelCancellation cancellation, LmdbQueryMemoryManager.QueryLedger ledger, long maxBytes) {
		if (groupWidth < 0 || groupWidth > 64 || distinct == null || distinct.length > 64 || maxBytes < 4096L) {
			throw new IllegalArgumentException("Invalid grouping dimensions or budget (minimum 4096 bytes)");
		}
		this.groups = groupWidth;
		this.outputs = distinct.length;
		this.distinct = distinct.clone();
		int n = 0;
		for (boolean d : distinct)
			if (d)
				n++;
		this.distinctChannels = n;
		this.canonicalKey = Objects.requireNonNull(canonicalKey, "canonical key");
		this.cancellation = cancellation;
		this.ownsLedger = ledger == null;
		this.ledger = ledger != null ? ledger : LmdbQueryMemoryManager.process().openQuery();
		this.localBytes = maxBytes;
		// [canonical group, tag, distinct term, original group, first ordinal, ordinary counts]
		this.keyWidth = groups + 2;
		this.originalOffset = keyWidth;
		this.ordinalOffset = keyWidth + groups;
		this.countsOffset = ordinalOffset + 1;
		this.recordWidth = countsOffset + outputs;
		this.scratch = new long[recordWidth];
		this.readRow = new long[recordWidth];
		this.groupRow = new long[1 + groups + outputs];
	}

	/**
	 * Final groups may arrive in merge-key order when the consumer applies its own ordering. The consumer must carry
	 * {@link #currentOrdinal()} into that order to preserve stable ties. Call before the first input.
	 */
	public NativeCountGroupStore unorderedOutput() {
		if (rowsSeen != 0L || finished || closed)
			throw new IllegalStateException("Output mode is already in use");
		unorderedOutput = true;
		return this;
	}

	/** First input ordinal of the last successfully returned group, independent of merge/partition order. */
	public long currentOrdinal() {
		return currentOrdinal;
	}

	public static boolean enabled() {
		return !"false".equals(System.getProperty(ENABLED_PROPERTY));
	}

	public static long configuredMaxBytes() {
		String setting = System.getProperty(MAX_BYTES_PROPERTY);
		if (setting == null)
			return DEFAULT_MAX_BYTES;
		try {
			long bytes = Long.parseLong(setting);
			if (bytes < 4096L)
				throw new IllegalArgumentException(MAX_BYTES_PROPERTY + " must be at least 4096");
			return bytes;
		} catch (NumberFormatException failure) {
			throw new IllegalArgumentException("Invalid " + MAX_BYTES_PROPERTY, failure);
		}
	}

	/** The original grouping IDs and count-argument IDs are copied only when a new arena record is needed. */
	public void add(long[] groupIds, long[] values, long weight) {
		if (closed || finished)
			throw new IllegalStateException("Grouping input is closed");
		Objects.requireNonNull(groupIds, "group IDs");
		Objects.requireNonNull(values, "count values");
		if (groupIds.length < groups || values.length < outputs || weight < 0L) {
			throw new IllegalArgumentException("Invalid weighted group input");
		}
		if (weight == 0L)
			return;
		try {
			poll();
			checkCancelled();
			ensureRoom(1 + distinctChannels);
			// Count payload remains zero in the input scratch. Reset only the event tag/term.
			scratch[groups] = 0L;
			scratch[groups + 1] = 0L;
			for (int i = 0; i < groups; i++) {
				long id = groupIds[i];
				scratch[i] = id == -1L ? -1L : canonicalKey.applyAsLong(id);
				scratch[originalOffset + i] = id;
			}
			scratch[ordinalOffset] = rowsSeen;
			rowsSeen = Math.addExact(rowsSeen, 1L);
			int marker = intern();
			int countBase = marker * recordWidth + countsOffset;
			for (int i = 0; i < outputs; i++) {
				long value = values[i];
				if (value == -1L)
					continue;
				if (distinct[i]) {
					scratch[groups] = i + 1L;
					scratch[groups + 1] = canonicalKey.applyAsLong(value);
					int previousSize = size;
					intern();
					if (size != previousSize)
						records[countBase + i] = Math.addExact(records[countBase + i], 1L);
				} else {
					records[countBase + i] = Math.addExact(records[countBase + i], weight);
				}
			}
		} catch (IOException failure) {
			fail(new UncheckedIOException(failure));
		} catch (RuntimeException | Error failure) {
			fail(failure);
		}
	}

	/**
	 * One exact marginal contribution. channel==-1 establishes a group with no count update. Independent projections
	 * must never increment other channels or sum local DISTINCT counts.
	 */
	public void addChannel(long[] groupIds, int channel, long value, long weight) {
		if (groupIds == null || groupIds.length != groups || channel < -1 || channel >= outputs)
			throw new IllegalArgumentException("invalid marginal update");
		if (closed || finished)
			throw new IllegalStateException("Grouping input is closed");
		if (weight < 0L)
			throw new IllegalArgumentException("negative marginal weight");
		if (weight == 0L)
			return;
		try {
			poll();
			checkCancelled();
			ensureRoom(channel >= 0 && distinct[channel] && value != -1L ? 2 : 1);
			scratch[groups] = scratch[groups + 1] = 0L;
			for (int i = 0; i < groups; i++) {
				long id = groupIds[i];
				scratch[i] = id == -1L ? -1L : canonicalKey.applyAsLong(id);
				scratch[originalOffset + i] = id;
			}
			scratch[ordinalOffset] = rowsSeen;
			rowsSeen = Math.addExact(rowsSeen, 1L);
			int marker = intern();
			if (channel < 0 || value == -1L)
				return;
			int cell = marker * recordWidth + countsOffset + channel;
			if (distinct[channel]) {
				scratch[groups] = channel + 1L;
				scratch[groups + 1] = canonicalKey.applyAsLong(value);
				int before = size;
				intern();
				if (size != before)
					records[cell] = Math.addExact(records[cell], 1L);
			} else
				records[cell] = Math.addExact(records[cell], weight);
		} catch (IOException failure) {
			fail(new UncheckedIOException(failure));
		} catch (RuntimeException | Error failure) {
			fail(failure);
		}
	}

	/** Scalar specialization of the same arena for one group column and one ordinary COUNT channel. */
	public void addSingleCount(long groupId, long argument, long weight) {
		if (groups != 1 || outputs != 1 || distinctChannels != 0) {
			throw new IllegalStateException("Not a unary ordinary-count store");
		}
		if (closed || finished)
			throw new IllegalStateException("Grouping input is closed");
		if (weight < 0L)
			throw new IllegalArgumentException("Negative aggregate multiplicity");
		if (weight == 0L)
			return;
		try {
			poll();
			checkCancelled();
			long canonical = groupId == -1L ? -1L : canonicalKey.applyAsLong(groupId);
			if (capacity == 0)
				ensureRoom(1);
			int slot = (int) mix(canonical) & mask;
			for (int entry; (entry = slots[slot]) != 0; slot = (slot + 1) & mask) {
				int base = (entry - 1) * recordWidth;
				if (records[base] == canonical) {
					if (argument != -1L)
						records[base + countsOffset] = Math.addExact(records[base + countsOffset], weight);
					rowsSeen = Math.addExact(rowsSeen, 1L);
					return;
				}
			}
			// Grow/spill only on a miss. A full hot-key arena must not grow just to update a present key.
			ensureRoom(1);
			scratch[0] = canonical;
			scratch[1] = scratch[2] = 0L;
			scratch[originalOffset] = groupId;
			scratch[ordinalOffset] = rowsSeen;
			int marker = intern();
			if (argument != -1L)
				records[marker * recordWidth + countsOffset] = weight;
			rowsSeen = Math.addExact(rowsSeen, 1L);
		} catch (IOException failure) {
			fail(new UncheckedIOException(failure));
		} catch (RuntimeException | Error failure) {
			fail(failure);
		}
	}

	private void ensureRoom(int needed) throws IOException {
		if (capacity - size >= needed)
			return;
		int next = capacity == 0 ? 16 : capacity;
		while (next < size + needed && next <= (Integer.MAX_VALUE - 8) / (2 * recordWidth))
			next *= 2;
		long bytes = arenaSize(next);
		long ceiling = Math.max(arenaSize(Math.max(16, 2 * needed)), localBytes / 3L);
		if (next >= size + needed && bytes <= ceiling && grow(next, bytes))
			return;
		if (capacity == 0)
			throw refused("initial aggregation arena");
		flushArena();
		if (capacity < needed)
			throw refused("one aggregate input exceeds the arena");
	}

	private long arenaSize(int entries) {
		int table = tableSize(entries);
		return Math.addExact(256L + 8L * (scratch.length + readRow.length + groupRow.length),
				Math.addExact(8L * entries * recordWidth, 4L * table));
	}

	private static int tableSize(int entries) {
		if (entries < 1 || entries > 1 << 28)
			throw new IllegalArgumentException("Group arena too large");
		int n = 2;
		while (n < entries * 2)
			n <<= 1;
		return n;
	}

	private boolean grow(int next, long bytes) {
		// Claim the complete replacement before allocation: old and new coexist during the copy.
		if (arenaClaim == null) {
			arenaClaim = ledger.reserve(bytes, null);
			if (arenaClaim == null)
				return false;
		} else if (!arenaClaim.tryGrow(bytes))
			return false;
		long oldBytes = arenaBytes;
		try {
			long[] newRecords = Arrays.copyOf(records, Math.multiplyExact(next, recordWidth));
			int[] newSlots = new int[tableSize(next)];
			int newMask = newSlots.length - 1;
			for (int i = 0; i < size; i++) {
				int slot = hash(newRecords, i * recordWidth) & newMask;
				while (newSlots[slot] != 0)
					slot = (slot + 1) & newMask;
				newSlots[slot] = i + 1;
			}
			records = newRecords;
			slots = newSlots;
			capacity = next;
			mask = newMask;
			peakClaimed = Math.max(peakClaimed, oldBytes + bytes + spillAllowance);
			arenaBytes = bytes;
			if (oldBytes != 0L)
				arenaClaim.release(oldBytes);
			return true;
		} catch (RuntimeException | Error failure) {
			arenaClaim.release(bytes);
			throw failure;
		}
	}

	private static long mix(long x) {
		x ^= x >>> 33;
		x *= 0xff51afd7ed558ccdL;
		x ^= x >>> 33;
		x *= 0xc4ceb9fe1a85ec53L;
		return x ^ (x >>> 33);
	}

	private int hash(long[] row, int offset) {
		// The common unary group key must not pay for hashing two constant marker fields.
		long h;
		if (groups == 1)
			h = row[offset];
		else {
			h = 0x9e3779b97f4a7c15L;
			for (int i = 0; i < groups; i++)
				h = Long.rotateLeft(h ^ mix(row[offset + i]), 27);
		}
		long tag = row[offset + groups];
		if (tag != 0L)
			h ^= mix(row[offset + groups + 1] ^ tag * 0x9e3779b97f4a7c15L);
		return (int) mix(h);
	}

	private boolean sameKey(long[] a, int ao, long[] b, int bo, int width) {
		for (int i = 0; i < width; i++)
			if (a[ao + i] != b[bo + i])
				return false;
		return true;
	}

	/** Returns an arena record index. Space for every event of the input has already been admitted. */
	private int intern() {
		int slot = hash(scratch, 0) & mask;
		while (slots[slot] != 0) {
			int row = slots[slot] - 1;
			if (sameKey(records, row * recordWidth, scratch, 0, keyWidth))
				return row;
			slot = (slot + 1) & mask;
		}
		// A repeated input does not allocate a private row. Apply probe limits to retained partial records,
		// not to all contributing solution mappings; a hot group can summarize arbitrarily many inputs.
		int retained = partialRecords >= Integer.MAX_VALUE - size ? Integer.MAX_VALUE : (int) partialRecords + size;
		KernelRuntime.checkMaterializationCapacity(cancellation, retained);
		int index = size++;
		System.arraycopy(scratch, 0, records, index * recordWidth, recordWidth);
		slots[slot] = index + 1;
		return index;
	}

	private void initializeSpill() {
		if (partials != null)
			return;
		// Both sorters can merge concurrently (input merge -> first-encounter-order sorter).
		// Each receives full rows as its key section: one input stream per reader, at most 16 readers.
		long sorters = unorderedOutput ? 1L : 2L;
		long overhead = sorters * (17L * IO_BYTES + 16L * (8L * recordWidth + 1024L)) + (128L << 10);
		long runBudget = Math.max(4096L, localBytes / 3L);
		for (;;) {
			long claim = Math.addExact(overhead, Math.multiplyExact(sorters, runBudget));
			spillClaim = ledger.reserve(claim, null);
			if (spillClaim != null) {
				spillAllowance = claim;
				peakClaimed = Math.max(peakClaimed, arenaBytes + claim);
				break;
			}
			if (runBudget <= 4096L)
				throw refused("bounded aggregate spill workspace");
			runBudget = Math.max(4096L, runBudget / 2L);
		}
		PackedRowComparator keys = (a, ao, b, bo) -> {
			poll();
			for (int i = 0; i < keyWidth; i++) {
				int comparison = Long.compareUnsigned(a[ao + i], b[bo + i]);
				if (comparison != 0)
					return comparison;
			}
			return 0;
		};
		partials = new NativeSpillSort(recordWidth, recordWidth, keys, -1,
				LmdbNativeAttemptMetrics.direct(), runBudget, this::checkCancelled, IO_BYTES);
		int outputWidth = 1 + groups + outputs;
		if (!unorderedOutput)
			ordered = new NativeSpillSort(outputWidth, outputWidth,
					(a, ao, b, bo) -> {
						poll();
						return Long.compare(a[ao], b[bo]);
					}, -1,
					LmdbNativeAttemptMetrics.direct(), runBudget, this::checkCancelled, IO_BYTES);
	}

	private void flushArena() throws IOException {
		if (size == 0)
			return;
		initializeSpill();
		for (int i = 0; i < size; i++) {
			poll();
			System.arraycopy(records, i * recordWidth, readRow, 0, recordWidth);
			// Partial distinct cardinalities are NOT additive. Export the actual distinct events instead.
			if (readRow[groups] == 0L) {
				for (int j = 0; j < outputs; j++)
					if (distinct[j])
						readRow[countsOffset + j] = 0L;
			}
			partials.add(readRow, readRow[ordinalOffset]);
			partialRecords = Math.addExact(partialRecords, 1L);
		}
		size = 0;
		Arrays.fill(slots, 0);
	}

	public void finish() {
		if (closed)
			throw new IllegalStateException("Grouping store closed");
		if (finished)
			return;
		try {
			checkCancelled();
			if (partials != null) {
				flushArena();
				records = NO_LONGS;
				slots = NO_INTS;
				capacity = size = 0;
				if (arenaClaim != null) {
					arenaClaim.close();
					arenaClaim = null;
					arenaBytes = 0L;
				}
				mergedInput = partials.sortedRows();
				if (!unorderedOutput) {
					while (nextMergedGroup())
						ordered.add(groupRow, groupRow[0]);
					mergedInput.close();
					mergedInput = null;
					partials.close();
					result = ordered.sortedRows();
				}
			}
			finished = true;
		} catch (IOException failure) {
			fail(new UncheckedIOException(failure));
		} catch (RuntimeException | Error failure) {
			fail(failure);
		}
	}

	/** Finalize one group with O(key width + channels) state, even for a huge DISTINCT hot key. */
	private boolean nextMergedGroup() throws IOException {
		if (mergedEof)
			return false;
		if (!pendingPartial && !mergedInput.next(readRow)) {
			mergedEof = true;
			return false;
		}
		pendingPartial = false;
		System.arraycopy(readRow, 0, scratch, 0, groups);
		Arrays.fill(groupRow, 0L);
		groupRow[0] = Long.MAX_VALUE;
		boolean haveEvent = false;
		long eventTag = -1L, eventTerm = 0L;
		for (;;) {
			poll();
			long tag = readRow[groups];
			if (tag == 0L) {
				if (readRow[ordinalOffset] < groupRow[0]) {
					groupRow[0] = readRow[ordinalOffset];
					System.arraycopy(readRow, originalOffset, groupRow, 1, groups);
				}
				for (int j = 0; j < outputs; j++)
					if (!distinct[j]) {
						groupRow[1 + groups + j] = Math.addExact(groupRow[1 + groups + j], readRow[countsOffset + j]);
					}
			} else {
				int channel = Math.toIntExact(tag - 1L);
				if (channel < 0 || channel >= outputs || !distinct[channel])
					throw new IOException("Invalid aggregate distinct channel in spill");
				long term = readRow[groups + 1];
				if (!haveEvent || tag != eventTag || term != eventTerm) {
					groupRow[1 + groups + channel] = Math.addExact(groupRow[1 + groups + channel], 1L);
					eventTag = tag;
					eventTerm = term;
					haveEvent = true;
				}
			}
			if (!mergedInput.next(readRow)) {
				mergedEof = true;
				return true;
			}
			if (!sameKey(scratch, 0, readRow, 0, groups)) {
				pendingPartial = true;
				return true;
			}
		}
	}

	/** Streams [original group IDs, final counts]; first-encounter order unless explicitly relaxed. */
	public boolean next(long[] target) {
		if (target == null || target.length < groups + outputs)
			throw new IllegalArgumentException("Short group row");
		if (closed)
			return false;
		if (!finished)
			throw new IllegalStateException("Grouping input has not finished");
		try {
			poll();
			if (rowsSeen == 0L && groups == 0 && outputs > 0 && !emittedEmpty) {
				emittedEmpty = true;
				currentOrdinal = 0L;
				Arrays.fill(target, 0, outputs, 0L);
				return true;
			}
			if (mergedInput != null) {
				if (!nextMergedGroup()) {
					close();
					return false;
				}
				currentOrdinal = groupRow[0];
				System.arraycopy(groupRow, 1, target, 0, groups + outputs);
				return true;
			}
			if (result != null) {
				if (!result.next(groupRow)) {
					close();
					return false;
				}
				currentOrdinal = groupRow[0];
				System.arraycopy(groupRow, 1, target, 0, groups + outputs);
				return true;
			}
			while (residentPosition < size) {
				int start = residentPosition++ * recordWidth;
				if (records[start + groups] != 0L)
					continue;
				currentOrdinal = records[start + ordinalOffset];
				System.arraycopy(records, start + originalOffset, target, 0, groups);
				System.arraycopy(records, start + countsOffset, target, groups, outputs);
				return true;
			}
			close();
			return false;
		} catch (IOException failure) {
			fail(new UncheckedIOException(failure));
			return false;
		} catch (RuntimeException | Error failure) {
			fail(failure);
			return false;
		}
	}

	private void checkCancelled() {
		KernelRuntime.checkCancelled(cancellation);
	}

	private void poll() {
		if ((++ticks & 1023) == 0)
			checkCancelled();
	}

	private IllegalStateException refused(String what) {
		return new IllegalStateException("Query memory reservation refused for " + what);
	}

	private void fail(Throwable failure) {
		KernelRuntime.closeResource(this, failure);
		KernelRuntime.rethrowCloseFailure(failure);
	}

	public long inputRows() {
		return rowsSeen;
	}

	public long spilledRecords() {
		return partialRecords;
	}

	public long peakReservedBytes() {
		return peakClaimed;
	}

	public int peakRunPaths() {
		return partials == null ? 0 : Math.max(partials.peakRunCount, ordered == null ? 0 : ordered.peakRunCount);
	}

	@Override
	public void close() {
		if (closed)
			return;
		closed = true;
		Throwable failure = KernelRuntime.closeResource(result, null);
		result = null;
		failure = KernelRuntime.closeResource(mergedInput, failure);
		mergedInput = null;
		failure = KernelRuntime.closeResource(partials, failure);
		failure = KernelRuntime.closeResource(ordered, failure);
		records = NO_LONGS;
		slots = NO_INTS;
		failure = KernelRuntime.closeResource(arenaClaim, failure);
		arenaClaim = null;
		failure = KernelRuntime.closeResource(spillClaim, failure);
		spillClaim = null;
		if (ownsLedger)
			failure = KernelRuntime.closeResource(ledger, failure);
		KernelRuntime.rethrowCloseFailure(failure);
	}
}
