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
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.junit.Test;

public class DistanceQuerySpecTest extends SearchQueryEvaluatorTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	/**
	 * Reused from {@link org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailGeoSPARQLTest}
	 */
	private static final String QUERY = "PREFIX geo:  <" + GEO.NAMESPACE + ">\n" +
			"PREFIX geof: <" + GEOF.NAMESPACE + ">" +
			"PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>\n" +
			"SELECT ?toUri ?to { " +
			"   ?toUri geo:asWKT ?to . " +
			"   FILTER(geof:distance(\"POINT (2.2871 48.8630)\"^^geo:wktLiteral, ?to, uom:metre) < \"1500.0\")" +
			"}";

	@Test
	public void testReplaceQueryPatternsWithNonEmptyResults() {
		final String expectedQueryPlan = "Projection\n" +
				"   ProjectionElemList\n" +
				"      ProjectionElem \"toUri\"\n" +
				"      ProjectionElem \"to\"\n" +
				"   BindingSetAssignment ([[toUri=urn:subject1;to=\"POINT (2.2945 48.8582)\"^^<http://www.opengis.net/ont/geosparql#wktLiteral>]])\n";
		final ParsedQuery query = parseQuery(QUERY);

		final List<SearchQueryEvaluator> queries = new ArrayList<>();

		new DistanceQuerySpecBuilder(new SearchIndexImpl())
				.process(query.getTupleExpr(), EmptyBindingSet.getInstance(), queries);
		assertEquals(1, queries.size());

		final DistanceQuerySpec querySpec = (DistanceQuerySpec) queries.get(0);

		final MapBindingSet bindingSet = new MapBindingSet();
		bindingSet.addBinding("toUri", VF.createIRI("urn:subject1"));
		bindingSet.addBinding("to", VF.createLiteral("POINT (2.2945 48.8582)", GEO.WKT_LITERAL));

		BindingSetAssignment bsa = new BindingSetAssignment();
		bsa.setBindingSets(Collections.singletonList(bindingSet));

		querySpec.replaceQueryPatternsWithResults(bsa);
		String result = querySpec.getParentQueryModelNode().getParentNode().toString().replaceAll("\r\n|\r", "\n");
		assertEquals(expectedQueryPlan, result);
	}

	@Test
	public void testReplaceQueryPatternsWithEmptyResults() {
		final String expectedQueryPlan = "Projection\n" +
				"   ProjectionElemList\n" +
				"      ProjectionElem \"toUri\"\n" +
				"      ProjectionElem \"to\"\n" +
				"   EmptySet\n";
		final ParsedQuery query = parseQuery(QUERY);

		final List<SearchQueryEvaluator> queries = new ArrayList<>();

		new DistanceQuerySpecBuilder(new SearchIndexImpl())
				.process(query.getTupleExpr(), EmptyBindingSet.getInstance(), queries);
		assertEquals(1, queries.size());

		final DistanceQuerySpec querySpec = (DistanceQuerySpec) queries.get(0);

		querySpec.replaceQueryPatternsWithResults(new BindingSetAssignment());
		String result = querySpec.getParentQueryModelNode().getParentNode().toString().replaceAll("\r\n|\r", "\n");
		assertEquals(expectedQueryPlan, result);
	}

}
