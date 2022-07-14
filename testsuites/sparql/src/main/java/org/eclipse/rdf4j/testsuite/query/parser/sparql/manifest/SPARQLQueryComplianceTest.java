/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
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
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base functionality for SPARQL query compliance test suites .
 *
 * @author Jeen Broekstra
 *
 */
public abstract class SPARQLQueryComplianceTest extends SPARQLComplianceTest {

	private static final Logger logger = LoggerFactory.getLogger(SPARQLQueryComplianceTest.class);

	private final String queryFileURL;
	private final String resultFileURL;
	private final Dataset dataset;
	private final boolean ordered;
	private final boolean laxCardinality;

	private Repository dataRepository;

	/**
	 * @param displayName
	 * @param testURI
	 * @param name
	 * @param queryFileURL
	 * @param resultFileURL
	 * @param dataset
	 * @param ordered
	 */
	public SPARQLQueryComplianceTest(String displayName, String testURI, String name, String queryFileURL,
			String resultFileURL, Dataset dataset, boolean ordered, boolean laxCardinality) {
		super(displayName, testURI, name);
		this.queryFileURL = queryFileURL;
		this.resultFileURL = resultFileURL;
		this.dataset = dataset;
		this.ordered = ordered;
		this.laxCardinality = laxCardinality;
	}

	@Before
	public void setUp() throws Exception {
		dataRepository = createRepository();
		if (dataset != null) {
			try {
				uploadDataset(dataset);
			} catch (Exception exc) {
				try {
					dataRepository.shutDown();
					dataRepository = null;
				} catch (Exception e2) {
					logger.error(e2.toString(), e2);
				}
				throw exc;
			}
		}
	}

	@After
	public void tearDown() throws Exception {
		if (dataRepository != null) {
			dataRepository.shutDown();
			dataRepository = null;
		}
	}

	@Override
	protected void runTest() throws Exception {

		logger.debug("running {}", getName());

		try (RepositoryConnection conn = getDataRepository().getConnection()) {
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

	protected abstract Repository newRepository() throws Exception;

	private Repository createRepository() throws Exception {
		Repository repo = newRepository();
		try (RepositoryConnection con = repo.getConnection()) {
			con.clear();
			con.clearNamespaces();
		}
		return repo;
	}

	private String readQueryString() throws IOException {
		try (InputStream stream = new URL(queryFileURL).openStream()) {
			return IOUtil.readString(new InputStreamReader(stream, StandardCharsets.UTF_8));
		}
	}

	private TupleQueryResult readExpectedTupleQueryResult() throws Exception {
		Optional<QueryResultFormat> tqrFormat = QueryResultIO.getParserFormatForFileName(resultFileURL);

		if (tqrFormat.isPresent()) {
			try (InputStream in = new URL(resultFileURL).openStream()) {
				TupleQueryResultParser parser = QueryResultIO.createTupleParser(tqrFormat.get());
				parser.setValueFactory(getDataRepository().getValueFactory());

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

	private boolean readExpectedBooleanQueryResult() throws Exception {
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

	private Set<Statement> readExpectedGraphQueryResult() throws Exception {
		RDFFormat rdfFormat = Rio.getParserFormatForFileName(resultFileURL)
				.orElseThrow(Rio.unsupportedFormat(resultFileURL));

		RDFParser parser = Rio.createParser(rdfFormat);
		parser.setPreserveBNodeIDs(true);
		parser.setValueFactory(getDataRepository().getValueFactory());

		Set<Statement> result = new LinkedHashSet<>();
		parser.setRDFHandler(new StatementCollector(result));

		try (InputStream in = new URL(resultFileURL).openStream()) {
			parser.parse(in, resultFileURL);
		}

		return result;
	}

	private void compareTupleQueryResults(TupleQueryResult queryResult, TupleQueryResult expectedResult)
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

	@Override
	protected Repository getDataRepository() {
		return this.dataRepository;
	}

	protected static class SPARQLQueryTestManifest {
		private final List<Object[]> tests = new ArrayList<>();
		private final List<String> subManifests = new ArrayList<>();

		public SPARQLQueryTestManifest(String filename, List<String> excludedSubdirs) {
			this(filename, excludedSubdirs, true);
		}

		public SPARQLQueryTestManifest(String filename, List<String> excludedSubdirs, boolean approvedOnly) {
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
						if (SPARQLQueryComplianceTest.includeSubManifest(subManifestFile, excludedSubdirs)) {
							getSubManifests().add(subManifestFile);
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
						" SELECT DISTINCT ?testURI ?testName ?resultFile ?action ?queryFile ?defaultGraph ?ordered ?laxCardinality \n");
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
				query.append(" OPTIONAL { ?testURI mf:resultCardinality ?laxCardinality, mf:LaxCardinality } \n");
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
						String displayName = filename.substring(0, filename.lastIndexOf('/'));
						displayName = displayName.substring(displayName.lastIndexOf('/') + 1, displayName.length())
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

						getTests().add(new Object[] {
								displayName,
								bs.getValue("testURI").stringValue(),
								testName,
								bs.getValue("queryFile").stringValue(),
								bs.getValue("resultFile").stringValue(),
								dataset,
								Literals.getBooleanValue(ordered, false),
								bs.hasBinding("laxCardinality")
						});
					}
				}

			}

		}

		/**
		 * @return the tests
		 */
		public List<Object[]> getTests() {
			return tests;
		}

		/**
		 * @return the subManifests
		 */
		public List<String> getSubManifests() {
			return subManifests;
		}

	}
}
