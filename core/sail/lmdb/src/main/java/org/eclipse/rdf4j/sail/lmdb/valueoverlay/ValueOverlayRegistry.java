/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Exact committed-transaction publications: an immutable compressed base plus size-tiered changed-ID runs. A miss is
 * UNKNOWN and must go to the authoritative transaction, never to an older overlay view. Native commit is external;
 * prepare before it, complete only after it succeeds and its ID is verified.
 */
public final class ValueOverlayRegistry implements AutoCloseable {
	public record DeltaOptions(long maxNativeBytes, int maxChangedIds, int retainedSnapshots, int maxLevels) {
		public DeltaOptions {
			if (maxNativeBytes < 0 || maxChangedIds < 1 || maxChangedIds > (1 << 24)
					|| retainedSnapshots < 1 || retainedSnapshots > 64 || maxLevels < 1 || maxLevels > 30)
				throw new IllegalArgumentException("invalid delta options");
		}

		public static DeltaOptions defaults() {
			return new DeltaOptions(64L << 20, 65_536, 4, 20);
		}

		public static DeltaOptions configured() {
			return new DeltaOptions(
					Long.parseLong(System.getProperty("rdf4j.lmdb.valueOverlay.delta.maxBytes", "67108864")),
					Integer.parseInt(System.getProperty("rdf4j.lmdb.valueOverlay.delta.maxChangedIds", "65536")),
					Integer.parseInt(System.getProperty("rdf4j.lmdb.valueOverlay.delta.retainedSnapshots", "4")),
					Integer.parseInt(System.getProperty("rdf4j.lmdb.valueOverlay.delta.maxLevels", "20")));
		}
	}

	/** Null bytes mean deleted or deliberately uncovered. Both shadow earlier records and fall back to LMDB. */
	public record Record(byte[] bytes, boolean reverseVisible) {
	}

	@FunctionalInterface
	public interface Loader {
		Record load(long id) throws IOException;
	}

	public record ViewStats(long transactionId, long baseTransactionId, int runs, long changedEntries,
			long currentDeltaReservedBytes, int retainedSnapshots, long commits, long compactions,
			long invalidations, String lastRefusal) {
	}

	private final DeltaOptions options;
	private final OverlayMemoryBudget memoryBudget;
	private final boolean automaticMaintenance;
	private ExecutorService maintenanceExecutor;
	private boolean maintenanceScheduled, maintenanceActive;
	private long maintenanceRefusals, maintenanceDiscards;
	private String maintenanceFailure;
	private volatile State current;
	private final ArrayDeque<State> history = new ArrayDeque<>();
	private volatile boolean closed;
	private long commits, compactions, invalidations;
	private String lastRefusal;

	public ValueOverlayRegistry() {
		this(DeltaOptions.defaults());
	}

	public ValueOverlayRegistry(DeltaOptions options) {
		this(options, OverlayMemoryBudget.unbounded(), false);
	}

	public ValueOverlayRegistry(DeltaOptions options, OverlayMemoryBudget memoryBudget) {
		this(options, memoryBudget, false);
	}

	public ValueOverlayRegistry(DeltaOptions options, OverlayMemoryBudget memoryBudget, boolean automaticMaintenance) {
		this.options = Objects.requireNonNull(options);
		this.memoryBudget = Objects.requireNonNull(memoryBudget);
		this.automaticMaintenance = automaticMaintenance;
	}

	/** Production registries share one process-wide admission budget; worker creation is lazy. */
	public static ValueOverlayRegistry configured() {
		return new ValueOverlayRegistry(DeltaOptions.configured(), OverlayMemoryBudget.configuredShared(),
				Boolean.parseBoolean(System.getProperty("rdf4j.lmdb.valueOverlay.delta.asyncCompaction", "true")));
	}

	public OverlayMemoryBudget.Stats retainedMemoryStats() {
		return memoryBudget.stats();
	}

	public record MaintenanceStats(boolean scheduled, boolean active, long refusals, long discarded,
			String lastFailure) {
	}

