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
package org.eclipse.rdf4j.query.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.rdf4j.query.parser.QueryPrologLexer.Token;
import org.eclipse.rdf4j.query.parser.QueryPrologLexer.TokenType;
import org.junit.Test;

/**
 * @author jeen
 */
public class QueryPrologLexerTest {

	@Test
	public void testLexEmptyString() {
		List<Token> tokens = QueryPrologLexer.lex("");
		assertNotNull(tokens);
		assertEquals(0, tokens.size());

	}

	@Test
	public void testFinalTokenEmptyString() {
		try {
			Token t = QueryPrologLexer.getRestOfQueryToken("");
		} catch (Exception e) {
			fail("lexer should not throw exception on malformed input");
		}
	}

	@Test
	public void testLexNoProlog1() {
		List<Token> tokens = QueryPrologLexer.lex("SELECT * WHERE {?s ?p ?o} ");
		assertNotNull(tokens);
		assertEquals(1, tokens.size());

		Token t = tokens.get(0);
		assertTrue(t.getType().equals(TokenType.REST_OF_QUERY));
	}

	@Test
	public void testFinalTokenNoProlog1() {
		Token t = QueryPrologLexer.getRestOfQueryToken("SELECT * WHERE {?s ?p ?o} ");
		assertNotNull(t);
		assertTrue(t.getType().equals(TokenType.REST_OF_QUERY));
	}

	@Test
	public void testLexWithComment() {
		List<Token> tokens = QueryPrologLexer.lex("# COMMENT \nSELECT * WHERE {?s ?p ?o} ");
		assertNotNull(tokens);
		assertEquals(3, tokens.size());
		assertEquals(" COMMENT \n", tokens.get(1).s);

		Token t = tokens.get(tokens.size() - 1);
		assertTrue(t.getType().equals(TokenType.REST_OF_QUERY));
		assertEquals("SELECT * WHERE {?s ?p ?o} ", t.s);
	}

	@Test
	public void testLexWithComment_WindowsLinebreak() {
		List<Token> tokens = QueryPrologLexer.lex("# COMMENT \r\nSELECT * WHERE {?s ?p ?o} ");
		assertNotNull(tokens);

		Token t = tokens.get(tokens.size() - 1);
		assertTrue(t.getType().equals(TokenType.REST_OF_QUERY));
		assertEquals("SELECT * WHERE {?s ?p ?o} ", t.s);
	}

	@Test
	public void testLexWithComment_NoSpaceBeforeQuery() {
		List<Token> tokens = QueryPrologLexer.lex("# COMMENT \nSELECT * WHERE {?s ?p ?o} ");
		assertNotNull(tokens);

		Token t = tokens.get(tokens.size() - 1);
		assertTrue(t.getType().equals(TokenType.REST_OF_QUERY));
		assertEquals("SELECT * WHERE {?s ?p ?o} ", t.s);
	}

	@Test
	public void testFinalTokenWithComment() {
		Token t = QueryPrologLexer.getRestOfQueryToken("# COMMENT \n SELECT * WHERE {?s ?p ?o} ");
		assertNotNull(t);
		assertTrue(t.getType().equals(TokenType.REST_OF_QUERY));
		assertEquals("SELECT * WHERE {?s ?p ?o} ", t.s);
	}

	@Test
	public void testFinalTokenWithMultilineComment() {
		Token t = QueryPrologLexer
				.getRestOfQueryToken("# COMMENT \n# COMMENT (continued) \nSELECT * WHERE {?s ?p ?o} ");
		assertNotNull(t);
		assertTrue(t.getType().equals(TokenType.REST_OF_QUERY));
		assertEquals("SELECT * WHERE {?s ?p ?o} ", t.s);
	}

	@Test
	public void testFinalTokenWithMultilineComment2() {
		Token t = QueryPrologLexer
				.getRestOfQueryToken("#comment1\n#another comment\n" + "SELECT * WHERE { ?s ?p ?o } LIMIT 1");
		assertNotNull(t);
		assertTrue(t.getType().equals(TokenType.REST_OF_QUERY));
		assertEquals("SELECT * WHERE { ?s ?p ?o } LIMIT 1", t.s);
	}

	@Test
	public void testLexWithBaseAndComment() {
		List<Token> tokens = QueryPrologLexer.lex("BASE <foobar> # COMMENT \nSELECT * WHERE {?s ?p ?o} ");
		assertNotNull(tokens);

		Token t = tokens.get(tokens.size() - 1);
		assertTrue(t.getType().equals(TokenType.REST_OF_QUERY));
		assertEquals("SELECT * WHERE {?s ?p ?o} ", t.s);
	}

	@Test
	public void testFinalTokenWithBaseAndComment() {
		Token t = QueryPrologLexer.getRestOfQueryToken("BASE <foobar> # COMMENT \nSELECT * WHERE {?s ?p ?o} ");
		assertNotNull(t);
		assertTrue(t.getType().equals(TokenType.REST_OF_QUERY));
	}

	@Test
	public void testLexSyntaxError() {
		// all that is guaranteed in queries with syntax errors is that the lexer
		// returns. there are no guarantees that the
		// last token is the rest of the query in this case. Any syntax errors in
		// the query are to be picked up by subsequent processing.

		try {
			List<Token> tokens = QueryPrologLexer
					.lex("BASE <foobar # missing closing bracket \nSELECT * WHERE {?s ?p ?o} ");
		} catch (Exception e) {
			fail("malformed query should not make lexer fail");

		}
	}

	@Test
	public void testFinalTokenSyntaxError() {
		// all that is guaranteed in queries with syntax errors is that the lexer
		// returns. there are no guarantees that the
		// token returned is the rest of the query in this case. Any syntax errors
		// in the query are to be picked up by subsequent processing.

		try {
			Token t = QueryPrologLexer
					.getRestOfQueryToken("BASE <foobar # missing closing bracket \nSELECT * WHERE {?s ?p ?o} ");
		} catch (Exception e) {
			fail("malformed query should not make lexer fail");
		}
	}

	@Test
	public void testFinalTokenSyntaxErrorPrefix() {
		try {
			Token t = QueryPrologLexer.getRestOfQueryToken("PREFIX");
		} catch (Exception e) {
			fail("Malformed query should not make lexer throw Exception");
		}
	}
}
