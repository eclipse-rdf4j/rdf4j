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

import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;

/**
 * Normalize FILTER conditions by reconstructing simple NOT IN expressions from top-level conjunctions of inequalities
 * against the same variable, e.g., ( ?p != <a> && ?p != <b> ) -> ?p NOT IN (<a>, <b>).
 *
 * This runs on textual IrFilter conditions and does not alter EXISTS bodies or nested structures.
 */
public final class NormalizeFilterNotInTransform extends BaseTransform {

	private NormalizeFilterNotInTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrFilter) {
				IrFilter f = (IrFilter) n;
				if (f.getBody() == null && f.getConditionText() != null) {
					String rewritten = tryRewriteNotIn(f.getConditionText());
					if (rewritten != null) {
						IrFilter nf = new IrFilter(rewritten, f.isNewScope());
						m = nf;
					}
				}
			}

			// Recurse into containers via shared helper
			m = BaseTransform.rewriteContainers(m, child -> NormalizeFilterNotInTransform.apply(child, r));
			out.add(m);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	// Attempt to reconstruct "?v NOT IN (a, b, ...)" from a top-level conjunction of "?v != item" terms.
	// Only applies when two or more distinct items are found; otherwise returns null.
	static String tryRewriteNotIn(String cond) {
		if (cond == null) {
			return null;
		}
		String s = cond.trim();
		List<String> parts = splitTopLevelAnd(s);
		if (parts.size() < 2) {
			return null; // not a conjunction
		}
		String varName = null;
		List<String> items = new ArrayList<>();
		for (String p : parts) {
			String t = stripOuterParens(p.trim());
			// match ?v != item or item != ?v
			Match m = matchInequality(t);
			if (m == null) {
				return null; // unsupported term in conjunction
			}
			if (varName == null) {
				varName = m.var;
			} else if (!varName.equals(m.var)) {
				return null; // different variables involved
			}
			items.add(m.item);
		}
		if (items.size() < 2 || varName == null) {
			return null; // do not rewrite a single inequality
		}
		return "?" + varName + " NOT IN (" + String.join(", ", items) + ")";
	}

	private static final class Match {
		final String var;
		final String item;

		Match(String var, String item) {
			this.var = var;
			this.item = item;
		}
	}

	private static Match matchInequality(String t) {
		int idx = t.indexOf("!=");
		if (idx < 0) {
			return null;
		}
		String left = t.substring(0, idx).trim();
		String right = t.substring(idx + 2).trim();
		// Allow optional outer parentheses around left/right
		left = stripOuterParens(left);
		right = stripOuterParens(right);
		if (left.startsWith("?")) {
			String v = left.substring(1);
			if (!v.isEmpty() && isVarName(v) && isItemToken(right)) {
				return new Match(v, right);
			}
		}
		if (right.startsWith("?")) {
			String v = right.substring(1);
			if (!v.isEmpty() && isVarName(v) && isItemToken(left)) {
				return new Match(v, left);
			}
		}
		return null;
	}

	private static boolean isVarName(String s) {
		char c0 = s.isEmpty() ? '\0' : s.charAt(0);
		if (!(Character.isLetter(c0) || c0 == '_')) {
			return false;
		}
		for (int i = 1; i < s.length(); i++) {
			char c = s.charAt(i);
			if (!(Character.isLetterOrDigit(c) || c == '_')) {
				return false;
			}
		}
		return true;
	}

	// Token acceptance for NOT IN members roughly matching renderExpr/renderValue output: angle-IRI, prefixed name,
	// numeric/boolean constants, or quoted literal with optional @lang or ^^datatype suffix.
	private static boolean isItemToken(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		// Angle-bracketed IRI
		if (s.charAt(0) == '<') {
			return s.endsWith(">");
		}
		// Quoted literal with optional suffix: @lang or ^^<iri> or ^^prefix:name
		if (s.charAt(0) == '"') {
			int i = 1;
			boolean esc = false;
			boolean closed = false;
			while (i < s.length()) {
				char c = s.charAt(i++);
				if (esc) {
					esc = false;
				} else if (c == '\\') {
					esc = true;
				} else if (c == '"') {
					closed = true;
					break;
				}
			}
			if (!closed) {
				return false;
			}
			// Accept no suffix
			if (i == s.length()) {
				return true;
			}
			// Accept @lang
			if (s.charAt(i) == '@') {
				String lang = s.substring(i + 1);
				return !lang.isEmpty() && lang.matches("[A-Za-z0-9-]+");
			}
			// Accept ^^<iri> or ^^prefix:name
			if (i + 1 < s.length() && s.charAt(i) == '^' && s.charAt(i + 1) == '^') {
				String rest = s.substring(i + 2);
				if (rest.startsWith("<") && rest.endsWith(">")) {
					return true;
				}
				// prefixed name
				return rest.matches("[A-Za-z_][\\w.-]*:[^\\s,()]+");
			}
			return false;
		}
		// Booleans
		if ("true".equals(s) || "false".equals(s)) {
			return true;
		}
		// Numeric literals (integer/decimal/double)
		if (s.matches("[+-]?((\\d+\\.\\d*)|(\\.\\d+)|(\\d+))(?:[eE][+-]?\\d+)?")) {
			return true;
		}
		// Prefixed name
		if (s.matches("[A-Za-z_][\\w.-]*:[^\\s,()]+")) {
			return true;
		}
		// Fallback: reject tokens containing whitespace or parentheses
		return !s.contains(" ") && !s.contains(")") && !s.contains("(");
	}

	private static String stripOuterParens(String x) {
		String t = x;
		while (t.length() >= 2 && t.charAt(0) == '(' && t.charAt(t.length() - 1) == ')') {
			int depth = 0;
			boolean ok = true;
			for (int i = 0; i < t.length(); i++) {
				char c = t.charAt(i);
				if (c == '(') {
					depth++;
				} else if (c == ')') {
					depth--;
				}
				if (depth == 0 && i < t.length() - 1) {
					ok = false;
					break;
				}
			}
			if (!ok) {
				break;
			}
			t = t.substring(1, t.length() - 1).trim();
		}
		return t;
	}

	private static List<String> splitTopLevelAnd(String s) {
		List<String> parts = new ArrayList<>();
		int depth = 0;
		boolean inStr = false;
		boolean esc = false;
		int last = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (inStr) {
				if (esc) {
					esc = false;
				} else if (c == '\\') {
					esc = true;
				} else if (c == '"') {
					inStr = false;
				}
				continue;
			}
			if (c == '"') {
				inStr = true;
				continue;
			}
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
			} else if (c == '&' && depth == 0) {
				// lookahead for '&&'
				if (i + 1 < s.length() && s.charAt(i + 1) == '&') {
					parts.add(s.substring(last, i).trim());
					i++; // skip second '&'
					last = i + 1;
				}
			}
		}
		parts.add(s.substring(last).trim());
		return parts;
	}
}