	public synchronized MaintenanceStats maintenanceStats() {
		return new MaintenanceStats(maintenanceScheduled, maintenanceActive, maintenanceRefusals, maintenanceDiscards,
				maintenanceFailure);
	}

	public CompressedValueOverlay.Builder newBaseBuilder(CompressedValueOverlay.Options options) {
		return CompressedValueOverlay.builder(options, memoryBudget);
	}

	/** Backward-compatible base-only lease; layered production readers use acquireSnapshot. */
	public synchronized CompressedValueOverlay.Lease acquire(Object generation, long transactionId) {
		State s = current;
		return !closed && generation != null && s != null && s.generation == generation
				&& s.transactionId == transactionId && s.runs.isEmpty() ? s.base.overlay.acquire() : null;
	}

	public synchronized SnapshotLease acquireSnapshot(long transactionId) {
		if (closed)
			return null;
		State s = current;
		if (s != null && s.transactionId == transactionId)
			return new SnapshotLease(s.retain());
		for (State old : history)
			if (old.transactionId == transactionId)
				return new SnapshotLease(old.retain());
		return null;
	}

	public boolean isPopulated() {
		return current != null;
	}

	public CompressedValueOverlay.Stats stats() {
		State s = current;
		return s == null ? null : s.base.overlay.stats();
	}

	public synchronized ViewStats viewStats() {
		State s = current;
		long entries = 0, bytes = 0;
		if (s != null)
			for (ValueOverlayDelta run : s.runs) {
				entries += run.size();
				bytes += run.reserved();
			}
		return new ViewStats(s == null ? -1 : s.transactionId, s == null ? -1 : s.base.transactionId,
				s == null ? 0 : s.runs.size(), entries, bytes, history.size() + (s == null ? 0 : 1),
				commits, compactions, invalidations, lastRefusal);
	}

	/**
	 * Consumes candidate ownership on every outcome. Warm publication clears historical acquisition, not live leases.
	 */
	public boolean publish(Object generation, long transactionId, CompressedValueOverlay candidate,
			BooleanSupplier stillCurrent) {
		Objects.requireNonNull(candidate);
		Objects.requireNonNull(stillCurrent);
		synchronized (this) {
			try {
				if (closed || generation == null || !stillCurrent.getAsBoolean()) {
					candidate.close();
					return false;
				}
				// Do not replace a newer exact snapshot with a slow warm scan of an older read transaction.
				if (current != null && Long.compareUnsigned(transactionId, current.transactionId) < 0) {
					candidate.close();
					return false;
				}
				candidate.adoptBudget(memoryBudget);
				Base base = new Base(candidate, transactionId);
				State next;
				try {
					next = new State(generation, transactionId, base, List.of());
				} finally {
					base.close();
				}
				retireAll();
				current = next;
				lastRefusal = null;
				return true;
			} catch (RuntimeException | Error e) {
				candidate.close();
				throw e;
			}
		}
	}

	/** The caller must pass the newly opened native WRITE transaction's ID, never a guessed timestamp. */
	public synchronized Mutation begin(long writeTransactionId) {
		State s = current;
		if (closed || s == null)
			return null;
		if (s.transactionId != writeTransactionId - 1) {
			refuse("unobserved native transaction gap");
			return null;
		}
		State pin = s.retain();
		try {
			return new Mutation(this, pin, writeTransactionId, options.maxChangedIds);
		} catch (OverlayCapacityException refusal) {
			// The authoritative writer is already open. Acceleration refusal must not abort it or invalidate
			// existing committed readers before the actual commit; rollback can still retain the old view.
			return new Mutation(this, pin, writeTransactionId, "changed-ID retained-memory admission refused");
		} catch (RuntimeException | Error failed) {
			pin.close();
			throw failed;
		}
	}

