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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrText;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Recognize a parsed subselect encoding of a simple zero-or-one property path between two variables and rewrite it to a
 * compact IrPathTriple with a trailing '?' quantifier.
 *
 * Roughly matches a UNION containing a sameTerm(?s, ?o) branch and one or more single-step patterns connecting ?s and
 * ?o (possibly via GRAPH or already-fused path triples). Produces {@code ?s (step1|step2|...) ? ?o}.
 *
 * This normalization simplifies common shapes produced by the parser for "?s (p? ) ?o" and enables subsequent path
 * fusions.
 */
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
				// Prefer node-aware rewrite to preserve GRAPH context when possible
				IrNode repl = tryRewriteZeroOrOneNode((IrSubSelect) n, r);
				if (repl != null) {
					transformed = repl;
				} else {
					IrPathTriple pt = tryRewriteZeroOrOne((IrSubSelect) n, r);
					if (pt != null) {
						transformed = pt;
					}
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
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	public static IrPathTriple tryRewriteZeroOrOne(IrSubSelect ss, TupleExprIRRenderer r) {
		Z01Analysis a = analyzeZeroOrOne(ss, r);
		if (a != null) {
			final String expr = BaseTransform.applyQuantifier(a.exprInner, '?');
			return new IrPathTriple(varNamed(a.sName), expr, varNamed(a.oName), false,
					java.util.Collections.emptySet());
		}
		IrSelect sel = ss.getSelect();
		if (sel == null || sel.getWhere() == null) {
			return null;
		}
		List<IrNode> inner = sel.getWhere().getLines();
		if (inner.isEmpty()) {
			return null;
		}
		IrUnion u = null;
		if (inner.size() == 1 && inner.get(0) instanceof IrUnion) {
			u = (IrUnion) inner.get(0);
		} else if (inner.size() == 1 && inner.get(0) instanceof IrBGP) {
			IrBGP w0 = (IrBGP) inner.get(0);
			if (w0.getLines().size() == 1 && w0.getLines().get(0) instanceof IrUnion) {
				u = (IrUnion) w0.getLines().get(0);
			}
		}
		if (u == null) {
			return null;
		}
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
		String[] so;
		IrNode fbLine = filterBranch.getLines().get(0);
		if (fbLine instanceof IrText) {
			so = parseSameTermVars(((IrText) fbLine).getText());
		} else if (fbLine instanceof IrFilter) {
			String cond = ((IrFilter) fbLine).getConditionText();
			so = parseSameTermVarsFromCondition(cond);
		} else {
			so = null;
		}
		if (so == null) {
			return null;
		}
		final String sName = so[0], oName = so[1];

		// Collect simple single-step patterns from the non-filter branches
		final List<String> steps = new ArrayList<>();
		// Track if all step branches are GRAPH-wrapped and, if so, that they use the same graph ref
		Var commonGraph = null;
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
				IrGraph g = (IrGraph) ln;
				sp = (IrStatementPattern) g.getWhere().getLines().get(0);
				if (commonGraph == null) {
					commonGraph = g.getGraph();
				} else if (!sameVar(commonGraph, g.getGraph())) {
					// Mixed different GRAPH refs; bail out
					return null;
				}
			} else if (ln instanceof IrPathTriple) {
				// already fused; accept as-is
				IrPathTriple pt = (IrPathTriple) ln;
				if (sameVar(varNamed(sName), pt.getSubject()) && sameVar(varNamed(oName), pt.getObject())) {
					steps.add(pt.getPathText());
					continue;
				}
				return null;
			} else if (ln instanceof IrGraph && ((IrGraph) ln).getWhere() != null
					&& ((IrGraph) ln).getWhere().getLines().size() == 1
					&& ((IrGraph) ln).getWhere().getLines().get(0) instanceof IrPathTriple) {
				// GRAPH wrapper around a single fused path step (e.g., an NPS) — handle orientation
				final IrGraph g = (IrGraph) ln;
				final IrPathTriple pt = (IrPathTriple) g.getWhere().getLines().get(0);
				if (commonGraph == null) {
					commonGraph = g.getGraph();
				} else if (!sameVar(commonGraph, g.getGraph())) {
					return null;
				}
				if (sameVar(varNamed(sName), pt.getSubject()) && sameVar(varNamed(oName), pt.getObject())) {
					steps.add(BaseTransform.normalizeCompactNps(pt.getPathText()));
					continue;
				} else if (sameVar(varNamed(sName), pt.getObject()) && sameVar(varNamed(oName), pt.getSubject())) {
					final String inv = invertNpsIfPossible(BaseTransform.normalizeCompactNps(pt.getPathText()));
					if (inv == null) {
						return null;
					}
					steps.add(inv);
					continue;
				} else {
					return null;
				}
			} else {
				return null;
			}
			Var p = sp.getPredicate();
			if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
				return null;
			}
			String step = r.convertIRIToString((IRI) p.getValue());
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
		String exprInner;
		// If all steps are simple negated property sets of the form !(...), merge their members into one NPS
		boolean allNps = true;
		List<String> npsMembers = new ArrayList<>();
		for (String st : steps) {
			String t = st == null ? null : st.trim();
			if (t == null || !t.startsWith("!(") || !t.endsWith(")")) {
				allNps = false;
				break;
			}
			String innerMembers = t.substring(2, t.length() - 1).trim();
			if (!innerMembers.isEmpty()) {
				npsMembers.add(innerMembers);
			}
		}
		if (allNps && !npsMembers.isEmpty()) {
			exprInner = "!(" + String.join("|", npsMembers) + ")";
		} else {
			exprInner = (steps.size() == 1) ? steps.get(0) : ("(" + String.join("|", steps) + ")");
		}
		final String expr = BaseTransform.applyQuantifier(exprInner, '?');
		return new IrPathTriple(varNamed(sName), expr, varNamed(oName), false, java.util.Collections.emptySet());
	}

	/**
	 * Variant of tryRewriteZeroOrOne that returns a generic IrNode. When all step branches are GRAPH-wrapped with the
	 * same graph ref, this returns an IrGraph containing the fused IrPathTriple, so that graph context is preserved and
	 * downstream coalescing can merge adjacent GRAPH blocks.
	 */
	public static IrNode tryRewriteZeroOrOneNode(IrSubSelect ss,
			TupleExprIRRenderer r) {
		Z01Analysis a = analyzeZeroOrOne(ss, r);
		if (a != null) {
			final String expr = BaseTransform.applyQuantifier(a.exprInner, '?');
			final IrPathTriple pt = new IrPathTriple(varNamed(a.sName), expr, varNamed(a.oName), false,
					java.util.Collections.emptySet());
			if (a.allGraphWrapped && a.commonGraph != null) {
				IrBGP innerBgp = new IrBGP(false);
				innerBgp.add(pt);
				return new IrGraph(a.commonGraph, innerBgp, false);
			}
			return pt;
		}
		IrSelect sel = ss.getSelect();
		if (sel == null || sel.getWhere() == null) {
			return null;
		}
		List<IrNode> inner = sel.getWhere().getLines();
		if (inner.isEmpty()) {
			return null;
		}
		IrUnion u = null;
		if (inner.size() == 1 && inner.get(0) instanceof IrUnion) {
			u = (IrUnion) inner.get(0);
		} else if (inner.size() == 1 && inner.get(0) instanceof IrBGP) {
			IrBGP w0 = (IrBGP) inner.get(0);
			if (w0.getLines().size() == 1 && w0.getLines().get(0) instanceof IrUnion) {
				u = (IrUnion) w0.getLines().get(0);
			}
		}
		if (u == null) {
			return null;
		}

		IrBGP filterBranch = null;
		List<IrBGP> stepBranches = new ArrayList<>();
		for (IrBGP b : u.getBranches()) {
			if (isSameTermFilterBranch(b)) {
				if (filterBranch != null) {
					return null;
				}
				filterBranch = b;
			} else {
				stepBranches.add(b);
			}
		}
		if (filterBranch == null || stepBranches.isEmpty()) {
			return null;
		}
		String[] so;
		IrNode fbLine = filterBranch.getLines().get(0);
		if (fbLine instanceof IrText) {
			so = parseSameTermVars(((IrText) fbLine).getText());
		} else if (fbLine instanceof IrFilter) {
			String cond = ((IrFilter) fbLine).getConditionText();
			so = parseSameTermVarsFromCondition(cond);
		} else {
			so = null;
		}
		if (so == null) {
			return null;
		}
		final String sName = so[0], oName = so[1];

		// Gather steps and graph context
		final List<String> steps = new ArrayList<>();
		boolean allGraphWrapped = true;
		Var commonGraph = null;
		for (IrBGP b : stepBranches) {
			if (b.getLines().size() != 1) {
				return null;
			}
			IrNode ln = b.getLines().get(0);
			if (ln instanceof IrStatementPattern) {
				allGraphWrapped = false;
				IrStatementPattern sp = (IrStatementPattern) ln;
				Var p = sp.getPredicate();
				if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
					return null;
				}
				String step = r.convertIRIToString((IRI) p.getValue());
				if (sameVar(varNamed(sName), sp.getSubject()) && sameVar(varNamed(oName), sp.getObject())) {
					steps.add(step);
				} else if (sameVar(varNamed(sName), sp.getObject()) && sameVar(varNamed(oName), sp.getSubject())) {
					steps.add("^" + step);
				} else {
					return null;
				}
			} else if (ln instanceof IrGraph) {
				IrGraph g = (IrGraph) ln;
				if (g.getWhere() == null || g.getWhere().getLines().size() != 1) {
					return null;
				}
				IrNode innerLn = g.getWhere().getLines().get(0);
				if (innerLn instanceof IrStatementPattern) {
					IrStatementPattern sp = (IrStatementPattern) innerLn;
					Var p = sp.getPredicate();
					if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
						return null;
					}
					if (commonGraph == null) {
						commonGraph = g.getGraph();
					} else if (!sameVar(commonGraph, g.getGraph())) {
						return null;
					}
					String step = r.convertIRIToString((IRI) p.getValue());
					if (sameVar(varNamed(sName), sp.getSubject()) && sameVar(varNamed(oName), sp.getObject())) {
						steps.add(step);
					} else if (sameVar(varNamed(sName), sp.getObject())
							&& sameVar(varNamed(oName), sp.getSubject())) {
						steps.add("^" + step);
					} else {
						return null;
					}
				} else if (innerLn instanceof IrPathTriple) {
					IrPathTriple pt = (IrPathTriple) innerLn;
					if (commonGraph == null) {
						commonGraph = g.getGraph();
					} else if (!sameVar(commonGraph, g.getGraph())) {
						return null;
					}
					if (sameVar(varNamed(sName), pt.getSubject()) && sameVar(varNamed(oName), pt.getObject())) {
						steps.add(BaseTransform.normalizeCompactNps(pt.getPathText()));
					} else if (sameVar(varNamed(sName), pt.getObject())
							&& sameVar(varNamed(oName), pt.getSubject())) {
						final String inv = invertNpsIfPossible(BaseTransform.normalizeCompactNps(pt.getPathText()));
						if (inv == null) {
							return null;
						}
						steps.add(inv);
					} else {
						return null;
					}
				} else {
					return null;
				}
			} else if (ln instanceof IrPathTriple) {
				allGraphWrapped = false;
				IrPathTriple pt = (IrPathTriple) ln;
				if (sameVar(varNamed(sName), pt.getSubject()) && sameVar(varNamed(oName), pt.getObject())) {
					steps.add(BaseTransform.normalizeCompactNps(pt.getPathText()));
				} else if (sameVar(varNamed(sName), pt.getObject()) && sameVar(varNamed(oName), pt.getSubject())) {
					final String inv = invertNpsIfPossible(BaseTransform.normalizeCompactNps(pt.getPathText()));
					if (inv == null) {
						return null;
					}
					steps.add(inv);
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		if (steps.isEmpty()) {
			return null;
		}
		// Merge NPS members if applicable
		boolean allNps = true;
		List<String> npsMembers = new ArrayList<>();
		for (String st : steps) {
			String t = st == null ? null : st.trim();
			if (t == null || !t.startsWith("!(") || !t.endsWith(")")) {
				allNps = false;
				break;
			}
			String innerMembers = t.substring(2, t.length() - 1).trim();
			if (!innerMembers.isEmpty()) {
				npsMembers.add(innerMembers);
			}
		}
		String exprInner;
		if (allNps && !npsMembers.isEmpty()) {
			exprInner = "!(" + String.join("|", npsMembers) + ")";
		} else {
			exprInner = (steps.size() == 1) ? steps.get(0) : ("(" + String.join("|", steps) + ")");
		}

		final String expr = BaseTransform.applyQuantifier(exprInner, '?');
		final IrPathTriple pt = new IrPathTriple(varNamed(sName), expr, varNamed(oName), false,
				java.util.Collections.emptySet());
		if (allGraphWrapped && commonGraph != null) {
			IrBGP innerBgp = new IrBGP(false);
			innerBgp.add(pt);
			return new IrGraph(commonGraph, innerBgp, false);
		}
		return pt;
	}

	/** Invert a negated property set: !(a|^b|c) -> !(^a|b|^c). Return null if not a simple NPS. */
	private static String invertNpsIfPossible(String nps) {
		if (nps == null) {
			return null;
		}
		final String s = BaseTransform.normalizeCompactNps(nps);
		if (!s.startsWith("!(") || !s.endsWith(")")) {
			return null;
		}
		final String inner = s.substring(2, s.length() - 1);
		if (inner.isEmpty()) {
			return s;
		}
		final String[] toks = inner.split("\\|");
		final List<String> out = new ArrayList<>(toks.length);
		for (String tok : toks) {
			final String t = tok.trim();
			if (t.isEmpty()) {
				continue;
			}
			if (t.startsWith("^")) {
				out.add(t.substring(1));
			} else {
				out.add("^" + t);
			}
		}
		return "!(" + String.join("|", out) + ")";
	}

	private static final class Z01Analysis {
		final String sName;
		final String oName;
		final String exprInner;
		final boolean allGraphWrapped;
		final Var commonGraph;

		Z01Analysis(String sName, String oName, String exprInner, boolean allGraphWrapped, Var commonGraph) {
			this.sName = sName;
			this.oName = oName;
			this.exprInner = exprInner;
			this.allGraphWrapped = allGraphWrapped;
			this.commonGraph = commonGraph;
		}
	}

	private static Z01Analysis analyzeZeroOrOne(IrSubSelect ss, TupleExprIRRenderer r) {
		IrSelect sel = ss.getSelect();
		if (sel == null || sel.getWhere() == null) {
			return null;
		}
		List<IrNode> inner = sel.getWhere().getLines();
		if (inner.isEmpty()) {
			return null;
		}
		IrUnion u = null;
		if (inner.size() == 1 && inner.get(0) instanceof IrUnion) {
			u = (IrUnion) inner.get(0);
		} else if (inner.size() == 1 && inner.get(0) instanceof IrBGP) {
			IrBGP w0 = (IrBGP) inner.get(0);
			if (w0.getLines().size() == 1 && w0.getLines().get(0) instanceof IrUnion) {
				u = (IrUnion) w0.getLines().get(0);
			}
		}
		if (u == null) {
			return null;
		}
		IrBGP filterBranch = null;
		List<IrBGP> stepBranches = new ArrayList<>();
		for (IrBGP b : u.getBranches()) {
			if (isSameTermFilterBranch(b)) {
				if (filterBranch != null) {
					return null;
				}
				filterBranch = b;
			} else {
				stepBranches.add(b);
			}
		}
		if (filterBranch == null || stepBranches.isEmpty()) {
			return null;
		}
		String[] so;
		IrNode fbLine = filterBranch.getLines().get(0);
		if (fbLine instanceof IrText) {
			so = parseSameTermVars(((IrText) fbLine).getText());
		} else if (fbLine instanceof IrFilter) {
			String cond = ((IrFilter) fbLine).getConditionText();
			so = parseSameTermVarsFromCondition(cond);
		} else {
			so = null;
		}
		String sName;
		String oName;
		if (so != null) {
			sName = so[0];
			oName = so[1];
		} else {
			// Fallback: derive s/o from the first step branch when sameTerm uses a non-var (e.g., [])
			// Require at least one branch and a simple triple/path with variable endpoints
			IrBGP first = stepBranches.get(0);
			if (first.getLines().size() != 1) {
				return null;
			}
			IrNode ln = first.getLines().get(0);
			Var sVar = null, oVar = null;
			if (ln instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) ln;
				sVar = sp.getSubject();
				oVar = sp.getObject();
			} else if (ln instanceof IrGraph) {
				IrGraph g = (IrGraph) ln;
				if (g.getWhere() == null || g.getWhere().getLines().size() != 1) {
					return null;
				}
				IrNode gln = g.getWhere().getLines().get(0);
				if (gln instanceof IrStatementPattern) {
					IrStatementPattern sp = (IrStatementPattern) gln;
					sVar = sp.getSubject();
					oVar = sp.getObject();
				} else if (gln instanceof IrPathTriple) {
					IrPathTriple pt = (IrPathTriple) gln;
					sVar = pt.getSubject();
					oVar = pt.getObject();
				} else {
					return null;
				}
			} else if (ln instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) ln;
				sVar = pt.getSubject();
				oVar = pt.getObject();
			} else {
				return null;
			}
			if (sVar == null || sVar.hasValue() || sVar.getName() == null) {
				return null;
			}
			if (oVar == null || oVar.hasValue() || oVar.getName() == null) {
				return null;
			}
			sName = sVar.getName();
			oName = oVar.getName();
		}
		final List<String> steps = new ArrayList<>();
		boolean allGraphWrapped = true;
		Var commonGraph = null;
		for (IrBGP b : stepBranches) {
			if (b.getLines().size() != 1) {
				return null;
			}
			IrNode ln = b.getLines().get(0);
			if (ln instanceof IrStatementPattern) {
				allGraphWrapped = false;
				IrStatementPattern sp = (IrStatementPattern) ln;
				Var p = sp.getPredicate();
				if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
					return null;
				}
				String step = r.convertIRIToString((IRI) p.getValue());
				if (sameVar(varNamed(sName), sp.getSubject()) && sameVar(varNamed(oName), sp.getObject())) {
					steps.add(step);
				} else if (sameVar(varNamed(sName), sp.getObject()) && sameVar(varNamed(oName), sp.getSubject())) {
					steps.add("^" + step);
				} else {
					return null;
				}
			} else if (ln instanceof IrGraph) {
				IrGraph g = (IrGraph) ln;
				if (g.getWhere() == null || g.getWhere().getLines().size() != 1) {
					return null;
				}
				IrNode innerLn = g.getWhere().getLines().get(0);
				if (innerLn instanceof IrStatementPattern) {
					IrStatementPattern sp = (IrStatementPattern) innerLn;
					Var p = sp.getPredicate();
					if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
						return null;
					}
					if (commonGraph == null) {
						commonGraph = g.getGraph();
					} else if (!sameVar(commonGraph, g.getGraph())) {
						return null;
					}
					String step = r.convertIRIToString((IRI) p.getValue());
					if (sameVar(varNamed(sName), sp.getSubject()) && sameVar(varNamed(oName), sp.getObject())) {
						steps.add(step);
					} else if (sameVar(varNamed(sName), sp.getObject()) && sameVar(varNamed(oName), sp.getSubject())) {
						steps.add("^" + step);
					} else {
						return null;
					}
				} else if (innerLn instanceof IrPathTriple) {
					IrPathTriple pt = (IrPathTriple) innerLn;
					if (commonGraph == null) {
						commonGraph = g.getGraph();
					} else if (!sameVar(commonGraph, g.getGraph())) {
						return null;
					}
					String txt = BaseTransform.normalizeCompactNps(pt.getPathText());
					if (sameVar(varNamed(sName), pt.getSubject()) && sameVar(varNamed(oName), pt.getObject())) {
						steps.add(txt);
					} else if (sameVar(varNamed(sName), pt.getObject()) && sameVar(varNamed(oName), pt.getSubject())) {
						final String inv = invertNpsIfPossible(txt);
						if (inv == null) {
							return null;
						}
						steps.add(inv);
					} else {
						return null;
					}
				} else {
					return null;
				}
			} else if (ln instanceof IrPathTriple) {
				allGraphWrapped = false;
				IrPathTriple pt = (IrPathTriple) ln;
				String txt = BaseTransform.normalizeCompactNps(pt.getPathText());
				if (sameVar(varNamed(sName), pt.getSubject()) && sameVar(varNamed(oName), pt.getObject())) {
					steps.add(txt);
				} else if (sameVar(varNamed(sName), pt.getObject()) && sameVar(varNamed(oName), pt.getSubject())) {
					final String inv = invertNpsIfPossible(txt);
					if (inv == null) {
						return null;
					}
					steps.add(inv);
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		if (steps.isEmpty()) {
			return null;
		}
		boolean allNps = true;
		List<String> npsMembers = new ArrayList<>();
		for (String st : steps) {
			String t = st == null ? null : st.trim();
			if (t == null || !t.startsWith("!(") || !t.endsWith(")")) {
				allNps = false;
				break;
			}
			String innerMembers = t.substring(2, t.length() - 1).trim();
			if (!innerMembers.isEmpty()) {
				npsMembers.add(innerMembers);
			}
		}
		String exprInner;
		if (allNps && !npsMembers.isEmpty()) {
			exprInner = "!(" + String.join("|", npsMembers) + ")";
		} else {
			exprInner = (steps.size() == 1) ? steps.get(0) : ("(" + String.join("|", steps) + ")");
		}
		return new Z01Analysis(sName, oName, exprInner, allGraphWrapped, commonGraph);
	}

	// compact NPS normalization is centralized in BaseTransform

	public static String[] parseSameTermVars(String text) {
		if (text == null) {
			return null;
		}
		Matcher m = Pattern
				.compile(
						"(?i)\\s*FILTER\\s*(?:\\(\\s*)?sameTerm\\s*\\(\\s*\\?(?<s>[A-Za-z_][\\w]*)\\s*,\\s*\\?(?<o>[A-Za-z_][\\w]*)\\s*\\)\\s*(?:\\)\\s*)?")
				.matcher(text);
		if (!m.matches()) {
			return null;
		}
		return new String[] { m.group("s"), m.group("o") };
	}

	public static boolean isSameTermFilterBranch(IrBGP b) {
		if (b == null || b.getLines().size() != 1) {
			return false;
		}
		IrNode ln = b.getLines().get(0);
		if (ln instanceof IrText) {
			String t = ((IrText) ln).getText();
			if (t == null) {
				return false;
			}
			if (parseSameTermVars(t) != null) {
				return true;
			}
			// Accept generic sameTerm() even when not both args are variables (e.g., sameTerm([], ?x))
			return t.contains("sameTerm(");
		}
		if (ln instanceof IrFilter) {
			String cond = ((IrFilter) ln).getConditionText();
			if (parseSameTermVarsFromCondition(cond) != null) {
				return true;
			}
			return cond != null && cond.contains("sameTerm(");
		}
		return false;
	}

	public static Var varNamed(String name) {
		if (name == null) {
			return null;
		}
		return new Var(name);
	}

	/** Parse sameTerm(?s,?o) from a plain FILTER condition text (no leading "FILTER"). */
	private static String[] parseSameTermVarsFromCondition(String cond) {
		if (cond == null) {
			return null;
		}
		Matcher m = Pattern
				.compile(
						"(?i)\\s*sameTerm\\s*\\(\\s*\\?(?<s>[A-Za-z_][\\w]*)\\s*,\\s*\\?(?<o>[A-Za-z_][\\w]*)\\s*\\)\\s*")
				.matcher(cond);
		if (!m.matches()) {
			return null;
		}
		return new String[] { m.group("s"), m.group("o") };
	}

}
