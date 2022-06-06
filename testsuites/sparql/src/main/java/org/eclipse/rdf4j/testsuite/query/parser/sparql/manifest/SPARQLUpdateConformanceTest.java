/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * A SPARQL 1.1 Update test, created by reading in a W3C working-group style manifest.
 *
 * @author Jeen Broekstra
 *
 * @deprecated since 3.3.0. Use {@link SPARQL11UpdateComplianceTest} instead.
 */
@Deprecated
public abstract class SPARQLUpdateConformanceTest extends TestCase {

	/*-----------*
	 * Constants *
	 *-----------*/

	static final Logger logger = LoggerFactory.getLogger(SPARQLUpdateConformanceTest.class);

	protected final String testURI;

	protected final String requestFileURL;

	/*-----------*
	 * Variables *
	 *-----------*/

	protected Repository dataRep;

	protected Repository expectedResultRepo;

	private final IRI inputDefaultGraph;

	private final Map<String, IRI> inputNamedGraphs;

	private final IRI resultDefaultGraph;

	private final Map<String, IRI> resultNamedGraphs;

	protected final Dataset dataset;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SPARQLUpdateConformanceTest(String testURI, String name, String requestFile, IRI defaultGraphURI,
			Map<String, IRI> inputNamedGraphs, IRI resultDefaultGraphURI, Map<String, IRI> resultNamedGraphs) {
		super(name);

		this.testURI = testURI;
		this.requestFileURL = requestFile;
		this.inputDefaultGraph = defaultGraphURI;
		this.inputNamedGraphs = inputNamedGraphs;
		this.resultDefaultGraph = resultDefaultGraphURI;
		this.resultNamedGraphs = resultNamedGraphs;

		final SimpleDataset ds = new SimpleDataset();

		// This ensures that the repository operates in 'exclusive
		// mode': the default graph _only_ consists of the null-context (instead
		// of the entire repository).
		ds.addDefaultGraph(null);
		ds.addDefaultRemoveGraph(null);
		ds.setDefaultInsertGraph(null);

		if (this.inputNamedGraphs.size() > 0) {
			for (String ng : inputNamedGraphs.keySet()) {
				IRI namedGraph = SimpleValueFactory.getInstance().createIRI(ng);
				ds.addNamedGraph(namedGraph);
			}
		}
		this.dataset = ds;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void setUp() throws Exception {
		dataRep = createRepository();

		try (RepositoryConnection conn = dataRep.getConnection()) {
			conn.clear();

			if (inputDefaultGraph != null) {
				URL graphURL = new URL(inputDefaultGraph.stringValue());
				conn.add(graphURL, null, Rio.getParserFormatForFileName(graphURL.toString())
						.orElseThrow(Rio.unsupportedFormat(graphURL.toString())));
			}

			for (String ng : inputNamedGraphs.keySet()) {
				URL graphURL = new URL(inputNamedGraphs.get(ng).stringValue());
				conn.add(graphURL, null,
						Rio.getParserFormatForFileName(graphURL.toString())
								.orElseThrow(Rio.unsupportedFormat(graphURL.toString())),
						dataRep.getValueFactory().createIRI(ng));
			}
		}

		expectedResultRepo = createRepository();

		try (RepositoryConnection conn = expectedResultRepo.getConnection()) {
			conn.clear();

			if (resultDefaultGraph != null) {
				URL graphURL = new URL(resultDefaultGraph.stringValue());
				conn.add(graphURL, null, Rio.getParserFormatForFileName(graphURL.toString())
						.orElseThrow(Rio.unsupportedFormat(graphURL.toString())));
			}

			for (String ng : resultNamedGraphs.keySet()) {
				URL graphURL = new URL(resultNamedGraphs.get(ng).stringValue());
				conn.add(graphURL, null,
						Rio.getParserFormatForFileName(graphURL.toString())
								.orElseThrow(Rio.unsupportedFormat(graphURL.toString())),
						dataRep.getValueFactory().createIRI(ng));
			}
		}
	}

	protected Repository createRepository() throws Exception {
		Repository repo = newRepository();
		Repositories.consume(repo, con -> {
			con.clear();
			con.clearNamespaces();
		});

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
		RepositoryConnection con = dataRep.getConnection();
		RepositoryConnection erCon = expectedResultRepo.getConnection();
		try {
			String updateString = readUpdateString();

			con.begin();

			Update update = con.prepareUpdate(QueryLanguage.SPARQL, updateString, requestFileURL);
			update.setDataset(dataset);
			update.execute();

			con.commit();

			// check default graph
			logger.info("checking default graph");
			compareGraphs(Iterations.asList(con.getStatements(null, null, null, true, (Resource) null)),
					Iterations.asList(erCon.getStatements(null, null, null, true, (Resource) null)));

			for (String namedGraph : inputNamedGraphs.keySet()) {
				logger.info("checking named graph {}", namedGraph);
				IRI contextURI = con.getValueFactory().createIRI(namedGraph.replaceAll("\"", ""));
				compareGraphs(Iterations.asList(con.getStatements(null, null, null, true, contextURI)),
						Iterations.asList(erCon.getStatements(null, null, null, true, contextURI)));
			}
		} catch (Exception e) {
			if (con.isActive()) {
				con.rollback();
			}
			throw e;
		} finally {
			con.close();
			erCon.close();
		}
	}

	private void compareGraphs(Iterable<? extends Statement> actual, Iterable<? extends Statement> expected)
			throws Exception {
		if (!Models.isomorphic(expected, actual)) {
			StringBuilder message = new StringBuilder(128);
			message.append("\n============ ");
			message.append(getName());
			message.append(" =======================\n");
			message.append("Expected result: \n");
			for (Statement st : expected) {
				message.append(st.toString());
				message.append("\n");
			}
			message.append("=============");
			StringUtil.appendN('=', getName().length(), message);
			message.append("========================\n");

			message.append("Actual result: \n");
			for (Statement st : actual) {
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

	private String readUpdateString() throws IOException {
		try (InputStream stream = new URL(requestFileURL).openStream()) {
			return IOUtil.readString(new InputStreamReader(stream, StandardCharsets.UTF_8));
		}
	}

	public interface Factory {

		SPARQLUpdateConformanceTest createSPARQLUpdateConformanceTest(String testURI, String name, String requestFile,
				IRI defaultGraphURI, Map<String, IRI> inputNamedGraphs, IRI resultDefaultGraphURI,
				Map<String, IRI> resultNamedGraphs);

	}

	public static TestSuite suite(String manifestFileURL, Factory factory) throws Exception {
		return suite(manifestFileURL, factory, true);
	}

	public static TestSuite suite(String manifestFileURL, Factory factory, boolean approvedOnly) throws Exception {
		logger.info("Building test suite for {}", manifestFileURL);

		TestSuite suite = new TestSuite(factory.getClass().getName());

		// Read manifest and create declared test cases
		Repository manifestRep = new SailRepository(new MemoryStore());
		try (RepositoryConnection con = manifestRep.getConnection()) {

			SPARQL11ManifestTest.addTurtle(con, new URL(manifestFileURL), manifestFileURL);

			suite.setName(getManifestName(manifestRep, con, manifestFileURL));

			// Extract test case information from the manifest file. Note that we
			// only select those test cases that are mentioned in the list.
			StringBuilder query = new StringBuilder(512);
			query.append("PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n ");
			query.append("PREFIX dawgt = <http://www.w3.org/2001/sw/DataAccess/tests/test-dawg#>\n");
			query.append("PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
			query.append("PREFIX ut: <http://www.w3.org/2009/sparql/tests/test-update#>\n");
			query.append("PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>\n");
			query.append("PREFIX ent: <http://www.w3.org/ns/entailment/> \n");
			query.append(
					" SELECT DISTINCT ?testURI ?testName ?result ?action ?requestFile ?defaultGraph ?resultDefaultGraph \n");
			query.append(" WHERE { [] rdf:first ?testURI. ?testURI a mf:UpdateEvaluationTest; ");
			if (approvedOnly) {
				query.append("                          dawgt:approval dawgt:Approved; ");
			}
			query.append("                             mf:name ?testName; ");
			query.append("                             mf:action ?action. ?action ut:request ?requestFile. ");
			query.append("  OPTIONAL {?action ut:data ?defaultGraph .} ");
			query.append("    ?testURI mf:result ?result . \n");
			query.append("  OPTIONAL { ?result ut:data ?resultDefaultGraph }} ");

			TupleQuery testCaseQuery = con.prepareTupleQuery(query.toString());

			query.setLength(0);
			query.append("PREFIX ut: <http://www.w3.org/2009/sparql/tests/test-update#> \n");
			query.append(" SELECT DISTINCT ?namedGraphData ?namedGraphLabel ");
			query.append(" WHERE { ?graphDef ut:graphData [ ut:graph ?namedGraphData ; ");
			query.append("                                  rdfs:label ?namedGraphLabel].} ");

			TupleQuery namedGraphsQuery = con.prepareTupleQuery(query.toString());

			logger.debug("evaluating query..");
			TupleQueryResult testCases = testCaseQuery.evaluate();
			while (testCases.hasNext()) {
				BindingSet bindingSet = testCases.next();

				IRI testURI = (IRI) bindingSet.getValue("testURI");
				String testName = bindingSet.getValue("testName").toString();
				Value result = bindingSet.getValue("result");
				Value action = bindingSet.getValue("action");
				IRI requestFile = (IRI) bindingSet.getValue("requestFile");
				IRI defaultGraphURI = (IRI) bindingSet.getValue("defaultGraph");
				IRI resultDefaultGraphURI = (IRI) bindingSet.getValue("resultDefaultGraph");

				logger.debug("found test case : {}", testName);

				// Query input named graphs
				namedGraphsQuery.setBinding("graphDef", action);
				TupleQueryResult inputNamedGraphsResult = namedGraphsQuery.evaluate();

				HashMap<String, IRI> inputNamedGraphs = new HashMap<>();

				if (inputNamedGraphsResult.hasNext()) {
					while (inputNamedGraphsResult.hasNext()) {
						BindingSet graphBindings = inputNamedGraphsResult.next();
						IRI namedGraphData = (IRI) graphBindings.getValue("namedGraphData");
						String namedGraphLabel = ((Literal) graphBindings.getValue("namedGraphLabel")).getLabel();
						logger.debug(" adding named graph : {}", namedGraphLabel);
						inputNamedGraphs.put(namedGraphLabel, namedGraphData);
					}
				}

				// Query result named graphs
				namedGraphsQuery.setBinding("graphDef", result);
				TupleQueryResult resultNamedGraphsResult = namedGraphsQuery.evaluate();

				HashMap<String, IRI> resultNamedGraphs = new HashMap<>();

				if (resultNamedGraphsResult.hasNext()) {
					while (resultNamedGraphsResult.hasNext()) {
						BindingSet graphBindings = resultNamedGraphsResult.next();
						IRI namedGraphData = (IRI) graphBindings.getValue("namedGraphData");
						String namedGraphLabel = ((Literal) graphBindings.getValue("namedGraphLabel")).getLabel();
						logger.debug(" adding named graph : {}", namedGraphLabel);
						resultNamedGraphs.put(namedGraphLabel, namedGraphData);
					}
				}

				SPARQLUpdateConformanceTest test = factory.createSPARQLUpdateConformanceTest(testURI.toString(),
						testName,
						requestFile.toString(), defaultGraphURI, inputNamedGraphs, resultDefaultGraphURI,
						resultNamedGraphs);

				if (test != null) {
					suite.addTest(test);
				}
			}

			testCases.close();
		}
		manifestRep.shutDown();
		logger.info("Created test suite with " + suite.countTestCases() + " test cases.");
		return suite;
	}

	protected static String getManifestName(Repository manifestRep, RepositoryConnection con, String manifestFileURL)
			throws QueryEvaluationException, RepositoryException, MalformedQueryException {
		// Try to extract suite name from manifest file
		TupleQuery manifestNameQuery = con.prepareTupleQuery(
				"SELECT ?ManifestName WHERE { ?ManifestURL rdfs:label ?ManifestName .}");
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
