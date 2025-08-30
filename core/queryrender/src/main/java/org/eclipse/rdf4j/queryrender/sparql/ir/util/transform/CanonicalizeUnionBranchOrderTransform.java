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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
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
				IrOptional no = new IrOptional(apply(o.getWhere(), select));
				no.setNewScope(o.isNewScope());
				m = no;
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
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	private static IrNode reorderUnion(IrUnion u, IrSelect select) {
		// Recurse first into branches
		IrUnion u2 = new IrUnion();
		u2.setNewScope(u.isNewScope());
		for (IrBGP b : u.getBranches()) {
			u2.addBranch(apply(b, select));
		}
		// Keep original UNION branch order. Even though UNION is semantically commutative,
		// preserving source order stabilizes round-trip rendering and aligns with tests
		// that expect original text structure.
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
