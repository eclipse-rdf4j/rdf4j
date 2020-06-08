/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
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
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.Query;
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
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParser.DatatypeHandling;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A test suite that runs the W3C Approved SPARQL 1.0 query tests.
 *
 * @author Jeen Broekstra
 *
 * @see https://www.w3.org/2009/sparql/docs/tests/
 */
@RunWith(Parameterized.class)
public abstract class SPARQL10QueryComplianceTest {

	private static final Logger logger = LoggerFactory.getLogger(SPARQL10QueryComplianceTest.class);

	private static final String[] defaultIgnoredTests = {
			// incompatible with SPARQL 1.1 - syntax for decimals was modified
			"Basic - Term 6",
			// incompatible with SPARQL 1.1 - syntax for decimals was modified
			"Basic - Term 7",
			// Test is incorrect: assumes timezoned date is comparable with non-timezoned
			"date-2",
			// Incompatible with SPARQL 1.1 - string-typed literals and plain literals are identical
			"Strings: Distinct",
			// Incompatible with SPARQL 1.1 - string-typed literals and plain literals are identical
			"All: Distinct",
			// Incompatible with SPARQL 1.1 - string-typed literals and plain literals are identical
			"SELECT REDUCED ?x with strings"
	};

	private List<String> ignoredTests = new ArrayList<>(Arrays.asList(defaultIgnoredTests));

	private static final String[] excludedSubdirs = { "service" };

