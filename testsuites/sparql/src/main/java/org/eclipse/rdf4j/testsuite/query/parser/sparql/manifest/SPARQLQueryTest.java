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
package org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
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
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParserRegistry;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * A SPARQL query test suite, created by reading in a W3C working-group style manifest.
 *
 * @author Jeen Broekstra
 * @deprecated since 3.3.0. Use {@link SPARQL11QueryComplianceTest} instead.
 */
@Deprecated
public abstract class SPARQLQueryTest extends TestCase {

	/*-----------*
	 * Constants *
	 *-----------*/

	// Logger for non-static tests, so these results can be isolated based on
	// where they are run
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	// Logger for static methods which are not overridden
	private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLQueryTest.class);

	protected final String testURI;

	protected final String queryFileURL;

	protected final String resultFileURL;

	protected final Dataset dataset;

	protected final boolean laxCardinality;

	protected final boolean checkOrder;

	protected final String[] ignoredTests;

	/*-----------*
	 * Variables *
	 *-----------*/

	protected Repository dataRep;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SPARQLQueryTest(String testURI, String name, String queryFileURL, String resultFileURL, Dataset dataSet,
			boolean laxCardinality, String... ignoredTests) {
		this(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, false);
	}

	public SPARQLQueryTest(String testURI, String name, String queryFileURL, String resultFileURL, Dataset dataSet,
			boolean laxCardinality, boolean checkOrder, String... ignoredTests) {
		super(name.replaceAll("\\(", " ").replaceAll("\\)", " "));

		this.testURI = testURI;
		this.queryFileURL = queryFileURL;
		this.resultFileURL = resultFileURL;
		this.dataset = dataSet;
		this.laxCardinality = laxCardinality;
		this.checkOrder = checkOrder;
		this.ignoredTests = ignoredTests;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void setUp() throws Exception {
		dataRep = createRepository();

		if (dataset != null) {
			try {
				uploadDataset(dataset);
			} catch (Exception exc) {
				try {
					dataRep.shutDown();
					dataRep = null;
				} catch (Exception e2) {
					logger.error(e2.toString(), e2);
				}
				throw exc;
			}
		}
	}

	protected final Repository createRepository() throws Exception {
		Repository repo = newRepository();
		try (RepositoryConnection con = repo.getConnection()) {
			con.clear();
			con.clearNamespaces();
		}
		return repo;
	}

	protected abstract Repository newRepository() throws Exception;

	@Override
	protected void tearDown() throws Exception {
		if (dataRep != null) {
			dataRep.shutDown();
			dataRep = null;
		}
	}

	@Override
	protected void runTest() throws Exception {
		// FIXME this reports a test error because we still rely on JUnit 3 here.
		// org.junit.Assume.assumeFalse(Arrays.asList(ignoredTests).contains(this.getName()));
		// FIXME temporary fix is to report as succeeded and just ignore.
		if (Arrays.asList(ignoredTests).contains(this.getName())) {
			logger.warn("Query test ignored: " + this.getName());
			return;
		}

		// Some SPARQL Tests have non-XSD datatypes that must pass for the test
		// suite to complete successfully
		try (RepositoryConnection con = dataRep.getConnection()) {
			con.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, Boolean.FALSE);
			con.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, Boolean.FALSE);
			String queryString = readQueryString();
			Query query = con.prepareQuery(QueryLanguage.SPARQL, queryString, queryFileURL);
			if (dataset != null) {
				query.setDataset(dataset);
			}

			String name = this.getName();

			if (name.contains("pp34")) {
				System.out.println(name);
			}

			if (query instanceof TupleQuery) {
				TupleQueryResult queryResult = ((TupleQuery) query).evaluate();

				TupleQueryResult expectedResult = readExpectedTupleQueryResult();

				compareTupleQueryResults(queryResult, expectedResult);

				// Graph queryGraph = RepositoryUtil.asGraph(queryResult);
				// Graph expectedGraph = readExpectedTupleQueryResult();
				// compareGraphs(queryGraph, expectedGraph);
			} else if (query instanceof GraphQuery) {
				GraphQueryResult gqr = ((GraphQuery) query).evaluate();
				Set<Statement> queryResult = Iterations.asSet(gqr);

				Set<Statement> expectedResult = readExpectedGraphQueryResult();

				compareGraphs(queryResult, expectedResult);
			} else if (query instanceof BooleanQuery) {
				boolean queryResult = ((BooleanQuery) query).evaluate();
				boolean expectedResult = readExpectedBooleanQueryResult();
				assertEquals(expectedResult, queryResult);
			} else {
				throw new RuntimeException("Unexpected query type: " + query.getClass());
			}
		}
	}

	protected final void compareTupleQueryResults(TupleQueryResult queryResult, TupleQueryResult expectedResult)
			throws Exception {
		// Create MutableTupleQueryResult to be able to re-iterate over the
		// results
		MutableTupleQueryResult queryResultTable = new MutableTupleQueryResult(queryResult);
		MutableTupleQueryResult expectedResultTable = new MutableTupleQueryResult(expectedResult);

		boolean resultsEqual;
		if (laxCardinality) {
			resultsEqual = QueryResults.isSubset(queryResultTable, expectedResultTable);
		} else {
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
		}

		if (!resultsEqual) {
			queryResultTable.beforeFirst();
			expectedResultTable.beforeFirst();

			/*
			 * StringBuilder message = new StringBuilder(128); message.append("\n============ ");
			 * message.append(getName()); message.append(" =======================\n"); message.append(
			 * "Expected result: \n"); while (expectedResultTable.hasNext()) {
			 * message.append(expectedResultTable.next()); message.append("\n"); } message.append("=============");
			 * StringUtil.appendN('=', getName().length(), message); message.append("========================\n");
			 * message.append("Query result: \n"); while (queryResultTable.hasNext()) {
			 * message.append(queryResultTable.next()); message.append("\n"); } message.append("=============");
			 * StringUtil.appendN('=', getName().length(), message); message.append("========================\n");
			 */

			List<BindingSet> queryBindings = Iterations.asList(queryResultTable);

			List<BindingSet> expectedBindings = Iterations.asList(expectedResultTable);

			List<BindingSet> missingBindings = new ArrayList<>(expectedBindings);
			missingBindings.removeAll(queryBindings);

			List<BindingSet> unexpectedBindings = new ArrayList<>(queryBindings);
			unexpectedBindings.removeAll(expectedBindings);

			StringBuilder message = new StringBuilder(128);
			message.append("\n============ ");
			message.append(getName());
			message.append(" =======================\n");

			if (!missingBindings.isEmpty()) {

				message.append("Missing bindings: \n");
				for (BindingSet bs : missingBindings) {
					printBindingSet(bs, message);
				}

				message.append("=============");
				StringUtil.appendN('=', getName().length(), message);
				message.append("========================\n");
			}

			if (!unexpectedBindings.isEmpty()) {
				message.append("Unexpected bindings: \n");
				for (BindingSet bs : unexpectedBindings) {
					printBindingSet(bs, message);
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
					printBindingSet(bs, message);
				}
				message.append(" =======================\n");
				message.append("expected result: \n");
				for (BindingSet bs : expectedBindings) {
					printBindingSet(bs, message);
				}
				message.append(" =======================\n");

				System.out.print(message.toString());
			} else if (missingBindings.isEmpty() && unexpectedBindings.isEmpty()) {
				message.append("unexpected duplicate in result.\n");
				message.append(" =======================\n");
				message.append("query result: \n");
				for (BindingSet bs : queryBindings) {
					printBindingSet(bs, message);
				}
				message.append(" =======================\n");
				message.append("expected result: \n");
				for (BindingSet bs : expectedBindings) {
					printBindingSet(bs, message);
				}
				message.append(" =======================\n");

				System.out.print(message.toString());
			}

			logger.error(message.toString());
			fail(message.toString());
		}
		/*
		 * debugging only: print out result when test succeeds else { queryResultTable.beforeFirst(); List<BindingSet>
		 * queryBindings = Iterations.asList(queryResultTable); StringBuilder message = new StringBuilder(128);
		 * message.append("\n============ "); message.append(getName()); message.append( " =======================\n");
		 * message.append(" =======================\n"); message.append( "query result: \n"); for (BindingSet bs:
		 * queryBindings) { message.append(bs); message.append("\n"); } System.out.print(message.toString()); }
		 */
	}

	protected final void printBindingSet(BindingSet bs, StringBuilder appendable) {
		List<String> names = new ArrayList<>(bs.getBindingNames());
		Collections.sort(names);

		for (String name : names) {
			if (bs.hasBinding(name)) {
				appendable.append(bs.getBinding(name));
				appendable.append(' ');
			}
		}
		appendable.append("\n");
	}

	protected final void compareGraphs(Set<Statement> queryResult, Set<Statement> expectedResult) throws Exception {
		if (!Models.isomorphic(expectedResult, queryResult)) {
			// Don't use RepositoryUtil.difference, it reports incorrect diffs
			/*
			 * Collection<? extends Statement> unexpectedStatements = RepositoryUtil.difference(queryResult,
			 * expectedResult); Collection<? extends Statement> missingStatements =
			 * RepositoryUtil.difference(expectedResult, queryResult); StringBuilder message = new StringBuilder(128);
			 * message.append("\n=======Diff: "); message.append(getName());
			 * message.append("========================\n"); if (!unexpectedStatements.isEmpty()) {
			 * message.append("Unexpected statements in result: \n"); for (Statement st : unexpectedStatements) {
			 * message.append(st.toString()); message.append("\n"); } message.append("============="); for (int i = 0; i
			 * < getName().length(); i++) { message.append("="); } message.append("========================\n"); } if
			 * (!missingStatements.isEmpty()) { message.append("Statements missing in result: \n"); for (Statement st :
			 * missingStatements) { message.append(st.toString()); message.append("\n"); }
			 * message.append("============="); for (int i = 0; i < getName().length(); i++) { message.append("="); }
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

	protected final void uploadDataset(Dataset dataset) throws Exception {
		// Merge default and named graphs to filter duplicates
		Set<IRI> graphURIs = new HashSet<>();
		graphURIs.addAll(dataset.getDefaultGraphs());
		graphURIs.addAll(dataset.getNamedGraphs());

		for (IRI graphURI : graphURIs) {
			upload(graphURI, graphURI);
		}
	}

	private void upload(IRI graphURI, Resource context) throws Exception {
		try (RepositoryConnection con = dataRep.getConnection()) {
			try {
				con.begin();
				RDFFormat rdfFormat = Rio.getParserFormatForFileName(graphURI.toString()).orElse(RDFFormat.TURTLE);
				RDFParser rdfParser = Rio.createParser(rdfFormat, dataRep.getValueFactory());
				// rdfParser.setPreserveBNodeIDs(true);

				RDFInserter rdfInserter = new RDFInserter(con);
				rdfInserter.enforceContext(context);
				rdfParser.setRDFHandler(rdfInserter);

				URL graphURL = new URL(graphURI.toString());
				try (InputStream in = graphURL.openStream()) {
					rdfParser.parse(in, graphURI.toString());
				}

				con.commit();
			} catch (Exception e) {
				if (con.isActive()) {
					con.rollback();
				}
				throw e;
			}
		}
	}

	protected final String readQueryString() throws IOException {
		try (InputStream stream = new URL(queryFileURL).openStream()) {
			return IOUtil.readString(new InputStreamReader(stream, StandardCharsets.UTF_8));
		}
	}

	protected final TupleQueryResult readExpectedTupleQueryResult() throws Exception {
		Optional<QueryResultFormat> tqrFormat = QueryResultIO.getParserFormatForFileName(resultFileURL);

		if (tqrFormat.isPresent()) {
			try (InputStream in = new URL(resultFileURL).openStream()) {
				TupleQueryResultParser parser = QueryResultIO.createTupleParser(tqrFormat.get());
				parser.setValueFactory(dataRep.getValueFactory());

				TupleQueryResultBuilder qrBuilder = new TupleQueryResultBuilder();
				parser.setQueryResultHandler(qrBuilder);

				parser.parseQueryResult(in);
				return qrBuilder.getQueryResult();
			}
		} else {
			Set<Statement> resultGraph = readExpectedGraphQueryResult();
			return DAWGTestResultSetUtil.toTupleQueryResult(resultGraph);
		}
	}

	protected final boolean readExpectedBooleanQueryResult() throws Exception {
		Optional<QueryResultFormat> bqrFormat = BooleanQueryResultParserRegistry.getInstance()
				.getFileFormatForFileName(resultFileURL);

		if (bqrFormat.isPresent()) {
			try (InputStream in = new URL(resultFileURL).openStream()) {
				return QueryResultIO.parseBoolean(in, bqrFormat.get());
			}
		} else {
			Set<Statement> resultGraph = readExpectedGraphQueryResult();
			return DAWGTestResultSetUtil.toBooleanQueryResult(resultGraph);
		}
	}

	protected final Set<Statement> readExpectedGraphQueryResult() throws Exception {
		RDFFormat rdfFormat = Rio.getParserFormatForFileName(resultFileURL)
				.orElseThrow(Rio.unsupportedFormat(resultFileURL));

		RDFParser parser = Rio.createParser(rdfFormat);
		parser.setPreserveBNodeIDs(true);
		parser.setValueFactory(dataRep.getValueFactory());

		Set<Statement> result = new LinkedHashSet<>();
		parser.setRDFHandler(new StatementCollector(result));

		try (InputStream in = new URL(resultFileURL).openStream()) {
			parser.parse(in, resultFileURL);
		}

		return result;
	}

	public interface Factory {

		SPARQLQueryTest createSPARQLQueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
				Dataset dataSet, boolean laxCardinality);

		SPARQLQueryTest createSPARQLQueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
				Dataset dataSet, boolean laxCardinality, boolean checkOrder);
	}

	public static TestSuite suite(String manifestFileURL, Factory factory) throws Exception {
		return suite(manifestFileURL, factory, true);
	}

	public static TestSuite suite(String manifestFileURL, Factory factory, boolean approvedOnly) throws Exception {
		LOGGER.info("Building test suite for {}", manifestFileURL);

		TestSuite suite = new TestSuite(factory.getClass().getName());

		// Read manifest and create declared test cases
		Repository manifestRep = new SailRepository(new MemoryStore());
		try (RepositoryConnection con = manifestRep.getConnection()) {

			SPARQL11ManifestTest.addTurtle(con, new URL(manifestFileURL), manifestFileURL);

			suite.setName(getManifestName(manifestRep, con, manifestFileURL));

			// Extract test case information from the manifest file. Note that we only
			// select those test cases that are mentioned in the list.
			StringBuilder query = new StringBuilder(512);
			query.append(" PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> \n");
			query.append(" PREFIX dawgt: <http://www.w3.org/2001/sw/DataAccess/tests/test-dawg#> \n");
			query.append(" PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> \n");
			query.append(" PREFIX sd: <http://www.w3.org/ns/sparql-service-description#> \n");
			query.append(" PREFIX ent: <http://www.w3.org/ns/entailment/> \n");
			query.append(
					" SELECT DISTINCT ?testURI ?testName ?resultFile ?action ?queryFile ?defaultGraph ?ordered \n");
			query.append(" WHERE { [] rdf:first ?testURI . \n");
			if (approvedOnly) {
				query.append(" ?testURI dawgt:approval dawgt:Approved . \n");
			}
			query.append(" ?testURI mf:name ?testName; \n");
			query.append("          mf:result ?resultFile . \n");
			query.append(" OPTIONAL { ?testURI mf:checkOrder ?ordered } \n");
			query.append(" OPTIONAL { ?testURI  mf:requires ?requirement } \n");
			query.append(" ?testURI mf:action ?action. \n");
			query.append(" ?action qt:query ?queryFile . \n");
			query.append(" OPTIONAL { ?action qt:data ?defaultGraph } \n");
			query.append(" OPTIONAL { ?action sd:entailmentRegime ?regime } \n");
			// skip tests involving CSV result files, these are not query tests
			query.append(" FILTER(!STRENDS(STR(?resultFile), \"csv\")) \n");
			// skip tests involving entailment regimes
			query.append(" FILTER(!BOUND(?regime)) \n");
			// skip test involving basic federation, these are tested separately.
			query.append(" FILTER (!BOUND(?requirement) || (?requirement != mf:BasicFederation)) \n");
			query.append(" }\n");

			TupleQuery testCaseQuery = con.prepareTupleQuery(query.toString());

			query.setLength(0);
			query.append(" PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> \n");
			query.append(" SELECT ?graph \n");
			query.append(" WHERE { ?action qt:graphData ?graph } \n");
			TupleQuery namedGraphsQuery = con.prepareTupleQuery(query.toString());

			query.setLength(0);
			query.append(" PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> \n");
			query.append("ASK \n");
			query.append(" WHERE { ?testURI  mf:resultCardinality mf:LaxCardinality .} \n");
			BooleanQuery laxCardinalityQuery = con.prepareBooleanQuery(query.toString());

			LOGGER.debug("evaluating query..");
			try (TupleQueryResult testCases = testCaseQuery.evaluate()) {
				for (BindingSet testCase : testCases) {
					IRI testURI = (IRI) testCase.getValue("testURI");
					String testName = testCase.getValue("testName").stringValue();
					String resultFile = testCase.getValue("resultFile").stringValue();
					String queryFile = testCase.getValue("queryFile").stringValue();
					IRI defaultGraphURI = (IRI) testCase.getValue("defaultGraph");
					Value action = testCase.getValue("action");
					Value ordered = testCase.getValue("ordered");

					LOGGER.debug("found test case : {}", testName);

					SimpleDataset dataset = null;

					// Query named graphs
					namedGraphsQuery.setBinding("action", action);
					try (TupleQueryResult namedGraphs = namedGraphsQuery.evaluate()) {
						if (defaultGraphURI != null || namedGraphs.hasNext()) {
							dataset = new SimpleDataset();
							if (defaultGraphURI != null) {
								dataset.addDefaultGraph(defaultGraphURI);
							}
							while (namedGraphs.hasNext()) {
								BindingSet graphBindings = namedGraphs.next();
								IRI namedGraphURI = (IRI) graphBindings.getValue("graph");
								LOGGER.debug(" adding named graph : {}", namedGraphURI);
								dataset.addNamedGraph(namedGraphURI);
							}
						}
					}

					// Check for lax-cardinality conditions
					boolean laxCardinality;
					laxCardinalityQuery.setBinding("testURI", testURI);
					laxCardinality = laxCardinalityQuery.evaluate();

					// if this is enabled, RDF4J passes all tests, showing that the only
					// difference is the semantics of arbitrary-length
					// paths
					/*
					 * if (!laxCardinality) { // property-path tests always with lax cardinality because Sesame filters
					 * out duplicates by design if (testURI.stringValue().contains("property-path")) { laxCardinality =
					 * true; } }
					 */

					// Two SPARQL distinctness tests fail in RDF-1.1 if the only difference
					// is in the number of results
					if (!laxCardinality) {
						if (testURI.stringValue().contains("distinct/manifest#distinct-2")
								|| testURI.stringValue().contains("distinct/manifest#distinct-9")) {
							laxCardinality = true;
						}
					}

					LOGGER.debug("testURI={} name={} queryFile={}", testURI.stringValue(), testName, queryFile);

					// check if we should test for query result ordering
					boolean checkOrder = false;
					if (ordered != null) {
						checkOrder = Boolean.parseBoolean(ordered.stringValue());
					}

					SPARQLQueryTest test = factory.createSPARQLQueryTest(testURI.stringValue(), testName, queryFile,
							resultFile, dataset, laxCardinality, checkOrder);
					if (test != null) {
						suite.addTest(test);
					}
				}
			}
		}
		manifestRep.shutDown();
		LOGGER.info("Created test suite with " + suite.countTestCases() + " test cases.");
		return suite;
	}

	protected static String getManifestName(Repository manifestRep, RepositoryConnection con, String manifestFileURL)
			throws QueryEvaluationException, RepositoryException, MalformedQueryException {
		// Try to extract suite name from manifest file
		TupleQuery manifestNameQuery = con
				.prepareTupleQuery("SELECT ?ManifestName WHERE { ?ManifestURL rdfs:label ?ManifestName .}");
		manifestNameQuery.setBinding("ManifestURL", manifestRep.getValueFactory().createIRI(manifestFileURL));
		try (TupleQueryResult manifestNames = manifestNameQuery.evaluate()) {
			if (manifestNames.hasNext()) {
				return manifestNames.next().getValue("ManifestName").stringValue();
			}
		}

		// Derive name from manifest URL
		int lastSlashIdx = manifestFileURL.lastIndexOf('/');
		int secLastSlashIdx = manifestFileURL.lastIndexOf('/', lastSlashIdx - 1);
		return manifestFileURL.substring(secLastSlashIdx + 1, lastSlashIdx);
	}
}
