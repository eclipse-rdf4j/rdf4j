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

public class RDFContainersTest {

	private final static ValueFactory vf = SimpleValueFactory.getInstance();

	private final List<Value> values = new ArrayList<>();

	private Literal a;

	private Literal b;

	private Literal c;

	private IRI RDF_1;

	private IRI RDF_2;

	private IRI RDF_3;

	@Before
	public void setUp() throws Exception {
		a = Literals.createLiteral(vf, "A");
		b = Literals.createLiteral(vf, "B");
		c = Literals.createLiteral(vf, "C");

		RDF_1 = vf.createIRI(RDF.NAMESPACE, "_" + 1);
		RDF_2 = vf.createIRI(RDF.NAMESPACE, "_" + 2);
		RDF_3 = vf.createIRI(RDF.NAMESPACE, "_" + 3);

		values.add(a);
		values.add(b);
		values.add(c);
	}

	@Test
	public void testConversionRoundtrip() {
		IRI container = vf.createIRI("urn:container");
		Model m = RDFContainers.toRDF(RDF.BAG, values, container, new TreeModel());
		assertNotNull(m);

		assertTrue(m.contains(container, RDF_1, a));
		assertTrue(m.contains(container, RDF_2, b));
		assertTrue(m.contains(container, RDF_3, c));

		List<Value> newList = RDFContainers.toValues(RDF.BAG, m, container, new ArrayList<>());

		assertNotNull(newList);
		assertTrue(newList.contains(a));
		assertTrue(newList.contains(b));
		assertTrue(newList.contains(c));
	}

	@Test
	public void testInjectedValueFactoryIsUsed() {
		Resource container = vf.createBNode();
		ValueFactory injected = mock(SimpleValueFactory.class, CALLS_REAL_METHODS);
		RDFContainers.toRDF(RDF.BAG, values, container, new TreeModel(), injected);
		verify(injected, atLeastOnce()).createStatement(any(), any(), any());
	}

	@Test
	public void testExtract() {
		Resource container = vf.createBNode();
		Model m = RDFContainers.toRDF(RDF.BAG, values, container, new TreeModel());

		// add something to the model that is not part of the RDF container.
		m.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);

		Model containerModel = RDFContainers.getContainer(RDF.BAG, m, container, new TreeModel());
		assertNotNull(containerModel);
		assertFalse(containerModel.contains(RDF.TYPE, RDF.TYPE, RDF.PROPERTY));
	}

	@Test
	public void testRemove() {
		Resource container = vf.createBNode();
		Model m = RDFContainers.toRDF(RDF.BAG, values, container, new TreeModel());

		// add something to the model that is not part of the RDF container.
		m.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);

		// remove the entire container
		RDFContainers.extract(RDF.BAG, m, container, st -> m.remove(st));

		assertTrue(m.contains(RDF.TYPE, RDF.TYPE, RDF.PROPERTY));
	}

}
