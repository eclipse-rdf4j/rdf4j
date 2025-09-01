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
import java.util.regex.Pattern;

import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Simplify redundant parentheses in textual path expressions for readability and idempotence.
 *
 * Safe rewrites: - ((!(...))) -> (!(...)) - (((X))?) -> ((X)?)
 */
public final class SimplifyPathParensTransform extends BaseTransform {
	private SimplifyPathParensTransform() {
	}

	private static final Pattern DOUBLE_WRAP_NPS = Pattern.compile("\\(\\(\\(!\\([^()]*\\)\\)\\)\\)");
	private static final Pattern TRIPLE_WRAP_OPTIONAL = Pattern.compile("\\(\\(\\(([^()]+)\\)\\)\\?\\)\\)");
	// Reduce double parens around a simple segment: ((...)) -> (...)
	private static final Pattern DOUBLE_PARENS_SEGMENT = Pattern.compile("\\(\\(([^()]+)\\)\\)");
	// Drop parens around a simple sequence when immediately followed by '/': (a/b)/ -> a/b/
	private static final Pattern PARENS_AROUND_SEQ_BEFORE_SLASH = Pattern
			.compile("\\(([^()|]+/[^()|]+)\\)(?=/)");

	// Remove parentheses around an atomic segment (optionally with a single quantifier) e.g., (ex:p?) -> ex:p?
	private static final Pattern PARENS_AROUND_ATOMIC = Pattern
			.compile("\\(([^()|/]+[?+*]?)\\)");

	// Compact single-member negated property set: !(^p) -> !^p, !(p) -> !p
	private static final Pattern COMPACT_NPS_SINGLE_INVERSE = Pattern
			// !(^<iri>) or !(^prefixed)
			.compile("!\\(\\s*(\\^\\s*(?:<[^>]+>|[^()|/\\s]+))\\s*\\)");
	private static final Pattern COMPACT_NPS_SINGLE = Pattern
			// !(<iri>) or !(prefixed)
			.compile("!\\(\\s*((?:<[^>]+>|[^()|/\\s]+))\\s*\\)");

