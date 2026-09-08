/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;

import java.util.Arrays;
import java.util.Objects;
import org.eclipse.rdf4j.sail.lmdb.evaluation.NativeLmdbQuerySource;
import org.eclipse.rdf4j.sail.lmdb.evaluation.NativeLmdbQuerySource.NativeAdjacency;

/**
 * Resumable primitive algorithms shared by interpreted and generated kernels. These cursors know neither IR nodes
 * nor scalar-register layouts. Each instance is owned by one operator activation; the enclosing context retains
 * the snapshot. Close releases activation state but never closes an adjacency owned by that context.
 */
public final class KernelExpansionCursors {
    private KernelExpansionCursors() { }

    public abstract static class Values implements AutoCloseable {
        protected final KernelCancellation cancellation;
        protected long value;
        protected int tick;
        protected boolean done;
        protected Values(KernelCancellation cancellation) { this.cancellation = cancellation; }
        public final long value() { return value; }
        protected final void poll() { if ((++tick & 1023) == 0) KernelRuntime.checkCancelled(cancellation); }
        public abstract boolean next();
        @Override public void close() { done = true; }
    }

    /** A partition cursor remains positioned while its downstream continuation is suspended. */
    public static final class Domains extends Values {
        private NativeLmdbQuerySource.NodeDomainIntersection view;
        private NativeLmdbQuerySource.NodeDomainIntersection.Cursor cursor;
        private int partition;
        public Domains(NativeLmdbQuerySource.NodeDomainIntersection view, KernelCancellation cancellation) {
            super(cancellation); this.view = Objects.requireNonNull(view);
        }
        @Override public boolean next() {
            if (done) return false;
            KernelRuntime.checkCancelled(cancellation);
            while (true) {
                if (cursor != null && cursor.next()) { value = cursor.nodeId(); return true; }
                if (partition == view.partitionCount()) { close(); return false; }
                poll(); cursor = view.cursor(partition++);
            }
        }
        @Override public void close() { super.close(); cursor = null; view = null; }
    }

    /** Subject/object DISTINCT with bounded quad staging and demand-driven source opening. */
    public static final class Terms extends Values {
        private KernelScanner scanner;
        private final int scan;
        private KernelQuadCursor cursor;
        private KernelRuntime.LongHashSet seen;
        private long[] buffer;
        private int position, count;
        public Terms(KernelScanner scanner, int scan, KernelCancellation cancellation) {
            super(cancellation); this.scanner = Objects.requireNonNull(scanner); this.scan = scan;
        }
        @Override public boolean next() {
            if (done) return false;
            try {
                if (cursor == null) {
                    KernelRuntime.checkCancelled(cancellation);
                    cursor = scanner.open(scan, -1L, -1L, -1L, -1L);
                    seen = new KernelRuntime.LongHashSet();
                    buffer = new long[KernelRuntime.SCAN_BATCH_ROWS * 4];
                }
                while (true) {
                    if (position == count * 2) {
                        KernelRuntime.checkCancelled(cancellation);
                        count = cursor.fill(buffer, KernelRuntime.SCAN_BATCH_ROWS);
                        KernelRuntime.checkCancelled(cancellation);
                        if (count < 0 || count > KernelRuntime.SCAN_BATCH_ROWS)
                            throw new IllegalStateException("quad cursor exceeded window bounds");
                        position = 0;
                        if (count == 0) { close(); return false; }
                    }
                    poll();
                    int at = position++;
                    long id = buffer[(at >>> 1) * 4 + ((at & 1) == 0 ? 0 : 2)];
                    if (id != -1L && seen.add(id)) { value = id; return true; }
                }
            } catch (RuntimeException | Error failure) {
                KernelRuntime.closeCursor(this, failure); throw failure;
            }
        }
        @Override public void close() {
            super.close(); KernelQuadCursor owned = cursor; cursor = null;
            buffer = null; seen = null; scanner = null;
            if (owned != null) KernelRuntime.closeScanCursor(owned, null);
        }
    }

