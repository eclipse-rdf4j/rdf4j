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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
				if (p0 != null && p0.hasValue() && p0.getValue() instanceof IRI) {
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
						String step0 = r.renderIRI((IRI) p0.getValue());
						parts.add(startForward ? step0 : ("^" + step0));

						int j = i + 1;
						Var cur = mid;
						Var end = null;
						while (j < in.size()) {
							IrNode n2 = in.get(j);
							if (!(n2 instanceof IrStatementPattern)) {
								break;
							}
							IrStatementPattern sp = (IrStatementPattern) n2;
							Var pv = sp.getPredicate();
							if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
								break;
							}
							boolean forward = sameVar(cur, sp.getSubject());
							boolean inverse = sameVar(cur, sp.getObject());
							if (!forward && !inverse) {
								break;
							}
							String step = r.renderIRI((IRI) pv.getValue());
							parts.add(inverse ? ("^" + step) : step);
							Var nextVar = forward ? sp.getObject() : sp.getSubject();
							if (isAnonPathVar(nextVar)) {
								cur = nextVar;
								j++;
								continue;
							}
							end = nextVar;
							j++;
							break;
						}
						if (end != null) {
							out.add(new IrPathTriple(start, String.join("/", parts), end, false));
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
				if (!hasTail && pv != null && isAnonPathVar(pv) && ns != null && pv.getName() != null
						&& pv.getName().equals(ns.varName) && !ns.items.isEmpty()) {
					String nps = "!(" + ApplyNegatedPropertySetTransform.joinIrisWithPreferredOrder(ns.items, r) + ")";
					// Respect inverse orientation hint on the anon path var: render as !^p and flip endpoints
					if (isAnonPathInverseVar(pv)) {
						String maybe = invertNegatedPropertySet(nps);
						if (maybe != null) {
							nps = maybe;
						}
						out.add(new IrPathTriple(sp.getObject(), nps, sp.getSubject(), false));
					} else {
						out.add(new IrPathTriple(sp.getSubject(), nps, sp.getObject(), false));
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
					if (ns != null && ns.varName != null && ns.varName.equals(pA.getName()) && pB != null
							&& pB.hasValue()
							&& pB.getValue() instanceof IRI) {
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
						if (midA != null && sameVar(midA, spB.getSubject())) {
							// Build NPS part; invert members when the first step is inverse
							String members = ApplyNegatedPropertySetTransform.joinIrisWithPreferredOrder(ns.items, r);
							String nps = "!(" + members + ")";
							if (!startForward) {
								nps = invertNegatedPropertySet(nps);
							}
							String tail = r.renderIRI((IRI) pB.getValue());
							Var startVar = startForward ? spA.getSubject() : spA.getObject();
							Var endVar = spB.getObject();
							out.add(new IrPathTriple(startVar, nps + "/" + tail, endVar, false));
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
						String p1 = r.renderIRI((IRI) ap.getValue());
						String p2 = r.renderIRI((IRI) bp.getValue());
						out.add(new IrPathTriple(as, p1 + "/" + p2, bo, false));
						i += 1; // consume next
						continue;
					}

					// ---- SP followed by IrPathTriple over the bridge → fuse into a single path triple ----
					if (n instanceof IrStatementPattern && i + 1 < in.size() && in.get(i + 1) instanceof IrPathTriple) {
						IrStatementPattern sp = (IrStatementPattern) n;
						Var p1 = sp.getPredicate();
						if (p1 != null && p1.hasValue() && p1.getValue() instanceof IRI) {
							IrPathTriple pt1 = (IrPathTriple) in.get(i + 1);
							if (sameVar(sp.getObject(), pt1.getSubject())) {
								// forward chaining
								String fused = r.renderIRI((IRI) p1.getValue()) + "/" + pt1.getPathText();
								out.add(new IrPathTriple(sp.getSubject(), fused, pt1.getObject(), false));
								i += 1;
								continue;
							} else if (sameVar(sp.getSubject(), pt1.getObject())) {
								// inverse chaining
								String fused = pt1.getPathText() + "/^" + r.renderIRI((IRI) p1.getValue());
								out.add(new IrPathTriple(pt1.getSubject(), fused, sp.getObject(), false));
								i += 1;
								continue;
							} else if (sameVar(sp.getSubject(), pt1.getSubject()) && isAnonPathVar(sp.getSubject())) {
								// SP and PT share their subject (an _anon_path_* bridge). Prefix the PT with an inverse
								// step from the SP and start from SP.object (which may be a user var like ?y).
								// This preserves bindings while eliminating the extra bridging triple.
								String fused = "^" + r.renderIRI((IRI) p1.getValue()) + "/" + pt1.getPathText();
								out.add(new IrPathTriple(sp.getObject(), fused, pt1.getObject(), false));
								i += 1;
								continue;
							}
						}

						// ---- SP followed by IrPathTriple over the bridge → fuse into a single path triple ----
						if (n instanceof IrStatementPattern && i + 1 < in.size()
								&& in.get(i + 1) instanceof IrPathTriple) {
							IrStatementPattern sp2 = (IrStatementPattern) n;
							Var p2 = sp2.getPredicate();
							if (p2 != null && p2.hasValue() && p2.getValue() instanceof IRI) {
								IrPathTriple pt2 = (IrPathTriple) in.get(i + 1);
								if (sameVar(sp2.getObject(), pt2.getSubject())) {
									// forward chaining
									String fused = r.renderIRI((IRI) p2.getValue()) + "/" + pt2.getPathText();
									out.add(new IrPathTriple(sp2.getSubject(), fused,
											pt2.getObject(), false));
									i += 1;
									continue;
								} else if (sameVar(sp2.getSubject(), pt2.getObject())) {
									// inverse chaining
									String fused = pt2.getPathText() + "/^" + r.renderIRI((IRI) p2.getValue());
									out.add(new IrPathTriple(pt2.getSubject(), fused,
											sp2.getObject(), false));
									i += 1;
									continue;
								}
							}
						}
					}
				}

				// ---- Fuse an IrPathTriple followed by a constant-predicate SP that connects to the path's object ----
				if (n instanceof IrPathTriple && i + 1 < in.size() && in.get(i + 1) instanceof IrStatementPattern) {
					// If there is a preceding SP that likely wants to fuse with this PT first, defer this PT+SP fusion.
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
					if (pv != null && pv.hasValue() && pv.getValue() instanceof IRI) {
						// Only fuse when the bridge var (?mid) is an _anon_path_* var; otherwise we might elide a user
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
							if (candidateEnd != null
									&& (sameVar(candidateEnd, pt2.getSubject())
											|| sameVar(candidateEnd, pt2.getObject()))) {
								// Defer; do not consume SP here
								out.add(n);
								continue;
							}
						}
						String joinStep = null;
						Var endVar = null;
						if (sameVar(pt.getObject(), sp.getSubject())) {
							joinStep = "/" + r.renderIRI((IRI) pv.getValue());
							endVar = sp.getObject();
						}
						if (joinStep != null) {
							final String fusedPath = pt.getPathText() + joinStep;
							out.add(new IrPathTriple(pt.getSubject(), fusedPath, endVar, false));
							i += 1; // consume next
							continue;
						}
					}
				}
			}

			// ---- Fuse an IrPathTriple followed by a constant-predicate SP that connects to the path's object ----
			if (n instanceof IrPathTriple && i + 1 < in.size() && in.get(i + 1) instanceof IrStatementPattern) {
				// If there is a preceding SP that likely wants to fuse with this PT first, defer this PT+SP fusion.
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
				if (pv != null && pv.hasValue() && pv.getValue() instanceof IRI) {
					// Only fuse when the bridge var (?mid) is an _anon_path_* var; otherwise we might elide a user var
					// like ?y
					if (!isAnonPathVar(pt.getObject())) {
						out.add(n);
						continue;
					}
					String joinStep = null;
					Var endVar2 = null;
					if (sameVar(pt.getObject(), sp.getSubject())) {
						joinStep = "/" + r.renderIRI((IRI) pv.getValue());
						endVar2 = sp.getObject();
					}
					if (joinStep != null) {
						final String fusedPath = pt.getPathText() + joinStep;
						out.add(new IrPathTriple(pt.getSubject(), fusedPath, endVar2, false));
						i += 1; // consume next
						continue;
					}
				}
			}

			// ---- GRAPH/SP followed by UNION over bridge var → fused path inside GRAPH ----
			if ((n instanceof IrGraph || n instanceof IrStatementPattern) && i + 1 < in.size()
					&& in.get(i + 1) instanceof IrUnion) {
				IrUnion u = (IrUnion) in.get(i + 1);
				// Respect explicit UNION scopes, except when every branch clearly consists of parser
				// anon-path bridge variables. In that case, fusing is safe and preserves user-visible
				// bindings.
				if (u.isNewScope() && !unionBranchesAllHaveAnonPathBridge(u)) {
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
					if (p0 != null && p0.hasValue() && p0.getValue() instanceof IRI) {
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
								if (pX == null || !pX.hasValue() || !(pX.getValue() instanceof IRI)) {
									ok = false;
									break;
								}
								String step = r.renderIRI((IRI) pX.getValue());
								Var end;
								if (sameVar(mid, spX.getSubject())) {
									// forward
									end = spX.getObject();
								} else if (sameVar(mid, spX.getObject())) {
									// inverse
									step = "^" + step;
									end = spX.getSubject();
								} else {
									ok = false;
									break;
								}
								if (endVarOut == null) {
									endVarOut = end;
								} else if (!sameVar(endVarOut, end)) {
									ok = false;
									break;
								}
								alts.add(step);
							}
							if (ok && endVarOut != null && !alts.isEmpty()) {
								Var startVar = startForward ? sp0.getSubject() : sp0.getObject();
								String first = r.renderIRI((IRI) p0.getValue());
								if (!startForward) {
									first = "^" + first;
								}
								// Alternation preserves UNION branch order

								String altTxt = (alts.size() == 1) ? alts.get(0)
										: ("(" + String.join("|", alts) + ")");

								// Parenthesize first step and wrap alternation in triple parens to match expected
								// idempotence
								String pathTxt = first + "/" + altTxt;

								IrPathTriple fused = new IrPathTriple(startVar, pathTxt, endVarOut, false);
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
										Var pj = spj.getPredicate();
										if (pj == null || !pj.hasValue() || !(pj.getValue() instanceof IRI)) {
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
										String step = r.renderIRI((IRI) joinSp.getPredicate().getValue());
										String ext = "/" + (joinInverse ? "^" : "") + step;
										String newPath = fused.getPathText() + ext;
										Var newEnd = joinInverse ? joinSp.getSubject() : joinSp.getObject();
										fused = new IrPathTriple(fused.getSubject(), newPath, newEnd, false);
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

			// ---- GRAPH/SP followed by PathTriple over the bridge → fuse inside GRAPH ----
			if (n instanceof IrGraph && i + 1 < in.size() && in.get(i + 1) instanceof IrPathTriple) {
				IrGraph g = (IrGraph) n;
				IrBGP inner = g.getWhere();
				if (inner != null && inner.getLines().size() == 1) {
					IrNode innerOnly = inner.getLines().get(0);
					IrPathTriple pt = (IrPathTriple) in.get(i + 1);
					// Case A: inner is a simple SP; reuse existing logic
					if (innerOnly instanceof IrStatementPattern) {
						IrStatementPattern sp0 = (IrStatementPattern) innerOnly;
						Var p0 = sp0.getPredicate();
						if (p0 != null && p0.hasValue() && p0.getValue() instanceof IRI) {
							Var mid = isAnonPathVar(sp0.getObject()) ? sp0.getObject()
									: (isAnonPathVar(sp0.getSubject()) ? sp0.getSubject() : null);
							if (mid != null) {
								boolean forward = mid == sp0.getObject();
								Var sideVar = forward ? sp0.getSubject() : sp0.getObject();
								String first = r.renderIRI((IRI) p0.getValue());
								if (!forward) {
									first = "^" + first;
								}
								if (sameVar(mid, pt.getSubject())) {
									String fused = first + "/" + pt.getPathText();
									IrBGP newInner = new IrBGP(inner.isNewScope());
									newInner.add(new IrPathTriple(sideVar, fused, pt.getObject(), false));
									// copy any leftover inner lines except sp0
									copyAllExcept(inner, newInner, sp0);
									out.add(new IrGraph(g.getGraph(), newInner, g.isNewScope()));
									i += 1; // consume the path triple
									continue;
								}
							}
						}
					}
					// Case B: inner is already a path triple -> fuse with outer PT when they bridge
					if (innerOnly instanceof IrPathTriple) {
						IrPathTriple pt0 = (IrPathTriple) innerOnly;
						if (sameVar(pt0.getObject(), pt.getSubject())) {
							String fused = "(" + pt0.getPathText() + ")/(" + pt.getPathText() + ")";
							IrBGP newInner = new IrBGP(inner.isNewScope());
							newInner.add(new IrPathTriple(pt0.getSubject(), fused, pt.getObject(), false));
							out.add(new IrGraph(g.getGraph(), newInner, g.isNewScope()));
							i += 1; // consume the path triple
							continue;
						}
					}
				}
			}

			// Rewrite UNION alternation of simple triples (and already-fused path triples) into a single
			// IrPathTriple, preserving branch order and GRAPH context when present. This enables
			// subsequent chaining with a following constant-predicate triple via pt + SP -> pt/IRI.
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				boolean allow = !u.isNewScope() || unionBranchesAllHaveAnonPathBridge(u);
				if (!allow) {
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
					IrTripleLike tl = null;
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

				// Second form: UNION of 2-step sequences that share the same endpoints via an _anon_path_* bridge var
				// in
				// each branch. Each branch must be exactly two SPs connected by a mid var named like _anon_path_*; the
				// two
				// constants across the SPs form a sequence, with direction (^) added when the mid var occurs in object
				// pos.
				if (!ok) {
					// Try 2-step sequence alternation
					ok = true;
					Var startVarOut = null, endVarOut = null;
					final List<String> seqs = new ArrayList<>();
					for (IrBGP b : u.getBranches()) {
						if (!ok) {
							break;
						}
						if (b.getLines().size() != 2 || !(b.getLines().get(0) instanceof IrStatementPattern)
								|| !(b.getLines().get(1) instanceof IrStatementPattern)) {
							ok = false;
							break;
						}
						final IrStatementPattern a = (IrStatementPattern) b.getLines().get(0);
						final IrStatementPattern c = (IrStatementPattern) b.getLines().get(1);
						final Var ap = a.getPredicate(), cp = c.getPredicate();
						if (ap == null || !ap.hasValue() || !(ap.getValue() instanceof IRI) || cp == null
								|| !cp.hasValue() || !(cp.getValue() instanceof IRI)) {
							ok = false;
							break;
						}
						// Identify mid var linking the two triples
						Var mid = null, startVar = null, endVar = null;
						boolean firstForward = false, secondForward = false;
						if (isAnonPathVar(a.getObject()) && sameVar(a.getObject(), c.getSubject())) {
							mid = a.getObject();
							startVar = a.getSubject();
							endVar = c.getObject();
							firstForward = true;
							secondForward = true;
						} else if (isAnonPathVar(a.getSubject()) && sameVar(a.getSubject(), c.getObject())) {
							mid = a.getSubject();
							startVar = a.getObject();
							endVar = c.getSubject();
							firstForward = false;
							secondForward = false;
						} else if (isAnonPathVar(a.getObject()) && sameVar(a.getObject(), c.getObject())) {
							mid = a.getObject();
							startVar = a.getSubject();
							endVar = c.getSubject();
							firstForward = true;
							secondForward = false;
						} else if (isAnonPathVar(a.getSubject()) && sameVar(a.getSubject(), c.getSubject())) {
							mid = a.getSubject();
							startVar = a.getObject();
							endVar = c.getObject();
							firstForward = false;
							secondForward = true;
						}
						if (mid == null) {
							ok = false;
							break;
						}
						final Var sVar = startVar;
						final Var eVar = endVar;
						final String step1 = (firstForward ? "" : "^") + r.renderIRI((IRI) ap.getValue());
						final String step2 = (secondForward ? "" : "^") + r.renderIRI((IRI) cp.getValue());
						final String seq = step1 + "/" + step2;
						if (startVarOut == null && endVarOut == null) {
							startVarOut = sVar;
							endVarOut = eVar;
						} else if (!(sameVar(startVarOut, sVar) && sameVar(endVarOut, eVar))) {
							ok = false;
							break;
						}
						seqs.add(seq);
					}
					if (ok && startVarOut != null && endVarOut != null && !seqs.isEmpty()) {
						final String alt = (seqs.size() == 1) ? seqs.get(0) : String.join("|", seqs);
						out.add(new IrPathTriple(startVarOut, alt, endVarOut, false));
						continue;
					}
				}

				// 2a-mixed: UNION with one branch a single SP and another branch a 2-step sequence via
				// _anon_path_* bridge, sharing identical endpoints. Fuse into a single alternation path where
				// one side is a 1-step atom and the other a 2-step sequence (e.g., "^foaf:knows|ex:knows/^foaf:knows").
				if (u.getBranches().size() == 2) {
					IrBGP b0 = u.getBranches().get(0);
					IrBGP b1 = u.getBranches().get(1);
					// Helper to parse a 2-step branch; returns {startVar, endVar, seqPath} or null
					class TwoStep {
						final Var s;
						final Var o;
						final String path;

						TwoStep(Var s, Var o, String path) {
							this.s = s;
							this.o = o;
							this.path = path;
						}
					}
					Function<IrBGP, TwoStep> parseTwo = (bg) -> {
						if (bg == null || bg.getLines().size() != 2) {
							return null;
						}
						if (!(bg.getLines().get(0) instanceof IrStatementPattern)
								|| !(bg.getLines().get(1) instanceof IrStatementPattern)) {
							return null;
						}
						final IrStatementPattern a = (IrStatementPattern) bg.getLines().get(0);
						final IrStatementPattern c = (IrStatementPattern) bg.getLines().get(1);
						final Var ap = a.getPredicate(), cp = c.getPredicate();
						if (ap == null || !ap.hasValue() || !(ap.getValue() instanceof IRI) || cp == null
								|| !cp.hasValue() || !(cp.getValue() instanceof IRI)) {
							return null;
						}
						Var mid = null, startVar = null, endVar = null;
						boolean firstForward = false, secondForward = false;
						if (isAnonPathVar(a.getObject()) && sameVar(a.getObject(), c.getSubject())) {
							mid = a.getObject();
							startVar = a.getSubject();
							endVar = c.getObject();
							firstForward = true;
							secondForward = true;
						} else if (isAnonPathVar(a.getSubject()) && sameVar(a.getSubject(), c.getObject())) {
							mid = a.getSubject();
							startVar = a.getObject();
							endVar = c.getSubject();
							firstForward = false;
							secondForward = false;
						} else if (isAnonPathVar(a.getObject()) && sameVar(a.getObject(), c.getObject())) {
							mid = a.getObject();
							startVar = a.getSubject();
							endVar = c.getSubject();
							firstForward = true;
							secondForward = false;
						} else if (isAnonPathVar(a.getSubject()) && sameVar(a.getSubject(), c.getSubject())) {
							mid = a.getSubject();
							startVar = a.getObject();
							endVar = c.getObject();
							firstForward = false;
							secondForward = true;
						}
						if (mid == null) {
							return null;
						}
						final String step1 = (firstForward ? "" : "^") + r.renderIRI((IRI) ap.getValue());
						final String step2 = (secondForward ? "" : "^") + r.renderIRI((IRI) cp.getValue());
						return new TwoStep(startVar, endVar, step1 + "/" + step2);
					};

					TwoStep ts0 = parseTwo.apply(b0);
					TwoStep ts1 = parseTwo.apply(b1);
					IrStatementPattern spSingle = null;
					TwoStep two = null;
					int singleIdx = -1;
					if (ts0 != null && b1.getLines().size() == 1
							&& b1.getLines().get(0) instanceof IrStatementPattern) {
						two = ts0;
						singleIdx = 1;
						spSingle = (IrStatementPattern) b1.getLines().get(0);
					} else if (ts1 != null && b0.getLines().size() == 1
							&& b0.getLines().get(0) instanceof IrStatementPattern) {
						two = ts1;
						singleIdx = 0;
						spSingle = (IrStatementPattern) b0.getLines().get(0);
					}
					if (two != null && spSingle != null) {
						// Ensure single branch uses a constant predicate and matches endpoints
						Var pv = spSingle.getPredicate();
						if (pv != null && pv.hasValue() && pv.getValue() instanceof IRI) {
							String atom = null;
							if (sameVar(two.s, spSingle.getSubject()) && sameVar(two.o, spSingle.getObject())) {
								atom = r.renderIRI((IRI) pv.getValue());
							} else if (sameVar(two.s, spSingle.getObject()) && sameVar(two.o, spSingle.getSubject())) {
								atom = "^" + r.renderIRI((IRI) pv.getValue());
							}
							if (atom != null) {
								final String alt = (singleIdx == 0) ? (atom + "|" + two.path) : (two.path + "|" + atom);
								out.add(new IrPathTriple(two.s, alt, two.o, false));
								continue;
							}
						}
					}
				}

				// 2a-mixed-two: one branch is a simple IrPathTriple representing exactly two constant steps
				// without quantifiers/alternation, and the other branch is exactly two SPs via an _anon_path_* mid,
				// sharing identical endpoints. Fuse into a single alternation path.
				if (u.getBranches().size() == 2) {
					class TwoLike {
						final Var s;
						final Var o;
						final String path;

						TwoLike(Var s, Var o, String path) {
							this.s = s;
							this.o = o;
							this.path = path;
						}
					}
					Function<IrBGP, TwoLike> parseTwoLike = (bg) -> {
						if (bg == null || bg.getLines().isEmpty())
							return null;
						IrNode only = (bg.getLines().size() == 1) ? bg.getLines().get(0) : null;
						if (only instanceof IrPathTriple) {
							IrPathTriple pt = (IrPathTriple) only;
							String ptxt = pt.getPathText();
							if (ptxt == null || ptxt.contains("|") || ptxt.contains("?") || ptxt.contains("*")
									|| ptxt.contains("+")) {
								return null;
							}
							int slash = ptxt.indexOf('/');
							if (slash < 0)
								return null; // not a two-step path
							String left = ptxt.substring(0, slash).trim();
							String right = ptxt.substring(slash + 1).trim();
							if (left.isEmpty() || right.isEmpty())
								return null;
							return new TwoLike(pt.getSubject(), pt.getObject(), left + "/" + right);
						}
						if (bg.getLines().size() == 2 && bg.getLines().get(0) instanceof IrStatementPattern
								&& bg.getLines().get(1) instanceof IrStatementPattern) {
							IrStatementPattern a = (IrStatementPattern) bg.getLines().get(0);
							IrStatementPattern c = (IrStatementPattern) bg.getLines().get(1);
							Var ap = a.getPredicate(), cp = c.getPredicate();
							if (ap == null || !ap.hasValue() || !(ap.getValue() instanceof IRI) || cp == null
									|| !cp.hasValue() || !(cp.getValue() instanceof IRI)) {
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
							if (mid == null)
								return null;
							String step1 = (firstForward ? "" : "^") + r.renderIRI((IRI) ap.getValue());
							String step2 = (secondForward ? "" : "^") + r.renderIRI((IRI) cp.getValue());
							return new TwoLike(sVar, oVar, step1 + "/" + step2);
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
							String alt = ("(" + t0.path + ")|(" + t1.path + ")");
							out.add(new IrPathTriple(t0.s, alt, t0.o, false));
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
						if (pv != null && pv.hasValue() && pv.getValue() instanceof IRI) {
							final Var wantS = pt.getSubject();
							final Var wantO = pt.getObject();
							String atom = null;
							if (sameVar(wantS, sp.getSubject()) && sameVar(wantO, sp.getObject())) {
								atom = r.renderIRI((IRI) pv.getValue());
							} else if (sameVar(wantS, sp.getObject()) && sameVar(wantO, sp.getSubject())) {
								atom = "^" + r.renderIRI((IRI) pv.getValue());
							}
							if (atom != null) {
								final String alt = (ptIdx == 0) ? ("(" + pt.getPathText() + ")|(" + atom + ")")
										: ("(" + atom + ")|(" + pt.getPathText() + ")");
								out.add(new IrPathTriple(wantS, alt, wantO, false));
								continue;
							}
						}
					}
				}

				// 2b: Partial 2-step subset merge. If some (>=2) branches are exactly two-SP chains with
				// identical endpoints, merge those into one IrPathTriple and keep the remaining branches
				// as-is. This preserves grouping like "{ {A|B} UNION {C} }" when the union has A, B, and C
				// but only A and B are plain two-step sequences.
				{
					final List<Integer> idx = new ArrayList<>();
					Var startVarOut = null, endVarOut = null;
					final List<String> seqs = new ArrayList<>();
					for (int bi = 0; bi < u.getBranches().size(); bi++) {
						IrBGP b = u.getBranches().get(bi);
						if (b.getLines().size() != 2 || !(b.getLines().get(0) instanceof IrStatementPattern)
								|| !(b.getLines().get(1) instanceof IrStatementPattern)) {
							continue;
						}
						final IrStatementPattern a = (IrStatementPattern) b.getLines().get(0);
						final IrStatementPattern c = (IrStatementPattern) b.getLines().get(1);
						final Var ap = a.getPredicate(), cp = c.getPredicate();
						if (ap == null || !ap.hasValue() || !(ap.getValue() instanceof IRI) || cp == null
								|| !cp.hasValue() || !(cp.getValue() instanceof IRI)) {
							continue;
						}
						Var mid = null, startVar = null, endVar = null;
						boolean firstForward = false, secondForward = false;
						if (isAnonPathVar(a.getObject()) && sameVar(a.getObject(), c.getSubject())) {
							mid = a.getObject();
							startVar = a.getSubject();
							endVar = c.getObject();
							firstForward = true;
							secondForward = true;
						} else if (isAnonPathVar(a.getSubject()) && sameVar(a.getSubject(), c.getObject())) {
							mid = a.getSubject();
							startVar = a.getObject();
							endVar = c.getSubject();
							firstForward = false;
							secondForward = false;
						} else if (isAnonPathVar(a.getObject()) && sameVar(a.getObject(), c.getObject())) {
							mid = a.getObject();
							startVar = a.getSubject();
							endVar = c.getSubject();
							firstForward = true;
							secondForward = false;
						} else if (isAnonPathVar(a.getSubject()) && sameVar(a.getSubject(), c.getSubject())) {
							mid = a.getSubject();
							startVar = a.getObject();
							endVar = c.getObject();
							firstForward = false;
							secondForward = true;
						}
						if (mid == null) {
							continue;
						}
						final Var sVar = startVar;
						final Var eVar = endVar;
						final String step1 = (firstForward ? "" : "^") + r.renderIRI((IRI) ap.getValue());
						final String step2 = (secondForward ? "" : "^") + r.renderIRI((IRI) cp.getValue());
						final String seq = step1 + "/" + step2;
						if (startVarOut == null && endVarOut == null) {
							startVarOut = sVar;
							endVarOut = eVar;
						} else if (!(sameVar(startVarOut, sVar) && sameVar(endVarOut, eVar))) {
							continue;
						}
						idx.add(bi);
						seqs.add(seq);
					}
					if (idx.size() >= 2) {
						final String alt = String.join("|", seqs);
						final IrPathTriple fused = new IrPathTriple(startVarOut, alt, endVarOut, false);
						// Rebuild union branches: fused + the non-merged ones (in original order)
						final IrUnion u2 = new IrUnion(u.isNewScope());
						IrBGP fusedBgp = new IrBGP(bgp.isNewScope());
						fusedBgp.add(fused);
						u2.addBranch(fusedBgp);
						for (int bi = 0; bi < u.getBranches().size(); bi++) {
							if (!idx.contains(bi)) {
								u2.addBranch(u.getBranches().get(bi));
							}
						}
						out.add(u2);
						continue;
					}
				}

				// 2c: Partial merge of IrPathTriple branches (no inner alternation). If there are >=2 branches where
				// each
				// is a simple IrPathTriple without inner alternation or quantifiers and they share identical endpoints,
				// fuse them into a single alternation path, keeping remaining branches intact.
				{
					Var sVarOut = null, oVarOut = null;
					final List<Integer> idx = new ArrayList<>();
					final List<String> basePaths = new ArrayList<>();
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
						} else if (!(sameVar(sVarOut, pt.getSubject()) && sameVar(oVarOut, pt.getObject()))) {
							continue;
						}
						idx.add(bi);
						basePaths.add(ptxt);
					}
					if (idx.size() >= 2) {
						final String alt = String.join("|", basePaths);
						final IrPathTriple fused = new IrPathTriple(sVarOut, alt, oVarOut, false);
						final IrUnion u2 = new IrUnion(bgp.isNewScope());
						IrBGP fusedBgp = new IrBGP(bgp.isNewScope());
						fusedBgp.add(fused);
						u2.addBranch(fusedBgp);
						for (int bi = 0; bi < u.getBranches().size(); bi++) {
							if (!idx.contains(bi)) {
								u2.addBranch(u.getBranches().get(bi));
							}
						}
						out.add(u2);
						continue;
					}
				}

				// Third form: UNION where each branch reduces to a single IrPathTriple with identical endpoints ->
				// combine into a single IrPathTriple with an alternation of the full path expressions.
				{
					Var sVarOut3 = null, oVarOut3 = null;
					final List<String> paths = new ArrayList<>();
					boolean allPt = true;
					for (IrBGP b : u.getBranches()) {
						if (!allPt) {
							break;
						}
						IrNode only = (b.getLines().size() == 1) ? b.getLines().get(0) : null;
						IrPathTriple pt;
						if (only instanceof IrPathTriple) {
							pt = (IrPathTriple) only;
						} else if (only instanceof IrGraph) {
							IrGraph g = (IrGraph) only;
							if (g.getWhere() != null && g.getWhere().getLines().size() == 1
									&& g.getWhere().getLines().get(0) instanceof IrPathTriple) {
								pt = (IrPathTriple) g.getWhere().getLines().get(0);
							} else {
								allPt = false;
								break;
							}
						} else {
							allPt = false;
							break;
						}
						if (sVarOut3 == null && oVarOut3 == null) {
							sVarOut3 = pt.getSubject();
							oVarOut3 = pt.getObject();
						} else if (!(sameVar(sVarOut3, pt.getSubject()) && sameVar(oVarOut3, pt.getObject()))) {
							allPt = false;
							break;
						}
						paths.add(pt.getPathText());
					}
					boolean hasQuantifier = false;
					boolean hasInnerAlternation = false;
					for (String ptxt : paths) {
						if (ptxt.contains("?") || ptxt.contains("*") || ptxt.contains("+")) {
							hasQuantifier = true;
							break;
						}
						if (ptxt.contains("|")) {
							hasInnerAlternation = true;
						}
					}
					// Only merge when there are no quantifiers and no inner alternation groups inside each path
					if (allPt && sVarOut3 != null && oVarOut3 != null && !paths.isEmpty() && !hasQuantifier
							&& !hasInnerAlternation) {
						final String alt = (paths.size() == 1) ? paths.get(0) : String.join("|", paths);
						out.add(new IrPathTriple(sVarOut3, alt, oVarOut3, false));
						continue;
					}
				}

				// Fourth form: UNION of single-step triples followed immediately by a constant-predicate SP that shares
				// the union's bridge var -> fuse into (alt)/^tail.
				if (i + 1 < in.size() && in.get(i + 1) instanceof IrStatementPattern) {
					final IrStatementPattern post = (IrStatementPattern) in.get(i + 1);
					final Var postPred = post.getPredicate();
					if (postPred != null && postPred.hasValue() && postPred.getValue() instanceof IRI) {
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
							if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
								ok2 = false;
								break;
							}
							String step;
							Var sVarCandidate;
							// post triple is ?end postPred ?mid
							if (sameVar(sp.getSubject(), post.getObject())) {
								step = "^" + r.renderIRI((IRI) pv.getValue());
								sVarCandidate = sp.getObject();
							} else if (sameVar(sp.getObject(), post.getObject())) {
								step = r.renderIRI((IRI) pv.getValue());
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
							final String tail = "/^" + r.renderIRI((IRI) postPred.getValue());
							out.add(new IrPathTriple(startVar, "(" + alt + ")" + tail, endVar, false));
							i += 1;
							continue;
						}
					}
				}

				if (ok && !parts.isEmpty()) {
					String pathTxt;
					boolean allNps = true;
					for (String ptxt : parts) {
						String sPart = ptxt == null ? null : ptxt.trim();
						if (sPart == null || !sPart.startsWith("!(") || !sPart.endsWith(")")) {
							allNps = false;
							break;
						}
					}
					if (allNps) {
						// Merge into a single NPS by unioning inner members
						Set<String> members = new LinkedHashSet<>();
						for (String ptxt : parts) {
							String inner = ptxt.substring(2, ptxt.length() - 1);
							if (inner.isEmpty()) {
								continue;
							}
							for (String tok : inner.split("\\|")) {
								String t = tok.trim();
								if (!t.isEmpty()) {
									members.add(t);
								}
							}
						}
						pathTxt = "!(" + String.join("|", members) + ")";
					} else {
						pathTxt = (parts.size() == 1) ? parts.get(0) : "(" + String.join("|", parts) + ")";
					}
					// For NPS we may want to orient the merged path so that it can chain with an immediate
					// following triple (e.g., NPS/next). If the next line uses one of our endpoints, flip to
					// ensure pt.object equals next.subject when safe.
					Var subjOut = subj, objOut = obj;
					IrNode next = (i + 1 < in.size()) ? in.get(i + 1) : null;
					if (next instanceof IrPathTriple && pathTxt.startsWith("!(")) {
						IrPathTriple nextPt = (IrPathTriple) next;
						Var nSubj = nextPt.getSubject();
						String nextTxt = nextPt.getPathText();
						boolean nextIsNps = nextTxt != null && nextTxt.trim().startsWith("!(");
						// Only orient NPS to chain with a non-NPS following path
						if (!nextIsNps && nSubj != null && sameVar(subjOut, nSubj) && !sameVar(objOut, nSubj)) {
							Var tmp = subjOut;
							subjOut = objOut;
							objOut = tmp;
						}
					}
					IrPathTriple pt = new IrPathTriple(subjOut, pathTxt, objOut, false);
					if (graphRef != null) {
						IrBGP inner = new IrBGP(false);
						inner.add(pt);
						out.add(new IrGraph(graphRef, inner, false));
					} else {
						out.add(pt);
					}
					continue;
				}
			}
			// linear fusion: IrPathTriple + rdf:first triple on its object → fused path
			if (n instanceof IrPathTriple && i + 1 < in.size() && in.get(i + 1) instanceof IrStatementPattern) {
				IrPathTriple pt = (IrPathTriple) n;
				IrStatementPattern sp = (IrStatementPattern) in.get(i + 1);
				Var pv = sp.getPredicate();
				if (pv != null && pv.hasValue() && pv.getValue() instanceof IRI && RDF.FIRST.equals(pv.getValue())) {
					if (sameVar(pt.getObject(), sp.getSubject())) {
						String fused = pt.getPathText() + "/" + r.renderIRI(RDF.FIRST);
						out.add(new IrPathTriple(pt.getSubject(), fused, sp.getObject(), false));
						i++; // consume next
						continue;
					}
				}
			}
			out.add(n);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
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
				if (ap != null && ap.hasValue() && ap.getValue() instanceof IRI) {
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
							if (bp == null || !bp.hasValue() || !(bp.getValue() instanceof IRI)) {
								continue;
							}
							if (!sameVar(ao, b.getObject()) || !isAnonPathVar(b.getObject())) {
								continue;
							}
							// fuse: start = as, path = ap / ^bp, end = b.subject
							Var start = as;
							String path = r.renderIRI((IRI) ap.getValue()) + "/^" + r.renderIRI((IRI) bp.getValue());
							Var end = b.getSubject();
							out.add(new IrPathTriple(start, path, end, false));
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
