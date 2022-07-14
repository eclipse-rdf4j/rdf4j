/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.nquads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesParserSettings;
import org.eclipse.rdf4j.rio.helpers.SimpleParseLocationListener;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JUnit test for the N-Quads parser that uses the tests that are available
 * <a href="http://www.w3.org/2013/N-QuadsTests/">online</a>.
 */
public abstract class AbstractNQuadsParserUnitTest {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String NQUADS_TEST_URL = "http://www.w3.org/2000/10/rdf-tests/rdfcore/ntriples/test.nt";

	private static final String NQUADS_TEST_FILE = "/testcases/nquads/test1.nq";

	private static final String NTRIPLES_TEST_URL = "http://www.w3.org/2000/10/rdf-tests/rdfcore/ntriples/test.nt";

	private static final String NTRIPLES_TEST_FILE = "/testcases/ntriples/test.nt";

	private RDFParser parser;

	private TestRDFHandler rdfHandler;

	@BeforeEach
	public void setUp() throws Exception {
		parser = createRDFParser();
		rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(this.rdfHandler);
	}

	@AfterEach
	public void tearDown() throws Exception {
		parser = null;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public void testNQuadsFile() throws Exception {
		RDFParser nquadsParser = createRDFParser();
		nquadsParser.setRDFHandler(new AbstractRDFHandler() {
		});

		try (InputStream in = AbstractNQuadsParserUnitTest.class.getResourceAsStream(NQUADS_TEST_FILE)) {
			nquadsParser.parse(in, NQUADS_TEST_URL);
		} catch (RDFParseException e) {
			fail("NQuadsParser failed to parse N-Quads test document: " + e.getMessage());
		}
	}

	/**
	 * The N-Quads parser must be able to parse the N-Triples test file without error.
	 */
	public void testNTriplesFile() throws Exception {
		RDFParser nquadsParser = createRDFParser();
		nquadsParser.setRDFHandler(new AbstractRDFHandler() {
		});

		try (InputStream in = AbstractNQuadsParserUnitTest.class.getResourceAsStream(NTRIPLES_TEST_FILE)) {
			nquadsParser.parse(in, NTRIPLES_TEST_URL);
		} catch (RDFParseException e) {
			fail("NQuadsParser failed to parse N-Triples test document: " + e.getMessage());
		}
	}

	/**
	 * Tests the correct behavior with incomplete input.
	 */
	@Test
	public void testIncompleteParsingWithoutPeriod() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"<http://s> <http://p> <http://o> <http://g>".getBytes());
		try {
			parser.parse(bais, "http://test.base.uri");
			fail("Expected exception when not inserting a trailing period at the end of a statement.");
		} catch (RDFParseException rdfpe) {
			// FIXME: Enable this test when first line number is 1 in parser
			// instead of -1
			// Assert.assertEquals(1, rdfpe.getLineNumber());
			// FIXME: Enable column numbers when parser supports them
			// Assert.assertEquals(44, rdfpe.getColumnNumber());
		}
	}

	/**
	 * Tests the behaviour with non-whitespace characters after a period character without a context.
	 *
	 * @throws RDFHandlerException
	 * @throws IOException
	 * @throws RDFParseException
	 */
	@Test
	public void testNonWhitespaceAfterPeriodNoContext() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"<http://www.wrong.com> <http://wrong.com/1.1/tt> \"x\"^^<http://xxx.net/int> . <http://path.to.graph> "
						.getBytes());
		try {
			parser.parse(bais, "http://base-uri");
			fail("Expected exception when there is non-whitespace characters after a period.");
		} catch (RDFParseException rdfpe) {
			assertEquals(1, rdfpe.getLineNumber());
			// FIXME: Enable column numbers when parser supports them
			// Assert.assertEquals(44, rdfpe.getColumnNumber());
		}
	}

	/**
	 * Tests the behaviour with non-whitespace characters after a period character without a context.
	 *
	 * @throws RDFHandlerException
	 * @throws IOException
	 * @throws RDFParseException
	 */
	@Test
	public void testNonWhitespaceAfterPeriodWithContext() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"<http://www.wrong.com> <http://wrong.com/1.1/tt> \"x\"^^<http://xxx.net/int> <http://path.to.graph> . <thisisnotlegal> "
						.getBytes());
		try {
			parser.parse(bais, "http://base-uri");
			fail("Expected exception when there is non-whitespace characters after a period.");
		} catch (RDFParseException rdfpe) {
			assertEquals(1, rdfpe.getLineNumber());
			// FIXME: Enable column numbers when parser supports them
			// Assert.assertEquals(44, rdfpe.getColumnNumber());
		}
	}

	/**
	 * Tests the correct behaviour with no context.
	 */
	@Test
	public void testParseNoContext() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream("<http://s> <http://p> <http://o> .".getBytes());
		parser.parse(bais, "http://base-uri");
	}

	/**
	 * Tests parsing of empty lines and comments.
	 */
	@Test
	public void testParseEmptyLinesAndComments() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"  \n\n\n# This is a comment\n\n#this is another comment.".getBytes());
		final TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(rdfHandler);
		parser.parse(bais, "http://test.base.uri");
		assertEquals(rdfHandler.getStatements().size(), 0);
	}

	/**
	 * Tests basic N-Quads parsing.
	 */
	@Test
	public void testParseBasic() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"<http://www.v/dat/4b> <http://www.w3.org/20/ica#dtend> <http://sin/value/2> <http://sin.siteserv.org/def/> ."
						.getBytes());
		final TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(rdfHandler);
		parser.parse(bais, "http://test.base.uri");
		assertEquals(1, rdfHandler.getStatements().size());
		final Statement statement = rdfHandler.getStatements().iterator().next();
		assertEquals("http://www.v/dat/4b", statement.getSubject().stringValue());
		assertEquals("http://www.w3.org/20/ica#dtend", statement.getPredicate().stringValue());
		assertTrue(statement.getObject() instanceof IRI);
		assertEquals("http://sin/value/2", statement.getObject().stringValue());
		assertEquals("http://sin.siteserv.org/def/", statement.getContext().stringValue());
	}

	/**
	 * Tests basic N-Quads parsing with blank node.
	 */
	@Test
	public void testParseBasicBNode() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"_:a123456768 <http://www.w3.org/20/ica#dtend> <http://sin/value/2> <http://sin.siteserv.org/def/>."
						.getBytes());
		final TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(rdfHandler);
		parser.parse(bais, "http://test.base.uri");
		assertThat(rdfHandler.getStatements()).hasSize(1);
		final Statement statement = rdfHandler.getStatements().iterator().next();
		assertTrue(statement.getSubject() instanceof BNode);
		assertEquals("http://www.w3.org/20/ica#dtend", statement.getPredicate().stringValue());
		assertTrue(statement.getObject() instanceof IRI);
		assertEquals("http://sin/value/2", statement.getObject().stringValue());
		assertEquals("http://sin.siteserv.org/def/", statement.getContext().stringValue());
	}

	/**
	 * Tests basic N-Quads parsing with literal.
	 */
	@Test
	public void testParseBasicLiteral() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"_:a123456768 <http://www.w3.org/20/ica#dtend> \"2010-05-02\" <http://sin.siteserv.org/def/>."
						.getBytes());
		final TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(rdfHandler);
		parser.parse(bais, "http://test.base.uri");
		assertThat(rdfHandler.getStatements()).hasSize(1);
		final Statement statement = rdfHandler.getStatements().iterator().next();
		assertTrue(statement.getSubject() instanceof BNode);
		assertEquals("http://www.w3.org/20/ica#dtend", statement.getPredicate().stringValue());
		assertTrue(statement.getObject() instanceof Literal);
		assertEquals("2010-05-02", statement.getObject().stringValue());
		assertEquals("http://sin.siteserv.org/def/", statement.getContext().stringValue());
	}

	/**
	 * Tests N-Quads parsing with literal and language.
	 */
	@Test
	public void testParseBasicLiteralLang() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"<http://www.v/dat/4b2-21> <http://www.w3.org/20/ica#dtend> \"2010-05-02\"@en <http://sin.siteserv.org/def/>."
						.getBytes());
		final TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(rdfHandler);
		parser.parse(bais, "http://test.base.uri");
		final Statement statement = rdfHandler.getStatements().iterator().next();
		assertEquals("http://www.v/dat/4b2-21", statement.getSubject().stringValue());
		assertEquals("http://www.w3.org/20/ica#dtend", statement.getPredicate().stringValue());
		assertTrue(statement.getObject() instanceof Literal);
		Literal object = (Literal) statement.getObject();
		assertEquals("2010-05-02", object.stringValue());
		assertEquals("en", object.getLanguage().orElse(null));
		assertEquals(RDF.LANGSTRING, object.getDatatype());
		assertEquals("http://sin.siteserv.org/def/", statement.getContext().stringValue());
	}

	/**
	 * Tests N-Quads parsing with literal and datatype.
	 */
	@Test
	public void testParseBasicLiteralDatatype() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				("<http://www.v/dat/4b2-21> " + "<http://www.w3.org/20/ica#dtend> "
						+ "\"2010\"^^<http://www.w3.org/2001/XMLSchema#integer> " + "<http://sin.siteserv.org/def/>.")
								.getBytes());
		final TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(rdfHandler);
		parser.parse(bais, "http://test.base.uri");
		final Statement statement = rdfHandler.getStatements().iterator().next();
		assertEquals("http://www.v/dat/4b2-21", statement.getSubject().stringValue());
		assertEquals("http://www.w3.org/20/ica#dtend", statement.getPredicate().stringValue());
		assertTrue(statement.getObject() instanceof Literal);
		Literal object = (Literal) statement.getObject();
		assertEquals("2010", object.stringValue());
		assertFalse(object.getLanguage().isPresent());
		assertEquals("http://www.w3.org/2001/XMLSchema#integer", object.getDatatype().toString());
		assertEquals("http://sin.siteserv.org/def/", statement.getContext().stringValue());
	}

	/**
	 * Tests N-Quads parsing with literal and datatype using a prefix, which is illegal in NQuads, but legal in
	 * N3/Turtle that may otherwise look like NQuads
	 */
	@Test
	public void testParseBasicLiteralDatatypePrefix() throws RDFHandlerException, IOException {

		final ByteArrayInputStream bais = new ByteArrayInputStream(
				("<http://www.v/dat/4b2-21> " + "<http://www.w3.org/20/ica#dtend> " + "\"2010\"^^xsd:integer "
						+ "<http://sin.siteserv.org/def/>.").getBytes());
		final TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(rdfHandler);
		try {
			parser.parse(bais, "http://test.base.uri");
			fail("Expected exception when passing in a datatype using an N3 style prefix");
		} catch (RDFParseException rdfpe) {
			assertEquals(1, rdfpe.getLineNumber());
			// FIXME: Enable column numbers when parser supports them
			// assertEquals(69, rdfpe.getColumnNumber());
		}
	}

	/**
	 * Tests the correct support for literal escaping.
	 */
	@Test
	public void testLiteralEscapeManagement1() throws RDFHandlerException, IOException, RDFParseException {
		TestParseLocationListener parseLocationListener = new TestParseLocationListener();
		TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setParseLocationListener(parseLocationListener);
		parser.setRDFHandler(rdfHandler);

		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"<http://a> <http://b> \"\\\\\" <http://c> .".getBytes());
		parser.parse(bais, "http://base-uri");

		rdfHandler.assertHandler(1);
		// parseLocationListener.assertListener(1, 40);
		// FIXME: Enable column numbers when parser supports them
		parseLocationListener.assertListener(1, 1);
	}

	/**
	 * Tests the correct support for literal escaping.
	 */
	@Test
	public void testLiteralEscapeManagement2() throws RDFHandlerException, IOException, RDFParseException {
		TestParseLocationListener parseLocationListener = new TestParseLocationListener();
		TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setParseLocationListener(parseLocationListener);
		parser.setRDFHandler(rdfHandler);

		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"<http://a> <http://b> \"Line text 1\\nLine text 2\" <http://c> .".getBytes());
		parser.parse(bais, "http://base-uri");

		rdfHandler.assertHandler(1);
		final Value object = rdfHandler.getStatements().iterator().next().getObject();
		assertTrue(object instanceof Literal);
		final String literalContent = ((Literal) object).getLabel();
		assertEquals("Line text 1\nLine text 2", literalContent);
	}

	/**
	 * Tests the correct decoding of UTF-8 encoded chars in URIs.
	 */
	@Test
	public void testURIDecodingManagement() throws RDFHandlerException, IOException, RDFParseException {
		TestParseLocationListener parseLocationListener = new TestParseLocationListener();
		TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setParseLocationListener(parseLocationListener);
		parser.setRDFHandler(rdfHandler);

		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"<http://s/\\u306F\\u3080> <http://p/\\u306F\\u3080> <http://o/\\u306F\\u3080> <http://g/\\u306F\\u3080> ."
						.getBytes());
		parser.parse(bais, "http://base-uri");

		rdfHandler.assertHandler(1);
		final Statement statement = rdfHandler.getStatements().iterator().next();

		final Resource subject = statement.getSubject();
		assertTrue(subject instanceof IRI);
		final String subjectURI = subject.toString();
		assertEquals("http://s/はむ", subjectURI);

		final Resource predicate = statement.getPredicate();
		assertTrue(predicate instanceof IRI);
		final String predicateURI = predicate.toString();
		assertEquals("http://p/はむ", predicateURI);

		final Value object = statement.getObject();
		assertTrue(object instanceof IRI);
		final String objectURI = object.toString();
		assertEquals("http://o/はむ", objectURI);

		final Resource graph = statement.getContext();
		assertTrue(graph instanceof IRI);
		final String graphURI = graph.toString();
		assertEquals("http://g/はむ", graphURI);
	}

	@Test
	public void testUnicodeLiteralDecoding() throws RDFHandlerException, IOException, RDFParseException {
		TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(rdfHandler);
		final String INPUT_LITERAL_PLAIN = "[は]";
		final String INPUT_LITERAL_ENCODED = "[\\u306F]";
		final String INPUT_STRING = String.format("<http://a> <http://b> \"%s\" <http://c> .", INPUT_LITERAL_ENCODED);
		final ByteArrayInputStream bais = new ByteArrayInputStream(INPUT_STRING.getBytes());
		parser.parse(bais, "http://base-uri");

		rdfHandler.assertHandler(1);
		final Literal obj = (Literal) rdfHandler.getStatements().iterator().next().getObject();
		assertEquals(INPUT_LITERAL_PLAIN, obj.getLabel());
	}

	@Test
	public void testWrongUnicodeEncodedCharFail() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"<http://s> <http://p> \"\\u123X\" <http://g> .".getBytes());
		try {
			parser.parse(bais, "http://test.base.uri");
			fail("Expected exception when an incorrect unicode character is included");
		} catch (RDFParseException rdfpe) {
			assertEquals(1, rdfpe.getLineNumber());
			// FIXME: Enable column numbers when parser supports them
			// assertEquals(30, rdfpe.getColumnNumber());
		}
	}

	/**
	 * Tests the correct support for EOS exception.
	 */
	@Test
	public void testEndOfStreamReached() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				"<http://a> <http://b> \"\\\" <http://c> .".getBytes());
		try {
			parser.parse(bais, "http://test.base.uri");
			fail("Expected exception when a literal is not closed");
		} catch (RDFParseException rdfpe) {
			// FIXME: Enable this test when first line number is 1 in parser
			// instead of -1
			// assertEquals(1, rdfpe.getLineNumber());
			// FIXME: Enable column numbers when parser supports them
			// assertEquals(39, rdfpe.getColumnNumber());
		}
	}

	/**
	 * Tests the parser with all cases defined by the NQuads grammar.
	 */
	@Test
	public void testParserWithAllCases() throws IOException, RDFParseException, RDFHandlerException {
		TestParseLocationListener parseLocationListerner = new TestParseLocationListener();
		// SpecificTestRDFHandler rdfHandler = new SpecificTestRDFHandler();
		parser.setParseLocationListener(parseLocationListerner);
		parser.setRDFHandler(rdfHandler);

		BufferedReader br = new BufferedReader(new InputStreamReader(
				AbstractNQuadsParserUnitTest.class.getResourceAsStream("/testcases/nquads/test1.nq")));
		parser.parse(br, "http://test.base.uri");

		rdfHandler.assertHandler(6);
		parseLocationListerner.assertListener(8, 1);
	}

	/**
	 * Tests parser with real data.
	 */
	@Test
	public void testParserWithRealData() throws IOException, RDFParseException, RDFHandlerException {
		TestParseLocationListener parseLocationListener = new TestParseLocationListener();
		TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setParseLocationListener(parseLocationListener);
		parser.setRDFHandler(rdfHandler);

		parser.parse(AbstractNQuadsParserUnitTest.class.getResourceAsStream("/testcases/nquads/test2.nq"),
				"http://test.base.uri");

		rdfHandler.assertHandler(400);
		parseLocationListener.assertListener(400, 1);
	}

	@Test
	public void testStatementWithInvalidLiteralContentAndIgnoreValidation()
			throws RDFHandlerException, IOException, RDFParseException {
		// Note: Float declare as int.
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				("<http://dbpedia.org/resource/Camillo_Benso,_conte_di_Cavour> "
						+ "<http://dbpedia.org/property/mandatofine> "
						+ "\"1380.0\"^^<http://www.w3.org/2001/XMLSchema#int> "
						+ "<http://it.wikipedia.org/wiki/Camillo_Benso,_conte_di_Cavour#absolute-line=20> .")
								.getBytes());
		parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
		parser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);
		parser.parse(bais, "http://base-uri");
	}

	@Test
	public void testStatementWithInvalidLiteralContentAndStrictValidation()
			throws RDFHandlerException, IOException, RDFParseException {
		// Note: Float declare as int.
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				("<http://dbpedia.org/resource/Camillo_Benso,_conte_di_Cavour> "
						+ "<http://dbpedia.org/property/mandatofine> "
						+ "\"1380.0\"^^<http://www.w3.org/2001/XMLSchema#int> "
						+ "<http://it.wikipedia.org/wiki/Camillo_Benso,_conte_di_Cavour#absolute-line=20> .")
								.getBytes());
		parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		parser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		try {
			parser.parse(bais, "http://test.base.uri");
			fail("Expected exception when passing in a datatype using an N3 style prefix");
		} catch (RDFParseException rdfpe) {
			// FIXME: Fix line numbers for validation errors during the line
			// assertEquals(1, rdfpe.getLineNumber());
			// FIXME: Enable column numbers when parser supports them
			// assertEquals(152, rdfpe.getColumnNumber());
		}
	}

	@Test
	public void testStatementWithInvalidDatatypeAndIgnoreValidation()
			throws RDFHandlerException, IOException, RDFParseException {
		verifyStatementWithInvalidDatatype(false);
	}

	@Test
	public void testStatementWithInvalidDatatypeAndVerifyValidation()
			throws RDFHandlerException, IOException, RDFParseException {
		try {
			verifyStatementWithInvalidDatatype(true);
			fail("Did not find expected exception");
		} catch (RDFParseException rdfpe) {
			// FIXME: Fix line numbers for validation errors during the line
			// assertEquals(1, rdfpe.getLineNumber());
		}
	}

	@Test
	public void testStopAtFirstErrorStrictParsing() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				("<http://s0> <http://p0> <http://o0> <http://g0> .\n"
						+ "<http://sX>                                     .\n" + // Line
						// with
						// error.
						"<http://s1> <http://p1> <http://o1> <http://g1> .\n").getBytes());

		parser.getParserConfig().set(NTriplesParserSettings.FAIL_ON_INVALID_LINES, false);

		try {
			parser.parse(bais, "http://test.base.uri");
			fail("Expected exception when encountering an invalid line");
		} catch (RDFParseException rdfpe) {
			assertEquals(2, rdfpe.getLineNumber());
			// assertEquals(50, rdfpe.getColumnNumber());
		}
	}

	@Test
	public void testStopAtFirstErrorTolerantParsing() throws RDFHandlerException, IOException, RDFParseException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(
				("<http://s0> <http://p0> <http://o0> <http://g0> .\n"
						+ "<http://sX>                                     .\n" + // Line
						// with
						// error.
						"<http://s1> <http://p1> <http://o1> <http://g1> .\n").getBytes());
		final TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(rdfHandler);

		parser.getParserConfig().set(NTriplesParserSettings.FAIL_ON_INVALID_LINES, false);
		parser.getParserConfig().addNonFatalError(NTriplesParserSettings.FAIL_ON_INVALID_LINES);

		parser.parse(bais, "http://base-uri");
		rdfHandler.assertHandler(2);
		final Collection<Statement> statements = rdfHandler.getStatements();
		int i = 0;
		for (Statement nextStatement : statements) {
			assertEquals("http://s" + i, nextStatement.getSubject().stringValue());
			assertEquals("http://p" + i, nextStatement.getPredicate().stringValue());
			assertEquals("http://o" + i, nextStatement.getObject().stringValue());
			assertEquals("http://g" + i, nextStatement.getContext().stringValue());
			i++;
		}
	}

	private void verifyStatementWithInvalidDatatype(boolean useDatatypeVerification)
			throws RDFHandlerException, IOException, RDFParseException {
		TestRDFHandler rdfHandler = new TestRDFHandler();
		parser.setRDFHandler(rdfHandler);
		parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, useDatatypeVerification);
		parser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, useDatatypeVerification);
		if (!useDatatypeVerification) {
			parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
			parser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);
		}

		final ByteArrayInputStream bais = new ByteArrayInputStream(
				("<http://dbpedia.org/resource/Camillo_Benso,_conte_di_Cavour> "
						+ "<http://dbpedia.org/property/mandatofine> "
						+ "\"1380.0\"^^<http://dbpedia.org/invalid/datatype/second> "
						+ "<http://it.wikipedia.org/wiki/Camillo_Benso,_conte_di_Cavour#absolute-line=20> .")
								.getBytes());
		parser.parse(bais, "http://base-uri");
		rdfHandler.assertHandler(1);
	}

	private class TestParseLocationListener extends SimpleParseLocationListener {

		private void assertListener(int row, int col) {
			assertEquals("Unexpected last row", row, this.getLineNo());
			assertEquals("Unexpected last col", col, this.getColumnNo());
		}

	}

	private class TestRDFHandler extends StatementCollector {

		private boolean started = false;

		private boolean ended = false;

		@Override
		public void startRDF() throws RDFHandlerException {
			super.startRDF();
			started = true;
		}

		@Override
		public void endRDF() throws RDFHandlerException {
			super.endRDF();
			ended = true;
		}

		public void assertHandler(int expected) {
			assertTrue(started, "Never started.");
			assertTrue(ended, "Never ended.");
			assertEquals("Unexpected number of statements.", expected, getStatements().size());
		}
	}

	@Test
	public void testSupportedSettings() throws Exception {
		assertThat(parser.getSupportedSettings()).hasSize(14);
	}

	protected abstract RDFParser createRDFParser();
}
