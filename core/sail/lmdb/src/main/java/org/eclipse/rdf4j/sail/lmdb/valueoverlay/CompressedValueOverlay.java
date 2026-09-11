/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Immutable, memory-only overlay over exact ValueStore records. Owns no RDF Values and mints no dictionary IDs. A
 * sorted streaming builder creates type/affinity pages and compressed ID/locator vectors. Reads require a lease.
 * Missing entries, including reverse-index misses, are UNKNOWN, never proof of absence in the authoritative store.
 */
public final class CompressedValueOverlay implements AutoCloseable {
	public static final int PAGE_ENTRIES = 256;
	private static final int PAGE_SHIFT = 8, PAGE_MASK = 255;

	public record Options(long maxNativeBytes, int reverseSlots, int maxActivePages, int maxPageBytes,
			int maxRecordBytes, boolean adaptiveTokens, boolean vectorCompression) {
		public Options {
			if (maxNativeBytes < 0 || reverseSlots < 0 || reverseSlots > (1 << 28)
					|| reverseSlots != 0 && (reverseSlots & (reverseSlots - 1)) != 0
					|| maxActivePages < 1 || maxActivePages > 128 || maxPageBytes < 1024
					|| maxPageBytes > (1 << 20) || maxRecordBytes < 1 || maxRecordBytes > (64 << 20)) {
				throw new IllegalArgumentException("invalid overlay options");
			}
		}

		public static Options defaults(long budget) {
			return new Options(budget, 1 << 20, 32, 64 << 10, 1 << 20, true, true);
		}
	}

	public record Stats(long records, long skippedLargeRecords, long inputRecordBytes, long pageBytes,
			long indexBytes, long nativeUsedBytes, long nativeReservedBytes, long pages, long rawPages,
			long frontPages, long tokenPages, long vectorPages, long reverseIndexedRecords,
			long reverseCapacity, long peakStagedBytes, long tokenTrials, long tokenTrialsSkipped,
			int coalescedFamilies, long prefixPages) {
		public Stats(long records, long skippedLargeRecords, long inputRecordBytes, long pageBytes,
				long indexBytes, long nativeUsedBytes, long nativeReservedBytes, long pages, long rawPages,
				long frontPages, long tokenPages, long vectorPages, long reverseIndexedRecords,
				long reverseCapacity, long peakStagedBytes, long tokenTrials, long tokenTrialsSkipped,
				int coalescedFamilies) {
			this(records, skippedLargeRecords, inputRecordBytes, pageBytes, indexBytes, nativeUsedBytes,
					nativeReservedBytes, pages, rawPages, frontPages, tokenPages, vectorPages, reverseIndexedRecords,
					reverseCapacity, peakStagedBytes, tokenTrials, tokenTrialsSkipped, coalescedFamilies, 0);
		}

		public double bytesPerRecord() {
			return records == 0 ? 0 : (double) nativeUsedBytes / records;
		}

		public double recordCompressionRatio() {
			return pageBytes == 0 ? 1 : (double) inputRecordBytes / pageBytes;
		}
	}

	private final NativeSlabAllocator allocator;
	private final NativeLongList pages, blockFirst, blockIds, blockRoutes;
	private final long count;
	private final long firstId, stride;
	private final boolean affine;
	private final MemorySegment reverse;
	private final int reverseSlots;
	private final Stats stats;
	private final Options options;
	private final AtomicInteger references = new AtomicInteger(1);
	private final AtomicBoolean closed = new AtomicBoolean();

	private CompressedValueOverlay(Builder b) {
		allocator = b.allocator;
		options = b.options;
		pages = b.pages;
		blockFirst = b.blockFirst;
		blockIds = b.blockIds;
		blockRoutes = b.blockRoutes;
		count = b.count;
		firstId = b.firstId;
		stride = b.stride;
		affine = b.affine;
		reverse = b.reverse;
		reverseSlots = b.options.reverseSlots;
		long pageBytes = b.pageBytes;
		stats = new Stats(count, b.skipped, b.logicalBytes, pageBytes, allocator.usedBytes() - pageBytes,
				allocator.usedBytes(), allocator.reservedBytes(), pages.size(), b.modeCounts[0], b.modeCounts[1],
				b.modeCounts[2], b.modeCounts[3], b.reverseCount, reverseSlots, b.peakStagedBytes,
				b.tokenTrials, b.tokenTrialsSkipped, b.coalescedCount(), b.modeCounts[4]);
	}

