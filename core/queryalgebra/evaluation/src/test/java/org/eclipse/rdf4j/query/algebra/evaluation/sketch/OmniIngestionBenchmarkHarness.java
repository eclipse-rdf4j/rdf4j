/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.sketch;

import java.util.SplittableRandom;

/**
 * Standalone timing harness for the Omni sketch ingestion hot path.
 * <p>
 * Replicates the per-statement update sequence of {@code SketchBasedJoinEstimator.updateOmniJoinEstimator} (context
 * pair sketches disabled) against synthetic, SPO-clustered data with production sketch parameters, and reports
 * steady-state statements/second plus a semantic checksum for before/after comparisons.
 */
public final class OmniIngestionBenchmarkHarness {

	private static final int WIDTH = 16;
	private static final int ROWS = 2;
	private static final int NOMINAL_ENTRIES = 2048;
	private static final long SEED = 0x51E7C0DEL;
	private static final long ANY_VALUE_HASH = 0x4f4d4e495f454447L;

	private static final int STATEMENTS = 5_000_000;
	private static final int STATEMENTS_PER_SUBJECT = 20;
	private static final int PREDICATES = 64;
	private static final int POPULAR_OBJECTS = 4_000;

	private static final byte ATTR_S = OmniAttributeRef.component(0);
	private static final byte ATTR_P = OmniAttributeRef.component(1);
	private static final byte ATTR_O = OmniAttributeRef.component(2);
	private static final byte ATTR_C = OmniAttributeRef.component(3);
	private static final byte ATTR_PO = OmniAttributeRef.subjectStarPO();
	private static final byte TS_S_PO = OmniAttributeRef.tupleSurface(0, 1, 2);
	private static final byte TS_P_SO = OmniAttributeRef.tupleSurface(1, 0, 2);
	private static final byte TS_O_SP = OmniAttributeRef.tupleSurface(2, 0, 1);

	private OmniIngestionBenchmarkHarness() {
	}

	public static void main(String[] args) {
		int warmupPasses = args.length > 0 ? Integer.parseInt(args[0]) : 2;
		int measuredPasses = args.length > 1 ? Integer.parseInt(args[1]) : 3;

		long[] hs = new long[STATEMENTS];
		long[] hp = new long[STATEMENTS];
		long[] ho = new long[STATEMENTS];
		long[] sig = new long[STATEMENTS];
		SketchTermKey[] predicateKeys = new SketchTermKey[PREDICATES];
		int[] predicateIndex = new int[STATEMENTS];
		generateData(hs, hp, ho, sig, predicateKeys, predicateIndex);
		long hc = mix(0xC0FFEE);

		System.out.printf("statements=%d subjects=%d predicates=%d%n", STATEMENTS,
				STATEMENTS / STATEMENTS_PER_SUBJECT, PREDICATES);

		for (int pass = 0; pass < warmupPasses + measuredPasses; pass++) {
			OmniJoinEstimator estimator = new OmniJoinEstimator(WIDTH, ROWS, NOMINAL_ENTRIES, SEED);
			long start = System.nanoTime();
			ingest(estimator, hs, hp, ho, sig, hc, predicateKeys, predicateIndex);
			long elapsed = System.nanoTime() - start;
			double perSecond = STATEMENTS / (elapsed / 1_000_000_000.0d);
			String label = pass < warmupPasses ? "warmup " : "measure";
			System.out.printf("%s pass %d: %,.0f statements/s (%.2f s) checksum=%016x%n", label, pass, perSecond,
					elapsed / 1_000_000_000.0d, checksum(estimator, hs, hp, predicateKeys));
		}
	}

	private static void generateData(long[] hs, long[] hp, long[] ho, long[] sig, SketchTermKey[] predicateKeys,
			int[] predicateIndex) {
		SplittableRandom random = new SplittableRandom(42);
		for (int p = 0; p < PREDICATES; p++) {
			predicateKeys[p] = SketchTermKey.lmdbValueId(1000 + p, 7);
		}
		long[] popularObjects = new long[POPULAR_OBJECTS];
		for (int i = 0; i < POPULAR_OBJECTS; i++) {
			popularObjects[i] = mix(random.nextLong());
		}
		for (int i = 0; i < hs.length; i++) {
			long subjectId = i / STATEMENTS_PER_SUBJECT;
			hs[i] = mix(subjectId * 0x9E3779B97F4A7C15L + 1);
			// skewed predicate choice, sorted runs within a subject like an SPO scan
			int predicate = Math.min(PREDICATES - 1, (int) (-Math.log(random.nextDouble()) * 8.0d));
			predicateIndex[i] = predicate;
			hp[i] = mix(0xABCDEF00L + predicate);
			if (random.nextInt(10) < 3) {
				ho[i] = popularObjects[random.nextInt(POPULAR_OBJECTS)];
			} else {
				ho[i] = mix(random.nextLong());
			}
			sig[i] = mix(hs[i] ^ Long.rotateLeft(hp[i], 17) ^ Long.rotateLeft(ho[i], 34));
		}
	}

