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
package org.eclipse.rdf4j.rio.ntriples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Test;

/**
 * Unit tests for N-Triples Parser.
 *
 * @author Peter Ansell
 */
public abstract class AbstractNTriplesParserUnitTest {

	private static final String NTRIPLES_TEST_URL = "http://www.w3.org/2000/10/rdf-tests/rdfcore/ntriples/test.nt";

	private static final String NTRIPLES_TEST_FILE = "/testcases/ntriples/test.nt";

	@Test
	public void testNTriplesFile() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		try (InputStream in = this.getClass().getResourceAsStream(NTRIPLES_TEST_FILE)) {
			ntriplesParser.parse(in, NTRIPLES_TEST_URL);
		} catch (RDFParseException e) {
			fail("Failed to parse N-Triples test document: " + e.getMessage());
		}

		assertEquals(30, model.size());
		assertEquals(28, model.subjects().size());
		assertEquals(1, model.predicates().size());
		assertEquals(23, model.objects().size());
	}

	@Test
	public void testExceptionHandlingWithDefaultSettings() throws Exception {
		String data = "invalid nt";

		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		try {
			ntriplesParser.parse(new StringReader(data), NTRIPLES_TEST_URL);
			fail("expected RDFParseException due to invalid data");
		} catch (RDFParseException expected) {
			assertEquals(expected.getLineNumber(), 1);
		}
	}

	@Test
	public void testExceptionHandlingWithStopAtFirstError() throws Exception {
		String data = "invalid nt";

		RDFParser ntriplesParser = createRDFParser();
		ntriplesParser.getParserConfig().set(NTriplesParserSettings.FAIL_ON_INVALID_LINES, Boolean.TRUE);

		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		try {
			ntriplesParser.parse(new StringReader(data), NTRIPLES_TEST_URL);
			fail("expected RDFParseException due to invalid data");
		} catch (RDFParseException expected) {
			assertEquals(expected.getLineNumber(), 1);
		}
	}

	@Test
	public void testExceptionHandlingWithoutStopAtFirstError() throws Exception {
		String data = "invalid nt";

		RDFParser ntriplesParser = createRDFParser();
		ntriplesParser.getParserConfig().addNonFatalError(NTriplesParserSettings.FAIL_ON_INVALID_LINES);

		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		ntriplesParser.parse(new StringReader(data), NTRIPLES_TEST_URL);

		assertEquals(0, model.size());
		assertEquals(0, model.subjects().size());
		assertEquals(0, model.predicates().size());
		assertEquals(0, model.objects().size());
	}

	@Test
	public void testExceptionHandlingWithoutStopAtFirstError2() throws Exception {
		String data = "invalid nt";

		RDFParser ntriplesParser = createRDFParser();
		ntriplesParser.getParserConfig().set(NTriplesParserSettings.FAIL_ON_INVALID_LINES, false);

		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		ntriplesParser.parse(new StringReader(data), NTRIPLES_TEST_URL);

		assertEquals(0, model.size());
		assertEquals(0, model.subjects().size());
		assertEquals(0, model.predicates().size());
		assertEquals(0, model.objects().size());
	}

	@Test
	public void testEscapes() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> \" \\t \\b \\n \\r \\f \\\" \\' \\\\ \" . "),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(" \t \b \n \r \f \" \' \\ ", Models.objectLiteral(model).get().getLabel());
	}

	@Test
	public void testEndOfLineCommentNoSpace() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> .#endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineCommentWithSpaceBefore() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> . #endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineCommentWithSpaceAfter() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> .# endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineCommentWithSpaceBoth() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineCommentsNoSpace() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> .#endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineCommentsWithSpaceBefore() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> . #endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineCommentsWithSpaceAfter() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> .# endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineCommentsWithSpaceBoth() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> . # endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineEmptyCommentNoSpace() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> .#\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineEmptyCommentWithSpaceBefore() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> . #\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineEmptyCommentWithSpaceAfter() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> .# \n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineEmptyCommentWithSpaceBoth() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> . # \n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testBlankNodeIdentifiersRDF11() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader("_:123 <urn:test:predicate> _:456 ."), "http://example/");
		assertEquals(1, model.size());
	}

	@Test
	public void testSupportedSettings() throws Exception {
		assertEquals(14, createRDFParser().getSupportedSettings().size());
	}

	@Test
	public void testUriWithSpaceShouldFailToParse() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		String nt = "<http://example/ space> <http://example/p> <http://example/o> .";

		try {
			ntriplesParser.parse(new StringReader(nt), NTRIPLES_TEST_URL);
			fail("Should have failed to parse invalid N-Triples uri with space");
		} catch (RDFParseException ignored) {
		}

		assertEquals(0, model.size());
		assertEquals(0, model.subjects().size());
		assertEquals(0, model.predicates().size());
		assertEquals(0, model.objects().size());
	}

	@Test
	public void testUriWithEscapeCharactersShouldFailToParse() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));

		String nt = "<http://example/\\n> <http://example/p> <http://example/o> .";

		try {
			ntriplesParser.parse(new StringReader(nt), NTRIPLES_TEST_URL);
			fail("Should have failed to parse invalid N-Triples uri with space");
		} catch (RDFParseException ignored) {
		}

		assertEquals(0, model.size());
		assertEquals(0, model.subjects().size());
		assertEquals(0, model.predicates().size());
		assertEquals(0, model.objects().size());
	}

	@Test
	public void testBlankNodeIdentifiersWithUnderScore() throws Exception {
		// The characters _ and [0-9] may appear anywhere in a blank node label.
		RDFParser ntriplesParser = new NTriplesParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader("_:123_ <urn:test:predicate> _:_456 ."), NTRIPLES_TEST_URL);

		assertEquals(1, model.size());
		assertEquals(1, model.subjects().size());
		assertEquals(1, model.predicates().size());
		assertEquals(1, model.objects().size());
	}

	@Test
	public void testBlankNodeIdentifiersWithDot() throws Exception {
		// The character . may appear anywhere except the first or last character.
		RDFParser ntriplesParser = new NTriplesParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		ntriplesParser.parse(new StringReader("_:1.23 <urn:test:predicate> _:45.6 ."), NTRIPLES_TEST_URL);

		assertEquals(1, model.size());
		assertEquals(1, model.subjects().size());
		assertEquals(1, model.predicates().size());
		assertEquals(1, model.objects().size());
	}

	@Test
	public void testBlankNodeIdentifiersWithDotAsFirstCahracter() throws Exception {
		// The character . may appear anywhere except the first or last character.
		RDFParser ntriplesParser = new NTriplesParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		try {
			ntriplesParser.parse(new StringReader("_:123 <urn:test:predicate> _:.456 ."), NTRIPLES_TEST_URL);
			fail("Should have failed to parse invalid N-Triples bnode with '.' at the begining of the bnode label");
		} catch (Exception e) {
		}

		assertEquals(0, model.size());
		assertEquals(0, model.subjects().size());
		assertEquals(0, model.predicates().size());
		assertEquals(0, model.objects().size());
	}

	@Test(expected = RDFParseException.class)
	public void testBlankNodeIdentifiersWithDotAsLastCahracter() throws Exception {
		// The character . may appear anywhere except the first or last character.
		RDFParser ntriplesParser = new NTriplesParser();
		Model model = new LinkedHashModel();
		ntriplesParser.setRDFHandler(new StatementCollector(model));
		try {
			ntriplesParser.parse(new StringReader("_:123 <urn:test:predicate> _:456. ."), NTRIPLES_TEST_URL);
		} catch (RDFParseException e) {
			assertEquals(0, model.size());
			assertEquals(0, model.subjects().size());
			assertEquals(0, model.predicates().size());
			assertEquals(0, model.objects().size());
			throw e;
		}
		fail("Should have failed to parse invalid N-Triples bnode with '.' at the end of the bnode label");
	}

	@Test
	public void testBlankNodeIdentifiersWithOtherCharacters() throws Exception {
		// The characters -, U+00B7, U+0300 to U+036F and U+203F to U+2040 are permitted anywhere except the first
		// character.
		List<Character> charactersList = new ArrayList<>();
		charactersList.add('-');
		charactersList.add('\u00B7');
		charactersList.add('\u0300');
		charactersList.add('\u036F');
		charactersList.add('\u0301');
		charactersList.add('\u203F');

		for (int i = 0; i < charactersList.size(); i++) {
			Character character = charactersList.get(i);
			RDFParser ntriplesParser = new NTriplesParser();
			Model model = new LinkedHashModel();
			ntriplesParser.setRDFHandler(new StatementCollector(model));

			String triple = "<urn:test:subject> <urn:test:predicate> _:1" + character + " . ";
			try {
				ntriplesParser.parse(new StringReader(triple), NTRIPLES_TEST_URL);
			} catch (Exception e) {
				fail(" Failed to parse triple : " + triple + " containing character '" + character + "' at index " + i
						+ " in charactersList");
			}

			assertEquals("Should parse '" + character + "'", 1, model.size());
			assertEquals("Should have subject when triple has character : '" + character + "'", 1,
					model.subjects().size());
			assertEquals("Should have predicate when triple has character : '" + character + "'", 1,
					model.predicates().size());
			assertEquals("Should have object when triple has character : '" + character + "'", 1,
					model.objects().size());
		}

	}

	@Test(expected = RDFParseException.class)
	public void testBlankNodeIdentifiersWithOtherCharactersAsFirstCharacter() throws Exception {
		// The characters -, U+00B7, U+0300 to U+036F and U+203F to U+2040 are permitted anywhere except the first
		// character.
		List<Character> charactersList = new ArrayList<>();
		charactersList.add('-');
		charactersList.add('\u00B7');
		charactersList.add('\u0300');
		charactersList.add('\u036F');
		charactersList.add('\u0301');
		charactersList.add('\u203F');

		for (Character character : charactersList) {
			RDFParser ntriplesParser = new NTriplesParser();
			Model model = new LinkedHashModel();
			ntriplesParser.setRDFHandler(new StatementCollector(model));

			try {
				ntriplesParser.parse(
						new StringReader("<urn:test:subject> <urn:test:predicate> _:" + character + "1 . "),
						NTRIPLES_TEST_URL);
			} catch (RDFParseException e) {
				assertEquals(0, model.size());
				assertEquals(0, model.subjects().size());
				assertEquals(0, model.predicates().size());
				assertEquals(0, model.objects().size());
				throw e;
			}
			fail("Should have failed to parse invalid N-Triples bnode with '" + character
					+ "' at the begining of the bnode label");
		}
	}

	private static class CommentCollector extends StatementCollector {
		final List<String> comments = new LinkedList<>();

		public CommentCollector(Model model) {
			super(model);
		}

		@Override
		public void handleComment(String comment) throws RDFHandlerException {
			comments.add(comment);
		}
	}

	@Test
	public void testHandleComment() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		Model model = new LinkedHashModel();
		String commentStr = "some comment in it's own line";
		CommentCollector cc = new CommentCollector(model);
		ntriplesParser.setRDFHandler(cc);
		ntriplesParser.parse(
				new StringReader("<s:1> <p:1> <o:1> .\n#" + commentStr + "\n<s:2> <p:2> <o:2> ."),
				"http://example/");
		assertEquals(2, model.size());
		assertEquals(new TreeSet<>(Arrays.asList("o:1", "o:2")), Models.objectStrings(model));
		assertEquals(List.of(commentStr), cc.comments);
	}

	@Test
	public void testLinenumberDatatypeValidation() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		ntriplesParser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		ntriplesParser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		ParseErrorCollector collector = new ParseErrorCollector();
		ntriplesParser.setParseErrorListener(collector);

		ntriplesParser.parse(
				new StringReader("<urn:test:o> <urn:test:p> \"invalid\"^^<" + XSD.DATETIME.stringValue() + "> ."),
				NTRIPLES_TEST_URL);
		List<String> errors = collector.getErrors();

		assertEquals(1, errors.size());
		assertTrue("Unknown line number", errors.get(0).contains("(1, 32)"));
	}

	@Test
	public void testLinenumberLanguagetagValidation() throws Exception {
		RDFParser ntriplesParser = createRDFParser();
		ntriplesParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES);
		ntriplesParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		ntriplesParser.getParserConfig().set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);
		ParseErrorCollector collector = new ParseErrorCollector();
		ntriplesParser.setParseErrorListener(collector);

		ntriplesParser.parse(
				new StringReader("<urn:test:o> <urn:test:p> \"hello\"@inv+alid ."),
				NTRIPLES_TEST_URL);
		List<String> errors = collector.getErrors();

		assertEquals(1, errors.size());
		assertTrue("Unknown line number", errors.get(0).contains("(1, 32)"));
	}

	protected abstract RDFParser createRDFParser();
}
