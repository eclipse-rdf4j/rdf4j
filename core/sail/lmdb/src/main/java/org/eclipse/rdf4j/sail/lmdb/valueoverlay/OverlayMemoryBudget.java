/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

/**
 * Shared admission ledger for overlay allocations. Charge physical owners, not publications or leases. Reservations
 * precede allocations and survive retirement until the final owner releases its storage. Native capacity is exact.
 * Explicit heap/workspace charges are conservative estimates, not JVM/RSS accounting. No read-path update is required.
 * This object can be injected into several registries to share one limit.
 */
public final class OverlayMemoryBudget {
	public enum Kind {
		NATIVE,
		RETAINED_HEAP,
		WORKSPACE
	}

	public record Stats(long limit, long used, long peak, long nativeBytes, long retainedHeapBytes,
			long workspaceBytes, long refusals) {
	}

	private static final OverlayMemoryBudget UNLIMITED = new OverlayMemoryBudget(Long.MAX_VALUE);

	private static final class Configured {
		static final OverlayMemoryBudget INSTANCE = new OverlayMemoryBudget(Long.parseLong(System.getProperty(
				"rdf4j.lmdb.valueOverlay.retained.maxBytes", "2147483648")));
	}

	public static OverlayMemoryBudget configuredShared() {
		return Configured.INSTANCE;
	}

	static OverlayMemoryBudget unbounded() {
		return UNLIMITED;
	}

	private final long limit;
	private final long[] byKind = new long[Kind.values().length];
	private long used, peak, refusals;

	public OverlayMemoryBudget(long limit) {
		if (limit < 0)
			throw new IllegalArgumentException("negative retained-memory limit");
		this.limit = limit;
	}

	public Reservation reserve(Kind kind, long bytes) {
		if (kind == null)
			throw new NullPointerException("kind");
		Reservation result = new Reservation(this, kind);
		result.grow(bytes);
		return result;
	}

	private synchronized void charge(Kind kind, long bytes) {
		if (bytes < 0)
			throw new IllegalArgumentException("negative reservation");
		if (bytes > limit - used) {
			refusals++;
			throw new OverlayCapacityException("shared retained overlay budget exceeded: used=" + used
					+ ", requested=" + bytes + ", limit=" + limit);
		}
		used += bytes;
		byKind[kind.ordinal()] += bytes;
		peak = Math.max(peak, used);
	}

	private synchronized void release(Kind kind, long bytes) {
		if (bytes < 0 || bytes > byKind[kind.ordinal()])
			throw new IllegalStateException("reservation underflow");
		used -= bytes;
		byKind[kind.ordinal()] -= bytes;
	}

	public synchronized Stats stats() {
		return new Stats(limit, used, peak, byKind[0], byKind[1], byKind[2], refusals);
	}

	/** Estimate for an explicitly allocated primitive/reference array (8-byte references, 24-byte header). */
	static long arrayBytes(long elements, int width) {
		return Math.addExact(Math.addExact(Math.multiplyExact(elements, width), 24), 7) & ~7L;
	}

	public static final class Reservation implements AutoCloseable {
		private final OverlayMemoryBudget budget;
		private final Kind kind;
		private long bytes;
		private boolean closed;

		private Reservation(OverlayMemoryBudget budget, Kind kind) {
			this.budget = budget;
			this.kind = kind;
		}

		public synchronized void grow(long additional) {
			if (closed)
				throw new IllegalStateException("reservation closed");
			if (additional < 0)
				throw new IllegalArgumentException("negative growth");
			long next = Math.addExact(bytes, additional);
			budget.charge(kind, additional);
			bytes = next;
		}

		public synchronized void shrink(long amount) {
			if (closed || amount < 0 || amount > bytes)
				throw new IllegalStateException("invalid release");
			budget.release(kind, amount);
			bytes -= amount;
		}

		public synchronized long bytes() {
			return bytes;
		}

		@Override
		public synchronized void close() {
			if (!closed) {
				closed = true;
				budget.release(kind, bytes);
				bytes = 0;
			}
		}
	}
}
