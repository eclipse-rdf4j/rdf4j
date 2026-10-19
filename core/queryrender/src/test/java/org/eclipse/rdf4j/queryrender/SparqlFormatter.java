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

	public static String format(String query) {
		return format(query, 2);
	}

	public static String format(String query, int indentWidth) {
		if (query == null) {
			return null;
		}

		final String s = query;
		final int n = s.length();

		StringBuilder out = new StringBuilder(n + 64);

		int braceIndent = 0; // spaces due to { }
		boolean atLineStart = true;
		int lineStart = 0; // start index in 'out' of the current line
		int pendingPredicateCol = -1; // set after ';', used exactly once on the next non-ws token

		State st = new State();

		for (int i = 0; i < n; i++) {
			char ch = s.charAt(i);

			// COMMENT MODE
			if (st.inComment) {
				out.append(ch);
				if (ch == '\n') {
					atLineStart = true;
					lineStart = out.length();
					st.inComment = false;
					pendingPredicateCol = -1; // new line cancels alignment
				}
				continue;
			}

			// STRING MODES
			if (st.inString) {
				out.append(ch);
				if (st.esc) {
					st.esc = false;
					continue;
				}
				if (ch == '\\') {
					st.esc = true;
					continue;
				}
				if (ch == st.quote) {
					if (st.longString) {
						if (i + 2 < n && s.charAt(i + 1) == st.quote && s.charAt(i + 2) == st.quote) {
							out.append(st.quote).append(st.quote);
							i += 2;
							st.resetString();
						}
					} else {
						st.resetString();
					}
				}
				continue;
			}

			// IRI MODE
			if (st.inIRI) {
				out.append(ch);
				if (ch == '>') {
					st.inIRI = false;
				}
				continue;
			}

			// TOP-LEVEL: decide behavior

			if (ch == '#') {
				// Start a comment at current line; honor pending alignment if at line start.
				if (atLineStart) {
					appendLineIndent(out, braceIndent, pendingPredicateCol);
					atLineStart = false;
					pendingPredicateCol = -1;
				}
				out.append('#');
				st.inComment = true;
				continue;
			}

			if (ch == '<') { // IRI start
				if (atLineStart) {
					appendLineIndent(out, braceIndent, pendingPredicateCol);
					atLineStart = false;
					pendingPredicateCol = -1;
				}
				out.append('<');
				st.inIRI = true;
				continue;
			}

			if (ch == '"' || ch == '\'') { // string start
				if (atLineStart) {
					appendLineIndent(out, braceIndent, pendingPredicateCol);
					atLineStart = false;
					pendingPredicateCol = -1;
				}
				boolean isLong = (i + 2 < n && s.charAt(i + 1) == ch && s.charAt(i + 2) == ch);
				out.append(ch);
				if (isLong) {
					out.append(ch).append(ch);
					i += 2;
				}
				st.startString(ch, isLong);
				continue;
			}

			if (ch == '{') {
				if (atLineStart) {
					appendIndent(out, braceIndent);
				} else if (needsSpaceBefore(out)) {
					out.append(' ');
				}
				out.append('{').append('\n');
				atLineStart = true;
				lineStart = out.length();
				braceIndent += indentWidth;
				pendingPredicateCol = -1; // after an opening brace, no predicate alignment pending
				i = skipWs(s, i + 1) - 1; // normalize whitespace after '{'
				continue;
			}

			if (ch == '}') {
				// finish any partial line
				if (!atLineStart) {
					rstripLine(out, lineStart);
					out.append('\n');
				}
				braceIndent = Math.max(0, braceIndent - indentWidth);
				appendIndent(out, braceIndent);
				out.append('}').append('\n');
				atLineStart = true;
				lineStart = out.length();
				pendingPredicateCol = -1;

				// handle "} UNION {"
				int j = skipWs(s, i + 1);
				if (matchesWordIgnoreCase(s, j, "UNION")) {
					appendIndent(out, braceIndent + 2);
					out.append("UNION").append('\n');
					atLineStart = true;
					lineStart = out.length();

					j = skipWs(s, j + 5);
					if (j < n && s.charAt(j) == '{') {
						appendIndent(out, braceIndent);
						out.append('{').append('\n');
						atLineStart = true;
						lineStart = out.length();
						braceIndent += indentWidth;
						j = skipWs(s, j + 1);
					}
					i = j - 1;
				} else {
					i = j - 1;
				}
				continue;
			}

			if (ch == '[') {
				if (atLineStart) {
					appendLineIndent(out, braceIndent, pendingPredicateCol);
					atLineStart = false;
					pendingPredicateCol = -1;
				}
				int after = formatSquareBlock(s, i, out, lineStart); // writes either [] or a multi-line block
				i = after - 1;
				// if helper ended with newline, reflect that
				if (out.length() > 0 && out.charAt(out.length() - 1) == '\n') {
					atLineStart = true;
					lineStart = out.length();
				}
				continue;
			}

			if (ch == '(') {
				if (atLineStart) {
					appendLineIndent(out, braceIndent, pendingPredicateCol);
					atLineStart = false;
					pendingPredicateCol = -1;
				}
				int after = formatParenCollapsed(s, i, out);
				i = after - 1;
				continue;
			}

			if (ch == ';') {
				// End of predicate-object pair (outside []), start next predicate under the same column.
				out.append(';');
				pendingPredicateCol = computePredicateColumnFromCurrentLine(out, lineStart);
				out.append('\n');
				atLineStart = true;
				lineStart = out.length();

				// CRITICAL: skip all whitespace in INPUT following ';' so we don't double-indent.
				i = skipWs(s, i + 1) - 1;
				continue;
			}

			if (ch == '\r' || ch == '\n') {
				if (!atLineStart) {
					rstripLine(out, lineStart);
					out.append('\n');
					atLineStart = true;
					lineStart = out.length();
				}
				i = skipNewlines(s, i + 1) - 1;
				pendingPredicateCol = -1; // a raw newline resets alignment
				continue;
			}

			if (ch == ' ' || ch == '\t') {
				// Drop leading indentation from the input; otherwise copy spaces.
				if (!atLineStart) {
					out.append(ch);
				}
				while (atLineStart && i + 1 < n && (s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\t')) {
					i++;
				}
				continue;
			}

			// Default: normal token character
			if (atLineStart) {
				appendLineIndent(out, braceIndent, pendingPredicateCol);
				atLineStart = false;
				pendingPredicateCol = -1;
			}
			out.append(ch);
		}

		// Trim trailing whitespace/newlines.
		int end = out.length();
		while (end > 0 && Character.isWhitespace(out.charAt(end - 1))) {
			end--;
		}
		return out.substring(0, end);
	}

	/* ================= helpers ================= */

	private static void appendLineIndent(StringBuilder out, int braceIndent, int pendingPredicateCol) {
		appendIndent(out, pendingPredicateCol >= 0 ? pendingPredicateCol : braceIndent);
	}

	private static void appendIndent(StringBuilder sb, int spaces) {
		for (int i = 0; i < spaces; i++) {
			sb.append(' ');
		}
	}

	private static void rstripLine(StringBuilder sb, int lineStart) {
		int i = sb.length();
		while (i > lineStart) {
			char c = sb.charAt(i - 1);
			if (c == ' ' || c == '\t') {
				i--;
			} else {
				break;
			}
		}
		if (i < sb.length()) {
			sb.setLength(i);
		}
	}

	private static boolean needsSpaceBefore(StringBuilder out) {
		int len = out.length();
		return len > 0 && !Character.isWhitespace(out.charAt(len - 1));
	}

	private static int skipWs(String s, int pos) {
		int i = pos;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
				break;
			}
			i++;
		}
		return i;
	}

	private static int skipNewlines(String s, int pos) {
		int i = pos;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (c != '\r' && c != '\n') {
				break;
			}
			i++;
		}
		return i;
	}

	private static boolean matchesWordIgnoreCase(String s, int pos, String word) {
		int end = pos + word.length();
		if (pos < 0 || end > s.length()) {
			return false;
		}
		if (!s.regionMatches(true, pos, word, 0, word.length())) {
			return false;
		}
		if (end < s.length() && isWordChar(s.charAt(end))) {
			return false;
		}
		return pos == 0 || !isWordChar(s.charAt(pos - 1));
	}

	private static boolean isWordChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

	/** Decide the predicate start column by reading the ALREADY EMITTED current line. */
	private static int computePredicateColumnFromCurrentLine(StringBuilder out, int lineStart) {
		int i = lineStart, n = out.length();
		while (i < n && (out.charAt(i) == ' ' || out.charAt(i) == '\t')) {
			i++; // leading spaces
		}
		i = skipSubjectToken(out, i, n); // subject token
		while (i < n && (out.charAt(i) == ' ' || out.charAt(i) == '\t')) {
			i++; // spaces before predicate
		}
		return i - lineStart;
	}

	private static int skipSubjectToken(CharSequence s, int i, int n) {
		if (i >= n) {
			return i;
		}
		char c = s.charAt(i);

		if (c == '[') { // blank node subject
			int depth = 0;
			boolean inIRI = false, inStr = false, esc = false;
			char q = 0;
			for (int j = i + 1; j < n; j++) {
				char d = s.charAt(j);
				if (inIRI) {
					if (d == '>') {
						inIRI = false;
					}
					continue;
				}
				if (inStr) {
					if (esc) {
						esc = false;
						continue;
					}
					if (d == '\\') {
						esc = true;
						continue;
					}
					if (d == q) {
						inStr = false;
					}
					continue;
				}
				if (d == '<') {
					inIRI = true;
					continue;
				}
				if (d == '"' || d == '\'') {
					inStr = true;
					q = d;
					continue;
				}
				if (d == '[') {
					depth++;
					continue;
				}
				if (d == ']') {
					if (depth == 0) {
						return j + 1;
					}
					depth--;
				}
			}
			return n;
		}

		if (c == '(') { // collection subject
			int depth = 0;
			boolean inIRI = false, inStr = false, esc = false;
			char q = 0;
			for (int j = i + 1; j < n; j++) {
				char d = s.charAt(j);
				if (inIRI) {
					if (d == '>') {
						inIRI = false;
					}
					continue;
				}
				if (inStr) {
					if (esc) {
						esc = false;
						continue;
					}
					if (d == '\\') {
						esc = true;
						continue;
					}
					if (d == q) {
						inStr = false;
					}
					continue;
				}
				if (d == '<') {
					inIRI = true;
					continue;
				}
				if (d == '"' || d == '\'') {
					inStr = true;
					q = d;
					continue;
				}
				if (d == '(') {
					depth++;
					continue;
				}
				if (d == ')') {
					if (depth == 0) {
						return j + 1;
					}
					depth--;
				}
			}
			return n;
		}

		if (c == '<') { // IRI subject
			int j = i + 1;
			while (j < n && s.charAt(j) != '>') {
				j++;
			}
			return Math.min(n, j + 1);
		}

		if (c == '?' || c == '$') { // variable subject
			int j = i + 1;
			while (j < n && isNameChar(s.charAt(j))) {
				j++;
			}
			return j;
		}

		// QName or 'a'
		int j = i;
		while (j < n) {
			char d = s.charAt(j);
			if (Character.isWhitespace(d)) {
				break;
			}
			if ("{}[]().,;".indexOf(d) >= 0) {
				break;
			}
			j++;
		}
		return j;
	}

	private static boolean isNameChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '-';
	}

	/* -------- square brackets -------- */

	/**
	 * Format a '[' ... ']' block. - If no top-level ';' inside: single line with collapsed inner whitespace: `[ ... ]`
	 * - Else: multi-line with content indented 2 spaces past '[' and ']' aligned under '['. Returns index AFTER the
	 * matching ']' in the INPUT.
	 */
	private static int formatSquareBlock(String s, int i, StringBuilder out, int lineStartOut) {
		final int n = s.length();
		int j = i + 1;

		ScanState scan = new ScanState();
		int innerDepth = 0;
		boolean hasTopLevelSemicolon = false;

		for (; j < n; j++) {
			char c = s.charAt(j);

			if (scan.inComment) {
				if (c == '\n') {
					scan.inComment = false;
				}
				continue;
			}
			if (scan.inIRI) {
				if (c == '>') {
					scan.inIRI = false;
				}
				continue;
			}
			if (scan.inString) {
				if (scan.esc) {
					scan.esc = false;
					continue;
				}
				if (c == '\\') {
					scan.esc = true;
					continue;
				}
				if (c == scan.quote) {
					if (scan.longString) {
						if (j + 2 < n && s.charAt(j + 1) == scan.quote && s.charAt(j + 2) == scan.quote) {
							j += 2;
							scan.resetString();
						}
					} else {
						scan.resetString();
					}
				}
				continue;
			}

			if (c == '#') {
				scan.inComment = true;
				continue;
			}
			if (c == '<') {
				scan.inIRI = true;
				continue;
			}
			if (c == '"' || c == '\'') {
				boolean isLong = (j + 2 < n && s.charAt(j + 1) == c && s.charAt(j + 2) == c);
				scan.startString(c, isLong);
				continue;
			}

			if (c == '[') {
				innerDepth++;
				continue;
			}
			if (c == ']') {
				if (innerDepth == 0) {
					break;
				}
				innerDepth--;
				continue;
			}
			if (c == ';' && innerDepth == 0) {
				hasTopLevelSemicolon = true;
			}
		}
		int end = j; // position of the matching ']'

		if (end >= n || s.charAt(end) != ']') {
			out.append('['); // unmatched; emit literal '[' and move on
			return i + 1;
		}

		if (!hasTopLevelSemicolon) {
			// Single-line blank node: normalize inner ws to single spaces.
			String inner = collapseWsExceptInStringsAndIRIs(s.substring(i + 1, end));
			if (inner.isEmpty()) {
				out.append("[]");
			} else {
				out.append('[').append(' ').append(inner).append(' ').append(']');
			}
			return end + 1;
		}

		// Multi-line blank node
		int bracketCol = out.length() - lineStartOut; // column where '[' appears
		out.append('[').append('\n');

		int contentIndent = bracketCol + 2;
		int k = i + 1;
		boolean atLineStart = true;

		while (k < end) {
			char c = s.charAt(k);

			// comments
			if (scan.inComment) {
				if (atLineStart) {
					appendIndent(out, contentIndent);
					atLineStart = false;
				}
				out.append(c);
				if (c == '\n') {
					atLineStart = true;
					scan.inComment = false;
				}
				k++;
				continue;
			}
			// IRIs
			if (scan.inIRI) {
				if (atLineStart) {
					appendIndent(out, contentIndent);
					atLineStart = false;
				}
				out.append(c);
				if (c == '>') {
					scan.inIRI = false;
				}
				k++;
				continue;
			}
			// strings
			if (scan.inString) {
				if (atLineStart) {
					appendIndent(out, contentIndent);
					atLineStart = false;
				}
				out.append(c);
				if (scan.esc) {
					scan.esc = false;
					k++;
					continue;
				}
				if (c == '\\') {
					scan.esc = true;
					k++;
					continue;
				}
				if (c == scan.quote) {
					if (scan.longString) {
						if (k + 2 < end && s.charAt(k + 1) == scan.quote && s.charAt(k + 2) == scan.quote) {
							out.append(scan.quote).append(scan.quote);
							k += 3;
							scan.resetString();
							continue;
						}
					} else {
						scan.resetString();
					}
				}
				k++;
				continue;
			}

			// structural
			if (c == '#') {
				if (atLineStart) {
					appendIndent(out, contentIndent);
					atLineStart = false;
				}
				out.append('#');
				scan.inComment = true;
				k++;
				continue;
			}
			if (c == '<') {
				if (atLineStart) {
					appendIndent(out, contentIndent);
					atLineStart = false;
				}
				out.append('<');
				scan.inIRI = true;
				k++;
				continue;
			}
			if (c == '"' || c == '\'') {
				boolean isLong = (k + 2 < end && s.charAt(k + 1) == c && s.charAt(k + 2) == c);
				if (atLineStart) {
					appendIndent(out, contentIndent);
					atLineStart = false;
				}
				out.append(c);
				if (isLong) {
					out.append(c).append(c);
					k += 3;
				} else {
					k++;
				}
				scan.startString(c, isLong);
				continue;
			}
			if (c == '[') {
				if (atLineStart) {
					appendIndent(out, contentIndent);
					atLineStart = false;
				}
				int after = formatSquareBlock(s, k, out,
						out.length() - (out.length() - (out.length() - contentIndent))); // effectively line start
				k = after;
				continue;
			}
			if (c == '(') {
				if (atLineStart) {
					appendIndent(out, contentIndent);
					atLineStart = false;
				}
				int after = formatParenCollapsed(s, k, out);
				k = after;
				continue;
			}
			if (c == ';') {
				out.append(';').append('\n');
				atLineStart = true;
				k = skipWs(s, k + 1);
				continue;
			}

			if (c == '\r' || c == '\n') {
				if (!atLineStart) {
					out.append(' ');
				}
				k = skipNewlines(s, k + 1);
				continue;
			}
			if (c == ' ' || c == '\t') {
				int w = k + 1;
				while (w < end && (s.charAt(w) == ' ' || s.charAt(w) == '\t')) {
					w++;
				}
				if (!atLineStart) {
					out.append(' ');
				}
				k = w;
				continue;
			}

			if (atLineStart) {
				appendIndent(out, contentIndent);
				atLineStart = false;
			}
			out.append(c);
			k++;
		}

		// Close and align ']'
		if (out.length() == 0 || out.charAt(out.length() - 1) != '\n') {
			out.append('\n');
		}
		appendIndent(out, bracketCol);
		out.append(']');
		return end + 1;
	}

	/** Format a '(' ... ')' block by collapsing inner whitespace to single spaces. */
	private static int formatParenCollapsed(String s, int i, StringBuilder out) {
		final int n = s.length();
		int j = i + 1;

		ScanState scan = new ScanState();
		int parenDepth = 0;
		StringBuilder inner = new StringBuilder();

		for (; j < n; j++) {
			char c = s.charAt(j);
			if (scan.inComment) {
				if (c == '\n') {
					scan.inComment = false;
				}
				continue;
			}
			if (scan.inIRI) {
				inner.append(c);
				if (c == '>') {
					scan.inIRI = false;
				}
				continue;
			}
			if (scan.inString) {
				inner.append(c);
				if (scan.esc) {
					scan.esc = false;
					continue;
				}
				if (c == '\\') {
					scan.esc = true;
					continue;
				}
				if (c == scan.quote) {
					if (scan.longString) {
						if (j + 2 < n && s.charAt(j + 1) == scan.quote && s.charAt(j + 2) == scan.quote) {
							inner.append(scan.quote).append(scan.quote);
							j += 2;
							scan.resetString();
						}
					} else {
						scan.resetString();
					}
				}
				continue;
			}
			if (c == '#') {
				scan.inComment = true;
				continue;
			}
			if (c == '<') {
				inner.append('<');
				scan.inIRI = true;
				continue;
			}
			if (c == '"' || c == '\'') {
				boolean isLong = (j + 2 < n && s.charAt(j + 1) == c && s.charAt(j + 2) == c);
				inner.append(c);
				if (isLong) {
					inner.append(c).append(c);
					j += 2;
				}
				scan.startString(c, isLong);
				continue;
			}
			if (c == '(') {
				parenDepth++;
				inner.append(c);
				continue;
			}
			if (c == ')') {
				if (parenDepth == 0) {
					break;
				}
				parenDepth--;
				inner.append(c);
				continue;
			}
			inner.append(c);
		}
		int end = j;

		String collapsed = collapseSimple(inner);
		out.append('(');
		if (!collapsed.isEmpty()) {
			out.append(' ').append(collapsed).append(' ');
		}
		out.append(')');
		return end + 1;
	}

	private static String collapseSimple(CharSequence inner) {
		StringBuilder dst = new StringBuilder(inner.length());
		boolean lastSpace = false;
		for (int i = 0; i < inner.length(); i++) {
			char c = inner.charAt(i);
			if (Character.isWhitespace(c)) {
				if (!lastSpace) {
					dst.append(' ');
					lastSpace = true;
				}
			} else {
				dst.append(c);
				lastSpace = false;
			}
		}
		int a = 0, b = dst.length();
		if (a < b && dst.charAt(a) == ' ') {
			a++;
		}
		if (a < b && dst.charAt(b - 1) == ' ') {
			b--;
		}
		return dst.substring(a, b);
	}

	private static String collapseWsExceptInStringsAndIRIs(String src) {
		StringBuilder dst = new StringBuilder(src.length());
		boolean inIRI = false, inStr = false, esc = false, longStr = false;
		char quote = 0;
		boolean wroteSpace = false;

		for (int i = 0; i < src.length(); i++) {
			char c = src.charAt(i);
			if (inIRI) {
				dst.append(c);
				if (c == '>') {
					inIRI = false;
				}
				continue;
			}
			if (inStr) {
				dst.append(c);
				if (esc) {
					esc = false;
					continue;
				}
				if (c == '\\') {
					esc = true;
					continue;
				}
				if (c == quote) {
					if (longStr) {
						if (i + 2 < src.length() && src.charAt(i + 1) == quote && src.charAt(i + 2) == quote) {
							dst.append(quote).append(quote);
							i += 2;
							inStr = false;
						}
					} else {
						inStr = false;
					}
				}
				continue;
			}
			if (c == '<') {
				dst.append(c);
				inIRI = true;
				wroteSpace = false;
				continue;
			}
			if (c == '"' || c == '\'') {
				boolean isLong = (i + 2 < src.length() && src.charAt(i + 1) == c && src.charAt(i + 2) == c);
				dst.append(c);
				if (isLong) {
					dst.append(c).append(c);
					i += 2;
				}
				inStr = true;
				quote = c;
				longStr = isLong;
				wroteSpace = false;
				continue;
			}
			if (Character.isWhitespace(c)) {
				if (!wroteSpace) {
					dst.append(' ');
					wroteSpace = true;
				}
				continue;
			}
			dst.append(c);
			wroteSpace = false;
		}
		int a = 0, b = dst.length();
		if (a < b && dst.charAt(a) == ' ') {
			a++;
		}
		if (a < b && dst.charAt(b - 1) == ' ') {
			b--;
		}
		return dst.substring(a, b);
	}

	/* ===== small state carriers ===== */

	private static final class State {
		boolean inIRI = false, inComment = false, inString = false, longString = false, esc = false;
		char quote = 0;

		void startString(char q, boolean isLong) {
			inString = true;
			quote = q;
			longString = isLong;
			esc = false;
		}

		void resetString() {
			inString = false;
			longString = false;
			quote = 0;
			esc = false;
		}
	}

	private static final class ScanState {
		boolean inIRI = false, inComment = false, inString = false, longString = false, esc = false;
		char quote = 0;

		void startString(char q, boolean isLong) {
			inString = true;
			quote = q;
			longString = isLong;
			esc = false;
		}

		void resetString() {
			inString = false;
			longString = false;
			quote = 0;
			esc = false;
		}
	}

	public static void main(String[] args) {
		String test = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pC ?u2 .\n" +
				"    FILTER EXISTS {\n" +
				"        {\n" +
				"          ?s ex:pC ?u0 .\n" +
				"          FILTER EXISTS { { \n" +
				"            ?s !(ex:pB|foaf:name) ?o .\n" +
				"          } }\n" +
				"      }\n" +
				"        UNION\n" +
				"      {\n" +
				"        ?u1 ex:pD ?v1 .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

//		System.out.println("Original:\n" + test);
//		System.out.println("Formatted:");

		System.out.println(format(test));
	}

}
