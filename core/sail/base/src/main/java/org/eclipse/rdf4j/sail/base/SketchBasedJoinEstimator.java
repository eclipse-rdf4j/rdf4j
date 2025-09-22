/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.base;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAdder;

import org.apache.datasketches.theta.AnotB;
import org.apache.datasketches.theta.Intersection;
import org.apache.datasketches.theta.SetOperation;
import org.apache.datasketches.theta.Sketch;
import org.apache.datasketches.theta.Union;
import org.apache.datasketches.theta.UpdateSketch;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sketch‑based selectivity and join‑size estimator for RDF4J.
 *
 * <p>
 * **Changes from the original**<br>
 * – Replaces the <em>Build&nbsp;+&nbsp;Read</em> split with a single mutable {@code State}.<br>
 * – Keeps the original <em>tomb‑stone</em> approach by storing a mirror set of “delete” sketches in every
 * {@code State}.<br>
 * – Double‑buffer publication (bufA / bufB) is retained, so all readers stay lock‑free and wait‑free. Only code that
 * was strictly necessary to achieve those goals has been modified.
 * </p>
 *
 * <ul>
 * <li>Θ‑Sketches over S, P, O, C singles and all six pairs.</li>
 * <li>Lock‑free reads; double‑buffered rebuilds.</li>
 * <li>Incremental {@code addStatement} / {@code deleteStatement} with tombstone sketches and A‑NOT‑B subtraction.</li>
 * </ul>
 */
public class SketchBasedJoinEstimator {

	/* ────────────────────────────────────────────────────────────── */
	/* Logging */
	/* ────────────────────────────────────────────────────────────── */

	private static final Logger logger = LoggerFactory.getLogger(SketchBasedJoinEstimator.class);

	/* ────────────────────────────────────────────────────────────── */
	/* Public enums */
	/* ────────────────────────────────────────────────────────────── */

	public enum Component {
		S,
		P,
		O,
		C
	}

	public enum Pair {
		SP(Component.S, Component.P, Component.O, Component.C),
		SO(Component.S, Component.O, Component.P, Component.C),
		SC(Component.S, Component.C, Component.P, Component.O),
		PO(Component.P, Component.O, Component.S, Component.C),
		PC(Component.P, Component.C, Component.S, Component.O),
		OC(Component.O, Component.C, Component.S, Component.P);

		public final Component x, y, comp1, comp2;

		Pair(Component x, Component y, Component c1, Component c2) {
			this.x = x;
			this.y = y;
			this.comp1 = c1;
			this.comp2 = c2;
		}
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Configuration & high‑level state */
	/* ────────────────────────────────────────────────────────────── */

	private final SailStore sailStore;
	private final int nominalEntries; // ← bucket count for array indices
	private final long throttleEveryN;
	private final long throttleMillis;

	/** Two interchangeable buffers; one of them is always the current snapshot. */
	private final State bufA, bufB;
	/** `current` is published to readers via a single volatile store. */
	private volatile State current;

	/** Which buffer will receive the next rebuild. */
	private volatile boolean usingA = true;

	private volatile boolean running;
	private Thread refresher;

	private long seenTriples = 0L;

	private static final Sketch EMPTY = UpdateSketch.builder().build().compact();

	// ──────────────────────────────────────────────────────────────
	// Staleness tracking (global, lock‑free reads)
	// ──────────────────────────────────────────────────────────────
	private volatile long lastRebuildStartMs = System.currentTimeMillis();
	private volatile long lastRebuildPublishMs = 0L;
	private final LongAdder addsSinceRebuild = new LongAdder();
	private final LongAdder deletesSinceRebuild = new LongAdder();

	/* ────────────────────────────────────────────────────────────── */
	/* Construction */
	/* ────────────────────────────────────────────────────────────── */

	public SketchBasedJoinEstimator(SailStore sailStore, int nominalEntries,
			long throttleEveryN, long throttleMillis) {
		nominalEntries *= 2;

//		System.out.println("RdfJoinEstimator: Using nominalEntries = " + nominalEntries +
//				", throttleEveryN = " + throttleEveryN + ", throttleMillis = " + throttleMillis);

		this.sailStore = sailStore;
		this.nominalEntries = nominalEntries; // used for array bucket count
		this.throttleEveryN = throttleEveryN;
		this.throttleMillis = throttleMillis;

		// k for DataSketches is larger than bucket count; keep original multiplier
		this.bufA = new State(nominalEntries * 8, this.nominalEntries);
		this.bufB = new State(nominalEntries * 8, this.nominalEntries);
		this.current = usingA ? bufA : bufB; // start with an empty snapshot
	}

	/* Suggest k (=nominalEntries) so the estimator stays ≤ heap/16. */
	public static int suggestNominalEntries() {
		final long heap = Runtime.getRuntime().maxMemory(); // what -Xmx resolved to

		final long budget = heap >>> 4; // 1/16th of heap
		final long budgetMB = budget / 1024 / 1024;
//		System.out.println("RdfJoinEstimator: Suggesting nominalEntries for budget = " + budgetMB + " MB.");
		if (budgetMB <= (8 * 1024)) {
			if (budgetMB > 4096) {
				return 2048;
			} else if (budgetMB > 2048) {
				return 1024;
			} else if (budgetMB > 1024) {
				return 512;
			} else if (budgetMB > 512) {
				return 256;
			} else if (budgetMB > 256) {
				return 128;
			} else if (budgetMB > 128) {
				return 64;
			} else if (budgetMB > 64) {
				return 32;
			} else if (budgetMB > 32) {
				return 16;
			} else if (budgetMB > 16) {
				return 8;
			}
		}
		final double PAIR_FILL = 0.01; // empirical default

		int k = 4;
		while (true) {
			long singles = 16L * k; // 4 + 12
			long pairs = (long) (18L * PAIR_FILL * k * k); // triples + cmpl
			long bytesPerSketch = Sketch.getMaxUpdateSketchBytes(k * 8) / 4;

			long projected = (singles + pairs) * bytesPerSketch;
//			System.out.println("RdfJoinEstimator: Suggesting nominalEntries = " + k +
//					", projected memory usage = " + projected / 1024 / 1024 + " MB, budget = " + budget / 1024 / 1024
//					+ " MB.");

			if (projected > budget || k >= (1 << 22)) { // cap at 4 M entries (256 MB/sketch!)
				return k >>> 1; // previous k still fitted
			}
			k <<= 1; // next power‑of‑two
		}
	}

