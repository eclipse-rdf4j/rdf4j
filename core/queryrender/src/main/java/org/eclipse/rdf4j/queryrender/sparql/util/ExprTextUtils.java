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
package org.eclipse.rdf4j.queryrender.sparql.util;

/** Helpers for adding/removing parentheses around expression text. */
public final class ExprTextUtils {
	private ExprTextUtils() {
	}

	public static String stripRedundantOuterParens(final String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		if (t.length() >= 2 && t.charAt(0) == '(' && t.charAt(t.length() - 1) == ')') {
			int depth = 0;
			for (int i = 0; i < t.length(); i++) {
				char ch = t.charAt(i);
				if (ch == '(') {
					depth++;
				} else if (ch == ')') {
					depth--;
				}
				if (depth == 0 && i < t.length() - 1) {
					return t; // outer pair doesn't span full string
				}
			}
			return t.substring(1, t.length() - 1).trim();
		}
		return t;
	}

	/**
	 * Simple parentheses wrapper used in a few contexts (e.g., HAVING NOT): if the string is non-empty and does not
	 * start with '(', wrap it.
	 */
	public static String parenthesizeIfNeededSimple(String s) {
		if (s == null) {
			return "()";
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return "()";
		}
		if (t.charAt(0) == '(') {
			return t;
		}
		return "(" + t + ")";
	}

	/**
	 * Parenthesize an expression only if the current string is not already wrapped by a single outer pair.
	 */
	public static String parenthesizeIfNeededExpr(final String expr) {
		if (expr == null) {
			return "()";
		}
		final String t = expr.trim();
		if (t.isEmpty()) {
			return "()";
		}
		if (t.charAt(0) == '(' && t.charAt(t.length() - 1) == ')') {
			int depth = 0;
			boolean spans = true;
			for (int i = 0; i < t.length(); i++) {
				char ch = t.charAt(i);
				if (ch == '(') {
					depth++;
				} else if (ch == ')') {
					depth--;
				}
				if (depth == 0 && i < t.length() - 1) {
					spans = false;
					break;
				}
			}
			if (spans) {
				return t;
			}
		}
		return "(" + t + ")";
	}
}
