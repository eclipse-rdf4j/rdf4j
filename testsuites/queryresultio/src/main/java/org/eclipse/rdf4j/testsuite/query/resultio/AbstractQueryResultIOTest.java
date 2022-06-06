/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.testsuite.query.resultio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQueryResultHandler;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.BasicQueryWriterSettings;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParser;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.UnsupportedQueryResultFormatException;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Ansell
 */
public abstract class AbstractQueryResultIOTest {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	private final Random random = new Random(43252333);

	/**
	 *
	 */
	public AbstractQueryResultIOTest() {
		super();
	}

	/**
	 * @return An example filename that will match the {@link QueryResultFormat} returned by {@link #getFormat()}.
	 */
	protected abstract String getFileName();

	protected abstract QueryResultFormat getFormat();

	/**
	 * Override this to customise how the tuple parsing is performed, particularly to test background and other parsing
	 * strategies.
	 *
	 * @param format The {@link TupleQueryResultFormat} for the parser.
	 * @param in     The InputStream to parse
	 * @return A {@link TupleQueryResult} that can be parsed.
	 * @throws IOException
	 * @throws QueryResultParseException
	 * @throws TupleQueryResultHandlerException
	 * @throws UnsupportedQueryResultFormatException
	 */
	protected TupleQueryResult parseTupleInternal(TupleQueryResultFormat format, InputStream in) throws IOException,
			QueryResultParseException, TupleQueryResultHandlerException, UnsupportedQueryResultFormatException {
		return QueryResultIO.parseTuple(in, format, new WeakReference<>(this));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.query.resultio.QueryResultIO#getParserFormatForFileName(java.lang.String)} .
	 */
	@Test
	public final void testGetParserFormatForFileNameString() throws Exception {
		String fileName = getFileName();

		Optional<QueryResultFormat> format;

		if (getFormat() instanceof TupleQueryResultFormat) {
			format = QueryResultIO.getParserFormatForFileName(fileName);
		} else {
			format = QueryResultIO.getBooleanParserFormatForFileName(fileName);
		}

		assertTrue(format.isPresent(), "Could not find parser for this format.");
		assertEquals(getFormat(), format.get());
	}

	protected TupleQueryResult createTupleSingleVarMultipleBindingSets() {
		List<String> bindingNames = List.of("a");

		MapBindingSet solution1 = new MapBindingSet(bindingNames.size());
		solution1.addBinding("a", vf.createIRI("foo:bar"));

		MapBindingSet solution2 = new MapBindingSet(bindingNames.size());
		solution2.addBinding("a", vf.createLiteral("2.0", CoreDatatype.XSD.DOUBLE));

		MapBindingSet solution3 = new MapBindingSet(bindingNames.size());
		solution3.addBinding("a", vf.createBNode("bnode3"));

		MapBindingSet solution4 = new MapBindingSet(bindingNames.size());
		solution4.addBinding("a", vf.createLiteral("''single-quoted string", XSD.STRING));

		MapBindingSet solution5 = new MapBindingSet(bindingNames.size());
		solution5.addBinding("a", vf.createLiteral("\"\"double-quoted string", XSD.STRING));

		MapBindingSet solution6 = new MapBindingSet(bindingNames.size());
		solution6.addBinding("a", vf.createLiteral("space at the end         ", CoreDatatype.XSD.STRING));

		MapBindingSet solution7 = new MapBindingSet(bindingNames.size());
		solution7.addBinding("a", vf.createLiteral("space at the end         ", XSD.STRING));

		MapBindingSet solution8 = new MapBindingSet(bindingNames.size());
		solution8.addBinding("a", vf.createLiteral("\"\"double-quoted string with no datatype"));

		MapBindingSet solution9 = new MapBindingSet(bindingNames.size());
		solution9.addBinding("a", vf.createLiteral("newline at the end \n", CoreDatatype.XSD.STRING));

		MapBindingSet solution10 = new MapBindingSet(bindingNames.size());
		solution10.addBinding("a", vf.createTriple(vf.createIRI("urn:a"), RDF.TYPE, vf.createIRI("urn:b")));

		List<? extends BindingSet> bindingSetList = Arrays.asList(solution1, solution2, solution3, solution4, solution5,
				solution6, solution7, solution8, solution9, solution10);

		IteratingTupleQueryResult result = new IteratingTupleQueryResult(bindingNames, bindingSetList);

		return result;
	}

	protected TupleQueryResult createTupleMultipleBindingSets() {
		List<String> bindingNames = Arrays.asList("a", "b", "c");

		MapBindingSet solution1 = new MapBindingSet(bindingNames.size());
		solution1.addBinding("a", vf.createIRI("foo:bar"));
		solution1.addBinding("b", vf.createBNode("bnode"));
		solution1.addBinding("c", vf.createLiteral("baz"));

		MapBindingSet solution2 = new MapBindingSet(bindingNames.size());
		solution2.addBinding("a", vf.createLiteral("1", CoreDatatype.XSD.INTEGER));
		solution2.addBinding("c", vf.createLiteral("Hello World!", "en"));

		MapBindingSet solution3 = new MapBindingSet(bindingNames.size());
		solution3.addBinding("a", vf.createIRI("http://example.org/test/ns/bindingA"));
		solution3.addBinding("b", vf.createLiteral("http://example.com/other/ns/bindingB"));
		solution3.addBinding("c", vf.createIRI("http://example.com/other/ns/binding,C"));

		MapBindingSet solution4 = new MapBindingSet(bindingNames.size());
		solution4.addBinding("a", vf.createLiteral("string with newline at the end       \n"));
		solution4.addBinding("b", vf.createLiteral("string with space at the end         "));
		solution4.addBinding("c", vf.createLiteral("    "));

		MapBindingSet solution5 = new MapBindingSet(bindingNames.size());
		solution5.addBinding("a", vf.createLiteral("''single-quoted string"));
		solution5.addBinding("b", vf.createLiteral("\"\"double-quoted string"));
		solution5.addBinding("c", vf.createLiteral("		unencoded tab characters followed by encoded \t\t"));

		MapBindingSet solution6 = new MapBindingSet(bindingNames.size());
		solution6.addBinding("a", vf.createTriple(vf.createIRI("urn:a"), RDF.TYPE, vf.createIRI("urn:b")));
		solution6.addBinding("b", vf.createIRI("urn:test"));
		solution6.addBinding("c", vf.createBNode("bnode1"));

		List<? extends BindingSet> bindingSetList = Arrays.asList(solution1, solution2, solution3, solution4,
				solution5);

		IteratingTupleQueryResult result = new IteratingTupleQueryResult(bindingNames, bindingSetList);

		return result;
	}

	/**
	 * @return A map of test namespaces for the writer to handle.
	 */
	protected Map<String, String> getNamespaces() {
		Map<String, String> result = new HashMap<>();
		result.put("test", "http://example.org/test/ns/");
		result.put("other", "http://example.com/other/ns/");
		return result;
	}

	/**
	 * @return A map of test namespaces for the writer to handle, including an empty namespace.
	 */
	protected Map<String, String> getNamespacesWithEmpty() {
		Map<String, String> result = new HashMap<>();
		result.put("test", "http://example.org/test/ns/");
		result.put("other", "http://example.com/other/ns/");
		result.put("", "http://other.example.org/ns/");
		return result;
	}

	protected TupleQueryResult createTupleNoBindingSets() {
		List<String> bindingNames = Arrays.asList("a", "b", "c");

		List<? extends BindingSet> bindingSetList = Collections.emptyList();

		IteratingTupleQueryResult result = new IteratingTupleQueryResult(bindingNames, bindingSetList);

		return result;
	}

	protected void doTupleLinks(TupleQueryResultFormat format, TupleQueryResult input, TupleQueryResult expected,
			List<String> links) throws QueryResultHandlerException, QueryEvaluationException, QueryResultParseException,
			UnsupportedQueryResultFormatException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		writer.startDocument();
		writer.startHeader();
		writer.handleLinks(links);
		QueryResults.report(input, writer);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TupleQueryResult output = parseTupleInternal(format, in);

		assertQueryResultsEqual(expected, output);
	}

