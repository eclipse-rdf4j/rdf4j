/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.queryrender;

public final class SparqlFormatter {

	private SparqlFormatter() {
	}

	/**
	 * Formats SPARQL by: • newline after each '{' • each '}' on its own line • two spaces per nested block •
	 * special-case: "} UNION {" becomes: } UNION {
	 */
	public static String formatBraces(String query) {
		return formatBraces(query, 2);
	}

	/** Same as formatBraces(query) but with a customizable indent width. */
	public static String formatBraces(String query, int indentWidth) {
		if (query == null)
			return null;

		StringBuilder out = new StringBuilder(query.length() + 32);
		int indent = 0;
		boolean atLineStart = true;
		final int n = query.length();

		for (int i = 0; i < n; i++) {
			char ch = query.charAt(i);

			if (ch == '{') {
				if (atLineStart) {
					appendIndent(out, indent);
				} else if (out.length() > 0 && !Character.isWhitespace(out.charAt(out.length() - 1))) {
					out.append(' ');
				}
				out.append('{').append('\n');
				indent += indentWidth;
				atLineStart = true;

				i = skipWhitespace(query, i + 1) - 1; // normalize whitespace after '{'
			} else if (ch == '}') {
				// Close current line if needed, then print '}' on its own line.
				if (!atLineStart)
					out.append('\n');
				indent = Math.max(0, indent - indentWidth);
				appendIndent(out, indent);
				out.append('}').append('\n');
				atLineStart = true;

				// SPECIAL CASE: handle "} UNION {"
				int j = skipWhitespace(query, i + 1);
				if (matchesWordIgnoreCase(query, j, "UNION")) {
					// Print " UNION" at current indent + 2 spaces.
					appendIndent(out, indent + 2);
					out.append("UNION").append('\n');
					atLineStart = true;

					j = skipWhitespace(query, j + "UNION".length());
					// If next non-space is '{', put it alone on the next line, then indent inside it.
					if (j < n && query.charAt(j) == '{') {
						appendIndent(out, indent);
						out.append('{').append('\n');
						indent += indentWidth;
						atLineStart = true;
						j = skipWhitespace(query, j + 1);
					}
					i = j - 1; // continue from here
				} else {
					// Otherwise, continue as usual after the '}'.
					i = j - 1;
				}
			} else if (ch == '\r' || ch == '\n') {
				// Normalize any newline runs to a single controlled boundary.
				if (!atLineStart) {
					out.append('\n');
					atLineStart = true;
				}
				i = skipNewlines(query, i + 1) - 1;
			} else {
				if (atLineStart) {
					appendIndent(out, indent);
					atLineStart = false;
				}
				out.append(ch);
			}
		}

		// Trim trailing whitespace/newlines.
		int end = out.length();
		while (end > 0 && Character.isWhitespace(out.charAt(end - 1)))
			end--;
		return out.substring(0, end);
	}

	private static void appendIndent(StringBuilder sb, int spaces) {
		for (int i = 0; i < spaces; i++)
			sb.append(' ');
	}

	private static int skipWhitespace(String s, int pos) {
		int i = pos;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (c != ' ' && c != '\t' && c != '\r' && c != '\n')
				break;
			i++;
		}
		return i;
	}

	private static int skipNewlines(String s, int pos) {
		int i = pos;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (c != '\r' && c != '\n')
				break;
			i++;
		}
		return i;
	}

	private static boolean matchesWordIgnoreCase(String s, int pos, String word) {
		int end = pos + word.length();
		if (pos < 0 || end > s.length())
			return false;
		if (!s.regionMatches(true, pos, word, 0, word.length()))
			return false;

		// Right boundary: next char must not be a word char (letter/digit/underscore)
		if (end < s.length() && isWordChar(s.charAt(end)))
			return false;
		// Left boundary: previous char must not be a word char (safe in our use, but keep consistent)
		if (pos > 0 && isWordChar(s.charAt(pos - 1)))
			return false;

		return true;
	}

	private static boolean isWordChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

	public static void main(String[] args) {
		String test = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
//				"      {\n" +
				"        ?s ^<http://example.org/p/I2> ?o . \n" +
//				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";
		System.out.println(formatBraces(test));
	}

}
