/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link QuadIndex#getBestIndex(List, long, long, long, long)} — ensures the best sort order is selected for
 * different query patterns.
 */
class QuadIndexSelectionTest {

	private static final QuadIndex SPOC = new QuadIndex("spoc");
	private static final QuadIndex OPSC = new QuadIndex("opsc");
	private static final QuadIndex CSPO = new QuadIndex("cspo");
	private static final List<QuadIndex> ALL = List.of(SPOC, OPSC, CSPO);

	@Test
	void subjectBound_selectsSPOC() {
		QuadIndex best = QuadIndex.getBestIndex(ALL, 1, -1, -1, -1);
		assertEquals("spoc", best.getFieldSeqString());
	}

	@Test
	void objectBound_selectsOPSC() {
		QuadIndex best = QuadIndex.getBestIndex(ALL, -1, -1, 1, -1);
		assertEquals("opsc", best.getFieldSeqString());
	}

	@Test
	void contextBound_selectsCSPO() {
		QuadIndex best = QuadIndex.getBestIndex(ALL, -1, -1, -1, 1);
		assertEquals("cspo", best.getFieldSeqString());
	}

	@Test
	void subjectAndPredicateBound_selectsSPOC() {
		QuadIndex best = QuadIndex.getBestIndex(ALL, 1, 2, -1, -1);
		assertEquals("spoc", best.getFieldSeqString());
	}

	@Test
	void allBound_selectsSPOC() {
		QuadIndex best = QuadIndex.getBestIndex(ALL, 1, 2, 3, 4);
		assertEquals("spoc", best.getFieldSeqString());
	}

	@Test
	void noneBound_selectsSPOC_asDefault() {
		QuadIndex best = QuadIndex.getBestIndex(ALL, -1, -1, -1, -1);
		// All have score 0; SPOC is first in the list so it wins ties
		assertEquals("spoc", best.getFieldSeqString());
	}

	@Test
	void predicateOnlyBound_selectsSPOC() {
		// Predicate is second in SPOC, second in OPSC, third in CSPO — all have score 0
		// SPOC wins as first in list
		QuadIndex best = QuadIndex.getBestIndex(ALL, -1, 5, -1, -1);
		assertEquals("spoc", best.getFieldSeqString());
	}

	@Test
	void objectAndPredicate_selectsOPSC() {
		// OPSC: o(bound)=1, p(bound)=2, score=2
		// SPOC: s(unbound)=0, score=0
		// CSPO: c(unbound)=0, score=0
		QuadIndex best = QuadIndex.getBestIndex(ALL, -1, 5, 10, -1);
		assertEquals("opsc", best.getFieldSeqString());
	}

	@Test
	void contextAndSubject_selectsCSPO() {
		// CSPO: c(bound)=1, s(bound)=2, score=2
		// SPOC: s(bound)=1, p(unbound), score=1
		// OPSC: o(unbound), score=0
		QuadIndex best = QuadIndex.getBestIndex(ALL, 1, -1, -1, 5);
		assertEquals("cspo", best.getFieldSeqString());
	}

	@Test
	void patternScore_countsLeadingBound() {
		assertEquals(4, SPOC.getPatternScore(1, 2, 3, 4));
		assertEquals(2, SPOC.getPatternScore(1, 2, -1, -1));
		assertEquals(1, SPOC.getPatternScore(1, -1, -1, -1));
		assertEquals(0, SPOC.getPatternScore(-1, 2, 3, 4)); // s unbound → 0

		assertEquals(2, OPSC.getPatternScore(-1, 2, 3, -1)); // o=3 bound, p=2 bound → 2
		assertEquals(0, OPSC.getPatternScore(1, -1, -1, -1)); // o unbound → 0

		assertEquals(1, CSPO.getPatternScore(-1, -1, -1, 5)); // c=5 bound → 1
		assertEquals(3, CSPO.getPatternScore(1, 2, -1, 5)); // c=5, s=1, p=2 → 3
	}
}