	/* --------------------------------------------------------------------- */

	public boolean isReady() {
		return seenTriples > 0;
	}

	public void startBackgroundRefresh(int stalenessThreshold) {
		if (running) {
			return;
		}
		running = true;

		refresher = new Thread(() -> {
			while (running) {
				boolean stale = isStale(stalenessThreshold);
				if (!stale && seenTriples > 0) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
					continue;
				}
				Staleness staleness = staleness();
//				System.out.println(staleness.toString());

				try {
					rebuildOnceSlow();
				} catch (Throwable t) {
					logger.error("Error while rebuilding join estimator", t);
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}

				logger.debug("RdfJoinEstimator: Rebuilt join estimator.");
			}
		}, "RdfJoinEstimator-Refresh");

		refresher.setDaemon(true);
		refresher.start();
	}

	public void stop() {
		running = false;
		if (refresher != null) {
			refresher.interrupt();
			try {
				refresher.join(TimeUnit.SECONDS.toMillis(5));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Rebuild */
	/* ────────────────────────────────────────────────────────────── */

	/**
	 * Rebuild the inactive buffer from scratch (blocking). <br>
	 * Readers stay lock‑free; once complete a single volatile store publishes the fresh {@code State}.
	 *
	 * @return number of statements scanned.
	 */
	public synchronized long rebuildOnceSlow() {

//		long currentMemoryUsage = currentMemoryUsage();

		boolean rebuildIntoA = !usingA; // remember before toggling

		State tgt = rebuildIntoA ? bufA : bufB;
		tgt.clear(); // wipe everything (add + del)

		long seen = 0L;
		long l = System.currentTimeMillis();

		// staleness: record rebuild start
		lastRebuildStartMs = l;

		try (SailDataset ds = sailStore.getExplicitSailSource().dataset(IsolationLevels.SERIALIZABLE);
				CloseableIteration<? extends Statement> it = ds.getStatements(null, null, null)) {

			while (it.hasNext()) {
				Statement st = it.next();
				synchronized (tgt) {
					ingest(tgt, st, /* isDelete= */false);
				}

				if (++seen % throttleEveryN == 0 && throttleMillis > 0) {
					try {
						Thread.sleep(throttleMillis);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}

//				if (seen % 100000 == 0) {
//					System.out.println("RdfJoinEstimator: Rebuilding " + (rebuildIntoA ? "bufA" : "bufB") + ", seen "
//							+ seen + " triples so far. Elapsed: " + (System.currentTimeMillis() - l) / 1000 + " s.");
//				}
			}
		}

		current = tgt; // single volatile write → visible to all readers
		seenTriples = seen;
		usingA = !usingA;

//		long currentMemoryUsageAfter = currentMemoryUsage();
//		System.out.println("RdfJoinEstimator: Rebuilt " + (rebuildIntoA ? "bufA" : "bufB") +
//				", seen " + seen + " triples, memory usage: " +
//				currentMemoryUsageAfter / 1024 / 1024 + " MB, delta = " +
//				(currentMemoryUsageAfter - currentMemoryUsage) / 1024 / 1024 + " MB.");

		// staleness: publish times & reset deltas
		lastRebuildPublishMs = System.currentTimeMillis();
		addsSinceRebuild.reset();
		deletesSinceRebuild.reset();

		return seen;
	}

	private long currentMemoryUsage() {
		System.gc();
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		System.gc();
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		System.gc();
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Incremental updates */
	/* ────────────────────────────────────────────────────────────── */

	public void addStatement(Statement st) {
		Objects.requireNonNull(st);

		synchronized (bufA) {
			ingest(bufA, st, /* isDelete= */false);
		}
		synchronized (bufB) {
			ingest(bufB, st, /* isDelete= */false);
		}

		// staleness: track deltas
		addsSinceRebuild.increment();
	}

	public void addStatement(Resource s, IRI p, Value o, Resource c) {
		addStatement(sailStore.getValueFactory().createStatement(s, p, o, c));
	}

	public void addStatement(Resource s, IRI p, Value o) {
		addStatement(s, p, o, null);
	}

	public void deleteStatement(Statement st) {
		Objects.requireNonNull(st);

		synchronized (bufA) {
			ingest(bufA, st, /* isDelete= */true);
		}
		synchronized (bufB) {
			ingest(bufB, st, /* isDelete= */true);
		}

		// staleness: track deltas
		deletesSinceRebuild.increment();
	}

	public void deleteStatement(Resource s, IRI p, Value o, Resource c) {
		deleteStatement(sailStore.getValueFactory().createStatement(s, p, o, c));
	}

	public void deleteStatement(Resource s, IRI p, Value o) {
		deleteStatement(s, p, o, null);
	}

	/* ------------------------------------------------------------------ */

	/**
	 * Common ingestion path for both add and delete operations.
	 *
	 * @param t        target {@code State} (one of the two buffers)
	 * @param st       statement to ingest
	 * @param isDelete {@code false}=live sketch, {@code true}=tomb‑stone sketch
	 */
	private void ingest(State t, Statement st, boolean isDelete) {
		try {
			String s = str(st.getSubject());
			String p = str(st.getPredicate());
			String o = str(st.getObject());
			String c = str(st.getContext());

			int si = hash(s), pi = hash(p), oi = hash(o), ci = hash(c);
			String sig = sig(s, p, o, c);

			/* Select the correct target maps depending on add / delete. */
			var tgtST = isDelete ? t.delSingleTriples : t.singleTriples;
			var tgtS = isDelete ? t.delSingles : t.singles;
			var tgtP = isDelete ? t.delPairs : t.pairs;

			/* single‑component cardinalities (array-backed) */
			updateCell(tgtST.get(Component.S), si, sig, t.k);
			updateCell(tgtST.get(Component.P), pi, sig, t.k);
			updateCell(tgtST.get(Component.O), oi, sig, t.k);
			updateCell(tgtST.get(Component.C), ci, sig, t.k);

			/* ★ churn: record incremental adds since rebuild (S bucket only) */
			if (!isDelete) {
				updateCell(t.incAddSingleTriples.get(Component.S), si, sig, t.k);
			}

			/* complement sets for singles (array-backed second layer) */
			tgtS.get(Component.S).upd(Component.P, si, p);
			tgtS.get(Component.S).upd(Component.O, si, o);
			tgtS.get(Component.S).upd(Component.C, si, c);

			tgtS.get(Component.P).upd(Component.S, pi, s);
			tgtS.get(Component.P).upd(Component.O, pi, o);
			tgtS.get(Component.P).upd(Component.C, pi, c);

			tgtS.get(Component.O).upd(Component.S, oi, s);
			tgtS.get(Component.O).upd(Component.P, oi, p);
			tgtS.get(Component.O).upd(Component.C, oi, c);

			tgtS.get(Component.C).upd(Component.S, ci, s);
			tgtS.get(Component.C).upd(Component.P, ci, p);
			tgtS.get(Component.C).upd(Component.O, ci, o);

			/* pairs (triples + complements) — row-chunked arrays */
			tgtP.get(Pair.SP).upT(pairKey(si, pi), sig);
			tgtP.get(Pair.SP).up1(pairKey(si, pi), o);
			tgtP.get(Pair.SP).up2(pairKey(si, pi), c);

			tgtP.get(Pair.SO).upT(pairKey(si, oi), sig);
			tgtP.get(Pair.SO).up1(pairKey(si, oi), p);
			tgtP.get(Pair.SO).up2(pairKey(si, oi), c);

			tgtP.get(Pair.SC).upT(pairKey(si, ci), sig);
			tgtP.get(Pair.SC).up1(pairKey(si, ci), p);
			tgtP.get(Pair.SC).up2(pairKey(si, ci), o);

			tgtP.get(Pair.PO).upT(pairKey(pi, oi), sig);
			tgtP.get(Pair.PO).up1(pairKey(pi, oi), s);
			tgtP.get(Pair.PO).up2(pairKey(pi, oi), c);

			tgtP.get(Pair.PC).upT(pairKey(pi, ci), sig);
			tgtP.get(Pair.PC).up1(pairKey(pi, ci), s);
			tgtP.get(Pair.PC).up2(pairKey(pi, ci), o);

			tgtP.get(Pair.OC).upT(pairKey(oi, ci), sig);
			tgtP.get(Pair.OC).up1(pairKey(oi, ci), s);
			tgtP.get(Pair.OC).up2(pairKey(oi, ci), p);
		} catch (NullPointerException npe) {
			// ignore NPEs from null values (e.g. missing context)
		}
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Quick cardinalities (public) */
	/* ────────────────────────────────────────────────────────────── */

	public double cardinalitySingle(Component c, String v) {
		int idx = hash(v);
		AtomicReferenceArray<UpdateSketch> arrAdd = current.singleTriples.get(c);
		AtomicReferenceArray<UpdateSketch> arrDel = current.delSingleTriples.get(c);
		UpdateSketch add = arrAdd.get(idx);
		UpdateSketch del = arrDel.get(idx);
		return estimateMinus(add, del);
	}

	public double cardinalityPair(Pair p, String x, String y) {
		long key = pairKey(hash(x), hash(y));
		UpdateSketch add = current.pairs.get(p).getTriple(key);
		UpdateSketch del = current.delPairs.get(p).getTriple(key);
		return estimateMinus(add, del);
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Legacy join helpers (unchanged external API) */
	/* ────────────────────────────────────────────────────────────── */

	public double estimateJoinOn(Component join, Pair a, String ax, String ay,
			Pair b, String bx, String by) {
		return joinPairs(current, join, a, ax, ay, b, bx, by);
	}

	public double estimateJoinOn(Component j, Component a, String av,
			Component b, String bv) {
		return joinSingles(current, j, a, av, b, bv);
	}

	/* ────────────────────────────────────────────────────────────── */
	/* ✦ Fluent BGP builder ✦ */
	/* ────────────────────────────────────────────────────────────── */

	public JoinEstimate estimate(Component joinVar, String s, String p, String o, String c) {
		State snap = current;
		PatternStats st = statsOf(snap, joinVar, s, p, o, c);
		Sketch bindings = st.sketch == null ? EMPTY : st.sketch;
		return new JoinEstimate(snap, joinVar, bindings, bindings.getEstimate(), st.card);
	}

	public double estimateCount(Component joinVar, String s, String p, String o, String c) {
		return estimate(joinVar, s, p, o, c).estimate();
	}

	public final class JoinEstimate {
		private final State snap;
		private Component joinVar;
		private Sketch bindings;
		private double distinct;
		private double resultSize;

		private JoinEstimate(State snap, Component joinVar, Sketch bindings,
				double distinct, double size) {
			this.snap = snap;
			this.joinVar = joinVar;
			this.bindings = bindings;
			this.distinct = distinct;
			this.resultSize = size;
		}

		public JoinEstimate join(Component newJoinVar, String s, String p, String o, String c) {
			/* stats of the right‑hand relation */
			PatternStats rhs = statsOf(snap, newJoinVar, s, p, o, c);

			/* intersection of bindings */
			Intersection ix = SetOperation.builder().buildIntersection();
			ix.intersect(this.bindings);
			if (rhs.sketch != null) {
				ix.intersect(rhs.sketch);
			}
			Sketch inter = ix.getResult();
			double interDistinct = inter.getEstimate();

			if (interDistinct == 0.0) { // early out
				this.bindings = inter;
				this.distinct = 0.0;
				this.resultSize = 0.0;
				this.joinVar = newJoinVar;
				return this;
			}

			/* average fan‑outs */
			double leftAvg = Math.max(0.001, distinct == 0 ? 0 : resultSize / distinct);
			double rightAvg = Math.max(0.001, rhs.distinct == 0 ? 0 : rhs.card / rhs.distinct);

			/* join‑size estimate */
			double newSize = interDistinct * leftAvg * rightAvg;

			/* round to nearest whole solution count (optional) */
			this.resultSize = Math.round(newSize);

			/* carry forward */
			this.bindings = inter;
			this.distinct = interDistinct;
			this.joinVar = newJoinVar;
			return this;
		}

		/** Estimated number of solutions produced so far. */
		public double estimate() {
			return resultSize;
		}

		public double size() {
			return resultSize;
		}

		public double count() {
			return resultSize;
		}
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Pattern statistics */
	/* ────────────────────────────────────────────────────────────── */

	private static final class PatternStats {
		final Sketch sketch; // Θ‑sketch of join‑var bindings
		final double distinct; // = sketch.getEstimate()
		final double card; // relation size |R|

		PatternStats(Sketch s, double card) {
			this.sketch = s;
			this.distinct = s == null ? 0.0 : s.getEstimate();
			this.card = card;
		}
	}

	/** Build both |R| and Θ‑sketch for one triple pattern. */
	private PatternStats statsOf(State st, Component j,
			String s, String p, String o, String c) {

		Sketch sk = bindingsSketch(st, j, s, p, o, c);

		/* ------------- relation cardinality --------------------------- */
		EnumMap<Component, String> fixed = new EnumMap<>(Component.class);
		if (s != null) {
			fixed.put(Component.S, s);
		}
		if (p != null) {
			fixed.put(Component.P, p);
		}
		if (o != null) {
			fixed.put(Component.O, o);
		}
		if (c != null) {
			fixed.put(Component.C, c);
		}

		double card;

		switch (fixed.size()) {
		case 0:
			card = 0.0;
			break;

		case 1: {
			Map.Entry<Component, String> e = fixed.entrySet().iterator().next();
			card = cardSingle(st, e.getKey(), e.getValue());
			break;
		}

		case 2: {
			Component[] cmp = fixed.keySet().toArray(new Component[0]);
			Pair pr = findPair(cmp[0], cmp[1]);
			if (pr != null) {
				card = cardPair(st, pr, fixed.get(pr.x), fixed.get(pr.y));
			} else { // components not a known pair – conservative min
				double a = cardSingle(st, cmp[0], fixed.get(cmp[0]));
				double b = cardSingle(st, cmp[1], fixed.get(cmp[1]));
				card = Math.min(a, b);
			}
			break;
		}

		default: { // 3 or 4 bound – use smallest single cardinality
			card = Double.POSITIVE_INFINITY;
			for (Map.Entry<Component, String> e : fixed.entrySet()) {
				card = Math.min(card, cardSingle(st, e.getKey(), e.getValue()));
			}
			break;
		}
		}
		return new PatternStats(sk, card);
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Snapshot‑level cardinalities */
	/* ────────────────────────────────────────────────────────────── */

	private double cardSingle(State st, Component c, String val) {
		int idx = hash(val);
		UpdateSketch add = st.singleTriples.get(c).get(idx);
		UpdateSketch del = st.delSingleTriples.get(c).get(idx);
		return estimateMinus(add, del);
	}

	private double cardPair(State st, Pair p, String x, String y) {
		long key = pairKey(hash(x), hash(y));
		UpdateSketch add = st.pairs.get(p).getTriple(key);
		UpdateSketch del = st.delPairs.get(p).getTriple(key);
		return estimateMinus(add, del);
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Sketch helpers */
	/* ────────────────────────────────────────────────────────────── */

	private Sketch bindingsSketch(State st, Component j,
			String s, String p, String o, String c) {

		EnumMap<Component, String> f = new EnumMap<>(Component.class);
		if (s != null) {
			f.put(Component.S, s);
		}
		if (p != null) {
			f.put(Component.P, p);
		}
		if (o != null) {
			f.put(Component.O, o);
		}
		if (c != null) {
			f.put(Component.C, c);
		}

		if (f.isEmpty()) {
			return null; // no constant – unsupported
		}

		/* 1 constant → single complement */
		if (f.size() == 1) {
			Map.Entry<Component, String> e = f.entrySet().iterator().next();
			return singleWrapper(st, e.getKey()).getComplementSketch(j, hash(e.getValue()));
		}

		/* 2 constants: pair fast path */
		Component[] cs = f.keySet().toArray(new Component[0]);
		if (f.size() == 2) {
			Pair pr = findPair(cs[0], cs[1]);
			if (pr != null && (j == pr.comp1 || j == pr.comp2)) {
				int idxX = hash(f.get(pr.x));
				int idxY = hash(f.get(pr.y));
				return pairWrapper(st, pr).getComplementSketch(j, pairKey(idxX, idxY));
			}
		}

		/* generic fall‑back */
		Sketch acc = null;
		for (Map.Entry<Component, String> e : f.entrySet()) {
			Sketch sk = singleWrapper(st, e.getKey())
					.getComplementSketch(j, hash(e.getValue()));
			if (sk == null) {
				continue;
			}
			if (acc == null) {
				acc = sk;
			} else {
				Intersection ix = SetOperation.builder().buildIntersection();
				ix.intersect(acc);
				ix.intersect(sk);
				acc = ix.getResult();
			}
		}
		return acc;
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Pair & single wrappers (read‑only) */
	/* ────────────────────────────────────────────────────────────── */

	private StateSingleWrapper singleWrapper(State st, Component fixed) {
		return new StateSingleWrapper(fixed, st.singles.get(fixed), st.delSingles.get(fixed));
	}

	private StatePairWrapper pairWrapper(State st, Pair p) {
		return new StatePairWrapper(p, st.pairs.get(p), st.delPairs.get(p));
	}

	private static final class StateSingleWrapper {
		final Component fixed;
		final SingleBuild add, del;

		StateSingleWrapper(Component f, SingleBuild add, SingleBuild del) {
			this.fixed = f;
			this.add = add;
			this.del = del;
		}

		Sketch getComplementSketch(Component c, int fi) {
			if (c == fixed) {
				return null;
			}
			AtomicReferenceArray<UpdateSketch> arrA = add.cmpl.get(c);
			AtomicReferenceArray<UpdateSketch> arrD = del.cmpl.get(c);
			if (arrA == null || arrD == null) {
				return null;
			}
			UpdateSketch a = arrA.get(fi);
			UpdateSketch d = arrD.get(fi);
			return subtractSketch(a, d);
		}
	}

	private static final class StatePairWrapper {
		final Pair p;
		final PairBuild add, del;

		StatePairWrapper(Pair p, PairBuild add, PairBuild del) {
			this.p = p;
			this.add = add;
			this.del = del;
		}

		Sketch getComplementSketch(Component c, long key) {
			UpdateSketch a, d;
			if (c == p.comp1) {
				a = add.getComp1(key);
				d = del.getComp1(key);
			} else if (c == p.comp2) {
				a = add.getComp2(key);
				d = del.getComp2(key);
			} else {
				return null;
			}
			return subtractSketch(a, d);
		}
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Join primitives */
	/* ────────────────────────────────────────────────────────────── */

	private double joinPairs(State st, Component j,
			Pair a, String ax, String ay,
			Pair b, String bx, String by) {

		long keyA = pairKey(hash(ax), hash(ay));
		long keyB = pairKey(hash(bx), hash(by));

		Sketch sa = pairWrapper(st, a).getComplementSketch(j, keyA);
		Sketch sb = pairWrapper(st, b).getComplementSketch(j, keyB);

		if (sa == null || sb == null) {
			return 0.0;
		}

		Intersection ix = SetOperation.builder().buildIntersection();
		ix.intersect(sa);
		ix.intersect(sb);
		return ix.getResult().getEstimate();
	}

	private double joinSingles(State st, Component j,
			Component a, String av,
			Component b, String bv) {

		int idxA = hash(av), idxB = hash(bv);

		Sketch sa = singleWrapper(st, a).getComplementSketch(j, idxA);
		Sketch sb = singleWrapper(st, b).getComplementSketch(j, idxB);

		if (sa == null || sb == null) {
			return 0.0;
		}

		Intersection ix = SetOperation.builder().buildIntersection();
		ix.intersect(sa);
		ix.intersect(sb);
		return ix.getResult().getEstimate();
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Unified mutable state (add + delete) */
	/* ────────────────────────────────────────────────────────────── */

	private static final class State {
		final int k; // sketch nominal entries
		final int buckets; // array bucket count (outer.nominalEntries)

		/* live (add) sketches */
		final EnumMap<Component, AtomicReferenceArray<UpdateSketch>> singleTriples = new EnumMap<>(
				Component.class);
		final EnumMap<Component, SingleBuild> singles = new EnumMap<>(Component.class);
		final EnumMap<Pair, PairBuild> pairs = new EnumMap<>(Pair.class);

		/* tomb‑stone (delete) sketches */
		final EnumMap<Component, AtomicReferenceArray<UpdateSketch>> delSingleTriples = new EnumMap<>(
				Component.class);
		final EnumMap<Component, SingleBuild> delSingles = new EnumMap<>(Component.class);
		final EnumMap<Pair, PairBuild> delPairs = new EnumMap<>(Pair.class);

		/* ★ incremental‑adds since last rebuild (array‑backed; we only use S in metrics) */
		final EnumMap<Component, AtomicReferenceArray<UpdateSketch>> incAddSingleTriples = new EnumMap<>(
				Component.class);

		State(int k, int buckets) {
			this.k = k;
			this.buckets = buckets;

			for (Component c : Component.values()) {
				singleTriples.put(c, new AtomicReferenceArray<>(buckets));
				delSingleTriples.put(c, new AtomicReferenceArray<>(buckets));
				incAddSingleTriples.put(c, new AtomicReferenceArray<>(buckets));

				singles.put(c, new SingleBuild(k, c, buckets));
				delSingles.put(c, new SingleBuild(k, c, buckets));
			}
			for (Pair p : Pair.values()) {
				pairs.put(p, new PairBuild(k, buckets));
				delPairs.put(p, new PairBuild(k, buckets));
			}
		}

		void clear() {
			singleTriples.values().forEach(SketchBasedJoinEstimator::clearArray);
			delSingleTriples.values().forEach(SketchBasedJoinEstimator::clearArray);
			incAddSingleTriples.values().forEach(SketchBasedJoinEstimator::clearArray); // ★

			singles.values().forEach(SingleBuild::clear);
			delSingles.values().forEach(SingleBuild::clear);

			pairs.values().forEach(PairBuild::clear);
			delPairs.values().forEach(PairBuild::clear);
		}
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Build‑time structures */
	/* ────────────────────────────────────────────────────────────── */

	private static final class SingleBuild {
		final int k;
		final int buckets;
		final EnumMap<Component, AtomicReferenceArray<UpdateSketch>> cmpl = new EnumMap<>(Component.class);

		SingleBuild(int k, Component fixed, int buckets) {
			this.k = k;
			this.buckets = buckets;
			for (Component c : Component.values()) {
				if (c != fixed) {
					cmpl.put(c, new AtomicReferenceArray<>(buckets));
				}
			}
		}

		void clear() {
			for (AtomicReferenceArray<UpdateSketch> arr : cmpl.values()) {
				SketchBasedJoinEstimator.clearArray(arr);
			}
		}

		void upd(Component c, int idx, String v) {
			AtomicReferenceArray<UpdateSketch> arr = cmpl.get(c);
			if (arr == null) {
				return;
			}
			UpdateSketch sk = arr.get(idx);
			if (sk == null) {
				sk = newSk(k);
				arr.set(idx, sk);
			}
			sk.update(v);
		}
	}

	private static final class PairBuild {
		final int k;
		final int buckets;

		/** row-chunked: rows indexed by X; each row has AtomicReferenceArray cells over Y */
		final AtomicReferenceArray<Row> rows;

		PairBuild(int k, int buckets) {
			this.k = k;
			this.buckets = buckets;
			this.rows = new AtomicReferenceArray<>(buckets);
		}

		void clear() {
			for (int i = 0; i < buckets; i++) {
				rows.set(i, null);
			}
		}

		void upT(long key, String sig) {
			int x = (int) (key >>> 32);
			int y = (int) key;
			Row r = getOrCreateRow(x);
			UpdateSketch sk = r.triples.get(y);
			if (sk == null) {
				sk = newSk(k);
				r.triples.set(y, sk);
			}
			sk.update(sig);
		}

		void up1(long key, String v) {
			int x = (int) (key >>> 32);
			int y = (int) key;
			Row r = getOrCreateRow(x);
			UpdateSketch sk = r.comp1.get(y);
			if (sk == null) {
				sk = newSk(k);
				r.comp1.set(y, sk);
			}
			sk.update(v);
		}

		void up2(long key, String v) {
			int x = (int) (key >>> 32);
			int y = (int) key;
			Row r = getOrCreateRow(x);
			UpdateSketch sk = r.comp2.get(y);
			if (sk == null) {
				sk = newSk(k);
				r.comp2.set(y, sk);
			}
			sk.update(v);
		}

		UpdateSketch getTriple(long key) {
			int x = (int) (key >>> 32);
			int y = (int) key;
			Row r = rows.get(x);
			return (r == null) ? null : r.triples.get(y);
		}

		UpdateSketch getComp1(long key) {
			int x = (int) (key >>> 32);
			int y = (int) key;
			Row r = rows.get(x);
			return (r == null) ? null : r.comp1.get(y);
		}

		UpdateSketch getComp2(long key) {
			int x = (int) (key >>> 32);
			int y = (int) key;
			Row r = rows.get(x);
			return (r == null) ? null : r.comp2.get(y);
		}

		private Row getOrCreateRow(int x) {
			Row r = rows.get(x);
			if (r == null) {
				r = new Row(buckets);
				rows.set(x, r);
			}
			return r;
		}

		static final class Row {
			final AtomicReferenceArray<UpdateSketch> triples;
			final AtomicReferenceArray<UpdateSketch> comp1;
			final AtomicReferenceArray<UpdateSketch> comp2;

			Row(int buckets) {
				this.triples = new AtomicReferenceArray<>(buckets);
				this.comp1 = new AtomicReferenceArray<>(buckets);
				this.comp2 = new AtomicReferenceArray<>(buckets);
			}
		}
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Utility */
	/* ────────────────────────────────────────────────────────────── */

	private static double estimateMinus(UpdateSketch add, UpdateSketch del) {
		if (add == null) {
			return 0.0;
		}
		if (del == null || del.getRetainedEntries() == 0) {
			return add.getEstimate();
		}
		AnotB diff = SetOperation.builder().buildANotB();
		diff.setA(add);
		diff.notB(del);
		return diff.getResult(false).getEstimate();
	}

	private static Sketch subtractSketch(UpdateSketch add, UpdateSketch del) {
		if (add == null) {
			return null;
		}
		if (del == null || del.getRetainedEntries() == 0) {
			return add;
		}
		AnotB diff = SetOperation.builder().buildANotB();
		diff.setA(add);
		diff.notB(del);
		return diff.getResult(false);
	}

	private static UpdateSketch newSk(int k) {
		return UpdateSketch.builder().setNominalEntries(k).build();
	}

	private int hash(String v) {
		// Ensure non-negative index in [0, nominalEntries)
		int h = Objects.hashCode(v);
		return (h & 0x7fffffff) % nominalEntries;
	}

	private static long pairKey(int a, int b) {
		return (((long) a) << 32) ^ (b & 0xffffffffL);
	}

	private static Pair findPair(Component a, Component b) {
		for (Pair p : Pair.values()) {
			if ((p.x == a && p.y == b) || (p.x == b && p.y == a)) {
				return p;
			}
		}
		return null;
	}

	private static String str(Resource r) {
		return r == null ? "urn:default-context" : r.stringValue();
	}

	private static String str(Value v) {
		return v == null ? "urn:default-context" : v.stringValue();
	}

	private static String sig(String s, String p, String o, String c) {
		return s + ' ' + p + ' ' + o + ' ' + c;
	}

	/* ────────────────────────────────────────────────────────────── */
	/* OPTIONAL optimiser helper (unchanged API) */
	/* ────────────────────────────────────────────────────────────── */

	public double cardinality(Join node) {
		TupleExpr leftArg = node.getLeftArg();
		TupleExpr rightArg = node.getRightArg();

		if (leftArg instanceof StatementPattern && rightArg instanceof StatementPattern) {
			StatementPattern l = (StatementPattern) leftArg;
			StatementPattern r = (StatementPattern) rightArg;

			/* find first common unbound variable */
			Var common = null;
			List<Var> lVars = l.getVarList();
			for (Var v : r.getVarList()) {
				if (!v.hasValue() && lVars.contains(v)) {
					common = v;
					break;
				}
			}
			if (common == null) {
				return Double.MAX_VALUE; // no common var
			}

			Component lc = getComponent(l, common);
			Component rc = getComponent(r, common);

			return this
					.estimate(lc,
							getIriOrNull(l.getSubjectVar()),
							getIriOrNull(l.getPredicateVar()),
							getIriOrNull(l.getObjectVar()),
							getIriOrNull(l.getContextVar()))
					.join(rc,
							getIriOrNull(r.getSubjectVar()),
							getIriOrNull(r.getPredicateVar()),
							getIriOrNull(r.getObjectVar()),
							getIriOrNull(r.getContextVar()))
					.estimate();
		}
		return -1;
	}

	private String getIriOrNull(Var v) {
		return (v == null || v.getValue() == null || !(v.getValue() instanceof IRI))
				? null
				: v.getValue().stringValue();
	}

	private Component getComponent(StatementPattern sp, Var var) {
		if (var.equals(sp.getSubjectVar())) {
			return Component.S;
		}
		if (var.equals(sp.getPredicateVar())) {
			return Component.P;
		}
		if (var.equals(sp.getObjectVar())) {
			return Component.O;
		}
		if (var.equals(sp.getContextVar())) {
			return Component.C;
		}
		throw new IllegalStateException("Unexpected variable " + var + " in pattern " + sp);
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Staleness API */
	/* ────────────────────────────────────────────────────────────── */

	/**
	 * Immutable staleness snapshot. All values are approximate by design.
	 */
	public static final class Staleness {
		public final long ageMillis; // AoI: time since last publish
		public final long lastRebuildStartMs;
		public final long lastRebuildPublishMs;

		public final long addsSinceRebuild;
		public final long deletesSinceRebuild;
		public final double deltaRatio; // (adds+deletes)/max(1, seenTriples)

		public final double tombstoneLoadSingles; // coarse: sumRetained(delSingles)/sumRetained(addSingles)
		public final double tombstoneLoadPairs; // coarse: sumRetained(delPairs)/sumRetained(addPairs)
		public final double tombstoneLoadComplements;// coarse: from complement maps

		public final double distinctTriples; // union over singleTriples[S]
		public final double distinctDeletes; // union over delSingleTriples[S]
		public final double distinctNetLive; // union of (A-not-B per S-bucket)

		// ★ churn‑specific
		public final double distinctIncAdds; // union over incAddSingleTriples[S]
		public final double readdOverlap; // union of per‑bucket intersections incAdd[S] ∧ del[S]
		public final double readdOverlapOnIncAdds; // ratio readdOverlap / distinctIncAdds

		public final double stalenessScore; // combined 0..1+

		private Staleness(
				long ageMillis,
				long lastRebuildStartMs,
				long lastRebuildPublishMs,
				long addsSinceRebuild,
				long deletesSinceRebuild,
				double deltaRatio,
				double tombstoneLoadSingles,
				double tombstoneLoadPairs,
				double tombstoneLoadComplements,
				double distinctTriples,
				double distinctDeletes,
				double distinctNetLive,
				double distinctIncAdds,
				double readdOverlap,
				double readdOverlapOnIncAdds,
				double stalenessScore) {
			this.ageMillis = ageMillis;
			this.lastRebuildStartMs = lastRebuildStartMs;
			this.lastRebuildPublishMs = lastRebuildPublishMs;
			this.addsSinceRebuild = addsSinceRebuild;
			this.deletesSinceRebuild = deletesSinceRebuild;
			this.deltaRatio = deltaRatio;
			this.tombstoneLoadSingles = tombstoneLoadSingles;
			this.tombstoneLoadPairs = tombstoneLoadPairs;
			this.tombstoneLoadComplements = tombstoneLoadComplements;
			this.distinctTriples = distinctTriples;
			this.distinctDeletes = distinctDeletes;
			this.distinctNetLive = distinctNetLive;
			this.distinctIncAdds = distinctIncAdds;
			this.readdOverlap = readdOverlap;
			this.readdOverlapOnIncAdds = readdOverlapOnIncAdds;
			this.stalenessScore = stalenessScore;
		}

		@Override
		public String toString() {
			return "Staleness{" +
					"ageMillis=" + ageMillis +
					", lastRebuildStartMs=" + lastRebuildStartMs +
					", lastRebuildPublishMs=" + lastRebuildPublishMs +
					", addsSinceRebuild=" + addsSinceRebuild +
					", deletesSinceRebuild=" + deletesSinceRebuild +
					", deltaRatio=" + deltaRatio +
					", tombstoneLoadSingles=" + tombstoneLoadSingles +
					", tombstoneLoadPairs=" + tombstoneLoadPairs +
					", tombstoneLoadComplements=" + tombstoneLoadComplements +
					", distinctTriples=" + distinctTriples +
					", distinctDeletes=" + distinctDeletes +
					", distinctNetLive=" + distinctNetLive +
					", distinctIncAdds=" + distinctIncAdds +
					", readdOverlap=" + readdOverlap +
					", readdOverlapOnIncAdds=" + readdOverlapOnIncAdds +
					", stalenessScore=" + stalenessScore +
					'}';
		}
	}

	/**
	 * Compute a staleness snapshot using the *current* published State. No locks taken.
	 *
	 * This is O(total number of populated sketch keys) and intended for occasional diagnostics or adaptive scheduling.
	 * All numbers are approximate by design of Theta sketches.
	 */
	public Staleness staleness() {
		State snap = current;

		final long now = System.currentTimeMillis();
		final long age = lastRebuildPublishMs == 0L ? Long.MAX_VALUE : (now - lastRebuildPublishMs);

		final long adds = addsSinceRebuild.sum();
		final long dels = deletesSinceRebuild.sum();

		final double base = Math.max(1.0, seenTriples);
		final double deltaRatio = (adds + dels) / base;

		// Coarse tombstone pressure via retained entries (symmetric double-counting)
		long addSinglesRet = sumRetainedEntriesSingles(snap.singleTriples.values());
		long delSinglesRet = sumRetainedEntriesSingles(snap.delSingleTriples.values());
		double tombSingle = safeRatio(delSinglesRet, addSinglesRet);

		long addPairsRet = sumRetainedEntriesPairs(snap.pairs.values());
		long delPairsRet = sumRetainedEntriesPairs(snap.delPairs.values());
		double tombPairs = safeRatio(delPairsRet, addPairsRet);

		long addComplRet = sumRetainedEntriesComplements(snap.singles.values());
		long delComplRet = sumRetainedEntriesComplements(snap.delSingles.values());
		double tombCompl = safeRatio(delComplRet, addComplRet);

		// Distinct-aware: unions across S-buckets
		double distinctAdds = unionDistinctTriplesS(snap.singleTriples.get(Component.S));
		double distinctDels = unionDistinctTriplesS(snap.delSingleTriples.get(Component.S));
		double distinctNet = unionDistinctNetLiveTriplesS(
				snap.singleTriples.get(Component.S),
				snap.delSingleTriples.get(Component.S));

		// ★ Churn: delete→re‑add overlap using incremental‑adds (S bucket only)
		double distinctIncAdds = unionDistinctTriplesS(snap.incAddSingleTriples.get(Component.S));
		double readdOverlap = overlapIncAddVsDelS(
				snap.incAddSingleTriples.get(Component.S),
				snap.delSingleTriples.get(Component.S));
		double readdOverlapOnIncAdds = distinctIncAdds <= 0.0 ? 0.0 : (readdOverlap / distinctIncAdds);

		// Combined score (dimensionless). Emphasize churn risk.
		double ageScore = normalize(age, TimeUnit.MINUTES.toMillis(10)); // 10 min SLA by default
		double deltaScore = clamp(deltaRatio, 0.0, 10.0); // cap to avoid runaway
		double tombScore = (tombSingle + tombPairs + tombCompl) / 3.0;
		double churnScore = clamp(readdOverlapOnIncAdds * 3.0, 0.0, 3.0); // up‑weight churn

		double score = ageScore * 0.20 + deltaScore * 0.20 + tombScore * 0.20 + churnScore * 0.40;

		return new Staleness(
				age,
				lastRebuildStartMs,
				lastRebuildPublishMs,
				adds,
				dels,
				deltaRatio,
				tombSingle,
				tombPairs,
				tombCompl,
				distinctAdds,
				distinctDels,
				distinctNet,
				distinctIncAdds,
				readdOverlap,
				readdOverlapOnIncAdds,
				score);
	}

	/** Convenience: true if combined staleness score exceeds a given threshold. */
	public boolean isStale(double threshold) {
		return staleness().stalenessScore > threshold;
	}

	// ──────────────────────────────────────────────────────────────
	// Staleness helpers (private)
	// ──────────────────────────────────────────────────────────────

	private static long sumRetainedEntriesSingles(Collection<AtomicReferenceArray<UpdateSketch>> arrays) {
		long sum = 0L;
		for (AtomicReferenceArray<UpdateSketch> arr : arrays) {
			if (arr == null) {
				continue;
			}
			for (int i = 0; i < arr.length(); i++) {
				UpdateSketch sk = arr.get(i);
				if (sk != null) {
					sum += sk.getRetainedEntries();
				}
			}
		}
		return sum;
	}

	private static long sumRetainedEntriesPairs(Collection<PairBuild> pbs) {
		long sum = 0L;
		for (PairBuild pb : pbs) {
			if (pb == null) {
				continue;
			}
			for (int x = 0; x < pb.buckets; x++) {
				PairBuild.Row r = pb.rows.get(x);
				if (r == null) {
					continue;
				}
				for (int y = 0; y < pb.buckets; y++) {
					UpdateSketch sk;
					sk = r.triples.get(y);
					if (sk != null) {
						sum += sk.getRetainedEntries();
					}
					sk = r.comp1.get(y);
					if (sk != null) {
						sum += sk.getRetainedEntries();
					}
					sk = r.comp2.get(y);
					if (sk != null) {
						sum += sk.getRetainedEntries();
					}
				}
			}
		}
		return sum;
	}

	private static long sumRetainedEntriesComplements(Collection<SingleBuild> sbs) {
		long sum = 0L;
		for (SingleBuild sb : sbs) {
			for (AtomicReferenceArray<UpdateSketch> arr : sb.cmpl.values()) {
				for (int i = 0; i < arr.length(); i++) {
					UpdateSketch sk = arr.get(i);
					if (sk != null) {
						sum += sk.getRetainedEntries();
					}
				}
			}
		}
		return sum;
	}

	private static double unionDistinctTriplesS(AtomicReferenceArray<UpdateSketch> arr) {
		if (arr == null || arr.length() == 0) {
			return 0.0;
		}
		Union u = SetOperation.builder().buildUnion();
		for (int i = 0; i < arr.length(); i++) {
			UpdateSketch sk = arr.get(i);
			if (sk != null) {
				u.union(sk); // DataSketches 5.x: union(Sketch)
			}
		}
		return u.getResult().getEstimate();
	}

	private static double unionDistinctNetLiveTriplesS(
			AtomicReferenceArray<UpdateSketch> addS,
			AtomicReferenceArray<UpdateSketch> delS) {
		if (addS == null || addS.length() == 0) {
			return 0.0;
		}
		Union u = SetOperation.builder().buildUnion();
		for (int i = 0; i < addS.length(); i++) {
			UpdateSketch a = addS.get(i);
			if (a == null) {
				continue;
			}
			UpdateSketch d = (delS == null || delS.length() <= i) ? null : delS.get(i);
			if (d == null || d.getRetainedEntries() == 0) {
				u.union(a);
			} else {
				AnotB diff = SetOperation.builder().buildANotB();
				diff.setA(a);
				diff.notB(d);
				u.union(diff.getResult(false)); // union A-not-B Sketch
			}
		}
		return u.getResult().getEstimate();
	}

	/** ★ The key churn metric: per‑bucket (incAdd[S] ∧ del[S]) summed via a union of intersections. */
	private static double overlapIncAddVsDelS(
			AtomicReferenceArray<UpdateSketch> incAddS,
			AtomicReferenceArray<UpdateSketch> delS) {
		if (incAddS == null || delS == null) {
			return 0.0;
		}
		Union u = SetOperation.builder().buildUnion();
		int len = Math.min(incAddS.length(), delS.length());
		for (int i = 0; i < len; i++) {
			UpdateSketch ia = incAddS.get(i);
			UpdateSketch d = delS.get(i);
			if (ia == null || d == null) {
				continue;
			}
			Intersection ix = SetOperation.builder().buildIntersection();
			ix.intersect(ia);
			ix.intersect(d);
			Sketch inter = ix.getResult();
			if (inter != null && inter.getRetainedEntries() > 0) {
				u.union(inter);
			}
		}
		return u.getResult().getEstimate();
	}

	private static double safeRatio(long num, long den) {
		if (den <= 0L) {
			return (num == 0L) ? 0.0 : Double.POSITIVE_INFINITY;
		}
		return (double) num / (double) den;
	}

	private static double normalize(long value, long max) {
		if (max <= 0L) {
			return 0.0;
		}
		return clamp((double) value / (double) max, 0.0, Double.POSITIVE_INFINITY);
	}

	private static double clamp(double v, double lo, double hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Array helpers (private) */
	/* ────────────────────────────────────────────────────────────── */

	private static void clearArray(AtomicReferenceArray<?> arr) {
		if (arr == null) {
			return;
		}
		for (int i = 0; i < arr.length(); i++) {
			arr.set(i, null);
		}
	}

	private static void updateCell(AtomicReferenceArray<UpdateSketch> arr, int idx, String value, int k) {
		UpdateSketch sk = arr.get(idx);
		if (sk == null) {
			sk = newSk(k);
			arr.set(idx, sk);
		}
		sk.update(value);
	}
}
