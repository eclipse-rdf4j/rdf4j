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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.AbstractParserTest;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for N-Triples Parser.
 *
 * @author Peter Ansell
 */
public abstract class AbstractNTriplesParserUnitTest extends AbstractParserTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private static final String NTRIPLES_TEST_URL = "http://www.w3.org/2000/10/rdf-tests/rdfcore/ntriples/test.nt";

	private static final String NTRIPLES_TEST_FILE = "/testcases/ntriples/test.nt";

	private Model model;

	@BeforeEach
	@Override
	public void setUp() {
		model = new LinkedHashModel();
		statementCollector = new StatementCollector(model);
		super.setUp();
	}

	@Test
	public void testNTriplesFile() throws Exception {
		try (InputStream in = this.getClass().getResourceAsStream(NTRIPLES_TEST_FILE)) {
			parser.parse(in, NTRIPLES_TEST_URL);
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

		try {
			parser.parse(new StringReader(data), NTRIPLES_TEST_URL);
			fail("expected RDFParseException due to invalid data");
		} catch (RDFParseException expected) {
			assertEquals(1, expected.getLineNumber());
		}
	}

	@Test
	public void testExceptionHandlingWithStopAtFirstError() throws Exception {
		String data = "invalid nt";

		parser.getParserConfig().set(NTriplesParserSettings.FAIL_ON_INVALID_LINES, Boolean.TRUE);

		try {
			parser.parse(new StringReader(data), NTRIPLES_TEST_URL);
			fail("expected RDFParseException due to invalid data");
		} catch (RDFParseException expected) {
			assertEquals(1, expected.getLineNumber());
		}
	}

	@Test
	public void testExceptionHandlingWithoutStopAtFirstError() throws Exception {
		String data = "invalid nt";

		parser.getParserConfig().addNonFatalError(NTriplesParserSettings.FAIL_ON_INVALID_LINES);

		parser.parse(new StringReader(data), NTRIPLES_TEST_URL);

		assertEquals(0, model.size());
		assertEquals(0, model.subjects().size());
		assertEquals(0, model.predicates().size());
		assertEquals(0, model.objects().size());
	}

	@Test
	public void testExceptionHandlingWithoutStopAtFirstError2() throws Exception {
		String data = "invalid nt";

		parser.getParserConfig().set(NTriplesParserSettings.FAIL_ON_INVALID_LINES, false);

		parser.parse(new StringReader(data), NTRIPLES_TEST_URL);

		assertEquals(0, model.size());
		assertEquals(0, model.subjects().size());
		assertEquals(0, model.predicates().size());
		assertEquals(0, model.objects().size());
	}

	@Test
	public void testEscapes() throws Exception {
		parser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> \" \\t \\b \\n \\r \\f \\\" \\' \\\\ \" . "),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(" \t \b \n \r \f \" \' \\ ", Models.objectLiteral(model).get().getLabel());
	}

	@Test
	public void testEndOfLineCommentNoSpace() throws Exception {
		parser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> .#endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineCommentWithSpaceBefore() throws Exception {
		parser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> . #endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineCommentWithSpaceAfter() throws Exception {
		parser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> .# endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineCommentWithSpaceBoth() throws Exception {
		parser.parse(
				new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineCommentsNoSpace() throws Exception {
		parser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> .#endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineCommentsWithSpaceBefore() throws Exception {
		parser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> . #endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineCommentsWithSpaceAfter() throws Exception {
		parser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> .# endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineCommentsWithSpaceBoth() throws Exception {
		parser.parse(new StringReader(
				"<urn:test:subject> <urn:test:predicate> <urn:test:object> . # endoflinecomment\n<urn:test:subject> <urn:test:predicate> <urn:test:secondobject> . # endoflinecomment\n"),
				"http://example/");
		assertEquals(2, model.size());
	}

	@Test
	public void testEndOfLineEmptyCommentNoSpace() throws Exception {
		parser.parse(new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> .#\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineEmptyCommentWithSpaceBefore() throws Exception {
		parser.parse(new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> . #\n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineEmptyCommentWithSpaceAfter() throws Exception {
		parser.parse(new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> .# \n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testEndOfLineEmptyCommentWithSpaceBoth() throws Exception {
		parser.parse(new StringReader("<urn:test:subject> <urn:test:predicate> <urn:test:object> . # \n"),
				"http://example/");
		assertEquals(1, model.size());
		assertEquals(Collections.singleton("urn:test:object"), Models.objectStrings(model));
	}

	@Test
	public void testBlankNodeIdentifiersRDF11() throws Exception {
		parser.parse(new StringReader("_:123 <urn:test:predicate> _:456 ."), "http://example/");
		assertEquals(1, model.size());
	}

	@Test
	public void testSupportedSettings() {
		assertEquals(14, createRDFParser().getSupportedSettings().size());
	}

	@Test
	public void testUriWithSpaceShouldFailToParse() throws Exception {
		String nt = "<http://example/ space> <http://example/p> <http://example/o> .";

		try {
			parser.parse(new StringReader(nt), NTRIPLES_TEST_URL);
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
		String nt = "<http://example/\\n> <http://example/p> <http://example/o> .";

		try {
			parser.parse(new StringReader(nt), NTRIPLES_TEST_URL);
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
		parser.parse(new StringReader("_:123_ <urn:test:predicate> _:_456 ."), NTRIPLES_TEST_URL);

		assertEquals(1, model.size());
		assertEquals(1, model.subjects().size());
		assertEquals(1, model.predicates().size());
		assertEquals(1, model.objects().size());
	}

	@Test
	public void testBlankNodeIdentifiersWithDot() throws Exception {
		// The character . may appear anywhere except the first or last character.
		parser.parse(new StringReader("_:1.23 <urn:test:predicate> _:45.6 ."), NTRIPLES_TEST_URL);

		assertEquals(1, model.size());
		assertEquals(1, model.subjects().size());
		assertEquals(1, model.predicates().size());
		assertEquals(1, model.objects().size());
	}

	@Test
	public void testBlankNodeIdentifiersWithDotAsFirstCahracter() {
		// The character . may appear anywhere except the first or last character.
		try {
			parser.parse(new StringReader("_:123 <urn:test:predicate> _:.456 ."), NTRIPLES_TEST_URL);
			fail("Should have failed to parse invalid N-Triples bnode with '.' at the begining of the bnode label");
		} catch (Exception e) {
		}

		assertEquals(0, model.size());
		assertEquals(0, model.subjects().size());
		assertEquals(0, model.predicates().size());
		assertEquals(0, model.objects().size());
	}

	@Test
	public void testBlankNodeIdentifiersWithDotAsLastCahracter() {
		// The character . may appear anywhere except the first or last character.
		assertThrows(RDFParseException.class,
				() -> parser.parse(new StringReader("_:123 <urn:test:predicate> _:456. ."), NTRIPLES_TEST_URL));
		assertEquals(0, model.size());
		assertEquals(0, model.subjects().size());
		assertEquals(0, model.predicates().size());
		assertEquals(0, model.objects().size());
	}

	@Test
	public void testBlankNodeIdentifiersWithOtherCharacters() {
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
			Model model = new LinkedHashModel();
			parser.setRDFHandler(new StatementCollector(model));

			String triple = "<urn:test:subject> <urn:test:predicate> _:1" + character + " . ";
			try {
				parser.parse(new StringReader(triple), NTRIPLES_TEST_URL);
			} catch (Exception e) {
				fail(" Failed to parse triple : " + triple + " containing character '" + character + "' at index " + i
						+ " in charactersList");
			}

			assertEquals(1, model.size(), "Should parse '" + character + "'");
			assertEquals(1, model.subjects().size(),
					"Should have subject when triple has character : '" + character + "'");
			assertEquals(1, model.predicates().size(),
					"Should have predicate when triple has character : '" + character + "'");
			assertEquals(1, model.objects().size(),
					"Should have object when triple has character : '" + character + "'");
		}

	}

	@Test
	public void testBlankNodeIdentifiersWithOtherCharactersAsFirstCharacter() {
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
			Model model = new LinkedHashModel();
			parser.setRDFHandler(new StatementCollector(model));

			assertThrows(RDFParseException.class, () -> {
				parser.parse(
						new StringReader("<urn:test:subject> <urn:test:predicate> _:" + character + "1 . "),
						NTRIPLES_TEST_URL);
			});
			assertEquals(0, model.size());
			assertEquals(0, model.subjects().size());
			assertEquals(0, model.predicates().size());
			assertEquals(0, model.objects().size());
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
		Model model = new LinkedHashModel();
		String commentStr = "some comment in it's own line";
		CommentCollector cc = new CommentCollector(model);
		parser.setRDFHandler(cc);
		parser.parse(
				new StringReader("<s:1> <p:1> <o:1> .\n#" + commentStr + "\n<s:2> <p:2> <o:2> ."),
				"http://example/");
		assertEquals(2, model.size());
		assertEquals(new TreeSet<>(Arrays.asList("o:1", "o:2")), Models.objectStrings(model));
		assertEquals(List.of(commentStr), cc.comments);
	}

	@Test
	public void testLinenumberDatatypeValidation() throws Exception {
		parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);

		parser.parse(
				new StringReader("<urn:test:o> <urn:test:p> \"invalid\"^^<" + XSD.DATETIME.stringValue() + "> ."),
				NTRIPLES_TEST_URL);
		List<String> errors = errorCollector.getErrors();

		assertEquals(1, errors.size());
		assertTrue(errors.get(0).contains("(1, 32)"), "Unknown line number");
	}

	@Test
	public void testLinenumberLanguagetagValidation() throws Exception {
		parser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES);
		parser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		parser.getParserConfig().set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);

		parser.parse(
				new StringReader("<urn:test:o> <urn:test:p> \"hello\"@inv+alid ."),
				NTRIPLES_TEST_URL);
		List<String> errors = errorCollector.getErrors();

		assertEquals(1, errors.size());
		assertTrue(errors.get(0).contains("(1, 32)"), "Unknown line number");
	}

	@Test
	public void testDirLangStringLTR() {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--ltr .";
		dirLangStringTestHelper(data, "en--ltr", false, false);
	}

	@Test
	public void testDirLangStringRTL() {
		String data = "<http://example/a> <http://example/b> \"שלום\"@he--rtl .";
		dirLangStringTestHelper(data, "he--rtl", false, false);
	}

	@Test
	public void testDirLangStringLTRWithNormalization() {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--ltr .";
		dirLangStringTestHelper(data, "en--ltr", true, false);
	}

	@Test
	public void testDirLangStringRTLWithNormalization() {
		String data = "<http://example/a> <http://example/b> \"שלום\"@HE--rtl .";
		dirLangStringTestHelper(data, "he--rtl", true, false);
	}

	@Test
	public void testBadDirLangString() {
		String data = "<http://example/a> <http://example/b> \"hello\"@en--unk .";
		dirLangStringTestHelper(data, "", true, true);
	}

	@Test
	public void testBadCapitalizationDirLangString() {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--LTR .";
		dirLangStringTestHelper(data, "", true, true);
	}

	@Test
	public void testDirLangStringNoLanguage() throws IOException {
		String data = "<http://example/a> <http://example/b> \"Hello\"^^<http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString> .";
		dirLangStringNoLanguageTestHelper(data);
	}

	@Test
	public void testTripleTerm() throws IOException {
		String data = "<http://example/a> <http://example/b> <<( <http://example/a> <http://example/b> <http://example/c> )>> .";
		parser.parse(new StringReader(data));

		assertEquals(1, model.size());
		assertEquals(vf.createTriple(vf.createIRI("http://example/a"), vf.createIRI("http://example/b"), vf.createIRI("http://example/c")), Models.object(model).get());
	}

	@Test
	public void testTripleTermNoWhitespace() throws IOException {
		String data = "<http://example/a><http://example/b><<(<http://example/a><http://example/b><http://example/c>)>>.";
		parser.parse(new StringReader(data));

		assertEquals(1, model.size());
		assertEquals(vf.createTriple(vf.createIRI("http://example/a"), vf.createIRI("http://example/b"), vf.createIRI("http://example/c")), Models.object(model).get());
	}

	@Test
	public void testBadTripleTermSubject() throws IOException {
		String data = "<<( <http://example/a> <http://example/b> <http://example/c> )>> <http://example/a> <http://example/b> .";
		assertThrows(RDFParseException.class, () -> parser.parse(new StringReader(data)));
	}

	@Test
	public void testBadTripleTermMissingObject() throws IOException {
		String data = "<http://example/a> <http://example/b> <<( <http://example/a> <http://example/b> )>> .";
		assertThrows(RDFParseException.class, () -> parser.parse(new StringReader(data)));
	}

	protected abstract RDFParser createRDFParser();
}
