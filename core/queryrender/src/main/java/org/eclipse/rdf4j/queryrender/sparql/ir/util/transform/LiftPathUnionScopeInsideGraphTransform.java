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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Inside GRAPH bodies, lift the scope marker from a path-generated UNION (branches all non-scoped) to the containing
 * BGP. This preserves brace grouping when the UNION is later fused into a single path triple.
 *
 * Strictly limited to GRAPH bodies; no other heuristics.
 */
public final class LiftPathUnionScopeInsideGraphTransform extends BaseTransform {

	private LiftPathUnionScopeInsideGraphTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				IrBGP inner = liftInGraph(g.getWhere());
				m = new IrGraph(g.getGraph(), inner, g.isNewScope());
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				m = new IrOptional(apply(o.getWhere()), o.isNewScope());
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere()), mi.isNewScope());
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere()), s.isNewScope());
			} else if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b));
				}
				m = u2;
			} else if (n instanceof IrBGP) {
				m = apply((IrBGP) n);
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			}
			out.add(m);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		return res;
	}

	private static IrBGP liftInGraph(IrBGP where) {
		if (where == null) {
			return null;
		}
		// If the GRAPH body consists of exactly one UNION whose branches all have newScope=false,
		// set the body's newScope to true so braces are preserved post-fuse.
		if (where.getLines().size() == 1 && where.getLines().get(0) instanceof IrUnion) {
			IrUnion u = (IrUnion) where.getLines().get(0);
			boolean allBranchesNonScoped = true;
			for (IrBGP b : u.getBranches()) {
				if (b != null && b.isNewScope()) {
					allBranchesNonScoped = false;
					break;
				}
			}
			if (allBranchesNonScoped) {
				IrBGP res = new IrBGP(true);
				res.add(u);
				return res;
			}
		}
		return where;
	}
}
