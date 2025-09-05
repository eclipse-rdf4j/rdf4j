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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Rewrite a UNION whose multiple branches are each a single GRAPH block with the same graph reference into a single
 * GRAPH whose body contains a UNION of the inner branch bodies. This preserves user-intended grouping like "GRAPH ?g {
 * { A } UNION { B } }" instead of rendering as "{ GRAPH ?g { A } } UNION { GRAPH ?g { B } }".
 *
 * Safety: - Only rewrites when two or more UNION branches are single GRAPHs with identical graph refs. - Preserves
 * branch order by collapsing the first encountered group into a single GRAPH and skipping subsequent branches belonging
 * to the same group.
 */
public final class GroupUnionOfSameGraphBranchesTransform extends BaseTransform {

	private GroupUnionOfSameGraphBranchesTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			if (n instanceof IrUnion) {
				out.add(rewriteUnion((IrUnion) n));
				continue;
			}
			// Recurse into containers
			IrNode m = n.transformChildren(child -> {
				if (child instanceof IrBGP) {
					return apply((IrBGP) child);
				}
				return child;
			});
			out.add(m);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	private static IrNode rewriteUnion(IrUnion u) {
		if (!u.isNewScope()) {
			return u;
		}

		// Build groups of branch indexes by common graph ref when the branch is exactly one GRAPH node
		final int n = u.getBranches().size();
		final Map<String, List<Integer>> byKey = new HashMap<>();
		final Map<String, Var> keyVar = new HashMap<>();
		for (int i = 0; i < n; i++) {
			IrBGP b = u.getBranches().get(i);
			if (b.getLines().size() != 1 || !(b.getLines().get(0) instanceof IrGraph)) {
				continue;
			}
			IrGraph g = (IrGraph) b.getLines().get(0);
			Var v = g.getGraph();
			String key = graphKey(v);
			byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
			keyVar.putIfAbsent(key, v);
		}

		// If no group has >= 2 entries, return union as-is but recurse branches
		boolean hasAnyGroup = byKey.values().stream().anyMatch(list -> list.size() >= 2);
		if (!hasAnyGroup) {
			IrUnion u2 = new IrUnion(u.isNewScope());
			for (IrBGP b : u.getBranches()) {
				u2.addBranch(apply(b));
			}
			u2.setNewScope(u.isNewScope());
			return u2;
		}

		// Collapse groups while preserving order
		Set<Integer> consumed = new HashSet<>();
		IrUnion u2 = new IrUnion(u.isNewScope());
		for (int i = 0; i < n; i++) {
			if (consumed.contains(i)) {
				continue;
			}
			IrBGP branch = u.getBranches().get(i);
			if (branch.getLines().size() == 1 && branch.getLines().get(0) instanceof IrGraph) {
				IrGraph g = (IrGraph) branch.getLines().get(0);
				String key = graphKey(g.getGraph());
				List<Integer> group = byKey.get(key);
				if (group != null && group.size() >= 2) {
					// Build inner UNION of the GRAPH bodies for all branches in the group
					IrUnion inner = new IrUnion(u.isNewScope());
					for (int idx : group) {
						consumed.add(idx);
						IrBGP irBGP = u.getBranches().get(idx);
						IrBGP body = ((IrGraph) irBGP.getLines().get(0)).getWhere();
						if (irBGP.isNewScope()) {
							// the outer irBGP had a new scope, instead of playing around with the body we just wrap it
							// in an IrBGP which represents this new scope
							body = new IrBGP(body, false);
						}
						// Recurse inside the body before grouping and preserve explicit grouping
						inner.addBranch(apply(body));
					}
					// Wrap union inside the GRAPH as a single-line BGP
					IrBGP graphWhere = new IrBGP(false);
					graphWhere.add(inner);
					IrGraph mergedGraph = new IrGraph(keyVar.get(key), graphWhere, g.isNewScope());
					IrBGP newBranch = new IrBGP(false);
					newBranch.add(mergedGraph);
					u2.addBranch(newBranch);
					continue;
				}
			}
			// Default: keep branch (with recursion inside)
			u2.addBranch(apply(branch));
		}
		u2.setNewScope(u.isNewScope());

		// If the rewrite collapsed the UNION to a single branch (e.g., both branches
		// were GRAPH blocks with the same graph ref), drop the outer UNION entirely
		// and return the single branch BGP. This avoids leaving behind a degenerate
		// UNION wrapper that would introduce extra grouping braces at print time.
		if (u2.getBranches().size() == 1) {
			IrBGP only = u2.getBranches().get(0);
			if (only.getLines().size() == 1) {
				return only.getLines().get(0); // return the single GRAPH directly (no extra braces)
			}
			return only;
		}

		return u2;
	}

	private static String graphKey(Var v) {
		if (v == null) {
			return "<null>";
		}
		if (v.hasValue() && v.getValue() != null) {
			return "val:" + v.getValue().stringValue();
		}
		return "var:" + String.valueOf(v.getName());
	}
}