	protected void doTupleLinksAndStylesheet(TupleQueryResultFormat format, TupleQueryResult input,
			TupleQueryResult expected, List<String> links, String stylesheetUrl) throws QueryResultHandlerException,
			QueryEvaluationException, QueryResultParseException, UnsupportedQueryResultFormatException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		writer.startDocument();
		writer.handleStylesheet(stylesheetUrl);
		writer.startHeader();
		writer.handleLinks(links);
		QueryResults.report(input, writer);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TupleQueryResult output = parseTupleInternal(format, in);

		assertQueryResultsEqual(expected, output);
	}

	protected void doTupleLinksAndStylesheetAndNamespaces(TupleQueryResultFormat format, TupleQueryResult input,
			TupleQueryResult expected, List<String> links, String stylesheetUrl, Map<String, String> namespaces)
			throws QueryResultHandlerException, QueryEvaluationException, QueryResultParseException,
			UnsupportedQueryResultFormatException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		for (String nextPrefix : namespaces.keySet()) {
			writer.handleNamespace(nextPrefix, namespaces.get(nextPrefix));
		}
		writer.startDocument();
		writer.handleStylesheet(stylesheetUrl);
		writer.startHeader();
		writer.handleLinks(links);
		QueryResults.report(input, writer);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TupleQueryResult output = parseTupleInternal(format, in);

		assertQueryResultsEqual(expected, output);
	}

	/**
	 * Test specifically for QName support.
	 */
	protected void doTupleLinksAndStylesheetAndNamespacesQName(TupleQueryResultFormat format, TupleQueryResult input,
			TupleQueryResult expected, List<String> links, String stylesheetUrl, Map<String, String> namespaces)
			throws QueryResultHandlerException, QueryEvaluationException, QueryResultParseException,
			UnsupportedQueryResultFormatException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		if (writer.getSupportedSettings().contains(BasicQueryWriterSettings.ADD_SESAME_QNAME)) {
			// System.out.println("Enabling Sesame qname support");
			writer.getWriterConfig().set(BasicQueryWriterSettings.ADD_SESAME_QNAME, true);
		}

		for (String nextPrefix : namespaces.keySet()) {
			writer.handleNamespace(nextPrefix, namespaces.get(nextPrefix));
		}
		writer.startDocument();
		writer.handleStylesheet(stylesheetUrl);
		writer.startHeader();
		writer.handleLinks(links);
		QueryResults.report(input, writer);

		String result = out.toString(StandardCharsets.UTF_8);

		// System.out.println("output: " + result);

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TupleQueryResult output = parseTupleInternal(format, in);

		assertQueryResultsEqual(expected, output);

		// only do this additional test if sesame q:qname is supported by this
		// writer
		if (writer.getSupportedSettings().contains(BasicQueryWriterSettings.ADD_SESAME_QNAME)) {
			assertTrue(result.contains("test:bindingA"));
			assertFalse(result.contains("other:bindingB"));
			assertTrue(result.contains("other:binding,C"));
		}

	}

	/**
	 * Test specifically for JSONP callback support.
	 */
	protected void doTupleJSONPCallback(TupleQueryResultFormat format, TupleQueryResult input,
			TupleQueryResult expected) throws QueryResultHandlerException, QueryEvaluationException,
			QueryResultParseException, UnsupportedQueryResultFormatException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);

		// only do this test if the callback is enabled
		if (writer.getSupportedSettings().contains(BasicQueryWriterSettings.JSONP_CALLBACK)) {

			String callback = "nextfunctionname" + Math.abs(random.nextInt());

			writer.getWriterConfig().set(BasicQueryWriterSettings.JSONP_CALLBACK, callback);

			QueryResults.report(input, writer);

			String result = out.toString(StandardCharsets.UTF_8);

			// System.out.println("output: " + result);

			assertTrue(result.startsWith(callback + "("));
			assertTrue(result.endsWith(");"));

			// Strip off the callback function and verify that it contains a
			// valid
			// JSON object containing the correct results
			result = result.substring(callback.length() + 1, result.length() - 2);

			ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
			TupleQueryResult output = parseTupleInternal(format, in);

			assertQueryResultsEqual(expected, output);
		}
	}

	protected void doTupleNoLinks(TupleQueryResultFormat format, TupleQueryResult input, TupleQueryResult expected)
			throws IOException, QueryResultParseException, UnsupportedQueryResultFormatException,
			QueryEvaluationException, QueryResultHandlerException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		QueryResultIO.writeTuple(input, format, out);
		input.close();

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TupleQueryResult output = parseTupleInternal(format, in);

		assertQueryResultsEqual(expected, output);
	}

	protected void doTupleStylesheet(TupleQueryResultFormat format, TupleQueryResult input, TupleQueryResult expected,
			String stylesheetUrl) throws QueryResultHandlerException, QueryEvaluationException,
			QueryResultParseException, UnsupportedQueryResultFormatException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		writer.handleStylesheet(stylesheetUrl);
		QueryResults.report(input, writer);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TupleQueryResult output = parseTupleInternal(format, in);

		assertQueryResultsEqual(expected, output);
	}

	protected void doTupleLinksAndStylesheetNoStarts(TupleQueryResultFormat format, TupleQueryResult input,
			TupleQueryResult expected, List<String> links, String stylesheetUrl) throws QueryResultHandlerException,
			QueryEvaluationException, QueryResultParseException, UnsupportedQueryResultFormatException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		// Test for handling when startDocument and startHeader are not called
		writer.handleStylesheet(stylesheetUrl);
		writer.handleLinks(links);
		QueryResults.report(input, writer);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TupleQueryResult output = parseTupleInternal(format, in);

		assertQueryResultsEqual(expected, output);
	}

	protected void doTupleLinksAndStylesheetMultipleEndHeaders(TupleQueryResultFormat format, TupleQueryResult input,
			TupleQueryResult expected, List<String> links, String stylesheetUrl) throws QueryResultHandlerException,
			QueryEvaluationException, QueryResultParseException, UnsupportedQueryResultFormatException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		// Test for handling when startDocument and startHeader are not called
		writer.handleStylesheet(stylesheetUrl);
		writer.startQueryResult(input.getBindingNames());
		writer.handleLinks(links);
		writer.endHeader();
		writer.endHeader();
		try (input) {
			while (input.hasNext()) {
				BindingSet bindingSet = input.next();
				writer.handleSolution(bindingSet);
			}
		}
		writer.endQueryResult();

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TupleQueryResult output = parseTupleInternal(format, in);

		assertQueryResultsEqual(expected, output);
	}

	protected void assertQueryResultsEqual(TupleQueryResult expected, TupleQueryResult output)
			throws QueryEvaluationException, TupleQueryResultHandlerException, QueryResultHandlerException,
			UnsupportedEncodingException {
		assertTrue(QueryResults.equals(expected, output));
	}

	protected void doTupleMissingStartQueryResult(TupleQueryResultFormat format, TupleQueryResult input,
			TupleQueryResult expected, List<String> links, String stylesheetUrl) throws QueryResultHandlerException,
			QueryEvaluationException, QueryResultParseException, UnsupportedQueryResultFormatException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		// Test for handling when startDocument and startHeader are not called
		writer.startDocument();
		writer.handleStylesheet(stylesheetUrl);
		writer.startHeader();
		writer.handleLinks(links);
		writer.endHeader();
		try (input) {
			while (input.hasNext()) {
				BindingSet bindingSet = input.next();
				writer.handleSolution(bindingSet);
			}
			writer.endQueryResult();
			fail("Expected exception when calling handleSolution without startQueryResult");
		} catch (IllegalStateException ise) {
			// Expected exception
		}
	}

	/**
	 * Tests that parsing a tuple results set without specifying a {@link TupleQueryResultHandler} does not throw any
	 * exceptions.
	 *
	 * @param format
	 * @param input
	 * @throws QueryResultParseException
	 * @throws IOException
	 * @throws TupleQueryResultHandlerException
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryResultFormatException
	 */
	protected void doTupleNoHandler(TupleQueryResultFormat format, TupleQueryResult input, TupleQueryResult expected)
			throws QueryResultParseException, IOException, TupleQueryResultHandlerException,
			UnsupportedQueryResultFormatException, QueryEvaluationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		QueryResultIO.writeTuple(input, format, out);
		out.flush();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TupleQueryResultParser parser = QueryResultIO.createTupleParser(format);
		// This should perform a full parse to verify the document, even though
		// the handler is not set
		parser.parseQueryResult(in);
	}

	/**
	 * Tests that the parser returned for a TupleQueryResultFormat is not able to parse a BooleanQueryResultFormat using
	 * the deprecated {@link TupleQueryResultParser#parse(java.io.InputStream)} method, and that it does indeed through
	 * an exception of type {@link QueryResultParseException}.
	 *
	 * @param format
	 * @param input
	 * @param matchingBooleanFormat A BooleanQueryResultFormat that matches the given TupleQueryResultFormat .
	 * @throws IOException
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryResultFormatException
	 * @throws QueryResultHandlerException
	 * @see <a href="https://openrdf.atlassian.net/browse/SES-1860">SES-1860</a>
	 */
	protected void doTupleParseNoHandlerOnBooleanResults(TupleQueryResultFormat format, boolean input,
			BooleanQueryResultFormat matchingBooleanFormat)
			throws UnsupportedQueryResultFormatException, QueryResultHandlerException, IOException {
		if (matchingBooleanFormat == null) {
			// This test is not supported for this boolean format
			return;
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		BooleanQueryResultWriter writer = QueryResultIO.createBooleanWriter(matchingBooleanFormat, out);
		// Test for handling when handler is not set
		writer.handleBoolean(input);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TupleQueryResultParser parser = QueryResultIO.createTupleParser(format);

		try {
			parser.parse(in);
			fail("Did not find expected parse exception");
		} catch (QueryResultParseException expected) {

		}
	}

	protected void doBooleanNoLinks(BooleanQueryResultFormat format, boolean input)
			throws IOException, QueryResultHandlerException, QueryResultParseException,
			UnsupportedQueryResultFormatException, QueryEvaluationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		QueryResultIO.writeBoolean(input, format, out);
		out.flush();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		boolean output = QueryResultIO.parseBoolean(in, format);

		assertEquals(output, input);
	}

	protected void doBooleanLinksOnly(BooleanQueryResultFormat format, boolean input, List<String> links)
			throws IOException, QueryResultHandlerException, QueryResultParseException,
			UnsupportedQueryResultFormatException, QueryEvaluationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		BooleanQueryResultWriter writer = QueryResultIO.createBooleanWriter(format, out);
		writer.handleLinks(links);
		writer.handleBoolean(input);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		boolean output = QueryResultIO.parseBoolean(in, format);

		assertEquals(output, input);
	}

	protected void doInvalidBooleanAfterStartQueryResult(BooleanQueryResultFormat format, boolean input,
			List<String> links) throws IOException, QueryResultHandlerException, QueryResultParseException,
			UnsupportedQueryResultFormatException, QueryEvaluationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		BooleanQueryResultWriter writer = QueryResultIO.createBooleanWriter(format, out);
		writer.handleLinks(links);
		// Determine whether this writer also supports startQueryResult, if not,
		// this test is irrelevant as it will fail early
		boolean supported = true;
		try {
			writer.startQueryResult(List.of("foo"));
		} catch (UnsupportedOperationException uoe) {
			// Boolean writers are allowed to throw this for startQueryResult
			supported = false;
		}

		if (supported) {
			try {
				// After calling startQueryResult, we should not be able to call
				// handleBoolean without an exception occuring
				writer.handleBoolean(input);
				fail("Did not find expected exception");
			} catch (QueryResultHandlerException e) {
				// Expected
			}
		}
	}

	protected void doBooleanLinks(BooleanQueryResultFormat format, boolean input, List<String> links)
			throws IOException, QueryResultHandlerException, QueryResultParseException,
			UnsupportedQueryResultFormatException, QueryEvaluationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		BooleanQueryResultWriter writer = QueryResultIO.createBooleanWriter(format, out);
		writer.startDocument();
		writer.startHeader();
		writer.handleLinks(links);
		writer.handleBoolean(input);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		boolean output = QueryResultIO.parseBoolean(in, format);

		assertEquals(output, input);
	}

	protected void doBooleanLinksAndStylesheet(BooleanQueryResultFormat format, boolean input, List<String> links,
			String stylesheetUrl) throws IOException, QueryResultHandlerException, QueryResultParseException,
			UnsupportedQueryResultFormatException, QueryEvaluationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		BooleanQueryResultWriter writer = QueryResultIO.createBooleanWriter(format, out);
		writer.startDocument();
		writer.handleStylesheet(stylesheetUrl);
		writer.startHeader();
		writer.handleLinks(links);
		writer.handleBoolean(input);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		boolean output = QueryResultIO.parseBoolean(in, format);

		assertEquals(output, input);
	}

	protected void doBooleanLinksAndStylesheetAndNamespaces(BooleanQueryResultFormat format, boolean input,
			List<String> links, String stylesheetUrl, Map<String, String> namespaces)
			throws IOException, QueryResultHandlerException, QueryResultParseException,
			UnsupportedQueryResultFormatException, QueryEvaluationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		BooleanQueryResultWriter writer = QueryResultIO.createBooleanWriter(format, out);
		for (String nextPrefix : namespaces.keySet()) {
			writer.handleNamespace(nextPrefix, namespaces.get(nextPrefix));
		}
		writer.startDocument();
		writer.handleStylesheet(stylesheetUrl);
		writer.startHeader();
		writer.handleLinks(links);
		writer.handleBoolean(input);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		boolean output = QueryResultIO.parseBoolean(in, format);

		assertEquals(output, input);
	}

	protected void doBooleanStylesheet(BooleanQueryResultFormat format, boolean input, String stylesheetUrl)
			throws IOException, QueryResultHandlerException, QueryResultParseException,
			UnsupportedQueryResultFormatException, QueryEvaluationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		BooleanQueryResultWriter writer = QueryResultIO.createBooleanWriter(format, out);
		writer.handleStylesheet(stylesheetUrl);
		writer.handleBoolean(input);

		// System.out.println("output: " + out.toString(StandardCharsets.UTF_8));

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		boolean output = QueryResultIO.parseBoolean(in, format);

		assertEquals(output, input);
	}

	/**
	 * Tests that parsing a boolean without specifying a {@link BooleanQueryResultHandler} does not throw any
	 * exceptions.
	 *
	 * @param format
	 * @param input
	 * @throws QueryResultParseException
	 * @throws IOException
	 */
	protected void doBooleanNoHandler(BooleanQueryResultFormat format, boolean input)
			throws QueryResultParseException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		QueryResultIO.writeBoolean(input, format, out);
		out.flush();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		BooleanQueryResultParser parser = QueryResultIO.createBooleanParser(format);
		assertEquals(input, parser.parse(in));
	}

	/**
	 * Tests that the parser returned for a BooleanQueryResultFormat is not able to parse a TupleQueryResultFormat using
	 * the deprecated {@link BooleanQueryResultParser#parse(java.io.InputStream)} method, and that it does indeed
	 * through an exception of type {@link QueryResultParseException}.
	 *
	 * @param format
	 * @param tqr
	 * @param matchingTupleFormat A TupleQueryResultFormat that matches the given BooleanQueryResultFormat.
	 * @throws IOException
	 * @throws QueryEvaluationException
	 * @throws UnsupportedQueryResultFormatException
	 * @throws TupleQueryResultHandlerException
	 * @see <a href="https://openrdf.atlassian.net/browse/SES-1860">SES-1860</a>
	 */
	protected void doBooleanParseNoHandlerOnTupleResults(BooleanQueryResultFormat format, TupleQueryResult tqr,
			TupleQueryResultFormat matchingTupleFormat) throws TupleQueryResultHandlerException,
			UnsupportedQueryResultFormatException, QueryEvaluationException, IOException {
		if (matchingTupleFormat == null) {
			// This test is not supported for this boolean format
			return;
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		QueryResultIO.writeTuple(tqr, matchingTupleFormat, out);
		out.flush();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		BooleanQueryResultParser parser = QueryResultIO.createBooleanParser(format);
		try {
			parser.parse(in);
			fail("Did not find expected parse exception");
		} catch (QueryResultParseException e) {

		}
	}

}
