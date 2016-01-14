/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.dawg.DAWGTestResultSetUtil;
import org.eclipse.rdf4j.query.impl.MutableTupleQueryResult;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RDFParser.DatatypeHandling;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test suite for evaluation of SPARQL queries involving SERVICE clauses. The
 * test suite starts up an embedded Jetty server running Sesame, which functions
 * as the SPARQL endpoint to test against. The test is configured to execute the
 * W3C service tests located in
 * sesame-sparql-testsuite/src/main/resources/testcases-service
 * 
 * @author Jeen Broekstra
 * @author Andreas Schwarte
 */
public class SPARQLServiceEvaluationTest extends TestCase {

	static final Logger logger = LoggerFactory.getLogger(SPARQLServiceEvaluationTest.class);

	/**
	 * The maximal number of endpoints occurring in a (single) test case
	 */
	protected static final int MAX_ENDPOINTS = 3;

	private SPARQLEmbeddedServer server;

	private SailRepository localRepository;

	private List<HTTPRepository> remoteRepositories;

	public SPARQLServiceEvaluationTest() {

	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		// set up the server: the maximal number of endpoints must be known
		List<String> repositoryIds = new ArrayList<String>(MAX_ENDPOINTS);
		for (int i = 1; i <= MAX_ENDPOINTS; i++)
			repositoryIds.add("endpoint" + i);
		server = new SPARQLEmbeddedServer(repositoryIds);

		try {
			server.start();
		}
		catch (Exception e) {
			server.stop();
			throw e;
		}

		remoteRepositories = new ArrayList<HTTPRepository>(MAX_ENDPOINTS);
		for (int i = 1; i <= MAX_ENDPOINTS; i++) {
			HTTPRepository r = new HTTPRepository(getRepositoryUrl(i));
			r.initialize();
			remoteRepositories.add(r);
		}

		localRepository = new SailRepository(new MemoryStore());
		localRepository.initialize();
	}

	/**
	 * Get the repository url, initialized repositories are called endpoint1
	 * endpoint2 .. endpoint%MAX_ENDPOINTS%
	 * 
	 * @param i
	 *        the index of the repository, starting with 1
	 * @return
	 */
	protected String getRepositoryUrl(int i) {
		return server.getRepositoryUrl("endpoint" + i);
	}

	/**
	 * Get the repository, initialized repositories are called endpoint1
	 * endpoint2 .. endpoint%MAX_ENDPOINTS%
	 * 
	 * @param i
	 *        the index of the repository, starting with 1
	 * @return
	 */
	public HTTPRepository getRepository(int i) {
		return remoteRepositories.get(i - 1);
	}

	/**
	 * Prepare a particular test, and load the specified data. Note: the
	 * repositories are cleared before loading data
	 * 
	 * @param localData
	 *        a local data file that is added to local repository, use null if
	 *        there is no local data
	 * @param endpointData
	 *        a list of endpoint data files, dataFile at index is loaded to
	 *        endpoint%i%, use empty list for no remote data
	 * @throws Exception
	 */
	protected void prepareTest(String localData, List<String> endpointData)
		throws Exception
	{

		if (endpointData.size() > MAX_ENDPOINTS)
			throw new RuntimeException("MAX_ENDPOINTs to low, " + endpointData.size()
					+ " repositories needed. Adjust configuration");

		if (localData != null) {
			loadDataSet(localRepository, localData);
		}

		int i = 1; // endpoint id, start with 1
		for (String s : endpointData) {
			loadDataSet(getRepository(i++), s);
		}

	}

