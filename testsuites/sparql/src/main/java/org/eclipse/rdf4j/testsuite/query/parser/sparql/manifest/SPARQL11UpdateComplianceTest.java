/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A test suite that runs the W3C Approved SPARQL 1.1 update compliance tests.
 *
 * @author Jeen Broekstra
 *
 * @see <a href="https://www.w3.org/2009/sparql/docs/tests/">sparql docs tests</a>
 */
@RunWith(Parameterized.class)
public abstract class SPARQL11UpdateComplianceTest extends SPARQLComplianceTest {

	private static final Logger logger = LoggerFactory.getLogger(SPARQL11UpdateComplianceTest.class);

	private static final String[] defaultIgnoredTests = {
//			// test case incompatible with RDF 1.1 - see
//			// http://lists.w3.org/Archives/Public/public-sparql-dev/2013AprJun/0006.html
//			"STRDT() TypeErrors",
//			// test case incompatible with RDF 1.1 - see
//			// http://lists.w3.org/Archives/Public/public-sparql-dev/2013AprJun/0006.html
//			"STRLANG() TypeErrors",
//			// known issue: SES-937
//			"sq03 - Subquery within graph pattern, graph variable is not bound"
	};

	private static final List<String> excludedSubdirs = List.of("service");

	private String queryFileURL;
	private String resultFileURL;
	private final Dataset dataset;
	private boolean ordered;
	private Repository dataRep;

	protected Repository expectedResultRepo;

	private final String requestFile;

	private final IRI inputDefaultGraphURI;

	private final Map<String, IRI> inputNamedGraphs;

	private final IRI resultDefaultGraphURI;

