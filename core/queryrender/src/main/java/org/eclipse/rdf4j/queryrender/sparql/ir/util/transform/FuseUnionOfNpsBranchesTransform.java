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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrValues;

/**
 * Fuse a UNION whose branches are each a single bare-NPS path triple (optionally inside the same GRAPH) into a single
 * NPS triple that combines members, preserving forward orientation and inverting members from inverse-oriented branches
 * (using '^') when needed.
 *
 * Scope/safety rules: - No new scope (u.isNewScope() == false): merge only when each branch contains an _anon_path_*
 * bridge var (see BaseTransform.unionBranchesAllHaveAnonPathBridge). This ensures we do not collapse user-visible
 * variables. - New scope (u.isNewScope() == true): by default do not merge. Special exception: merge when the branches
 * share a common _anon_path_* variable name (see BaseTransform.unionBranchesShareCommonAnonPathVarName). In that case
 * we preserve explicit grouping by wrapping the fused result in a grouped IrBGP.
 *
 * Additional constraints: - Each branch must be a single IrPathTriple, optionally GRAPH-wrapped with an identical graph
 * ref. - Each path must be a bare NPS '!(...)' (no '/', no quantifiers). Orientation is aligned by inverting members
 * when the branch is reversed. - Member order is kept stable; duplicates are removed while preserving first occurrence.
 */
public final class FuseUnionOfNpsBranchesTransform extends BaseTransform {

	private FuseUnionOfNpsBranchesTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			// Do not fuse UNIONs at top-level; only fuse within EXISTS bodies (handled below)
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				// Recurse into the GRAPH body and fuse UNION-of-NPS locally inside the GRAPH when eligible.
				IrBGP inner = apply(g.getWhere(), r);
				inner = fuseUnionsInBGP(inner);
				m = new IrGraph(g.getGraph(), inner, g.isNewScope());
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
				IrBGP inner = apply(s.getWhere(), r);
				inner = fuseUnionsInBGP(inner);
				m = new IrService(s.getServiceRefText(), s.isSilent(), inner, s.isNewScope());
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			} else if (n instanceof IrFilter) {
				// Recurse into EXISTS bodies and allow fusing inside them
				IrFilter f = (IrFilter) n;
				IrNode body = f.getBody();
				if (body instanceof IrExists) {
					IrExists ex = (IrExists) body;
					IrFilter nf = new IrFilter(new IrExists(applyInsideExists(ex.getWhere(), r), ex.isNewScope()),
							f.isNewScope());
					m = nf;
				} else {
					m = n.transformChildren(child -> {
						if (child instanceof IrBGP) {
							return apply((IrBGP) child, r);
						}
						return child;
					});
				}
			} else if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				// Do not fuse UNIONs at the top-level here; limit fusion to EXISTS/SERVICE contexts
				// handled by dedicated passes to avoid altering expected top-level UNION shapes.
				IrUnion u2 = new IrUnion(u.isNewScope());
				boolean parentHasValues = branchHasTopLevelValues(bgp);
				for (IrBGP b : u.getBranches()) {
					if (parentHasValues || branchHasTopLevelValues(b)) {
						// Apply recursively but avoid NPS-union fusing inside GRAPH bodies for this branch
						IrBGP nb = new IrBGP(b.isNewScope());
						for (IrNode ln2 : b.getLines()) {
							if (ln2 instanceof IrGraph) {
								IrGraph g2 = (IrGraph) ln2;
								IrBGP inner = apply(g2.getWhere(), r);
								// intentionally skip fuseUnionsInBGP(inner)
								nb.add(new IrGraph(g2.getGraph(), inner, g2.isNewScope()));
							} else if (ln2 instanceof IrBGP) {
								nb.add(apply((IrBGP) ln2, r));
							} else {
								nb.add(ln2.transformChildren(child -> {
									if (child instanceof IrBGP) {
										return apply((IrBGP) child, r);
									}
									return child;
								}));
							}
						}
						u2.addBranch(nb);
					} else {
						u2.addBranch(apply(b, r));
					}
				}
				m = u2;
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
		final IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		return res;
	}

	private static IrBGP fuseUnionsInBGP(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		boolean containsValues = false;
		for (IrNode ln0 : bgp.getLines()) {
			if (ln0 instanceof IrValues) {
				containsValues = true;
				break;
			}
		}
		for (IrNode ln : bgp.getLines()) {
			if (!containsValues && ln instanceof IrUnion) {
				IrUnion u = (IrUnion) ln;
				IrNode fused = tryFuseUnion(u);
				// Preserve explicit new-scope grouping braces when present; only unwrap
				// synthetic single-child groups that do not carry new scope.
				if (fused instanceof IrBGP) {
					IrBGP grp = (IrBGP) fused;
					if (!grp.isNewScope()) {
						List<IrNode> ls = grp.getLines();
						if (ls != null && ls.size() == 1) {
							fused = ls.get(0);
						}
					}
				}
				out.add(fused);
			} else if (ln instanceof IrGraph) {
				IrGraph g = (IrGraph) ln;
				out.add(new IrGraph(g.getGraph(), fuseUnionsInBGP(g.getWhere()), g.isNewScope()));
			} else if (ln instanceof IrOptional) {
				IrOptional o = (IrOptional) ln;
				IrOptional no = new IrOptional(fuseUnionsInBGP(o.getWhere()), o.isNewScope());
				no.setNewScope(o.isNewScope());
				out.add(no);
			} else if (ln instanceof IrMinus) {
				IrMinus mi = (IrMinus) ln;
				out.add(new IrMinus(fuseUnionsInBGP(mi.getWhere()), mi.isNewScope()));
			} else if (ln instanceof IrService) {
				IrService s = (IrService) ln;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), fuseUnionsInBGP(s.getWhere()),
						s.isNewScope()));
			} else if (ln instanceof IrBGP) {
				// Recurse into nested groups
				out.add(fuseUnionsInBGP((IrBGP) ln));
			} else {
				out.add(ln);
			}
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		return res;
	}

	private static boolean branchHasTopLevelValues(IrBGP b) {
		if (b == null) {
			return false;
		}
		for (IrNode ln : b.getLines()) {
			if (ln instanceof IrValues) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Try to fuse a UNION of bare-NPS path triples according to the scope/safety rules described above.
	 */
	private static IrNode tryFuseUnion(IrUnion u) {
		if (u == null || u.getBranches().size() < 2) {
			return u;
		}
		// Track whether this UNION originated from an explicit user grouping that introduced
		// a new scope. If we fuse such a UNION, we preserve the explicit braces by wrapping
		// the fused result in a grouped IrBGP (see callers for context-specific unwrapping).
		final boolean wasNewScope = u.isNewScope();

		// Gather candidate branches: (optional GRAPH g) { IrPathTriple with bare NPS }.
		Var graphRef = null;
		boolean graphRefNewScope = false;
		boolean innerBgpNewScope = false;
		Var sCanon = null;
		Var oCanon = null;
		IrPathTriple firstPt = null;
		final List<String> members = new ArrayList<>();
		int fusedCount = 0;

		for (IrBGP b : u.getBranches()) {
			// Unwrap common single-child wrappers to reach a path triple, and capture graph ref if present.
			Var g = null;
			boolean gNewScope = false;
			boolean whereNewScope = false;
			IrNode node = singleChild(b);
			// unwrap nested single-child BGPs introduced for explicit grouping
			while (node instanceof IrBGP) {
				IrNode inner = singleChild((IrBGP) node);
				if (inner == null) {
					break;
				}
				node = inner;
			}
			if (node instanceof IrGraph) {
				IrGraph gb = (IrGraph) node;
				g = gb.getGraph();
				gNewScope = gb.isNewScope();
				whereNewScope = gb.getWhere() != null && gb.getWhere().isNewScope();
				node = singleChild(gb.getWhere());
				while (node instanceof IrBGP) {
					IrNode inner = singleChild((IrBGP) node);
					if (inner == null) {
						break;
					}
					node = inner;
				}
			}
			// allow one more level of single-child BGP (explicit grouping)
			if (node instanceof IrBGP) {
				node = singleChild((IrBGP) node);
			}
			IrPathTriple pt = (node instanceof IrPathTriple) ? (IrPathTriple) node : null;
			if (pt == null) {
				return u; // non-candidate branch
			}
			final String rawPath = pt.getPathText() == null ? null : pt.getPathText().trim();
			final String path = BaseTransform.normalizeCompactNps(rawPath);
			if (path == null || !path.startsWith("!(") || !path.endsWith(")") || path.indexOf('/') >= 0
					|| path.endsWith("?") || path.endsWith("+") || path.endsWith("*")) {
				return u; // not a bare NPS
			}

			// Initialize canonical orientation from first branch
			if (sCanon == null && oCanon == null) {
				sCanon = pt.getSubject();
				oCanon = pt.getObject();
				firstPt = pt;
				graphRef = g;
				graphRefNewScope = gNewScope;
				innerBgpNewScope = whereNewScope;
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
			if (sameVarOrValue(sCanon, pt.getObject()) && sameVarOrValue(oCanon, pt.getSubject())) {
				String inv = invertNegatedPropertySet(path);
				if (inv == null) {
					return u; // be safe
				}
				toAdd = inv;
			} else if (!(sameVarOrValue(sCanon, pt.getSubject()) && sameVarOrValue(oCanon, pt.getObject()))) {
				return u; // endpoints mismatch
			}

			addMembers(toAdd, members);
			fusedCount++;
		}

		if (fusedCount >= 2 && !members.isEmpty()) {
			// Safety gates:
			// - No new scope: require anon-path bridge vars present in every branch.
			// - New scope: require a common _anon_path_* variable across branches in allowed roles.
			if (wasNewScope) {
				final boolean allowedByCommonAnon = unionBranchesShareAnonPathVarWithAllowedRoleMapping(u);
				if (!allowedByCommonAnon) {
					unionBranchesShareAnonPathVarWithAllowedRoleMapping(u);
					return u;
				}
			} else {
				final boolean allHaveAnon = unionBranchesAllHaveAnonPathBridge(u);
				if (!allHaveAnon) {
					return u;
				}
			}
			final String merged = "!(" + String.join("|", members) + ")";
			IrPathTriple mergedPt = new IrPathTriple(sCanon,
					firstPt == null ? null : firstPt.getSubjectOverride(), merged, oCanon,
					firstPt == null ? null : firstPt.getObjectOverride(),
					firstPt == null ? java.util.Collections.emptySet() : firstPt.getPathVars(), false);
			IrNode fused;
			if (graphRef != null) {
				IrBGP inner = new IrBGP(innerBgpNewScope);
				inner.add(mergedPt);
				fused = new IrGraph(graphRef, inner, graphRefNewScope);
			} else {
				fused = mergedPt;
			}
			if (wasNewScope) {
				// Wrap in an extra group to preserve explicit braces that existed around the UNION branches
				IrBGP grp = new IrBGP(true);
				grp.add(fused);
				grp.setNewScope(true);
				return grp;
			}
			return fused;
		}
		return u;
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

	/** Apply union-of-NPS fusing only within EXISTS bodies. */
	private static IrBGP applyInsideExists(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrUnion) {
				m = tryFuseUnion((IrUnion) n);
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), applyInsideExists(g.getWhere(), r), g.isNewScope());
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no2 = new IrOptional(applyInsideExists(o.getWhere(), r), o.isNewScope());
				no2.setNewScope(o.isNewScope());
				m = no2;
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(applyInsideExists(mi.getWhere(), r), mi.isNewScope());
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), applyInsideExists(s.getWhere(), r),
						s.isNewScope());
			} else if (n instanceof IrSubSelect) {
				// keep
			} else if (n instanceof IrFilter) {
				IrFilter f = (IrFilter) n;
				IrNode body = f.getBody();
				if (body instanceof IrExists) {
					IrExists ex = (IrExists) body;
					IrFilter nf = new IrFilter(new IrExists(applyInsideExists(ex.getWhere(), r), ex.isNewScope()),
							f.isNewScope());
					m = nf;
				}
			}
			out.add(m);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		return res;
	}

	private static void addMembers(String npsPath, List<String> out) {
		// npsPath assumed to be '!(...)'
		int start = npsPath.indexOf('(');
		int end = npsPath.lastIndexOf(')');
		if (start < 0 || end < 0 || end <= start) {
			return;
		}
		String inner = npsPath.substring(start + 1, end);
		for (String tok : inner.split("\\|")) {
			String t = tok.trim();
			if (!t.isEmpty()) {
				out.add(t);
			}
		}
	}

	// compact NPS normalization centralized in BaseTransform
}
