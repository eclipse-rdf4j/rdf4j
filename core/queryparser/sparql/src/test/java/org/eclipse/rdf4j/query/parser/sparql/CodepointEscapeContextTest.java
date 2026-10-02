/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.junit.jupiter.api.Test;

/**
 * Codepoint escapes (backslash-u with 4 hex digits, backslash-U with 8 hex digits) must be recognised in IRIREFs and in
 * all four string literal forms regardless of the surrounding lexical context (escaped quotes, quotes inside long
 * strings, comments, compact comparisons), and each escape must be processed exactly once.
 */
class CodepointEscapeContextTest {

	private static final String BASE = "http://example.org/";
	private static final String SLASH = Character.toString((char) 92);
	private static final String U = SLASH + "u";
	private static final String BIG_U = SLASH + "U";
	private final SPARQLParser parser = new SPARQLParser();

	@Test
	void escapedQuoteMustNotEndStringContext() {
		for (String quote : List.of("\"", "'")) {
			for (String codepoint : List.of(U + "0062", BIG_U + "00000062")) {
				String literal = quote + "a" + SLASH + quote + codepoint + quote;
				literal(project(literal), "a" + quote + "b");
				literal("SELECT ?v WHERE { OPTIONAL { BIND(" + literal + " AS ?v) } }", "a" + quote + "b");
				literal("SELECT ?v WHERE { FILTER NOT EXISTS { BIND(" + literal + " AS ?v) } }", "a" + quote + "b");
				literal("SELECT ?v WHERE { {} MINUS { BIND(" + literal + " AS ?v) } }", "a" + quote + "b");
				assertDoesNotThrow(() -> parser.parseUpdate("INSERT DATA { <urn:s> <urn:p> " + literal + " }", BASE));
			}
		}
	}

	@Test
	void ordinaryQuotesMustNotEndLongStringContext() {
		for (String quote : List.of("\"", "'")) {
			String literal = quote.repeat(3) + "a" + quote + U + "0062" + quote.repeat(3);
			literal(project(literal), "a" + quote + "b");
			literal = quote.repeat(3) + "a" + quote + "\n" + BIG_U + "0001F47E" + quote.repeat(3);
			literal(project(literal), "a" + quote + "\n" + new String(Character.toChars(0x1F47E)));
		}
	}

	@Test
	void commentsMustNotChangeNextStringContext() {
		for (String quote : List.of("\"", "'")) {
			literal("# unmatched " + quote + "\n" + project(quote + U + "0061" + quote), "a");
			literal("# " + quote + U + "D800\n" + project("\"ok\""), "ok");
		}
	}

	@Test
	void numericEscapeCanBeFirstIriCharacter() {
		for (String codepoint : List.of(U + "0061", BIG_U + "00000061")) {
			value(project("<" + codepoint + ">"), SimpleValueFactory.getInstance().createIRI(BASE + "a"));
			value("PREFIX ex: <" + codepoint + "> " + project("ex:tail"),
					SimpleValueFactory.getInstance().createIRI(BASE + "atail"));
		}
	}

	@Test
	void numericEscapeCanBeginAbsoluteBaseIri() {
		for (String codepoint : List.of(U + "0068", BIG_U + "00000068")) {
			value("BASE <" + codepoint + "ttp://example.org/> " + project("<tail>"),
					SimpleValueFactory.getInstance().createIRI(BASE + "tail"));
		}
	}

	@Test
	void compactLessThanMustNotStartIriContext() {
		literal("SELECT ?x WHERE { FILTER(?x<\"a>" + U + "0062\") }", "a>b");
		literal("SELECT ?x WHERE { FILTER(?x<\"\"\"a>" + U + "0062\"\"\") }", "a>b");
	}

	@Test
	void delimitersOutsideStringsAndIrisMustNotChangeContext() {
		// an escaped quote in a PN_LOCAL does not start a string
		literal("PREFIX ex: <urn:ex:> SELECT ?v WHERE { ex:o" + SLASH + "'n ex:p ?o BIND(\"" + U + "0061\" AS ?v) }",
				"a");
		// a '<' inside a comment does not start an IRI
		literal("# <comment\n" + project("\"" + U + "0061\""), "a");
		// a compact '<=' followed by a string is an operator, not an IRI
		literal("SELECT ?x WHERE { FILTER(?x<=\"a>" + U + "0062\") }", "a>b");
	}

	@Test
	void literalBackslashMustNotIntroduceAnotherNumericEscape() {
		for (String quote : List.of("\"", "'", "\"\"\"", "'''")) {
			literal(project(quote + SLASH + SLASH + "u0041" + quote), SLASH + "u0041");
			literal(project(quote + U + "005C" + "u0041" + quote), SLASH + "u0041");
		}
	}

	@Test
	void ordinaryAndEscapedSupplementaryCharactersRemainValid() {
		String supplementary = new String(Character.toChars(0x1F47E));
		for (String quote : List.of("\"", "'", "\"\"\"", "'''")) {
			literal(project(quote + supplementary + quote), supplementary);
			literal(project(quote + BIG_U + "0001F47E" + quote), supplementary);
			literal(project(quote + U + "0061" + quote), "a");
		}
		literal("SELECT ?x WHERE { FILTER(?x< \"a>" + U + "0062\") }", "a>b");
	}

	private void literal(String query, String expected) {
		value(query, SimpleValueFactory.getInstance().createLiteral(expected));
	}

	private void value(String query, Value expected) {
		ParsedQuery parsed = assertDoesNotThrow(() -> parser.parseQuery(query, BASE), query);
		List<Value> values = new ArrayList<>();
		parsed.getTupleExpr().visit(new AbstractQueryModelVisitor<RuntimeException>() {
			@Override
			public void meet(ValueConstant node) {
				values.add(node.getValue());
			}
		});
		assertEquals(List.of(expected), values, query);
	}

	private static String project(String term) {
		return "SELECT (" + term + " AS ?v) WHERE {}";
	}
}
