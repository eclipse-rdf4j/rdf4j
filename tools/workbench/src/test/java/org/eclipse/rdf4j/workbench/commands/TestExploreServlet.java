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
package org.eclipse.rdf4j.workbench.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.workbench.commands.ExploreServlet.ResultCursor;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Dale Visser
 */
public class TestExploreServlet {

	private RepositoryConnection connection;

	private ExploreServlet servlet;

	private IRI foo, bar, bang, foos[];

	private static final String PREFIX = "PREFIX : <http://www.test.com/>\nINSERT DATA { GRAPH :foo { ";

	private static final String SUFFIX = " . } }";

	private TupleResultBuilder builder;

	/**
	 * @throws RepositoryException      if an issue occurs making the connection
	 * @throws MalformedQueryException  if an issue occurs inserting data
	 * @throws UpdateExecutionException if an issue occurs inserting data
	 */
	@Before
	public void setUp() throws RepositoryException, MalformedQueryException, UpdateExecutionException {
		Repository repo = new SailRepository(new MemoryStore());
		connection = repo.getConnection();
		servlet = new ExploreServlet();
		ValueFactory factory = connection.getValueFactory();
		foo = factory.createIRI("http://www.test.com/foo");
		bar = factory.createIRI("http://www.test.com/bar");
		bang = factory.createIRI("http://www.test.com/bang");
		foos = new IRI[128];
		for (int i = 0; i < foos.length; i++) {
			foos[i] = factory.createIRI("http://www.test.com/foo/" + i);
		}
		builder = mock(TupleResultBuilder.class);
	}

	@After
	public void tearDown() throws RepositoryException {
		connection.close();
		servlet.destroy();
	}

	@Test
	public final void testRegressionSES1748() throws RDF4JException {
		for (int i = 0; i < foos.length; i++) {
			connection.add(foo, bar, foos[i]);
		}
		assertStatementCount(foo, 10, foos.length, 10);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.workbench.commands.ExploreServlet#processResource(org.eclipse.rdf4j.repository.RepositoryConnection, org.eclipse.rdf4j.workbench.util.TupleResultBuilder, org.eclipse.rdf4j.model.Value, int, int, boolean)}
	 *
	 * @throws RepositoryException if a problem occurs executing the method under test
	 */
	@Test
	public final void testSubjectSameAsContext() throws RDF4JException {
		addToFooContext(":foo a :bar");
		assertStatementCount(foo, 1, 1);
		verify(builder).result(foo, RDF.TYPE, bar, foo);
	}

	@Test
	public final void testPredicateSameAsContext() throws RDF4JException {
		addToFooContext(":bar :foo :bar");
		assertStatementCount(foo, 1, 1);
		verify(builder).result(bar, foo, bar, foo);
	}

	@Test
	public final void testObjectSameAsContext() throws RDF4JException {
		addToFooContext(":bar a :foo");
		assertStatementCount(foo, 1, 1);
		verify(builder).result(bar, RDF.TYPE, foo, foo);
	}

	@Test
	public final void testNoValueSameAsContext() throws RDF4JException {
		addToFooContext(":bar a :bar");
		assertStatementCount(foo, 1, 1);
		verify(builder).result(bar, RDF.TYPE, bar, foo);
	}

	@Test
	public final void testOneObjectSameAsContext() throws RDF4JException {
		addToFooContext(":bar a :bar , :foo");
		assertStatementCount(foo, 2, 2);
		verify(builder).result(bar, RDF.TYPE, bar, foo);
		verify(builder).result(bar, RDF.TYPE, foo, foo);
	}

	@Test
	public final void testSubjectSameAsPredicate() throws RDF4JException {
		addToFooContext(":bar :bar :bang");
		assertStatementCount(bar, 1, 1);
		verify(builder).result(bar, bar, bang, foo);
	}

	@Test
	public final void testSubjectSameAsObject() throws RDF4JException {
		addToFooContext(":bar a :bar");
		assertStatementCount(bar, 1, 1);
		verify(builder).result(bar, RDF.TYPE, bar, foo);
	}

	@Test
	public final void testPredicateSameAsObject() throws RDF4JException {
		addToFooContext(":bar :bang :bang");
		assertStatementCount(bang, 1, 1);
		verify(builder).result(bar, bang, bang, foo);
	}

	@Test
	public final void testWorstCaseDuplication() throws RDF4JException {
		addToFooContext(":foo :foo :foo");
		assertStatementCount(foo, 1, 1);
		verify(builder).result(foo, foo, foo, foo);
	}

	@Test
	public final void testSES1723regression() throws RDF4JException {
		addToFooContext(":foo :foo :foo");
		connection.add(foo, foo, foo);
		assertStatementCount(foo, 2, 2);
		verify(builder).result(foo, foo, foo, foo);
		verify(builder).result(foo, foo, foo, null);
	}

	private void addToFooContext(String pattern)
			throws UpdateExecutionException, RepositoryException, MalformedQueryException {
		connection.prepareUpdate(QueryLanguage.SPARQL, PREFIX + pattern + SUFFIX).execute();
	}

	private void assertStatementCount(IRI uri, int expectedTotal, int expectedRendered) throws RDF4JException {
		// limit = 0 means render all
		assertStatementCount(uri, 0, expectedTotal, expectedRendered);
	}

	private void assertStatementCount(IRI uri, int limit, int expectedTotal, int expectedRendered)
			throws RDF4JException {
		ResultCursor cursor = servlet.processResource(connection, builder, uri, 0, limit, true);
		assertThat(cursor.getTotalResultCount()).isEqualTo(expectedTotal);
		assertThat(cursor.getRenderedResultCount()).isEqualTo(expectedRendered);
	}
}
