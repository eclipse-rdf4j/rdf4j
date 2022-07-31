/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
	public void setUp() throws Exception {
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

	@Test(expected = ModelException.class)
	public void testNonWellformedCollection_MissingTerminator() {
		Resource head = vf.createBNode();
		Model m = RDFCollections.asRDF(values, head, new TreeModel());
		m.remove(null, RDF.REST, RDF.NIL);
		RDFCollections.asValues(m, head, new ArrayList<>());
	}

	@Test(expected = ModelException.class)
	public void testNonWellformedCollection_Cycle() {
		Resource head = vf.createBNode("z");
		Model m = RDFCollections.asRDF(values, head, new TreeModel());

		// Replace rdf:rest relation for head node with one pointing to itself.
		// This introduces a cycle in an otherwise well-formed collection.
		m.remove(head, RDF.REST, null);
		m.add(head, RDF.REST, head);

		RDFCollections.asValues(m, head, new ArrayList<>());
	}

	@Test(expected = ModelException.class)
	public void testNonWellformedCollection_IncorrectHeadNode() {
		Resource head = vf.createBNode();
		Model m = RDFCollections.asRDF(values, head, new TreeModel());

		// Use resource that is unrelated to the actual collection as the head node
		RDFCollections.asValues(m, vf.createBNode(), new ArrayList<>());
	}

	@Test
	public void testInjectedValueFactoryIsUsed() {
		Resource head = vf.createBNode();
		ValueFactory injected = mock(SimpleValueFactory.class, CALLS_REAL_METHODS);
		RDFCollections.asRDF(values, head, new TreeModel(), injected);
		verify(injected, atLeastOnce()).createStatement(any(), any(), any());
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