	private final Map<String, IRI> resultNamedGraphs;

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(getTestData());
	}

	public SPARQL11UpdateComplianceTest(String displayName, String testURI, String name, String requestFile,
			IRI defaultGraphURI, Map<String, IRI> inputNamedGraphs, IRI resultDefaultGraphURI,
			Map<String, IRI> resultNamedGraphs) {
		super(displayName, testURI, name);
		this.requestFile = requestFile;
		this.inputDefaultGraphURI = defaultGraphURI;
		this.inputNamedGraphs = inputNamedGraphs;
		this.resultDefaultGraphURI = resultDefaultGraphURI;
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

	@Before
	public void setUp() throws Exception {
		dataRep = createRepository();

		try (RepositoryConnection conn = dataRep.getConnection()) {
			conn.clear();

			if (inputDefaultGraphURI != null) {
				URL graphURL = new URL(inputDefaultGraphURI.stringValue());
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

			if (resultDefaultGraphURI != null) {
				URL graphURL = new URL(resultDefaultGraphURI.stringValue());
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

	@After
	public void tearDown() throws Exception {
		if (dataRep != null) {
			dataRep.shutDown();
			dataRep = null;
		}
		if (expectedResultRepo != null) {
			expectedResultRepo.shutDown();
			expectedResultRepo = null;
		}
	}

	private Repository createRepository() throws Exception {
		Repository repo = newRepository();
		try (RepositoryConnection con = repo.getConnection()) {
			con.clear();
			con.clearNamespaces();
		}
		return repo;
	}

	protected abstract Repository newRepository() throws Exception;

	@Override
	protected Repository getDataRepository() {
		return this.dataRep;
	}

	private static Object[][] getTestData() {

		List<Object[]> tests = new ArrayList<>();

		Deque<String> manifests = new ArrayDeque<>();
		manifests.add(
				SPARQL11UpdateComplianceTest.class.getClassLoader()
						.getResource("testcases-sparql-1.1-w3c/manifest-all.ttl")
						.toExternalForm());
		while (!manifests.isEmpty()) {
			String pop = manifests.pop();
			SPARQLUpdateTestManifest manifest = new SPARQLUpdateTestManifest(pop);
			tests.addAll(manifest.tests);
			manifests.addAll(manifest.subManifests);
		}

		Object[][] result = new Object[tests.size()][6];
		tests.toArray(result);

		return result;
	}

	static class SPARQLUpdateTestManifest {
		List<Object[]> tests = new ArrayList<>();
		List<String> subManifests = new ArrayList<>();

		public SPARQLUpdateTestManifest(String filename) {
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
				query.append("PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> \n ");
				query.append("PREFIX dawgt: <http://www.w3.org/2001/sw/DataAccess/tests/test-dawg#> \n");
				query.append("PREFIX qt:  <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> \n");
				query.append("PREFIX ut: <http://www.w3.org/2009/sparql/tests/test-update#> \n");
				query.append("PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>\n ");
				query.append("PREFIX ent: <http://www.w3.org/ns/entailment/> \n");
				query.append(
						" SELECT DISTINCT ?testURI ?testName ?result ?action ?requestFile ?defaultGraph ?resultDefaultGraph ");
				query.append(" wHERE { [] rdf:first ?testURI. ?testURI a mf:UpdateEvaluationTest .\n");
				query.append("         ?testURI  dawgt:approval dawgt:Approved; \n");
				query.append("                   mf:name ?testName; \n");
				query.append("                   mf:action ?action . \n");
				query.append("         ?action ut:request ?requestFile. \n");
				query.append("         OPTIONAL { ?action ut:data ?defaultGraph } \n");
				query.append("         ?testURI mf:result ?result. \n");
				query.append("         OPTIONAL { ?result ut:data ?resultDefaultGraph } \n");
				query.append("}");

				try (TupleQueryResult result = connection.prepareTupleQuery(query.toString()).evaluate()) {

					query.setLength(0);
					query.append(" PREFIX ut: <http://www.w3.org/2009/sparql/tests/test-update#> \n");
					query.append(" SELECT DISTINCT ?namedGraphData ?namedGraphLabel \n");
					query.append(" WHERE { ?graphDef ut:graphData [ ut:graph ?namedGraphData ; \n ");
					query.append("                                  rdfs:label ?namedGraphLabel ]. }\n ");

					TupleQuery namedGraphsQuery = connection.prepareTupleQuery(query.toString());

					for (BindingSet bs : result) {
						// FIXME I'm sure there's a neater way to do this
						String testName = bs.getValue("testName").stringValue();
						String displayName = filename.substring(
								filename.lastIndexOf("testcases-sparql-1.1-w3c/")
										+ "testcases-sparql-1.1-w3c/".length(),
								filename.lastIndexOf("/"))
								+ ": " + testName;

						IRI testURI = (IRI) bs.getValue("testURI");
						Value testResult = bs.getValue("result");
						Value action = bs.getValue("action");
						IRI requestFile = (IRI) bs.getValue("requestFile");
						IRI defaultGraphURI = (IRI) bs.getValue("defaultGraph");
						IRI resultDefaultGraphURI = (IRI) bs.getValue("resultDefaultGraph");

						SimpleDataset dataset = null;
						namedGraphsQuery.setBinding("graphDef", action);
						TupleQueryResult inputNamedGraphsResult = namedGraphsQuery.evaluate();

						HashMap<String, IRI> inputNamedGraphs = new HashMap<>();

						if (inputNamedGraphsResult.hasNext()) {
							while (inputNamedGraphsResult.hasNext()) {
								BindingSet graphBindings = inputNamedGraphsResult.next();
								IRI namedGraphData = (IRI) graphBindings.getValue("namedGraphData");
								String namedGraphLabel = ((Literal) graphBindings.getValue("namedGraphLabel"))
										.getLabel();
								logger.debug(" adding named graph : {}", namedGraphLabel);
								inputNamedGraphs.put(namedGraphLabel, namedGraphData);
							}
						}

						// Query result named graphs
						namedGraphsQuery.setBinding("graphDef", testResult);
						TupleQueryResult resultNamedGraphsResult = namedGraphsQuery.evaluate();

						HashMap<String, IRI> resultNamedGraphs = new HashMap<>();

						if (resultNamedGraphsResult.hasNext()) {
							while (resultNamedGraphsResult.hasNext()) {
								BindingSet graphBindings = resultNamedGraphsResult.next();
								IRI namedGraphData = (IRI) graphBindings.getValue("namedGraphData");
								String namedGraphLabel = ((Literal) graphBindings.getValue("namedGraphLabel"))
										.getLabel();
								logger.debug(" adding named graph : {}", namedGraphLabel);
								resultNamedGraphs.put(namedGraphLabel, namedGraphData);
							}
						}

						tests.add(new Object[] {
								displayName,
								testURI.stringValue(),
								testName,
								requestFile.stringValue(),
								defaultGraphURI,
								inputNamedGraphs,
								resultDefaultGraphURI,
								resultNamedGraphs
						});
					}
				}
			}
		}
	}

	@Override
	protected void runTest() throws Exception {

		logger.debug("running {}", getName());

		RepositoryConnection con = dataRep.getConnection();
		RepositoryConnection erCon = expectedResultRepo.getConnection();
		try {
			String updateString = readUpdateString();

			con.begin();

			Update update = con.prepareUpdate(QueryLanguage.SPARQL, updateString, requestFile);
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

	private String readUpdateString() throws IOException {
		try (InputStream stream = new URL(requestFile).openStream()) {
			return IOUtil.readString(new InputStreamReader(stream, StandardCharsets.UTF_8));
		}
	}
}