	/**
	 * Load a dataset. Note: the repositories are cleared before loading data
	 * 
	 * @param rep
	 * @param datasetFile
	 * @throws RDFParseException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	protected void loadDataSet(Repository rep, String datasetFile)
		throws RDFParseException, RepositoryException, IOException
	{
		logger.debug("loading dataset...");
		InputStream dataset = SPARQLServiceEvaluationTest.class.getResourceAsStream(datasetFile);

		if (dataset == null)
			throw new IllegalArgumentException("Datasetfile " + datasetFile + " not found.");

		RepositoryConnection con = rep.getConnection();
		try {
			con.clear();
			con.add(dataset, "",
					Rio.getParserFormatForFileName(datasetFile).orElseThrow(Rio.unsupportedFormat(datasetFile)));
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
		try {
			localRepository.shutDown();
		}
		finally {
			server.stop();
		}
	}

	@Test
	public void testSimpleServiceQuery()
		throws RepositoryException
	{
		// test setup
		String EX_NS = "http://example.org/";
		ValueFactory f = localRepository.getValueFactory();
		IRI bob = f.createIRI(EX_NS, "bob");
		IRI alice = f.createIRI(EX_NS, "alice");
		IRI william = f.createIRI(EX_NS, "william");

		// clears the repository and adds new data
		try {
			prepareTest("/testcases-service/simple-default-graph.ttl",
					Arrays.asList("/testcases-service/simple.ttl"));
		}
		catch (Exception e1) {
			fail(e1.getMessage());
		}

		StringBuilder qb = new StringBuilder();
		qb.append(" SELECT * \n");
		qb.append(" WHERE { \n");
		qb.append("     SERVICE <" + getRepositoryUrl(1) + "> { \n");
		qb.append("             ?X <" + FOAF.NAME + "> ?Y \n ");
		qb.append("     } \n ");
		qb.append("     ?X a <" + FOAF.PERSON + "> . \n");
		qb.append(" } \n");

		RepositoryConnection conn = localRepository.getConnection();
		try {
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
		finally {
			conn.close();
		}
	}

	@Test
	public void test1()
		throws Exception
	{
		prepareTest("/testcases-service/data01.ttl", Arrays.asList("/testcases-service/data01endpoint.ttl"));
		execute("/testcases-service/service01.rq", "/testcases-service/service01.srx", false);
	}

	@Test
	public void test2()
		throws Exception
	{
		prepareTest(null,
				Arrays.asList("/testcases-service/data02endpoint1.ttl", "/testcases-service/data02endpoint2.ttl"));
		execute("/testcases-service/service02.rq", "/testcases-service/service02.srx", false);
	}

	@Test
	public void test3()
		throws Exception
	{
		prepareTest(null,
				Arrays.asList("/testcases-service/data03endpoint1.ttl", "/testcases-service/data03endpoint2.ttl"));
		execute("/testcases-service/service03.rq", "/testcases-service/service03.srx", false);
	}

	@Test
	public void test4()
		throws Exception
	{
		prepareTest("/testcases-service/data04.ttl", Arrays.asList("/testcases-service/data04endpoint.ttl"));
		execute("/testcases-service/service04.rq", "/testcases-service/service04.srx", false);
	}

	@Test
	public void test5()
		throws Exception
	{
		prepareTest("/testcases-service/data05.ttl",
				Arrays.asList("/testcases-service/data05endpoint1.ttl", "/testcases-service/data05endpoint2.ttl"));
		execute("/testcases-service/service05.rq", "/testcases-service/service05.srx", false);
	}

	@Test
	public void test6()
		throws Exception
	{
		prepareTest(null, Arrays.asList("/testcases-service/data06endpoint1.ttl"));
		execute("/testcases-service/service06.rq", "/testcases-service/service06.srx", false);
	}

	@Test
	public void test7()
		throws Exception
	{
		// clears the repository and adds new data + execute
		prepareTest("/testcases-service/data07.ttl", Collections.<String> emptyList());
		execute("/testcases-service/service07.rq", "/testcases-service/service07.srx", false);
	}

	@Test
	public void test8()
		throws Exception
	{
		/* test where the SERVICE expression is to be evaluated as ASK request */
		prepareTest("/testcases-service/data08.ttl", Arrays.asList("/testcases-service/data08endpoint.ttl"));
		execute("/testcases-service/service08.rq", "/testcases-service/service08.srx", false);
	}

	@Test
	public void test9()
		throws Exception
	{
		/* test where the service endpoint is bound at runtime through BIND */
		prepareTest(null, Arrays.asList("/testcases-service/data09endpoint.ttl"));
		execute("/testcases-service/service09.rq", "/testcases-service/service09.srx", false);
	}

	@Test
	public void test10()
		throws Exception
	{
		/* test how we deal with blank node */
		prepareTest("/testcases-service/data10.ttl", Arrays.asList("/testcases-service/data10endpoint.ttl"));
		execute("/testcases-service/service10.rq", "/testcases-service/service10.srx", false);
	}

	@Test
	public void test11()
		throws Exception
	{
		/* test vectored join with more intermediate results */
		// clears the repository and adds new data + execute
		prepareTest("/testcases-service/data11.ttl", Arrays.asList("/testcases-service/data11endpoint.ttl"));
		execute("/testcases-service/service11.rq", "/testcases-service/service11.srx", false);
	}

	// test on remote DBpedia endpoint disabled. Only enable for manual testing,
	// should not be enabled for
	// Surefire or Hudson.
	// /**
	// * This is a manual test to see the Fallback in action. Query asks
	// * DBpedia, which does not support BINDINGS
	// *
	// * @throws Exception
	// */
	// public void testFallbackWithDBpedia() throws Exception {
	// /* test vectored join with more intermediate results */
	// // clears the repository and adds new data + execute
	// prepareTest("/testcases-service/data12.ttl",
	// Collections.<String>emptyList());
	// execute("/testcases-service/service12.rq",
	// "/testcases-service/service12.srx", false);
	// }

	@Test
	public void test13()
		throws Exception
	{
		/* test for bug SES-899: cross product is required */
		prepareTest(null, Arrays.asList("/testcases-service/data13.ttl"));
		execute("/testcases-service/service13.rq", "/testcases-service/service13.srx", false);
	}

	@Test
	public void testEmptyServiceBlock()
		throws Exception
	{
		/* test for bug SES-900: nullpointer for empty service block */
		prepareTest(null, Arrays.asList("/testcases-service/data13.ttl"));
		execute("/testcases-service/service14.rq", "/testcases-service/service14.srx", false);
	}

	@Test
	public void testNotProjectedCount()
		throws Exception
	{
		/* test projection of subqueries - SES-1000 */
		prepareTest(null, Arrays.asList("/testcases-service/data17endpoint1.ttl"));
		execute("/testcases-service/service17.rq", "/testcases-service/service17.srx", false);
	}

	@Test
	public void testNonAsciiCharHandling()
		throws Exception
	{
		/* SES-1056 */
		prepareTest(null, Arrays.asList("/testcases-service/data18endpoint1.rdf"));
		execute("/testcases-service/service18.rq", "/testcases-service/service18.srx", false);
	}

	/**
	 * Execute a testcase, both queryFile and expectedResultFile must be files
	 * located on the class path.
	 * 
	 * @param queryFile
	 * @param expectedResultFile
	 * @param checkOrder
	 * @throws Exception
	 */
	private void execute(String queryFile, String expectedResultFile, boolean checkOrder)
		throws Exception
	{
		RepositoryConnection conn = localRepository.getConnection();
		String queryString = readQueryString(queryFile);

		try {
			Query query = conn.prepareQuery(QueryLanguage.SPARQL, queryString);

			if (query instanceof TupleQuery) {
				TupleQueryResult queryResult = ((TupleQuery)query).evaluate();

				TupleQueryResult expectedResult = readExpectedTupleQueryResult(expectedResultFile);

				compareTupleQueryResults(queryResult, expectedResult, checkOrder);

			}
			else if (query instanceof GraphQuery) {
				GraphQueryResult gqr = ((GraphQuery)query).evaluate();
				Set<Statement> queryResult = Iterations.asSet(gqr);

				Set<Statement> expectedResult = readExpectedGraphQueryResult(expectedResultFile);

				compareGraphs(queryResult, expectedResult);

			}
			else if (query instanceof BooleanQuery) {
				// TODO implement if needed
				throw new RuntimeException("Not yet supported " + query.getClass());
			}
			else {
				throw new RuntimeException("Unexpected query type: " + query.getClass());
			}
		}
		finally {
			conn.close();
		}
	}

	/**
	 * Read the query string from the specified resource
	 * 
	 * @param queryResource
	 * @return
	 * @throws RepositoryException
	 * @throws IOException
	 */
	private String readQueryString(String queryResource)
		throws RepositoryException, IOException
	{
		InputStream stream = SPARQLServiceEvaluationTest.class.getResourceAsStream(queryResource);
		try {
			return IOUtil.readString(new InputStreamReader(stream, "UTF-8"));
		}
		finally {
			stream.close();
		}
	}

	/**
	 * Read the expected tuple query result from the specified resource
	 * 
	 * @param queryResource
	 * @return
	 * @throws RepositoryException
	 * @throws IOException
	 */
	private TupleQueryResult readExpectedTupleQueryResult(String resultFile)
		throws Exception
	{
		Optional<QueryResultFormat> tqrFormat = QueryResultIO.getParserFormatForFileName(resultFile);

		if (tqrFormat.isPresent()) {
			InputStream in = SPARQLServiceEvaluationTest.class.getResourceAsStream(resultFile);
			try {
				TupleQueryResultParser parser = QueryResultIO.createTupleParser(tqrFormat.get());
				parser.setValueFactory(SimpleValueFactory.getInstance());

				TupleQueryResultBuilder qrBuilder = new TupleQueryResultBuilder();
				parser.setQueryResultHandler(qrBuilder);

				parser.parseQueryResult(in);
				return qrBuilder.getQueryResult();
			}
			finally {
				in.close();
			}
		}
		else {
			Set<Statement> resultGraph = readExpectedGraphQueryResult(resultFile);
			return DAWGTestResultSetUtil.toTupleQueryResult(resultGraph);
		}
	}

	/**
	 * Read the expected graph query result from the specified resource
	 * 
	 * @param resultFile
	 * @return
	 * @throws Exception
	 */
	private Set<Statement> readExpectedGraphQueryResult(String resultFile)
		throws Exception
	{
		RDFFormat rdfFormat = Rio.getParserFormatForFileName(resultFile).orElseThrow(
				Rio.unsupportedFormat(resultFile));

		RDFParser parser = Rio.createParser(rdfFormat);
		parser.setDatatypeHandling(DatatypeHandling.IGNORE);
		parser.setPreserveBNodeIDs(true);
		parser.setValueFactory(SimpleValueFactory.getInstance());

		Set<Statement> result = new LinkedHashSet<Statement>();
		parser.setRDFHandler(new StatementCollector(result));

		InputStream in = SPARQLServiceEvaluationTest.class.getResourceAsStream(resultFile);
		try {
			parser.parse(in, null); // TODO check
		}
		finally {
			in.close();
		}

		return result;
	}

	/**
	 * Compare two tuple query results
	 * 
	 * @param queryResult
	 * @param expectedResult
	 * @param checkOrder
	 * @throws Exception
	 */
	private void compareTupleQueryResults(TupleQueryResult queryResult, TupleQueryResult expectedResult,
			boolean checkOrder)
		throws Exception
	{
		// Create MutableTupleQueryResult to be able to re-iterate over the
		// results
		MutableTupleQueryResult queryResultTable = new MutableTupleQueryResult(queryResult);
		MutableTupleQueryResult expectedResultTable = new MutableTupleQueryResult(expectedResult);

		boolean resultsEqual;

		resultsEqual = QueryResults.equals(queryResultTable, expectedResultTable);

		if (checkOrder) {
			// also check the order in which solutions occur.
			queryResultTable.beforeFirst();
			expectedResultTable.beforeFirst();

			while (queryResultTable.hasNext()) {
				BindingSet bs = queryResultTable.next();
				BindingSet expectedBs = expectedResultTable.next();

				if (!bs.equals(expectedBs)) {
					resultsEqual = false;
					break;
				}
			}
		}

		if (!resultsEqual) {
			queryResultTable.beforeFirst();
			expectedResultTable.beforeFirst();

			/*
			 * StringBuilder message = new StringBuilder(128);
			 * message.append("\n============ "); message.append(getName());
			 * message.append(" =======================\n");
			 * message.append("Expected result: \n"); while
			 * (expectedResultTable.hasNext()) {
			 * message.append(expectedResultTable.next()); message.append("\n"); }
			 * message.append("============="); StringUtil.appendN('=',
			 * getName().length(), message);
			 * message.append("========================\n"); message.append("Query
			 * result: \n"); while (queryResultTable.hasNext()) {
			 * message.append(queryResultTable.next()); message.append("\n"); }
			 * message.append("============="); StringUtil.appendN('=',
			 * getName().length(), message);
			 * message.append("========================\n");
			 */

			List<BindingSet> queryBindings = Iterations.asList(queryResultTable);

			List<BindingSet> expectedBindings = Iterations.asList(expectedResultTable);

			List<BindingSet> missingBindings = new ArrayList<BindingSet>(expectedBindings);
			missingBindings.removeAll(queryBindings);

			List<BindingSet> unexpectedBindings = new ArrayList<BindingSet>(queryBindings);
			unexpectedBindings.removeAll(expectedBindings);

			StringBuilder message = new StringBuilder(128);
			message.append("\n============ ");
			message.append(getName());
			message.append(" =======================\n");

			if (!missingBindings.isEmpty()) {

				message.append("Missing bindings: \n");
				for (BindingSet bs : missingBindings) {
					message.append(bs);
					message.append("\n");
				}

				message.append("=============");
				StringUtil.appendN('=', getName().length(), message);
				message.append("========================\n");
			}

			if (!unexpectedBindings.isEmpty()) {
				message.append("Unexpected bindings: \n");
				for (BindingSet bs : unexpectedBindings) {
					message.append(bs);
					message.append("\n");
				}

				message.append("=============");
				StringUtil.appendN('=', getName().length(), message);
				message.append("========================\n");
			}

			if (checkOrder && missingBindings.isEmpty() && unexpectedBindings.isEmpty()) {
				message.append("Results are not in expected order.\n");
				message.append(" =======================\n");
				message.append("query result: \n");
				for (BindingSet bs : queryBindings) {
					message.append(bs);
					message.append("\n");
				}
				message.append(" =======================\n");
				message.append("expected result: \n");
				for (BindingSet bs : expectedBindings) {
					message.append(bs);
					message.append("\n");
				}
				message.append(" =======================\n");

				System.out.print(message.toString());
			}

			logger.error(message.toString());
			fail(message.toString());
		}
		/* debugging only: print out result when test succeeds 
		else {
			queryResultTable.beforeFirst();
		
			List<BindingSet> queryBindings = Iterations.asList(queryResultTable);
			StringBuilder message = new StringBuilder(128);
		
			message.append("\n============ ");
			message.append(getName());
			message.append(" =======================\n");
		
			message.append(" =======================\n");
			message.append("query result: \n");
			for (BindingSet bs: queryBindings) {
				message.append(bs);
				message.append("\n");
			}
			
			System.out.print(message.toString());
		}
		*/
	}

	/**
	 * Compare two graphs
	 * 
	 * @param queryResult
	 * @param expectedResult
	 * @throws Exception
	 */
	private void compareGraphs(Set<Statement> queryResult, Set<Statement> expectedResult)
		throws Exception
	{
		if (!Models.isomorphic(expectedResult, queryResult)) {
			// Don't use RepositoryUtil.difference, it reports incorrect diffs
			/*
			 * Collection<? extends Statement> unexpectedStatements =
			 * RepositoryUtil.difference(queryResult, expectedResult); Collection<?
			 * extends Statement> missingStatements =
			 * RepositoryUtil.difference(expectedResult, queryResult);
			 * StringBuilder message = new StringBuilder(128);
			 * message.append("\n=======Diff: "); message.append(getName());
			 * message.append("========================\n"); if
			 * (!unexpectedStatements.isEmpty()) { message.append("Unexpected
			 * statements in result: \n"); for (Statement st :
			 * unexpectedStatements) { message.append(st.toString());
			 * message.append("\n"); } message.append("============="); for (int i =
			 * 0; i < getName().length(); i++) { message.append("="); }
			 * message.append("========================\n"); } if
			 * (!missingStatements.isEmpty()) { message.append("Statements missing
			 * in result: \n"); for (Statement st : missingStatements) {
			 * message.append(st.toString()); message.append("\n"); }
			 * message.append("============="); for (int i = 0; i <
			 * getName().length(); i++) { message.append("="); }
			 * message.append("========================\n"); }
			 */
			StringBuilder message = new StringBuilder(128);
			message.append("\n============ ");
			message.append(getName());
			message.append(" =======================\n");
			message.append("Expected result: \n");
			for (Statement st : expectedResult) {
				message.append(st.toString());
				message.append("\n");
			}
			message.append("=============");
			StringUtil.appendN('=', getName().length(), message);
			message.append("========================\n");

			message.append("Query result: \n");
			for (Statement st : queryResult) {
				message.append(st.toString());
				message.append("\n");
			}
			message.append("=============");
			StringUtil.appendN('=', getName().length(), message);
			message.append("========================\n");

			logger.error(message.toString());
			fail(message.toString());
		}
	}

}
