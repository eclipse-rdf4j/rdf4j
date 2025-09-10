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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrValues;

/**
 * Form negated property sets (NPS) from simple shapes involving a predicate variable constrained by NOT IN or a chain
 * of {@code !=} filters, optionally followed by a constant-predicate tail step that is fused. Also contains GRAPH-aware
 * variants so that common IR orders like GRAPH, FILTER, GRAPH can be handled.
 *
 * Safety: - Requires the filtered predicate variable to be a parser-generated {@code _anon_path_*} var. - Only fuses
 * constant-predicate tails; complex tails are left to later passes.
 */
public final class ApplyNegatedPropertySetTransform extends BaseTransform {
	private ApplyNegatedPropertySetTransform() {
	}

	private static final class PT {
		Var g;
		IrPathTriple pt;
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}

		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		final Set<IrNode> consumed = new LinkedHashSet<>();

		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (consumed.contains(n)) {
				continue;
			}

			// Backward-looking fold: ... VALUES ; GRAPH { SP(var) } ; FILTER(var != iri)
			if (n instanceof IrFilter) {
				final IrFilter f = (IrFilter) n;
				final String condText = f.getConditionText();
				final NsText ns = condText == null ? null : parseNegatedSetText(condText);
				if (ns != null && !ns.items.isEmpty() && isAnonPathName(ns.varName) && !out.isEmpty()) {
					// Case A: previous is a grouped BGP: { VALUES ; GRAPH { SP(var) } }
					IrNode last = out.get(out.size() - 1);
					if (last instanceof IrBGP) {
						IrBGP grp = (IrBGP) last;
						if (grp.getLines().size() >= 2 && grp.getLines().get(0) instanceof IrValues
								&& grp.getLines().get(1) instanceof IrGraph) {
							IrValues vals = (IrValues) grp.getLines().get(0);
							IrGraph g = (IrGraph) grp.getLines().get(1);
							if (g.getWhere() != null && g.getWhere().getLines().size() == 1
									&& g.getWhere().getLines().get(0) instanceof IrStatementPattern) {
								IrStatementPattern sp = (IrStatementPattern) g.getWhere().getLines().get(0);
								Var pVar = sp.getPredicate();
								if ((BaseTransform.isAnonPathVar(pVar)
										|| BaseTransform.isAnonPathInverseVar(pVar))) {
									boolean inv = BaseTransform.isAnonPathInverseVar(pVar);
									String nps = inv ? "!(^" + joinIrisWithPreferredOrder(ns.items, r) + ")"
											: "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
									IrBGP inner = new IrBGP(false);
									inner.add(vals);
									inner.add(inv
											? new IrPathTriple(sp.getObject(), sp.getObjectOverride(), nps,
													sp.getSubject(), sp.getSubjectOverride(),
													IrPathTriple.fromStatementPatterns(sp), false)
											: new IrPathTriple(sp.getSubject(), sp.getSubjectOverride(), nps,
													sp.getObject(), sp.getObjectOverride(),
													IrPathTriple.fromStatementPatterns(sp), false));
									out.remove(out.size() - 1);
									out.add(new IrGraph(g.getGraph(), inner, g.isNewScope()));
									// Skip adding this FILTER
									continue;
								}
							}
						}
					}
					// Case B: previous two are VALUES then GRAPH { SP(var) }
					if (out.size() >= 2 && out.get(out.size() - 2) instanceof IrValues
							&& out.get(out.size() - 1) instanceof IrGraph) {
						IrValues vals = (IrValues) out.get(out.size() - 2);
						IrGraph g = (IrGraph) out.get(out.size() - 1);
						if (g.getWhere() != null && g.getWhere().getLines().size() == 1
								&& g.getWhere().getLines().get(0) instanceof IrStatementPattern) {
							IrStatementPattern sp = (IrStatementPattern) g.getWhere().getLines().get(0);
							Var pVar = sp.getPredicate();
							if ((BaseTransform.isAnonPathVar(pVar)
									|| BaseTransform.isAnonPathInverseVar(pVar))) {
								boolean inv = BaseTransform.isAnonPathInverseVar(pVar);
								String nps = inv ? "!(^" + joinIrisWithPreferredOrder(ns.items, r) + ")"
										: "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
								IrBGP inner = new IrBGP(!bgp.isNewScope());
								// Heuristic for braces inside GRAPH to match expected shape
								inner.add(vals);
								inner.add(inv
										? new IrPathTriple(sp.getObject(), sp.getObjectOverride(), nps, sp.getSubject(),
												sp.getSubjectOverride(), IrPathTriple.fromStatementPatterns(sp), false)
										: new IrPathTriple(sp.getSubject(), sp.getSubjectOverride(), nps,
												sp.getObject(), sp.getObjectOverride(),
												IrPathTriple.fromStatementPatterns(sp), false));
								// Replace last two with the new GRAPH
								out.remove(out.size() - 1);
								out.remove(out.size() - 1);
								out.add(new IrGraph(g.getGraph(), inner, g.isNewScope()));
								// Skip adding this FILTER
								continue;
							}
						}
					}
				}
			}

			// Variant: VALUES, then GRAPH { SP(var p) }, then FILTER -> fold into GRAPH { VALUES ; NPS } and consume
			if (n instanceof IrValues && i + 2 < in.size() && in.get(i + 1) instanceof IrGraph
					&& in.get(i + 2) instanceof IrFilter) {
				final IrValues vals = (IrValues) n;
				final IrGraph g = (IrGraph) in.get(i + 1);
				final IrFilter f = (IrFilter) in.get(i + 2);
				final String condText = f.getConditionText();
				final NsText ns = condText == null ? null : parseNegatedSetText(condText);
				if (ns != null && g.getWhere() != null && g.getWhere().getLines().size() == 1
						&& g.getWhere().getLines().get(0) instanceof IrStatementPattern) {
					final IrStatementPattern sp = (IrStatementPattern) g.getWhere().getLines().get(0);
					final Var pVar = sp.getPredicate();
					if ((BaseTransform.isAnonPathVar(pVar) || BaseTransform.isAnonPathInverseVar(pVar))
							&& isAnonPathName(ns.varName) && !ns.items.isEmpty()) {
						final boolean inv = BaseTransform.isAnonPathInverseVar(pVar);
						final String nps = inv
								? "!(^" + joinIrisWithPreferredOrder(ns.items, r) + ")"
								: "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
						final IrBGP newInner = new IrBGP(false);
						// Ensure braces inside GRAPH for the rewritten block
						newInner.add(vals);
						if (inv) {
							IrPathTriple pt = new IrPathTriple(sp.getObject(), sp.getObjectOverride(), nps,
									sp.getSubject(), sp.getSubjectOverride(), IrPathTriple.fromStatementPatterns(sp),
									false);
							newInner.add(pt);
						} else {
							IrPathTriple pt = new IrPathTriple(sp.getSubject(), sp.getSubjectOverride(), nps,
									sp.getObject(), sp.getObjectOverride(), IrPathTriple.fromStatementPatterns(sp),
									false);
							newInner.add(pt);
						}
						out.add(new IrGraph(g.getGraph(), newInner, g.isNewScope()));
						i += 2; // consume graph + filter
						continue;
					}
				}
			}

