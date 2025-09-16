/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.ir.util.transform;

import java.util.ArrayList;
import java.util.List;

/**
 * Depth-aware helpers for property path text handling. Centralizes common logic used by transforms to avoid duplication
 * and keep precedence/parentheses behavior consistent.
 */
public final class PathTextUtils {

	private PathTextUtils() {
	}

	/** Return true if the string has the given character at top level (not inside parentheses). */
	public static boolean hasTopLevel(final String s, final char ch) {
		if (s == null) {
			return false;
		}
		final String t = s.trim();
		int depth = 0;
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
			} else if (c == ch && depth == 0) {
				return true;
			}
		}
		return false;
	}

	/** True if the text is wrapped by a single pair of outer parentheses. */
	public static boolean isWrapped(final String s) {
		if (s == null) {
			return false;
		}
		final String t = s.trim();
		if (t.length() < 2 || t.charAt(0) != '(' || t.charAt(t.length() - 1) != ')') {
			return false;
		}
		int depth = 0;
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
			}
			if (depth == 0 && i < t.length() - 1) {
				return false; // closes too early
			}
		}
		return true;
	}

	/**
	 * True when the path text is atomic for grouping: no top-level '|' or '/', already wrapped, or NPS/inverse form.
	 */
	public static boolean isAtomicPathText(final String s) {
		if (s == null) {
			return true;
		}
		final String t = s.trim();
		if (t.isEmpty()) {
			return true;
		}
		if (isWrapped(t)) {
			return true;
		}
		if (t.startsWith("!(")) {
			return true; // negated property set is atomic
		}
		if (t.startsWith("^")) {
			final String rest = t.substring(1).trim();
			// ^IRI or ^( ... )
			return rest.startsWith("(") || (!hasTopLevel(rest, '|') && !hasTopLevel(rest, '/'));
		}
		return !hasTopLevel(t, '|') && !hasTopLevel(t, '/');
	}

	/**
	 * When using a part inside a sequence with '/', only wrap it if it contains a top-level alternation '|'.
	 */
	public static String wrapForSequence(final String part) {
		if (part == null) {
			return null;
		}
		final String t = part.trim();
		if (isWrapped(t) || !hasTopLevel(t, '|')) {
			return t;
		}
		return "(" + t + ")";
	}

	/** Prefix with '^', wrapping if the inner is not atomic. */
	public static String wrapForInverse(final String inner) {
		if (inner == null) {
			return "^()";
		}
		final String t = inner.trim();
		return "^" + (isAtomicPathText(t) ? t : ("(" + t + ")"));
	}

	/** Apply a quantifier to a path, wrapping only when the inner is not atomic. */
	public static String applyQuantifier(final String inner, final char quant) {
		if (inner == null) {
			return "()" + quant;
		}
		final String t = inner.trim();
		return (isAtomicPathText(t) ? t : ("(" + t + ")")) + quant;
	}

	/** Remove outer parens when they enclose the full string, otherwise return input unchanged. */
	public static String trimSingleOuterParens(String in) {
		String t = in;
		if (t.length() >= 2 && t.charAt(0) == '(' && t.charAt(t.length() - 1) == ')') {
			int depth = 0;
			for (int i = 0; i < t.length(); i++) {
				char c = t.charAt(i);
				if (c == '(') {
					depth++;
				} else if (c == ')') {
					depth--;
				}
				if (depth == 0 && i < t.length() - 1) {
					return in; // closes before the end -> not a single outer pair
				}
			}
			// single outer pair spans entire string
			return t.substring(1, t.length() - 1).trim();
		}
		return in;
	}

	/** Split by a separator at top level, ignoring nested parentheses. */
	public static List<String> splitTopLevel(String in, char sep) {
		ArrayList<String> out = new ArrayList<>();
		int depth = 0;
		int last = 0;
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
			} else if (c == sep && depth == 0) {
				out.add(in.substring(last, i));
				last = i + 1;
			}
		}
		// tail
		if (last <= in.length()) {
			out.add(in.substring(last));
		}
		return out;
	}
}
