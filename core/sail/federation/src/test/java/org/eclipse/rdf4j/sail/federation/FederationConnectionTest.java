/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnectionWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public class FederationConnectionTest {

	@Test
	public void testSize()
		throws Exception
	{
		Federation federation = new Federation();

		SailRepository repository = new SailRepository(new MemoryStore());
		federation.addMember(repository);

		federation.initialize();
		try {
			SailConnection connection = federation.getConnection();
			try {
				assertEquals("Should get size", 0, connection.size());

				connection.begin();
				assertEquals("Should get size", 0, connection.size());

				connection.addStatement(OWL.CLASS, RDFS.COMMENT, RDF.REST);
				assertEquals("Should get size", 1, connection.size());

				connection.commit();
				assertEquals("Should get size", 1, connection.size());
			}
			finally {
				connection.close();
			}
		}
		finally {
			federation.shutDown();
		}
	}

	@Test
	public void testSizeWithInferredStatements()
		throws Exception
	{
		Federation federation = new Federation();

		SailRepository repository = new SailRepository(new TestInferencer(new MemoryStore()));

		federation.addMember(repository);

		federation.initialize();
		try {
			SailConnection connection = federation.getConnection();
			try {
				connection.begin();
				connection.addStatement(OWL.CLASS, RDFS.COMMENT, RDF.REST);
				connection.commit();

				assertHasStatement("Should find explicit statement", OWL.CLASS, RDFS.COMMENT, RDF.REST,
						connection);
				assertHasStatement("Should find inferred statement", OWL.THING, RDFS.COMMENT, RDF.ALT,
						connection);

				assertEquals("Should get explicit statement size", 1, connection.size());
			}
			finally {
				connection.close();
			}
		}
		finally {
			federation.shutDown();
		}
	}

	private static void assertHasStatement(String message, Resource subject, URI predicate, Value object,
			SailConnection connection)
		throws SailException
	{
		CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(subject,
				(IRI)predicate, object, true);
		try {
			assertTrue(message, statements.hasNext());
		}
		finally {
			statements.close();
		}
	}

	public static class TestInferencer extends NotifyingSailWrapper {

		public TestInferencer(NotifyingSail baseSail) {
			super(baseSail);
		}

		@Override
		public TestInferencerConnection getConnection()
			throws SailException
		{
			try {
				return new TestInferencerConnection((InferencerConnection)super.getConnection());
			}
			catch (ClassCastException e) {
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
			public void flushUpdates()
				throws SailException
			{
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

}