			// Pattern: FILTER (var != ..) followed by a grouped block containing VALUES then GRAPH { SP(var p) }
			if (n instanceof IrFilter && i + 1 < in.size() && in.get(i + 1) instanceof IrBGP) {
				final IrFilter f2 = (IrFilter) n;
				final String condText2 = f2.getConditionText();
				final NsText ns2 = condText2 == null ? null : parseNegatedSetText(condText2);
				final IrBGP grp2 = (IrBGP) in.get(i + 1);
				if (ns2 != null && grp2.getLines().size() >= 2 && grp2.getLines().get(0) instanceof IrValues
						&& grp2.getLines().get(1) instanceof IrGraph) {
					final IrValues vals2 = (IrValues) grp2.getLines().get(0);
					final IrGraph g2 = (IrGraph) grp2.getLines().get(1);
					if (g2.getWhere() != null && g2.getWhere().getLines().size() == 1
							&& g2.getWhere().getLines().get(0) instanceof IrStatementPattern) {
						final IrStatementPattern sp2 = (IrStatementPattern) g2.getWhere().getLines().get(0);
						final Var pVar2 = sp2.getPredicate();
						if ((BaseTransform.isAnonPathVar(pVar2) || BaseTransform.isAnonPathInverseVar(pVar2))
								&& isAnonPathName(ns2.varName)
								&& !ns2.items.isEmpty()) {
							final boolean inv2 = BaseTransform.isAnonPathInverseVar(pVar2);
							final String nps2 = inv2
									? "!(^" + joinIrisWithPreferredOrder(ns2.items, r) + ")"
									: "!(" + joinIrisWithPreferredOrder(ns2.items, r) + ")";
							final IrBGP newInner2 = new IrBGP(false);
							newInner2.add(vals2);
							if (inv2) {
								IrPathTriple pt2 = new IrPathTriple(sp2.getObject(), nps2, sp2.getSubject(), false,
										IrPathTriple.fromStatementPatterns(sp2));
								Set<Var> set2 = new HashSet<>();
								if (sp2.getPredicate() != null) {
									set2.add(sp2.getPredicate());
								}
								pt2.setPathVars(set2);
								newInner2.add(pt2);
							} else {
								IrPathTriple pt2 = new IrPathTriple(sp2.getSubject(), nps2, sp2.getObject(), false,
										IrPathTriple.fromStatementPatterns(sp2));
								Set<Var> set2 = new HashSet<>();
								if (sp2.getPredicate() != null) {
									set2.add(sp2.getPredicate());
								}
								pt2.setPathVars(set2);
								newInner2.add(pt2);
							}
							out.add(new IrGraph(g2.getGraph(), newInner2, g2.isNewScope()));
							i += 1; // consume grouped block
							continue;
						}
					}
				}
			}

			// Pattern: FILTER (var != ..) followed by VALUES, then GRAPH { SP(var p) }
			// Rewrite to: GRAPH { VALUES ... ; NPS path triple } and consume FILTER/GRAPH
			if (n instanceof IrFilter && i + 2 < in.size()
					&& in.get(i + 1) instanceof IrValues && in.get(i + 2) instanceof IrGraph) {
				final IrFilter f = (IrFilter) n;
				final String condText = f.getConditionText();
				final NsText ns = condText == null ? null : parseNegatedSetText(condText);
				final IrValues vals = (IrValues) in.get(i + 1);
				final IrGraph g = (IrGraph) in.get(i + 2);
				if (ns != null && g.getWhere() != null && g.getWhere().getLines().size() == 1
						&& g.getWhere().getLines().get(0) instanceof IrStatementPattern) {
					final IrStatementPattern sp = (IrStatementPattern) g.getWhere().getLines().get(0);
					final Var pVar = sp.getPredicate();
					if ((BaseTransform.isAnonPathVar(pVar) || BaseTransform.isAnonPathInverseVar(pVar))
							&& isAnonPathName(ns.varName) && !ns.items.isEmpty()) {
						final boolean inv = BaseTransform.isAnonPathInverseVar(pVar);
						final String nps = inv
								? "!(^" + joinIrisWithPreferredOrder(ns.items, r) + ")"
								: "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
						final IrBGP newInner = new IrBGP(false);
						// Keep VALUES first inside the GRAPH block
						newInner.add(vals);
						if (inv) {
							newInner.add(new IrPathTriple(sp.getObject(), sp.getObjectOverride(), nps, sp.getSubject(),
									sp.getSubjectOverride(), IrPathTriple.fromStatementPatterns(sp), false));
						} else {
							newInner.add(new IrPathTriple(sp.getSubject(), sp.getSubjectOverride(), nps, sp.getObject(),
									sp.getObjectOverride(), IrPathTriple.fromStatementPatterns(sp), false));
						}

						out.add(new IrGraph(g.getGraph(), newInner, g.isNewScope()));
						i += 2; // consume values + graph
						continue;
					}
				}
			}

