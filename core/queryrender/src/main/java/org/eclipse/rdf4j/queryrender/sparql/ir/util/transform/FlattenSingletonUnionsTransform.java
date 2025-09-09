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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Remove UNION nodes that have a single branch, effectively inlining their content. This keeps the IR compact and
 * avoids printing unnecessary braces/UNION keywords.
 *
 * Safety: - Does not flatten inside OPTIONAL bodies to avoid subtle scope/precedence shifts when later transforms
 * reorder filters and optionals. - Preserves explicit UNIONs with new variable scope (not constructed by transforms),
 * even if they degenerate to a single branch, to respect original user structure.
 */
public final class FlattenSingletonUnionsTransform extends BaseTransform {
	private FlattenSingletonUnionsTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			// Recurse first (but do not flatten inside OPTIONAL bodies)
			n = n.transformChildren(child -> {
				if (child instanceof IrOptional) {
					return child; // skip
				}
				if (child instanceof IrBGP) {
					return apply((IrBGP) child);
				}
				return child;
			});
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				// Detect unions that originate from property-path alternation: they often carry
				// newScope=true on the UNION node but have branches with newScope=false. In that
				// case, when only one branch remains, we can safely flatten the UNION node as it
				// is not an explicit user-authored UNION.
				boolean branchesAllNonScoped = true;
				for (IrBGP b : u.getBranches()) {
					if (b != null && b.isNewScope()) {
						branchesAllNonScoped = false;
						break;
					}
				}
				// Preserve explicit UNIONs (newScope=true) unless they are clearly path-generated
				// and have collapsed to a single branch.
				if (u.isNewScope() && !(branchesAllNonScoped && u.getBranches().size() == 1)) {
					out.add(u);
					continue;
				}
				if (u.getBranches().size() == 1) {
					IrBGP only = u.getBranches().get(0);
					out.addAll(only.getLines());
					continue;
				}
			}
			out.add(n);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}
}
