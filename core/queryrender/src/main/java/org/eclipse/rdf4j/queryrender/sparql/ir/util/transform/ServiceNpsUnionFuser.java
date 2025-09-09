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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Helper to fuse a UNION of two bare NPS path triples in a SERVICE body into a single negated property set triple.
 *
 * Shape fused: - { { ?s !ex:p ?o } UNION { ?o !ex:q ?s } } => { ?s !(ex:p|^ex:q) ?o } - { { ?s !ex:p ?o } UNION { ?s
 * !ex:q ?o } } => { ?s !(ex:p|ex:q) ?o }
 */
public final class ServiceNpsUnionFuser {

	private ServiceNpsUnionFuser() {
	}

	public static IrBGP fuse(IrBGP bgp) {
		if (bgp == null || bgp.getLines().isEmpty()) {
			return bgp;
		}

		// Exact-body UNION case
		if (bgp.getLines().size() == 1 && bgp.getLines().get(0) instanceof IrUnion) {
			IrNode fused = tryFuseUnion((IrUnion) bgp.getLines().get(0));
			if ((fused instanceof IrPathTriple || fused instanceof IrGraph)) {
				IrBGP nw = new IrBGP(bgp.isNewScope());
				nw.add(fused);
				return nw;
			}
			if (fused instanceof IrBGP) {
				// If the fuser already produced a BGP (should be rare after not preserving new-scope),
				// use it directly to avoid introducing nested brace layers.
				return (IrBGP) fused;
			}
		}

		// Inline UNION case: scan and replace
		boolean replaced = false;
		List<IrNode> out = new ArrayList<>();
		for (IrNode ln : bgp.getLines()) {
			if (ln instanceof IrUnion) {
				IrNode fused = tryFuseUnion((IrUnion) ln);
				if ((fused instanceof IrPathTriple || fused instanceof IrGraph)) {
					out.add(fused);
					replaced = true;
					continue;
				}
				if (fused instanceof IrBGP) {
					out.add(fused);
					replaced = true;
					continue;
				}
			}
			out.add(ln);
		}
		if (!replaced) {
			return bgp;
		}
		IrBGP nw = new IrBGP(bgp.isNewScope());
		out.forEach(nw::add);
		return nw;
	}

	private static IrNode tryFuseUnion(IrUnion u) {
		if (u == null || u.getBranches().size() != 2) {
			return u;
		}

		// Respect explicit UNION new scopes: only fuse when both branches share an _anon_path_* variable
		// under an allowed role mapping (s-s, s-o, o-s, o-p). Otherwise, preserve the UNION.
		if (BaseTransform.unionIsExplicitAndAllBranchesScoped(u)) {
			return u;
		}

		// Robustly unwrap each branch: allow nested single-child BGP groups and an optional GRAPH wrapper.
		// holder for extracted branch shape

		Branch b1 = extractBranch(u.getBranches().get(0));
		Branch b2 = extractBranch(u.getBranches().get(1));
		if (b1 == null || b2 == null) {
			return u;
		}

		IrPathTriple p1 = b1.pt;
		IrPathTriple p2 = b2.pt;
		Var graphRef = b1.graph;
		// Graph refs must match (both null or equal)
		if ((graphRef == null && b2.graph != null) || (graphRef != null && b2.graph == null)
				|| (graphRef != null && !eqVarOrValue(graphRef, b2.graph))) {
			return u;
		}

		Var sCanon = p1.getSubject();
		Var oCanon = p1.getObject();

		// Normalize compact NPS forms
		String m1 = BaseTransform.normalizeCompactNps(p1.getPathText());
		String m2 = BaseTransform.normalizeCompactNps(p2.getPathText());
		if (m1 == null || m2 == null) {
			return u;
		}

		// Align branch 2 orientation to branch 1
		String add2 = m2;
		if (eqVarOrValue(sCanon, p2.getObject()) && eqVarOrValue(oCanon, p2.getSubject())) {
			String inv = BaseTransform.invertNegatedPropertySet(m2);
			if (inv == null) {
				return u;
			}
			add2 = inv;
		} else if (!(eqVarOrValue(sCanon, p2.getSubject()) && eqVarOrValue(oCanon, p2.getObject()))) {
			return u;
		}

		String merged = BaseTransform.mergeNpsMembers(m1, add2);
		Set<Var> pv = new HashSet<>();
		pv.addAll(p1.getPathVars());
		pv.addAll(p2.getPathVars());
		IrPathTriple fused = new IrPathTriple(sCanon, p1.getSubjectOverride(), merged, oCanon, p1.getObjectOverride(),
				pv, u.isNewScope());
		IrNode out = fused;
		if (graphRef != null) {
			IrBGP inner = new IrBGP(false);
			inner.add(fused);
			out = new IrGraph(graphRef, inner, false);
		}
		// Preserve explicit UNION new-scope grouping by wrapping the fused result in a grouped BGP.
		if (u.isNewScope()) {
			IrBGP grp = new IrBGP(false);
			grp.add(out);
			return grp;
		}
		return out;
	}

	/** extract a single IrPathTriple (possibly under a single GRAPH) from a branch consisting only of wrappers. */
	private static Branch extractBranch(IrBGP b) {
		Branch out = new Branch();
		if (b == null || b.getLines() == null || b.getLines().isEmpty()) {
			return null;
		}
		// unwrap chains of single-child BGPs
		IrNode cur = singleChild(b);
		while (cur instanceof IrBGP) {
			IrNode inner = singleChild((IrBGP) cur);
			if (inner == null) {
				break;
			}
			cur = inner;
		}
		if (cur instanceof IrGraph) {
			IrGraph g = (IrGraph) cur;
			out.graph = g.getGraph();
			cur = singleChild(g.getWhere());
			while (cur instanceof IrBGP) {
				IrNode inner = singleChild((IrBGP) cur);
				if (inner == null) {
					break;
				}
				cur = inner;
			}
		}
		if (cur instanceof IrPathTriple) {
			out.pt = (IrPathTriple) cur;
			return out;
		}
		return null;
	}

	private static final class Branch {
		Var graph;
		IrPathTriple pt;
	}

	private static IrNode singleChild(IrBGP b) {
		if (b == null) {
			return null;
		}
		List<IrNode> ls = b.getLines();
		if (ls == null || ls.size() != 1) {
			return null;
		}
		return ls.get(0);
	}

	private static boolean eqVarOrValue(Var a, Var b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a.hasValue() && b.hasValue()) {
			return a.getValue().equals(b.getValue());
		}
		if (!a.hasValue() && !b.hasValue()) {
			String an = a.getName();
			String bn = b.getName();
			return an != null && an.equals(bn);
		}
		return false;
	}
}