    /**
     * Lazy p* / p+ reachability. A node is queued at most once, so duplicate edges do not make the pending stack
     * proportional to edge count. The start of p* is returned before any adjacency lookup. This is reachability,
     * not path-counting; graph-context restrictions apply to every traversed edge.
     */
    public static final class Path extends Values {
        private NativeAdjacency adjacency;
        private KernelRuntime.LongHashSet emitted = new KernelRuntime.LongHashSet();
        private KernelRuntime.LongHashSet queued = new KernelRuntime.LongHashSet();
        private long[] stack = new long[16];
        private long[] contexts;
        private int top;
        private long handle, position, end;
        private boolean initial;
        public Path(NativeAdjacency adjacency, long start, int minHops, long[] contexts,
                KernelCancellation cancellation) {
            super(cancellation);
            if (minHops != 0 && minHops != 1) throw new IllegalArgumentException("only p* / p+ is supported");
            this.adjacency = Objects.requireNonNull(adjacency);
            this.contexts = Objects.requireNonNull(contexts).clone();
            if (start == -1L) { done = true; return; }
            stack[top++] = start; queued.add(start);
            if (minHops == 0) { initial = true; value = start; emitted.add(start); }
        }
        @Override public boolean next() {
            if (done) return false;
            KernelRuntime.checkCancelled(cancellation);
            if (initial) { initial = false; return true; }
            while (true) {
                while (position < end) {
                    poll(); long at = position++;
                    if (contexts.length != 0) {
                        long context = adjacency.contextAt(handle, at); boolean accepted = false;
                        for (long allowed : contexts) if (allowed == context) { accepted = true; break; }
                        if (!accepted) continue;
                    }
                    long neighbor = adjacency.neighborAt(handle, at);
                    if (queued.add(neighbor)) {
                        if (top == stack.length) stack = Arrays.copyOf(stack, Math.multiplyExact(top, 2));
                        stack[top++] = neighbor;
                    }
                    if (emitted.add(neighbor)) { value = neighbor; return true; }
                }
                if (top == 0) { close(); return false; }
                poll(); long node = stack[--top];
                handle = adjacency.find(node); position = 0;
                end = handle > 0L ? adjacency.size(handle) : 0L;
            }
        }
        @Override public void close() {
            super.close(); adjacency = null; emitted = null; queued = null; stack = contexts = null;
        }
    }

