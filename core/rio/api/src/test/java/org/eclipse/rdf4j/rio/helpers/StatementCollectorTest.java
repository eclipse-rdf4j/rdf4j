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
package org.eclipse.rdf4j.rio.helpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Peter Ansell
 */
public class StatementCollectorTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.helpers.StatementCollector#StatementCollector()}.
	 */
	@Test
	public final void testStatementCollector() throws Exception {
		StatementCollector collector = new StatementCollector();

		assertNotNull(collector.getStatements());
		assertNotNull(collector.getNamespaces());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.helpers.StatementCollector#StatementCollector(java.util.Collection)}
	 * .
	 */
	@Test
	public final void testStatementCollectorList() throws Exception {
		List<Statement> testList = new ArrayList<>();
		StatementCollector collector = new StatementCollector(testList);

		// Actual variable is exactly the same, although it could be theoretically
		// wrapped and still be consistent
		assertTrue(testList == collector.getStatements());
		assertNotNull(collector.getNamespaces());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.helpers.StatementCollector#StatementCollector(java.util.Collection)}
	 * .
	 */
	@Test
	public final void testStatementCollectorSet() throws Exception {
		Set<Statement> testList = new LinkedHashSet<>();
		StatementCollector collector = new StatementCollector(testList);

		// Actual variable is exactly the same, although it could be theoretically
		// wrapped and still be consistent
		assertTrue(testList == collector.getStatements());
		assertNotNull(collector.getNamespaces());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.helpers.StatementCollector#StatementCollector(java.util.Collection)}
	 * .
	 */
	@Test
	public final void testStatementCollectorCollectionModel() throws Exception {
		Model testList = new LinkedHashModel();
		StatementCollector collector = new StatementCollector(testList);

		// Actual variable is exactly the same, although it could be theoretically
		// wrapped and still be consistent
		assertTrue(testList == collector.getStatements());
		assertNotNull(collector.getNamespaces());

		assertTrue(testList.getNamespaces().isEmpty());
		assertTrue(collector.getNamespaces().isEmpty());

		collector.handleNamespace("ns1", "http://example.org/ns1#");

		assertFalse(testList.getNamespaces().isEmpty());
		assertFalse(collector.getNamespaces().isEmpty());
		assertTrue(collector.getNamespaces().containsKey("ns1"));
		assertTrue(collector.getNamespaces().containsValue("http://example.org/ns1#"));
		assertTrue(testList.getNamespaces().iterator().next().getPrefix().equals("ns1"));
		assertTrue(testList.getNamespaces().iterator().next().getName().equals("http://example.org/ns1#"));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.StatementCollector#StatementCollector(java.util.Collection, java.util.Map)}
	 * .
	 */
	@Test
	public final void testStatementCollectorCollectionModelMapIndependent() throws Exception {
		Model testList = new LinkedHashModel();
		Map<String, String> testNamespaces = new LinkedHashMap<>();
		StatementCollector collector = new StatementCollector(testList, testNamespaces);

		// Actual variable is exactly the same, although it could be theoretically
		// wrapped and still be consistent
		assertTrue(testList == collector.getStatements());
		assertTrue(testNamespaces == collector.getNamespaces());

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.StatementCollector#StatementCollector(java.util.Collection, java.util.Map)}
	 * .
	 */
	@Test
	public final void testStatementCollectorCollectionMapModel() throws Exception {
		Collection<Statement> testList = new LinkedHashModel();
		Map<String, String> testNamespaces = new LinkedHashMap<>();
		StatementCollector collector = new StatementCollector(testList, testNamespaces);

		// Actual variable is exactly the same, although it could be theoretically
		// wrapped and still be consistent
		assertTrue(testList == collector.getStatements());
		assertTrue(testNamespaces == collector.getNamespaces());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler#startRDF()}.
	 */
	@Test
	public final void testStartRDF() throws Exception {
		StatementCollector testCollector = new StatementCollector();
		testCollector.startRDF();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler#endRDF()}.
	 */
	@Test
	public final void testEndRDF() throws Exception {
		StatementCollector testCollector = new StatementCollector();
		testCollector.startRDF();
		testCollector.endRDF();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler#handleComment(java.lang.String)} .
	 *
	 * @throws Exception
	 */
	@Test
	public final void testHandleComment() throws Exception {
		StatementCollector testCollector = new StatementCollector();
		// StatementCollector must be able to handle comments, but does not
		// preserve them
		testCollector.handleComment("Comment");
	}

}
