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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrText;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

public final class NormalizeZeroOrOneSubselectTransform extends BaseTransform {
	private NormalizeZeroOrOneSubselectTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode transformed = n;
			if (n instanceof IrSubSelect) {
				IrPathTriple pt = tryRewriteZeroOrOne((IrSubSelect) n, r);
				if (pt != null) {
					transformed = pt;
				}
			}
			// Recurse into containers using transformChildren
			transformed = transformed.transformChildren(child -> {
				if (child instanceof IrBGP) {
					return apply((IrBGP) child, r);
				}
				return child;
			});
			out.add(transformed);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	public static IrPathTriple tryRewriteZeroOrOne(IrSubSelect ss, TupleExprIRRenderer r) {
		IrSelect sel = ss.getSelect();
		if (sel == null || sel.getWhere() == null) {
			return null;
		}
		List<IrNode> inner = sel.getWhere().getLines();
		if (inner.size() != 1 || !(inner.get(0) instanceof IrUnion)) {
			return null;
		}
		IrUnion u = (IrUnion) inner.get(0);
		if (u.getBranches().size() != 2) {
			return null;
		}
		IrBGP b1 = u.getBranches().get(0);
		IrBGP b2 = u.getBranches().get(1);
		IrBGP filterBranch, chainBranch;
		// Identify which branch is the sameTerm filter
		if (isSameTermFilterBranch(b1)) {
			filterBranch = b1;
			chainBranch = b2;
		} else if (isSameTermFilterBranch(b2)) {
			filterBranch = b2;
			chainBranch = b1;
		} else {
			return null;
		}
		String[] so = parseSameTermVars(((IrText) filterBranch.getLines().get(0)).getText());
		if (so == null) {
			return null;
		}
		final String sName = so[0], oName = so[1];

		// Fast-path: if earlier passes have already fused the chain into a single IrPathTriple,
		// and its endpoints match ?s and ?o, simply wrap the path with '?'.
		if (chainBranch.getLines().size() == 1 && chainBranch.getLines().get(0) instanceof IrPathTriple) {
			IrPathTriple pt = (IrPathTriple) chainBranch.getLines().get(0);
			if (sameVar(varNamed(sName), pt.getSubject()) && sameVar(varNamed(oName), pt.getObject())) {
				final String expr = "(" + pt.getPathText() + ")?";
				return new IrPathTriple(pt.getSubject(), expr, pt.getObject());
			}
			// If orientation is reversed or endpoints differ, conservatively skip.
		}
		// Collect simple SPs in the chain branch
		List<IrStatementPattern> sps = new ArrayList<>();
		for (IrNode ln : chainBranch.getLines()) {
			if (ln instanceof IrStatementPattern) {
				sps.add((IrStatementPattern) ln);
			} else {
				return null; // be conservative
			}
		}
		if (sps.isEmpty()) {
			return null;
		}
		// Walk from ?s to ?o via _anon_path_* vars
		Var cur = varNamed(sName);
		Var goal = varNamed(oName);
		List<String> steps = new ArrayList<>();
		Set<IrStatementPattern> used = new LinkedHashSet<>();
		int guard = 0;
		while (!sameVar(cur, goal)) {
			if (++guard > 10000) {
				return null;
			}
			boolean advanced = false;
			for (IrStatementPattern sp : sps) {
				if (used.contains(sp)) {
					continue;
				}
				Var p = sp.getPredicate();
				if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
					continue;
				}
				String step = r.renderIRI((IRI) p.getValue());
				Var sub = sp.getSubject();
				Var oo = sp.getObject();
				if (sameVar(cur, sub) && (isAnonPathVar(oo) || sameVar(oo, goal))) {
					steps.add(step);
					cur = oo;
					used.add(sp);
					advanced = true;
					break;
				} else if (sameVar(cur, oo) && (isAnonPathVar(sub) || sameVar(sub, goal))) {
					steps.add("^" + step);
					cur = sub;
					used.add(sp);
					advanced = true;
					break;
				}
			}
			if (!advanced) {
				return null;
			}
		}
		if (used.size() != sps.size() || steps.isEmpty()) {
			return null;
		}
		final String seq = (steps.size() == 1) ? steps.get(0) : String.join("/", steps);
		final String expr = "(" + seq + ")?";
		return new IrPathTriple(varNamed(sName), expr, varNamed(oName));
	}

	public static String[] parseSameTermVars(String text) {
		if (text == null) {
			return null;
		}
		Matcher m = Pattern
				.compile(
						"(?i)\\s*FILTER\\s*\\(\\s*sameTerm\\s*\\(\\s*\\?(?<s>[A-Za-z_][\\w]*)\\s*,\\s*\\?(?<o>[A-Za-z_][\\w]*)\\s*\\)\\s*\\)\\s*")
				.matcher(text);
		if (!m.matches()) {
			return null;
		}
		return new String[] { m.group("s"), m.group("o") };
	}

	public static boolean isSameTermFilterBranch(IrBGP b) {
		return b != null && b.getLines().size() == 1 && b.getLines().get(0) instanceof IrText
				&& parseSameTermVars(((IrText) b.getLines().get(0)).getText()) != null;
	}

	public static Var varNamed(String name) {
		if (name == null) {
			return null;
		}
		return new Var(name);
	}

}