    /**
     * Sorted unsigned multiset intersection. Equal runs remain factorized as duplicate ranges. Row replay uses a
     * mixed-radix counter, so the first LIMIT result does not require a potentially overflowing product. Consumers
     * needing an exact signed-long group count can explicitly request {@link #groupMultiplicity()} instead.
     */
    public static final class Intersection extends Values {
        private NativeAdjacency[] views;
        private long[] handles, positions, sizes, duplicates, digits, keys;
        private boolean group;
        public Intersection(NativeAdjacency[] views, KernelCancellation cancellation) {
            super(cancellation);
            if (views.length < 1) throw new IllegalArgumentException("intersection arity");
            this.views = views.clone();
            int n = views.length;
            handles = new long[n]; positions = new long[n]; sizes = new long[n]; duplicates = new long[n]; digits = new long[n]; keys = new long[n];
            for (NativeAdjacency view : this.views) Objects.requireNonNull(view);
            done = true;
        }
        public Intersection(NativeAdjacency[] views, long[] keys, KernelCancellation cancellation) {
            this(views, cancellation);
            bind(keys);
        }
        /** Set one input key, then call bind(). For generated fixed-arity code this needs no temporary key array. */
        public void key(int index, long key) {
            if (views == null) throw new IllegalStateException("closed intersection");
            keys[index] = key;
        }
        public void bind(long[] sourceKeys) {
            if (views == null) throw new IllegalStateException("closed intersection");
            if (sourceKeys.length != keys.length) throw new IllegalArgumentException("intersection arity");
            System.arraycopy(sourceKeys, 0, keys, 0, keys.length);
            bind();
        }
        /** Rebind one non-reentrant operator activation, retaining only its reusable primitive scratch arrays. */
        public void bind() {
            if (views == null) throw new IllegalStateException("closed intersection");
            done = true; group = false; tick = 0;
            KernelRuntime.checkCancelled(cancellation);
            for (int i=0; i<views.length; i++) {
                positions[i] = 0;
                if (keys[i] == -1L) return;
                handles[i] = views[i].find(keys[i]);
                if (handles[i] <= 0L) return;
                sizes[i] = views[i].size(handles[i]);
                if (sizes[i] == 0L) return;
                if (sizes[i] < 0L) throw new IllegalStateException("negative adjacency run size");
            }
            done = false;
        }
        @Override public boolean next() {
            if (done) return false;
            poll();
            if (group) {
                for (int i=digits.length-1; i>=0; i--) {
                    if (digits[i] < duplicates[i]-1) { digits[i]++; return true; }
                    digits[i]=0;
                }
                group = false;
            }
            return nextGroup();
        }
        /** Advances past the previous duplicate group without enumerating its remaining combinations. */
        public boolean nextGroup() {
            if (done) return false;
            KernelRuntime.checkCancelled(cancellation);
            group = false;
            while (true) {
                poll(); long maximum = 0; boolean first = true, equal = true;
                for (int i=0; i<views.length; i++) {
                    if (positions[i] >= sizes[i]) { done = true; return false; }
                    long current = views[i].neighborAt(handles[i], positions[i]);
                    if (first) { maximum=current; first=false; }
                    else if (current != maximum) { equal=false; if (Long.compareUnsigned(current, maximum)>0) maximum=current; }
                }
                if (equal) {
                    value = maximum;
                    for (int i=0; i<views.length; i++) {
                        long start = positions[i], limit = upperBound(i, start, maximum);
                        duplicates[i] = limit-start; positions[i]=limit; digits[i]=0;
                    }
                    group = true; return true;
                }
                for (int i=0; i<views.length; i++) positions[i] = lowerBound(i, positions[i], maximum);
            }
        }
        public long groupMultiplicity() {
            if (!group || done) throw new IllegalStateException("no positioned group");
            long count=1;
            for (long duplicatesInRun : duplicates) count=Math.multiplyExact(count, duplicatesInRun);
            return count;
        }
        private long lowerBound(int i, long from, long target) { return seek(i, from, target, false); }
        // The equality frontier already read and proved the first member. Do not load/decode it twice.
        private long upperBound(int i, long from, long target) { return seek(i, from + 1L, target, true); }
        // Exponential positioning followed by binary search. All increments are bounded by remaining run length,
        // including runs near Long.MAX_VALUE. There is no signed doubling/addition overflow in galloping.
        private long seek(int i, long from, long target, boolean upper) {
            long end = sizes[i];
            if (from == end || !before(i, from, target, upper)) return from;
            long low=from+1, high=low, step=1;
            while (high < end && before(i, high, target, upper)) {
                poll(); low=high+1;
                long remaining=end-high;
                step=step>=remaining-step ? remaining : step+step;
                high+=step;
            }
            while (low < high) {
                poll(); long mid=low+((high-low)>>>1);
                if (before(i,mid,target,upper)) low=mid+1; else high=mid;
            }
            return low;
        }
        private boolean before(int i,long at,long target,boolean upper) {
            int comparison=Long.compareUnsigned(views[i].neighborAt(handles[i],at),target);
            return upper ? comparison<=0 : comparison<0;
        }
        @Override public void close() {
            super.close(); group=false; views=null; handles=positions=sizes=duplicates=digits=keys=null;
        }
    }
}
