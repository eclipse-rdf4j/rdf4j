/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.util.BitSet;
import java.util.OptionalLong;
import java.util.function.BooleanSupplier;

/** Immutable compressed changed-ID run. Tombstones are retained through compaction to shadow the unchanged base. */
final class ValueOverlayDelta implements AutoCloseable {
	final CompressedValueOverlay records;
	final CompressedValueOverlay.Lease reader;
	final BitSet missing, reverseVisible;
	final int level;
	private int references = 1;
	private final OverlayMemoryBudget.Reservation masks;

	ValueOverlayDelta(CompressedValueOverlay records, BitSet missing, BitSet reverseVisible, int level,
			OverlayMemoryBudget.Reservation masks) {
		this.records = records;
		this.reader = records.acquire();
		this.missing = missing;
		this.reverseVisible = reverseVisible;
		this.level = level;
		this.masks = masks;
	}

	synchronized ValueOverlayDelta retain() {
		if (references == 0)
			throw new IllegalStateException("delta released");
		references = Math.addExact(references, 1);
		return this;
	}

	@Override
	public void close() {
		boolean dispose;
		synchronized (this) {
			if (references == 0)
				throw new IllegalStateException("delta double release");
			dispose = --references == 0;
		}
		if (dispose) {
			try {
				reader.close();
				records.close();
			} finally {
				masks.close();
			}
		}
	}

	long rank(long id) {
		return reader.rankOf(id);
	}

	byte[] getRank(long rank) {
		return missing.get(Math.toIntExact(rank)) ? null : reader.recordAtRank(rank);
	}

	long size() {
		return records.stats().records();
	}

	long reserved() {
		return records.stats().nativeReservedBytes() + ((long) missing.size() + reverseVisible.size()) / 8;
	}

	OptionalLong findId(byte[] bytes) {
		return reader.findId(bytes);
	}

	static final class Builder implements AutoCloseable {
		private final CompressedValueOverlay.Builder data;
		private final BitSet missing = new BitSet(), visible = new BitSet();
		private final int level;
		private final OverlayMemoryBudget.Reservation masks;
		private int maskWords;
		private boolean transferred;

		Builder(CompressedValueOverlay.Options options, int level, BooleanSupplier cancel) {
			this(options, level, cancel, OverlayMemoryBudget.unbounded());
		}

		Builder(CompressedValueOverlay.Options options, int level, BooleanSupplier cancel, OverlayMemoryBudget budget) {
			this(options, level, cancel, budget, false);
		}

		Builder(CompressedValueOverlay.Options options, int level, BooleanSupplier cancel, OverlayMemoryBudget budget,
				boolean capture) {
			this.data = (capture ? CompressedValueOverlay.captureBuilder(options, budget)
					: CompressedValueOverlay.deltaBuilder(options, budget)).cancellation(cancel);
			this.level = level;
			try {
				masks = budget.reserve(OverlayMemoryBudget.Kind.RETAINED_HEAP, 128);
			} catch (RuntimeException | Error failed) {
				data.close();
				throw failed;
			}
		}

		void add(long id, byte[] bytes, boolean reverse) {
			int position = Math.toIntExact(data.size());
			int required = (position >>> 6) + 1;
			if (required > maskWords) {
				int next = Math.max(required, Math.max(1, maskWords * 2));
				// Two BitSet arrays, including possible old/new overlap. Physical owner releases the reservation.
				masks.grow(2 * OverlayMemoryBudget.arrayBytes(next, 8));
				maskWords = next;
			}
			// A missing or intentionally uncovered replacement shadows EVERY older payload and reverse mapping.
			if (bytes == null) {
				missing.set(position);
				bytes = new byte[0];
				reverse = false;
			}
			if (reverse)
				visible.set(position);
			if (!data.add(id, bytes, ValueStoreRecordLayout.family(id, bytes),
					ValueStoreRecordLayout.affinity(bytes), reverse))
				throw new OverlayCapacityException("delta record must not be silently skipped");
		}

		ValueOverlayDelta finish() {
			CompressedValueOverlay records = data.finish();
			try {
				ValueOverlayDelta result = new ValueOverlayDelta(records, missing, visible, level, masks);
				transferred = true;
				return result;
			} catch (RuntimeException | Error failed) {
				records.close();
				throw failed;
			}
		}

		@Override
		public void close() {
			data.close();
			if (!transferred)
				masks.close();
		}
	}

	/** Newer wins by full unsigned ID. No base page is decoded or rewritten. Input runs remain immutable. */
	static ValueOverlayDelta merge(ValueOverlayDelta newer, ValueOverlayDelta older,
			CompressedValueOverlay.Options options, int level, BooleanSupplier cancel) {
		try (Builder b = new Builder(options, level, cancel, newer.records.memoryBudget())) {
			long n = 0, o = 0, ns = newer.size(), os = older.size();
			while (n < ns || o < os) {
				if ((n + o & 255) == 0 && (Thread.currentThread().isInterrupted() || cancel.getAsBoolean()))
					throw new java.util.concurrent.CancellationException("delta compaction cancelled");
				long ni = n < ns ? newer.reader.idAtRank(n) : 0;
				long oi = o < os ? older.reader.idAtRank(o) : 0;
				if (o == os || n < ns && Long.compareUnsigned(ni, oi) <= 0) {
					b.add(ni, newer.getRank(n), newer.reverseVisible.get(Math.toIntExact(n)));
					n++;
					if (o < os && ni == oi)
						o++;
				} else {
					b.add(oi, older.getRank(o), older.reverseVisible.get(Math.toIntExact(o)));
					o++;
				}
			}
			return b.finish();
		}
	}
}
