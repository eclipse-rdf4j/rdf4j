/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ntriples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.StringReader;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.NTriplesParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests for N-Triples Parser.
 * 
 * @author Peter Ansell
 */
public abstract class AbstractNTriplesParserUnitTest {

	private static String NTRIPLES_TEST_URL = "http://www.w3.org/2000/10/rdf-tests/rdfcore/ntriples/test.nt";

	private static String NTRIPLES_TEST_FILE = "/testcases/ntriples/test.nt";

	@Test
	public void testNTriplesFile()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		ntriplesParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		InputStream in = this.getClass().getResourceAsStream(NTRIPLES_TEST_FILE);
		try {
			ntriplesParser.parse(in, NTRIPLES_TEST_URL);
		}
		catch (RDFParseException e) {
			fail("Failed to parse N-Triples test document: " + e.getMessage());
		}
		finally {
			in.close();
		}

		assertEquals(30, model.size());
		assertEquals(28, model.subjects().size());
		assertEquals(1, model.predicates().size());
		assertEquals(23, model.objects().size());
	}

	@Test
	public void testExceptionHandlingWithDefaultSettings()
		throws Exception
	{
		String data = "invalid nt";

		RDFParser ntriplesParser = createRDFParser();
		ntriplesParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		try {
			ntriplesParser.parse(new StringReader(data), NTRIPLES_TEST_URL);
			fail("expected RDFParseException due to invalid data");
		}
		catch (RDFParseException expected) {
			assertEquals(expected.getLineNumber(), 1);
		}
	}

	@Test
	public void testExceptionHandlingWithStopAtFirstError()
		throws Exception
	{
		String data = "invalid nt";

		RDFParser ntriplesParser = createRDFParser();
		ntriplesParser.getParserConfig().set(NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES,
				Boolean.FALSE);

		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		try {
			ntriplesParser.parse(new StringReader(data), NTRIPLES_TEST_URL);
			fail("expected RDFParseException due to invalid data");
		}
		catch (RDFParseException expected) {
			assertEquals(expected.getLineNumber(), 1);
		}
	}

	@Test
	public void testExceptionHandlingWithoutStopAtFirstError()
		throws Exception
	{
		String data = "invalid nt";

		RDFParser ntriplesParser = createRDFParser();
		ntriplesParser.getParserConfig().addNonFatalError(NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
		ntriplesParser.getParserConfig().set(NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES,
				Boolean.TRUE);

		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		ntriplesParser.parse(new StringReader(data), NTRIPLES_TEST_URL);

		assertEquals(0, model.size());
		assertEquals(0, model.subjects().size());
		assertEquals(0, model.predicates().size());
		assertEquals(0, model.objects().size());
	}

	@Test
	public void testExceptionHandlingParsingNTriplesIntoMemoryStore()
		throws Exception
	{
		Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		RepositoryConnection conn = repo.getConnection();
		try {
			// Force the connection to use stop at first error
			conn.getParserConfig().set(NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES, Boolean.FALSE);

			String data = "invalid nt";
			conn.add(new StringReader(data), "http://example/", RDFFormat.NTRIPLES);
			fail("expected RDFParseException due to invalid data");
		}
		catch (RDFParseException expected) {
			;
		}
		finally {
			conn.close();
			repo.shutDown();
		}
	}

	@Test
	public void testExceptionHandlingParsingNTriplesIntoMemoryStoreWithoutStopAtFirstError()
		throws Exception
	{
		Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		RepositoryConnection conn = repo.getConnection();
		try {
			// Force the connection to not use stop at first error
			conn.getParserConfig().addNonFatalError(NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
			conn.getParserConfig().set(NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES, Boolean.TRUE);

			String data = "invalid nt";
			conn.add(new StringReader(data), "http://example/", RDFFormat.NTRIPLES);

			// verify that no triples were added after the successful parse
			assertEquals(0, conn.size());
		}
		finally {
			conn.close();
			repo.shutDown();
		}
	}

	@Test
	public void testEscapes()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> \" \\t \\b \\n \\r \\f \\\" \\' \\\\ \" . "),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(" \t \b \n \r \f \" \' \\ ", model.objectLiteral().get().getLabel());
	}

	@Test
	public void testEndOfLineCommentNoSpace()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> .#endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals("urn:test:object", model.objectString().get());
	}

	@Test
	public void testEndOfLineCommentWithSpaceBefore()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> . #endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals("urn:test:object", model.objectString().get());
	}

	@Test
	public void testEndOfLineCommentWithSpaceAfter()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> .# endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals("urn:test:object", model.objectString().get());
	}

	@Test
	public void testEndOfLineCommentWithSpaceBoth()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals("urn:test:object", model.objectString().get());
	}

	@Test
	public void testEndOfLineCommentsNoSpace()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader(
						"<urn:test:subject> <urn:test:predicate> <urn:test:object> .#endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineCommentsWithSpaceBefore()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader(
						"<urn:test:subject> <urn:test:predicate> <urn:test:object> . #endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineCommentsWithSpaceAfter()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader(
						"<urn:test:subject> <urn:test:predicate> <urn:test:object> .# endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineCommentsWithSpaceBoth()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader(
						"<urn:test:subject> <urn:test:predicate> <urn:test:object> . # endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineEmptyCommentNoSpace()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> .#\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals("urn:test:object", model.objectString().get());
	}

	@Test
	public void testEndOfLineEmptyCommentWithSpaceBefore()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> . #\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals("urn:test:object", model.objectString().get());
	}

	@Test
	public void testEndOfLineEmptyCommentWithSpaceAfter()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> .# \n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals("urn:test:object", model.objectString().get());
	}

	@Test
	public void testEndOfLineEmptyCommentWithSpaceBoth()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> . # \n"), "http://example/");
		assertEquals(1, model.size());
		assertEquals("urn:test:object", model.objectString().get());
	}

	@Test
	public void testBlankNodeIdentifiersRDF11()
		throws Exception
	{
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader("_:123 <urn:test:predicate> _:456 ."), "http://example/");
		assertEquals(1, model.size());
	}

	@Test
	public void testSupportedSettings()
		throws Exception
	{
		assertEquals(12, createRDFParser().getSupportedSettings().size());
	}

    @Test
    public void testUriWithSpaceShouldFailToParse()
            throws Exception
    {
        RDFParser ntriplesParser = createRDFParser();
        Model model = new LinkedHashModel();
        ntriplesParser.setRDFHandler(new StatementCollector(model));

        String nt = "<http://example/ space> <http://example/p> <http://example/o> .";

        try {
            ntriplesParser.parse(new StringReader(nt), NTRIPLES_TEST_URL);
            fail("Should have failed to parse invalid N-Triples uri with space");
        }
        catch (RDFParseException ignored) {
        }

        assertEquals(0, model.size());
        assertEquals(0, model.subjects().size());
        assertEquals(0, model.predicates().size());
        assertEquals(0, model.objects().size());
    }

    @Test
    public void testUriWithEscapeCharactersShouldFailToParse()
            throws Exception
    {
        RDFParser ntriplesParser = createRDFParser();
        Model model = new LinkedHashModel();
        ntriplesParser.setRDFHandler(new StatementCollector(model));

        String nt = "<http://example/\\n> <http://example/p> <http://example/o> .";

        try {
            ntriplesParser.parse(new StringReader(nt), NTRIPLES_TEST_URL);
            fail("Should have failed to parse invalid N-Triples uri with space");
        }
        catch (RDFParseException ignored) {
        }

        assertEquals(0, model.size());
        assertEquals(0, model.subjects().size());
        assertEquals(0, model.predicates().size());
        assertEquals(0, model.objects().size());
    }

	protected abstract RDFParser createRDFParser();
}

