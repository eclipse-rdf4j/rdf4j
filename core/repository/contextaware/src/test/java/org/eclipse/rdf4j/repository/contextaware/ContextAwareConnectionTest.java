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
package org.eclipse.rdf4j.repository.contextaware;

import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.impl.AbstractQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.junit.Test;

public class ContextAwareConnectionTest {

	static class GraphQueryStub extends AbstractQuery implements GraphQuery {

		@Override
		public GraphQueryResult evaluate() {
			return null;
		}

		@Override
		public void evaluate(RDFHandler arg0) {
		}

		@Override
		public Explanation explain(Explanation.Level level) {
			throw new UnsupportedOperationException();
		}
	}

	static class InvocationHandlerStub implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return null;
		}
	}

	static class QueryStub extends AbstractQuery {
		@Override
		public Explanation explain(Explanation.Level level) {
			throw new UnsupportedOperationException();
		}
	}

	static class RepositoryStub extends RepositoryWrapper {

		@Override
		public RepositoryConnection getConnection() throws RepositoryException {
			ClassLoader cl = ContextAwareConnectionTest.class.getClassLoader();
			Class<?>[] classes = new Class[] { RepositoryConnection.class };
			InvocationHandlerStub handler = new InvocationHandlerStub();
			Object proxy = Proxy.newProxyInstance(cl, classes, handler);
			return (RepositoryConnection) proxy;
		}
	}

	static class TupleQueryStub extends AbstractQuery implements TupleQuery {

		@Override
		public TupleQueryResult evaluate() {
			return null;
		}

		@Override
		public void evaluate(TupleQueryResultHandler arg0) {
		}

		@Override
		public Explanation explain(Explanation.Level level) {
			throw new UnsupportedOperationException();
		}
	}

	private static class RepositoryConnectionStub extends RepositoryConnectionWrapper {

		public RepositoryConnectionStub() {
			super(new RepositoryStub());
		}
	}

	IRI context = SimpleValueFactory.getInstance().createIRI("urn:test:context");

	String queryString = "SELECT ?o WHERE { ?s ?p ?o}";

	@Test
	public void testGraphQuery() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub() {

			@Override
			public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI)
					throws MalformedQueryException, RepositoryException {
				assertEquals(SPARQL, ql);
				assertEquals(queryString, query);
				return new GraphQueryStub() {

					@Override
					public void setDataset(Dataset dataset) {
						Set<IRI> contexts = Collections.singleton(context);
						assertEquals(contexts, dataset.getDefaultGraphs());
						super.setDataset(dataset);
					}
				};
			}
		};
		Repository repo = stub.getRepository();
		ContextAwareConnection con = new ContextAwareConnection(repo, stub);
		con.setReadContexts(context);
		con.prepareGraphQuery(SPARQL, queryString, null);
	}

	@Test
	public void testQuery() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub() {

			@Override
			public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
					throws MalformedQueryException, RepositoryException {
				assertEquals(SPARQL, ql);
				assertEquals(queryString, query);
				return new QueryStub() {

					@Override
					public void setDataset(Dataset dataset) {
						Set<IRI> contexts = Collections.singleton(context);
						assertEquals(contexts, dataset.getDefaultGraphs());
						super.setDataset(dataset);
					}
				};
			}
		};
		Repository repo = stub.getRepository();
		ContextAwareConnection con = new ContextAwareConnection(repo, stub);
		con.setReadContexts(context);
		con.prepareQuery(SPARQL, queryString, null);
	}

	@Test
	public void testTupleQuery() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub() {

			@Override
			public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI)
					throws MalformedQueryException, RepositoryException {
				assertEquals(SPARQL, ql);
				assertEquals(queryString, query);
				return new TupleQueryStub() {

					@Override
					public void setDataset(Dataset dataset) {
						Set<IRI> contexts = Collections.singleton(context);
						assertEquals(contexts, dataset.getDefaultGraphs());
						super.setDataset(dataset);
					}
				};
			}
		};
		Repository repo = stub.getRepository();
		ContextAwareConnection con = new ContextAwareConnection(repo, stub);
		con.setReadContexts(context);
		con.prepareTupleQuery(SPARQL, queryString, null);
	}

	@Test
	public void testIncludeInferred() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub();
		Repository repo = stub.getRepository();
		ContextAwareConnection a = new ContextAwareConnection(repo, stub);
		ContextAwareConnection b = new ContextAwareConnection(repo, a);
		b.setIncludeInferred(true);
		assertTrue(b.isIncludeInferred());
		assertTrue(a.isIncludeInferred());
	}

	@Test
	public void testMaxQueryTime() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub();
		Repository repo = stub.getRepository();
		ContextAwareConnection a = new ContextAwareConnection(repo, stub);
		ContextAwareConnection b = new ContextAwareConnection(repo, a);
		b.setMaxQueryTime(1);
		assertEquals(1, b.getMaxQueryTime());
		assertEquals(1, a.getMaxQueryTime());
	}

	@Test
	public void testQueryLanguage() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub();
		Repository repo = stub.getRepository();
		ContextAwareConnection a = new ContextAwareConnection(repo, stub);
		ContextAwareConnection b = new ContextAwareConnection(repo, a);
		b.setQueryLanguage(QueryLanguage.SPARQL);
		assertEquals(QueryLanguage.SPARQL, b.getQueryLanguage());
		assertEquals(QueryLanguage.SPARQL, a.getQueryLanguage());
	}

	@Test
	public void testBaseURI() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub();
		Repository repo = stub.getRepository();
		ContextAwareConnection a = new ContextAwareConnection(repo, stub);
		ContextAwareConnection b = new ContextAwareConnection(repo, a);
		b.setBaseURI("http://example.com/");
		assertEquals("http://example.com/", b.getBaseURI());
		assertEquals("http://example.com/", a.getBaseURI());
	}

	@Test
	public void testReadContexts() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub();
		Repository repo = stub.getRepository();
		ContextAwareConnection a = new ContextAwareConnection(repo, stub);
		ContextAwareConnection b = new ContextAwareConnection(repo, a);
		b.setReadContexts(context);
		assertEquals(context, b.getReadContexts()[0]);
		assertEquals(context, a.getReadContexts()[0]);
	}

	@Test
	public void testRemoveContexts() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub();
		Repository repo = stub.getRepository();
		ContextAwareConnection a = new ContextAwareConnection(repo, stub);
		ContextAwareConnection b = new ContextAwareConnection(repo, a);
		b.setRemoveContexts(context);
		assertEquals(context, b.getRemoveContexts()[0]);
		assertEquals(context, a.getRemoveContexts()[0]);
	}

	@Test
	public void testAddContexts() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub();
		Repository repo = stub.getRepository();
		ContextAwareConnection a = new ContextAwareConnection(repo, stub);
		ContextAwareConnection b = new ContextAwareConnection(repo, a);
		b.setAddContexts(context);
		assertEquals(context, b.getAddContexts()[0]);
		assertEquals(context, a.getAddContexts()[0]);
	}

	@Test
	public void testArchiveContexts() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub();
		Repository repo = stub.getRepository();
		ContextAwareConnection a = new ContextAwareConnection(repo, stub);
		ContextAwareConnection b = new ContextAwareConnection(repo, a);
		b.setArchiveContexts(context);
		assertEquals(context, b.getArchiveContexts()[0]);
		assertEquals(context, a.getArchiveContexts()[0]);
	}

	@Test
	public void testInsertContexts() throws Exception {
		RepositoryConnection stub = new RepositoryConnectionStub();
		Repository repo = stub.getRepository();
		ContextAwareConnection a = new ContextAwareConnection(repo, stub);
		ContextAwareConnection b = new ContextAwareConnection(repo, a);
		b.setInsertContext(context);
		assertEquals(context, b.getInsertContext());
		assertEquals(context, a.getInsertContext());
	}
}
