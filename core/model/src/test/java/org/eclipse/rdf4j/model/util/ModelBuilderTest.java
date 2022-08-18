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
package org.eclipse.rdf4j.model.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.Before;
import org.junit.Test;

public class ModelBuilderTest {

	private ModelBuilder testBuilder;

	private Model model;

	@Before
	public void setUp() throws Exception {
		model = new LinkedHashModel();
		testBuilder = new ModelBuilder(model);
	}

	@Test
	public void testAddPredicateObject1() {
		try {
			testBuilder.add(RDF.TYPE, RDFS.CLASS);
			fail("add should have failed with model exception: subject not set");
		} catch (ModelException e) {
			// fall through, expected
		}
	}

	@Test
	public void testAddPredicateObject2() {
		testBuilder.subject(RDF.TYPE);
		testBuilder.add(RDF.TYPE, RDFS.CLASS);

		assertTrue(model.contains(RDF.TYPE, RDF.TYPE, RDFS.CLASS));
	}

	@Test
	public void testAddWithStringSubject() {
		testBuilder.add("foaf:Person", RDF.TYPE, RDFS.CLASS);

		assertTrue(model.contains(FOAF.PERSON, RDF.TYPE, RDFS.CLASS));
	}

	@Test
	public void testAddWithUnknownStringSubject() {
		testBuilder.add("ex:Person", RDF.TYPE, RDFS.CLASS);

		assertTrue(model.contains(SimpleValueFactory.getInstance().createIRI("ex:Person"), RDF.TYPE, RDFS.CLASS));
	}

	@Test
	public void testAddWithIllegalStringSubject() {
		try {
			testBuilder.add("Johnny", RDF.TYPE, RDFS.CLASS);
			fail("should have failed on illegal IRI for subject");
		} catch (ModelException e) {
			// fall through, expected
		}

	}

	@Test
	public void testAddInteger() {
		testBuilder.subject(FOAF.PERSON).add("rdfs:label", 9);

		assertTrue(model.contains(FOAF.PERSON, RDFS.LABEL, SimpleValueFactory.getInstance().createLiteral(9)));
	}

	@Test
	public void testAddObjectStringIRI() {
		testBuilder.subject(FOAF.PERSON).add("rdf:type", "rdfs:Class");
		assertTrue(model.contains(FOAF.PERSON, RDF.TYPE, RDFS.CLASS));
	}

	@Test
	public void testAddNamedGraph() {
		testBuilder.namedGraph(RDF.ALT).subject(FOAF.PERSON).add("rdf:type", RDFS.CLASS);

		assertTrue(model.contains(FOAF.PERSON, RDF.TYPE, RDFS.CLASS, RDF.ALT));

		testBuilder.add(RDF.TYPE, RDF.PROPERTY);

		assertTrue(model.contains(FOAF.PERSON, RDF.TYPE, RDF.PROPERTY, RDF.ALT));

		testBuilder.defaultGraph().add(FOAF.PERSON, RDF.TYPE, RDF.BAG);

		assertTrue(model.contains(FOAF.PERSON, RDF.TYPE, RDF.BAG));
		assertFalse(model.contains(FOAF.PERSON, RDF.TYPE, RDF.BAG, RDF.ALT));
	}

	@Test
	public void testAddNamedGraph2() {
		testBuilder.namedGraph("rdf:Alt").subject(FOAF.PERSON).add("rdf:type", RDFS.CLASS);

		assertTrue(model.contains(FOAF.PERSON, RDF.TYPE, RDFS.CLASS, RDF.ALT));

		testBuilder.add(RDF.TYPE, RDF.PROPERTY);

		assertTrue(model.contains(FOAF.PERSON, RDF.TYPE, RDF.PROPERTY, RDF.ALT));
	}

	@Test
	public void testNSAddedForDatatype() {
		testBuilder.add("ex:Person", FOAF.AGE, 42);

		assertTrue(model.getNamespaces().contains(XSD.NS));
	}

	@Test
	public void testNSNotAddedForDatatypeString() {
		testBuilder.add("ex:Person", FOAF.NAME, "John Doe");

		assertFalse(model.getNamespaces().contains(XSD.NS));
	}
}
