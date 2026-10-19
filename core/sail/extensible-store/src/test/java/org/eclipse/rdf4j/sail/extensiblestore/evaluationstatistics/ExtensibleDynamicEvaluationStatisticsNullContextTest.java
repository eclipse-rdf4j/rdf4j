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
package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.junit.jupiter.api.Test;

/**
 * Reproduces a NullPointerException in ExtensibleDynamicEvaluationStatistics when evaluating the cardinality of a
 * ZeroLengthPath with a null context variable.
 *
 * The EvaluationStatistics visitor for ZeroLengthPath calls getContextCardinality(node.getContextVar()) where the
 * context var may be null. The overridden implementation in ExtensibleDynamicEvaluationStatistics assumed a non-null
 * Var and dereferenced it, causing an NPE.
 */
public class ExtensibleDynamicEvaluationStatisticsNullContextTest {

	@Test
	public void testZeroLengthPathWithNullContextDoesNotThrow() {
		// Given a dynamic evaluation statistics instance with no data loaded
		ExtensibleDynamicEvaluationStatistics stats = new ExtensibleDynamicEvaluationStatistics(null);

		// And a ZeroLengthPath with subject and object vars, but no context var (null)
		ZeroLengthPath zlp = new ZeroLengthPath(new Var("s"), new Var("o"));

		// When asking for cardinality, this used to throw a NullPointerException because
		// getContextCardinality(Var var) dereferenced var without checking for null.
		// Then it should simply return a numeric value (0.0 with empty stats), not throw.
		double cardinality = stats.getCardinality(zlp);

		// With no statements added, subject/object/context cardinalities are 0 -> overall 0
		assertEquals(0.0, cardinality);
	}
}
