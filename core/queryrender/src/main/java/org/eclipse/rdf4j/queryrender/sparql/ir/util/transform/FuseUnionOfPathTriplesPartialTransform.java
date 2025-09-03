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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrValues;

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
		boolean containsValues = false;
		for (IrNode ln0 : bgp.getLines()) {
			if (ln0 instanceof IrValues) {
				containsValues = true;
				break;
			}
		}
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (!containsValues && n instanceof IrUnion) {
				m = fuseUnion((IrUnion) n, r);
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				// Allow union fusing inside GRAPH bodies even when a VALUES exists in the outer BGP.
				IrBGP inner = apply(g.getWhere(), r);
				m = new IrGraph(g.getGraph(), inner, g.isNewScope());
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(containsValues ? applyNoUnion(o.getWhere(), r) : apply(o.getWhere(), r),
						o.isNewScope());
				no.setNewScope(o.isNewScope());
				m = no;
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(containsValues ? applyNoUnion(mi.getWhere(), r) : apply(mi.getWhere(), r),
						mi.isNewScope());
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(),
						containsValues ? applyNoUnion(s.getWhere(), r) : apply(s.getWhere(), r), s.isNewScope());
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

	private static IrBGP applyNoUnion(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrUnion) {
				// keep union as-is but still recurse into children without fusing
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(applyNoUnion(b, r));
				}
				m = u2;
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), applyNoUnion(g.getWhere(), r), g.isNewScope());
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(applyNoUnion(o.getWhere(), r), o.isNewScope());
				no.setNewScope(o.isNewScope());
				m = no;
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(applyNoUnion(mi.getWhere(), r), mi.isNewScope());
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), applyNoUnion(s.getWhere(), r), s.isNewScope());
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
		// Safety for new-scope UNIONs: only allow fusing when all branches share a unique common
		// _anon_path_* variable name (parser bridge), so we don't collapse user-visible vars.
		if (u.isNewScope()) {
			Set<String> common = collectCommonAnonPathVarNames(u);
			if (common == null || common.size() != 1) {
				return u;
			}
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
			// Exclude only quantifiers; allow alternation and NPS and normalize during merging.
			String trimmed = ptxt.trim();
			if (trimmed.endsWith("?") || trimmed.endsWith("*") || trimmed.endsWith("+")) {
				pathTexts.add(null);
				continue; // skip complex paths with quantifiers
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
				ArrayList<String> alts = new ArrayList<>();
				for (int idx : idxs) {
					String t = pathTexts.get(idx);
					if (t != null) {
						alts.add(t);
					}
				}
				String merged;
				if (idxs.size() == 2) {
					List<String> aTokens = splitTopLevelAlternation(pathTexts.get(idxs.get(0)));
					List<String> bTokens = splitTopLevelAlternation(pathTexts.get(idxs.get(1)));
					List<String> negMembers = new ArrayList<>();
					List<String> aNonNeg = new ArrayList<>();
					List<String> bNonNeg = new ArrayList<>();
					extractNegAndNonNeg(aTokens, negMembers, aNonNeg);
					extractNegAndNonNeg(bTokens, negMembers, bNonNeg);
					ArrayList<String> outTok = new ArrayList<>();
					outTok.addAll(aNonNeg);
					if (!negMembers.isEmpty()) {
						outTok.add("!(" + String.join("|", negMembers) + ")");
					}
					outTok.addAll(bNonNeg);
					merged = outTok.isEmpty() ? "(" + String.join("|", alts) + ")"
							: "(" + String.join("|", outTok) + ")";
				} else {
					merged = String.join("|", alts);
					if (alts.size() > 1) {
						merged = "(" + merged + ")";
					}
				}

				// Preserve explicit new-scope grouping from the original UNION by marking the
				// merged branch BGP with the same newScope flag. This ensures the renderer
				// prints the extra pair of braces expected around the fused branch.
				IrBGP b = new IrBGP(u.isNewScope());
				// Branches are simple or path triples; if path triples, union their pathVars
				Set<Var> acc = new HashSet<>();
				for (int idx : idxs) {
					IrBGP br = u.getBranches().get(idx - 1);
					IrNode only = (br.getLines().size() == 1) ? br.getLines().get(0) : null;
					if (only instanceof IrGraph) {
						IrGraph gb = (IrGraph) only;
						if (gb.getWhere() != null && gb.getWhere().getLines().size() == 1
								&& gb.getWhere()
										.getLines()
										.get(0) instanceof IrPathTriple) {
							IrPathTriple pt = (IrPathTriple) gb
									.getWhere()
									.getLines()
									.get(0);
							acc.addAll(pt.getPathVars());
						}
					} else if (only instanceof IrPathTriple) {
						acc.addAll(((IrPathTriple) only).getPathVars());
					}
				}
				IrPathTriple mergedPt = new IrPathTriple(grp.s, merged, grp.o, false, acc);
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

	private static Set<String> collectCommonAnonPathVarNames(IrUnion u) {
		Set<String> common = null;
		for (IrBGP b : u.getBranches()) {
			Set<String> names = new HashSet<>();
			collectAnonNamesFromNode(b, names);
			if (names.isEmpty()) {
				return Collections.emptySet();
			}
			if (common == null) {
				common = new HashSet<>(names);
			} else {
				common.retainAll(names);
				if (common.isEmpty()) {
					return common;
				}
			}
		}
		return common == null ? Collections.emptySet() : common;
	}

	private static void collectAnonNamesFromNode(IrNode n, Set<String> out) {
		if (n == null) {
			return;
		}
		if (n instanceof IrBGP) {
			for (IrNode ln : ((IrBGP) n).getLines()) {
				collectAnonNamesFromNode(ln, out);
			}
			return;
		}
		if (n instanceof IrGraph) {
			collectAnonNamesFromNode(((IrGraph) n).getWhere(), out);
			return;
		}
		if (n instanceof IrOptional) {
			collectAnonNamesFromNode(((IrOptional) n).getWhere(), out);
			return;
		}
		if (n instanceof IrMinus) {
			collectAnonNamesFromNode(((IrMinus) n).getWhere(), out);
			return;
		}
		if (n instanceof IrService) {
			collectAnonNamesFromNode(((IrService) n).getWhere(), out);
			return;
		}
		if (n instanceof IrUnion) {
			for (IrBGP b : ((IrUnion) n).getBranches()) {
				collectAnonNamesFromNode(b, out);
			}
			return;
		}
		if (n instanceof IrStatementPattern) {
			Var s = ((IrStatementPattern) n).getSubject();
			Var o = ((IrStatementPattern) n).getObject();
			Var p = ((IrStatementPattern) n).getPredicate();
			if (isAnonPathVar(s) || isAnonPathInverseVar(s)) {
				out.add(s.getName());
			}
			if (isAnonPathVar(o) || isAnonPathInverseVar(o)) {
				out.add(o.getName());
			}
			if (p != null && !p.hasValue() && p.getName() != null
					&& (p.getName().startsWith(ANON_PATH_PREFIX) || p.getName().startsWith(ANON_PATH_INVERSE_PREFIX))) {
				out.add(p.getName());
			}
			return;
		}
		if (n instanceof IrPathTriple) {
			Var s = ((IrPathTriple) n).getSubject();
			Var o = ((IrPathTriple) n).getObject();
			if (isAnonPathVar(s) || isAnonPathInverseVar(s)) {
				out.add(s.getName());
			}
			if (isAnonPathVar(o) || isAnonPathInverseVar(o)) {
				out.add(o.getName());
			}
		}
	}

	private static List<String> splitTopLevelAlternation(String path) {
		ArrayList<String> out = new ArrayList<>();
		if (path == null) {
			return out;
		}
		String s = path.trim();
		if (BaseTransform.isWrapped(s)) {
			s = s.substring(1, s.length() - 1).trim();
		}
		int depth = 0;
		StringBuilder cur = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (ch == '(') {
				depth++;
				cur.append(ch);
			} else if (ch == ')') {
				depth--;
				cur.append(ch);
			} else if (ch == '|' && depth == 0) {
				String tok = cur.toString().trim();
				if (!tok.isEmpty()) {
					out.add(tok);
				}
				cur.setLength(0);
			} else {
				cur.append(ch);
			}
		}
		String tok = cur.toString().trim();
		if (!tok.isEmpty()) {
			out.add(tok);
		}
		if (out.isEmpty()) {
			out.add(s);
		}
		return out;
	}

	private static void extractNegAndNonNeg(List<String> tokens, List<String> negMembers, List<String> nonNeg) {
		if (tokens == null) {
			return;
		}
		for (String t : tokens) {
			String x = t.trim();
			if (x.startsWith("!(") && x.endsWith(")")) {
				String inner = x.substring(2, x.length() - 1).trim();
				List<String> innerToks = splitTopLevelAlternation(inner);
				for (String it : innerToks) {
					String m = it.trim();
					if (!m.isEmpty()) {
						negMembers.add(m);
					}
				}
			} else if (x.startsWith("!^")) {
				negMembers.add(x.substring(1).trim());
			} else if (x.startsWith("!") && (x.length() == 1 || x.charAt(1) != '(')) {
				negMembers.add(x.substring(1).trim());
			} else {
				nonNeg.add(x);
			}
		}
	}
}
