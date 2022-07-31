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

public class GeoRelationQuerySpecTest extends SearchQueryEvaluatorTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private static final String QUERY = "PREFIX geo:  <" + GEO.NAMESPACE + ">\n" +
			"PREFIX geof: <" + GEOF.NAMESPACE + ">" +
			"PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>\n" +
			"SELECT ?matchUri ?match { " +
			"   ?matchUri geo:asWKT ?match . " +
			"   FILTER(geof:sfIntersects(\"POLYGON ((2.315 48.855, 2.360 48.835, 2.370 48.850, 2.315 48.855))\"^^geo:wktLiteral, ?match)) "
			+
			"}";

	@Test
	public void testReplaceQueryPatternsWithNonEmptyResults() {
		final String expectedQueryPlan = "Projection\n" +
				"   ProjectionElemList\n" +
				"      ProjectionElem \"matchUri\"\n" +
				"      ProjectionElem \"match\"\n" +
				"   BindingSetAssignment ([[toUri=urn:subject4;to=\"POLYGON ((2.3294 48.8726, 2.2719 48.8643, 2.3370 48.8398, 2.3294 48.8726))\"^^<http://www.opengis.net/ont/geosparql#wktLiteral>]])\n";
		final ParsedQuery query = parseQuery(QUERY);

		final List<SearchQueryEvaluator> queries = new ArrayList<>();

		new GeoRelationQuerySpecBuilder(new SearchIndexImpl())
				.process(query.getTupleExpr(), EmptyBindingSet.getInstance(), queries);
		assertEquals(1, queries.size());

		final GeoRelationQuerySpec querySpec = (GeoRelationQuerySpec) queries.get(0);

		final MapBindingSet bindingSet = new MapBindingSet();
		bindingSet.addBinding("toUri", VF.createIRI("urn:subject4"));
		bindingSet.addBinding("to", VF.createLiteral(
				"POLYGON ((2.3294 48.8726, 2.2719 48.8643, 2.3370 48.8398, 2.3294 48.8726))", GEO.WKT_LITERAL));

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
				"      ProjectionElem \"matchUri\"\n" +
				"      ProjectionElem \"match\"\n" +
				"   EmptySet\n";
		final ParsedQuery query = parseQuery(QUERY);

		final List<SearchQueryEvaluator> queries = new ArrayList<>();

		new GeoRelationQuerySpecBuilder(new SearchIndexImpl())
				.process(query.getTupleExpr(), EmptyBindingSet.getInstance(), queries);
		assertEquals(1, queries.size());

		final GeoRelationQuerySpec querySpec = (GeoRelationQuerySpec) queries.get(0);

		BindingSetAssignment bsa = new BindingSetAssignment();

		querySpec.replaceQueryPatternsWithResults(bsa);
		String result = querySpec.getParentQueryModelNode().getParentNode().toString().replaceAll("\r\n|\r", "\n");
		assertEquals(expectedQueryPlan, result);
	}

}
