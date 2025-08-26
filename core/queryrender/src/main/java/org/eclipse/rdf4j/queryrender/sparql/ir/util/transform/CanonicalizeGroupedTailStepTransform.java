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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Normalize grouping of a final tail step like "/foaf:name" so that it appears outside the top-level grouped PT/PT
 * fusion instead of inside the right-hand side group. This rewrites patterns of the form:
 *
 * (?LEFT)/((?RIGHT/tail)) -> ((?LEFT)/(?RIGHT))/tail
 *
 * It is a best-effort string-level fix applied late in the pipeline to match expected canonical output.
 */
public final class CanonicalizeGroupedTailStepTransform extends BaseTransform {

	private CanonicalizeGroupedTailStepTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				String ptxt = pt.getPathText();
				// First: move a final tail step out of the right-hand group when safe:
				// (LEFT)/((RIGHT/tail)) -> ((LEFT)/(RIGHT))/tail
				String afterTail = rewriteGroupedTail(ptxt);
				// Second: normalize split-middle grouping like ((L)/(M))/((R)) -> ((L)/(M/(R)))
				String rew = rewriteFuseSplitMiddle(afterTail);
				if (!rew.equals(ptxt)) {
					m = new IrPathTriple(pt.getSubject(), rew, pt.getObject());
				}
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere(), r));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				m = new IrOptional(apply(o.getWhere(), r));
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere(), r));
			} else if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion();
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b, r));
				}
				m = u2;
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere(), r));
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			}
			out.add(m);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	/**
	 * Rewrite a path text of the form "((LEFT)/(MID))/((RIGHT))" into "((LEFT)/(MID/(RIGHT)))". MID is assumed to be a
	 * simple step or small group like "^ex:d".
	 */
	static String rewriteFuseSplitMiddle(String path) {
		if (path == null) {
			return null;
		}
		String s = path.trim();
		if (!s.startsWith("((")) {
			return s;
		}
		int first = s.indexOf(")/(");
		if (first <= 0) {
			return s;
		}
		// After first delim, expect MID then ")/(" then RIGHT then ')'
		String afterFirst = s.substring(first + 3);
		int second = afterFirst.indexOf(")/(");
		if (second <= 0) {
			return s;
		}
		String left = s.substring(2, first); // drop initial "(("
		String mid = afterFirst.substring(0, second);
		String rightWithParens = afterFirst.substring(second + 2); // starts with '('
		if (rightWithParens.length() < 3 || rightWithParens.charAt(0) != '('
				|| rightWithParens.charAt(rightWithParens.length() - 1) != ')') {
			return s;
		}
		String right = rightWithParens.substring(1, rightWithParens.length() - 1);
		// Build fused: ((LEFT)/(MID/(RIGHT)))
		return "((" + left + ")/(" + mid + "/(" + right + ")))";
	}

	/**
	 * Rewrite a path text of the form "(LEFT)/((RIGHT/tail))" into "((LEFT)/(RIGHT))/tail". Returns the original text
	 * when no safe rewrite is detected.
	 */
	static String rewriteGroupedTail(String path) {
		if (path == null) {
			return null;
		}
		String s = path.trim();
		// Require pattern starting with '(' and containing ")/(" and ending with ')'
		int sep = s.indexOf(")/(");
		if (sep <= 0 || s.charAt(0) != '(' || s.charAt(s.length() - 1) != ')') {
			return s;
		}
		String left = s.substring(1, sep); // drop leading '('
		String rightWithParens = s.substring(sep + 2); // starts with "("
		if (rightWithParens.length() < 3 || rightWithParens.charAt(0) != '('
				|| rightWithParens.charAt(rightWithParens.length() - 1) != ')') {
			return s;
		}
		String right = rightWithParens.substring(1, rightWithParens.length() - 1);
		int lastSlash = right.lastIndexOf('/');
		if (lastSlash < 0) {
			return s; // nothing to peel off
		}
		String base = right.substring(0, lastSlash);
		String tail = right.substring(lastSlash + 1);
		// Tail must look like a simple step (IRI or ^IRI) without inner alternation or quantifier
		if (tail.isEmpty() || tail.contains("|") || tail.contains("(") || tail.contains(")") ||
				tail.endsWith("?") || tail.endsWith("*") || tail.endsWith("+")) {
			return s;
		}
		// Rebuild: ((LEFT)/(BASE))/TAIL
		return "((" + left + ")/(" + base + "))/" + tail;
	}
}
