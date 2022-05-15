/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple lexer that tokenizes a syntactically legal input SPARQL query string on prolog items (prefixes, base
 * declarations, IRIs, comments, and syntactical tokens such as keywords, opening and closing brackets, and hashes).
 *
 * @author Jeen Broekstra
 */
public class QueryPrologLexer {

	public enum TokenType {
		PREFIX_KEYWORD,
		PREFIX,
		BASE_KEYWORD,
		LBRACKET,
		RBRACKET,
		IRI,
		HASH,
		COMMENT,
		REST_OF_QUERY
	}

	private static final Token HASH_TOKEN = new Token(TokenType.HASH, "#");

	private static final Token PREFIX_KEYWORD_TOKEN = new Token(TokenType.PREFIX_KEYWORD, "PREFIX");

	private static final Token BASE_KEYWORD_TOKEN = new Token(TokenType.BASE_KEYWORD, "BASE");

	private static final Token LBRACKET_TOKEN = new Token(TokenType.LBRACKET, "<");

	private static final Token RBRACKET_TOKEN = new Token(TokenType.RBRACKET, ">");

	private static final Pattern IRI_PATTERN = Pattern.compile("^<([^>]*)>*");

	private static final Pattern PREFIX_PATTERN = Pattern.compile("^prefix([^:]+):", Pattern.CASE_INSENSITIVE);

	// private static final Pattern COMMENT_PATTERN = Pattern.compile("^#([^\n]+/)");
	private static final Pattern COMMENT_PATTERN = Pattern.compile("^(#.*((\r)?\n|(\r)?\n*))*");

	public static class Token {

		public final TokenType t;

		public final String s;

		public Token(TokenType t, String s) {
			this.t = t;
			this.s = s;
		}

		public TokenType getType() {
			return t;
		}

		/**
		 * Get the corresponding string value for this token. For example in the case of an {@link TokenType#IRI} token,
		 * this will return the string representation of that IRI.
		 */
		public String getStringValue() {
			return s;
		}

		@Override
		public String toString() {
			return "[" + t.toString() + "] '" + s + "'";
		}
	}

	/**
	 * Tokenizes a syntactically legal input SPARQL query on prolog elements. The last token in the returned list is of
	 * type {@link TokenType#REST_OF_QUERY} and contains the SPARQL query string minus the prolog.
	 *
	 * @param input a syntactically legal SPARQL query string
	 * @return a list with tokens for each prolog element. If the input string is syntactically legal SPARQL, the final
	 *         returned token is guaranteed to be of type {@link TokenType#REST_OF_QUERY} and to contain the SPARQL
	 *         query string minus the prolog. If the input string is not syntactically legal SPARQL, the method will
	 *         still return normally but no guarantees about the returned list are made.
	 */
	public static List<Token> lex(String input) {
		final List<Token> result = new ArrayList<>();
		for (int i = 0; i < input.length();) {
			char c = input.charAt(i);
			switch (c) {
			case '#':
				result.add(HASH_TOKEN);
				String comment = readComment(input, i);
				i += comment.length() + 1; // 1 for hash
				result.add(new Token(TokenType.COMMENT, comment));
				break;
			case 'p':
			case 'P':
				result.add(PREFIX_KEYWORD_TOKEN);
				// read PREFIX
				String prefix = readPrefix(input, i);
				result.add(new Token(TokenType.PREFIX, prefix.trim()));
				i = i + prefix.length() + 7; // 6 for prefix keyword, 1 for ':'
				break;
			case 'b':
			case 'B':
				result.add(BASE_KEYWORD_TOKEN);
				i += 4; // 4 for base keyword
				break;
			case '<':
				// read IRI
				result.add(LBRACKET_TOKEN);
				String iri = readIRI(input, i);
				result.add(new Token(TokenType.IRI, iri));
				result.add(RBRACKET_TOKEN);
				i += iri.length() + 2; // 2 for opening and closing brackets
				break;
			default:
				if (Character.isWhitespace(c)) {
					i++;
				} else {
					String restOfQuery = input.substring(i);
					result.add(new Token(TokenType.REST_OF_QUERY, restOfQuery));
					i += restOfQuery.length();
				}
				break;
			}
		}

		return result;
	}

	/**
	 * Tokenizes the input string on prolog elements and returns the final Token. If the input string is a syntactically
	 * legal SPARQL query, this Token will be of type {@link TokenType#REST_OF_QUERY} and contain the query string minus
	 * prolog.
	 *
	 * @param input a syntactically legal SPARQL string
	 * @return if the input is syntactically legal SPARQL, a Token containing the query string without prolog. If the
	 *         input is not syntactically legal, the method will still exist normally, but no guarantees are made about
	 *         the returned object.
	 */
	public static Token getRestOfQueryToken(String input) {
		Token result = null;
		for (int i = 0; i < input.length();) {
			char c = input.charAt(i);
			switch (c) {
			case '#':
				String comment = readComment(input, i);
				i += comment.length() + 1; // 1 for hash
				break;
			case 'p':
			case 'P':
				// read PREFIX
				String prefix = readPrefix(input, i);
				if (prefix == null) {
					prefix = ""; // prevent NPE on bad input
				}
				i = i + prefix.length() + 7; // 6 for prefix keyword, 1 for ':'
				break;
			case 'b':
			case 'B':
				i += 4; // 4 for base keyword
				break;
			case '<':
				// read IRI
				String iri = readIRI(input, i);
				if (iri == null) {
					iri = ""; // prevent NPE on bad input
				}
				i += iri.length() + 2; // 2 for opening and closing brackets
				break;
			default:
				if (Character.isWhitespace(c)) {
					i++;
				} else {
					String restOfQuery = input.substring(i);
					result = (new Token(TokenType.REST_OF_QUERY, restOfQuery));
					i += restOfQuery.length();
				}
				break;
			}
		}

		return result;
	}

	/**
	 * Reads the first comment line from the input, and returns the comment line (including the line break character)
	 * without the leading "#".
	 *
	 * @param input
	 * @param index
	 * @return
	 */
	private static String readComment(String input, int index) {
		String comment = null;
		Matcher matcher = COMMENT_PATTERN.matcher(input.substring(index));
		if (matcher.find()) {
			comment = matcher.group(0);
			// the regex group includes the # => just remove it
			comment = comment.substring(1);
		}
		return comment;
	}

	private static String readPrefix(String input, int index) {
		String prefix = null;
		Matcher matcher = PREFIX_PATTERN.matcher(input.substring(index));
		if (matcher.find()) {
			prefix = matcher.group(1);
		}
		return prefix;
	}

	private static String readIRI(String input, int index) {
		String iri = null;
		Matcher matcher = IRI_PATTERN.matcher(input.substring(index));
		if (matcher.find()) {
			iri = matcher.group(1);
		}
		return iri;
	}
}
