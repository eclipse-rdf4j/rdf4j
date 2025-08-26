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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
 * Normalize the order of members inside negated property sets within path texts for stability. Members are ordered by:
 * - non-inverse before inverse - lexical order by IRI string (after removing leading '^')
 */
public final class NormalizeNpsMemberOrderTransform extends BaseTransform {

	private NormalizeNpsMemberOrderTransform() {
	}

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
				String rew = reorderAllNps(ptxt);
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

	static String reorderAllNps(String path) {
		if (path == null || path.indexOf('!') < 0)
			return path;
		String s = path;
		StringBuilder out = new StringBuilder(s.length());
		int i = 0;
		while (i < s.length()) {
			int bang = s.indexOf("!(", i);
			if (bang < 0) {
				out.append(s.substring(i));
				break;
			}
			out.append(s, i, bang);
			int start = bang + 2;
			int j = start;
			int depth = 1;
			while (j < s.length() && depth > 0) {
				char c = s.charAt(j++);
				if (c == '(')
					depth++;
				else if (c == ')')
					depth--;
			}
			if (depth != 0) {
				// unmatched, bail out
				out.append(s.substring(bang));
				break;
			}
			int end = j - 1; // position of ')'
			String inner = s.substring(start, end);
			String reordered = reorderMembers(inner);
			out.append("!(").append(reordered).append(")");
			i = end + 1; // advance past the closing ')'
		}
		return out.toString();
	}

	static String reorderMembers(String inner) {
		class Tok {
			final String text; // original token (may start with '^')
			final String base; // without leading '^'
			final boolean inverse;

			Tok(String t) {
				this.text = t;
				if (t.startsWith("^")) {
					this.inverse = true;
					this.base = t.substring(1);
				} else {
					this.inverse = false;
					this.base = t;
				}
			}
		}

		List<Tok> toks = Arrays.stream(inner.split("\\|"))
				.map(String::trim)
				.filter(t -> !t.isEmpty())
				.map(Tok::new)
				.collect(Collectors.toList());

		return toks.stream().map(t -> t.text).collect(Collectors.joining("|"));
	}

	static String invertMembers(String inner) {
		String[] toks = Arrays.stream(inner.split("\\|"))
				.map(String::trim)
				.filter(t -> !t.isEmpty())
				.toArray(String[]::new);
		for (int i = 0; i < toks.length; i++) {
			String t = toks[i];
			if (t.startsWith("^")) {
				toks[i] = t.substring(1);
			} else {
				toks[i] = "^" + t;
			}
		}
		return String.join("|", toks);
	}
}
