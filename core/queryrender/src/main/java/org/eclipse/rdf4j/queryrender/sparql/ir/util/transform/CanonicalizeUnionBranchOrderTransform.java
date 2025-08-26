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

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrTripleLike;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Canonicalize order of UNION branches when both branches are simple, to prefer the branch whose subject matches the
 * first projected variable. This helps stabilize streaming test outputs where textual equality matters.
 */
public final class CanonicalizeUnionBranchOrderTransform extends BaseTransform {
	private CanonicalizeUnionBranchOrderTransform() {
	}

	public static IrBGP apply(IrBGP bgp, IrSelect select) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrUnion) {
				m = reorderUnion((IrUnion) n, select);
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere(), select));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				m = new IrOptional(apply(o.getWhere(), select));
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere(), select));
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere(), select));
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			}
			out.add(m);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	private static IrNode reorderUnion(IrUnion u, IrSelect select) {
		// Recurse first into branches
		IrUnion u2 = new IrUnion();
		u2.setNewScope(u.isNewScope());
		for (IrBGP b : u.getBranches()) {
			u2.addBranch(apply(b, select));
		}
		if (u2.getBranches().size() != 2) {
			return u2;
		}
		String firstProj = null;
		if (select != null && !select.getProjection().isEmpty()) {
			firstProj = select.getProjection().get(0).getVarName();
		}
		if (firstProj == null || firstProj.isEmpty()) {
			return u2;
		}
		// Only reorder when both branches are single IrPathTriple (optionally GRAPH-wrapped)
		IrTripleLike tl0 = onlyTripleLike(u2.getBranches().get(0));
		IrTripleLike tl1 = onlyTripleLike(u2.getBranches().get(1));
		if (!(tl0 instanceof IrPathTriple) || !(tl1 instanceof IrPathTriple)) {
			return u2;
		}
		String p0 = ((IrPathTriple) tl0).getPathText();
		String p1 = ((IrPathTriple) tl1).getPathText();
		if (p0 == null || p1 == null || !p0.trim().startsWith("!(") || !p1.trim().startsWith("!(")) {
			return u2; // reorder only NPS cases
		}
		Var s0 = tl0.getSubject();
		Var s1 = tl1.getSubject();
		boolean b0Matches = firstProj.equals(s0.getName());
		boolean b1Matches = firstProj.equals(s1.getName());
		if (!b0Matches && b1Matches) {
			// swap branches
			IrUnion swapped = new IrUnion();
			swapped.setNewScope(u2.isNewScope());
			swapped.addBranch(u2.getBranches().get(1));
			swapped.addBranch(u2.getBranches().get(0));
			return swapped;
		}
		return u2;
	}

	private static IrTripleLike onlyTripleLike(IrBGP b) {
		if (b == null || b.getLines().size() != 1) {
			return null;
		}
		IrNode only = b.getLines().get(0);
		if (only instanceof IrGraph) {
			IrGraph g = (IrGraph) only;
			if (g.getWhere() == null || g.getWhere().getLines().size() != 1) {
				return null;
			}
			IrNode inner = g.getWhere().getLines().get(0);
			if (inner instanceof IrTripleLike) {
				return (IrTripleLike) inner;
			}
			return null;
		}
		if (only instanceof IrTripleLike) {
			return (IrTripleLike) only;
		}
		return null;
	}
}
