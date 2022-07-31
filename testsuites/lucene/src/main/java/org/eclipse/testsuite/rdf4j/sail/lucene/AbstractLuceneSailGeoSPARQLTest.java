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
package org.eclipse.testsuite.rdf4j.sail.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractLuceneSailGeoSPARQLTest {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	public static final IRI SUBJECT_1 = vf.createIRI("urn:subject1");

	public static final IRI SUBJECT_2 = vf.createIRI("urn:subject2");

	public static final IRI SUBJECT_3 = vf.createIRI("urn:subject3");

	public static final IRI SUBJECT_4 = vf.createIRI("urn:subject4");

	public static final IRI SUBJECT_5 = vf.createIRI("urn:subject5");

	public static final IRI CONTEXT_1 = vf.createIRI("urn:context1");

	public static final IRI CONTEXT_2 = vf.createIRI("urn:context2");

	public static final IRI CONTEXT_3 = vf.createIRI("urn:context3");

	public static final Literal EIFFEL_TOWER = vf.createLiteral("POINT (2.2945 48.8582)", GEO.WKT_LITERAL);

	public static final Literal ARC_TRIOMPHE = vf.createLiteral("POINT (2.2950 48.8738)", GEO.WKT_LITERAL);

	public static final Literal NOTRE_DAME = vf.createLiteral("POINT (2.3465 48.8547)", GEO.WKT_LITERAL);

	public static final Literal POLY1 = vf.createLiteral(
			"POLYGON ((2.3294 48.8726, 2.2719 48.8643, 2.3370 48.8398, 2.3294 48.8726))", GEO.WKT_LITERAL);

	public static final Literal POLY2 = vf.createLiteral(
			"POLYGON ((2.3509 48.8429, 2.3785 48.8385, 2.3576 48.8487, 2.3509 48.8429))", GEO.WKT_LITERAL);

	public static final Literal TEST_POINT = vf.createLiteral("POINT (2.2871 48.8630)", GEO.WKT_LITERAL);

	public static final Literal TEST_POLY = vf
			.createLiteral("POLYGON ((2.315 48.855, 2.360 48.835, 2.370 48.850, 2.315 48.855))", GEO.WKT_LITERAL);

	private static final double ERROR = 2.0;

	protected LuceneSail sail;

	protected Repository repository;

	protected abstract void configure(LuceneSail sail) throws IOException;

	@Before
	public void setUp() throws Exception {
		// setup a LuceneSail
		MemoryStore memoryStore = new MemoryStore();
		// enable lock tracking
		org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true);
		sail = new LuceneSail();
		configure(sail);
		sail.setBaseSail(memoryStore);

		// create a Repository wrapping the LuceneSail
		repository = new SailRepository(sail);

		// add some statements to it
		loadPoints();
		loadPolygons();
	}

	protected void loadPoints() {
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.add(SUBJECT_1, GEO.AS_WKT, EIFFEL_TOWER, CONTEXT_1);
			connection.add(SUBJECT_2, GEO.AS_WKT, ARC_TRIOMPHE);
			connection.add(SUBJECT_3, GEO.AS_WKT, NOTRE_DAME, CONTEXT_2);
		}
	}

	protected void loadPolygons() {
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.add(SUBJECT_4, GEO.AS_WKT, POLY1);
			connection.add(SUBJECT_5, GEO.AS_WKT, POLY2, CONTEXT_3);
		}
	}

	@After
	public void tearDown() throws IOException, RepositoryException {
		if (repository != null) {
			repository.shutDown();
		}
		org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(false);

	}

	@Test
	public void testTriplesStored() throws Exception {
		// are the triples stored in the underlying sail?
		checkPoints();
		checkPolygons();
	}

	protected void checkPoints() throws RepositoryException {
		try (RepositoryConnection connection = repository.getConnection()) {
			assertTrue(connection.hasStatement(SUBJECT_1, GEO.AS_WKT, EIFFEL_TOWER, false, CONTEXT_1));
			assertTrue(connection.hasStatement(SUBJECT_2, GEO.AS_WKT, ARC_TRIOMPHE, false));
			assertTrue(connection.hasStatement(SUBJECT_3, GEO.AS_WKT, NOTRE_DAME, false, CONTEXT_2));
		}
	}

	protected void checkPolygons() throws RepositoryException {
		try (RepositoryConnection connection = repository.getConnection()) {
			assertTrue(connection.hasStatement(SUBJECT_4, GEO.AS_WKT, POLY1, false));
			assertTrue(connection.hasStatement(SUBJECT_5, GEO.AS_WKT, POLY2, false, CONTEXT_3));
		}
	}

	@Test
	public void testDistanceQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection connection = repository.getConnection()) {
			String queryStr = "prefix geo:  <" + GEO.NAMESPACE + ">" + "prefix geof: <" + GEOF.NAMESPACE + ">"
					+ "select ?toUri ?to where { ?toUri geo:asWKT ?to. filter(geof:distance(?from, ?to, ?units) < ?range) }";
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			query.setBinding("from", TEST_POINT);
			query.setBinding("units", GEOF.UOM_METRE);
			query.setBinding("range", sail.getValueFactory().createLiteral(1500.0));

			try (TupleQueryResult result = query.evaluate()) {

				// check the results
				Map<IRI, Literal> expected = new LinkedHashMap<>();
				expected.put(SUBJECT_1, EIFFEL_TOWER);
				expected.put(SUBJECT_2, ARC_TRIOMPHE);

				while (result.hasNext()) {
					BindingSet bindings = result.next();
					IRI subj = (IRI) bindings.getValue("toUri");
					// check ordering
					IRI expectedUri = expected.keySet().iterator().next();
					assertEquals(expectedUri, subj);

					Literal location = expected.remove(subj);
					assertNotNull(location);
					assertEquals(location, bindings.getValue("to"));
				}
				assertTrue(expected.isEmpty());
			}
		}
	}

	@Test
	public void testComplexDistanceQuery()
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection connection = repository.getConnection()) {
			String queryStr = "prefix geo:  <" + GEO.NAMESPACE + ">" + "prefix geof: <" + GEOF.NAMESPACE + ">"
					+ "select ?toUri ?dist ?g where { graph ?g {?toUri geo:asWKT ?to.}"
					+ " bind(geof:distance(?from, ?to, ?units) as ?dist)" + " filter(?dist < ?range)" + " }";
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			query.setBinding("from", TEST_POINT);
			query.setBinding("units", GEOF.UOM_METRE);
			query.setBinding("range", sail.getValueFactory().createLiteral(1500.0));

			try (TupleQueryResult result = query.evaluate()) {

				// check the results
				Map<IRI, Literal> expected = new LinkedHashMap<>();
				expected.put(SUBJECT_1, sail.getValueFactory().createLiteral(760.0));

				while (result.hasNext()) {
					BindingSet bindings = result.next();
					IRI subj = (IRI) bindings.getValue("toUri");
					// check ordering
					IRI expectedUri = expected.keySet().iterator().next();
					assertEquals(expectedUri, subj);

					Literal dist = expected.remove(subj);
					assertNotNull(dist);
					assertEquals(dist.doubleValue(), ((Literal) bindings.getValue("dist")).doubleValue(), ERROR);

					assertNotNull(bindings.getValue("g"));
				}
				assertTrue(expected.isEmpty());
			}
		}
	}

	@Test
	public void testComplexDistanceQueryMathExpr()
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection connection = repository.getConnection()) {
			String queryStr = "prefix geo:  <" + GEO.NAMESPACE + ">" + "prefix geof: <" + GEOF.NAMESPACE + ">"
					+ "select ?toUri ?dist ?g where { graph ?g {?toUri geo:asWKT ?to.}"
					+ " bind((geof:distance(?from, ?to, ?units) / 1000) as ?dist)" + " filter(?dist < ?range)" + " }";
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			query.setBinding("from", TEST_POINT);
			query.setBinding("units", GEOF.UOM_METRE);
			query.setBinding("range", sail.getValueFactory().createLiteral(1.5));

			List<BindingSet> result = QueryResults.asList(query.evaluate());

			// check the results
			Map<IRI, Literal> expected = new LinkedHashMap<>();
			expected.put(SUBJECT_1, sail.getValueFactory().createLiteral(760.0 / 1000.0));

			for (BindingSet bindings : result) {
				System.out.println(bindings);
				IRI subj = (IRI) bindings.getValue("toUri");
				// check ordering
				IRI expectedUri = expected.keySet().iterator().next();
				assertEquals(expectedUri, subj);

				Literal dist = expected.remove(subj);
				assertNotNull(dist);
				assertEquals(dist.doubleValue(), ((Literal) bindings.getValue("dist")).doubleValue(), ERROR);

				assertNotNull(bindings.getValue("g"));
			}
			assertTrue(expected.isEmpty());
		}

	}

	public void testIntersectionQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection connection = repository.getConnection()) {
			String queryStr = "prefix geo:  <" + GEO.NAMESPACE + ">" + "prefix geof: <" + GEOF.NAMESPACE + ">"
					+ "select ?matchUri ?match where { ?matchUri geo:asWKT ?match. filter(geof:sfIntersects(?pattern, ?match)) }";
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			query.setBinding("pattern", TEST_POLY);

			try (TupleQueryResult result = query.evaluate()) {

				// check the results
				Map<IRI, Literal> expected = new HashMap<>();
				expected.put(SUBJECT_4, POLY1);
				expected.put(SUBJECT_5, POLY2);

				while (result.hasNext()) {
					BindingSet bindings = result.next();
					IRI subj = (IRI) bindings.getValue("matchUri");

					Literal location = expected.remove(subj);
					assertNotNull(location);
					assertEquals(location, bindings.getValue("match"));
				}
				assertTrue(expected.isEmpty());
			}
		}
	}

	public void testComplexIntersectionQuery()
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection connection = repository.getConnection()) {

			String queryStr = "prefix geo:  <" + GEO.NAMESPACE + ">" + "prefix geof: <" + GEOF.NAMESPACE + ">"
					+ "select ?matchUri ?intersects ?g where { graph ?g {?matchUri geo:asWKT ?match.}"
					+ " bind(geof:sfIntersects(?pattern, ?match) as ?intersects)" + " filter(?intersects)" + " }";
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			query.setBinding("pattern", TEST_POLY);

			try (TupleQueryResult result = query.evaluate()) {

				// check the results
				Map<IRI, Literal> expected = new HashMap<>();
				expected.put(SUBJECT_5, sail.getValueFactory().createLiteral(true));

				while (result.hasNext()) {
					BindingSet bindings = result.next();
					IRI subj = (IRI) bindings.getValue("matchUri");

					Literal location = expected.remove(subj);
					assertNotNull("Expected subject: " + subj, location);
					assertEquals(location.booleanValue(), ((Literal) bindings.getValue("intersects")).booleanValue());

					assertNotNull(bindings.getValue("g"));
				}
				assertTrue(expected.isEmpty());
			}
		}
	}
}
