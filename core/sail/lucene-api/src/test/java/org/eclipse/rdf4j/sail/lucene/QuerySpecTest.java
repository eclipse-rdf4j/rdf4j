/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.junit.Test;

public class QuerySpecTest extends SearchQueryEvaluatorTest {
	private static final String QUERY = "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>\n" +
			"SELECT * {" +
			"   ?searchR search:matches [" +
			"       search:query \"test\" ;" +
			"       search:score ?score" +
			"   ] ." +
			"   ?searchR rdfs:label ?label ." +
			"}";

	@Test
	public void testReplaceQueryPatternsWithNonEmptyResults() {
		final String expectedQueryPlan = "Join\n" +
				"   Join\n" +
				"      SingletonSet\n" +
				"      SingletonSet\n" +
				"   BindingSetAssignment ([[searchR=urn:1]])\n";
		final ParsedQuery query = parseQuery(QUERY);
		final List<SearchQueryEvaluator> queries = new ArrayList<>();
		new QuerySpecBuilder(true)
				.process(query.getTupleExpr(), EmptyBindingSet.getInstance(), queries);
		assertEquals(1, queries.size());
		QuerySpec querySpec = (QuerySpec) queries.get(0);
		BindingSetAssignment bsa = new BindingSetAssignment();
		bsa.setBindingSets(createBindingSet("searchR", "urn:1"));
		querySpec.replaceQueryPatternsWithResults(bsa);
		String result = querySpec.getParentQueryModelNode().getParentNode().toString().replaceAll("\r\n|\r", "\n");
		assertEquals(expectedQueryPlan, result);
	}

	@Test
	public void testReplaceQueryPatternsWithEmptyResults() {
		final String expectedQueryPlan = "Join\n" +
				"   Join\n" +
				"      SingletonSet\n" +
				"      SingletonSet\n" +
				"   EmptySet\n";
		final ParsedQuery query = parseQuery(QUERY);
		final List<SearchQueryEvaluator> queries = new ArrayList<>();
		new QuerySpecBuilder(true)
				.process(query.getTupleExpr(), EmptyBindingSet.getInstance(), queries);
		assertEquals(1, queries.size());
		QuerySpec querySpec = (QuerySpec) queries.get(0);
		BindingSetAssignment bsa = new BindingSetAssignment();
		querySpec.replaceQueryPatternsWithResults(bsa);
		String result = querySpec.getParentQueryModelNode().getParentNode().toString().replaceAll("\r\n|\r", "\n");
		assertEquals(expectedQueryPlan, result);
	}
}
