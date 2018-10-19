/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverImpl;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPMemServer;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test suite for evaluation of SPARQL queries involving SERVICE clauses. The test suite starts up an embedded
 * Jetty server running Sesame, which functions as the SPARQL endpoint to test against.
 * 
 * @author Jeen Broekstra
 */
public class SPARQLServiceEvaluationTest {

	static final Logger logger = LoggerFactory.getLogger(SPARQLServiceEvaluationTest.class);

	private static HTTPMemServer server;

	private HTTPRepository remoteRepository;

	private SailRepository localRepository;

	private ValueFactory f;

	private IRI bob;

	private IRI alice;

	private IRI william;

	protected static final String EX_NS = "http://example.org/";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void startServer()
		throws Exception
	{
		server = new HTTPMemServer();

		try {
			server.start();
		}
		catch (Exception e) {
			server.stop();
			throw e;
		}
	}

	@Before
	public void setUp()
		throws Exception
	{
		remoteRepository = new HTTPRepository(HTTPMemServer.REPOSITORY_URL);
		remoteRepository.initialize();
		loadDataSet(remoteRepository, "/testdata-query/graph1.ttl");
		loadDataSet(remoteRepository, "/testdata-query/graph2.ttl");

		localRepository = new SailRepository(new MemoryStore());
		localRepository.initialize();

		prepareLocalRepository();
	}

	private void prepareLocalRepository()
		throws IOException
	{
		loadDataSet(localRepository, "/testdata-query/defaultgraph.ttl");

		f = localRepository.getValueFactory();

		bob = f.createIRI(EX_NS, "bob");
		alice = f.createIRI(EX_NS, "alice");
		william = f.createIRI(EX_NS, "william");
	}

	protected void loadDataSet(Repository rep, String datasetFile)
		throws RDFParseException, RepositoryException, IOException
	{
		logger.debug("loading dataset...");
		InputStream dataset = SPARQLServiceEvaluationTest.class.getResourceAsStream(datasetFile);

		RepositoryConnection con = rep.getConnection();
		try {
			con.add(dataset, "", Rio.getParserFormatForFileName(datasetFile).orElseThrow(
					Rio.unsupportedFormat(datasetFile)));
		}
		finally {
			dataset.close();
			con.close();
		}
		logger.debug("dataset loaded.");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown()
		throws Exception
	{
		localRepository.shutDown();
	}

	@AfterClass
	public static void stopServer()
		throws Exception
	{
		server.stop();
		server = null;
	}

	@Test
	public void testSimpleServiceQuery()
		throws RepositoryException
	{
		StringBuilder qb = new StringBuilder();
		qb.append(" SELECT * \n");
		qb.append(" WHERE { \n");
		qb.append("     SERVICE <" + HTTPMemServer.REPOSITORY_URL + "> { \n");
		qb.append("             ?X <" + FOAF.NAME + "> ?Y \n ");
		qb.append("     } \n ");
		qb.append("     ?X a <" + FOAF.PERSON + "> . \n");
		qb.append(" } \n");

		try (RepositoryConnection conn = localRepository.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb.toString());

			TupleQueryResult tqr = tq.evaluate();

			assertNotNull(tqr);
			assertTrue(tqr.hasNext());

			int count = 0;
			while (tqr.hasNext()) {
				BindingSet bs = tqr.next();
				count++;

				Value x = bs.getValue("X");
				Value y = bs.getValue("Y");

				assertFalse(william.equals(x));

				assertTrue(bob.equals(x) || alice.equals(x));
				if (bob.equals(x)) {
					f.createLiteral("Bob").equals(y);
				}
				else if (alice.equals(x)) {
					f.createLiteral("Alice").equals(y);
				}
			}

			assertEquals(2, count);

		}
		catch (MalformedQueryException e) {
			fail(e.getMessage());
		}
		catch (QueryEvaluationException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * The provided FederatedServiceResolver should finds it way to the {@link EvaluationStrategy}
	 */
	@Test
	public void testRepositoryConfigurationSetup()
		throws Exception
	{
		tearDown();
		MemoryStoreFactory factory = new MemoryStoreFactory();
		MemoryStoreConfig config = new MemoryStoreConfig();
		config.setEvaluationStrategyFactoryClassName(StrictEvaluationStrategyFactory.class.getName());
		Sail sail = factory.getSail(config);
		localRepository = new SailRepository(sail);
		localRepository.setFederatedServiceResolver(new FederatedServiceResolverImpl());
		localRepository.initialize();
		prepareLocalRepository();
		testSimpleServiceQuery();
	}
}
