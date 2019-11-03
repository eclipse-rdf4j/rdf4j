/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import static org.assertj.core.api.Java6Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.stream.IntStream;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnectionWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public class CustomFederationConnectionTest {

	@Test
	public void testSize() throws Exception {
		Federation federation = new Federation();

		SailRepository repository = new SailRepository(new MemoryStore());
		federation.addMember(repository);

		federation.initialize();
		try {
			try (SailConnection connection = federation.getConnection()) {
				assertEquals("Should get size", 0, connection.size());

				connection.begin();
				assertEquals("Should get size", 0, connection.size());

				connection.addStatement(OWL.CLASS, RDFS.COMMENT, RDF.REST);
				assertEquals("Should get size", 1, connection.size());

				connection.commit();
				assertEquals("Should get size", 1, connection.size());
			}
		} finally {
			federation.shutDown();
		}
	}

	@Test
	public void testSizeWithInferredStatements() throws Exception {
		Federation federation = new Federation();

		SailRepository repository = new SailRepository(new TestInferencer(new MemoryStore()));

		federation.addMember(repository);

		federation.initialize();
		try {
			try (SailConnection connection = federation.getConnection()) {
				connection.begin();
				connection.addStatement(OWL.CLASS, RDFS.COMMENT, RDF.REST);
				connection.commit();

				assertHasStatement("Should find explicit statement", OWL.CLASS, RDFS.COMMENT, RDF.REST, connection);
				assertHasStatement("Should find inferred statement", OWL.THING, RDFS.COMMENT, RDF.ALT, connection);

				assertEquals("Should get explicit statement size", 1, connection.size());
			}
		} finally {
			federation.shutDown();
		}
	}

	private static void assertHasStatement(String message, Resource subject, URI predicate, Value object,
			SailConnection connection) throws SailException {
		try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(subject,
				(IRI) predicate, object, true)) {
			assertTrue(message, statements.hasNext());
		}
	}

	public static class TestInferencer extends NotifyingSailWrapper {

		public TestInferencer(NotifyingSail baseSail) {
			super(baseSail);
		}

		@Override
		public TestInferencerConnection getConnection() throws SailException {
			try {
				return new TestInferencerConnection((InferencerConnection) super.getConnection());
			} catch (ClassCastException e) {
				throw new SailException(e.getMessage(), e);
			}
		}

		public static class TestInferencerConnection extends InferencerConnectionWrapper {

			private boolean m_added;

			private boolean m_removed;

			public TestInferencerConnection(InferencerConnection con) {
				super(con);
				con.addConnectionListener(new SailConnectionListener() {

					@Override
					public void statementAdded(Statement st) {
						m_added = true;
					}

					@Override
					public void statementRemoved(Statement st) {
						m_removed = true;
					}
				});
			}

			@Override
			public void flushUpdates() throws SailException {
				if (m_added) {
					addInferredStatement(OWL.THING, RDFS.COMMENT, RDF.ALT);
					m_added = false;
				}
				if (m_removed) {
					addInferredStatement(OWL.THING, RDFS.COMMENT, RDF.REST);
					m_removed = false;
				}

				super.flushUpdates();
			}

		}

	}

	@Test
	public void testAssertionErrorReproduction() {

		for (int i = 0; i < 1000; i++) {

			ValueFactory vf = SimpleValueFactory.getInstance();
			BNode address = vf.createBNode();
			BNode anotherAddress = vf.createBNode();

			ModelBuilder builder = new ModelBuilder();
			builder
					.setNamespace("ex", "http://example.org/")
					.subject("ex:Picasso")
					.add(RDF.TYPE, "ex:Artist")
					.add(FOAF.FIRST_NAME, "Pablo")
					.add("ex:homeAddress", address)
					.subject("ex:AnotherArtist")
					.add(RDF.TYPE, "ex:Artist")
					.add(FOAF.FIRST_NAME, "AnotherArtist")
					.add("ex:homeAddress", anotherAddress)
					.subject(address)
					.add("ex:street", "31 Art Gallery")
					.add("ex:city", "Madrid")
					.add("ex:country", "Spain")
					.subject(anotherAddress)
					.add("ex:street", "32 Art Gallery")
					.add("ex:city", "London")
					.add("ex:country", "UK");

			Model model = builder.build();
			Repository repo1 = new SailRepository(new MemoryStore());
			repo1.init();
			try (RepositoryConnection connection = repo1.getConnection()) {
				connection.add(model);
			}

			Repository repo2 = new SailRepository(new MemoryStore());
			repo2.init();

			Federation fed = new Federation();
			fed.addMember(repo1);
			fed.addMember(repo2);

			String ex = "http://example.org/";
			String queryString = "PREFIX rdf: <" + RDF.NAMESPACE + ">\n" +
					"PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
					"PREFIX ex: <" + ex + ">\n" +
					"select (count(?persons) as ?count) {\n" +
					"   ?persons rdf:type ex:Artist ;\n"
					+ "          ex:homeAddress ?country .\n"
					+ " ?country ex:country \"Spain\" . }";

			SailRepository fedRepo = new SailRepository(fed);
			fedRepo.init();

			IntStream.range(0, 100).parallel().forEach(j -> {

				try (SailRepositoryConnection fedRepoConn = fedRepo.getConnection()) {

					fedRepoConn.begin();
					TupleQuery query = fedRepoConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
					try (TupleQueryResult eval = query.evaluate()) {
						if (eval.hasNext()) {
							Value next = eval.next().getValue("count");
							assertEquals(1, ((Literal) next).intValue());
						} else {
							fail("No result");
						}
					}

					fedRepoConn.commit();
				}

			});

			fedRepo.shutDown();
		}
	}

}
