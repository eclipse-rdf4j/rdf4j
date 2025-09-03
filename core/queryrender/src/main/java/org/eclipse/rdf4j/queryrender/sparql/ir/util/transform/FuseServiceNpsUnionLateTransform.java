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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Late transform: inside a SERVICE body, fuse a UNION of two single-branch bare-NPS path triples into a single negated
 * property set path triple combining members. This runs after path formation so branches are already IrPathTriple nodes
 * of the form "!ex:p" or "!(...)".
 */
public final class FuseServiceNpsUnionLateTransform extends BaseTransform {
	private FuseServiceNpsUnionLateTransform() {
	}

	private static final class Branch {
		Var graph;
		boolean graphNewScope;
		boolean whereNewScope;
		IrPathTriple pt;
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrService) {
				m = fuseInService((IrService) n);
			} else if (n instanceof IrGraph) {
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
			} else if (n instanceof IrSubSelect) {
				// keep
			} else {
				// recurse to children BGPs via transformChildren
				m = n.transformChildren(child -> {
					if (child instanceof IrBGP) {
						return apply((IrBGP) child);
					}
					return child;
				});
			}
			out.add(m);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	private static IrNode fuseInService(IrService s) {
		IrBGP where = s.getWhere();
		if (where == null) {
			return s;
		}
		// First, fuse a top-level UNION-of-NPS if present
		IrBGP fusedTop = ServiceNpsUnionFuser.fuse(where);
		// Then, recursively fuse any nested UNION-of-NPS inside the SERVICE body
		IrBGP fusedDeep = fuseUnionsInBGP(fusedTop);
		if (fusedDeep != where) {
			return new IrService(s.getServiceRefText(), s.isSilent(), fusedDeep, s.isNewScope());
		}
		return s;
	}

	private static IrBGP fuseUnionsInBGP(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> out = new ArrayList<>();
		for (IrNode ln : bgp.getLines()) {
			IrNode m = ln;
			if (ln instanceof IrUnion) {
				IrNode fused = fuseUnionNode((IrUnion) ln);
				m = fused;
			} else if (ln instanceof IrGraph) {
				IrGraph g = (IrGraph) ln;
				m = new IrGraph(g.getGraph(), fuseUnionsInBGP(g.getWhere()), g.isNewScope());
			} else if (ln instanceof IrOptional) {
				IrOptional o = (IrOptional) ln;
				IrOptional no = new IrOptional(fuseUnionsInBGP(o.getWhere()), o.isNewScope());
				no.setNewScope(o.isNewScope());
				m = no;
			} else if (ln instanceof IrMinus) {
				IrMinus mi = (IrMinus) ln;
				m = new IrMinus(fuseUnionsInBGP(mi.getWhere()), mi.isNewScope());
			} else if (ln instanceof IrBGP) {
				m = fuseUnionsInBGP((IrBGP) ln);
			}
			out.add(m);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	private static IrNode fuseUnionNode(IrUnion u) {
		if (u == null || u.getBranches().size() != 2) {
			return u;
		}

		Branch b1 = extractBranch(u.getBranches().get(0));
		Branch b2 = extractBranch(u.getBranches().get(1));
		if (b1 == null || b2 == null) {
			return u;
		}

		IrPathTriple p1 = b1.pt;
		IrPathTriple p2 = b2.pt;
		Var sCanon = p1.getSubject();
		Var oCanon = p1.getObject();
		Var graphRef = b1.graph;
		boolean graphRefNewScope = b1.graphNewScope;
		boolean innerBgpNewScope = b1.whereNewScope;
		if ((graphRef == null && b2.graph != null) || (graphRef != null && b2.graph == null)
				|| (graphRef != null && !eqVarOrValue(graphRef, b2.graph))) {
			return u;
		}
		if (graphRef != null) {
			if (graphRefNewScope != b2.graphNewScope) {
				return u;
			}
			if (innerBgpNewScope != b2.whereNewScope) {
				return u;
			}
		}
		String m1 = normalizeCompactNpsLocal(p1.getPathText());
		String m2 = normalizeCompactNpsLocal(p2.getPathText());
		if (m1 == null || m2 == null) {
			return u;
		}
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
		String merged = mergeMembersLocal(m1, add2);
		IrPathTriple fused = new IrPathTriple(sCanon, p1.getSubjectOverride(), merged, oCanon, p1.getObjectOverride(),
				false);
		Set<Var> pv = new HashSet<>();
		pv.addAll(p1.getPathVars());
		pv.addAll(p2.getPathVars());
		fused.setPathVars(pv);
		IrNode out = fused;
		if (graphRef != null) {
			IrBGP inner = new IrBGP(innerBgpNewScope);
			inner.add(fused);
			out = new IrGraph(graphRef, inner, graphRefNewScope);
		}
		// Preserve explicit UNION grouping braces by wrapping the fused result when the UNION carried new scope.
		if (u.isNewScope()) {
			IrBGP grp = new IrBGP(true);
			grp.add(out);
			grp.setNewScope(true);
			return grp;
		}
		return out;
	}

	private static Branch extractBranch(IrBGP b) {
		if (b == null) {
			return null;
		}
		Branch out = new Branch();
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
			out.graphNewScope = g.isNewScope();
			out.whereNewScope = g.getWhere() != null && g.getWhere().isNewScope();
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

	private static String normalizeCompactNpsLocal(String path) {
		if (path == null) {
			return null;
		}
		String t = path.trim();
		if (t.isEmpty()) {
			return null;
		}
		if (t.startsWith("!(") && t.endsWith(")")) {
			return t;
		}
		if (t.startsWith("!^")) {
			return "!(" + t.substring(1) + ")";
		}
		if (t.startsWith("!") && (t.length() == 1 || t.charAt(1) != '(')) {
			return "!(" + t.substring(1) + ")";
		}
		return null;
	}

	private static String mergeMembersLocal(String a, String b) {
		int a1 = a.indexOf('('), a2 = a.lastIndexOf(')');
		int b1 = b.indexOf('('), b2 = b.lastIndexOf(')');
		if (a1 < 0 || a2 < 0 || b1 < 0 || b2 < 0) {
			return a;
		}
		String ia = a.substring(a1 + 1, a2).trim();
		String ib = b.substring(b1 + 1, b2).trim();
		if (ia.isEmpty()) {
			return b;
		}
		if (ib.isEmpty()) {
			return a;
		}
		return "!(" + ia + "|" + ib + ")";
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
