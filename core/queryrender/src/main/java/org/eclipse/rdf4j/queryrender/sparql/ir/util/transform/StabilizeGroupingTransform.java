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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrExists;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPropertyList;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrValues;

/**
 * Stabilize brace grouping for readability and textual idempotence by re‑introducing a lightweight inner grouping block
 * when a container (top-level WHERE, GRAPH, OPTIONAL, SERVICE, UNION branch) mixes constructs that commonly appear
 * grouped in the original algebra (e.g., VALUES with triples or UNION, a triple with an OPTIONAL, or FILTER EXISTS with
 * a neighboring triple). This is purely presentational: it does not change algebraic semantics.
 *
 * Heuristics (conservative): - Only wrap when there are at least two top-level lines and the pattern includes one of
 * the following mixes: - VALUES together with (triple-like | UNION | OPTIONAL | FILTER EXISTS | GRAPH | SERVICE) - A
 * triple-like together with OPTIONAL - FILTER EXISTS together with (triple-like | VALUES) - UNION together with any
 * sibling line (wrap the union plus sibling as a grouped block) - Skip if already wrapped (i.e., the block consists of
 * a single nested IrBGP line). - For UNION branches, only apply to explicit user UNIONs (newScope=true) to avoid
 * interfering with synthesized unions created by path rewrites.
 */
public final class StabilizeGroupingTransform extends BaseTransform {

	private StabilizeGroupingTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null)
			return null;

		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				IrBGP inner = apply(g.getWhere());
				inner = maybeWrapValuesMix(inner);
				out.add(new IrGraph(g.getGraph(), inner));
				continue;
			}
			if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrBGP inner = apply(o.getWhere());
				inner = maybeWrapValuesMix(inner);
				out.add(new IrOptional(inner));
				continue;
			}
			if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				IrBGP inner = apply(mi.getWhere());
				// Do not alter MINUS grouping; keep as-is to avoid blocking fusions
				out.add(new IrMinus(inner));
				continue;
			}
			if (n instanceof IrService) {
				IrService s = (IrService) n;
				IrBGP inner = apply(s.getWhere());
				inner = maybeWrapValuesMix(inner);
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), inner));
				continue;
			}
			if (n instanceof IrFilter) {
				IrFilter f = (IrFilter) n;
				IrNode body = f.getBody();
				if (body instanceof IrExists) {
					IrExists ex = (IrExists) body;
					IrBGP inner = apply(ex.getWhere());
					// Inside EXISTS: if a simple triple/path is paired with another EXISTS or with VALUES, group them
					if (qualifiesForExistsInnerGrouping(inner)) {
						inner = wrap(inner);
					}
					IrFilter nf = new IrFilter(new IrExists(inner, ex.isNewScope()));
					nf.setNewScope(f.isNewScope());
					out.add(nf);
					continue;
				}
				// Otherwise, keep as-is
			}
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion();
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					IrBGP bx = apply(b);
					// Only add grouping inside explicit user UNIONs
					if (u.isNewScope()) {
						bx = maybeWrapValuesMix(bx);
					}
					u2.addBranch(bx);
				}
				out.add(u2);
				continue;
			}
			// Do not generically wrap nested IrBGPs; only targeted containers above
			// Keep other lines as-is
			out.add(n);
		}

		IrBGP res = new IrBGP();
		out.forEach(res::add);
		// Do not wrap at top-level; only targeted containers/union branches
		return res;
	}

	private static IrBGP maybeWrapValuesMix(IrBGP w) {
		if (w == null)
			return null;
		if (!qualifiesForValuesMixGrouping(w))
			return w;
		// Already wrapped? (single IrBGP line)
		if (w.getLines().size() == 1 && w.getLines().get(0) instanceof IrBGP) {
			return w;
		}
		IrBGP inner = new IrBGP();
		for (IrNode ln : w.getLines()) {
			inner.add(ln);
		}
		IrBGP wrapped = new IrBGP();
		wrapped.add(inner);
		return wrapped;
	}

	private static IrBGP wrap(IrBGP w) {
		if (w == null)
			return null;
		if (w.getLines().size() == 1 && w.getLines().get(0) instanceof IrBGP)
			return w;
		IrBGP inner = new IrBGP();
		for (IrNode ln : w.getLines())
			inner.add(ln);
		IrBGP wrapped = new IrBGP();
		wrapped.add(inner);
		return wrapped;
	}

	private static boolean qualifiesForValuesMixGrouping(IrBGP w) {
		if (w == null)
			return false;
		final List<IrNode> ls = w.getLines();
		if (ls.size() < 2)
			return false;

		boolean hasValues = false;
		boolean hasFilterExists = false;
		boolean hasNegatedPath = false;
		boolean hasUnionOrGraphService = false;

		for (IrNode ln : ls) {
			if (ln instanceof IrValues)
				hasValues = true;
			else if (ln instanceof IrUnion || ln instanceof IrGraph || ln instanceof IrService)
				hasUnionOrGraphService = true;
			else if (ln instanceof IrFilter) {
				IrFilter f = (IrFilter) ln;
				if (f.getBody() instanceof IrExists)
					hasFilterExists = true;
			}
			if (ln instanceof IrPathTriple) {
				String path = ((IrPathTriple) ln).getPathText();
				if (path != null) {
					String s = path.trim();
					if (s.startsWith("!") || s.startsWith("!^")) {
						hasNegatedPath = true;
					}
				}
			} else if (ln instanceof IrGraph) {
				IrGraph g = (IrGraph) ln;
				if (g.getWhere() != null && g.getWhere().getLines().size() == 1
						&& g.getWhere().getLines().get(0) instanceof IrPathTriple) {
					String path = ((IrPathTriple) g.getWhere().getLines().get(0)).getPathText();
					if (path != null) {
						String s = path.trim();
						if (s.startsWith("!") || s.startsWith("!^")) {
							hasNegatedPath = true;
						}
					}
				}
			}
		}

		if (hasValues && (hasNegatedPath || hasFilterExists || hasUnionOrGraphService))
			return true;
		if (hasFilterExists && hasValues)
			return true;

		return false;
	}

	private static boolean qualifiesForExistsInnerGrouping(IrBGP w) {
		if (w == null)
			return false;
		final List<IrNode> ls = w.getLines();
		if (ls.size() < 2)
			return false;
		boolean hasTripleLike = false;
		boolean hasNestedExists = false;
		boolean hasValues = false;
		for (IrNode ln : ls) {
			if (ln instanceof IrStatementPattern || ln instanceof IrPathTriple || ln instanceof IrPropertyList) {
				hasTripleLike = true;
			} else if (ln instanceof IrFilter) {
				IrFilter f = (IrFilter) ln;
				if (f.getBody() instanceof IrExists)
					hasNestedExists = true;
			} else if (ln instanceof IrValues) {
				hasValues = true;
			}
		}
		return hasTripleLike && (hasNestedExists || hasValues);
	}
}