	/**
	 * Consumes the prepared publication. committedTransactionId is read from native metadata AFTER successful commit.
	 * No-op commits are allowed only when native metadata still identifies the anchor. Future or missing IDs refuse.
	 */
	public boolean complete(Prepared prepared, long committedTransactionId) {
		if (prepared == null)
			return false;
		synchronized (this) {
			try {
				if (prepared.registry != this || prepared.closed)
					throw new IllegalStateException("wrong/preclosed publication");
				if (closed)
					return false;
				State anchor = prepared.anchor;
				// Compaction replaces physical storage, not logical commit identity. Rebase onto that storage.
				if (current == null || current.lineage != anchor.lineage) {
					if (current != null && Long.compareUnsigned(current.transactionId, committedTransactionId) <= 0)
						refuse("publication raced a replacement");
					return false;
				}
				if (committedTransactionId == anchor.transactionId)
					return true; // Native no-op: no invented generation.
				if (committedTransactionId != prepared.transactionId || !prepared.valid) {
					refuse(prepared.reason == null ? "commit transaction mismatch" : prepared.reason);
					return false;
				}
				State previous = current;
				ArrayList<ValueOverlayDelta> runs = new ArrayList<>(previous.runs.size() + prepared.runs.size());
				runs.addAll(prepared.runs);
				runs.addAll(previous.runs);
				long bytes = 0;
				for (ValueOverlayDelta run : runs)
					bytes = Math.addExact(bytes, run.reserved());
				if (bytes > options.maxNativeBytes || runs.size() > Math.max(4, options.maxLevels * 2)) {
					refuse("committed delta backlog exceeds bounded run/byte allowance");
					return false;
				}
				State next;
				try {
					next = new State(anchor.generation, committedTransactionId, previous.base, runs);
				} catch (OverlayCapacityException refusal) {
					// Native commit already succeeded. Refuse acceleration without surfacing a false commit failure.
					refuse("committed publication retained-memory admission refused");
					return false;
				}
				current = next;
				history.addFirst(previous);
				while (history.size() >= options.retainedSnapshots)
					history.removeLast().close();
				commits++;
				lastRefusal = null;
				requestMaintenance();
				return true;
			} finally {
				prepared.close();
			}
		}
	}

	public synchronized void invalidate() {
		refuse("explicit invalidation");
	}

	private void refuse(String reason) {
		lastRefusal = reason;
		invalidations++;
		retireAll();
	}

	private void retireAll() {
		State old = current;
		current = null;
		if (old != null)
			old.close();
		while (!history.isEmpty())
			history.removeFirst().close();
	}

	@Override
	public synchronized void close() {
		if (!closed) {
			closed = true;
			retireAll();
			if (maintenanceExecutor != null)
				maintenanceExecutor.shutdownNow();
			maintenanceScheduled = false;
		}
	}

	/**
	 * Claims one pair of committed immutable runs. No native transaction, decoding, or large builder allocation is done
	 * under the registry monitor. Call build/publish/close outside a writer; only one compaction is active per
	 * registry.
	 */
	public synchronized Compaction prepareCompaction() {
		if (closed || maintenanceActive || current == null)
			return null;
		int pair = compactionPair(current.runs);
		if (pair < 0)
			return null;
		State pin = current.retain();
		try {
			Compaction job = new Compaction(this, pin, pair);
			maintenanceActive = true;
			return job;
		} catch (RuntimeException | Error failed) {
			pin.close();
			throw failed;
		}
	}

	private int compactionPair(List<ValueOverlayDelta> runs) {
		for (int i = 0; i + 1 < runs.size(); i++)
			if (runs.get(i).level == runs.get(i + 1).level)
				return i;
		// Backlog pressure, not the lifetime commit count, can compact dissimilar adjacent tiers.
		return runs.size() > options.maxLevels ? runs.size() - 2 : -1;
	}

	/** Explicit maintenance for embedded users with their own scheduler. Never called by Mutation.prepare(). */
	public int compact() {
		return compact(() -> false);
	}

	public int compact(BooleanSupplier cancel) {
		Objects.requireNonNull(cancel);
		int installed = 0;
		for (;;) {
			try (Compaction job = prepareCompaction()) {
				if (job == null)
					return installed;
				job.build(cancel);
				if (job.publish())
					installed++;
				else
					return installed;
			}
		}
	}

