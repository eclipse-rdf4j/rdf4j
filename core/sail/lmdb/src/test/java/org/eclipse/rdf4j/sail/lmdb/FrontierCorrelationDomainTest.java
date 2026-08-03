/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cost.EvidenceGuarantee;
import org.junit.jupiter.api.Test;

class FrontierCorrelationDomainTest {

	private static FrontierCorrelationDomain domain(double rows, double estimate, double upperBound) {
		return new FrontierCorrelationDomain(1, List.of("k"), rows, 0.0d, estimate, upperBound, 0.0d, rows,
				EvidenceGuarantee.UNRESOLVED, 0.0d, null, null);
	}

	@Test
	void planningDistinctKeysPrefersFiniteEstimate() {
		assertThat(domain(100.0d, 7.0d, 50.0d).planningDistinctKeys()).isEqualTo(7.0d);
	}

	@Test
	void planningDistinctKeysFallsBackToFiniteUpperBound() {
		assertThat(domain(100.0d, Double.NaN, 50.0d).planningDistinctKeys()).isEqualTo(50.0d);
	}

	@Test
	void planningDistinctKeysBoundsUnresolvedDomainByRows() {
		// Unresolved domains have an infinite certified key bound; the planning value must stay finite
		// because packed physical costing rejects non-finite dimensions.
		assertThat(domain(100.0d, Double.NaN, Double.POSITIVE_INFINITY).planningDistinctKeys()).isEqualTo(100.0d);
	}

	@Test
	void planningDistinctKeysStaysFiniteWithoutRowEvidence() {
		assertThat(domain(Double.POSITIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY).planningDistinctKeys())
				.isEqualTo(Double.MAX_VALUE);
		assertThat(domain(Double.NaN, Double.NaN, Double.POSITIVE_INFINITY).planningDistinctKeys())
				.isEqualTo(Double.MAX_VALUE);
	}
}
