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

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null)
			return null;
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrService) {
				m = fuseInService((IrService) n);
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere()));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere()));
				no.setNewScope(o.isNewScope());
				m = no;
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere()));
			} else if (n instanceof IrSubSelect) {
				// keep
			} else {
				// recurse to children BGPs via transformChildren
				m = n.transformChildren(child -> {
					if (child instanceof IrBGP)
						return apply((IrBGP) child);
					return child;
				});
			}
			out.add(m);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	private static IrNode fuseInService(IrService s) {
		IrBGP where = s.getWhere();
		if (where == null || where.getLines().size() != 1 || !(where.getLines().get(0) instanceof IrUnion)) {
			return s;
		}
		IrUnion u = (IrUnion) where.getLines().get(0);
		if (u.getBranches().size() != 2)
			return s;

		IrBGP b1 = u.getBranches().get(0);
		IrBGP b2 = u.getBranches().get(1);
		if (b1.getLines().size() != 1 || b2.getLines().size() != 1)
			return s;
		if (!(b1.getLines().get(0) instanceof IrPathTriple) || !(b2.getLines().get(0) instanceof IrPathTriple))
			return s;

		IrPathTriple p1 = (IrPathTriple) b1.getLines().get(0);
		IrPathTriple p2 = (IrPathTriple) b2.getLines().get(0);

		Var s1 = p1.getSubject();
		Var o1 = p1.getObject();
		Var s2 = p2.getSubject();
		Var o2 = p2.getObject();

		// Must be opposing orientation between the same endpoints
		if (!(sameVar(s1, o2) && sameVar(o1, s2)))
			return s;

		String m1 = normalizeCompactNps(p1.getPathText());
		String m2 = normalizeCompactNps(p2.getPathText());
		if (m1 == null || m2 == null)
			return s;

		// Invert members of the reversed branch
		String m2inv = invertNegatedPropertySet(m2);
		if (m2inv == null)
			return s;

		String merged = mergeMembers(m1, m2inv);
		IrBGP nw = new IrBGP();
		nw.add(new IrPathTriple(s1, merged, o1));
		return new IrService(s.getServiceRefText(), s.isSilent(), nw);
	}

	private static String normalizeCompactNps(String path) {
		if (path == null)
			return null;
		String t = path.trim();
		if (t.isEmpty())
			return null;
		if (t.startsWith("!(") && t.endsWith(")"))
			return t;
		if (t.startsWith("!^"))
			return "!(" + t.substring(1) + ")";
		if (t.startsWith("!") && (t.length() == 1 || t.charAt(1) != '('))
			return "!(" + t.substring(1) + ")";
		return null;
	}

	private static String mergeMembers(String a, String b) {
		int a1 = a.indexOf('('), a2 = a.lastIndexOf(')');
		int b1 = b.indexOf('('), b2 = b.lastIndexOf(')');
		if (a1 < 0 || a2 < 0 || b1 < 0 || b2 < 0)
			return a;
		String ia = a.substring(a1 + 1, a2).trim();
		String ib = b.substring(b1 + 1, b2).trim();
		if (ia.isEmpty())
			return b;
		if (ib.isEmpty())
			return a;
		return "!(" + ia + "|" + ib + ")";
	}
}
