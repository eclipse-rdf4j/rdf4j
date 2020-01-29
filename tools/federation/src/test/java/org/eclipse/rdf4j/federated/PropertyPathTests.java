/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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

	private static final String EXAMPLE_NAMESPACE = "http://example.org/";

	@BeforeEach
	public void before() {
		QueryManager qm = federationContext().getQueryManager();
		qm.addPrefixDeclaration("owl", OWL.NAMESPACE);
		qm.addPrefixDeclaration("rdfs", RDFS.NAMESPACE);
		qm.addPrefixDeclaration("skos", SKOS.NAMESPACE);
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
					Sets.newHashSet(iri("MySubClass1"), iri("MySubClass2"), iri("MySubSubClass1")));
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

	protected void assertContainsAll(List<BindingSet> res, String bindingName, Set<Value> expected) {
		Assertions.assertEquals(expected,
				res.stream().map(bs -> bs.getValue(bindingName)).collect(Collectors.toSet()));
		Assertions.assertEquals(expected.size(), res.size());
	}

	private Literal l(String value) {
		return SimpleValueFactory.getInstance().createLiteral(value);
	}

	private IRI iri(String localName) {
		return SimpleValueFactory.getInstance().createIRI(EXAMPLE_NAMESPACE, localName);
	}
}