	private synchronized void requestMaintenance() {
		if (!automaticMaintenance || closed || maintenanceScheduled || maintenanceActive || current == null
				|| compactionPair(current.runs) < 0)
			return;
		maintenanceScheduled = true;
		try {
			if (maintenanceExecutor == null)
				maintenanceExecutor = Executors.newSingleThreadExecutor(r -> {
					Thread thread = new Thread(r, "rdf4j-value-overlay-compaction");
					thread.setDaemon(true);
					return thread;
				});
			maintenanceExecutor.execute(() -> {
				boolean failed = false;
				try {
					compact(() -> closed);
				} catch (RuntimeException refusal) {
					failed = true;
					synchronized (ValueOverlayRegistry.this) {
						maintenanceRefusals++;
						maintenanceFailure = refusal.getClass().getSimpleName() + ": " + refusal.getMessage();
					}
				} catch (Error failure) {
					failed = true; // Never busy-retry an allocation or VM failure from this worker.
					synchronized (ValueOverlayRegistry.this) {
						maintenanceFailure = failure.getClass().getSimpleName();
					}
					throw failure;
				} finally {
					synchronized (ValueOverlayRegistry.this) {
						maintenanceScheduled = false;
						// Do not spin on capacity/cancellation. A subsequent commit or explicit compact can retry.
						if (!failed)
							requestMaintenance();
						ValueOverlayRegistry.this.notifyAll();
					}
				}
			});
		} catch (RejectedExecutionException | SecurityException refusal) {
			maintenanceScheduled = false;
			maintenanceRefusals++;
			maintenanceFailure = "maintenance executor rejected task";
		}
	}

	public static final class Compaction implements AutoCloseable {
		private final ValueOverlayRegistry registry;
		private State anchor;
		private final ValueOverlayDelta newer, older;
		private ValueOverlayDelta merged;
		private boolean attempted, published;

		private Compaction(ValueOverlayRegistry registry, State anchor, int pair) {
			this.registry = registry;
			this.anchor = anchor;
			newer = anchor.runs.get(pair);
			older = anchor.runs.get(pair + 1);
		}

		public void build() {
			build(() -> false);
		}

		public void build(BooleanSupplier cancel) {
			if (anchor == null || attempted)
				throw new IllegalStateException("compaction closed/already built");
			Objects.requireNonNull(cancel);
			attempted = true;
			merged = ValueOverlayDelta.merge(newer, older,
					deltaOptions(anchor, registry.options, newer.size() + older.size()),
					Math.min(Math.max(newer.level, older.level) + 1, registry.options.maxLevels - 1),
					() -> registry.closed || cancel.getAsBoolean());
		}

		/**
		 * Replace exactly the captured contiguous pair. Newer commits may prepend runs and are preserved. Warm
		 * replacement, invalidation, or a missing pair discards the result; no stale snapshot is published.
		 */
		public boolean publish() {
			if (anchor == null || merged == null || published)
				throw new IllegalStateException("compaction not built/closed");
			published = true;
			synchronized (registry) {
				State now = registry.current;
				if (registry.closed || now == null || now.base != anchor.base) {
					registry.maintenanceDiscards++;
					return false;
				}
				int pair = -1;
				for (int i = 0; i + 1 < now.runs.size(); i++)
					if (now.runs.get(i) == newer && now.runs.get(i + 1) == older) {
						pair = i;
						break;
					}
				if (pair < 0) {
					registry.maintenanceDiscards++;
					return false;
				}
				ArrayList<ValueOverlayDelta> runs = new ArrayList<>(now.runs);
				runs.set(pair, merged);
				runs.remove(pair + 1);
				long bytes = 0;
				for (ValueOverlayDelta run : runs)
					bytes = Math.addExact(bytes, run.reserved());
				if (bytes > registry.options.maxNativeBytes) {
					registry.maintenanceRefusals++;
					registry.maintenanceFailure = "compacted current run allowance";
					return false;
				}
				State next = new State(now.generation, now.transactionId, now.base, runs, now.lineage);
				registry.current = next;
				// Same logical snapshot, not another history entry. Held readers retain the old physical owners.
				now.close();
				registry.compactions++;
				registry.maintenanceFailure = null;
				return true;
			}
		}