	public static Builder builder(Options options) {
		return builder(options, OverlayMemoryBudget.unbounded());
	}

	public static Builder builder(Options options, OverlayMemoryBudget budget) {
		return new Builder(options, 13, budget);
	}

	static Builder deltaBuilder(Options options) {
		return deltaBuilder(options, OverlayMemoryBudget.unbounded());
	}

	static Builder deltaBuilder(Options options, OverlayMemoryBudget budget) {
		return new Builder(options, 6, budget);
	}

	static Builder captureBuilder(Options options, OverlayMemoryBudget budget) {
		return new Builder(options, 6, budget, true);
	}

	void adoptBudget(OverlayMemoryBudget budget) {
		allocator.adoptBudget(budget);
	}

	OverlayMemoryBudget memoryBudget() {
		return allocator.memoryBudget();
	}

	Options options() {
		return options;
	}

	public Stats stats() {
		return stats;
	}

	/** Acquire once per query/batch when possible, not once per decoded byte. */
	public Lease acquire() {
		for (;;) {
			if (closed.get())
				throw new IllegalStateException("overlay retired");
			int n = references.get();
			if (n == 0 || n == Integer.MAX_VALUE)
				throw new IllegalStateException("overlay cannot be retained");
			if (references.compareAndSet(n, n + 1))
				return new Lease(this);
		}
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true))
			release();
	}

	private void release() {
		if (references.decrementAndGet() == 0)
			allocator.close();
	}

	public static final class Lease implements AutoCloseable {
		private CompressedValueOverlay owner;

		private Lease(CompressedValueOverlay owner) {
			this.owner = owner;
		}

		/** Returned bytes belong to the caller. Null means this overlay does not know the ID. */
		public byte[] get(long id) {
			CompressedValueOverlay o = checked();
			long rank = o.rank(id);
			return rank < 0 ? null : o.recordAt(rank);
		}

		/** Callback-scoped selective access. Never reconstructs a complete record array. */
		public boolean visitRecord(long id, ValueStoreRecordVisitor.Visitor visitor) {
			CompressedValueOverlay o = checked();
			long rank = o.rank(id);
			return rank >= 0 && ValueStoreRecordVisitor.visit(o.openRecordAt(rank), visitor);
		}

		boolean visitRank(long rank, ValueStoreRecordVisitor.Visitor visitor) {
			CompressedValueOverlay o = checked();
			if (rank < 0 || rank >= o.count)
				throw new IndexOutOfBoundsException();
			return ValueStoreRecordVisitor.visit(o.openRecordAt(rank), visitor);
		}

		/** No decoded RDF objects; exact serialized key comparison after a fingerprint match. */
		public java.util.OptionalLong findId(byte[] record) {
			Objects.requireNonNull(record, "record");
			CompressedValueOverlay o = checked();
			if (o.reverseSlots == 0)
				return java.util.OptionalLong.empty();
			long hash = fingerprint(record);
			int code = (int) (hash >>> 32);
			int slot = (int) hash & (o.reverseSlots - 1);
			for (int probes = 0; probes < o.reverseSlots; probes++, slot = (slot + 1) & (o.reverseSlots - 1)) {
				long entry = o.reverse.get(FfmAccess.LONG_LE, slot * 8L);
				int position = (int) entry;
				if (position == 0)
					return java.util.OptionalLong.empty();
				if ((int) (entry >>> 32) == code) {
					long rank = Integer.toUnsignedLong(position) - 1;
					if (rank >= o.count)
						throw new IllegalStateException("corrupt reverse locator");
					if (Arrays.equals(record, o.recordAt(rank)))
						return java.util.OptionalLong.of(o.idAt(rank));
				}
			}
			return java.util.OptionalLong.empty();
		}

		public int getMany(long[] ids, int from, int length, byte[][] target, int targetOffset) {
			Objects.checkFromIndexSize(from, length, ids.length);
			Objects.checkFromIndexSize(targetOffset, length, target.length);
			checked();
			for (int i = 0; i < length; i++)
				target[targetOffset + i] = get(ids[from + i]);
			return length;
		}

		public Stats stats() {
			return checked().stats;
		}

		long rankOf(long id) {
			return checked().rank(id);
		}

		long idAtRank(long rank) {
			return checked().idAt(rank);
		}

		byte[] recordAtRank(long rank) {
			CompressedValueOverlay o = checked();
			if (rank < 0 || rank >= o.count)
				throw new IndexOutOfBoundsException();
			return o.recordAt(rank);
		}

		private CompressedValueOverlay checked() {
			CompressedValueOverlay o = owner;
			if (o == null)
				throw new IllegalStateException("overlay lease closed");
			return o;
		}

		/** Call only after this lease's consumers stop. Independent leases may keep reading. */
		@Override
		public void close() {
			if (owner != null) {
				CompressedValueOverlay o = owner;
				owner = null;
				o.release();
			}
		}
	}

	private long rank(long id) {
		if (count == 0 || Long.compareUnsigned(id, firstId) < 0)
			return -1;
		if (affine) {
			if (count == 1)
				return id == firstId ? 0 : -1;
			long delta = id - firstId;
			long rank = Long.divideUnsigned(delta, stride);
			return Long.compareUnsigned(rank, count) < 0 && Long.remainderUnsigned(delta, stride) == 0 ? rank : -1;
		}
		int lo = 0, hi = blockFirst.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (Long.compareUnsigned(blockFirst.get(mid), id) <= 0)
				lo = mid + 1;
			else
				hi = mid;
		}
		if (lo == 0)
			return -1;
		int block = lo - 1;
		long handle = blockIds.get(block);
		int slot = PackedVector.lowerBound(allocator, handle, id);
		long rank = ((long) block << PAGE_SHIFT) + slot;
		if (slot >= PAGE_ENTRIES || rank >= count || PackedVector.get(allocator, handle, slot) != id)
			return -1;
		return rank;
	}

	private long idAt(long rank) {
		if (rank < 0 || rank >= count)
			throw new IndexOutOfBoundsException();
		return PackedVector.get(allocator, blockIds.get((int) (rank >>> PAGE_SHIFT)), (int) rank & PAGE_MASK);
	}

	private RecordByteAccess openRecordAt(long rank) {
		long locator = PackedVector.get(allocator, blockRoutes.get((int) (rank >>> PAGE_SHIFT)),
				(int) rank & PAGE_MASK);
		int page = Math.toIntExact(locator >>> PAGE_SHIFT), slot = (int) locator & PAGE_MASK;
		long handle = pages.get(page);
		if (handle == 0)
			throw new IllegalStateException("unpublished value page");
		return PageCodec.openRecord(allocator, handle, slot);
	}

	private byte[] recordAt(long rank) {
		long locator = PackedVector.get(allocator, blockRoutes.get((int) (rank >>> PAGE_SHIFT)),
				(int) rank & PAGE_MASK);
		int page = Math.toIntExact(locator >>> PAGE_SHIFT);
		int slot = (int) locator & PAGE_MASK;
		long handle = pages.get(page);
		if (handle == 0)
			throw new IllegalStateException("unpublished value page");
		return PageCodec.read(allocator, handle, slot).bytes();
	}

	static long fingerprint(byte[] data) {
		long h = 0x9e3779b97f4a7c15L ^ data.length;
		int i = 0;
		for (; i + 8 <= data.length; i += 8) {
			long x = 0;
			for (int j = 0; j < 8; j++)
				x |= (data[i + j] & 255L) << (j * 8);
			h = Long.rotateLeft(h ^ mix(x), 27) * 0x3c79ac492ba7b653L + 0x1c69b3f74ac4ae35L;
		}
		long tail = 0;
		for (int j = 0; i < data.length; i++, j++)
			tail |= (data[i] & 255L) << (j * 8);
		return mix(h ^ tail);
	}

	private static long mix(long h) {
		h ^= h >>> 30;
		h *= 0xbf58476d1ce4e5b9L;
		h ^= h >>> 27;
		h *= 0x94d049bb133111ebL;
		return h ^ (h >>> 31);
	}

	/** Sorted by the complete unsigned ID, not by a stripped ordinal. ID tags are never discarded. */
	public static final class Builder implements AutoCloseable {
		private final Options options;
		private final int tokenMinRecords;
		private final NativeSlabAllocator allocator;
		private final OverlayMemoryBudget.Reservation staging;
		private final NativeLongList pages, blockFirst, blockIds, blockRoutes;
		private final LinkedHashMap<Affinity, Tail> active = new LinkedHashMap<>(32, 0.75f, true);
		private final long[] pendingIds = new long[PAGE_ENTRIES], pendingRoutes = new long[PAGE_ENTRIES];
		private final long[] modeCounts = new long[PageCodec.Mode.values().length];
		private final boolean sharedPrefixes = Boolean.parseBoolean(System.getProperty(
				"rdf4j.lmdb.valueOverlay.sharedPrefixes", "true"));
		private final int vectorMinSavingPercent = vectorSavingFloor();

		private static int vectorSavingFloor() {
			int floor = Integer.parseInt(System.getProperty("rdf4j.lmdb.valueOverlay.vectorMinSavingPercent", "8"));
			if (floor < 0 || floor > 100)
				throw new IllegalArgumentException("invalid vector saving floor");
			return floor;
		}

		private final TokenPolicy[] tokenPolicy = new TokenPolicy[RecordFamily.values().length];
		private final int[] tinyEvictions = new int[RecordFamily.values().length];
		private final boolean[] coalesced = new boolean[RecordFamily.values().length];
		private MemorySegment reverse;
		private long count, skipped, logicalBytes, pageBytes, stagedBytes, peakStagedBytes;
		private long reverseCount, firstId, stride, lastInput, lastAccepted, tokenTrials, tokenTrialsSkipped;
		private boolean hasInput, affine = true, transferred, closed;
		private int pendingCount;
		private BooleanSupplier cancelled = () -> false;

		private Builder(Options options, int directoryShift, OverlayMemoryBudget budget) {
			this(options, directoryShift, budget, false);
		}

		private Builder(Options options, int directoryShift, OverlayMemoryBudget budget, boolean capture) {
			this.options = Objects.requireNonNull(options);
			// Tiny foreground deltas amortize poorly; later compaction can train full pages.
			tokenMinRecords = capture ? Integer.MAX_VALUE : directoryShift == 13 ? 0 : 128;
			allocator = new NativeSlabAllocator(4L << 20, options.maxNativeBytes, budget);
			OverlayMemoryBudget.Reservation reserved = null;
			try {
				reserved = budget.reserve(OverlayMemoryBudget.Kind.WORKSPACE, 8192);
				pages = new NativeLongList(allocator, directoryShift);
				blockFirst = new NativeLongList(allocator, directoryShift);
				blockIds = new NativeLongList(allocator, directoryShift);
				blockRoutes = new NativeLongList(allocator, directoryShift);
				for (int i = 0; i < tokenPolicy.length; i++)
					tokenPolicy[i] = new TokenPolicy();
				if (options.reverseSlots > 0) {
					long h = allocator.allocate(options.reverseSlots * 8L, 8);
					reverse = allocator.segment(h).asSlice(NativeSlabAllocator.offset(h), options.reverseSlots * 8L);
				}
			} catch (RuntimeException | Error e) {
				if (reserved != null)
					reserved.close();
				allocator.close();
				throw e;
			}
			staging = reserved;
		}

		public Builder cancellation(BooleanSupplier cancelled) {
			this.cancelled = Objects.requireNonNull(cancelled);
			return this;
		}

		public boolean reverseHasCapacity() {
			return options.reverseSlots > 0 && reverseCount < options.reverseSlots * 3L / 4;
		}

		public long size() {
			return count;
		}

		/** Advance over an oversized record without allocating or copying its payload. */
		public void skipLarge(long id, long length) {
			ensureOpen();
			try {
				poll();
				if (length <= options.maxRecordBytes)
					throw new IllegalArgumentException("record is not oversized");
				if (hasInput && Long.compareUnsigned(id, lastInput) <= 0)
					throw new IllegalArgumentException("IDs must be strictly increasing in unsigned order");
				lastInput = id;
				hasInput = true;
				skipped++;
			} catch (RuntimeException | Error e) {
				close();
				throw e;
			}
		}

		/**
		 * reverseVisible must be independently verified against the live value-to-ID mapping in the same snapshot. An
		 * ID-to-value record alone is insufficient: retired IDs can retain their payload after reverse deletion.
		 * Oversized records are skipped and remain source fallbacks; all other failures poison and close this builder.
		 */
		public boolean add(long id, byte[] record, RecordFamily family, long affinity, boolean reverseVisible) {
			ensureOpen();
			try {
				Objects.requireNonNull(record);
				Objects.requireNonNull(family);
				poll();
				if (hasInput && Long.compareUnsigned(id, lastInput) <= 0)
					throw new IllegalArgumentException("IDs must be strictly increasing in unsigned order");
				lastInput = id;
				hasInput = true;
				if (record.length > options.maxRecordBytes) {
					skipped++;
					return false;
				}
				if (count >= Integer.MAX_VALUE)
					throw new OverlayCapacityException("overlay record rank exceeds 31 bits");
				// Shape hints preserve all bytes. Numeric and temporal length partitioning favors stable lexical
				// layouts.
				int shapeLength = switch (family) {
				case INTEGER, DECIMAL_FLOAT, TEMPORAL, BOOLEAN, BINARY, IRI -> record.length <= 512 ? record.length
						: -1;
				default -> -1;
				};
				Affinity key = pageAffinity(family, affinity, shapeLength);
				Tail tail = active.get(key);
				if (tail == null) {
					if (active.size() >= options.maxActivePages) {
						sealOldest();
						key = pageAffinity(family, affinity, shapeLength);
						tail = active.get(key);
					}
					if (tail == null) {
						if (pages.size() >= (Integer.MAX_VALUE >>> PAGE_SHIFT))
							throw new OverlayCapacityException("page locator limit");
						staging.grow(OverlayMemoryBudget.arrayBytes(PAGE_ENTRIES, 8) + 128);
						tail = new Tail(pages.size(), key);
						pages.add(0);
						active.put(key, tail);
					}
				}
				staging.grow(record.length + 64L);
				byte[] copy = record.clone();
				int slot = tail.count;
				tail.records[tail.count++] = new PhysicalRecord(copy, false, copy.length);
				tail.bytes += copy.length;
				stagedBytes += copy.length;
				peakStagedBytes = Math.max(peakStagedBytes, stagedBytes);
				pendingIds[pendingCount] = id;
				pendingRoutes[pendingCount++] = ((long) tail.page << PAGE_SHIFT) | slot;
				if (count == 0)
					firstId = id;
				else if (count == 1)
					stride = id - firstId;
				else if (id - lastAccepted != stride)
					affine = false;
				lastAccepted = id;
				if (reverseVisible && reverseHasCapacity())
					insertReverse(record, count);
				count++;
				logicalBytes = Math.addExact(logicalBytes, record.length);
				if (pendingCount == PAGE_ENTRIES)
					sealIndex();
				if (tail.count == PAGE_ENTRIES || tail.bytes >= options.maxPageBytes) {
					seal(tail);
					active.remove(key);
				}
				return true;
			} catch (RuntimeException | Error e) {
				close();
				throw e;
			}
		}

		public CompressedValueOverlay finish() {
			ensureOpen();
			try {
				poll();
				for (Tail tail : active.values())
					seal(tail);
				active.clear();
				if (pendingCount > 0)
					sealIndex();
				CompressedValueOverlay out = new CompressedValueOverlay(this);
				transferred = true;
				closed = true;
				staging.close();
				return out;
			} catch (RuntimeException | Error e) {
				close();
				throw e;
			}
		}

		private void sealOldest() {
			Iterator<Map.Entry<Affinity, Tail>> it = active.entrySet().iterator();
			Map.Entry<Affinity, Tail> entry = it.next();
			Tail victim = entry.getValue();
			int family = victim.affinity.family.ordinal();
			if (victim.count < 8 && ++tinyEvictions[family] >= 16)
				coalesced[family] = true;
			seal(victim);
			it.remove();
		}

		private Affinity pageAffinity(RecordFamily family, long domain, int length) {
			// Too many tiny domain-specific pages cost more than their payload. Coarsen only the offending family.
			// This affects physical clustering only; no namespace/datatype byte is removed from any record.
			return coalesced[family.ordinal()] ? new Affinity(family, 0, -1) : new Affinity(family, domain, length);
		}

		private int coalescedCount() {
			int n = 0;
			for (boolean value : coalesced)
				if (value)
					n++;
			return n;
		}

		private void seal(Tail tail) {
			poll();
			TokenPolicy policy = tokenPolicy[tail.affinity.family.ordinal()];
			boolean eligible = tail.affinity.family.trainTokens() && tail.bytes >= 1024
					&& tail.count >= tokenMinRecords;
			boolean tokens = eligible && (!options.adaptiveTokens || policy.tryTokens());
			if (tokens)
				tokenTrials++;
			else if (eligible)
				tokenTrialsSkipped++;
			// Explicit allowance for simultaneous candidate encodings; this is not a JVM heap-size assertion.
			try (OverlayMemoryBudget.Reservation work = allocator.memoryBudget()
					.reserve(
							OverlayMemoryBudget.Kind.WORKSPACE,
							Math.addExact(Math.multiplyExact((long) tail.bytes, 16),
									tokens ? (2L << 20) : (128L << 10)))) {
				PageCodec.SealedPage sealed = PageCodec.seal(tail.records, tail.count, 16, 16, tokens, 1024, 48,
						options.vectorCompression && tokenMinRecords != Integer.MAX_VALUE, options.adaptiveTokens,
						sharedPrefixes, vectorMinSavingPercent);
				if (tokens && options.adaptiveTokens)
					policy.observed(sealed.mode() == PageCodec.Mode.TOKEN);
				long handle = allocator.copy(sealed.pageBytes());
				pages.set(tail.page, handle);
				pageBytes += sealed.pageBytes().length;
				modeCounts[sealed.mode().ordinal()]++;
				stagedBytes -= tail.bytes;
				staging.shrink(tail.bytes + 64L * tail.count + OverlayMemoryBudget.arrayBytes(PAGE_ENTRIES, 8) + 128);
				Arrays.fill(tail.records, null);
			}
		}

		private void sealIndex() {
			blockFirst.add(pendingIds[0]);
			blockIds.add(allocator.copy(PackedVector.encode(pendingIds, pendingCount, true)));
			blockRoutes.add(allocator.copy(PackedVector.encode(pendingRoutes, pendingCount, true)));
			pendingCount = 0;
		}

		private void insertReverse(byte[] bytes, long rank) {
			long h = fingerprint(bytes);
			int slot = (int) h & (options.reverseSlots - 1);
			while (reverse.get(FfmAccess.LONG_LE, slot * 8L) != 0)
				slot = (slot + 1) & (options.reverseSlots - 1);
			long entry = (h & 0xffffffff00000000L) | (rank + 1);
			reverse.set(FfmAccess.LONG_LE, slot * 8L, entry);
			reverseCount++;
		}

		private void poll() {
			if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean())
				throw new java.util.concurrent.CancellationException("overlay construction cancelled");
		}

		private void ensureOpen() {
			if (closed)
				throw new IllegalStateException("builder closed");
		}

		@Override
		public void close() {
			if (!closed) {
				closed = true;
				active.clear();
				if (!transferred)
					allocator.close();
				staging.close();
			}
		}

		private record Affinity(RecordFamily family, long domain, int length) {
		}

		private static final class Tail {
			final int page;
			final Affinity affinity;
			final PhysicalRecord[] records = new PhysicalRecord[PAGE_ENTRIES];
			int count, bytes;

			Tail(int page, Affinity affinity) {
				this.page = page;
				this.affinity = affinity;
			}
		}

		private static final class TokenPolicy {
			int losses, skip;

			boolean tryTokens() {
				if (skip > 0) {
					skip--;
					return false;
				}
				return true;
			}

			void observed(boolean won) {
				if (won) {
					losses = 0;
					skip = 0;
				} else {
					losses = Math.min(losses + 1, 6);
					skip = (1 << losses) - 1;
				}
			}
		}
	}
}
