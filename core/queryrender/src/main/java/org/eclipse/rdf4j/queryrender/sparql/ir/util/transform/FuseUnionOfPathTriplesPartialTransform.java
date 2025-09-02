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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Within a UNION, merge a subset of branches that are single IrPathTriple (or GRAPH with single IrPathTriple), share
 * identical endpoints and graph ref, and do not themselves contain alternation or quantifiers. Produces a single merged
 * branch with alternation of the path texts, leaving remaining branches intact.
 */
public final class FuseUnionOfPathTriplesPartialTransform extends BaseTransform {

	private FuseUnionOfPathTriplesPartialTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrUnion) {
				m = fuseUnion((IrUnion) n, r);
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere(), r), g.isNewScope());
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere(), r), o.isNewScope());
				no.setNewScope(o.isNewScope());
				m = no;
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere(), r), mi.isNewScope());
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere(), r), s.isNewScope());
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

	private static IrNode fuseUnion(IrUnion u, TupleExprIRRenderer r) {
		if (u == null || u.getBranches().size() < 2) {
			return u;
		}
		// Preserve explicit UNION (new variable scope) as-is; do not fuse branches inside it.
		if (u.isNewScope()) {
			return u;
		}
		// Group candidate branches by (graphName,sName,oName) and remember a sample Var triple per group
		class Key {
			final String gName;
			final String sName;
			final String oName;

			Key(String gName, String sName, String oName) {
				this.gName = gName;
				this.sName = sName;
				this.oName = oName;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (o == null || getClass() != o.getClass()) {
					return false;
				}
				Key key = (Key) o;
				return Objects.equals(gName, key.gName)
						&& Objects.equals(sName, key.sName)
						&& Objects.equals(oName, key.oName);
			}

			@Override
			public int hashCode() {
				return Objects.hash(gName, sName, oName);
			}
		}
		class Group {
			final Var g;
			final Var s;
			final Var o;
			final List<Integer> idxs = new ArrayList<>();

			Group(Var g, Var s, Var o) {
				this.g = g;
				this.s = s;
				this.o = o;
			}
		}
		Map<Key, Group> groups = new LinkedHashMap<>();
		List<String> pathTexts = new ArrayList<>();
		pathTexts.add(null); // 1-based indexing helper
		for (int i = 0; i < u.getBranches().size(); i++) {
			IrBGP b = u.getBranches().get(i);
			Var g = null;
			Var sVar = null;
			Var oVar = null;
			String ptxt = null;
			// Accept a single-line PT or SP, optionally GRAPH-wrapped
			IrNode only = (b.getLines().size() == 1) ? b.getLines().get(0) : null;
			if (only instanceof IrGraph) {
				IrGraph gb = (IrGraph) only;
				g = gb.getGraph();
				if (gb.getWhere() != null && gb.getWhere().getLines().size() == 1) {
					IrNode innerOnly = gb.getWhere().getLines().get(0);
					if (innerOnly instanceof IrPathTriple) {
						IrPathTriple pt = (IrPathTriple) innerOnly;
						sVar = pt.getSubject();
						oVar = pt.getObject();
						ptxt = pt.getPathText();
					} else if (innerOnly instanceof IrStatementPattern) {
						IrStatementPattern sp = (IrStatementPattern) innerOnly;
						sVar = sp.getSubject();
						oVar = sp.getObject();
						ptxt = sp.getPredicate() != null && sp.getPredicate().hasValue()
								? r.convertIRIToString((IRI) sp.getPredicate().getValue())
								: null;
					}
				}
			} else if (only instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) only;
				sVar = pt.getSubject();
				oVar = pt.getObject();
				ptxt = pt.getPathText();
			} else if (only instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) only;
				sVar = sp.getSubject();
				oVar = sp.getObject();
				ptxt = sp.getPredicate() != null && sp.getPredicate().hasValue()
						? r.convertIRIToString((IRI) sp.getPredicate().getValue())
						: null;
			}

			if (sVar == null || oVar == null || ptxt == null) {
				pathTexts.add(null);
				continue;
			}
			// Exclude complex path patterns: allow only a single atomic step (optionally starting with ^),
			// but treat a negated property set !(...) as a single atomic step even if its inner text contains '|'.
			String trimmed = ptxt.trim();
			boolean isNps = trimmed.startsWith("!(");
			if (!isNps && (trimmed.contains("|") || trimmed.endsWith("?") || trimmed.endsWith("*")
					|| trimmed.endsWith("+"))) {
				pathTexts.add(null);
				continue; // skip complex paths
			}
			pathTexts.add(trimmed);
			String gName = g == null ? null : g.getName();
			String sName = sVar.getName();
			String oName = oVar.getName();
			Key k = new Key(gName, sName, oName);
			Group grp = groups.get(k);
			if (grp == null) {
				grp = new Group(g, sVar, oVar);
				groups.put(k, grp);
			}
			grp.idxs.add(i + 1); // store 1-based idx
		}

		boolean changed = false;
		IrUnion out = new IrUnion(u.isNewScope());
		for (Group grp : groups.values()) {
			List<Integer> idxs = grp.idxs;
			if (idxs.size() >= 2) {
				// Merge these branches into one alternation path
				ArrayList<String> alts = new ArrayList<>();
				for (int idx : idxs) {
					String t = pathTexts.get(idx);
					if (t != null) {
						alts.add(t);
					}
				}
				String merged = String.join("|", alts);
				// Parenthesize alternation to be safe when fused further into sequences
				if (alts.size() > 1) {
					merged = "(" + merged + ")";
				}
				IrBGP b = new IrBGP(false);
				IrPathTriple mergedPt = new IrPathTriple(grp.s, merged, grp.o, false);
				if (grp.g != null) {
					b.add(new IrGraph(grp.g, wrap(mergedPt), false));
				} else {
					b.add(mergedPt);
				}
				out.addBranch(b);
				changed = true;
			}
		}
		// Add non-merged branches
		for (int i = 0; i < u.getBranches().size(); i++) {
			boolean merged = false;
			for (Group grp : groups.values()) {
				if (grp.idxs.size() >= 2 && grp.idxs.contains(i + 1)) {
					merged = true;
					break;
				}
			}
			if (!merged) {
				out.addBranch(u.getBranches().get(i));
			}
		}
		return changed ? out : u;
	}

	private static IrBGP wrap(IrPathTriple pt) {
		IrBGP b = new IrBGP(false);
		b.add(pt);
		return b;
	}
}