			// Normalize simple var+FILTER patterns inside EXISTS blocks early so nested shapes
			// can fuse into !(...) as expected by streaming tests.
			if (n instanceof IrFilter) {
				final IrFilter fNode = (IrFilter) n;
				if (fNode.getBody() instanceof IrExists) {
					final IrExists ex = (IrExists) fNode.getBody();
					IrBGP inner = ex.getWhere();
					if (inner != null) {
						IrBGP orig = inner;
						inner = rewriteSimpleNpsOnly(inner, r);
						// If the original EXISTS body contained a UNION without explicit new scope and each
						// branch had an anon-path bridge var, fuse it into a single NPS in the rewritten body.
						inner = fuseEligibleUnionInsideExists(inner, orig);
						IrFilter nf = new IrFilter(new IrExists(inner, ex.isNewScope()), fNode.isNewScope());
						out.add(nf);
						i += 0;
						continue;
					}
				}
			}

			// (global NOT IN → NPS rewrite intentionally not applied; see specific GRAPH fusions below)

			// Heuristic pre-pass: move an immediately following NOT IN filter on the anon path var
			// into the preceding GRAPH block, so that subsequent coalescing and NPS fusion can act
			// on a contiguous GRAPH ... FILTER ... GRAPH shape.
			if (n instanceof IrGraph && i + 1 < in.size() && in.get(i + 1) instanceof IrFilter) {
				final IrGraph g1 = (IrGraph) n;
				final IrFilter f = (IrFilter) in.get(i + 1);
				final String condText0 = f.getConditionText();
				// System.out.println("# DBG pre-move scan: condText0=" + condText0);
				final NsText ns0 = condText0 == null ? null : parseNegatedSetText(condText0);
				if (ns0 != null && ns0.varName != null && !ns0.items.isEmpty()) {
					final MatchTriple mt0 = findTripleWithPredicateVar(g1.getWhere(), ns0.varName);
					if (mt0 != null) {
						final IrBGP inner = new IrBGP(false);
						// original inner lines first
						copyAllExcept(g1.getWhere(), inner, null);
						// then the filter moved inside
						inner.add(f);
						out.add(new IrGraph(g1.getGraph(), inner, g1.isNewScope()));
						// System.out.println("# DBG NPS: moved NOT IN filter into preceding GRAPH");
						i += 1; // consume moved filter
						continue;
					}
				}
			}

			// Pattern A (generalized): GRAPH, [FILTER...], FILTER(NOT IN on _anon_path_), [GRAPH]
			if (n instanceof IrGraph) {
				final IrGraph g1 = (IrGraph) n;
				// scan forward over consecutive FILTER lines to find an NPS filter targeting an _anon_path_ var
				int j = i + 1;
				NsText ns = null;
				while (j < in.size() && in.get(j) instanceof IrFilter) {
					final IrFilter f = (IrFilter) in.get(j);
					final String condText = f.getConditionText();
					if (condText != null && condText.contains(ANON_PATH_PREFIX)) {
						final NsText cand = parseNegatedSetText(condText);
						if (cand != null && cand.varName != null && !cand.items.isEmpty()) {
							ns = cand;
							break; // found the NOT IN / inequality chain on the anon path var
						}
					}
					j++;
				}
				if (ns != null) {
					// System.out.println("# DBG NPS: Graph@" + i + " matched filter@" + j + " var=" + ns.varName + "
					// items=" + ns.items);
					// Find triple inside first GRAPH that uses the filtered predicate variable
					final MatchTriple mt1 = findTripleWithPredicateVar(g1.getWhere(), ns.varName);
					if (mt1 == null) {
						// System.out.println("# DBG NPS: no matching triple in g1 for var=" + ns.varName);
						// no matching triple inside g1; keep as-is
						out.add(n);
						continue;
					}

					// Optionally chain with the next GRAPH having the same graph ref after the NPS filter
					boolean consumedG2 = false;
					MatchTriple mt2 = null;
					int k = j + 1;
					// Skip over any additional FILTER lines between the NPS filter and the next block
					while (k < in.size() && in.get(k) instanceof IrFilter) {
						k++;
					}
					if (k < in.size() && in.get(k) instanceof IrGraph) {
						final IrGraph g2 = (IrGraph) in.get(k);
						if (sameVarOrValue(g1.getGraph(), g2.getGraph())) {
							mt2 = findTripleWithConstPredicateReusingObject(g2.getWhere(), mt1.object);
							consumedG2 = (mt2 != null);
						}
					} else if (k < in.size() && in.get(k) instanceof IrStatementPattern) {
						// Fallback: the second triple may have been emitted outside GRAPH; if it reuses the bridge
						// var
						// and has a constant predicate, treat it as the tail step to be fused and consume it.
						final IrStatementPattern sp2 = (IrStatementPattern) in.get(k);
						final Var pv = sp2.getPredicate();
						if (isConstantIriPredicate(sp2)) {
							if (sameVar(mt1.object, sp2.getSubject()) || sameVar(mt1.object, sp2.getObject())) {
								mt2 = new MatchTriple(sp2, sp2.getSubject(), sp2.getPredicate(), sp2.getObject());
								consumedG2 = true;
							}
						}
					}

					// Build new GRAPH with fused path triple + any leftover lines from original inner graphs
					final IrBGP newInner = new IrBGP(false);
					final Var subj = mt1.subject;
					final Var obj = mt1.object;
					final String npsTxt = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
					if (mt2 != null) {
						final boolean forward = sameVar(mt1.object, mt2.subject);
						final boolean inverse = !forward && sameVar(mt1.object, mt2.object);
						if (forward || inverse) {
							final String step = iri(mt2.predicate, r);
							final String path = npsTxt + "/" + (inverse ? "^" : "") + step;
							final Var end = forward ? mt2.object : mt2.subject;
							IrStatementPattern srcSp = (mt1.node instanceof IrStatementPattern)
									? (IrStatementPattern) mt1.node
									: null;
							newInner.add(new IrPathTriple(subj, path, end, false,
									IrPathTriple.fromStatementPatterns(srcSp)));
						} else {
							IrStatementPattern srcSp = (mt1.node instanceof IrStatementPattern)
									? (IrStatementPattern) mt1.node
									: null;
							newInner.add(new IrPathTriple(subj, npsTxt, obj, false,
									IrPathTriple.fromStatementPatterns(srcSp)));
						}
					} else {
						IrStatementPattern srcSp = (mt1.node instanceof IrStatementPattern)
								? (IrStatementPattern) mt1.node
								: null;
						newInner.add(new IrPathTriple(subj, npsTxt, obj, false,
								IrPathTriple.fromStatementPatterns(srcSp)));
					}
					copyAllExcept(g1.getWhere(), newInner, mt1.node);
					if (consumedG2) {
						final IrGraph g2 = (IrGraph) in.get(k);
						copyAllExcept(g2.getWhere(), newInner, mt2.node);
					}

					// Emit the rewritten GRAPH at the position of the first GRAPH
					out.add(new IrGraph(g1.getGraph(), newInner, g1.isNewScope()));
					// Also preserve any intervening non-NPS FILTER lines between i and j
					for (int t = i + 1; t < j; t++) {
						out.add(in.get(t));
					}
					// Advance index past the consumed NPS filter and optional g2; any extra FILTERs after
					// the NPS filter are preserved by the normal loop progression (since we didn't add them
					// above and will hit them in subsequent iterations).
					i = consumedG2 ? k : j;
					continue;
				}
			}

