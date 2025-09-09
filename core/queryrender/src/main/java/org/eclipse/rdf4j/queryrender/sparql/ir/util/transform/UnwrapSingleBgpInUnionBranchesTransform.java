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
 * Remove redundant single-child IrBGP layers inside UNION branches that do not carry new scope. This avoids introducing
 * an extra brace layer around branch content while preserving explicit grouping (newScope=true) and container
 * structure.
 */
public final class UnwrapSingleBgpInUnionBranchesTransform extends BaseTransform {

	private UnwrapSingleBgpInUnionBranchesTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrUnion) {
				m = unwrapUnionBranches((IrUnion) n);
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere()), g.isNewScope());
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				m = new IrOptional(apply(o.getWhere()), o.isNewScope());
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere()), mi.isNewScope());
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere()), s.isNewScope());
			} else if (n instanceof IrBGP) {
				m = apply((IrBGP) n);
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			}
			out.add(m);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	private static IrUnion unwrapUnionBranches(IrUnion u) {
		IrUnion u2 = new IrUnion(u.isNewScope());
		for (IrBGP b : u.getBranches()) {
			IrBGP cur = b;
			boolean branchScope = b.isNewScope();
			// Flatten exactly-one-child BGP wrappers inside UNION branches. If the inner BGP
			// carries newScope, lift that scope to the branch and drop the inner wrapper to
			// avoid printing double braces like "{ { ... } }".
			while (cur != null && cur.getLines().size() == 1 && cur.getLines().get(0) instanceof IrBGP) {
				IrBGP inner = (IrBGP) cur.getLines().get(0);
				branchScope = branchScope || inner.isNewScope();
				// Replace current with the inner's contents (flatten one level)
				IrBGP flattened = new IrBGP(false);
				for (IrNode ln : inner.getLines()) {
					flattened.add(ln);
				}
				cur = flattened;
			}
			// Reapply the accumulated scope to the flattened branch BGP
			cur.setNewScope(branchScope);
			u2.addBranch(cur);
		}
		return u2;
	}
}
