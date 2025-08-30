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
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
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
			if (fused instanceof IrPathTriple) {
				IrBGP nw = new IrBGP();
				nw.add(fused);
				nw.setNewScope(bgp.isNewScope());
				return nw;
			}
		}

		// Inline UNION case: scan and replace
		boolean replaced = false;
		List<IrNode> out = new ArrayList<>();
		for (IrNode ln : bgp.getLines()) {
			if (ln instanceof IrUnion) {
				IrNode fused = tryFuseUnion((IrUnion) ln);
				if (fused instanceof IrPathTriple) {
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
		IrBGP nw = new IrBGP();
		out.forEach(nw::add);
		nw.setNewScope(bgp.isNewScope());
		return nw;
	}

	private static IrNode tryFuseUnion(IrUnion u) {
		if (u == null || u.getBranches().size() != 2) {
			return u;
		}
		IrBGP b1 = u.getBranches().get(0);
		IrBGP b2 = u.getBranches().get(1);
		if (b1.getLines().size() != 1 || b2.getLines().size() != 1) {
			return u;
		}
		if (!(b1.getLines().get(0) instanceof IrPathTriple) || !(b2.getLines().get(0) instanceof IrPathTriple)) {
			return u;
		}
		IrPathTriple p1 = (IrPathTriple) b1.getLines().get(0);
		IrPathTriple p2 = (IrPathTriple) b2.getLines().get(0);
		Var s1 = p1.getSubject();
		Var o1 = p1.getObject();
		Var s2 = p2.getSubject();
		Var o2 = p2.getObject();

		Function<String, String> normalize = (path) -> {
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
		};

		String m1 = normalize.apply(p1.getPathText());
		String m2 = normalize.apply(p2.getPathText());
		if (m1 == null || m2 == null)
			return u;

		Function<String, String> invert = (s) -> {
			if (s == null || !s.startsWith("!(") || !s.endsWith(")"))
				return null;
			String inner = s.substring(2, s.length() - 1);
			if (inner.isEmpty())
				return s;
			String[] parts = inner.split("\\|");
			List<String> rev = new ArrayList<>();
			for (String tok : parts) {
				String t = tok.trim();
				if (!t.startsWith("^")) {
					rev.add("^" + t);
				} else {
					rev.add(t);
				}
			}
			return "!(" + String.join("|", rev) + ")";
		};

		BiFunction<String, String, String> merge = (a, btxt) -> {
			int a1 = a.indexOf('('), a2 = a.lastIndexOf(')');
			int bb1 = btxt.indexOf('('), bb2 = btxt.lastIndexOf(')');
			if (a1 < 0 || a2 < 0 || bb1 < 0 || bb2 < 0)
				return a;
			String ia = a.substring(a1 + 1, a2).trim();
			String ib = btxt.substring(bb1 + 1, bb2).trim();
			if (ia.isEmpty())
				return btxt;
			if (ib.isEmpty())
				return a;
			return "!(" + ia + "|" + ib + ")";
		};

		// reversed endpoints
		if (eqVarOrValue(s1, o2) && eqVarOrValue(o1, s2)) {
			String m2inv = invert.apply(m2);
			if (m2inv == null)
				return u;
			return new IrPathTriple(s1, merge.apply(m1, m2inv), o1);
		}
		// same orientation
		if (eqVarOrValue(s1, s2) && eqVarOrValue(o1, o2)) {
			return new IrPathTriple(s1, merge.apply(m1, m2), o1);
		}
		return u;
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