	private String testURI;
	private String name;
	private String queryFileURL;
	private String resultFileURL;
	private Dataset dataset;
	private boolean ordered;
	private Repository dataRep;

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(getTestData());
	}

	public SPARQL10QueryComplianceTest(String displayName, String testURI, String name, String queryFileURL,
			String resultFileURL, Dataset dataset, boolean ordered) {
		this.testURI = testURI;
		this.name = name;
		this.queryFileURL = queryFileURL;
		this.resultFileURL = resultFileURL;
		this.dataset = dataset;
		this.ordered = ordered;
	}

	@Before
	public void setUp() throws Exception {
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

	@After
	public void tearDown() throws Exception {
		if (dataRep != null) {
			dataRep.shutDown();
			dataRep = null;
		}
	}

	private final Repository createRepository() throws Exception {
		Repository repo = newRepository();
		try (RepositoryConnection con = repo.getConnection()) {
			con.clear();
			con.clearNamespaces();
		}
		return repo;
	}

	protected abstract Repository newRepository() throws Exception;

	@Test
	public void test() throws Exception {
		runTest();
	}

	private static Object[][] getTestData() {

		List<Object[]> tests = new ArrayList<>();

		Deque<String> manifests = new ArrayDeque<>();
		manifests.add(
				SPARQL10QueryComplianceTest.class.getClassLoader()
						.getResource("testcases-sparql-1.0-w3c/data-r2/manifest-evaluation.ttl")
						.toExternalForm());
		while (!manifests.isEmpty()) {
			String pop = manifests.pop();
			Manifest manifest = new Manifest(pop);
			tests.addAll(manifest.tests);
			manifests.addAll(manifest.subManifests);
		}

		Object[][] result = new Object[tests.size()][6];
		tests.toArray(result);

		return result;
	}

	static class Manifest {
		List<Object[]> tests = new ArrayList<>();
		List<String> subManifests = new ArrayList<>();

		public Manifest(String filename) {
			SailRepository sailRepository = new SailRepository(new MemoryStore());
			try (SailRepositoryConnection connection = sailRepository.getConnection()) {
				connection.add(new URL(filename), filename, RDFFormat.TURTLE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			try (SailRepositoryConnection connection = sailRepository.getConnection()) {

				String manifestQuery = " PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> "
						+ "PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> "
						+ "SELECT DISTINCT ?manifestFile "
						+ "WHERE { [] mf:include [ rdf:rest*/rdf:first ?manifestFile ] . }   ";

				try (TupleQueryResult manifestResults = connection
						.prepareTupleQuery(QueryLanguage.SPARQL, manifestQuery, filename)
						.evaluate()) {
					for (BindingSet bindingSet : manifestResults) {
						String subManifestFile = bindingSet.getValue("manifestFile").stringValue();
						if (includeSubManifest(subManifestFile, excludedSubdirs)) {
							subManifests.add(subManifestFile);
						}
					}
				}

				StringBuilder query = new StringBuilder(512);
				query.append(" PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> \n");
				query.append(" PREFIX dawgt: <http://www.w3.org/2001/sw/DataAccess/tests/test-dawg#> \n");
				query.append(" PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> \n");
				query.append(" PREFIX sd: <http://www.w3.org/ns/sparql-service-description#> \n");
				query.append(" PREFIX ent: <http://www.w3.org/ns/entailment/> \n");
				query.append(
						" SELECT DISTINCT ?testURI ?testName ?resultFile ?action ?queryFile ?defaultGraph ?ordered \n");
				query.append(" WHERE { [] rdf:first ?testURI . \n");
//				if (approvedOnly) {
				query.append(" ?testURI dawgt:approval dawgt:Approved . \n");
//				}
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

				try (TupleQueryResult result = connection.prepareTupleQuery(query.toString()).evaluate()) {

					query.setLength(0);
					query.append(" PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> \n");
					query.append(" SELECT ?graph \n");
					query.append(" WHERE { ?action qt:graphData ?graph } \n");
					TupleQuery namedGraphsQuery = connection.prepareTupleQuery(query.toString());

					for (BindingSet bs : result) {
						// FIXME I'm sure there's a neater way to do this
						String testName = bs.getValue("testName").stringValue();
						String displayName = filename.substring(
								filename.lastIndexOf("testcases-sparql-1.0-w3c/")
										+ "testcases-sparql-1.0-w3c/".length(),
								filename.lastIndexOf("/"))
								+ ": " + testName;

						IRI defaultGraphURI = (IRI) bs.getValue("defaultGraph");
						Value action = bs.getValue("action");
						Value ordered = bs.getValue("ordered");

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
									dataset.addNamedGraph(namedGraphURI);
								}
							}
						}

						tests.add(new Object[] {
								displayName,
								bs.getValue("testURI").stringValue(),
								testName,
								bs.getValue("queryFile").stringValue(),
								bs.getValue("resultFile").stringValue(),
								dataset,
								Literals.getBooleanValue(ordered, false) });
					}
				}

			}

		}

	}

	private void runTest() throws Exception {
		assumeThat(getIgnoredTests().contains(name)).withFailMessage("test case '%s' is ignored", name).isFalse();

		logger.debug("running {}", name);

		try (RepositoryConnection conn = dataRep.getConnection()) {
			// Some SPARQL Tests have non-XSD datatypes that must pass for the test
			// suite to complete successfully
			conn.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, Boolean.FALSE);
			conn.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, Boolean.FALSE);

			String queryString = readQueryString();
			Query query = conn.prepareQuery(QueryLanguage.SPARQL, queryString, queryFileURL);
			if (dataset != null) {
				query.setDataset(dataset);
			}

			if (query instanceof TupleQuery) {
				TupleQueryResult actualResult = ((TupleQuery) query).evaluate();
				TupleQueryResult expectedResult = readExpectedTupleQueryResult();
				compareTupleQueryResults(actualResult, expectedResult);
			} else if (query instanceof GraphQuery) {
				GraphQueryResult gqr = ((GraphQuery) query).evaluate();
				Set<Statement> actualResult = Iterations.asSet(gqr);
				Set<Statement> expectedResult = readExpectedGraphQueryResult();

				compareGraphs(actualResult, expectedResult);
			} else if (query instanceof BooleanQuery) {
				boolean actualResult = ((BooleanQuery) query).evaluate();
				boolean expectedResult = readExpectedBooleanQueryResult();
				assertThat(actualResult).isEqualTo(expectedResult);
			} else {
				throw new RuntimeException("Unexpected query type: " + query.getClass());
			}
		}
	}

	private final void uploadDataset(Dataset dataset) throws Exception {
		try (RepositoryConnection con = dataRep.getConnection()) {
			// Merge default and named graphs to filter duplicates
			Set<IRI> graphURIs = new HashSet<>();
			graphURIs.addAll(dataset.getDefaultGraphs());
			graphURIs.addAll(dataset.getNamedGraphs());

			for (Resource graphURI : graphURIs) {
				upload(((IRI) graphURI), graphURI);
			}
		}
	}

	private void upload(IRI graphURI, Resource context) throws Exception {
		RepositoryConnection con = dataRep.getConnection();

		try {
			con.begin();
			RDFFormat rdfFormat = Rio.getParserFormatForFileName(graphURI.toString()).orElse(RDFFormat.TURTLE);
			RDFParser rdfParser = Rio.createParser(rdfFormat, dataRep.getValueFactory());
			rdfParser.setVerifyData(false);
			rdfParser.setDatatypeHandling(DatatypeHandling.IGNORE);
			// rdfParser.setPreserveBNodeIDs(true);

			RDFInserter rdfInserter = new RDFInserter(con);
			rdfInserter.enforceContext(context);
			rdfParser.setRDFHandler(rdfInserter);

			URL graphURL = new URL(graphURI.toString());
			InputStream in = graphURL.openStream();
			try {
				rdfParser.parse(in, graphURI.toString());
			} finally {
				in.close();
			}

			con.commit();
		} catch (Exception e) {
			if (con.isActive()) {
				con.rollback();
			}
			throw e;
		} finally {
			con.close();
		}
	}

	private final String readQueryString() throws IOException {
		try (InputStream stream = new URL(queryFileURL).openStream()) {
			return IOUtil.readString(new InputStreamReader(stream, StandardCharsets.UTF_8));
		}
	}

	private final TupleQueryResult readExpectedTupleQueryResult() throws Exception {
		Optional<QueryResultFormat> tqrFormat = QueryResultIO.getParserFormatForFileName(resultFileURL);

		if (tqrFormat.isPresent()) {
			InputStream in = new URL(resultFileURL).openStream();
			try {
				TupleQueryResultParser parser = QueryResultIO.createTupleParser(tqrFormat.get());
				parser.setValueFactory(dataRep.getValueFactory());

				TupleQueryResultBuilder qrBuilder = new TupleQueryResultBuilder();
				parser.setQueryResultHandler(qrBuilder);

				parser.parseQueryResult(in);
				return qrBuilder.getQueryResult();
			} finally {
				in.close();
			}
		} else {
			Set<Statement> resultGraph = readExpectedGraphQueryResult();
			return DAWGTestResultSetUtil.toTupleQueryResult(resultGraph);
		}
	}

	private final boolean readExpectedBooleanQueryResult() throws Exception {
		Optional<QueryResultFormat> bqrFormat = BooleanQueryResultParserRegistry.getInstance()
				.getFileFormatForFileName(resultFileURL);

		if (bqrFormat.isPresent()) {
			InputStream in = new URL(resultFileURL).openStream();
			try {
				return QueryResultIO.parseBoolean(in, bqrFormat.get());
			} finally {
				in.close();
			}
		} else {
			Set<Statement> resultGraph = readExpectedGraphQueryResult();
			return DAWGTestResultSetUtil.toBooleanQueryResult(resultGraph);
		}
	}

	private final Set<Statement> readExpectedGraphQueryResult() throws Exception {
		RDFFormat rdfFormat = Rio.getParserFormatForFileName(resultFileURL)
				.orElseThrow(Rio.unsupportedFormat(resultFileURL));

		RDFParser parser = Rio.createParser(rdfFormat);
		parser.setDatatypeHandling(DatatypeHandling.IGNORE);
		parser.setPreserveBNodeIDs(true);
		parser.setValueFactory(dataRep.getValueFactory());

		Set<Statement> result = new LinkedHashSet<>();
		parser.setRDFHandler(new StatementCollector(result));

		InputStream in = new URL(resultFileURL).openStream();
		try {
			parser.parse(in, resultFileURL);
		} finally {
			in.close();
		}

		return result;
	}

	private final void compareGraphs(Set<Statement> queryResult, Set<Statement> expectedResult) throws Exception {
		if (!Models.isomorphic(expectedResult, queryResult)) {
			StringBuilder message = new StringBuilder(128);
			message.append("\n============ ");
			message.append(name);
			message.append(" =======================\n");
			message.append("Expected result: \n");
			for (Statement st : expectedResult) {
				message.append(st.toString());
				message.append("\n");
			}
			message.append("=============");
			StringUtil.appendN('=', name.length(), message);
			message.append("========================\n");

			message.append("Query result: \n");
			for (Statement st : queryResult) {
				message.append(st.toString());
				message.append("\n");
			}
			message.append("=============");
			StringUtil.appendN('=', name.length(), message);
			message.append("========================\n");

			logger.error(message.toString());
			fail(message.toString());
		}
	}

	private final void compareTupleQueryResults(TupleQueryResult queryResult, TupleQueryResult expectedResult)
			throws Exception {
		// Create MutableTupleQueryResult to be able to re-iterate over the
		// results
		MutableTupleQueryResult queryResultTable = new MutableTupleQueryResult(queryResult);
		MutableTupleQueryResult expectedResultTable = new MutableTupleQueryResult(expectedResult);

		boolean resultsEqual;
		boolean laxCardinality = false; // TODO determine if we still need this
		if (laxCardinality) {
			resultsEqual = QueryResults.isSubset(queryResultTable, expectedResultTable);
		} else {
			resultsEqual = QueryResults.equals(queryResultTable, expectedResultTable);

			if (ordered) {
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
			message.append(name);
			message.append(" =======================\n");

			if (!missingBindings.isEmpty()) {

				message.append("Missing bindings: \n");
				for (BindingSet bs : missingBindings) {
					printBindingSet(bs, message);
				}

				message.append("=============");
				StringUtil.appendN('=', name.length(), message);
				message.append("========================\n");
			}

			if (!unexpectedBindings.isEmpty()) {
				message.append("Unexpected bindings: \n");
				for (BindingSet bs : unexpectedBindings) {
					printBindingSet(bs, message);
				}

				message.append("=============");
				StringUtil.appendN('=', name.length(), message);
				message.append("========================\n");
			}

			if (ordered && missingBindings.isEmpty() && unexpectedBindings.isEmpty()) {
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
	}

	private final void printBindingSet(BindingSet bs, StringBuilder appendable) {
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

	/**
	 * Verifies if the selected subManifest occurs in the supplied list of excluded subdirs.
	 *
	 * @param subManifestFile the url of a sub-manifest
	 * @param excludedSubdirs an array of directory names. May be null.
	 * @return <code>false</code> if the supplied list of excluded subdirs is not empty and contains a match for the
	 *         supplied sub-manifest, <code>true</code> otherwise.
	 */
	private static boolean includeSubManifest(String subManifestFile, String[] excludedSubdirs) {
		boolean result = true;

		if (excludedSubdirs != null && excludedSubdirs.length > 0) {
			int index = subManifestFile.lastIndexOf('/');
			String path = subManifestFile.substring(0, index);
			String sd = path.substring(path.lastIndexOf('/') + 1);

			for (String subdir : excludedSubdirs) {
				if (sd.equals(subdir)) {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * @return the ignoredTests
	 */
	protected List<String> getIgnoredTests() {
		return ignoredTests;
	}

	protected void addIgnoredTest(String ignoredTest) {
		this.ignoredTests.add(ignoredTest);
	}

	/**
	 * @param ignoredTests the ignoredTests to set
	 */
	protected void setIgnoredTests(List<String> ignoredTests) {
		this.ignoredTests = ignoredTests;
	}
}
