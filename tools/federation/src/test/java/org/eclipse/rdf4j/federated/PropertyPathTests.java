/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class PropertyPathTests extends SPARQLBaseTest {

	@BeforeEach
	public void before() {
		QueryManager qm = federationContext().getQueryManager();
		qm.addPrefixDeclaration("owl", OWL.NAMESPACE);
		qm.addPrefixDeclaration("rdfs", RDFS.NAMESPACE);
		qm.addPrefixDeclaration("skos", SKOS.NAMESPACE);
		qm.addPrefixDeclaration("foaf", FOAF.NAMESPACE);
		qm.addPrefixDeclaration("", EXAMPLE_NAMESPACE);
	}

	@Test
	public void testArbitratyLengthPath() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { ?subClass rdfs:subClassOf+ :MyClass . ?subClass rdfs:label ?label }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);
			assertContainsAll(res, "subClass",
					Sets.newHashSet(iri("MySubClass1"), iri("MySubClass2"), iri("MySubSubClass1")));
		}
	}

	@Test
	public void testPropertyPath() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { ?subClass rdfs:subClassOf/rdfs:label ?label }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);
			assertContainsAll(res, "subClass",
					Sets.newHashSet(iri("MySubClass1"), iri("MySubClass2"), iri("MySubSubClass1"), FOAF.PERSON));
		}
	}

	@Test
	public void testPropertyPath_Alternatives() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { ?concept a :MyClass . ?concept rdfs:label|skos:altLabel ?label }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);

			assertContainsAll(res, "label",
					Sets.newHashSet(l("Concept1"), l("Concept1 AltLabel"), l("Concept2"), l("Concept2 AltLabel"),
							l("Concept3"), l("Concept3 AltLabel")));
		}
	}

	@Test
	public void testPropertyPath_ExclusiveGroup() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { ?concept a skos:Concept . ?concept skos:broader+ :mammals . ?concept rdfs:label ?label}";

		String actualQueryPlan = federationContext().getQueryManager().getQueryPlan(query);
		assertQueryPlanEquals(readResourceAsString("/tests/propertypath/query_path_exclusiveGroup.qp"),
				actualQueryPlan);

		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);

			assertContainsAll(res, "label",
					Sets.newHashSet(l("Bovinae"), l("Cows")));
			assertContainsAll(res, "concept",
					Sets.newHashSet(iri("bovinae"), iri("cows")));
		}
	}

	@Test
	public void testPropertyPath_ExclusivePath() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { ?x rdf:type/rdfs:subClassOf* foaf:Agent . ?x rdfs:label ?label}";

		String actualQueryPlan = federationContext().getQueryManager().getQueryPlan(query);

		// Note: we currently cannot compare the query plan, because the queryplan contains generated
		// variable name identifiers for anonymous nodes.
//		assertQueryPlanEquals(readResourceAsString("/tests/propertypath/query_path_exclusivePath.qp"),
//				actualQueryPlan);
		Assertions.assertTrue(actualQueryPlan.contains("ExclusiveArbitraryLengthPath"));

		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);

			assertContainsAll(res, "label",
					Sets.newHashSet(l("Person 1"), l("Person 2")));
		}
	}

	@Test
	public void testPropertyPath_BoundInJoin() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { BIND(foaf:Person AS ?subClass) . ?subClass rdfs:subClassOf+ foaf:Agent . ?subClass rdfs:label ?label }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);
			assertContainsAll(res, "subClass", Sets.newHashSet(FOAF.PERSON));
			assertContainsAll(res, "label", Sets.newHashSet(l("Person")));
		}

	}

}