	private static void ingest(OmniJoinEstimator estimator, long[] hs, long[] hp, long[] ho, long[] sig, long hc,
			SketchTermKey[] predicateKeys, int[] predicateIndex) {
		OmniJoinEstimator.Relation statements = estimator.relation(OmniRelation.STATEMENT);
		OmniJoinEstimator.Relation subjectStar = estimator.relation(OmniRelation.SUBJECT_STAR);
		OmniJoinEstimator.Relation tupleSurface = estimator.relation(OmniRelation.TUPLE_SURFACE);
		OmniJoinEstimator.Relation edgeForward = estimator.relation(OmniRelation.EDGE_FORWARD);
		OmniJoinEstimator.Relation edgeReverse = estimator.relation(OmniRelation.EDGE_REVERSE);
		for (int i = 0; i < hs.length; i++) {
			long subjectHash = hs[i];
			long predicateHash = hp[i];
			long objectHash = ho[i];
			long statementIdentifier = sig[i];
			SketchTermKey predicateKey = predicateKeys[predicateIndex[i]];

			statements.updateStatic(ATTR_S, subjectHash, statementIdentifier, 1.0d);
			statements.updateStatic(ATTR_P, predicateHash, statementIdentifier, 1.0d);
			statements.updateStatic(ATTR_O, objectHash, statementIdentifier, 1.0d);
			statements.updateStaticTotalOnly(ATTR_C, hc, 1.0d);

			subjectStar.updateStatic(ATTR_P, predicateHash, subjectHash, 1.0d);
			subjectStar.updateStatic(ATTR_O, objectHash, subjectHash, 1.0d);
			subjectStar.updateStatic(ATTR_PO, OmniJoinEstimator.orderedTupleHash(predicateHash, objectHash),
					subjectHash, 1.0d);

			edgeForward.updatePredicate(predicateKey, subjectHash, objectHash, 1.0d);
			edgeReverse.updatePredicate(predicateKey, objectHash, subjectHash, 1.0d);
			edgeForward.updatePredicate(predicateKey, ANY_VALUE_HASH, objectHash, 1.0d);
			edgeReverse.updatePredicate(predicateKey, ANY_VALUE_HASH, subjectHash, 1.0d);

			tupleSurface.updateStatic(TS_S_PO, subjectHash,
					OmniJoinEstimator.orderedTupleHash(predicateHash, objectHash), 1.0d);
			tupleSurface.updateStatic(TS_P_SO, predicateHash,
					OmniJoinEstimator.orderedTupleHash(subjectHash, objectHash), 1.0d);
			tupleSurface.updateStatic(TS_O_SP, objectHash,
					OmniJoinEstimator.orderedTupleHash(subjectHash, predicateHash), 1.0d);
		}
	}

	/** Order-insensitive semantic fingerprint of the ingested state, for before/after equivalence checks. */
	private static long checksum(OmniJoinEstimator estimator, long[] hs, long[] hp, SketchTermKey[] predicateKeys) {
		OmniJoinEstimator.Relation statements = estimator.relation(OmniRelation.STATEMENT);
		OmniJoinEstimator.Relation subjectStar = estimator.relation(OmniRelation.SUBJECT_STAR);
		long checksum = 0;
		for (int i = 0; i < hs.length; i += 997) {
			checksum = checksum * 31 + Double.hashCode(statements.staticTotalWeight(ATTR_S, hs[i]));
			checksum = checksum * 31 + Double.hashCode(statements.staticTotalWeight(ATTR_P, hp[i]));
			checksum = checksum * 31 + Double.hashCode(subjectStar.staticTotalWeight(ATTR_P, hp[i]));
		}
		return checksum;
	}

	private static long mix(long value) {
		long mixed = value ^ (value >>> 33);
		mixed *= 0xFF51AFD7ED558CCDL;
		mixed ^= mixed >>> 33;
		mixed *= 0xC4CEB9FE1A85EC53L;
		return mixed ^ (mixed >>> 33);
	}
}
