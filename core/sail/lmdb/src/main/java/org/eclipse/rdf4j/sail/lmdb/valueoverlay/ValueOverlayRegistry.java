/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Atomic publication for a single exact dictionary view. Retired snapshots survive already-acquired readers. */
public final class ValueOverlayRegistry implements AutoCloseable {
    private volatile Entry current;
    private boolean closed;
    private record Entry(Object generation, long transactionId, CompressedValueOverlay overlay) { }

    public CompressedValueOverlay.Lease acquire(Object generation, long transactionId) {
        Entry hint = current;
        if (generation == null || hint == null || hint.generation != generation || hint.transactionId != transactionId) return null;
        synchronized (this) {
            Entry e = current;
            if (closed || e == null || e.generation != generation || e.transactionId != transactionId) return null;
            return e.overlay.acquire();
        }
    }
    public boolean isPopulated() { return current != null; }
    public CompressedValueOverlay.Stats stats() { Entry e = current; return e == null ? null : e.overlay.stats(); }

    /** Always consumes ownership of candidate, including refusal. */
    public boolean publish(Object generation, long transactionId, CompressedValueOverlay candidate,
            BooleanSupplier stillCurrent) {
        Objects.requireNonNull(candidate); Objects.requireNonNull(stillCurrent);
        Entry previous;
        try {
            synchronized (this) {
                if (closed || generation == null || !stillCurrent.getAsBoolean()) { candidate.close(); return false; }
                previous = current;
                current = new Entry(generation, transactionId, candidate);
            }
        } catch (RuntimeException | Error e) { candidate.close(); throw e; }
        if (previous != null) previous.overlay.close();
        return true;
    }
    public void invalidate() {
        Entry old;
        synchronized (this) { old = current; current = null; }
        if (old != null) old.overlay.close();
    }
    @Override public void close() {
        synchronized (this) { if (closed) return; closed = true; }
        invalidate();
    }
}
