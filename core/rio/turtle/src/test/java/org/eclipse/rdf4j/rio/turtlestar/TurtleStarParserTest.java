/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtlestar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.SimpleParseLocationListener;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.helpers.TurtleParserSettings;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pavel Mihaylov
 */
public class TurtleStarParserTest {
	private TurtleStarParser parser;

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private final ParseErrorCollector errorCollector = new ParseErrorCollector();

	private final StatementCollector statementCollector = new StatementCollector();

	private final String baseURI = "http://example.org/";

	private final SimpleParseLocationListener locationListener = new SimpleParseLocationListener();

	@Before
	public void setUp() {
		parser = new TurtleStarParser();
		parser.setParseErrorListener(errorCollector);
		parser.setRDFHandler(statementCollector);
		parser.setParseLocationListener(locationListener);
	}

	@Test
	public void testParseRDFStarData() throws IOException {
		IRI bob = vf.createIRI("http://example.com/bob");
		IRI alice = vf.createIRI("http://example.com/alice");
		IRI book = vf.createIRI("http://example.com/book");
		IRI otherbook = vf.createIRI("http://example.com/otherbook");
		IRI bobshomepage = vf.createIRI("http://example.com/bobshomepage");
		IRI a = vf.createIRI("http://example.org/a");
		IRI b = vf.createIRI("http://example.com/b");
		IRI c = vf.createIRI("http://example.com/c");
		IRI valid = vf.createIRI("http://example.com/valid");
		Literal abcDate = vf.createLiteral("1999-08-16", XSD.DATE);
		Literal birthDate = vf.createLiteral("1908-03-18", XSD.DATE);
		Literal titleEn = vf.createLiteral("Example book", "en");
		Literal titleDe = vf.createLiteral("Beispielbuch", "de");
		Literal titleEnUs = vf.createLiteral("Example Book", "en-US");

		Triple bobCreatedBook = vf.createTriple(bob, DCTERMS.CREATED, book);
		Triple aliceKnowsBobCreatedBook = vf.createTriple(alice, FOAF.KNOWS, bobCreatedBook);
		Triple bobCreatedBookKnowsAlice = vf.createTriple(bobCreatedBook, FOAF.KNOWS, alice);
		Triple bookCreatorAlice = vf.createTriple(book, DCTERMS.CREATOR, alice);
		Triple aliceCreatedBook = vf.createTriple(alice, DCTERMS.CREATED, book);
		Triple abc = vf.createTriple(a, b, c);
		Triple bobBirthdayDate = vf.createTriple(bob, FOAF.BIRTHDAY, birthDate);
		Triple bookTitleEn = vf.createTriple(book, DCTERMS.TITLE, titleEn);
		Triple bookTitleDe = vf.createTriple(book, DCTERMS.TITLE, titleDe);
		Triple bookTitleEnUs = vf.createTriple(book, DCTERMS.TITLE, titleEnUs);

		try (InputStream in = this.getClass().getResourceAsStream("/test-rdfstar.ttls")) {
			parser.parse(in, baseURI);

			Collection<Statement> stmts = statementCollector.getStatements();

			assertEquals(10, stmts.size());

			assertTrue(stmts.contains(vf.createStatement(bob, FOAF.KNOWS, aliceKnowsBobCreatedBook)));
			assertTrue(stmts.contains(vf.createStatement(bobCreatedBookKnowsAlice, DCTERMS.SOURCE, otherbook)));
			assertTrue(stmts.contains(vf.createStatement(bobshomepage, DCTERMS.SOURCE, bookCreatorAlice)));
			assertTrue(stmts.contains(vf.createStatement(bookCreatorAlice, DCTERMS.SOURCE, bobshomepage)));
			assertTrue(stmts.contains(vf.createStatement(bookCreatorAlice, DCTERMS.REQUIRES, aliceCreatedBook)));
			assertTrue(stmts.contains(vf.createStatement(abc, valid, abcDate)));
			assertTrue(stmts.contains(vf.createStatement(bobBirthdayDate, DCTERMS.SOURCE, bobshomepage)));
			assertTrue(stmts.contains(vf.createStatement(bookTitleEn, DCTERMS.SOURCE, bobshomepage)));
			assertTrue(stmts.contains(vf.createStatement(bookTitleDe, DCTERMS.SOURCE, bobshomepage)));
			assertTrue(stmts.contains(vf.createStatement(bookTitleEnUs, DCTERMS.SOURCE, bobshomepage)));
		}
	}