		@Override
		public void close() {
			if (anchor != null) {
				State pin = anchor;
				anchor = null;
				try {
					if (merged != null) {
						merged.close();
						merged = null;
					}
				} finally {
					pin.close();
					synchronized (registry) {
						registry.maintenanceActive = false;
						registry.requestMaintenance();
						registry.notifyAll();
					}
				}
			}
		}
	}

	/** Writer-confined capture. At preparation the loader reads FINAL state in the same native write transaction. */
	public static final class Mutation implements AutoCloseable {
		private final ValueOverlayRegistry registry;
		private State anchor;
		private final long transactionId;
		private final ValueOverlayChanges changes;
		private boolean incomplete, prepared;
		private String reason;

		private Mutation(ValueOverlayRegistry registry, State anchor, long txn, int limit) {
			this.registry = registry;
			this.anchor = anchor;
			transactionId = txn;
			changes = new ValueOverlayChanges(limit, registry.memoryBudget);
		}

		private Mutation(ValueOverlayRegistry registry, State anchor, long txn, String refusal) {
			this.registry = registry;
			this.anchor = anchor;
			transactionId = txn;
			changes = null;
			incomplete = true;
			reason = refusal;
		}

		public long transactionId() {
			return transactionId;
		}

		public int maxRecordBytes() {
			if (anchor == null)
				throw new IllegalStateException("mutation closed");
			return anchor.base.overlay.options().maxRecordBytes();
		}

		public boolean requiresReverseMapping() {
			if (anchor == null)
				throw new IllegalStateException("mutation closed");
			return anchor.base.overlay.options().reverseSlots() != 0;
		}

		public int changedIds() {
			return changes == null ? 0 : changes.size();
		}

		public void touch(long id) {
			check();
			if (!incomplete) {
				try {
					if (!changes.add(id))
						invalidate("changed-ID staging limit");
				} catch (OverlayCapacityException refusal) {
					invalidate("changed-ID retained-memory limit");
				}
			}
		}

		/** For raw/bulk operations whose entire effect is not captured, never apply an incomplete delta. */
		public void invalidate(String why) {
			check();
			incomplete = true;
			reason = Objects.requireNonNull(why);
		}

		private void check() {
			if (anchor == null || prepared)
				throw new IllegalStateException("mutation closed/prepared");
		}

		public Prepared prepare(Loader loader) throws IOException {
			return prepare(loader, () -> false);
		}

		public Prepared prepare(Loader loader, BooleanSupplier cancel) throws IOException {
			check();
			Objects.requireNonNull(loader);
			Objects.requireNonNull(cancel);
			prepared = true;
			ArrayList<ValueOverlayDelta> runs = new ArrayList<>();
			int merges = 0;
			try {
				if (incomplete)
					return new Prepared(registry, anchor.retain(), transactionId, runs, false, reason, 0);
				if (changes.size() != 0) {
					CompressedValueOverlay.Options spec = deltaOptions(anchor, registry.options, changes.size());
					try (OverlayMemoryBudget.Reservation sorted = registry.memoryBudget.reserve(
							OverlayMemoryBudget.Kind.WORKSPACE, OverlayMemoryBudget.arrayBytes(changes.size(), 8));
							ValueOverlayDelta.Builder b = new ValueOverlayDelta.Builder(spec, 0, cancel,
									registry.memoryBudget, true)) {
						for (long id : changes.sorted()) {
							if (Thread.currentThread().isInterrupted() || cancel.getAsBoolean())
								throw new java.util.concurrent.CancellationException("delta preparation cancelled");
							Record record = Objects.requireNonNull(loader.load(id), "loader result");
							// Oversized replacements remain shadow entries, not permission to read stale base bytes.
							byte[] bytes = record.bytes;
							if (bytes != null && bytes.length > spec.maxRecordBytes())
								bytes = null;
							b.add(id, bytes, bytes != null && record.reverseVisible);
						}
						runs.add(b.finish());
					}
				}
				// Only the fresh bounded run is prepared in the native writer. Existing runs are immutable
				// and are NEVER decoded or recompressed here. complete() attaches the latest physical representation.
				long used = 0;
				for (ValueOverlayDelta run : runs)
					used = Math.addExact(used, run.reserved());
				if (used > registry.options.maxNativeBytes)
					throw new OverlayCapacityException("live delta budget");
				return new Prepared(registry, anchor.retain(), transactionId, runs, true, null, merges);
			} catch (IOException | RuntimeException | Error e) {
				for (ValueOverlayDelta run : runs)
					run.close();
				throw e;
			}
		}

