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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.datasketches.theta.AnotB;
import org.apache.datasketches.theta.HashIterator;
import org.apache.datasketches.theta.Intersection;
import org.apache.datasketches.theta.SetOperation;
import org.apache.datasketches.theta.Sketch;
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

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Sketch‑based selectivity and join‑size estimator for RDF4J.
 *
 * <p>
 * Features
 * </p>
 * <ul>
 * <li>Θ‑Sketches over S, P, O, C singles and all six pairs.</li>
 * <li>Lock‑free reads; double‑buffered rebuilds.</li>
 * <li>Incremental {@code addStatement}/ {@code deleteStatement} with tombstone sketches and A‑NOT‑B compaction.</li>
 * </ul>
 */
public class SketchBasedJoinEstimator {

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
	/* Configuration & state */
	/* ────────────────────────────────────────────────────────────── */

	private final int nominalEntries;
	private final long throttleEveryN, throttleMillis;
	private final SailStore sailStore;

	/** Immutable snapshot visible to queries. */
	private volatile ReadState current;

	/** Double buffer of *add* sketches. */
	private final BuildState bufA, bufB;
	/** Double buffer of *delete* (tombstone) sketches. */
	private final BuildState delA, delB;

	/** Which *add* buffer is being rebuilt next. */
	private volatile boolean usingA = true;

	private volatile boolean running;
	private Thread refresher;
	private volatile boolean rebuildRequested;

	private long seenTriples = 0L;

	private static final Sketch EMPTY = UpdateSketch.builder().build().compact();

	/* ────────────────────────────────────────────────────────────── */
	/* Construction */
	/* ────────────────────────────────────────────────────────────── */

	public SketchBasedJoinEstimator(SailStore sailStore, int nominalEntries, long throttleEveryN, long throttleMillis) {
		this.sailStore = sailStore;
		this.nominalEntries = nominalEntries;
		this.throttleEveryN = throttleEveryN;
		this.throttleMillis = throttleMillis;

		this.bufA = new BuildState(nominalEntries * 8);
		this.bufB = new BuildState(nominalEntries * 8);
		this.delA = new BuildState(nominalEntries * 8);
		this.delB = new BuildState(nominalEntries * 8);

		this.current = new ReadState(); // empty snapshot
	}

	/* Suggest k (=nominalEntries) so the estimator stays ≤ heap/16. */
	public static int suggestNominalEntries() {
		final long heap = Runtime.getRuntime().maxMemory(); // what -Xmx resolved to

		final long budget = heap >>> 4; // 1/16th of heap
		final double PAIR_FILL = 0.01; // empirical default
		long bytesPerSketch = Sketch.getMaxUpdateSketchBytes(4096);

		int k = 4;
		while (true) {
			long singles = 16L * k; // 4 + 12
			long pairs = (long) (18L * PAIR_FILL * k * k); // triples + cmpl
			long projected = (singles + pairs) * bytesPerSketch;
//			System.out.println("RdfJoinEstimator: Suggesting nominalEntries = " + k +
//					", projected memory usage = " + projected/1024/1024 + " MB, budget = " + budget/1024/1024 + " MB.");

			if (projected > budget || k >= (1 << 22)) { // cap at 4 M entries (256 MB/sketch!)
				return k >>> 1; // previous k still fitted
			}
			k <<= 1; // next power‑of‑two
		}
	}

	public boolean isReady() {
		return seenTriples > 0;
	}

	public void requestRebuild() {
		rebuildRequested = true;
	}

