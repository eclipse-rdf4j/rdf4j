/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.dawg.DAWGTestResultSetUtil;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.impl.MutableTupleQueryResult;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParserRegistry;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public abstract class FedXBaseTest {

	public static Logger log;

	@BeforeAll
	public static void initLogging() throws Exception {

		if (System.getProperty("log4j.configurationFile") == null) {
			System.setProperty("log4j.configurationFile", "file:build/test/log4j-debug.properties");
		}

		log = LoggerFactory.getLogger(FedXBaseTest.class);
	}

	protected static final String EXAMPLE_NAMESPACE = "http://example.org/";

	protected static final ValueFactory vf = SimpleValueFactory.getInstance();

	protected String defaultNamespace = EXAMPLE_NAMESPACE;

	/**
	 * Execute a testcase, both queryFile and expectedResultFile must be files
	 *
	 * @param queryFile
	 * @param expectedResultFile
	 * @param checkOrder
	 * @throws Exception
	 */
	protected void execute(RepositoryConnection conn, String queryFile, String expectedResultFile, boolean checkOrder)
			throws Exception {

		String queryString = readQueryString(queryFile);

		Query query = queryManager().prepareQuery(queryString);

		if (query instanceof TupleQuery) {
			try (TupleQueryResult queryResult = ((TupleQuery) query).evaluate()) {

				TupleQueryResult expectedResult = readExpectedTupleQueryResult(expectedResultFile);

				compareTupleQueryResults(queryResult, expectedResult, checkOrder);
			}

		} else if (query instanceof GraphQuery) {
			try (GraphQueryResult gqr = ((GraphQuery) query).evaluate()) {
				Set<Statement> queryResult = Iterations.asSet(gqr);

				Set<Statement> expectedResult = readExpectedGraphQueryResult(expectedResultFile);

				compareGraphs(queryResult, expectedResult);
			}

		} else if (query instanceof BooleanQuery) {

			boolean queryResult = ((BooleanQuery) query).evaluate();
			boolean expectedResult = readExpectedBooleanQueryResult(expectedResultFile);
			Assertions.assertEquals(expectedResult, queryResult);
		} else {
			throw new RuntimeException("Unexpected query type: " + query.getClass());
		}
	}

	protected TupleQueryResult runSelectQueryFile(String queryFile) throws Exception {
		String queryString = readQueryString(queryFile);

		Query query = queryManager().prepareQuery(queryString);

		if (query instanceof TupleQuery) {
			return ((TupleQuery) query).evaluate();
		}

		throw new Exception("unexpected query: " + queryString);
	}

	protected void evaluateQueryPlan(String queryFile, String expectedPlanFile) throws Exception {

		String actualQueryPlan = federationContext().getQueryManager().getQueryPlan(readQueryString(queryFile));
		String expectedQueryPlan = readResourceAsString(expectedPlanFile);
		assertQueryPlanEquals(expectedQueryPlan, actualQueryPlan);
	}

	protected void assertQueryPlanEquals(String expectedQueryPlan, String actualQueryPlan) {

		// make sure the comparison works cross operating system
		expectedQueryPlan = expectedQueryPlan.replace("\r\n", "\n");
		actualQueryPlan = actualQueryPlan.replace("\r\n", "\n");

		actualQueryPlan = actualQueryPlan.replace("sparql_localhost:18080_repositories_", "");
		actualQueryPlan = actualQueryPlan.replace("remote_", "");
		Assertions.assertEquals(expectedQueryPlan, actualQueryPlan);
	}

	protected void assertContainsAll(List<BindingSet> res, String bindingName, Set<Value> expected) {
		Assertions.assertEquals(expected,
				res.stream().map(bs -> bs.getValue(bindingName)).collect(Collectors.toSet()));
		Assertions.assertEquals(expected.size(), res.size());
	}

	protected Literal l(String value) {
		return SimpleValueFactory.getInstance().createLiteral(value);
	}

	/**
	 *
	 * @param localName
	 * @return the IRI in the instance's {@link #defaultNamespace}
	 */
	protected IRI iri(String localName) {
		return iri(defaultNamespace, localName);
	}

	protected static IRI iri(String namespace, String localName) {
		return vf.createIRI(namespace, localName);
	}

	protected static IRI fullIri(String fullIri) {
		return vf.createIRI(fullIri);
	}

	/**
	 * Read the query string from the specified resource
	 *
	 * @param queryResource
	 * @return
	 * @throws RepositoryException
	 * @throws IOException
	 */
	protected String readQueryString(String queryFile) throws RepositoryException, IOException {
		return readResourceAsString(queryFile);
	}

	/**
	 * Read resource from classpath as string, e.g. /tests/basic/data01endpoint1.ttl
	 *
	 * @param resource
	 * @return
	 * @throws IOException
	 */
	protected String readResourceAsString(String resource) throws IOException {
		try (InputStream stream = FedXBaseTest.class.getResourceAsStream(resource)) {
			return IOUtil.readString(new InputStreamReader(stream, StandardCharsets.UTF_8));
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
	protected TupleQueryResult readExpectedTupleQueryResult(String resultFile) throws Exception {
		QueryResultFormat tqrFormat = QueryResultIO.getParserFormatForFileName(resultFile).get();

		if (tqrFormat != null) {
			InputStream in = SPARQLBaseTest.class.getResourceAsStream(resultFile);

			try (in) {
				if (in == null) {
					throw new IOException("File could not be opened: " + resultFile);
				}
				TupleQueryResultParser parser = QueryResultIO.createTupleParser(tqrFormat);

				TupleQueryResultBuilder qrBuilder = new TupleQueryResultBuilder();
				parser.setQueryResultHandler(qrBuilder);

				parser.parseQueryResult(in);
				return qrBuilder.getQueryResult();
			}
		} else {
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
	protected Set<Statement> readExpectedGraphQueryResult(String resultFile) throws Exception {
		RDFFormat rdfFormat = Rio.getParserFormatForFileName(resultFile).get();

		if (rdfFormat != null) {
			RDFParser parser = Rio.createParser(rdfFormat);
			parser.setPreserveBNodeIDs(true);
			parser.setValueFactory(SimpleValueFactory.getInstance());

			Set<Statement> result = new LinkedHashSet<>();
			parser.setRDFHandler(new StatementCollector(result));

			try (InputStream in = SPARQLBaseTest.class.getResourceAsStream(resultFile)) {
				parser.parse(in, resultFile);
			}

			return result;
		} else {
			throw new RuntimeException("Unable to determine file type of results file");
		}
	}

	protected boolean readExpectedBooleanQueryResult(String resultFile) throws Exception {
		QueryResultFormat bqrFormat = BooleanQueryResultParserRegistry.getInstance()
				.getFileFormatForFileName(
						resultFile)
				.get();

		if (bqrFormat != null) {
			try (InputStream in = SPARQLBaseTest.class.getResourceAsStream(resultFile)) {
				return QueryResultIO.parseBoolean(in, bqrFormat);
			}
		} else {
			Set<Statement> resultGraph = readExpectedGraphQueryResult(resultFile);
			return DAWGTestResultSetUtil.toBooleanQueryResult(resultGraph);
		}
	}

	protected SimpleTupleQueryResultBuilder tupleQueryResultBuilder(List<String> bindingNames) {
		return new SimpleTupleQueryResultBuilder(bindingNames);
	}

	/**
	 *
	 * Note: metod can only be used after initialization phase
	 *
	 * @return the current {@link FederationContext}
	 */
	protected abstract FederationContext federationContext();

	protected QueryManager queryManager() {
		return federationContext().getQueryManager();
	}

	/**
	 * Compare two tuple query results
	 *
	 * @param queryResult
	 * @param expectedResult
	 * @param checkOrder
	 * @throws Exception
	 */
	protected void compareTupleQueryResults(TupleQueryResult queryResult, TupleQueryResult expectedResult,
			boolean checkOrder)
			throws Exception {
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

			List<BindingSet> queryBindings = Iterations.asList(queryResultTable);

			List<BindingSet> expectedBindings = Iterations.asList(expectedResultTable);

			List<BindingSet> missingBindings = new ArrayList<>(expectedBindings);
			missingBindings.removeAll(queryBindings);

			List<BindingSet> unexpectedBindings = new ArrayList<>(queryBindings);
			unexpectedBindings.removeAll(expectedBindings);

			StringBuilder message = new StringBuilder(128);

			if (!missingBindings.isEmpty()) {

				message.append("Missing bindings: \n");
				for (BindingSet bs : missingBindings) {
					message.append(bs);
					message.append("\n");
				}
			}

			if (!unexpectedBindings.isEmpty()) {
				message.append("Unexpected bindings: \n");
				for (BindingSet bs : unexpectedBindings) {
					message.append(bs);
					message.append("\n");
				}
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

			log.error(message.toString());
			Assertions.fail(message.toString());
		}

	}

	/**
	 * Compare two graphs
	 *
	 * @param queryResult
	 * @param expectedResult
	 * @throws Exception
	 */
	protected void compareGraphs(Set<Statement> queryResult, Set<Statement> expectedResult)
			throws Exception {
		if (!Models.isomorphic(expectedResult, queryResult)) {
			StringBuilder message = new StringBuilder(128);
			message.append("Expected result: \n");
			for (Statement st : expectedResult) {
				message.append(st.toString());
				message.append("\n");
			}

			message.append("Query result: \n");
			for (Statement st : queryResult) {
				message.append(st.toString());
				message.append("\n");
			}

			log.error(message.toString());
			Assertions.fail(message.toString());
		}
	}

	/**
	 * A builder for {@link TupleQueryResult}s.
	 *
	 * @author as
	 *
	 */
	public static class SimpleTupleQueryResultBuilder {

		private final List<String> bindingNames;
		private final List<BindingSet> bindings = Lists.newArrayList();

		private SimpleTupleQueryResultBuilder(List<String> bindingNames) {
			this.bindingNames = bindingNames;
		}

		/**
		 * Add the {@link BindingSet} to the result.
		 *
		 * @param b
		 * @return
		 * @throws IllegalArgumentException if the provided binding names is not a subset of the defined result binding
		 *                                  names
		 */
		public SimpleTupleQueryResultBuilder add(BindingSet b) throws IllegalArgumentException {

			// check if the binding names are a subset of defined binding names
			if (!bindingNames.containsAll(b.getBindingNames())) {
				throw new IllegalArgumentException(
						"Provided binding set does must be a subset of defined binding names: " + bindingNames
								+ ". Was: " + b.getBindingNames());
			}
			this.bindings.add(b);
			return this;
		}

		public SimpleTupleQueryResultBuilder add(List<? extends Value> values) {
			if (values.size() != bindingNames.size()) {
				throw new IllegalArgumentException("Values for each binding name required.");
			}
			BindingSet b = new ListBindingSet(bindingNames, values);
			return add(b);
		}

		@SuppressWarnings("unchecked")
		public SimpleTupleQueryResultBuilder add(List<? extends Value>... rows) {
			for (List<? extends Value> values : rows) {
				add(values);
			}
			return this;
		}

		public TupleQueryResult build() {
			return new MutableTupleQueryResult(bindingNames, bindings);
		}
	}

}