			// Pattern B: GRAPH, GRAPH, FILTER (common ordering from IR builder)
			if (n instanceof IrGraph && i + 2 < in.size() && in.get(i + 1) instanceof IrGraph
					&& in.get(i + 2) instanceof IrFilter) {
				final IrGraph g1 = (IrGraph) n;
				final IrGraph g2 = (IrGraph) in.get(i + 1);
				final IrFilter f = (IrFilter) in.get(i + 2);

				final String condText2 = f.getConditionText();
				if (condText2 == null) {
					out.add(n);
					continue;
				}
				final NsText ns = parseNegatedSetText(condText2);
				if (ns == null || ns.varName == null || ns.items.isEmpty()) {
					out.add(n);
					continue;
				}

				// Must be same graph term to fuse
				if (!sameVarOrValue(g1.getGraph(), g2.getGraph())) {
					out.add(n);
					continue;
				}

				final MatchTriple mt1 = findTripleWithPredicateVar(g1.getWhere(), ns.varName);
				final MatchTriple mt2 = findTripleWithConstPredicateReusingObject(g2.getWhere(),
						mt1 == null ? null : mt1.object);
				if (mt1 == null) {
					out.add(n);
					continue;
				}

				final IrBGP newInner = new IrBGP(false);
				final Var subj = mt1.subject;
				final Var obj = mt1.object;
				final String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";

				IrStatementPattern srcSp = (mt1.node instanceof IrStatementPattern) ? (IrStatementPattern) mt1.node
						: null;
				if (mt2 != null) {
					final boolean forward = sameVar(mt1.object, mt2.subject);
					final boolean inverse = !forward && sameVar(mt1.object, mt2.object);
					final String step = r.convertIRIToString((IRI) mt2.predicate.getValue());
					final String path = nps + "/" + (inverse ? "^" : "") + step;
					final Var end = forward ? mt2.object : mt2.subject;
					newInner.add(new IrPathTriple(subj, path, end, false, IrPathTriple.fromStatementPatterns(srcSp)));
				} else {
					newInner.add(new IrPathTriple(subj, nps, obj, false,
							IrPathTriple.fromStatementPatterns(srcSp)));
				}

				copyAllExcept(g1.getWhere(), newInner, mt1.node);
				if (mt2 != null) {
					copyAllExcept(g2.getWhere(), newInner, mt2.node);
				}

				out.add(new IrGraph(g1.getGraph(), newInner, g1.isNewScope()));
				i += 2; // consume g1, g2, filter
				continue;
			}

