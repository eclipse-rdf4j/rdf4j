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
package org.eclipse.rdf4j.testsuite.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * An abstract test class to test the handling of namespaces by {@link Model} implementations.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public abstract class ModelNamespacesTest {

	private Model testModel;

	/**
	 * Implementing tests must return a new, empty, Model for each call to this method.
	 *
	 * @return A new empty implementation of {@link Model} that implements the namespace related methods,
	 *         {@link Model#getNamespace(String)}, {@link Model#getNamespaces()}, {@link Model#setNamespace(Namespace)},
	 *         {@link Model#setNamespace(String, String)}, and {@link Model#removeNamespace(String)}.
	 */
	protected abstract Model getModelImplementation();

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		testModel = getModelImplementation();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
		testModel = null;
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#getNamespaces()}.
	 */
	@Test
	public final void testGetNamespacesEmpty() {
		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");
		assertTrue(namespaces.isEmpty(), "Namespaces must initially be empty");
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#getNamespaces()}.
	 */
	@Test
	public final void testGetNamespacesSingle() {
		testModel.setNamespace(RDF.PREFIX, RDF.NAMESPACE);

		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");
		assertFalse(namespaces.isEmpty());
		assertEquals(1, namespaces.size());

		assertTrue(
				namespaces.contains(new SimpleNamespace(RDF.PREFIX, RDF.NAMESPACE)),
				"Did not find the expected namespace in the set");
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#getNamespaces()}.
	 */
	@Test
	public final void testGetNamespacesMultiple() {
		testModel.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
		testModel.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
		testModel.setNamespace(DC.PREFIX, DC.NAMESPACE);
		testModel.setNamespace(SKOS.PREFIX, SKOS.NAMESPACE);
		testModel.setNamespace(SESAME.PREFIX, SESAME.NAMESPACE);

		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");
		assertFalse(namespaces.isEmpty());
		assertEquals(5, namespaces.size());

		assertTrue(namespaces.contains(new SimpleNamespace(RDF.PREFIX, RDF.NAMESPACE)));
		assertTrue(namespaces.contains(new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE)));
		assertTrue(namespaces.contains(new SimpleNamespace(DC.PREFIX, DC.NAMESPACE)));
		assertTrue(namespaces.contains(new SimpleNamespace(SKOS.PREFIX, SKOS.NAMESPACE)));
		assertTrue(namespaces.contains(new SimpleNamespace(SESAME.PREFIX, SESAME.NAMESPACE)));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#getNamespace(java.lang.String)}.
	 */
	@Test
	public final void testGetNamespaceEmpty() {
		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");
		assertTrue(namespaces.isEmpty(), "Namespaces must initially be empty");

		assertFalse(testModel.getNamespace(RDF.PREFIX).isPresent());
		assertFalse(testModel.getNamespace(RDFS.PREFIX).isPresent());
		assertFalse(testModel.getNamespace(DC.PREFIX).isPresent());
		assertFalse(testModel.getNamespace(SKOS.PREFIX).isPresent());
		assertFalse(testModel.getNamespace(SESAME.PREFIX).isPresent());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#getNamespace(java.lang.String)}.
	 */
	@Test
	public final void testGetNamespaceSingle() {
		testModel.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);

		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");
		assertFalse(namespaces.isEmpty());
		assertEquals(1, namespaces.size());

		assertTrue(namespaces.contains(new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE)),
				"Did not find the expected namespace in the set");

		assertFalse(testModel.getNamespace(RDF.PREFIX).isPresent());
		assertEquals(new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE), testModel.getNamespace(RDFS.PREFIX).get());
		assertFalse(testModel.getNamespace(DC.PREFIX).isPresent());
		assertFalse(testModel.getNamespace(SKOS.PREFIX).isPresent());
		assertFalse(testModel.getNamespace(SESAME.PREFIX).isPresent());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#getNamespace(java.lang.String)}.
	 */
	@Test
	public final void testGetNamespaceMultiple() {
		testModel.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
		testModel.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
		testModel.setNamespace(DC.PREFIX, DC.NAMESPACE);
		testModel.setNamespace(SKOS.PREFIX, SKOS.NAMESPACE);
		testModel.setNamespace(SESAME.PREFIX, SESAME.NAMESPACE);

		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");
		assertFalse(namespaces.isEmpty());
		assertEquals(5, namespaces.size());

		assertEquals(new SimpleNamespace(RDF.PREFIX, RDF.NAMESPACE), testModel.getNamespace(RDF.PREFIX).get());
		assertEquals(new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE), testModel.getNamespace(RDFS.PREFIX).get());
		assertEquals(new SimpleNamespace(DC.PREFIX, DC.NAMESPACE), testModel.getNamespace(DC.PREFIX).get());
		assertEquals(new SimpleNamespace(SKOS.PREFIX, SKOS.NAMESPACE), testModel.getNamespace(SKOS.PREFIX).get());
		assertEquals(new SimpleNamespace(SESAME.PREFIX, SESAME.NAMESPACE), testModel.getNamespace(SESAME.PREFIX).get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#setNamespace(java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testSetNamespaceSamePrefix() {
		testModel.setNamespace("r", RDF.NAMESPACE);
		testModel.setNamespace("r", RDFS.NAMESPACE);

		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");
		assertEquals(1, namespaces.size());

		assertEquals(new SimpleNamespace("r", RDFS.NAMESPACE), testModel.getNamespace("r").orElse(null));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#setNamespace(org.eclipse.rdf4j.model.Namespace)}.
	 */
	@Test
	public final void testSetNamespaceNamespace() {
		testModel.setNamespace(new SimpleNamespace(RDF.PREFIX, RDF.NAMESPACE));
		testModel.setNamespace(new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE));
		testModel.setNamespace(new SimpleNamespace(DC.PREFIX, DC.NAMESPACE));
		testModel.setNamespace(new SimpleNamespace(SKOS.PREFIX, SKOS.NAMESPACE));
		testModel.setNamespace(new SimpleNamespace(SESAME.PREFIX, SESAME.NAMESPACE));

		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");
		assertFalse(namespaces.isEmpty());
		assertEquals(5, namespaces.size());

		assertEquals(new SimpleNamespace(RDF.PREFIX, RDF.NAMESPACE), testModel.getNamespace(RDF.PREFIX).get());
		assertEquals(new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE), testModel.getNamespace(RDFS.PREFIX).get());
		assertEquals(new SimpleNamespace(DC.PREFIX, DC.NAMESPACE), testModel.getNamespace(DC.PREFIX).get());
		assertEquals(new SimpleNamespace(SKOS.PREFIX, SKOS.NAMESPACE), testModel.getNamespace(SKOS.PREFIX).get());
		assertEquals(new SimpleNamespace(SESAME.PREFIX, SESAME.NAMESPACE), testModel.getNamespace(SESAME.PREFIX).get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#setNamespace(org.eclipse.rdf4j.model.Namespace)}.
	 */
	@Test
	public final void testSetNamespaceNamespaceSamePrefix() {
		testModel.setNamespace(new SimpleNamespace("r", RDF.NAMESPACE));
		testModel.setNamespace(new SimpleNamespace("r", RDFS.NAMESPACE));

		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");
		assertEquals(1, namespaces.size());

		assertEquals(new SimpleNamespace("r", RDFS.NAMESPACE), testModel.getNamespace("r").orElse(null));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#removeNamespace(java.lang.String)}.
	 */
	@Test
	public final void testRemoveNamespaceEmpty() {
		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");

		assertFalse(testModel.removeNamespace(RDF.NAMESPACE).isPresent());
		assertFalse(testModel.removeNamespace(RDFS.NAMESPACE).isPresent());
		assertFalse(testModel.removeNamespace(DC.NAMESPACE).isPresent());
		assertFalse(testModel.removeNamespace(SKOS.NAMESPACE).isPresent());
		assertFalse(testModel.removeNamespace(SESAME.NAMESPACE).isPresent());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#removeNamespace(java.lang.String)}.
	 */
	@Test
	public final void testRemoveNamespaceSingle() {
		testModel.setNamespace(DC.PREFIX, DC.NAMESPACE);

		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces,
				"Namespaces set must not be null");
		assertFalse(namespaces.isEmpty());
		assertEquals(1, namespaces.size());

		assertTrue(
				namespaces.contains(new SimpleNamespace(DC.PREFIX, DC.NAMESPACE)),
				"Did not find the expected namespace in the set");

		assertFalse(testModel.removeNamespace(RDF.NAMESPACE).isPresent());
		assertFalse(testModel.removeNamespace(RDFS.NAMESPACE).isPresent());
		assertEquals(new SimpleNamespace(DC.PREFIX, DC.NAMESPACE), testModel.removeNamespace(DC.PREFIX).get());
		assertFalse(testModel.removeNamespace(SKOS.NAMESPACE).isPresent());
		assertFalse(testModel.removeNamespace(SESAME.NAMESPACE).isPresent());

		Set<Namespace> namespacesAfter = testModel.getNamespaces();

		assertNotNull(namespacesAfter, "Namespaces set must not be null");
		assertTrue(namespacesAfter.isEmpty(), "Namespaces must now be empty");
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#removeNamespace(java.lang.String)}.
	 */
	@Test
	public final void testRemoveNamespaceMultiple() {
		testModel.setNamespace(new SimpleNamespace(RDF.PREFIX, RDF.NAMESPACE));
		testModel.setNamespace(new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE));
		testModel.setNamespace(new SimpleNamespace(DC.PREFIX, DC.NAMESPACE));
		testModel.setNamespace(new SimpleNamespace(SKOS.PREFIX, SKOS.NAMESPACE));
		testModel.setNamespace(new SimpleNamespace(SESAME.PREFIX, SESAME.NAMESPACE));

		Set<Namespace> namespaces = testModel.getNamespaces();

		assertNotNull(namespaces, "Namespaces set must not be null");
		assertFalse(namespaces.isEmpty());
		assertEquals(5, namespaces.size());

		assertEquals(new SimpleNamespace(RDF.PREFIX, RDF.NAMESPACE), testModel.removeNamespace(RDF.PREFIX).get());
		assertEquals(new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE), testModel.removeNamespace(RDFS.PREFIX).get());
		assertEquals(new SimpleNamespace(DC.PREFIX, DC.NAMESPACE), testModel.removeNamespace(DC.PREFIX).get());
		assertEquals(new SimpleNamespace(SKOS.PREFIX, SKOS.NAMESPACE), testModel.removeNamespace(SKOS.PREFIX).get());
		assertEquals(new SimpleNamespace(SESAME.PREFIX, SESAME.NAMESPACE),
				testModel.removeNamespace(SESAME.PREFIX).get());

		Set<Namespace> namespacesAfter = testModel.getNamespaces();

		assertNotNull(namespacesAfter, "Could not find parser for this format.");
		assertTrue(namespacesAfter.isEmpty(), "Namespaces must now be empty");
	}

}