	// Remove parentheses around a simple negated token within an alternation: (!ex:p) -> !ex:p
	private static final Pattern COMPACT_PARENED_NEGATED_TOKEN = Pattern
			.compile("\\((!\\s*(?:<[^>]+>|[^()|/\\s]+))\\)");

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				String ptxt = pt.getPathText();
				String rew = simplify(ptxt);
				if (!rew.equals(ptxt)) {
					m = new IrPathTriple(pt.getSubject(), rew, pt.getObject(), pt.isNewScope());
				}
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere()), g.isNewScope());
			} else if (n instanceof IrBGP) {
				m = apply((IrBGP) n);
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere()), o.isNewScope());
				m = no;
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere()), mi.isNewScope());
			} else if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b));
				}
				m = u2;
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere()), s.isNewScope());
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			}
			out.add(m);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	public static String simplify(String s) {
		if (s == null) {
			return null;
		}
		String prev;
		String cur = s;
		int guard = 0;
		do {
			prev = cur;
			cur = DOUBLE_WRAP_NPS.matcher(cur).replaceAll("(!$1)");
			cur = TRIPLE_WRAP_OPTIONAL.matcher(cur).replaceAll("(($1)?)");
			cur = DOUBLE_PARENS_SEGMENT.matcher(cur).replaceAll("($1)");
			cur = PARENS_AROUND_SEQ_BEFORE_SLASH.matcher(cur).replaceAll("$1");
			cur = PARENS_AROUND_ATOMIC.matcher(cur).replaceAll("$1");
			// Compact a single-member NPS
			cur = COMPACT_NPS_SINGLE_INVERSE.matcher(cur).replaceAll("!$1");
			cur = COMPACT_NPS_SINGLE.matcher(cur).replaceAll("!$1");
			// Deduplicate alternation members inside parentheses when the group has no nested parentheses
			cur = dedupeParenedAlternations(cur);
			// Flatten nested alternation groups: ((a|b)|^a) -> (a|b|^a)
			cur = flattenNestedAlternationGroups(cur);
			// Remove parens around simple negated tokens to allow NPS normalization next
			cur = COMPACT_PARENED_NEGATED_TOKEN.matcher(cur).replaceAll("$1");
			// Normalize alternation of negated tokens (!a|!^b) into a proper NPS !(a|^b)
			cur = normalizeBangAlternationToNps(cur);
			// Normalize a paren group of negated tokens: (!a|!^b) -> !(a|^b)
			cur = normalizeParenBangAlternationGroups(cur);
			// Insert spaces around top-level alternations for readability
			cur = spaceTopLevelAlternations(cur);
		} while (!cur.equals(prev) && ++guard < 5);
		return cur;
	}

	// Flatten groups that contain nested alternation groups into a single-level alternation.
	private static String flattenNestedAlternationGroups(String s) {
		StringBuilder out = new StringBuilder(s.length());
		int i = 0;
		while (i < s.length()) {
			int open = s.indexOf('(', i);
			if (open < 0) {
				out.append(s.substring(i));
				break;
			}
			out.append(s, i, open);
			int j = open + 1;
			int depth = 1;
			while (j < s.length() && depth > 0) {
				char c = s.charAt(j++);
				if (c == '(') {
					depth++;
				} else if (c == ')') {
					depth--;
				}
			}
			if (depth != 0) {
				// Unbalanced; append rest
				out.append(s.substring(open));
				break;
			}
			int close = j - 1;
			String inner = s.substring(open + 1, close);
			// Recursively flatten inside first
			String innerFlat = flattenNestedAlternationGroups(inner);
			// Try to flatten one level of nested alternation groups at the top level of this group
			java.util.List<String> parts = splitTopLevel(innerFlat, '|');
			if (parts.size() >= 2) {
				java.util.ArrayList<String> members = new java.util.ArrayList<>();
				boolean changed = false;
				for (String seg : parts) {
					String u = seg.trim();
					String uw = trimSingleOuterParens(u);
					// If this part is a simple alternation group (no nested parens), flatten it
					if (uw.indexOf('(') < 0 && uw.indexOf(')') < 0 && uw.indexOf('|') >= 0) {
						for (String tok : uw.split("\\|")) {
							String t = tok.trim();
							if (!t.isEmpty()) {
								members.add(t);
							}
						}
						changed = true;
					} else {
						members.add(u);
					}
				}
				if (changed) {
					out.append('(').append(String.join("|", members)).append(')');
					i = close + 1;
					continue;
				}
			}
			// No flattening; keep recursively-flattened content
			out.append('(').append(innerFlat).append(')');
			i = close + 1;
		}
		return out.toString();
	}

	private static String normalizeBangAlternationToNps(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return s;
		}
		// Trim a single layer of wrapping parentheses if they enclose the full expression
		String tw = trimSingleOuterParens(t);
		// Split by top-level '|' to detect an alternation ignoring nested parentheses
		List<String> parts = splitTopLevel(tw, '|');
		if (parts.size() < 2) {
			return s;
		}
		ArrayList<String> members = new ArrayList<>();
		for (String seg : parts) {
			String u = seg.trim();
			// Allow parentheses around a simple negated token: (!ex:p) -> !ex:p
			u = trimSingleOuterParens(u);
			if (!u.startsWith("!")) {
				return s; // not all segments negated at top level
			}
			u = u.substring(1).trim();
			if (u.isEmpty()) {
				return s;
			}
			members.add(u);
		}
		return "!(" + String.join("|", members) + ")";
	}

	private static String trimSingleOuterParens(String in) {
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

	private static List<String> splitTopLevel(String in, char sep) {
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

	private static String dedupeParenedAlternations(String s) {
		StringBuilder out = new StringBuilder(s.length());
		int i = 0;
		while (i < s.length()) {
			int open = s.indexOf('(', i);
			if (open < 0) {
				out.append(s.substring(i));
				break;
			}
			out.append(s, i, open);
			int j = open + 1;
			int depth = 1;
			while (j < s.length() && depth > 0) {
				char c = s.charAt(j++);
				if (c == '(') {
					depth++;
				} else if (c == ')') {
					depth--;
				}
			}
			if (depth != 0) {
				// unmatched; append rest and break
				out.append(s.substring(open));
				break;
			}
			int close = j - 1;
			String inner = s.substring(open + 1, close);
			// Preserve original order and duplicates; do not deduplicate alternation members
			out.append('(').append(inner).append(')');
			i = close + 1;
		}
		return out.toString();
	}

	private static String normalizeParenBangAlternationGroups(String s) {
		StringBuilder out = new StringBuilder(s.length());
		int i = 0;
		while (i < s.length()) {
			int open = s.indexOf('(', i);
			if (open < 0) {
				out.append(s.substring(i));
				break;
			}
			out.append(s, i, open);
			int j = open + 1;
			int depth = 1;
			while (j < s.length() && depth > 0) {
				char c = s.charAt(j++);
				if (c == '(') {
					depth++;
				} else if (c == ')') {
					depth--;
				}
			}
			if (depth != 0) {
				// unmatched; append rest and break
				out.append(s.substring(open));
				break;
			}
			int close = j - 1;
			String inner = s.substring(open + 1, close).trim();

			// Recursively normalize nested groups first so that inner (!a|!^b) forms are handled
			String normalizedInner = normalizeParenBangAlternationGroups(inner);

			// Attempt top-level split on '|' inside this group, ignoring nested parens
			List<String> segs = splitTopLevel(normalizedInner, '|');
			if (segs.size() >= 2) {
				boolean allNeg = true;
				ArrayList<String> members = new ArrayList<>();
				for (String seg : segs) {
					String u = seg.trim();
					// Allow one layer of wrapping parens around the token
					u = trimSingleOuterParens(u).trim();
					if (!u.startsWith("!")) {
						allNeg = false;
						break;
					}
					u = u.substring(1).trim();
					if (u.isEmpty()) {
						allNeg = false;
						break;
					}
					members.add(u);
				}
				if (allNeg) {
					out.append("!(").append(String.join("|", members)).append(')');
					i = close + 1;
					continue;
				}
			}
			// No rewrite; keep group with recursively normalized content
			out.append('(').append(normalizedInner).append(')');
			i = close + 1;
		}
		return out.toString();
	}

	// Insert spaces around top-level '|' alternations for readability: a|b -> a | b
	@SuppressWarnings("unused")
	private static String spaceTopLevelAlternations(String s) {
		StringBuilder out = new StringBuilder(s.length() + 8);
		int depth = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '(') {
				depth++;
				out.append(c);
				continue;
			}
			if (c == ')') {
				depth--;
				out.append(c);
				continue;
			}
			if (c == '|' && depth == 0) {
				// ensure single spaces around
				if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') {
					out.append(' ');
				}
				out.append('|');
				int j = i + 1;
				if (j < s.length() && s.charAt(j) != ' ') {
					out.append(' ');
				}
				continue;
			}
			out.append(c);
		}
		return out.toString();
	}
}