	@Test
	public void testParseRDFStar_TurtleStarDisabled() throws IOException {
		parser.getParserConfig().set(TurtleParserSettings.ACCEPT_TURTLESTAR, false);

		try (InputStream in = this.getClass().getResourceAsStream("/test-rdfstar.ttls")) {
			parser.parse(in, baseURI);
		} catch (RDFParseException e) {
			fail("parser setting should have no influence on TurtleStarParser handling of RDF-star data");
		}
	}

	@Test
	public void testTripleInPredicate() throws IOException {
		String data = "@prefix ex: <http://example.com/>.\nex:Example << <urn:a> <urn:b> <urn:c> >> \"foo\" .";
		try (Reader r = new StringReader(data)) {
			parser.parse(r, baseURI);
			fail("Must fail with RDFParseException");
		} catch (RDFParseException e) {
			assertEquals("Illegal predicate value: <<urn:a urn:b urn:c>> [line 2]", e.getMessage());
		}
	}

	@Test
	public void testTripleInDatatype() throws IOException {
		String data = "@prefix ex: <http://example.com/>.\nex:Example ex:p \"foo\"^^<< <urn:a> <urn:b> <urn:c> >> .";
		try (Reader r = new StringReader(data)) {
			parser.parse(r, baseURI);
			fail("Must fail with RDFParseException");
		} catch (RDFParseException e) {
			assertEquals("Illegal datatype value: <<urn:a urn:b urn:c>> [line 2]", e.getMessage());
		}
	}

	@Test
	public void testTripleAnnotation() throws IOException {
		IRI example = iri("http://example.com/Example");
		IRI a = iri("urn:a"), b = iri("urn:b"), c = iri("urn:c");
		Literal foo = literal("foo");
		String data = "@prefix ex: <http://example.com/>.\nex:Example  <urn:a> <urn:b> {| <urn:c> \"foo\" |}.";
		Statement st = statement(example, a, b, null);
		try (Reader r = new StringReader(data)) {
			parser.parse(r, baseURI);
			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).contains(st);
			assertThat(stmts).contains(statement(Values.triple(st), c, foo, null));
		}
	}

	@Test
	public void testTripleMultipleAnnotationSameObject() throws IOException {
		IRI example = iri("http://example.com/Example");
		IRI a = iri("urn:a"), b = iri("urn:b"), c = iri("urn:c"), d = iri("urn:d");
		Literal foo = literal("foo"), bar = literal("bar");
		String data = "@prefix ex: <http://example.com/>.\n"
				+ "ex:Example  <urn:a> <urn:b> {| <urn:c> \"foo\", \"bar\"; <urn:d> \"foo\" |}."
				+ "ex:Example <urn:d> <urn:a> .";

		Statement annotatedSt = statement(example, a, b, null);
		try (Reader r = new StringReader(data)) {
			parser.parse(r, baseURI);
			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).contains(annotatedSt);
			assertThat(stmts).contains(statement(Values.triple(annotatedSt), c, foo, null));
			assertThat(stmts).contains(statement(Values.triple(annotatedSt), c, bar, null));
			assertThat(stmts).contains(statement(Values.triple(annotatedSt), d, foo, null));
			assertThat(stmts).contains(statement(example, d, a, null));
		}
	}

	@Test
	public void testTripleMultipleAnnotationMultipleObjects() throws IOException {
		IRI example = iri("http://example.com/Example");
		IRI a = iri("urn:a"), b = iri("urn:b"), c = iri("urn:c"), d = iri("urn:d");
		Literal foo = literal("foo"), bar = literal("bar");
		String data = "@prefix ex: <http://example.com/>.\n"
				+ "ex:Example  <urn:a> <urn:b> {| <urn:c> \"foo\" |}, <urn:d> {| <urn:c> \"bar\" |}."
				+ "ex:Example <urn:d> <urn:a> .";

		Statement annotatedSt1 = statement(example, a, b, null), annotatedSt2 = statement(example, a, d, null);
		try (Reader r = new StringReader(data)) {
			parser.parse(r, baseURI);
			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).contains(annotatedSt1);
			assertThat(stmts).contains(statement(Values.triple(annotatedSt1), c, foo, null));
			assertThat(stmts).contains(statement(Values.triple(annotatedSt2), c, bar, null));
			assertThat(stmts).contains(statement(example, d, a, null));
			assertThat(stmts).doesNotContain(statement(Values.triple(annotatedSt1), c, bar, null));

		}
	}

	@Test
	public void testMalformedTripleAnnotation() throws IOException {
		String data = "@prefix ex: <http://example.com/>.\nex:Example  <urn:a> <urn:b> {| <urn:c> \"foo\"  }.";
		try (Reader r = new StringReader(data)) {
			assertThatThrownBy(() -> parser.parse(r, baseURI))
					.isInstanceOf(RDFParseException.class)
					.hasMessageContaining("Expected '|', found '}'");
		}
	}

}
