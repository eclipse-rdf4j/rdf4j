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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrExists;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Fuse a UNION whose branches are each a single bare-NPS path triple (optionally inside the same GRAPH) into a single
 * NPS triple that combines members, preserving forward orientation and inverting members from inverse-oriented branches
 * (using '^') when needed.
 *
 * Scope/safety: - Only merges UNIONs that are not marked as new scope (explicit UNIONs). - Only accepts branches that
 * are a single IrPathTriple, optionally wrapped in a GRAPH with identical graph ref. - Only fuses when each branch path
 * is a bare NPS of the form '!(...)' with no '/' or quantifiers. - Preserves branch encounter order for member tokens;
 * duplicates are removed while keeping first occurrence.
 */
public final class FuseUnionOfNpsBranchesTransform extends BaseTransform {

	private FuseUnionOfNpsBranchesTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null)
			return null;
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			// Do not fuse UNIONs at top-level; only fuse within EXISTS bodies (handled below)
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere(), r));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				m = new IrOptional(apply(o.getWhere(), r));
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere(), r));
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere(), r));
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			} else if (n instanceof IrFilter) {
				// Recurse into EXISTS bodies and allow fusing inside them
				IrFilter f = (IrFilter) n;
				IrNode body = f.getBody();
				if (body instanceof IrExists) {
					IrExists ex = (IrExists) body;
					m = new IrFilter(new IrExists(applyInsideExists(ex.getWhere(), r), ex.isNewScope()));
				} else {
					m = n.transformChildren(child -> {
						if (child instanceof IrBGP) {
							return apply((IrBGP) child, r);
						}
						return child;
					});
				}
			} else {
				// Recurse into nested BGPs inside other containers (e.g., FILTER EXISTS)
				m = n.transformChildren(child -> {
					if (child instanceof IrBGP) {
						return apply((IrBGP) child, r);
					}
					return child;
				});
			}
			out.add(m);
		}
		final IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	private static IrNode tryFuseUnion(IrUnion u) {
		if (u == null || u.getBranches().size() < 2)
			return u;
		// Preserve knowledge of original newScope to optionally reintroduce grouping braces for textual stability.
		final boolean wasNewScope = u.isNewScope();

		// Gather candidate branches: (optional GRAPH g) { IrPathTriple with bare NPS }.
		Var graphRef = null;
		Var sCanon = null;
		Var oCanon = null;
		final Set<String> members = new LinkedHashSet<>();
		int fusedCount = 0;

		for (IrBGP b : u.getBranches()) {
			IrPathTriple pt = null;
			Var g = null;
			if (b.getLines().size() == 1 && b.getLines().get(0) instanceof IrPathTriple) {
				pt = (IrPathTriple) b.getLines().get(0);
			} else if (b.getLines().size() == 1 && b.getLines().get(0) instanceof IrGraph) {
				IrGraph gb = (IrGraph) b.getLines().get(0);
				g = gb.getGraph();
				if (gb.getWhere() != null && gb.getWhere().getLines().size() == 1
						&& gb.getWhere().getLines().get(0) instanceof IrPathTriple) {
					pt = (IrPathTriple) gb.getWhere().getLines().get(0);
				} else {
					return u; // complex branch: bail out
				}
			} else {
				return u; // non-candidate branch
			}

			if (pt == null)
				return u;
			final String path = pt.getPathText() == null ? null : pt.getPathText().trim();
			if (path == null || !path.startsWith("!(") || !path.endsWith(")") || path.indexOf('/') >= 0
					|| path.endsWith("?") || path.endsWith("+") || path.endsWith("*")) {
				return u; // not a bare NPS
			}

			// Initialize canonical orientation from first branch
			if (sCanon == null && oCanon == null) {
				sCanon = pt.getSubject();
				oCanon = pt.getObject();
				graphRef = g;
				addMembers(path, members);
				fusedCount++;
				continue;
			}

			// Graph refs must match (both null or same var/value)
			if ((graphRef == null && g != null) || (graphRef != null && g == null)
					|| (graphRef != null && !sameVarOrValue(graphRef, g))) {
				return u;
			}

			String toAdd = path;
			// Align orientation: if this branch is reversed, invert its inner members
			if (sameVar(sCanon, pt.getObject()) && sameVar(oCanon, pt.getSubject())) {
				String inv = invertNegatedPropertySet(path);
				if (inv == null)
					return u; // should not happen; be safe
				toAdd = inv;
			} else if (!(sameVar(sCanon, pt.getSubject()) && sameVar(oCanon, pt.getObject()))) {
				return u; // endpoints mismatch
			}

			addMembers(toAdd, members);
			fusedCount++;
		}

		if (fusedCount >= 2 && !members.isEmpty()) {
			final String merged = "!(" + String.join("|", members) + ")";
			IrPathTriple mergedPt = new IrPathTriple(sCanon, merged, oCanon);
			IrNode fused;
			if (graphRef != null) {
				IrBGP inner = new IrBGP();
				inner.add(mergedPt);
				fused = new IrGraph(graphRef, inner);
			} else {
				fused = mergedPt;
			}
			if (wasNewScope) {
				// Wrap in an extra group to preserve explicit braces that existed around the UNION branches
				IrBGP grp = new IrBGP();
				grp.add(fused);
				return grp;
			}
			return fused;
		}
		return u;
	}

	/** Apply union-of-NPS fusing only within EXISTS bodies. */
	private static IrBGP applyInsideExists(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null)
			return null;
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrUnion) {
				m = tryFuseUnion((IrUnion) n);
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), applyInsideExists(g.getWhere(), r));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				m = new IrOptional(applyInsideExists(o.getWhere(), r));
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(applyInsideExists(mi.getWhere(), r));
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), applyInsideExists(s.getWhere(), r));
			} else if (n instanceof IrSubSelect) {
				// keep
			} else if (n instanceof IrFilter) {
				IrFilter f = (IrFilter) n;
				IrNode body = f.getBody();
				if (body instanceof IrExists) {
					IrExists ex = (IrExists) body;
					m = new IrFilter(new IrExists(applyInsideExists(ex.getWhere(), r), ex.isNewScope()));
				}
			}
			out.add(m);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	private static void addMembers(String npsPath, Set<String> out) {
		// npsPath assumed to be '!(...)'
		int start = npsPath.indexOf('(');
		int end = npsPath.lastIndexOf(')');
		if (start < 0 || end < 0 || end <= start)
			return;
		String inner = npsPath.substring(start + 1, end);
		for (String tok : inner.split("\\|")) {
			String t = tok.trim();
			if (!t.isEmpty()) {
				out.add(t);
			}
		}
	}
}
