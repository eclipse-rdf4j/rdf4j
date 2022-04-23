/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.testsuite.rdf4j.sail.lucene;

import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.PROPERTY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.QUERY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
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

public abstract class AbstractLuceneSailIndexedPropertiesTest {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	protected LuceneSail sail;

	protected Repository repository;

	public static final IRI SUBJECT_1 = vf.createIRI("urn:subject1");

	public static final IRI SUBJECT_2 = vf.createIRI("urn:subject2");

	public static final IRI SUBJECT_3 = vf.createIRI("urn:subject3");

	public static final IRI SUBJECT_4 = vf.createIRI("urn:subject4");

	public static final IRI SUBJECT_5 = vf.createIRI("urn:subject5");

	public static final IRI CONTEXT_1 = vf.createIRI("urn:context1");

	public static final IRI CONTEXT_2 = vf.createIRI("urn:context2");

	public static final IRI CONTEXT_3 = vf.createIRI("urn:context3");

	public static final IRI RDFSLABEL = RDFS.LABEL;

	public static final IRI RDFSCOMMENT = RDFS.COMMENT;

	public static final IRI FOAFNAME = vf.createIRI("http://xmlns.com/foaf/0.1/name");

	public static final IRI FOAFPLAN = vf.createIRI("http://xmlns.com/foaf/0.1/plan");

	protected abstract void configure(LuceneSail sail);

	@Before
	public void setUp() throws Exception {
		// setup a LuceneSail
		MemoryStore memoryStore = new MemoryStore();
		// enable lock tracking
		org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true);
		sail = new LuceneSail();
		configure(sail);
		Properties indexedFields = new Properties();
		indexedFields.setProperty("index.1", RDFSLABEL.toString());
		indexedFields.setProperty("index.2", RDFSCOMMENT.toString());
		indexedFields.setProperty(FOAFNAME.toString(), RDFS.LABEL.toString());
		ByteArrayOutputStream indexedFieldsString = new ByteArrayOutputStream();
		indexedFields.store(indexedFieldsString, "For testing");
		sail.setParameter(LuceneSail.INDEXEDFIELDS, indexedFieldsString.toString());
		sail.setBaseSail(memoryStore);

		// create a Repository wrapping the LuceneSail
		repository = new SailRepository(sail);

		// add some statements to it
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.add(SUBJECT_1, RDFSLABEL, vf.createLiteral("the first resource"));
			connection.add(SUBJECT_1, RDFSCOMMENT, vf.createLiteral(
					"Groucho Marx is going to cut away the first part of the first party of the contract."));
			connection.add(SUBJECT_1, FOAFNAME, vf.createLiteral("groucho and harpo"));

			connection.add(SUBJECT_2, FOAFNAME, vf.createLiteral("the second resource"));
			connection.add(SUBJECT_2, RDFSCOMMENT,
					vf.createLiteral("in the night at the opera, groucho is in a cabin on a ship."));

			connection.add(SUBJECT_3, RDFSLABEL, vf.createLiteral("the third resource"));
			connection.add(SUBJECT_3, RDFSCOMMENT,
					vf.createLiteral("a not well known fact, groucho marx was not a smoker"));
			// this should not be indexed
			connection.add(SUBJECT_3, FOAFPLAN, vf.createLiteral("groucho did not smoke cigars nor cigarillos"));
			connection.commit();
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
		try (RepositoryConnection connection = repository.getConnection()) {
			assertTrue(connection.hasStatement(SUBJECT_1, RDFSLABEL, vf.createLiteral("the first resource"), false));
			assertTrue(connection.hasStatement(SUBJECT_1, RDFSCOMMENT,
					vf.createLiteral(
							"Groucho Marx is going to cut away the first part of the first party of the contract."),
					false));
			assertTrue(connection.hasStatement(SUBJECT_1, FOAFNAME, vf.createLiteral("groucho and harpo"), false));

			assertTrue(connection.hasStatement(SUBJECT_2, FOAFNAME, vf.createLiteral("the second resource"), false));
			assertTrue(connection.hasStatement(SUBJECT_2, RDFSCOMMENT,
					vf.createLiteral("in the night at the opera, groucho is in a cabin on a ship."), false));

			assertTrue(connection.hasStatement(SUBJECT_3, RDFSLABEL, vf.createLiteral("the third resource"), false));
			assertTrue(connection.hasStatement(SUBJECT_3, RDFSCOMMENT,
					vf.createLiteral("a not well known fact, groucho marx was not a smoker"), false));
			// this should not be indexed
			assertTrue(connection.hasStatement(SUBJECT_3, FOAFPLAN,
					vf.createLiteral("groucho did not smoke cigars nor cigarillos"), false));
			System.err.println("--- after");
		}
	}

	@Test
	public void testRegularQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection connection = repository.getConnection()) {
			// fire a query for all subjects with a given term
			String queryString = "SELECT ?Subject ?Score " + "WHERE { ?Subject <" + MATCHES + "> [ " + " <" + QUERY
					+ "> ?Query; " + " <" + PROPERTY + "> ?Property; " + " <" + SCORE + "> ?Score ].} ";
			{
				TupleQuery query = connection.prepareTupleQuery(queryString);
				query.setBinding("Query", vf.createLiteral("resource"));
				query.setBinding("Property", RDFSLABEL);
				try (TupleQueryResult result = query.evaluate()) {
					// check the results
					ArrayList<IRI> uris = new ArrayList<>();

					BindingSet bindings;
					while (result.hasNext()) {
						bindings = result.next();
						uris.add((IRI) bindings.getValue("Subject"));
						assertNotNull(bindings.getValue("Score"));
					}
					assertEquals(3, uris.size());
					assertTrue(uris.contains(SUBJECT_1));
					assertTrue(uris.contains(SUBJECT_2));
					assertTrue(uris.contains(SUBJECT_3));
				}
			}

			{
				TupleQuery query = connection.prepareTupleQuery(queryString);
				query.setBinding("Query", vf.createLiteral("groucho"));
				query.setBinding("Property", RDFSLABEL);
				try (TupleQueryResult result = query.evaluate()) {
					// check the results
					ArrayList<IRI> uris = new ArrayList<>();

					BindingSet bindings;
					while (result.hasNext()) {
						bindings = result.next();
						uris.add((IRI) bindings.getValue("Subject"));
						assertNotNull(bindings.getValue("Score"));
					}
					assertEquals(1, uris.size());
					assertTrue(uris.contains(SUBJECT_1));
				}
			}

			{
				TupleQuery query = connection.prepareTupleQuery(queryString);
				query.setBinding("Query", vf.createLiteral("cigarillos"));
				query.setBinding("Property", FOAFPLAN);
				try (TupleQueryResult result = query.evaluate()) {
					// check the results
					assertFalse(result.hasNext());
				}
			}
		}
	}
}