		/** Builds an explicitly refused publication after optional acceleration failed. */
		public Prepared refused(String why) {
			if (anchor == null)
				throw new IllegalStateException("mutation closed");
			prepared = true;
			return new Prepared(registry, anchor.retain(), transactionId, new ArrayList<>(), false,
					Objects.requireNonNull(why), 0);
		}

		@Override
		public void close() {
			if (anchor != null) {
				State a = anchor;
				anchor = null;
				if (changes != null)
					changes.close();
				a.close();
			}
		}
	}

	private static CompressedValueOverlay.Options deltaOptions(State anchor, DeltaOptions budget, long records) {
		CompressedValueOverlay.Options base = anchor.base.overlay.options();
		int slots = 0;
		if (base.reverseSlots() != 0) {
			slots = 4;
			while (slots < base.reverseSlots() && slots * 3L / 4 < records)
				slots <<= 1;
			slots = Math.min(slots, base.reverseSlots());
		}
		return new CompressedValueOverlay.Options(budget.maxNativeBytes, slots,
				Math.min(8, base.maxActivePages()), base.maxPageBytes(), base.maxRecordBytes(), true,
				base.vectorCompression());
	}

	public static final class Prepared implements AutoCloseable {
		private final ValueOverlayRegistry registry;
		private final State anchor;
		private final long transactionId;
		private final List<ValueOverlayDelta> runs;
		private final boolean valid;
		private final String reason;
		private final int merges;
		private boolean closed;

		private Prepared(ValueOverlayRegistry registry, State anchor, long transactionId,
				List<ValueOverlayDelta> runs, boolean valid, String reason, int merges) {
			this.registry = registry;
			this.anchor = anchor;
			this.transactionId = transactionId;
			this.runs = runs;
			this.valid = valid;
			this.reason = reason;
			this.merges = merges;
		}

		public long transactionId() {
			return transactionId;
		}

		@Override
		public void close() {
			if (!closed) {
				closed = true;
				for (ValueOverlayDelta run : runs)
					run.close();
				anchor.close();
			}
		}
	}

	public static final class SnapshotLease implements AutoCloseable {
		private State state;

		private SnapshotLease(State state) {
			this.state = state;
		}

		private State checked() {
			if (state == null)
				throw new IllegalStateException("snapshot lease closed");
			return state;
		}

		public long transactionId() {
			return checked().transactionId;
		}

		public byte[] get(long id) {
			State s = checked();
			for (ValueOverlayDelta run : s.runs) {
				long rank = run.rank(id);
				if (rank >= 0)
					return run.getRank(rank);
			}
			return s.base.reader.get(id);
		}

		/** Typed callback on the newest visible record; shadow entries never fall through to older runs. */
		public boolean visitRecord(long id, ValueStoreRecordVisitor.Visitor visitor) {
			Objects.requireNonNull(visitor);
			State s = checked();
			for (ValueOverlayDelta run : s.runs) {
				long rank = run.rank(id);
				if (rank >= 0)
					return !run.missing.get(Math.toIntExact(rank)) && run.reader.visitRank(rank, visitor);
			}
			return s.base.reader.visitRecord(id, visitor);
		}

		/** Reconstructs only the selected record; the returned owned lexical view survives closing this lease. */
		public ValueStoreRecordView recordView(long id) {
			byte[] record = get(id);
			return record == null ? null : ValueStoreRecordView.takeOwnership(record);
		}

