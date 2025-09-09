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
 * Lift the scope marker from a path-generated UNION to the containing IrBGP.
 *
 * Pattern: a UNION with newScope=true whose branches all have newScope=false is indicative of a UNION created by
 * property-path alternation rather than an explicit "... } UNION { ...}" in the original query. In such cases the
 * surrounding group braces are expected even if later transforms fuse the UNION down to a single path triple.
 *
 * This transform sets the containing BGP's newScope flag to true when it contains exactly one such UNION. The flag is
 * preserved even if downstream transforms replace the UNION.
 */
public final class LiftPathUnionScopeToBgpTransform extends BaseTransform {

	private LiftPathUnionScopeToBgpTransform() {
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
				m = new IrGraph(g.getGraph(), apply(g.getWhere()), g.isNewScope());
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere()), o.isNewScope());
				no.setNewScope(o.isNewScope());
				m = no;
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
				// keep as is
			}
			out.add(m);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);

		// If this BGP consists of exactly one UNION that is path-generated (union.newScope=true
		// and all branch BGPs newScope=false), lift the scope to this BGP so braces are preserved
		// even if the UNION is later fused away.
		if (out.size() == 1 && out.get(0) instanceof IrUnion) {
			IrUnion u = (IrUnion) out.get(0);
			if (u.isNewScope()) {
				boolean allBranchesNonScoped = true;
				for (IrBGP b : u.getBranches()) {
					if (b != null && b.isNewScope()) {
						allBranchesNonScoped = false;
						break;
					}
				}
				if (allBranchesNonScoped) {
					res.setNewScope(true);
				}
			}
		}

		return res;
	}
}
