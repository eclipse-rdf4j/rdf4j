/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

public class RDFCollectionsTest {

	private final static ValueFactory vf = SimpleValueFactory.getInstance();

	private final List<Value> values = new ArrayList<>();

	private Literal a;

	private Literal b;

	private Literal c;

	@Before
	public void setUp()
		throws Exception
	{
		a = Literals.createLiteral(vf, "A");
		b = Literals.createLiteral(vf, "B");
		c = Literals.createLiteral(vf, "C");

		values.add(a);
		values.add(b);
		values.add(c);
	}

	@Test
	public void testConversionRoundtrip() {
		IRI head = vf.createIRI("urn:head");
		Model m = RDFCollections.asRDF(values, head, new TreeModel());
		assertNotNull(m);
		assertTrue(m.contains(head, RDF.FIRST, a));
		assertFalse(m.contains(null, RDF.REST, head));

		List<Value> newList = RDFCollections.asValues(m, head, new ArrayList<>());
		assertNotNull(newList);
		assertTrue(newList.contains(a));
		assertTrue(newList.contains(b));
		assertTrue(newList.contains(c));

	}

	@Test
	public void testNonWellformedCollection() {
		Resource head = vf.createBNode();
		Model m = RDFCollections.asRDF(values, head, new TreeModel());
		m.remove(null, RDF.REST, RDF.NIL);
		try {
			RDFCollections.asValues(m, head, new ArrayList<>());
			fail("collection missing terminator should result in error");
		}
		catch (ModelException e) {
			// fall through, expected
		}

		m = RDFCollections.asRDF(values, head, new TreeModel());
		m.add(head, RDF.REST, head);

		try {
			RDFCollections.asValues(m, head, new ArrayList<>());
			fail("collection with cycle should result in error");
		}
		catch (ModelException e) {
			// fall through, expected
		}

		// supply incorrect head node
		try {
			RDFCollections.asValues(m, vf.createBNode(), new ArrayList<>());
			fail("resource that is not a collection should result in error");
		}
		catch (ModelException e) {
			// fall through, expected
		}
	}

	@Test
	public void testExtract() {
		Resource head = vf.createBNode();
		Model m = RDFCollections.asRDF(values, head, new TreeModel());

		// add something to the model that is not part of the RDF collection.
		m.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);

		Model collection = RDFCollections.getCollection(m, head, new TreeModel());
		assertNotNull(collection);
		assertFalse(collection.contains(RDF.TYPE, RDF.TYPE, RDF.PROPERTY));
		assertTrue(collection.contains(null, RDF.FIRST, a));
		assertTrue(collection.contains(null, RDF.FIRST, b));
		assertTrue(collection.contains(null, RDF.FIRST, c));
	}

	@Test
	public void testRemove() {
		Resource head = vf.createBNode();
		Model m = RDFCollections.asRDF(values, head, new TreeModel());

		// add something to the model that is not part of the RDF collection.
		m.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);

		// remove the entire collection
		RDFCollections.extract(m, head, st -> m.remove(st));

		assertFalse(m.contains(null, RDF.FIRST, a));
		assertFalse(m.contains(null, RDF.FIRST, b));
		assertFalse(m.contains(null, RDF.FIRST, c));
		assertFalse(m.contains(head, null, null));
		assertTrue(m.contains(RDF.TYPE, RDF.TYPE, RDF.PROPERTY));
	}
}
