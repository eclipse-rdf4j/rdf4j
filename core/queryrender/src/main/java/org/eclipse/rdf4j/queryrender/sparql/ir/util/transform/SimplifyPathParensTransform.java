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

	// Compact single-member negated property set: !(^p) -> !^p, !(p) -> !p
	private static final Pattern COMPACT_NPS_SINGLE_INVERSE = Pattern
			// !(^<iri>) or !(^prefixed)
			.compile("!\\(\\s*(\\^\\s*(?:<[^>]+>|[^()|/\\s]+))\\s*\\)");
	private static final Pattern COMPACT_NPS_SINGLE = Pattern
			// !(<iri>) or !(prefixed)
			.compile("!\\(\\s*((?:<[^>]+>|[^()|/\\s]+))\\s*\\)");

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null)
			return null;
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				String ptxt = pt.getPathText();
				String rew = simplify(ptxt);
				if (!rew.equals(ptxt)) {
					m = new IrPathTriple(pt.getSubject(), rew, pt.getObject());
				}
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere()));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				m = new IrOptional(apply(o.getWhere()));
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere()));
			} else if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion();
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b));
				}
				m = u2;
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere()));
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			}
			out.add(m);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	static String simplify(String s) {
		if (s == null)
			return null;
		String prev;
		String cur = s;
		int guard = 0;
		do {
			prev = cur;
			cur = DOUBLE_WRAP_NPS.matcher(cur).replaceAll("(!$1)");
			cur = TRIPLE_WRAP_OPTIONAL.matcher(cur).replaceAll("(($1)?)");
			cur = DOUBLE_PARENS_SEGMENT.matcher(cur).replaceAll("($1)");
			cur = PARENS_AROUND_SEQ_BEFORE_SLASH.matcher(cur).replaceAll("$1");
			// Compact a single-member NPS
			cur = COMPACT_NPS_SINGLE_INVERSE.matcher(cur).replaceAll("!$1");
			cur = COMPACT_NPS_SINGLE.matcher(cur).replaceAll("!$1");
			// Deduplicate alternation members inside parentheses when the group has no nested parentheses
			cur = dedupeParenedAlternations(cur);
		} while (!cur.equals(prev) && ++guard < 5);
		return cur;
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
				if (c == '(')
					depth++;
				else if (c == ')')
					depth--;
			}
			if (depth != 0) {
				// unmatched; append rest and break
				out.append(s.substring(open));
				break;
			}
			int close = j - 1;
			String inner = s.substring(open + 1, close);
			// Only dedupe when there are '|' and no nested parens inside the group (safety)
			if (inner.indexOf('|') >= 0 && inner.indexOf('(') < 0 && inner.indexOf(')') < 0) {
				java.util.LinkedHashSet<String> uniq = new java.util.LinkedHashSet<>();
				for (String tok : inner.split("\\|")) {
					String t = tok.trim();
					if (!t.isEmpty())
						uniq.add(t);
				}
				String rebuilt = String.join("|", uniq);
				out.append('(').append(rebuilt).append(')');
			} else {
				out.append('(').append(inner).append(')');
			}
			i = close + 1;
		}
		return out.toString();
	}
}