	public void startBackgroundRefresh(long periodMs) {
		if (running) {
			return;
		}
		running = true;
		refresher = new Thread(() -> {
			while (running) {
				if (!rebuildRequested) {
					try {
						Thread.sleep(periodMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
					continue;
				}

				try {
					rebuildOnceSlow();
					rebuildRequested = false; // reset
				} catch (Throwable t) {
					t.printStackTrace();
				}

				try {
					Thread.sleep(periodMs);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}

				System.out.println("RdfJoinEstimator: Rebuilt join estimator.");
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

	/**
	 * Rebuild sketches from scratch (blocking). Still lock‑free for readers.
	 *
	 * @return number of statements scanned
	 */
	public long rebuildOnceSlow() {
		boolean usingA = this.usingA; // which buffer to use for adds
		this.usingA = !usingA; // toggle for next rebuild

		BuildState tgtAdd = usingA ? bufA : bufB;
		BuildState tgtDel = usingA ? delA : delB;

		tgtAdd.clear();

		long seen = 0L;

		try (SailDataset ds = sailStore.getExplicitSailSource().dataset(IsolationLevels.READ_UNCOMMITTED);
				CloseableIteration<? extends Statement> it = ds.getStatements(null, null, null)) {

			while (it.hasNext()) {
				Statement st = it.next();
				synchronized (tgtAdd) {
					add(tgtAdd, st);
				}
				if (++seen % throttleEveryN == 0 && throttleMillis > 0) {
					try {
						Thread.sleep(throttleMillis);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}

		/* Compact with deletes – still under the same locks */
		ReadState snap;
		synchronized (tgtAdd) {
			synchronized (tgtDel) {
				snap = tgtAdd.compactWithDeletes(tgtDel);
			}
		}
		current = snap; // publish immutable snapshot

		synchronized (tgtAdd) {
			tgtAdd.clear();
		}
		synchronized (tgtDel) {
			tgtDel.clear();
		}

		this.seenTriples = seen;
		return seen;
	}

	/* Helper: merge src into dst & clear src */
	/*
	 * • Copies buckets that do not yet exist in dst. * • If a bucket exists in both, raw hashes from src are injected *
	 * into dst via UpdateSketch.update(long). * • Finally, src.clear() is called while still holding its lock * so no
	 * concurrent inserts are lost.
	 */
	/* ────────────────────────────────────────────────────────────── */
	private static void mergeBuildState(BuildState dst, BuildState src) {
		synchronized (dst) {
			synchronized (src) {

				/* -------- singles – triple sketches ---------- */
				for (Component cmp : Component.values()) {
					var dstMap = dst.singleTriples.get(cmp);
					src.singleTriples.get(cmp)
							.forEach(
									(idx, skSrc) -> dstMap.merge(idx, skSrc, (skDst, s) -> {
										absorbSketch(skDst, s);
										return skDst;
									}));
				}

				/* -------- singles – complement sketches ------ */
				for (Component fixed : Component.values()) {
					var dstSingle = dst.singles.get(fixed);
					var srcSingle = src.singles.get(fixed);

					for (Component cmp : Component.values()) {
						if (cmp == fixed) {
							continue; // skip non‑existing complement
						}
						var dstMap = dstSingle.cmpl.get(cmp);
						var srcMap = srcSingle.cmpl.get(cmp);
						srcMap.forEach(
								(idx, skSrc) -> dstMap.merge(idx, skSrc, (skDst, s) -> {
									absorbSketch(skDst, s);
									return skDst;
								}));
					}
				}

				/* -------- pairs (triples + complements) ------ */
				for (Pair p : Pair.values()) {
					var dPair = dst.pairs.get(p);
					var sPair = src.pairs.get(p);

					sPair.triples.forEach((k, skSrc) -> dPair.triples.merge(k, skSrc, (skDst, s) -> {
						absorbSketch(skDst, s);
						return skDst;
					}));
					sPair.comp1.forEach((k, skSrc) -> dPair.comp1.merge(k, skSrc, (skDst, s) -> {
						absorbSketch(skDst, s);
						return skDst;
					}));
					sPair.comp2.forEach((k, skSrc) -> dPair.comp2.merge(k, skSrc, (skDst, s) -> {
						absorbSketch(skDst, s);
						return skDst;
					}));
				}

				/* -------- reset src for next cycle ------------ */
				src.clear(); // safe: still under src’s lock
			}
		}
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Inject every retained hash of src into UpdateSketch dst */
	/* ────────────────────────────────────────────────────────────── */
	private static void absorbSketch(UpdateSketch dst, Sketch src) {
		if (src == null || src.getRetainedEntries() == 0) {
			return;
		}
		HashIterator it = src.iterator();
		while (it.next()) {
			dst.update(it.get());
		}
	}
	/* ────────────────────────────────────────────────────────────── */
	/* Incremental updates */
	/* ────────────────────────────────────────────────────────────── */

	public void addStatement(Statement st) {
		Objects.requireNonNull(st);
		synchronized (bufA) {
			add(bufA, st);
		}
		synchronized (bufB) {
			add(bufB, st);
		}
		requestRebuild();
	}

	public void addStatement(Resource s, IRI p, Value o, Resource c) {
		addStatement(sailStore.getValueFactory().createStatement(s, p, o, c));
	}

	public void addStatement(Resource s, IRI p, Value o) {
		addStatement(s, p, o, null);
	}

	public void deleteStatement(Statement st) {
		Objects.requireNonNull(st);
		synchronized (delA) {
			add(delA, st);
		}
		synchronized (delB) {
			add(delB, st);
		}
//		requestRebuild();
	}

	public void deleteStatement(Resource s, IRI p, Value o, Resource c) {
		deleteStatement(sailStore.getValueFactory().createStatement(s, p, o, c));
	}

	public void deleteStatement(Resource s, IRI p, Value o) {
		deleteStatement(s, p, o, null);
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Ingestion into BuildState */
	/* ────────────────────────────────────────────────────────────── */

	private void add(BuildState t, Statement st) {
		String s = str(st.getSubject());
		String p = str(st.getPredicate());
		String o = str(st.getObject());
		String c = str(st.getContext());

		int si = hash(s), pi = hash(p), oi = hash(o), ci = hash(c);

		String sig = sig(s, p, o, c);

		/* single‑component cardinalities */
		t.upSingle(Component.S, si, sig);
		t.upSingle(Component.P, pi, sig);
		t.upSingle(Component.O, oi, sig);
		t.upSingle(Component.C, ci, sig);

		/* complement sets for singles */
		t.upSingleCmpl(Component.S, Component.P, si, p);
		t.upSingleCmpl(Component.S, Component.O, si, o);
		t.upSingleCmpl(Component.S, Component.C, si, c);

		t.upSingleCmpl(Component.P, Component.S, pi, s);
		t.upSingleCmpl(Component.P, Component.O, pi, o);
		t.upSingleCmpl(Component.P, Component.C, pi, c);

		t.upSingleCmpl(Component.O, Component.S, oi, s);
		t.upSingleCmpl(Component.O, Component.P, oi, p);
		t.upSingleCmpl(Component.O, Component.C, oi, c);

		t.upSingleCmpl(Component.C, Component.S, ci, s);
		t.upSingleCmpl(Component.C, Component.P, ci, p);
		t.upSingleCmpl(Component.C, Component.O, ci, o);

		/* pairs (triples + complements) */
		t.upPair(Pair.SP, si, pi, sig, o, c);
		t.upPair(Pair.SO, si, oi, sig, p, c);
		t.upPair(Pair.SC, si, ci, sig, p, o);
		t.upPair(Pair.PO, pi, oi, sig, s, c);
		t.upPair(Pair.PC, pi, ci, sig, s, o);
		t.upPair(Pair.OC, oi, ci, sig, s, p);
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Quick cardinalities (public) */
	/* ────────────────────────────────────────────────────────────── */

	public double cardinalitySingle(Component c, String v) {
		Sketch sk = current.singleTriples.get(c).get(hash(v));
		BuildState del = usingA ? delA : delB;
		UpdateSketch deleted = del.singleTriples.get(c).get(hash(v));
		if (deleted != null && sk != null) {
			// subtract deleted hashes
			AnotB aNotB = SetOperation.builder().buildANotB();
			aNotB.setA(sk);
			aNotB.notB(deleted);
			sk = aNotB.getResult(false);
		}

		return sk == null ? 0.0 : sk.getEstimate();
	}

	public double cardinalityPair(Pair p, String x, String y) {
		long key = pairKey(hash(x), hash(y));

		Sketch sk = current.pairs.get(p).triples.get(key); // live data
		BuildState del = usingA ? delA : delB; // tomb-stones
		UpdateSketch deleted = del.pairs.get(p).triples.get(key);

		if (sk != null && deleted != null) { // A-NOT-B
			AnotB diff = SetOperation.builder().buildANotB();
			diff.setA(sk);
			diff.notB(deleted);
			sk = diff.getResult(false);
		}
		return sk == null ? 0.0 : sk.getEstimate();
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Legacy join helpers (unchanged API) */
	/* ────────────────────────────────────────────────────────────── */

	public double estimateJoinOn(Component join, Pair a, String ax, String ay, Pair b, String bx, String by) {
		return joinPairs(current, join, a, ax, ay, b, bx, by);
	}

	public double estimateJoinOn(Component j, Component a, String av, Component b, String bv) {
		return joinSingles(current, j, a, av, b, bv);
	}

	/* ────────────────────────────────────────────────────────────── */
	/* ✦ Fluent Basic‑Graph‑Pattern builder ✦ */
	/* ────────────────────────────────────────────────────────────── */

	public JoinEstimate estimate(Component joinVar, String s, String p, String o, String c) {
		ReadState snap = current;
		PatternStats st = statsOf(snap, joinVar, s, p, o, c);
		Sketch bindings = st.sketch == null ? EMPTY : st.sketch;
		return new JoinEstimate(snap, joinVar, bindings, bindings.getEstimate(), st.card);
	}

	public double estimateCount(Component joinVar, String s, String p, String o, String c) {
		return estimate(joinVar, s, p, o, c).estimate();
	}

	public final class JoinEstimate {
		private final ReadState snap;
		private Component joinVar;
		private Sketch bindings;
		private double distinct;
		private double resultSize;

		private JoinEstimate(ReadState snap, Component joinVar, Sketch bindings, double distinct, double size) {
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
	private PatternStats statsOf(ReadState rs, Component j, String s, String p, String o, String c) {
		Sketch sk = bindingsSketch(rs, j, s, p, o, c);

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
			// unsupported
			card = 0.0;
			break;

		case 1: {
			Map.Entry<Component, String> e = fixed.entrySet().iterator().next();
			card = cardSingle(rs, e.getKey(), e.getValue());
			break;
		}

		case 2: {
			Component[] cmp = fixed.keySet().toArray(new Component[0]);
			Pair pr = findPair(cmp[0], cmp[1]);
			if (pr != null) {
				card = cardPair(rs, pr, fixed.get(pr.x), fixed.get(pr.y));
			} else { // components not a known pair – conservative min
				double a = cardSingle(rs, cmp[0], fixed.get(cmp[0]));
				double b = cardSingle(rs, cmp[1], fixed.get(cmp[1]));
				card = Math.min(a, b);
			}
			break;
		}

		default: { // 3 or 4 bound – use smallest single cardinality
			card = Double.POSITIVE_INFINITY;
			for (Map.Entry<Component, String> e : fixed.entrySet()) {
				card = Math.min(card, cardSingle(rs, e.getKey(), e.getValue()));
			}
			break;
		}
		}
		return new PatternStats(sk, card);
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Snapshot‑level cardinalities */
	/* ────────────────────────────────────────────────────────────── */

	private double cardSingle(ReadState rs, Component c, String val) {
		Sketch sk = rs.singleTriples.get(c).get(hash(val));
		return sk == null ? 0.0 : sk.getEstimate();
	}

	private double cardPair(ReadState rs, Pair p, String x, String y) {
		Sketch sk = rs.pairs.get(p).triples.get(pairKey(hash(x), hash(y)));
		return sk == null ? 0.0 : sk.getEstimate();
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Sketch helpers */
	/* ────────────────────────────────────────────────────────────── */

	private Sketch bindingsSketch(ReadState rs, Component j, String s, String p, String o, String c) {

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
			var e = f.entrySet().iterator().next();
			return singleWrapper(rs, e.getKey()).getComplementSketch(j, hash(e.getValue()));
		}

		/* 2 constants: pair fast path */
		if (f.size() == 2) {
			Component[] cs = f.keySet().toArray(new Component[0]);
			Pair pr = findPair(cs[0], cs[1]);
			if (pr != null && (j == pr.comp1 || j == pr.comp2)) {
				int idxX = hash(f.get(pr.x));
				int idxY = hash(f.get(pr.y));
				return pairWrapper(rs, pr).getComplementSketch(j, pairKey(idxX, idxY));
			}
		}

		/* generic fall‑back */
		Sketch acc = null;
		for (var e : f.entrySet()) {
			Sketch sk = singleWrapper(rs, e.getKey()).getComplementSketch(j, hash(e.getValue()));
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
	/* Pair & single wrappers */
	/* ────────────────────────────────────────────────────────────── */

	private ReadStateSingleWrapper singleWrapper(ReadState rs, Component fixed) {
		return new ReadStateSingleWrapper(fixed, rs.singles.get(fixed));
	}

	private ReadStatePairWrapper pairWrapper(ReadState rs, Pair p) {
		return new ReadStatePairWrapper(p, rs.pairs.get(p));
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Join primitives */
	/* ────────────────────────────────────────────────────────────── */

	private double joinPairs(ReadState rs, Component j,
			Pair a, String ax, String ay,
			Pair b, String bx, String by) {

		long keyA = pairKey(hash(ax), hash(ay));
		long keyB = pairKey(hash(bx), hash(by));

		// live data
		Sketch sa = pairWrapper(rs, a).getComplementSketch(j, keyA);
		Sketch sb = pairWrapper(rs, b).getComplementSketch(j, keyB);

		// tomb-stones
		BuildState del = usingA ? delA : delB;

		UpdateSketch delSa = (j == a.comp1)
				? del.pairs.get(a).comp1.get(keyA)
				: (j == a.comp2 ? del.pairs.get(a).comp2.get(keyA) : null);

		UpdateSketch delSb = (j == b.comp1)
				? del.pairs.get(b).comp1.get(keyB)
				: (j == b.comp2 ? del.pairs.get(b).comp2.get(keyB) : null);

		if (sa != null && delSa != null) { // A-NOT-B
			AnotB diff = SetOperation.builder().buildANotB();
			diff.setA(sa);
			diff.notB(delSa);
			sa = diff.getResult(false);
		}
		if (sb != null && delSb != null) {
			AnotB diff = SetOperation.builder().buildANotB();
			diff.setA(sb);
			diff.notB(delSb);
			sb = diff.getResult(false);
		}

		if (sa == null || sb == null) {
			return 0.0;
		}

		Intersection ix = SetOperation.builder().buildIntersection();
		ix.intersect(sa);
		ix.intersect(sb);
		return ix.getResult().getEstimate();
	}

	private double joinSingles(ReadState rs, Component j,
			Component a, String av,
			Component b, String bv) {

		int idxA = hash(av), idxB = hash(bv);

		// live data
		Sketch sa = singleWrapper(rs, a).getComplementSketch(j, idxA);
		Sketch sb = singleWrapper(rs, b).getComplementSketch(j, idxB);

		// tomb-stones
		BuildState del = usingA ? delA : delB;
		UpdateSketch delSa = del.singles.get(a).cmpl.get(j).get(idxA);
		UpdateSketch delSb = del.singles.get(b).cmpl.get(j).get(idxB);

		if (sa != null && delSa != null) { // A-NOT-B
			AnotB diff = SetOperation.builder().buildANotB();
			diff.setA(sa);
			diff.notB(delSa);
			sa = diff.getResult(false);
		}
		if (sb != null && delSb != null) {
			AnotB diff = SetOperation.builder().buildANotB();
			diff.setA(sb);
			diff.notB(delSb);
			sb = diff.getResult(false);
		}

		if (sa == null || sb == null) {
			return 0.0;
		}

		Intersection ix = SetOperation.builder().buildIntersection();
		ix.intersect(sa);
		ix.intersect(sb);
		return ix.getResult().getEstimate();
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Read‑only snapshot structures */
	/* ────────────────────────────────────────────────────────────── */

	private static final class ReadStateSingleWrapper {
		final Component fixed;
		final SingleRead idx;

		ReadStateSingleWrapper(Component f, SingleRead i) {
			fixed = f;
			idx = i;
		}

		Sketch getComplementSketch(Component c, int fi) {
			if (c == fixed) {
				return null;
			}
			Int2ObjectOpenHashMap<Sketch> m = idx.complements.get(c);
			return m == null ? null : m.getOrDefault(fi, EMPTY);
		}
	}

	private static final class ReadStatePairWrapper {
		final Pair p;
		final PairRead idx;

		ReadStatePairWrapper(Pair p, PairRead i) {
			this.p = p;
			idx = i;
		}

		Sketch getComplementSketch(Component c, long key) {
			if (c == p.comp1) {
				return idx.comp1.getOrDefault(key, EMPTY);
			}
			if (c == p.comp2) {
				return idx.comp2.getOrDefault(key, EMPTY);
			}
			return null;
		}
	}

	private static final class ReadState {
		final EnumMap<Component, Int2ObjectOpenHashMap<Sketch>> singleTriples = new EnumMap<>(Component.class);
		final EnumMap<Component, SingleRead> singles = new EnumMap<>(Component.class);
		final EnumMap<Pair, PairRead> pairs = new EnumMap<>(Pair.class);

		ReadState() {
			for (Component c : Component.values()) {
				singleTriples.put(c, new Int2ObjectOpenHashMap<>(4, 0.99999f));
				singles.put(c, new SingleRead());
			}
			for (Pair p : Pair.values()) {
				pairs.put(p, new PairRead());
			}
		}
	}

	private static final class SingleRead {
		final EnumMap<Component, Int2ObjectOpenHashMap<Sketch>> complements = new EnumMap<>(Component.class);

		SingleRead() {
			for (Component c : Component.values()) {
				complements.put(c, new Int2ObjectOpenHashMap<>(4, 0.99999f));
			}
		}
	}

	private static final class PairRead {
		final Map<Long, Sketch> triples = new ConcurrentHashMap<>();
		final Map<Long, Sketch> comp1 = new ConcurrentHashMap<>();
		final Map<Long, Sketch> comp2 = new ConcurrentHashMap<>();
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Build‑time structures */
	/* ────────────────────────────────────────────────────────────── */

	private static final class SingleBuild {
		final int k;
		final EnumMap<Component, Int2ObjectOpenHashMap<UpdateSketch>> cmpl = new EnumMap<>(Component.class);

		SingleBuild(int k, Component fixed) {
			this.k = k;
			for (Component c : Component.values()) {
				if (c != fixed) {
					cmpl.put(c, new Int2ObjectOpenHashMap<>(4, 0.99999f));
				}
			}
		}

		void upd(Component c, int idx, String v) {
			Int2ObjectOpenHashMap<UpdateSketch> m = cmpl.get(c);
			if (m == null) {
				return;
			}
			UpdateSketch updateSketch = m.computeIfAbsent(idx, i -> newSk(k));
			if (updateSketch == null) {
				return; // sketch creation failed
			}
			updateSketch.update(v);
		}
	}

	private static final class PairBuild {
		final int k;
		final Map<Long, UpdateSketch> triples = new ConcurrentHashMap<>();
		final Map<Long, UpdateSketch> comp1 = new ConcurrentHashMap<>();
		final Map<Long, UpdateSketch> comp2 = new ConcurrentHashMap<>();

		PairBuild(int k) {
			this.k = k;
		}

		void upT(long key, String sig) {
			triples.computeIfAbsent(key, i -> newSk(k)).update(sig);
		}

		void up1(long key, String v) {
			comp1.computeIfAbsent(key, i -> newSk(k)).update(v);
		}

		void up2(long key, String v) {
			comp2.computeIfAbsent(key, i -> newSk(k)).update(v);
		}
	}

	private static final class BuildState {
		final int k;
		final EnumMap<Component, Int2ObjectOpenHashMap<UpdateSketch>> singleTriples = new EnumMap<>(Component.class);
		final EnumMap<Component, SingleBuild> singles = new EnumMap<>(Component.class);
		final EnumMap<Pair, PairBuild> pairs = new EnumMap<>(Pair.class);

		BuildState(int k) {
			this.k = k;
			for (Component c : Component.values()) {
				singleTriples.put(c, new Int2ObjectOpenHashMap<>(4, 0.99999f));
				singles.put(c, new SingleBuild(k, c));
			}
			for (Pair p : Pair.values()) {
				pairs.put(p, new PairBuild(k));
			}
		}

		void clear() {
			singleTriples.values().forEach(Map::clear);
			singles.values().forEach(s -> s.cmpl.values().forEach(Map::clear));
			pairs.values().forEach(p -> {
				p.triples.clear();
				p.comp1.clear();
				p.comp2.clear();
			});
		}

		/* singles */
		void upSingle(Component c, int idx, String sig) {
			try {
				singleTriples.get(c).computeIfAbsent(idx, i -> newSk(k)).update(sig);

			} catch (NullPointerException e) {
				// this can happen if the sketch is being cleared while being updated
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to update single sketch for {} at index {} with signature '{}': {}",
							c, idx, sig, e.getMessage());
				}

			}
		}

		void upSingleCmpl(Component fix, Component cmp, int idx, String val) {
			singles.get(fix).upd(cmp, idx, val);
		}

		/* pairs */
		void upPair(Pair p, int x, int y, String sig, String v1, String v2) {
			long key = pairKey(x, y);
			PairBuild b = pairs.get(p);
			b.upT(key, sig);
			b.up1(key, v1);
			b.up2(key, v2);
		}

		/* compact with optional deletes */
		ReadState compactWithDeletes(BuildState del) {
			ReadState r = new ReadState();

			for (Component c : Component.values()) {
				Int2ObjectOpenHashMap<Sketch> out = r.singleTriples.get(c);
				Int2ObjectOpenHashMap<UpdateSketch> addM = singleTriples.get(c);
				Int2ObjectOpenHashMap<UpdateSketch> delM = del == null ? null : del.singleTriples.get(c);
				addM.forEach((idx, addSk) -> out.put(idx, subtract(addSk, delM == null ? null : delM.get(idx))));
			}

			for (Component fix : Component.values()) {
				SingleBuild inAdd = singles.get(fix);
				SingleBuild inDel = del == null ? null : del.singles.get(fix);
				SingleRead out = r.singles.get(fix);
				for (var e : inAdd.cmpl.entrySet()) {
					Component cmp = e.getKey();
					Int2ObjectOpenHashMap<Sketch> outM = out.complements.get(cmp);
					Int2ObjectOpenHashMap<UpdateSketch> addM = e.getValue();
					Int2ObjectOpenHashMap<UpdateSketch> delM = inDel == null ? null : inDel.cmpl.get(cmp);
					addM.forEach((idx, addSk) -> outM.put(idx, subtract(addSk, delM == null ? null : delM.get(idx))));
				}
			}

			for (Pair p : Pair.values()) {
				PairBuild a = pairs.get(p);
				PairBuild d = del == null ? null : del.pairs.get(p);
				PairRead o = r.pairs.get(p);
				a.triples.forEach((k, sk) -> o.triples.put(k, subtract(sk, d == null ? null : d.triples.get(k))));
				a.comp1.forEach((k, sk) -> o.comp1.put(k, subtract(sk, d == null ? null : d.comp1.get(k))));
				a.comp2.forEach((k, sk) -> o.comp2.put(k, subtract(sk, d == null ? null : d.comp2.get(k))));
			}
			return r;
		}

		private static Sketch subtract(UpdateSketch addSk, UpdateSketch delSk) {
			if (addSk == null) {
				return EMPTY;
			}
			if (delSk == null || delSk.getRetainedEntries() == 0) {
				return addSk.compact();
			}
			AnotB diff = SetOperation.builder().buildANotB();
			diff.setA(addSk);
			diff.notB(delSk);
			return diff.getResult(false);
		}
	}

	/* ────────────────────────────────────────────────────────────── */
	/* Misc utility */
	/* ────────────────────────────────────────────────────────────── */

	private static UpdateSketch newSk(int k) {
		return UpdateSketch.builder().setNominalEntries(k).build();
	}

	private int hash(String v) {
		// using Math.abs(...) results in poor estimation of join sizes
		return Objects.hashCode(v) % nominalEntries;
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
	/* OPTIONAL: convenience wrapper for optimiser API */
	/* ────────────────────────────────────────────────────────────── */

	public double cardinality(Join node) {

		TupleExpr leftArg = node.getLeftArg();
		TupleExpr rightArg = node.getRightArg();

		if (leftArg instanceof StatementPattern && rightArg instanceof StatementPattern) {
			// get common variables
			var leftStatementPattern = (StatementPattern) leftArg;
			var rightStatementPattern = (StatementPattern) rightArg;

			// first common variable
			Var commonVar = null;
			List<Var> varList = leftStatementPattern.getVarList();
			for (Var var : rightStatementPattern.getVarList()) {
				if (!var.hasValue() && varList.contains(var)) {
					commonVar = var;
					break;
				}
			}

			if (commonVar == null) {
				// no common variable, we cannot estimate the join
				return Double.MAX_VALUE;
			}

			SketchBasedJoinEstimator.Component leftComponent = getComponent(leftStatementPattern, commonVar);
			SketchBasedJoinEstimator.Component rightComponent = getComponent(rightStatementPattern, commonVar);

			return this
					.estimate(leftComponent, getIriAsStringOrNull(leftStatementPattern.getSubjectVar()),
							getIriAsStringOrNull(leftStatementPattern.getPredicateVar()),
							getIriAsStringOrNull(leftStatementPattern.getObjectVar()),
							getIriAsStringOrNull(leftStatementPattern.getContextVar()))
					.join(rightComponent, getIriAsStringOrNull(rightStatementPattern.getSubjectVar()),
							getIriAsStringOrNull(rightStatementPattern.getPredicateVar()),
							getIriAsStringOrNull(rightStatementPattern.getObjectVar()),
							getIriAsStringOrNull(rightStatementPattern.getContextVar()))
					.estimate();
		} else {
			return -1;
		}

	}

	private String getIriAsStringOrNull(Var subjectVar) {
		if (subjectVar == null || subjectVar.getValue() == null) {
			return null;
		}
		Value value = subjectVar.getValue();
		if (value instanceof IRI) {
			return value.stringValue();
		}

		return null;
	}

	private SketchBasedJoinEstimator.Component getComponent(StatementPattern statementPattern, Var commonVar) {
		// if the common variable is a subject, predicate, object or context
		if (commonVar.equals(statementPattern.getSubjectVar())) {
			return SketchBasedJoinEstimator.Component.S;
		} else if (commonVar.equals(statementPattern.getPredicateVar())) {
			return SketchBasedJoinEstimator.Component.P;
		} else if (commonVar.equals(statementPattern.getObjectVar())) {
			return SketchBasedJoinEstimator.Component.O;
		} else if (commonVar.equals(statementPattern.getContextVar())) {
			return SketchBasedJoinEstimator.Component.C;
		} else {
			throw new IllegalStateException("Unexpected common variable " + commonVar
					+ " didn't match any component of statement pattern " + statementPattern);
		}

	}
}
