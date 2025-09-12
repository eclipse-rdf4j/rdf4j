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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
			} else if (n instanceof IrBGP) {
				// Recurse into nested BGPs introduced to preserve explicit grouping
				m = apply((IrBGP) n, r);
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				// Allow union fusing inside GRAPH bodies regardless of VALUES in the outer BGP.
				IrBGP inner = apply(g.getWhere(), r);
				m = new IrGraph(g.getGraph(), inner, g.isNewScope());
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere(), r),
						o.isNewScope());
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
		// First recursively transform branches so that nested unions are simplified before
		// attempting to fuse at this level.
		IrUnion transformed = new IrUnion(u.isNewScope());
		for (IrBGP b : u.getBranches()) {
			transformed.addBranch(apply(b, r));
		}
		u = transformed;

		// Universal safeguard: do not fuse explicit user UNIONs (new scope). Path-generated unions
		// are marked as newScope=false in the converter when safe alternation is detected.
		if (BaseTransform.unionIsExplicitAndAllBranchesScoped(u)) {
			return u;
		}
		// Use IrUnion.newScope as authoritative: the converter marks path-generated
		// alternation unions with newScope=false. Avoid inferring via branch scopes.
		// (no-op)
		// Note: do not early-return on new-scope unions. We gate fusing per-group below, allowing
		// either anon-path bridge sharing OR a conservative "safe alternation" case (identical
		// endpoints and graph, each branch a single PT/SP without quantifiers).
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
			// Accept a single-line PT or SP, optionally wrapped in one or more explicit grouping BGPs and/or a GRAPH
			IrNode cur = (b.getLines().size() == 1) ? b.getLines().get(0) : null;
			boolean progressed = true;
			while (progressed && cur != null) {
				progressed = false;
				if (cur instanceof IrBGP) {
					IrBGP nb = (IrBGP) cur;
					if (nb.getLines().size() == 1) {
						cur = nb.getLines().get(0);
						progressed = true;
						continue;
					}
				}
				if (cur instanceof IrGraph) {
					IrGraph gb = (IrGraph) cur;
					g = gb.getGraph();
					if (gb.getWhere() != null && gb.getWhere().getLines().size() == 1) {
						cur = gb.getWhere().getLines().get(0);
						progressed = true;
					}
				}
			}
			if (cur instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) cur;
				sVar = pt.getSubject();
				oVar = pt.getObject();
				ptxt = pt.getPathText();
				// no-op
			} else if (cur instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) cur;
				sVar = sp.getSubject();
				oVar = sp.getObject();
				ptxt = isConstantIriPredicate(sp) ? iri(sp.getPredicate(), r) : null;
				// no-op
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
			// no-op
		}

		HashSet<Integer> fusedIdxs = new HashSet<>();
		IrUnion out = new IrUnion(u.isNewScope());
		for (Group grp : groups.values()) {
			List<Integer> idxs = grp.idxs;
			if (idxs.size() >= 2) {
				// Safety: allow merging if branches share an anon path bridge, or when the
				// UNION is path-generated (all branches non-scoped) and branches form a
				// conservative safe alternation (single SP/PT without quantifiers).
				boolean shareAnon = branchesShareAnonPathVar(u, idxs);
				boolean safeAlt = branchesFormSafeAlternation(idxs, pathTexts);
				boolean pathGeneratedUnion = !u.isNewScope();
				if (!(shareAnon || (pathGeneratedUnion && safeAlt))) {
					continue;
				}
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
					ArrayList<String> outTok = new ArrayList<>(aNonNeg);
					if (!negMembers.isEmpty()) {
						outTok.add("!(" + String.join("|", negMembers) + ")");
					}
					outTok.addAll(bNonNeg);
					merged = outTok.isEmpty() ? String.join("|", alts) : String.join("|", outTok);
				} else {
					merged = String.join("|", alts);
				}

				// Preserve explicit grouping for unions that had new variable scope: propagate the
				// UNION's newScope to the fused replacement branch so that braces are retained even
				// when the UNION collapses to a single branch.
				boolean branchScope = u.isNewScope();
				IrBGP b = new IrBGP(branchScope);
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
				IrPathTriple mergedPt = new IrPathTriple(grp.s, merged, grp.o, branchScope, acc);
				if (grp.g != null) {
					b.add(new IrGraph(grp.g, wrap(mergedPt), false));
				} else {
					b.add(mergedPt);
				}
				out.addBranch(b);
				fusedIdxs.addAll(idxs);
				// no-op
			}
		}
		// Add non-merged branches (already recursively transformed above)
		for (int i = 0; i < u.getBranches().size(); i++) {
			if (!fusedIdxs.contains(i + 1)) {
				out.addBranch(u.getBranches().get(i));
			}
		}

		// Local cleanup of redundant BGP layer: If a branch is a BGP that contains exactly a
		// single inner BGP which itself contains exactly one simple node (path triple or GRAPH
		// with single path triple), unwrap that inner BGP so the branch prints with a single
		// brace layer.
		IrUnion normalized = new IrUnion(out.isNewScope());
		for (IrBGP br : out.getBranches()) {
			normalized.addBranch(unwrapSingleBgpLayer(br));
		}

		return normalized;
	}

	private static IrBGP unwrapSingleBgpLayer(IrBGP branch) {
		if (branch == null) {
			return null;
		}
		// Iteratively unwrap nested IrBGP layers that each wrap exactly one simple node
		IrBGP cur = branch;
		while (true) {
			IrBGP b = cur;
			if (b.getLines().size() != 1) {
				break;
			}
			IrNode only = b.getLines().get(0);
			if (!(only instanceof IrBGP)) {
				// Top-level is a BGP wrapping a non-BGP (ok)
				break;
			}
			IrBGP inner = (IrBGP) only;
			if (inner.getLines().size() != 1) {
				break;
			}
			IrNode innerOnly = inner.getLines().get(0);
			boolean simple = (innerOnly instanceof IrPathTriple)
					|| (innerOnly instanceof IrGraph && ((IrGraph) innerOnly).getWhere() != null
							&& ((IrGraph) innerOnly).getWhere().getLines().size() == 1
							&& ((IrGraph) innerOnly).getWhere().getLines().get(0) instanceof IrPathTriple);
			if (!simple) {
				break;
			}
			// Replace the inner BGP with its only simple node and continue to see if more layers exist
			IrBGP replaced = new IrBGP(b.isNewScope());
			replaced.add(innerOnly);
			cur = replaced;
		}
		return cur;
	}

	private static boolean branchesShareAnonPathVar(IrUnion u, List<Integer> idxs) {
		// Build intersection of anon-path var names across all selected branches
		Set<String> intersection = null;
		for (int idx : idxs) {
			IrBGP br = u.getBranches().get(idx - 1);
			Set<String> names = collectAnonNamesFromPathTripleBranch(br);
			if (names.isEmpty()) {
				return false;
			}
			if (intersection == null) {
				intersection = new HashSet<>(names);
			} else {
				intersection.retainAll(names);
				if (intersection.isEmpty()) {
					return false;
				}
			}
		}
		return intersection != null && !intersection.isEmpty();
	}

	private static Set<String> collectAnonNamesFromPathTripleBranch(IrBGP b) {
		Set<String> out = new HashSet<>();
		if (b == null || b.getLines().size() != 1) {
			return out;
		}
		IrNode only = b.getLines().get(0);
		if (only instanceof IrGraph) {
			IrGraph g = (IrGraph) only;
			if (g.getWhere() == null || g.getWhere().getLines().size() != 1) {
				return out;
			}
			only = g.getWhere().getLines().get(0);
		}
		if (only instanceof IrPathTriple) {
			IrPathTriple pt = (IrPathTriple) only;
			Var s = pt.getSubject();
			Var o = pt.getObject();
			if (isAnonPathVar(s) || isAnonPathInverseVar(s)) {
				out.add(s.getName());
			}
			if (isAnonPathVar(o) || isAnonPathInverseVar(o)) {
				out.add(o.getName());
			}
			Set<Var> pvs = pt.getPathVars();
			if (pvs != null) {
				for (Var v : pvs) {
					if (v != null && !v.hasValue() && v.getName() != null
							&& (v.getName().startsWith(ANON_PATH_PREFIX)
									|| v.getName().startsWith(ANON_PATH_INVERSE_PREFIX))) {
						out.add(v.getName());
					}
				}
			}
		}
		return out;
	}

	/**
	 * Conservative safety predicate: all selected UNION branches correspond to a single simple path expression
	 * (IrPathTriple or IrStatementPattern converted to a path step), without quantifiers. This is approximated by
	 * checking that the precomputed {@code pathTexts} entry for each branch index is non-null, because earlier in
	 * {@link #fuseUnion(IrUnion, TupleExprIRRenderer)} we only populate {@code pathTexts} when a branch is a single
	 * PT/SP (optionally GRAPH-wrapped) and exclude any that end with '?', '*' or '+'. Endpoints and graph equality are
	 * guaranteed by the grouping key used for {@code idxs}.
	 */
	private static boolean branchesFormSafeAlternation(List<Integer> idxs, List<String> pathTexts) {
		if (idxs == null || idxs.size() < 2) {
			return false;
		}
		for (int idx : idxs) {
			if (idx <= 0 || idx >= pathTexts.size()) {
				return false;
			}
			String p = pathTexts.get(idx);
			if (p == null) {
				return false;
			}
		}
		return true;
	}

	private static IrBGP wrap(IrPathTriple pt) {
		IrBGP b = new IrBGP(false);
		b.add(pt);
		return b;
	}

	private static List<String> splitTopLevelAlternation(String path) {
		ArrayList<String> out = new ArrayList<>();
		if (path == null) {
			return out;
		}
		String s = path.trim();
		if (PathTextUtils.isWrapped(s)) {
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
