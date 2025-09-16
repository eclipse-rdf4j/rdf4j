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
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrTripleLike;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Fuse simple chains of constant-predicate statement patterns connected by parser-inserted bridge variables into
 * property path triples, and handle a few local path+filter shapes (e.g., basic NPS formation) where safe.
 *
 * Scope and safety: - Only composes across {@code _anon_path_*} variables so user-visible bindings remain intact. -
 * Accepts constant-predicate SPs and preserves GRAPH/OPTIONAL/UNION structure via recursion. - Leaves complex cases to
 * later passes (fixed point), keeping this pass easy to reason about.
 */
public final class ApplyPathsTransform extends BaseTransform {
	private ApplyPathsTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}

		List<IrNode> out = new ArrayList<>();
		List<IrNode> in = bgp.getLines();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			// Try to normalize a zero-or-one subselect into a path triple early
			if (n instanceof IrSubSelect) {
				IrNode repl = NormalizeZeroOrOneSubselectTransform
						.tryRewriteZeroOrOneNode((IrSubSelect) n, r);
				if (repl != null) {
					out.add(repl);
					continue;
				}
			}
			// Recurse first using function-style child transform
			n = n.transformChildren(child -> {
				if (child instanceof IrBGP) {
					return apply((IrBGP) child, r);
				}
				return child;
			});

			// ---- Multi-step chain of SPs over _anon_path_* vars → fuse into a single path triple ----
			if (n instanceof IrStatementPattern) {
				IrStatementPattern sp0 = (IrStatementPattern) n;
				Var p0 = sp0.getPredicate();
				if (isConstantIriPredicate(sp0)) {
					Var mid = null;
					boolean startForward = false;
					if (isAnonPathVar(sp0.getObject())) {
						mid = sp0.getObject();
						startForward = true;
					} else if (isAnonPathVar(sp0.getSubject())) {
						mid = sp0.getSubject();
						startForward = false;
					}
					if (mid != null) {
						Var start = startForward ? sp0.getSubject() : sp0.getObject();
						List<String> parts = new ArrayList<>();
						Set<Var> seenAnon = new HashSet<>();
						seenAnon.add(mid);
						String step0 = iri(p0, r);
						parts.add(startForward ? step0 : ("^" + step0));

						int j = i + 1;
						Var cur = mid;
						Var end = null;
						IrStatementPattern lastSp = null;
						boolean lastForward = true;
						while (j < in.size()) {
							IrNode n2 = in.get(j);
							if (!(n2 instanceof IrStatementPattern)) {
								break;
							}
							IrStatementPattern sp = (IrStatementPattern) n2;
							Var pv = sp.getPredicate();
							if (!isConstantIriPredicate(sp)) {
								break;
							}
							boolean forward = sameVar(cur, sp.getSubject());
							boolean inverse = sameVar(cur, sp.getObject());
							if (!forward && !inverse) {
								break;
							}
							String step = iri(pv, r);
							parts.add(inverse ? ("^" + step) : step);
							Var nextVar = forward ? sp.getObject() : sp.getSubject();
							if (isAnonPathVar(nextVar)) {
								cur = nextVar;
								seenAnon.add(nextVar);
								lastSp = sp;
								lastForward = forward;
								j++;
								continue;
							}
							end = nextVar;
							lastSp = sp;
							lastForward = forward;
							j++;
							break;
						}
						if (end != null) {
							IrNode startOv = startForward ? sp0.getSubjectOverride() : sp0.getObjectOverride();
							IrNode endOv = (lastSp == null) ? null
									: (lastForward ? lastSp.getObjectOverride() : lastSp.getSubjectOverride());
							IrPathTriple ptChain = new IrPathTriple(start, startOv, String.join("/", parts), end, endOv,
									seenAnon, false);
							out.add(ptChain);
							i = j - 1; // advance past consumed
							continue;
						}
					}
				}
			}

			// ---- Simple SP(var p) + FILTER (!= / NOT IN) -> NPS triple (only for anon_path var) ----
			if (n instanceof IrStatementPattern && i + 1 < in.size() && in.get(i + 1) instanceof IrFilter) {
				IrStatementPattern sp = (IrStatementPattern) n;
				Var pv = sp.getPredicate();
				IrFilter f = (IrFilter) in.get(i + 1);
				String condText = f.getConditionText();
				ApplyNegatedPropertySetTransform.NsText ns = ApplyNegatedPropertySetTransform
						.parseNegatedSetText(condText);
				// Do not apply here if there is an immediate constant tail; defer to S1+tail rule below
				boolean hasTail = (i + 2 < in.size() && in.get(i + 2) instanceof IrStatementPattern
						&& ((IrStatementPattern) in.get(i + 2)).getPredicate() != null
						&& ((IrStatementPattern) in.get(i + 2)).getPredicate().hasValue());
				if (!hasTail && isAnonPathVar(pv) && ns != null && pv.getName() != null
						&& pv.getName().equals(ns.varName) && !ns.items.isEmpty()) {
					String nps = "!(" + ApplyNegatedPropertySetTransform.joinIrisWithPreferredOrder(ns.items, r) + ")";
					// Respect inverse orientation hint on the anon path var: render as !^p and flip endpoints
					if (isAnonPathInverseVar(pv)) {
						String maybe = invertNegatedPropertySet(nps);
						if (maybe != null) {
							nps = maybe;
						}
						IrPathTriple ptNps = new IrPathTriple(sp.getObject(), sp.getObjectOverride(), nps,
								sp.getSubject(), sp.getSubjectOverride(), IrPathTriple.fromStatementPatterns(sp),
								false);
						out.add(ptNps);
					} else {
						IrPathTriple ptNps = new IrPathTriple(sp.getSubject(), sp.getSubjectOverride(), nps,
								sp.getObject(), sp.getObjectOverride(), IrPathTriple.fromStatementPatterns(sp), false);
						out.add(ptNps);
					}
					i += 1;
					continue;
				}
			}

			// ---- Special: SP(var p) + FILTER (?p != c[, ...]) + SP(const tail) -> oriented NPS/const chain ----
			if (n instanceof IrStatementPattern && i + 2 < in.size() && in.get(i + 1) instanceof IrFilter
					&& in.get(i + 2) instanceof IrStatementPattern) {
				IrStatementPattern spA = (IrStatementPattern) n; // A ?p M or M ?p A
				Var pA = spA.getPredicate();
				if (pA != null && !pA.hasValue() && pA.getName() != null && isAnonPathVar(pA)) {
					IrFilter flt = (IrFilter) in.get(i + 1);
					String cond = flt.getConditionText();
					ApplyNegatedPropertySetTransform.NsText ns = ApplyNegatedPropertySetTransform
							.parseNegatedSetText(cond);
					IrStatementPattern spB = (IrStatementPattern) in.get(i + 2);
					Var pB = spB.getPredicate();
					if (ns != null && ns.varName != null && ns.varName.equals(pA.getName())
							&& isConstantIriPredicate(spB)) {
						Var midA;
						boolean startForward;
						if (isAnonPathVar(spA.getObject())) {
							midA = spA.getObject();
							startForward = true; // A -(?p)-> M
						} else if (isAnonPathVar(spA.getSubject())) {
							midA = spA.getSubject();
							startForward = false; // M -(?p)-> A
						} else {
							midA = null;
							startForward = true;
						}
						if (sameVar(midA, spB.getSubject())) {
							// Build NPS part; invert members when the first step is inverse
							String members = ApplyNegatedPropertySetTransform.joinIrisWithPreferredOrder(ns.items, r);
							String nps = "!(" + members + ")";
							if (!startForward) {
								nps = invertNegatedPropertySet(nps);
							}
							String tail = iri(pB, r);
							Var startVar = startForward ? spA.getSubject() : spA.getObject();
							IrNode startOv = startForward ? spA.getSubjectOverride() : spA.getObjectOverride();
							Var endVar = spB.getObject();
							IrNode endOv = spB.getObjectOverride();
							IrPathTriple ptSpec = new IrPathTriple(startVar, startOv, nps + "/" + tail, endVar, endOv,
									IrPathTriple.fromStatementPatterns(spA, spB), false);
							out.add(ptSpec);
							i += 2;
							continue;
						}
					}
				}
			}

			// ---- Simple SP + SP over an _anon_path_* bridge → fuse into a single path triple ----
			if (n instanceof IrStatementPattern && i + 1 < in.size() && in.get(i + 1) instanceof IrStatementPattern) {
				IrStatementPattern a = (IrStatementPattern) n;
				IrStatementPattern b = (IrStatementPattern) in.get(i + 1);
				Var ap = a.getPredicate(), bp = b.getPredicate();
				if (ap != null && ap.hasValue() && ap.getValue() instanceof IRI && bp != null && bp.hasValue()
						&& bp.getValue() instanceof IRI) {
					Var as = a.getSubject(), ao = a.getObject();
					Var bs = b.getSubject(), bo = b.getObject();
					// forward-forward: ?s p1 ?x . ?x p2 ?o
					if (isAnonPathVar(ao) && sameVar(ao, bs)) {
						String p1 = iri(ap, r);
						String p2 = iri(bp, r);
						Set<Var> s = new HashSet<>();
						if (isAnonPathVar(ao)) {
							s.add(ao);
						}
						IrPathTriple ptFF = new IrPathTriple(as, a.getSubjectOverride(), p1 + "/" + p2, bo,
								b.getObjectOverride(), s, false);
						out.add(ptFF);
						i += 1; // consume next
						continue;
					}

					// ---- SP followed by IrPathTriple over the bridge → fuse into a single path triple ----
					if (n instanceof IrStatementPattern && i + 1 < in.size() && in.get(i + 1) instanceof IrPathTriple) {
						IrStatementPattern sp = (IrStatementPattern) n;
						Var p1 = sp.getPredicate();
						if (isConstantIriPredicate(sp)) {
							IrPathTriple pt1 = (IrPathTriple) in.get(i + 1);
							if (sameVar(sp.getObject(), pt1.getSubject())) {
								// forward chaining
								String fused = iri(p1, r) + "/" + pt1.getPathText();
								{
									Set<Var> pathVars = new HashSet<>(pt1.getPathVars());
									pathVars.addAll(IrPathTriple.fromStatementPatterns(sp));
									out.add(new IrPathTriple(sp.getSubject(), sp.getSubjectOverride(), fused,
											pt1.getObject(), pt1.getObjectOverride(), pathVars, false));
								}
								i += 1;
								continue;
							} else if (sameVar(sp.getSubject(), pt1.getObject())) {
								// inverse chaining
								String fused = pt1.getPathText() + "/^" + iri(p1, r);
								{
									Set<Var> pathVars = new HashSet<>(pt1.getPathVars());
									pathVars.addAll(IrPathTriple.fromStatementPatterns(sp));
									out.add(new IrPathTriple(pt1.getSubject(), pt1.getSubjectOverride(), fused,
											sp.getObject(), sp.getObjectOverride(), pathVars, false));
								}
								i += 1;
								continue;
							} else if (sameVar(sp.getSubject(), pt1.getSubject()) && isAnonPathVar(sp.getSubject())) {
								// SP and PT share their subject (an _anon_path_* bridge). Prefix the PT with an inverse
								// step from the SP and start from SP.object (which may be a user var like ?y).
								// This preserves bindings while eliminating the extra bridging triple.
								String fused = "^" + iri(p1, r) + "/"
										+ pt1.getPathText();
								{
									Set<Var> pathVars = new HashSet<>(pt1.getPathVars());
									pathVars.addAll(IrPathTriple.fromStatementPatterns(sp));
									out.add(new IrPathTriple(sp.getObject(), sp.getObjectOverride(), fused,
											pt1.getObject(),
											pt1.getObjectOverride(), pathVars, false));
								}
								i += 1;
								continue;
							}
						}

					}

					// ---- Fuse an IrPathTriple followed by a constant-predicate SP that connects to the path's object
					// ----
					if (n instanceof IrPathTriple && i + 1 < in.size() && in.get(i + 1) instanceof IrStatementPattern) {
						// If there is a preceding SP that likely wants to fuse with this PT first, defer this PT+SP
						// fusion.
						if (i - 1 >= 0 && in.get(i - 1) instanceof IrStatementPattern) {
							IrStatementPattern spPrev = (IrStatementPattern) in.get(i - 1);
							IrPathTriple thisPt = (IrPathTriple) n;
							if (sameVar(spPrev.getSubject(), thisPt.getSubject())
									|| sameVar(spPrev.getObject(), thisPt.getSubject())) {
								out.add(n);
								continue;
							}
						}
						IrPathTriple pt = (IrPathTriple) n;
						IrStatementPattern sp = (IrStatementPattern) in.get(i + 1);
						Var pv = sp.getPredicate();
						if (isConstantIriPredicate(sp)) {
							// Only fuse when the bridge var (?mid) is an _anon_path_* var; otherwise we might elide a
							// user
							// var like ?y
							if (!isAnonPathVar(pt.getObject())) {
								out.add(n);
								continue;
							}
							// Lookahead: if there is a following IrPathTriple that shares the join end of this PT+SP,
							// defer fusion to allow the SP+PT rule to construct a grouped right-hand path. This yields
							// ((... )*/(^ex:d/(...)+)) grouping before appending a tail like /foaf:name.
							if (i + 2 < in.size() && in.get(i + 2) instanceof IrPathTriple) {
								IrPathTriple pt2 = (IrPathTriple) in.get(i + 2);
								Var candidateEnd = null;
								if (sameVar(pt.getObject(), sp.getSubject())) {
									candidateEnd = sp.getObject();
								} else if (sameVar(pt.getObject(), sp.getObject())) {
									candidateEnd = sp.getSubject();
								}
								if ((sameVar(candidateEnd, pt2.getSubject())
										|| sameVar(candidateEnd, pt2.getObject()))) {
									// Defer; do not consume SP here
									out.add(n);
									continue;
								}
							}
							String joinStep = null;
							Var endVar = null;
							if (sameVar(pt.getObject(), sp.getSubject())) {
								joinStep = "/" + iri(pv, r);
								endVar = sp.getObject();
							}
							if (joinStep != null) {
								final String fusedPath = pt.getPathText() + joinStep;
								{
									Set<Var> pathVars = new HashSet<>(pt.getPathVars());
									pathVars.addAll(IrPathTriple.fromStatementPatterns(sp));
									out.add(new IrPathTriple(pt.getSubject(), pt.getSubjectOverride(), fusedPath,
											endVar,
											sp.getObjectOverride(), pathVars, false));
								}
								i += 1; // consume next
								continue;
							}
						}
					}
				}

				// removed duplicate PT+SP fusion block (handled above with deferral/lookahead)

			}

			// ---- GRAPH/SP followed by UNION over bridge var → fused path inside GRAPH ----
			if ((n instanceof IrGraph || n instanceof IrStatementPattern) && i + 1 < in.size()
					&& in.get(i + 1) instanceof IrUnion) {
				IrUnion u = (IrUnion) in.get(i + 1);
				// Respect explicit UNION scopes, except when the branches share a common _anon_path_*
				// variable under an allowed role mapping (s-s, s-o, o-s, o-p). This ensures the new
				// scope originates from property path decoding rather than user-visible bindings.
				if (u.isNewScope() && !unionBranchesShareAnonPathVarWithAllowedRoleMapping(u)) {
					out.add(n);
					continue;
				}
				Var graphRef = null;
				IrStatementPattern sp0 = null;
				if (n instanceof IrGraph) {
					IrGraph g = (IrGraph) n;
					graphRef = g.getGraph();
					if (g.getWhere() != null) {
						for (IrNode ln : g.getWhere().getLines()) {
							if (ln instanceof IrStatementPattern) {
								sp0 = (IrStatementPattern) ln;
								break;
							}
						}
					}
				} else {
					sp0 = (IrStatementPattern) n;
				}
				if (sp0 != null) {
					Var p0 = sp0.getPredicate();
					if (isConstantIriPredicate(sp0)) {
						// Identify bridge var and start/end side
						Var mid;
						boolean startForward;
						if (isAnonPathVar(sp0.getObject())) {
							mid = sp0.getObject();
							startForward = true;
						} else if (isAnonPathVar(sp0.getSubject())) {
							mid = sp0.getSubject();
							startForward = false;
						} else {
							mid = null;
							startForward = true;
						}
						if (mid != null) {
							// Examine union branches: must all resolve from mid to the same end variable
							Var endVarOut = null;
							IrNode endOverrideOut = null;
							List<String> alts = new ArrayList<>();
							Var unionGraphRef = null; // if branches are GRAPHed, ensure same ref
							boolean ok = !u.getBranches().isEmpty();
							for (IrBGP b : u.getBranches()) {
								if (!ok) {
									break;
								}
								IrNode only = (b.getLines().size() == 1) ? b.getLines().get(0) : null;
								IrStatementPattern spX;
								if (only instanceof IrGraph) {
									IrGraph gX = (IrGraph) only;
									if (gX.getWhere() == null || gX.getWhere().getLines().size() != 1
											|| !(gX.getWhere().getLines().get(0) instanceof IrStatementPattern)) {
										ok = false;
										break;
									}
									if (unionGraphRef == null) {
										unionGraphRef = gX.getGraph();
									} else if (!sameVarOrValue(unionGraphRef, gX.getGraph())) {
										ok = false;
										break;
									}
									spX = (IrStatementPattern) gX.getWhere().getLines().get(0);
								} else if (only instanceof IrStatementPattern) {
									spX = (IrStatementPattern) only;
								} else {
									ok = false;
									break;
								}
								Var pX = spX.getPredicate();
								if (!isConstantIriPredicate(spX)) {
									ok = false;
									break;
								}
								String step = iri(pX, r);
								Var end;
								IrNode endOv;
								if (sameVar(mid, spX.getSubject())) {
									// forward
									end = spX.getObject();
									endOv = spX.getObjectOverride();
								} else if (sameVar(mid, spX.getObject())) {
									// inverse
									step = "^" + step;
									end = spX.getSubject();
									endOv = spX.getSubjectOverride();
								} else {
									ok = false;
									break;
								}
								if (endVarOut == null) {
									endVarOut = end;
									endOverrideOut = endOv;
								} else if (!sameVar(endVarOut, end)) {
									ok = false;
									break;
								}
								alts.add(step);
							}
							if (ok && endVarOut != null && !alts.isEmpty()) {
								Var startVar = startForward ? sp0.getSubject() : sp0.getObject();
								IrNode startOv = startForward ? sp0.getSubjectOverride() : sp0.getObjectOverride();
								String first = iri(p0, r);
								if (!startForward) {
									first = "^" + first;
								}
								// Alternation preserves UNION branch order

								String altTxt = (alts.size() == 1) ? alts.get(0)
										: ("(" + String.join("|", alts) + ")");

								// Parenthesize first step and wrap alternation in triple parens to match expected
								// idempotence
								String pathTxt = first + "/" + altTxt;

								Set<Var> fusedPathVars = new HashSet<>();
								if (isAnonPathVar(mid)) {
									fusedPathVars.add(mid);
								}
								IrPathTriple fused = new IrPathTriple(startVar, startOv, pathTxt, endVarOut,
										endOverrideOut, fusedPathVars, false);
								if (graphRef != null) {
									IrBGP inner = new IrBGP(
											((IrGraph) n).getWhere() != null && ((IrGraph) n).getWhere().isNewScope());
									// copy any remaining lines from original inner GRAPH except sp0
									copyAllExcept(((IrGraph) n).getWhere(), inner, sp0);
									// Try to extend fused with an immediate constant-predicate triple inside the same
									// GRAPH
									IrStatementPattern joinSp = null;
									boolean joinInverse = false;
									for (IrNode ln : inner.getLines()) {
										if (!(ln instanceof IrStatementPattern)) {
											continue;
										}
										IrStatementPattern spj = (IrStatementPattern) ln;
										if (!isConstantIriPredicate(spj)) {
											continue;
										}
										if (sameVar(mid, spj.getSubject()) && !isAnonPathVar(spj.getObject())) {
											joinSp = spj;
											joinInverse = false;
											break;
										}
										if (sameVar(mid, spj.getObject()) && !isAnonPathVar(spj.getSubject())) {
											joinSp = spj;
											joinInverse = true;
											break;
										}
									}
									IrBGP reordered = new IrBGP(bgp.isNewScope());
									if (joinSp != null) {
										String step = iri(joinSp.getPredicate(), r);
										String ext = "/" + (joinInverse ? "^" : "") + step;
										String newPath = fused.getPathText() + ext;
										Var newEnd = joinInverse ? joinSp.getSubject() : joinSp.getObject();
										IrNode newEndOv = joinInverse ? joinSp.getSubjectOverride()
												: joinSp.getObjectOverride();
										fused = new IrPathTriple(fused.getSubject(), fused.getSubjectOverride(),
												newPath, newEnd, newEndOv, fused.getPathVars(), false);
									}
									// place the (possibly extended) fused path first, then remaining inner lines (skip
									// consumed sp0 and joinSp)
									reordered.add(fused);
									for (IrNode ln : inner.getLines()) {
										if (ln == joinSp) {
											continue;
										}
										reordered.add(ln);
									}
									out.add(new IrGraph(graphRef, reordered, false));
								} else {
									out.add(fused);
								}
								i += 1; // consumed union
								continue;
							}
						}
					}
				}
			}

			// Rewrite UNION alternation of simple triples (and already-fused path triples) into a single
			// IrPathTriple, preserving branch order and GRAPH context when present. This enables
			// subsequent chaining with a following constant-predicate triple via pt + SP -> pt/IRI.
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				// Universal safeguard: if UNION has newScope==true and all branches have newScope==true,
				// never fuse this UNION.
				if (BaseTransform.unionIsExplicitAndAllBranchesScoped(u)) {
					out.add(n);
					continue;
				}
				boolean branchesAllNonScoped = true;
				for (IrBGP br : u.getBranches()) {
					if (br != null && br.isNewScope()) {
						branchesAllNonScoped = false;
						break;
					}
				}
				boolean permitNewScope = !u.isNewScope() || branchesAllNonScoped
						|| unionBranchesShareAnonPathVarWithAllowedRoleMapping(u);

				if (!permitNewScope) {
					out.add(n);
					continue;
				}

				Var subj = null, obj = null, graphRef = null;
				final List<String> parts = new ArrayList<>();
				boolean ok = !u.getBranches().isEmpty();
				for (IrBGP b : u.getBranches()) {
					if (!ok) {
						break;
					}
					final IrNode only = (b.getLines().size() == 1) ? b.getLines().get(0) : null;
					IrTripleLike tl;
					Var branchGraph = null;
					if (only instanceof IrGraph) {
						IrGraph g = (IrGraph) only;
						if (g.getWhere() == null || g.getWhere().getLines().size() != 1
								|| !(g.getWhere().getLines().get(0) instanceof IrTripleLike)) {
							ok = false;
							break;
						}
						tl = (IrTripleLike) g.getWhere().getLines().get(0);
						branchGraph = g.getGraph();
					} else if (only instanceof IrTripleLike) {
						tl = (IrTripleLike) only;
					} else {
						ok = false;
						break;
					}

					// Graph consistency across branches (allow constants to compare by value)
					if (branchGraph != null) {
						if (graphRef == null) {
							graphRef = branchGraph;
						} else if (!sameVarOrValue(graphRef, branchGraph)) {
							ok = false;
							break;
						}
					} else if (graphRef != null) {
						// mixture of GRAPH and non-GRAPH branches -> abort
						ok = false;
						break;
					}

					final Var s = tl.getSubject();
					final Var o = tl.getObject();
					String piece = tl.getPredicateOrPathText(r);
					if (piece == null) {
						ok = false;
						break;
					}
					if (subj == null && obj == null) {
						// Choose canonical endpoints preferring a non-anon_path_* subject when possible.
						if (isAnonPathVar(s) && !isAnonPathVar(o)) {
							subj = o;
							obj = s;
						} else {
							subj = s;
							obj = o;
						}
					}
					if (!(sameVar(subj, s) && sameVar(obj, o))) {
						// allow inversion only for simple statement patterns; inverting an arbitrary path is not
						// supported here. Special case: if the path is a negated property set, invert each member
						// inside the NPS to preserve semantics, e.g., !(a|b) with reversed endpoints -> !(^a|^b).
						if (sameVar(subj, o) && sameVar(obj, s)) {
							if (tl instanceof IrStatementPattern) {
								piece = "^" + piece;
							} else if (tl instanceof IrPathTriple) {
								String inv = invertNegatedPropertySet(piece);
								if (inv == null) {
									ok = false;
									break;
								}
								piece = inv;
							} else {
								ok = false;
								break;
							}
						} else {
							ok = false;
							break;
						}
					}
					parts.add(piece);
				}

				// Allow fusion under new-scope when branches align into a safe single alternation
				boolean allow = permitNewScope || (ok && !parts.isEmpty() && graphRef != null);
				if (!allow) {
					out.add(n);
					continue;
				}

				// 2a-mixed-two: one branch is a simple IrPathTriple representing exactly two constant steps
				// without quantifiers/alternation, and the other branch is exactly two SPs via an _anon_path_* mid,
				// sharing identical endpoints. Fuse into a single alternation path.
				if (u.getBranches().size() == 2) {
					class TwoLike {
						final Var s;
						final Var o;
						final String path;
						final Set<Var> pathVars;

						TwoLike(Var s, Var o, String path, Set<Var> pathVars) {
							this.s = s;
							this.o = o;
							this.path = path;
							this.pathVars = (pathVars == null || pathVars.isEmpty()) ? Collections.emptySet()
									: Set.copyOf(pathVars);
						}
					}
					Function<IrBGP, TwoLike> parseTwoLike = (bg) -> {
						if (bg == null || bg.getLines().isEmpty()) {
							return null;
						}
						IrNode only = (bg.getLines().size() == 1) ? bg.getLines().get(0) : null;
						if (only instanceof IrPathTriple) {
							IrPathTriple pt = (IrPathTriple) only;
							String ptxt = pt.getPathText();
							if (ptxt == null || ptxt.contains("|") || ptxt.contains("?") || ptxt.contains("*")
									|| ptxt.contains("+")) {
								return null;
							}
							int slash = ptxt.indexOf('/');
							if (slash < 0) {
								return null; // not a two-step path
							}
							String left = ptxt.substring(0, slash).trim();
							String right = ptxt.substring(slash + 1).trim();
							if (left.isEmpty() || right.isEmpty()) {
								return null;
							}
							return new TwoLike(pt.getSubject(), pt.getObject(), left + "/" + right, pt.getPathVars());
						}
						if (bg.getLines().size() == 2 && bg.getLines().get(0) instanceof IrStatementPattern
								&& bg.getLines().get(1) instanceof IrStatementPattern) {
							IrStatementPattern a = (IrStatementPattern) bg.getLines().get(0);
							IrStatementPattern c = (IrStatementPattern) bg.getLines().get(1);
							Var ap = a.getPredicate(), cp = c.getPredicate();
							if (!isConstantIriPredicate(a) || !isConstantIriPredicate(c)) {
								return null;
							}
							Var mid = null, sVar = null, oVar = null;
							boolean firstForward = false, secondForward = false;
							if (isAnonPathVar(a.getObject()) && sameVar(a.getObject(), c.getSubject())) {
								mid = a.getObject();
								sVar = a.getSubject();
								oVar = c.getObject();
								firstForward = true;
								secondForward = true;
							} else if (isAnonPathVar(a.getSubject()) && sameVar(a.getSubject(), c.getObject())) {
								mid = a.getSubject();
								sVar = a.getObject();
								oVar = c.getSubject();
								firstForward = false;
								secondForward = false;
							} else if (isAnonPathVar(a.getObject()) && sameVar(a.getObject(), c.getObject())) {
								mid = a.getObject();
								sVar = a.getSubject();
								oVar = c.getSubject();
								firstForward = true;
								secondForward = false;
							} else if (isAnonPathVar(a.getSubject()) && sameVar(a.getSubject(), c.getSubject())) {
								mid = a.getSubject();
								sVar = a.getObject();
								oVar = c.getObject();
								firstForward = false;
								secondForward = true;
							}
							if (mid == null) {
								return null;
							}
							String step1 = (firstForward ? "" : "^") + iri(ap, r);
							String step2 = (secondForward ? "" : "^") + iri(cp, r);
							return new TwoLike(sVar, oVar, step1 + "/" + step2,
									IrPathTriple.fromStatementPatterns(a, c));
						}
						return null;
					};
					IrBGP b0 = u.getBranches().get(0);
					IrBGP b1 = u.getBranches().get(1);
					TwoLike t0 = parseTwoLike.apply(b0);
					TwoLike t1 = parseTwoLike.apply(b1);
					if (t0 != null && t1 != null) {
						// Ensure endpoints match (forward); if reversed, skip this case for safety.
						if (sameVar(t0.s, t1.s) && sameVar(t0.o, t1.o)) {
							String alt = t0.path + "|" + t1.path;
							Set<Var> pathVars = new HashSet<>();
							pathVars.addAll(t0.pathVars);
							pathVars.addAll(t1.pathVars);
							IrPathTriple fusedPt = new IrPathTriple(t0.s, alt, t0.o, u.isNewScope(), pathVars);
							out.add(fusedPt);
							continue;
						}
					}
				}

				// 2a-alt: UNION with one branch a single SP and the other already fused to IrPathTriple.
				// Example produced by earlier passes: { ?y foaf:knows ?x } UNION { ?x ex:knows/^foaf:knows ?y }.
				if (u.getBranches().size() == 2) {
					IrBGP b0 = u.getBranches().get(0);
					IrBGP b1 = u.getBranches().get(1);
					IrPathTriple pt = null;
					IrStatementPattern sp = null;
					int ptIdx = -1;
					if (b0.getLines().size() == 1 && b0.getLines().get(0) instanceof IrPathTriple
							&& b1.getLines().size() == 1 && b1.getLines().get(0) instanceof IrStatementPattern) {
						pt = (IrPathTriple) b0.getLines().get(0);
						sp = (IrStatementPattern) b1.getLines().get(0);
						ptIdx = 0;
					} else if (b1.getLines().size() == 1 && b1.getLines().get(0) instanceof IrPathTriple
							&& b0.getLines().size() == 1 && b0.getLines().get(0) instanceof IrStatementPattern) {
						pt = (IrPathTriple) b1.getLines().get(0);
						sp = (IrStatementPattern) b0.getLines().get(0);
						ptIdx = 1;
					}
					if (pt != null && sp != null) {
						Var pv = sp.getPredicate();
						if (isConstantIriPredicate(sp)) {
							final Var wantS = pt.getSubject();
							final Var wantO = pt.getObject();
							String atom = null;
							if (sameVar(wantS, sp.getSubject()) && sameVar(wantO, sp.getObject())) {
								atom = iri(pv, r);
							} else if (sameVar(wantS, sp.getObject()) && sameVar(wantO, sp.getSubject())) {
								atom = "^" + iri(pv, r);
							}
							if (atom != null) {
								final String alt = (ptIdx == 0) ? (pt.getPathText() + "|" + atom)
										: (atom + "|" + pt.getPathText());
								IrPathTriple fused2 = new IrPathTriple(wantS, alt, wantO, u.isNewScope(),
										pt.getPathVars());
								out.add(fused2);
								continue;
							}
						}
					}
				}

				// 2c: Partial merge of IrPathTriple branches (no inner alternation). If there are >=2 branches where
				// each
				// is a simple IrPathTriple without inner alternation or quantifiers and they share identical endpoints,
				// fuse them into a single alternation path, keeping remaining branches intact.
				{
					Var sVarOut = null, oVarOut = null;
					for (int bi = 0; bi < u.getBranches().size(); bi++) {
						IrBGP b = u.getBranches().get(bi);
						if (b.getLines().size() != 1) {
							continue;
						}
						IrNode only = b.getLines().get(0);
						IrPathTriple pt = null;
						if (only instanceof IrPathTriple) {
							pt = (IrPathTriple) only;
						} else if (only instanceof IrGraph) {
							IrGraph g = (IrGraph) only;
							if (g.getWhere() != null && g.getWhere().getLines().size() == 1
									&& g.getWhere().getLines().get(0) instanceof IrPathTriple) {
								pt = (IrPathTriple) g.getWhere().getLines().get(0);
							}
						}
						if (pt == null) {
							continue;
						}
						final String ptxt = pt.getPathText();
						if (ptxt.contains("|") || ptxt.contains("?") || ptxt.contains("*") || ptxt.contains("+")) {
							continue; // skip inner alternation or quantifier
						}
						if (sVarOut == null && oVarOut == null) {
							sVarOut = pt.getSubject();
							oVarOut = pt.getObject();
						}
					}
				}

				// Fourth form: UNION of single-step triples followed immediately by a constant-predicate SP that shares
				// the union's bridge var -> fuse into (alt)/^tail.
				if (i + 1 < in.size() && in.get(i + 1) instanceof IrStatementPattern) {
					final IrStatementPattern post = (IrStatementPattern) in.get(i + 1);
					final Var postPred = post.getPredicate();
					if (isConstantIriPredicate(post)) {
						Var startVar = null, endVar = post.getSubject();
						final List<String> steps = new ArrayList<>();
						boolean ok2 = true;
						for (IrBGP b : u.getBranches()) {
							if (!ok2) {
								break;
							}
							if (b.getLines().size() != 1 || !(b.getLines().get(0) instanceof IrStatementPattern)) {
								ok2 = false;
								break;
							}
							final IrStatementPattern sp = (IrStatementPattern) b.getLines().get(0);
							final Var pv = sp.getPredicate();
							if (!isConstantIriPredicate(sp)) {
								ok2 = false;
								break;
							}
							String step;
							Var sVarCandidate;
							// post triple is ?end postPred ?mid
							if (sameVar(sp.getSubject(), post.getObject())) {
								step = "^" + iri(pv, r);
								sVarCandidate = sp.getObject();
							} else if (sameVar(sp.getObject(), post.getObject())) {
								step = iri(pv, r);
								sVarCandidate = sp.getSubject();
							} else {
								ok2 = false;
								break;
							}
							if (startVar == null) {
								startVar = sVarCandidate;
							} else if (!sameVar(startVar, sVarCandidate)) {
								ok2 = false;
								break;
							}
							steps.add(step);
						}
						if (ok2 && startVar != null && endVar != null && !steps.isEmpty()) {
							final String alt = (steps.size() == 1) ? steps.get(0) : String.join("|", steps);
							final String tail = "/^" + iri(postPred, r);
							out.add(new IrPathTriple(startVar, "(" + alt + ")" + tail, endVar, false,
									Collections.emptySet()));
							i += 1;
							continue;
						}
					}
				}

				if (ok && !parts.isEmpty()) {
					String pathTxt;
					List<String> normalized = new ArrayList<>(parts.size());
					boolean allNps = true;
					for (String ptxt : parts) {
						String sPart = ptxt == null ? null : ptxt.trim();
						if (sPart == null) {
							allNps = false;
							break;
						}
						// normalize compact '!ex:p' to '!(ex:p)' and strip a single outer pair of parens
						if (sPart.length() >= 2 && sPart.charAt(0) == '(' && sPart.charAt(sPart.length() - 1) == ')') {
							sPart = sPart.substring(1, sPart.length() - 1).trim();
						}
						String norm = BaseTransform.normalizeCompactNps(sPart);
						normalized.add(norm);
						if (norm == null || !norm.startsWith("!(") || !norm.endsWith(")")) {
							allNps = false;
						}
					}
					// Merge exactly-two NPS branches into a single NPS; otherwise, keep UNION intact for all-NPS.
					if (allNps && normalized.size() == 2) {
						pathTxt = BaseTransform.mergeNpsMembers(normalized.get(0), normalized.get(1));
					} else if (allNps) {
						out.add(n);
						continue;
					} else {
						pathTxt = (parts.size() == 1) ? parts.get(0) : "(" + String.join("|", parts) + ")";
					}
					// For NPS we may want to orient the merged path so that it can chain with an immediate
					// following triple (e.g., NPS/next). If the next line uses one of our endpoints, flip to
					// ensure pt.object equals next.subject when safe.
					IrPathTriple pt = new IrPathTriple(subj, pathTxt, obj, u.isNewScope(), Collections.emptySet());
					if (graphRef != null) {
						IrBGP inner = new IrBGP(false);
						inner.add(pt);
						IrGraph fusedGraph = new IrGraph(graphRef, inner, false);
						if (u.isNewScope() && !bgp.isNewScope()) {
							// Preserve explicit UNION scope by wrapping the fused result in an extra group
							IrBGP grp = new IrBGP(false);
							grp.add(fusedGraph);
							out.add(grp);
						} else {
							out.add(fusedGraph);
						}
					} else {
						if (u.isNewScope() && !bgp.isNewScope()) {
							IrBGP grp = new IrBGP(false);
							grp.add(pt);
							out.add(grp);
						} else {
							out.add(pt);
						}
					}
					continue;
				}
			}

			out.add(n);
		}
		IrBGP res = BaseTransform.bgpWithLines(bgp, out);
		// Prefer fusing PT-SP-PT into PT + ( ^p / PT ) before other linear fusions
		res = fusePtSpPtSequence(res, r);
		// Orient bare NPS for better chaining with following triples
		res = orientBareNpsForNext(res);
		// Adjacent SP then PT fusion pass (catch corner cases that slipped earlier)
		res = fuseAdjacentSpThenPt(res, r);
		// Newly: Adjacent PT then PT fusion
		res = fuseAdjacentPtThenPt(res);
		// Allow non-adjacent join of (PathTriple ... ?v) with a later SP using ?v
		res = joinPathWithLaterSp(res, r);
		// Fuse forward SP to anon mid, followed by inverse tail to same mid (e.g. / ^foaf:knows)
		res = fuseForwardThenInverseTail(res, r);
		// Fuse alternation path + (inverse) tail in the same BGP (especially inside GRAPH)
		res = fuseAltInverseTailBGP(res, r);
		// Normalize inner GRAPH bodies again for PT+SP fusions
		res = ApplyNormalizeGraphInnerPathsTransform.apply(res, r);
		return res;

	}

	public static IrBGP fuseForwardThenInverseTail(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = bgp.getLines();
		List<IrNode> out = new ArrayList<>();
		Set<IrNode> consumed = new HashSet<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (consumed.contains(n)) {
				continue;
			}
			if (n instanceof IrStatementPattern) {
				IrStatementPattern a = (IrStatementPattern) n;
				Var ap = a.getPredicate();
				if (isConstantIriPredicate(a)) {
					Var as = a.getSubject();
					Var ao = a.getObject();
					if (isAnonPathVar(ao)) {
						// find SP2 with subject endVar and object = ao
						for (int j = i + 1; j < in.size(); j++) {
							IrNode m = in.get(j);
							if (!(m instanceof IrStatementPattern)) {
								continue;
							}
							IrStatementPattern b = (IrStatementPattern) m;
							Var bp = b.getPredicate();
							if (!isConstantIriPredicate(b)) {
								continue;
							}
							if (!sameVar(ao, b.getObject()) || !isAnonPathVar(b.getObject())) {
								continue;
							}
							// fuse: start = as, path = ap / ^bp, end = b.subject
							Var start = as;
							String path = iri(ap, r) + "/^" + iri(bp, r);
							Var end = b.getSubject();
							out.add(new IrPathTriple(start, path, end, false, Collections.emptySet()));
							consumed.add(n);
							consumed.add(m);
							break;
						}
						if (consumed.contains(n)) {
							continue;
						}
					}
				}
			}
			// Recurse into nested BGPs
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), fuseForwardThenInverseTail(g.getWhere(), r), g.isNewScope()));
				continue;
			}
			if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(fuseForwardThenInverseTail(o.getWhere(), r), o.isNewScope());
				no.setNewScope(o.isNewScope());
				out.add(no);
				continue;
			}
			if (n instanceof IrMinus) {
				IrMinus m = (IrMinus) n;
				out.add(new IrMinus(fuseForwardThenInverseTail(m.getWhere(), r), m.isNewScope()));
				continue;
			}
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(fuseForwardThenInverseTail(b, r));
				}
				out.add(u2);
				continue;
			}
			if (n instanceof IrService) {
				IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(),
						fuseForwardThenInverseTail(s.getWhere(), r), s.isNewScope()));
				continue;
			}
			if (n instanceof IrSubSelect) {
				out.add(n);
				continue;
			}
			out.add(n);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		for (IrNode n : out) {
			if (!consumed.contains(n)) {
				res.add(n);
			}
		}
		res.setNewScope(bgp.isNewScope());
		return res;
	}

}
