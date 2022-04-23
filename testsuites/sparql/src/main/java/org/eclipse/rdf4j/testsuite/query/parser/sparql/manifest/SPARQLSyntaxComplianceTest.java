/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest;

import static org.junit.Assert.fail;

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
import java.util.List;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.DeleteData;
import org.eclipse.rdf4j.query.algebra.InsertData;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.sail.helpers.SailUpdateExecutor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A test suite that runs SPARQL syntax tests.
 *
 * @author Jeen Broekstra
 *
 */
public abstract class SPARQLSyntaxComplianceTest extends SPARQLComplianceTest {

	private static final Logger logger = LoggerFactory.getLogger(SPARQLSyntaxComplianceTest.class);

	private static final List<String> excludedSubdirs = List.of();

	private final String queryFileURL;
	private final boolean positiveTest;

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(getTestData());
	}

	public SPARQLSyntaxComplianceTest(String displayName, String testURI, String name, String queryFileURL,
			boolean positiveTest) {

		super(displayName, testURI, name);
		this.queryFileURL = queryFileURL;
		this.positiveTest = positiveTest;
	}

	private static Object[][] getTestData() {

		List<Object[]> tests = new ArrayList<>();

		Deque<String> manifests = new ArrayDeque<>();
		manifests.add(
				SPARQLSyntaxComplianceTest.class.getClassLoader()
						.getResource("testcases-sparql-1.1-w3c/manifest-all.ttl")
						.toExternalForm());
		while (!manifests.isEmpty()) {
			String pop = manifests.pop();
			SPARQLSyntaxManifest manifest = new SPARQLSyntaxManifest(pop);
			tests.addAll(manifest.tests);
			manifests.addAll(manifest.subManifests);
		}

		Object[][] result = new Object[tests.size()][6];
		tests.toArray(result);

		return result;
	}

	static class SPARQLSyntaxManifest {
		List<Object[]> tests = new ArrayList<>();
		List<String> subManifests = new ArrayList<>();

		public SPARQLSyntaxManifest(String filename) {
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
				query.append("PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> ");
				query.append("PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> ");
				query.append("PREFIX dawgt: <http://www.w3.org/2001/sw/DataAccess/tests/test-dawg#> ");
				query.append("SELECT ?TestURI ?Name ?Action ?Type ");
				query.append("WHERE { [] rdf:first ?TestURI. ");
				query.append("        ?TestURI a ?Type ; ");
				query.append("                 mf:name ?Name ;");
				query.append("                 mf:action ?Action ;");
				query.append("                 dawgt:approval dawgt:Approved . ");
				query.append(
						"        FILTER(?Type IN (mf:PositiveSyntaxTest, mf:NegativeSyntaxTest, mf:PositiveSyntaxTest11, mf:NegativeSyntaxTest11, mf:PositiveUpdateSyntaxTest11, mf:NegativeUpdateSyntaxTest11)) ");
				query.append(" } ");

				try (TupleQueryResult result = connection.prepareTupleQuery(query.toString()).evaluate()) {
					for (BindingSet bs : result) {
						// FIXME I'm sure there's a neater way to do this
						String testName = bs.getValue("Name").stringValue();
						String displayName = filename.substring(
								filename.lastIndexOf("testcases-sparql-1.1-w3c/")
										+ "testcases-sparql-1.1-w3c/".length(),
								filename.lastIndexOf("/"))
								+ ": " + testName;

						IRI testURI = (IRI) bs.getValue("TestURI");
						Value action = bs.getValue("Action");
						String type = bs.getValue("Type").toString();
						boolean positiveTest = type
								.equals("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#PositiveSyntaxTest11")
								|| type.equals(
										"http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#PositiveUpdateSyntaxTest11");

						tests.add(new Object[] {
								displayName,
								testURI.stringValue(),
								testName,
								action.stringValue(),
								positiveTest });
					}
				}

			}

		}

	}

	protected abstract ParsedOperation parseOperation(String operation, String fileURL) throws MalformedQueryException;

	@Override
	protected void runTest() throws Exception {
		InputStream stream = new URL(queryFileURL).openStream();
		String query = IOUtil.readString(new InputStreamReader(stream, StandardCharsets.UTF_8));
		stream.close();

		try {
			ParsedOperation operation = parseOperation(query, queryFileURL);

			if (!positiveTest) {
				boolean dataBlockUpdate = false;
				if (operation instanceof ParsedUpdate) {
					for (UpdateExpr updateExpr : ((ParsedUpdate) operation).getUpdateExprs()) {
						if (updateExpr instanceof InsertData || updateExpr instanceof DeleteData) {
							// parsing for these operation happens during actual
							// execution, so try and execute.
							dataBlockUpdate = true;

							MemoryStore store = new MemoryStore();
							store.init();
							NotifyingSailConnection conn = store.getConnection();
							try {
								conn.begin();
								SailUpdateExecutor exec = new SailUpdateExecutor(conn, store.getValueFactory(), null);
								exec.executeUpdate(updateExpr, null, null, true, -1);
								conn.rollback();
								fail("Negative test case should have failed to parse");
							} catch (SailException e) {
								if (!(e.getCause() instanceof RDFParseException)) {
									logger.error("unexpected error in negative test case", e);
									fail("unexpected error in negative test case");
								}
								// fall through - a parse exception is expected for a
								// negative test case
								conn.rollback();
							} finally {
								conn.close();
							}
						}
					}
				}
				if (!dataBlockUpdate) {
					fail("Negative test case should have failed to parse");
				}
			}
		} catch (MalformedQueryException e) {
			if (positiveTest) {
				e.printStackTrace();
				fail("Positive test case failed: " + e.getMessage());
			}
		}
	}
}
