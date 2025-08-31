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
			if (fused instanceof IrPathTriple || fused instanceof IrGraph) {
				IrBGP nw = new IrBGP(bgp.isNewScope());
				nw.add(fused);
				return nw;
			}
		}

		// Inline UNION case: scan and replace
		boolean replaced = false;
		List<IrNode> out = new ArrayList<>();
		for (IrNode ln : bgp.getLines()) {
			if (ln instanceof IrUnion) {
				IrNode fused = tryFuseUnion((IrUnion) ln);
				if (fused instanceof IrPathTriple || fused instanceof IrGraph) {
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
		// Deeply unwrap each branch to find a bare NPS IrPathTriple, optionally under the same GRAPH
		Var graphRef = null;
		IrPathTriple p1 = null, p2 = null;
		Var sCanon = null, oCanon = null;

		for (int idx = 0; idx < 2; idx++) {
			IrBGP b = u.getBranches().get(idx);
			IrNode node = singleChild(b);
			// unwrap nested single-child BGPs
			while (node instanceof IrBGP) {
				IrNode inner = singleChild((IrBGP) node);
				if (inner == null)
					break;
				node = inner;
			}
			Var g = null;
			if (node instanceof IrGraph) {
				IrGraph gb = (IrGraph) node;
				g = gb.getGraph();
				node = singleChild(gb.getWhere());
				while (node instanceof IrBGP) {
					IrNode inner = singleChild((IrBGP) node);
					if (inner == null)
						break;
					node = inner;
				}
			}
			if (!(node instanceof IrPathTriple)) {
				return u;
			}
			if (idx == 0) {
				p1 = (IrPathTriple) node;
				sCanon = p1.getSubject();
				oCanon = p1.getObject();
				graphRef = g;
			} else {
				p2 = (IrPathTriple) node;
				// Graph refs must match (both null or equal)
				if ((graphRef == null && g != null) || (graphRef != null && g == null)
						|| (graphRef != null && !eqVarOrValue(graphRef, g))) {
					return u;
				}
			}
		}

		if (p1 == null || p2 == null)
			return u;

		// Normalize compact NPS forms
		String m1 = BaseTransform.normalizeCompactNps(p1.getPathText());
		String m2 = BaseTransform.normalizeCompactNps(p2.getPathText());
		if (m1 == null || m2 == null)
			return u;

		// Align branch 2 orientation to branch 1
		String add2 = m2;
		if (eqVarOrValue(sCanon, p2.getObject()) && eqVarOrValue(oCanon, p2.getSubject())) {
			String inv = BaseTransform.invertNegatedPropertySet(m2);
			if (inv == null)
				return u;
			add2 = inv;
		} else if (!(eqVarOrValue(sCanon, p2.getSubject()) && eqVarOrValue(oCanon, p2.getObject()))) {
			return u;
		}

		String merged = BaseTransform.mergeNpsMembers(m1, add2);
		IrPathTriple fused = new IrPathTriple(sCanon, merged, oCanon, false);
		if (graphRef != null) {
			IrBGP inner = new IrBGP(false);
			inner.add(fused);
			return new IrGraph(graphRef, inner, false);
		}
		return fused;
	}

	private static IrNode singleChild(IrBGP b) {
		if (b == null)
			return null;
		List<IrNode> ls = b.getLines();
		if (ls == null || ls.size() != 1)
			return null;
		return ls.get(0);
	}

	private static boolean eqVarOrValue(Var a, Var b) {
		if (a == b)
			return true;
		if (a == null || b == null)
			return false;
		if (a.hasValue() && b.hasValue())
			return a.getValue().equals(b.getValue());
		if (!a.hasValue() && !b.hasValue()) {
			String an = a.getName();
			String bn = b.getName();
			return an != null && an.equals(bn);
		}
		return false;
	}
}
