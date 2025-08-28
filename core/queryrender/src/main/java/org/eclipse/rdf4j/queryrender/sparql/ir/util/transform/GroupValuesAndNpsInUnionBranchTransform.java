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

import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrValues;

/**
 * Stabilize rendering for UNION branches that combine a top-level VALUES clause with a negated property set path triple
 * by preserving an extra grouping block around the branch content.
 *
 * Rationale: path/NPS rewrites often eliminate an intermediate FILTER or JOIN that caused the RDF4J algebra to mark a
 * new variable scope. Tests expecting textual stability want the extra braces to persist (e.g., "{ { VALUES ... ?s
 * !(...) ?o . } } UNION { ... }").
 *
 * Heuristic (conservative): inside an explicit UNION branch (new scope), if the branch has a top-level IrValues and
 * also a top-level negated-path triple (IrPathTriple with path starting with '!' or '!^'), wrap the entire branch lines
 * in an inner IrBGP, resulting in double braces when printed by IrUnion.
 */
public final class GroupValuesAndNpsInUnionBranchTransform extends BaseTransform {

	private GroupValuesAndNpsInUnionBranchTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null)
			return null;

		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			if (n instanceof IrUnion) {
				out.add(groupUnionBranches((IrUnion) n));
			} else {
				// Recurse into nested containers, but only BGP-like children
				IrNode m = n.transformChildren(child -> {
					if (child instanceof IrBGP) {
						return apply((IrBGP) child);
					}
					return child;
				});
				out.add(m);
			}
		}

		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	private static IrUnion groupUnionBranches(IrUnion u) {
		IrUnion u2 = new IrUnion();
		u2.setNewScope(u.isNewScope());
		for (IrBGP b : u.getBranches()) {
			IrBGP toAdd = maybeWrapBranch(b, u.isNewScope());
			u2.addBranch(toAdd);
		}
		return u2;
	}

	// Only consider top-level lines in the branch for grouping to ensure idempotence.
	private static IrBGP maybeWrapBranch(IrBGP branch, boolean unionNewScope) {
		if (branch == null)
			return branch;

		boolean hasTopValues = false;
		boolean hasTopNegPath = false;
		int topCount = branch.getLines().size();
		int valuesCount = 0;
		int negPathCount = 0;

		for (IrNode ln : branch.getLines()) {
			if (ln instanceof IrValues) {
				hasTopValues = true;
				valuesCount++;
			} else if (ln instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) ln;
				String path = pt.getPathText();
				if (path != null) {
					String s = path.trim();
					if (s.startsWith("!") || s.startsWith("!^")) {
						hasTopNegPath = true;
						negPathCount++;
					}
				}
			} else if (ln instanceof IrGraph) {
				// Allow common shape: GRAPH { ?s !(...) ?o } at top-level
				IrGraph g = (IrGraph) ln;
				if (g.getWhere() != null && g.getWhere().getLines().size() == 1
						&& g.getWhere().getLines().get(0) instanceof IrPathTriple) {
					IrPathTriple pt = (IrPathTriple) g.getWhere().getLines().get(0);
					String path = pt.getPathText();
					if (path != null) {
						String s = path.trim();
						if (s.startsWith("!") || s.startsWith("!^")) {
							hasTopNegPath = true;
							negPathCount++;
						}
					}
				}
			}
		}

		// Only wrap for explicit UNION branches to mirror user grouping; avoid altering synthesized unions.
		// Guard for exact simple pattern: exactly two top-level lines: one VALUES and one NPS path (or GRAPH{NPS})
		if (unionNewScope && hasTopValues && hasTopNegPath && topCount == 2 && valuesCount == 1 && negPathCount == 1) {
			IrBGP inner = new IrBGP();
			for (IrNode ln : branch.getLines()) {
				inner.add(ln);
			}
			IrBGP wrapped = new IrBGP();
			wrapped.add(inner);
			return wrapped;
		}
		return branch;
	}
}
