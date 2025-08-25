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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
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
		// Accept unions with >=2 branches: exactly one sameTerm filter branch, remaining branches must be
		// single-step statement patterns that connect ?s and ?o in forward or inverse direction.
		IrBGP filterBranch = null;
		List<IrBGP> stepBranches = new ArrayList<>();
		for (IrBGP b : u.getBranches()) {
			if (isSameTermFilterBranch(b)) {
				if (filterBranch != null) {
					return null; // more than one sameTerm branch
				}
				filterBranch = b;
			} else {
				stepBranches.add(b);
			}
		}
		if (filterBranch == null || stepBranches.isEmpty()) {
			return null;
		}
		String[] so = parseSameTermVars(((IrText) filterBranch.getLines().get(0)).getText());
		if (so == null) {
			return null;
		}
		final String sName = so[0], oName = so[1];

		// Collect simple single-step patterns from the non-filter branches
		final List<String> steps = new ArrayList<>();
		for (IrBGP b : stepBranches) {
			if (b.getLines().size() != 1) {
				return null;
			}
			IrNode ln = b.getLines().get(0);
			IrStatementPattern sp;
			if (ln instanceof IrStatementPattern) {
				sp = (IrStatementPattern) ln;
			} else if (ln instanceof IrGraph && ((IrGraph) ln).getWhere() != null
					&& ((IrGraph) ln).getWhere().getLines().size() == 1
					&& ((IrGraph) ln).getWhere().getLines().get(0) instanceof IrStatementPattern) {
				sp = (IrStatementPattern) ((IrGraph) ln).getWhere().getLines().get(0);
			} else if (ln instanceof IrPathTriple) {
				// already fused; accept as-is
				IrPathTriple pt = (IrPathTriple) ln;
				if (sameVar(varNamed(sName), pt.getSubject()) && sameVar(varNamed(oName), pt.getObject())) {
					steps.add(pt.getPathText());
					continue;
				}
				return null;
			} else {
				return null;
			}
			Var p = sp.getPredicate();
			if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
				return null;
			}
			String step = r.renderIRI((IRI) p.getValue());
			if (sameVar(varNamed(sName), sp.getSubject()) && sameVar(varNamed(oName), sp.getObject())) {
				steps.add(step);
			} else if (sameVar(varNamed(sName), sp.getObject()) && sameVar(varNamed(oName), sp.getSubject())) {
				steps.add("^" + step);
			} else {
				return null;
			}
		}
		if (steps.isEmpty()) {
			return null;
		}
		final String innerAlt = (steps.size() == 1) ? steps.get(0) : ("(" + String.join("|", steps) + ")");
		final String expr = "(" + innerAlt + ")?";
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
