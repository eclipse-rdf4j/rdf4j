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
import java.util.Objects;
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

	private static final Pattern SIMPLE_ALT_GROUP = Pattern
			.compile("(?<!!)\\(\\s*([^()]+\\|[^()]+)\\s*\\)");

	private static final Pattern NPS_PARENS_SPACING = Pattern
			.compile("!\\(\\s*([^()]+?)\\s*\\)");

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
					IrPathTriple np = new IrPathTriple(pt.getSubject(), pt.getSubjectOverride(), rew, pt.getObject(),
							pt.getObjectOverride(), pt.getPathVars(), pt.isNewScope());
					m = np;
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
		return BaseTransform.bgpWithLines(bgp, out);
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
			// Style: ensure a single space just inside any parentheses before grouping
			cur = cur.replaceAll("\\((\\S)", "($1");
			cur = cur.replaceAll("(\\S)\\)", "$1)");
			// In a simple alternation group that mixes positive and negated tokens, compress the
			// negated tokens into a single NPS member: (ex:p|!a|!^b|ex:q) -> (ex:p|!(a|^b)|ex:q)
			cur = groupNegatedMembersInSimpleGroup(cur);
			// Style: add a space just inside simple alternation parentheses
			cur = SIMPLE_ALT_GROUP.matcher(cur).replaceAll("($1)");
			// (general parentheses spacing done earlier)
			// Finally: ensure no extra spaces inside NPS parentheses when used as a member
			cur = NPS_PARENS_SPACING.matcher(cur).replaceAll("!($1)");
		} while (!cur.equals(prev) && ++guard < 5);

		// If the entire path is a single parenthesized alternation group, remove the
		// outer parentheses: (a|^b) -> a|^b. This is safe only when the whole path
		// is that alternation (no top-level sequence operators outside).
		cur = unwrapWholeAlternationGroup(cur);
		return cur;
	}

	/** Remove outer parens when the entire expression is a single alternation group. */
	private static String unwrapWholeAlternationGroup(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		String inner = PathTextUtils.trimSingleOuterParens(t);
		if (Objects.equals(inner, t)) {
			return s; // not a single outer pair
		}
		// At this point, t is wrapped with a single pair of parentheses. Only unwrap when
		// the content is a pure top-level alternation (no top-level sequence '/')
		List<String> alts = PathTextUtils.splitTopLevel(inner, '|');
		if (alts.size() <= 1) {
			return s;
		}
		List<String> seqCheck = PathTextUtils.splitTopLevel(inner, '/');
		if (seqCheck.size() > 1) {
			return s; // contains a top-level sequence; need the outer parens
		}
		return inner;
	}

	// Compact sequences of !tokens inside a simple top-level alternation group into a single NPS member.
	private static String groupNegatedMembersInSimpleGroup(String s) {
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
				// unmatched parentheses; append rest and stop
				out.append(s.substring(open));
				break;
			}
			int close = j - 1;
			String inner = s.substring(open + 1, close);
			// Skip groups that contain nested parentheses
			if (inner.indexOf('(') >= 0 || inner.indexOf(')') >= 0) {
				out.append('(').append(inner).append(')');
				i = close + 1;
				continue;
			}
			String[] toks = inner.split("\\|");
			StringBuilder rebuilt = new StringBuilder(inner.length());
			StringBuilder neg = new StringBuilder();
			boolean insertedGroup = false;
			for (int k = 0; k < toks.length; k++) {
				String tok = toks[k].trim();
				if (tok.isEmpty()) {
					continue;
				}
				boolean isNeg = tok.startsWith("!") && (tok.length() == 1 || tok.charAt(1) != '(');
				if (isNeg) {
					String member = tok.substring(1).trim();
					if (neg.length() > 0) {
						neg.append('|');
					}
					neg.append(member);
					continue;
				}
				// flush any pending neg group before adding a positive token
				if (neg.length() > 0 && !insertedGroup) {
					if (rebuilt.length() > 0) {
						rebuilt.append('|');
					}
					rebuilt.append("!(").append(neg).append(")");
					neg.setLength(0);
					insertedGroup = true;
				}
				if (rebuilt.length() > 0) {
					rebuilt.append('|');
				}
				rebuilt.append(tok);
			}
			// flush at end if needed
			if (neg.length() > 0) {
				if (rebuilt.length() > 0) {
					rebuilt.append('|');
				}
				rebuilt.append("!(").append(neg).append(")");
			}
			out.append('(').append(rebuilt).append(')');
			i = close + 1;
		}
		return out.toString();
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
			List<String> parts = PathTextUtils.splitTopLevel(innerFlat, '|');
			if (parts.size() >= 2) {
				ArrayList<String> members = new ArrayList<>();
				boolean changed = false;
				for (String seg : parts) {
					String u = seg.trim();
					String uw = PathTextUtils.trimSingleOuterParens(u);
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
		String tw = PathTextUtils.trimSingleOuterParens(t);
		// Split by top-level '|' to detect an alternation ignoring nested parentheses
		List<String> parts = PathTextUtils.splitTopLevel(tw, '|');
		if (parts.size() < 2) {
			return s;
		}
		ArrayList<String> members = new ArrayList<>();
		for (String seg : parts) {
			String u = seg.trim();
			// Allow parentheses around a simple negated token: (!ex:p) -> !ex:p
			u = PathTextUtils.trimSingleOuterParens(u);
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

	// trimSingleOuterParens and splitTopLevel now centralized in PathTextUtils

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
			List<String> segs = PathTextUtils.splitTopLevel(normalizedInner, '|');
			if (segs.size() >= 2) {
				boolean allNeg = true;
				ArrayList<String> members = new ArrayList<>();
				for (String seg : segs) {
					String u = seg.trim();
					// Allow one layer of wrapping parens around the token
					u = PathTextUtils.trimSingleOuterParens(u).trim();
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

}