		public OptionalLong findId(byte[] record) {
			Objects.requireNonNull(record);
			State s = checked();
			for (int i = 0; i < s.runs.size(); i++) {
				OptionalLong candidate = s.runs.get(i).findId(record);
				if (candidate.isPresent() && !shadowed(s, candidate.getAsLong(), i))
					return candidate;
			}
			OptionalLong candidate = s.base.reader.findId(record);
			return candidate.isPresent() && !shadowed(s, candidate.getAsLong(), s.runs.size())
					? candidate
					: OptionalLong.empty();
		}

		private static boolean shadowed(State s, long id, int before) {
			for (int i = 0; i < before; i++)
				if (s.runs.get(i).rank(id) >= 0)
					return true;
			return false;
		}

		public int getMany(long[] ids, int from, int count, byte[][] output, int offset) {
			Objects.checkFromIndexSize(from, count, ids.length);
			Objects.checkFromIndexSize(offset, count, output.length);
			checked();
			for (int i = 0; i < count; i++)
				output[offset + i] = get(ids[from + i]);
			return count;
		}

		@Override
		public void close() {
			if (state != null) {
				State s = state;
				state = null;
				s.close();
			}
		}
	}

	private static final class Base implements AutoCloseable {
		final CompressedValueOverlay overlay;
		final CompressedValueOverlay.Lease reader;
		final long transactionId;
		int references = 1;

		Base(CompressedValueOverlay overlay, long transactionId) {
			this.overlay = overlay;
			this.transactionId = transactionId;
			reader = overlay.acquire();
		}

		synchronized Base retain() {
			if (references == 0)
				throw new IllegalStateException("base released");
			references = Math.addExact(references, 1);
			return this;
		}

		@Override
		public void close() {
			boolean dispose;
			synchronized (this) {
				if (references == 0)
					throw new IllegalStateException("base double release");
				dispose = --references == 0;
			}
			if (dispose) {
				reader.close();
				overlay.close();
			}
		}
	}

	private static final class State implements AutoCloseable {
		final Object generation;
		final Object lineage;
		final OverlayMemoryBudget.Reservation metadata;
		final long transactionId;
		final Base base;
		final List<ValueOverlayDelta> runs;
		final AtomicInteger references = new AtomicInteger(1);

		State(Object generation, long transactionId, Base base, List<ValueOverlayDelta> runs) {
			this(generation, transactionId, base, runs, new Object());
		}

		State(Object generation, long transactionId, Base base, List<ValueOverlayDelta> runs, Object lineage) {
			this.generation = generation;
			this.transactionId = transactionId;
			this.lineage = lineage;
			this.metadata = base.overlay.memoryBudget()
					.reserve(OverlayMemoryBudget.Kind.RETAINED_HEAP,
							160 + OverlayMemoryBudget.arrayBytes(runs.size(), 8));
			List<ValueOverlayDelta> copy;
			try {
				copy = List.copyOf(runs);
			} catch (RuntimeException | Error failed) {
				metadata.close();
				throw failed;
			}
			this.runs = copy;
			try {
				this.base = base.retain();
			} catch (RuntimeException | Error failed) {
				metadata.close();
				throw failed;
			}
			int retained = 0;
			try {
				for (ValueOverlayDelta run : copy) {
					run.retain();
					retained++;
				}
			} catch (RuntimeException | Error failed) {
				for (int i = 0; i < retained; i++)
					copy.get(i).close();
				this.base.close();
				metadata.close();
				throw failed;
			}
		}

		State retain() {
			for (;;) {
				int n = references.get();
				if (n == 0 || n == Integer.MAX_VALUE)
					throw new IllegalStateException("state released/overflow");
				if (references.compareAndSet(n, n + 1))
					return this;
			}
		}

		@Override
		public void close() {
			if (references.decrementAndGet() == 0) {
				try {
					for (ValueOverlayDelta run : runs)
						run.close();
					base.close();
				} finally {
					metadata.close();
				}
			}
		}
	}
}