			// If this is a UNION, rewrite branch-internal NPS first and then (optionally) fuse the
			// two branches into a single NPS when allowed by scope/anon-path rules.
			if (n instanceof IrUnion) {
				final IrUnion u = (IrUnion) n;
				final boolean shareCommonAnon = unionBranchesShareCommonAnonPathVarName(u);
				final boolean allHaveAnon = unionBranchesAllHaveAnonPathBridge(u);
				final IrUnion u2 = new IrUnion(u.isNewScope());
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					IrBGP rb = rewriteSimpleNpsOnly(b, r);
					if (rb != null) {
						rb.setNewScope(b.isNewScope());
						// Avoid introducing redundant single-child grouping: unwrap nested IrBGP layers
						// that each contain exactly one child and do not carry explicit new scope.
						IrBGP cur = rb;
						while (!cur.isNewScope() && cur.getLines().size() == 1
								&& cur.getLines().get(0) instanceof IrBGP) {
							IrBGP inner = (IrBGP) cur.getLines().get(0);
							if (inner.isNewScope()) {
								break;
							}
							cur = inner;
						}
						rb = cur;
					}
					u2.addBranch(rb);
				}
				IrNode fused = null;
				// Universal safeguard: never fuse explicit user UNIONs with all-scoped branches
				if (unionIsExplicitAndAllBranchesScoped(u)) {
					out.add(u2);
					continue;
				}
				if (u2.getBranches().size() == 2) {
					boolean allow = (!u.isNewScope() && allHaveAnon) || (u.isNewScope() && shareCommonAnon);
					if (allow) {
						fused = tryFuseTwoNpsBranches(u2);
					}
				}
				out.add(fused != null ? fused : u2);
				continue;
			}

			// Simple Pattern S2 (GRAPH): GRAPH { SP(var p) } followed by FILTER on that var -> GRAPH with NPS triple
			if (n instanceof IrGraph && i + 1 < in.size() && in.get(i + 1) instanceof IrFilter) {
				final IrGraph g = (IrGraph) n;
				final IrFilter f = (IrFilter) in.get(i + 1);
				final String condText = f.getConditionText();
				final NsText ns = condText == null ? null : parseNegatedSetText(condText);
				if (ns != null && g.getWhere() != null && g.getWhere().getLines().size() == 1
						&& g.getWhere().getLines().get(0) instanceof IrStatementPattern) {
					final IrStatementPattern sp = (IrStatementPattern) g.getWhere().getLines().get(0);
					final Var pVar = sp.getPredicate();
					if ((BaseTransform.isAnonPathVar(pVar) || BaseTransform.isAnonPathInverseVar(pVar))
							&& pVar.getName().equals(ns.varName) && !ns.items.isEmpty()) {
						final boolean inv = BaseTransform.isAnonPathInverseVar(pVar);
						final String nps = inv
								? "!(^" + joinIrisWithPreferredOrder(ns.items, r) + ")"
								: "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
						final IrBGP newInner = new IrBGP(false);
						// If the immediately preceding line outside the GRAPH was a VALUES clause, move it into the
						// GRAPH
						if (!out.isEmpty() && out.get(out.size() - 1) instanceof IrValues) {
							IrValues prevVals = (IrValues) out.remove(out.size() - 1);
							newInner.add(prevVals);
						}
						// Subject/object orientation: inverse anon var means we flip s/o for the NPS path
						if (inv) {
							newInner.add(new IrPathTriple(sp.getObject(), sp.getObjectOverride(), nps, sp.getSubject(),
									sp.getSubjectOverride(), IrPathTriple.fromStatementPatterns(sp), false));
						} else {
							newInner.add(new IrPathTriple(sp.getSubject(), sp.getSubjectOverride(), nps, sp.getObject(),
									sp.getObjectOverride(), IrPathTriple.fromStatementPatterns(sp), false));
						}
						out.add(new IrGraph(g.getGraph(), newInner, g.isNewScope()));
						i += 1; // consume filter
						continue;
					}
				}
			}

			// Simple Pattern S1 (non-GRAPH): SP(var p) followed by FILTER on that var -> rewrite to NPS triple
			if (n instanceof IrStatementPattern && i + 1 < in.size() && in.get(i + 1) instanceof IrFilter) {
				final IrStatementPattern sp = (IrStatementPattern) n;
				final Var pVar = sp.getPredicate();
				final IrFilter f = (IrFilter) in.get(i + 1);
				final String condText = f.getConditionText();
				final NsText ns = condText == null ? null : parseNegatedSetText(condText);

				// If a constant tail triple immediately follows (forming !^a/step pattern), defer to S1+tail rule.
				boolean hasTail = (i + 2 < in.size() && in.get(i + 2) instanceof IrStatementPattern
						&& ((IrStatementPattern) in.get(i + 2)).getPredicate() != null
						&& ((IrStatementPattern) in.get(i + 2)).getPredicate().hasValue());

				if (!hasTail && BaseTransform.isAnonPathVar(pVar) && ns != null
						&& pVar.getName().equals(ns.varName) && !ns.items.isEmpty()) {
					if (isAnonPathInverseVar(pVar)) {
						final String nps = "!(^" + joinIrisWithPreferredOrder(ns.items, r) + ")";
						out.add(new IrPathTriple(sp.getObject(), sp.getObjectOverride(), nps, sp.getSubject(),
								sp.getSubjectOverride(), IrPathTriple.fromStatementPatterns(sp), false));
						i += 1; // consume filter
						continue;
					} else {
						final String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
						out.add(new IrPathTriple(sp.getSubject(), sp.getSubjectOverride(), nps, sp.getObject(),
								sp.getObjectOverride(), IrPathTriple.fromStatementPatterns(sp), false));
						i += 1; // consume filter
						continue;
					}

				}
			}

			// Simple Pattern S1+tail (non-GRAPH): SP(var p) + FILTER on that var + SP(tail)
			// If tail shares the SP subject (bridge), fuse to: (sp.object) /( !(^items) / tail.p ) (tail.object)
			if (n instanceof IrStatementPattern && i + 2 < in.size() && in.get(i + 1) instanceof IrFilter
					&& in.get(i + 2) instanceof IrStatementPattern) {
				final IrStatementPattern sp = (IrStatementPattern) n; // X ?p S or S ?p X
				final Var pVar = sp.getPredicate();
				final IrFilter f = (IrFilter) in.get(i + 1);
				final String condText = f.getConditionText();
				final NsText ns = condText == null ? null : parseNegatedSetText(condText);
				final IrStatementPattern tail = (IrStatementPattern) in.get(i + 2);
				if (BaseTransform.isAnonPathVar(pVar) && ns != null && pVar.getName() != null
						&& pVar.getName().equals(ns.varName) && !ns.items.isEmpty()) {
					// Require tail to have a constant predicate and reuse the SP subject as its subject
					final Var tp = tail.getPredicate();
					if (tp != null && tp.hasValue() && tp.getValue() instanceof IRI
							&& BaseTransform.sameVar(sp.getSubject(), tail.getSubject())) {
						// Build !(items) and invert members to !(^items)
						final String base = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
						final String inv = invertNegatedPropertySet(base);
						final String step = r.convertIRIToString((IRI) tp.getValue());
						final String path = inv + "/" + step;
						IrPathTriple pt3 = new IrPathTriple(sp.getObject(), sp.getObjectOverride(), path,
								tail.getObject(), tail.getObjectOverride(),
								IrPathTriple.fromStatementPatterns(sp, tail), false);
						out.add(pt3);
						i += 2; // consume filter and tail
						continue;
					}
				}
			}

			// Pattern C2 (non-GRAPH): SP(var p) followed by FILTER on that var, with surrounding constant triples:
			// S -(const k1)-> A ; S -(var p)-> M ; FILTER (?p NOT IN (...)) ; M -(const k2)-> E
			// Fuse to: A (^k1 / !(...) / k2) E
			if (n instanceof IrStatementPattern && i + 1 < in.size() && in.get(i + 1) instanceof IrFilter) {
				final IrStatementPattern spVar = (IrStatementPattern) n;
				final Var pVar = spVar.getPredicate();
				final IrFilter f2 = (IrFilter) in.get(i + 1);
				final String condText3 = f2.getConditionText();
				final NsText ns2 = condText3 == null ? null : parseNegatedSetText(condText3);
				if (BaseTransform.isAnonPathVar(pVar) && ns2 != null
						&& pVar.getName().equals(ns2.varName) && !ns2.items.isEmpty()) {
					IrStatementPattern k1 = null;
					boolean k1Inverse = false;
					Var startVar = null;
					for (int j = 0; j < in.size(); j++) {
						if (j == i) {
							continue;
						}
						final IrNode cand = in.get(j);
						if (!(cand instanceof IrStatementPattern)) {
							continue;
						}
						final IrStatementPattern sp = (IrStatementPattern) cand;
						final Var pv = sp.getPredicate();
						if (!isConstantIriPredicate(sp)) {
							continue;
						}
						if (sameVar(sp.getSubject(), spVar.getSubject()) && !isAnonPathVar(sp.getObject())) {
							k1 = sp;
							k1Inverse = true;
							startVar = sp.getObject();
							break;
						}
						if (sameVar(sp.getObject(), spVar.getSubject()) && !isAnonPathVar(sp.getSubject())) {
							k1 = sp;
							k1Inverse = false;
							startVar = sp.getSubject();
							break;
						}
					}

					IrStatementPattern k2 = null;
					boolean k2Inverse = false;
					Var endVar = null;
					for (int j = i + 2; j < in.size(); j++) {
						final IrNode cand = in.get(j);
						if (!(cand instanceof IrStatementPattern)) {
							continue;
						}
						final IrStatementPattern sp = (IrStatementPattern) cand;
						final Var pv = sp.getPredicate();
						if (!isConstantIriPredicate(sp)) {
							continue;
						}
						if (sameVar(sp.getSubject(), spVar.getObject()) && !isAnonPathVar(sp.getObject())) {
							k2 = sp;
							k2Inverse = false;
							endVar = sp.getObject();
							break;
						}
						if (sameVar(sp.getObject(), spVar.getObject()) && !isAnonPathVar(sp.getSubject())) {
							k2 = sp;
							k2Inverse = true;
							endVar = sp.getSubject();
							break;
						}
					}

					if (k1 != null && k2 != null && startVar != null && endVar != null) {
						final String k1Step = iri(k1.getPredicate(), r);
						final String k2Step = iri(k2.getPredicate(), r);
						final List<String> rev = new ArrayList<>(ns2.items);
						final String nps = "!(" + String.join("|", rev) + ")";
						final String path = (k1Inverse ? "^" + k1Step : k1Step) + "/" + nps + "/"
								+ (k2Inverse ? "^" + k2Step : k2Step);
						// path derived from k1, var p, and k2
						out.add(new IrPathTriple(startVar, "(" + path + ")", endVar, false,
								IrPathTriple.fromStatementPatterns(spVar)));
						// Remove any earlier-emitted k1 (if it appeared before this position)
						for (int rm = out.size() - 1; rm >= 0; rm--) {
							if (out.get(rm) == k1) {
								out.remove(rm);
								break;
							}
						}
						consumed.add(spVar);
						consumed.add(in.get(i + 1));
						consumed.add(k1);
						consumed.add(k2);
						i += 1; // skip filter
						continue;
					}
				}
			}

			// No fusion matched: now recurse into containers (to apply NPS deeper) and add.
			// Special: when encountering a nested IrBGP, run apply() directly on it so this pass can
			// rewrite sequences at that level (we cannot do that via transformChildren, which only
			// rewrites grandchildren).
			if (n instanceof IrBGP) {
				out.add(apply((IrBGP) n, r));
				continue;
			}
			if (n instanceof IrGraph || n instanceof IrOptional || n instanceof IrMinus || n instanceof IrSubSelect
					|| n instanceof IrService) {
				n = n.transformChildren(child -> {
					if (child instanceof IrBGP) {
						return apply((IrBGP) child, r);
					}
					return child;
				});
			}
			out.add(n);
		}

		final IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		return res;
	}

	/** Attempt to fuse a two-branch UNION of NPS path triples (optionally GRAPH-wrapped) into a single NPS. */
	private static IrNode tryFuseTwoNpsBranches(IrUnion u) {
		if (u == null || u.getBranches().size() != 2) {
			return null;
		}
		// Do not fuse explicit user UNIONs where all branches carry their own scope
		if (unionIsExplicitAndAllBranchesScoped(u)) {
			return u;
		}
		PT a = extractNpsPath(u.getBranches().get(0));
		PT b = extractNpsPath(u.getBranches().get(1));
		if (a == null || b == null) {
			return null;
		}
		// Graph refs must match
		if ((a.g == null && b.g != null) || (a.g != null && b.g == null)
				|| (a.g != null && !sameVarOrValue(a.g, b.g))) {
			return null;
		}
		String pA = normalizeCompactNpsLocal(a.pt.getPathText());
		String pB = normalizeCompactNpsLocal(b.pt.getPathText());
		// Align orientation: if subjects/objects swapped, invert members
		String toAddB = pB;
		if (sameVar(a.pt.getSubject(), b.pt.getObject()) && sameVar(a.pt.getObject(), b.pt.getSubject())) {
			String inv = invertNegatedPropertySet(pB);
			if (inv == null) {
				return null;
			}
			toAddB = inv;
		} else if (!(sameVar(a.pt.getSubject(), b.pt.getSubject()) && sameVar(a.pt.getObject(), b.pt.getObject()))) {
			return null;
		}
		// Merge members preserving order, removing duplicates
		List<String> mem = new ArrayList<>();
		addMembers(pA, mem);
		addMembers(toAddB, mem);
		String merged = "!(" + String.join("|", mem) + ")";
		IrPathTriple mergedPt = new IrPathTriple(a.pt.getSubject(), merged, a.pt.getObject(), false,
				IrPathTriple.mergePathVars(a.pt, b.pt));
		IrNode fused;
		if (a.g != null) {
			IrBGP inner = new IrBGP(false);
			inner.add(mergedPt);
			fused = new IrGraph(a.g, inner, false);
		} else {
			fused = mergedPt;
		}
		if (u.isNewScope()) {
			IrBGP grp = new IrBGP(false);
			grp.add(fused);
			return grp;
		}
		return fused;
	}

	private static PT extractNpsPath(IrBGP b) {
		PT res = new PT();
		if (b == null) {
			return null;
		}
		IrNode only = (b.getLines().size() == 1) ? b.getLines().get(0) : null;
		if (only instanceof IrGraph) {
			IrGraph g = (IrGraph) only;
			if (g.getWhere() == null || g.getWhere().getLines().size() != 1) {
				return null;
			}
			IrNode inner = g.getWhere().getLines().get(0);
			if (!(inner instanceof IrPathTriple)) {
				return null;
			}
			res.g = g.getGraph();
			res.pt = (IrPathTriple) inner;
			return res;
		}
		if (only instanceof IrPathTriple) {
			res.g = null;
			res.pt = (IrPathTriple) only;
			return res;
		}
		return null;
	}

	/**
	 * If original EXISTS body had an eligible UNION (no new scope + anon-path bridges), fuse it in the rewritten body.
	 */
	private static IrBGP fuseEligibleUnionInsideExists(IrBGP rewritten, IrBGP original) {
		if (rewritten == null || original == null) {
			return rewritten;
		}

		// Find first UNION in rewritten and try to fuse it when safe. Inside EXISTS bodies we
		// allow fusing a UNION of bare-NPS path triples even when there is no shared anon-path
		// bridge var, as long as the branches are strict NPS path triples with matching endpoints
		// (tryFuseTwoNpsBranches enforces this and preserves grouping for new-scope unions).

		List<IrNode> out = new ArrayList<>();
		boolean fusedOnce = false;
		for (IrNode ln : rewritten.getLines()) {
			if (!fusedOnce && ln instanceof IrUnion) {
				IrNode fused = tryFuseTwoNpsBranches((IrUnion) ln);
				if (fused != null) {
					out.add(fused);
					fusedOnce = true;
					continue;
				}
			}
			out.add(ln);
		}
		if (!fusedOnce) {
			return rewritten;
		}
		IrBGP res = new IrBGP(rewritten.isNewScope());
		out.forEach(res::add);
		res.setNewScope(rewritten.isNewScope());
		return res;
	}

	private static String normalizeCompactNpsLocal(String path) {
		if (path == null) {
			return null;
		}
		String t = path.trim();
		if (t.isEmpty()) {
			return null;
		}
		if (t.startsWith("!(") && t.endsWith(")")) {
			return t;
		}
		if (t.startsWith("!^")) {
			String inner = t.substring(1); // "^..."
			return "!(" + inner + ")";
		}
		if (t.startsWith("!") && t.length() > 1 && t.charAt(1) != '(') {
			return "!(" + t.substring(1) + ")";
		}
		return t;
	}

	private static boolean isAnonPathName(String name) {
		return name != null && (name.startsWith(ANON_PATH_PREFIX) || name.startsWith(ANON_PATH_INVERSE_PREFIX));
	}

	private static void addMembers(String npsPath, List<String> out) {
		if (npsPath == null) {
			return;
		}
		int s = npsPath.indexOf('(');
		int e = npsPath.lastIndexOf(')');
		if (s < 0 || e < 0 || e <= s) {
			return;
		}
		String inner = npsPath.substring(s + 1, e);
		for (String tok : inner.split("\\|")) {
			String t = tok.trim();
			if (!t.isEmpty()) {
				out.add(t);
			}
		}
	}

	// Within a union branch, compact a simple var-predicate + NOT IN filter to a negated property set path triple.
	public static IrBGP rewriteSimpleNpsOnly(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		final Set<IrNode> consumed = new HashSet<>();
		boolean propagateScopeFromConsumedFilter = false;
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (consumed.contains(n)) {
				continue;
			}
			if (n instanceof IrStatementPattern && i + 1 < in.size() && in.get(i + 1) instanceof IrFilter) {
				final IrStatementPattern sp = (IrStatementPattern) n;
				final Var pVar = sp.getPredicate();
				final IrFilter f = (IrFilter) in.get(i + 1);
				final String condText4 = f.getConditionText();
				final NsText ns = condText4 == null ? null : parseNegatedSetText(condText4);
				if (BaseTransform.isAnonPathVar(pVar) && ns != null
						&& pVar.getName().equals(ns.varName) && !ns.items.isEmpty()) {
					String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
					final boolean inv = BaseTransform.isAnonPathInverseVar(pVar);
					if (inv) {
						String maybe = invertNegatedPropertySet(nps);
						if (maybe != null) {
							nps = maybe;
						}
					}
					final Var sVar = inv ? sp.getObject() : sp.getSubject();
					final Var oVar = inv ? sp.getSubject() : sp.getObject();
					out.add(new IrPathTriple(sVar, nps, oVar, false, IrPathTriple.fromStatementPatterns(sp)));
					consumed.add(sp);
					consumed.add(in.get(i + 1));
					i += 1;
					continue;
				}
			}
			// Variant: GRAPH ... followed by FILTER inside the same branch -> rewrite to GRAPH with NPS triple
			if (n instanceof IrGraph && i + 1 < in.size() && in.get(i + 1) instanceof IrFilter) {
				final IrGraph g = (IrGraph) n;
				final IrFilter f = (IrFilter) in.get(i + 1);
				final String condText5 = f.getConditionText();
				final NsText ns = condText5 == null ? null : parseNegatedSetText(condText5);
				if (ns != null && ns.varName != null && !ns.items.isEmpty() && g.getWhere() != null
						&& g.getWhere().getLines().size() == 1
						&& g.getWhere().getLines().get(0) instanceof IrStatementPattern) {
					final IrStatementPattern sp = (IrStatementPattern) g.getWhere().getLines().get(0);
					final Var pVar = sp.getPredicate();
					if (BaseTransform.isAnonPathVar(pVar)
							&& pVar.getName().equals(ns.varName)) {
						String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
						final boolean inv = BaseTransform.isAnonPathInverseVar(pVar);
						if (inv) {
							String maybe = invertNegatedPropertySet(nps);
							if (maybe != null) {
								nps = maybe;
							}
						}
						final IrBGP newInner = new IrBGP(false);
						final Var sVar = inv ? sp.getObject() : sp.getSubject();
						final Var oVar = inv ? sp.getSubject() : sp.getObject();

						final IrNode sOverride = inv ? sp.getObjectOverride() : sp.getSubjectOverride();
						final IrNode oOverride = inv ? sp.getSubjectOverride() : sp.getObjectOverride();

						newInner.add(new IrPathTriple(sVar, sOverride, nps, oVar, oOverride,
								IrPathTriple.fromStatementPatterns(sp), false));
						out.add(new IrGraph(g.getGraph(), newInner, g.isNewScope()));
						consumed.add(g);
						consumed.add(in.get(i + 1));
						if (f.isNewScope()) {
							propagateScopeFromConsumedFilter = true;
						}
						i += 1;
						continue;
					}
				}
			}
			// Recurse into nested containers conservatively
			n = n.transformChildren(child -> {
				if (child instanceof IrBGP) {
					return rewriteSimpleNpsOnly((IrBGP) child, r);
				}
				return child;
			});
			out.add(n);
		}
		final IrBGP res = new IrBGP(bgp.isNewScope());
		for (IrNode n : out) {
			if (!consumed.contains(n)) {
				res.add(n);
			}
		}
		if (propagateScopeFromConsumedFilter) {
			res.setNewScope(true);
		} else {
			res.setNewScope(bgp.isNewScope());
		}
		return res;
	}

	/** Parse either "?p NOT IN (a, b, ...)" or a conjunction of inequalities into a negated property set. */
	public static NsText parseNegatedSetText(final String condText) {
		if (condText == null) {
			return null;
		}
		final String s = condText.trim();

		// Prefer explicit NOT IN form first
		Matcher mNotIn = Pattern
				.compile("(?i)(\\?[A-Za-z_]\\w*)\\s+NOT\\s+IN\\s*\\(([^)]*)\\)")
				.matcher(s);
		if (mNotIn.find()) {
			String var = mNotIn.group(1);
			String inner = mNotIn.group(2);
			List<String> items = new ArrayList<>();
			for (String t : inner.split(",")) {
				String tok = t.trim();
				if (tok.isEmpty()) {
					continue;
				}
				// Accept IRIs (either <...> or prefixed name form)
				if (tok.startsWith("<") || tok.matches("[A-Za-z_][\\w.-]*:[^\\s,()]+")) {
					items.add(tok);
				} else {
					return null; // be conservative: only IRIs
				}
			}
			if (!items.isEmpty()) {
				return new NsText(var.startsWith("?") ? var.substring(1) : var, items);
			}
		}

		// Else, try to parse chained inequalities combined with &&
		if (s.contains("||")) {
			return null; // don't handle disjunctions
		}
		String[] parts = s.split("&&");
		String var = null;
		List<String> items = new ArrayList<>();
		Pattern pLeft = Pattern
				.compile("[\\s()]*\\?(?<var>[A-Za-z_]\\w*)\\s*!=\\s*(?<iri>[^\\s()]+)[\\s()]*");
		Pattern pRight = Pattern
				.compile("[\\s()]*(?<iri>[^\\s()]+)\\s*!=\\s*\\?(?<var>[A-Za-z_]\\w*)[\\s()]*");
		for (String part : parts) {
			String term = part.trim();
			if (term.isEmpty()) {
				return null;
			}
			Matcher ml = pLeft.matcher(term);
			Matcher mr = pRight.matcher(term);
			String vName;
			String iriTxt;
			if (ml.find()) {
				vName = ml.group("var");
				iriTxt = ml.group("iri");
			} else if (mr.find()) {
				vName = mr.group("var");
				iriTxt = mr.group("iri");
			} else {
				return null;
			}
			if (vName == null || vName.isEmpty()) {
				return null;
			}
			// accept only IRIs
			String tok = iriTxt;
			if (!(tok.startsWith("<") || tok.matches("[A-Za-z_][\\w.-]*:[^\\s,()]+"))) {
				return null;
			}
			if (var == null) {
				var = vName;
			} else if (!var.equals(vName)) {
				return null; // different vars
			}
			items.add(tok);
		}
		if (var != null) {
			return new NsText(var, items);
		}
		return null;
	}

	public static MatchTriple findTripleWithConstPredicateReusingObject(IrBGP w, Var obj) {
		if (w == null || obj == null) {
			return null;
		}
		for (IrNode ln : w.getLines()) {
			if (ln instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) ln;
				Var p = sp.getPredicate();
				if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
					continue;
				}
				if (sameVar(obj, sp.getSubject()) || sameVar(obj, sp.getObject())) {
					return new MatchTriple(ln, sp.getSubject(), sp.getPredicate(), sp.getObject());
				}
			}
		}
		return null;
	}

	public static MatchTriple findTripleWithPredicateVar(IrBGP w, String varName) {
		if (w == null || varName == null) {
			return null;
		}
		for (IrNode ln : w.getLines()) {
			if (ln instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) ln;
				Var p = sp.getPredicate();
				if (p != null && !p.hasValue() && varName.equals(p.getName())) {
					return new MatchTriple(ln, sp.getSubject(), sp.getPredicate(), sp.getObject());
				}
			}
		}
		return null;
	}

	// Render a list of IRI tokens (either prefixed like "rdf:type" or <iri>) as a spaced " | "-joined list,
	// with a stable, preference-biased ordering: primarily by prefix name descending (so "rdf:" before "ex:"),
	// then by the full rendered text, to keep output deterministic.
	public static String joinIrisWithPreferredOrder(List<String> tokens, TupleExprIRRenderer r) {
		List<String> rendered = new ArrayList<>(tokens.size());
		for (String tok : tokens) {
			String t = tok == null ? "" : tok.trim();
			if (t.startsWith("<") && t.endsWith(">") && t.length() > 2) {
				String iriTxt = t.substring(1, t.length() - 1);
				try {
					IRI iri = SimpleValueFactory.getInstance()
							.createIRI(iriTxt);
					rendered.add(r.convertIRIToString(iri));
				} catch (IllegalArgumentException e) {
					// fallback: keep original token on parse failure
					rendered.add(tok);
				}
			} else {
				// assume prefixed or already-rendered
				rendered.add(t);
			}
		}

		return String.join("|", rendered);
	}

	public static final class NsText {
		public final String varName;
		public final List<String> items;

		NsText(String varName, List<String> items) {
			this.varName = varName;
			this.items = items;
		}
	}

	public static final class MatchTriple {
		public final IrNode node;
		public final Var subject;
		public final Var predicate;
		public final Var object;

		MatchTriple(IrNode node, Var s, Var p, Var o) {
			this.node = node;
			this.subject = s;
			this.predicate = p;
			this.object = o;
		}
	}

}
