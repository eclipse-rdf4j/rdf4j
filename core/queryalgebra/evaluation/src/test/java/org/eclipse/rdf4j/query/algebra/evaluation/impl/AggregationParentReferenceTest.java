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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.ParentReferenceChecker;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.junit.jupiter.api.Test;

/**
 * Test to reproduce and fix the parent reference issue with aggregation queries.
 */
public class AggregationParentReferenceTest {

	@Test
	public void testAggregationParentReferences() {
		String query = "SELECT (COUNT(?s) AS ?count) WHERE { ?s ?p ?o  }";

		TupleExpr expr = new SPARQLParser().parseQuery(query, null).getTupleExpr();

		// Print the structure before parent reference check
		System.out.println("Structure: " + expr);

		// This should not throw an AssertionError about parent references
		new ParentReferenceChecker(null).optimize(expr, new SimpleDataset(), new EmptyBindingSet());
	}

	@Test
	public void testComplexAggregationParentReferences() {
		String query = "SELECT (COUNT(?s)/30 AS ?count) WHERE { ?s ?p ?o  }";

		TupleExpr expr = new SPARQLParser().parseQuery(query, null).getTupleExpr();

		// This should not throw an AssertionError about parent references
		new ParentReferenceChecker(null).optimize(expr, new SimpleDataset(), new EmptyBindingSet());
	}
}