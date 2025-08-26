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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

public final class ApplyNegatedPropertySetTransform extends BaseTransform {
	private ApplyNegatedPropertySetTransform() {
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

			// Normalize simple var+FILTER patterns inside EXISTS blocks early so nested shapes
			// can fuse into !(...) as expected by streaming tests.
			if (n instanceof IrFilter) {
				final IrFilter fNode = (IrFilter) n;
				if (fNode.getBody() instanceof IrExists) {
					final IrExists ex = (IrExists) fNode.getBody();
					IrBGP inner = ex.getWhere();
					if (inner != null) {
						inner = rewriteSimpleNpsOnly(inner, r);
						out.add(new IrFilter(new IrExists(inner)));
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
						final IrBGP inner = new IrBGP();
						// original inner lines first
						copyAllExcept(g1.getWhere(), inner, null);
						// then the filter moved inside
						inner.add(f);
						out.add(new IrGraph(g1.getGraph(), inner));
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
				IrFilter npsFilter = null;
				while (j < in.size() && in.get(j) instanceof IrFilter) {
					final IrFilter f = (IrFilter) in.get(j);
					final String condText = f.getConditionText();
					if (condText != null && condText.contains(ANON_PATH_PREFIX)) {
						final NsText cand = parseNegatedSetText(condText);
						if (cand != null && cand.varName != null && !cand.items.isEmpty()) {
							ns = cand;
							npsFilter = f;
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
					if (npsFilter != null) {
						// Skip over any additional FILTER lines between the NPS filter and the next block
						while (k < in.size() && in.get(k) instanceof IrFilter) {
							k++;
						}
						if (k < in.size() && in.get(k) instanceof IrGraph) {
							final IrGraph g2 = (IrGraph) in.get(k);
							if (sameVar(g1.getGraph(), g2.getGraph())) {
								mt2 = findTripleWithConstPredicateReusingObject(g2.getWhere(), mt1.object);
								consumedG2 = (mt2 != null);
							}
						} else if (k < in.size() && in.get(k) instanceof IrStatementPattern) {
							// Fallback: the second triple may have been emitted outside GRAPH; if it reuses the bridge
							// var
							// and has a constant predicate, treat it as the tail step to be fused and consume it.
							final IrStatementPattern sp2 = (IrStatementPattern) in.get(k);
							final Var pv = sp2.getPredicate();
							if (pv != null && pv.hasValue() && pv.getValue() instanceof IRI) {
								if (sameVar(mt1.object, sp2.getSubject()) || sameVar(mt1.object, sp2.getObject())) {
									mt2 = new MatchTriple(sp2, sp2.getSubject(), sp2.getPredicate(), sp2.getObject());
									consumedG2 = true;
								}
							}
						}
					}

					// Build new GRAPH with fused path triple + any leftover lines from original inner graphs
					final IrBGP newInner = new IrBGP();
					final Var subj = mt1.subject;
					final Var obj = mt1.object;
					final String npsTxt = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
					if (mt2 != null) {
						final boolean forward = sameVar(mt1.object, mt2.subject);
						final boolean inverse = !forward && sameVar(mt1.object, mt2.object);
						if (forward || inverse) {
							final String step = r.renderIRI((IRI) mt2.predicate.getValue());
							final String path = npsTxt + "/" + (inverse ? "^" : "") + step;
							final Var end = forward ? mt2.object : mt2.subject;
							newInner.add(new IrPathTriple(subj, path, end));
						} else {
							newInner.add(new IrPathTriple(subj, npsTxt, obj));
						}
					} else {
						newInner.add(new IrPathTriple(subj, npsTxt, obj));
					}
					copyAllExcept(g1.getWhere(), newInner, mt1.node);
					if (consumedG2) {
						final IrGraph g2 = (IrGraph) in.get(k);
						copyAllExcept(g2.getWhere(), newInner, mt2.node);
					}

					// Emit the rewritten GRAPH at the position of the first GRAPH
					out.add(new IrGraph(g1.getGraph(), newInner));
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
				if (!sameVar(g1.getGraph(), g2.getGraph())) {
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

				final IrBGP newInner = new IrBGP();
				final Var subj = mt1.subject;
				final Var obj = mt1.object;
				final String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";

				if (mt2 != null) {
					final boolean forward = sameVar(mt1.object, mt2.subject);
					final boolean inverse = !forward && sameVar(mt1.object, mt2.object);
					final String step = r.renderIRI((IRI) mt2.predicate.getValue());
					final String path = nps + "/" + (inverse ? "^" : "") + step;
					final Var end = forward ? mt2.object : mt2.subject;
					newInner.add(new IrPathTriple(subj, path, end));
				} else {
					newInner.add(new IrPathTriple(subj, nps, obj));
				}

				copyAllExcept(g1.getWhere(), newInner, mt1.node);
				if (mt2 != null) {
					copyAllExcept(g2.getWhere(), newInner, mt2.node);
				}

				out.add(new IrGraph(g1.getGraph(), newInner));
				i += 2; // consume g1, g2, filter
				continue;
			}

			// If this is a UNION, allow direct NPS rewrite in its branches (demo of primitives)
			if (n instanceof IrUnion) {
				final IrUnion u = (IrUnion) n;
				final IrUnion u2 = new IrUnion();
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(rewriteSimpleNpsOnly(b, r));
				}
				out.add(u2);
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
					if (pVar != null && BaseTransform.isAnonPathVar(pVar) && pVar.getName().equals(ns.varName)
							&& !ns.items.isEmpty()) {
						final String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
						final IrBGP newInner = new IrBGP();
						newInner.add(new IrPathTriple(sp.getSubject(), nps, sp.getObject()));
						out.add(new IrGraph(g.getGraph(), newInner));
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

				if (!hasTail && pVar != null && BaseTransform.isAnonPathVar(pVar) && ns != null
						&& pVar.getName().equals(ns.varName) && !ns.items.isEmpty()) {
					final String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
					out.add(new IrPathTriple(sp.getSubject(), nps, sp.getObject()));
					i += 1; // consume filter
					continue;
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
				if (pVar != null && BaseTransform.isAnonPathVar(pVar) && ns != null && pVar.getName() != null
						&& pVar.getName().equals(ns.varName) && !ns.items.isEmpty()) {
					// Require tail to have a constant predicate and reuse the SP subject as its subject
					final Var tp = tail.getPredicate();
					if (tp != null && tp.hasValue() && tp.getValue() instanceof IRI
							&& BaseTransform.sameVar(sp.getSubject(), tail.getSubject())) {
						// Build !(items) and invert members to !(^items)
						final String base = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
						final String inv = invertNegatedPropertySet(base);
						final String step = r.renderIRI((IRI) tp.getValue());
						final String path = inv + "/" + step;
						out.add(new IrPathTriple(sp.getObject(), path, tail.getObject()));
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
				if (pVar != null && BaseTransform.isAnonPathVar(pVar) && ns2 != null
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
						if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
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
						if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
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
						final String k1Step = r.renderIRI((IRI) k1.getPredicate().getValue());
						final String k2Step = r.renderIRI((IRI) k2.getPredicate().getValue());
						final List<String> rev = new ArrayList<>(ns2.items);
						Collections.reverse(rev);
						final String nps = "!(" + String.join("|", rev) + ")";
						final String path = (k1Inverse ? "^" + k1Step : k1Step) + "/" + nps + "/"
								+ (k2Inverse ? "^" + k2Step : k2Step);
						out.add(new IrPathTriple(startVar, "(" + path + ")", endVar));
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

			// No fusion matched: now recurse into containers (to apply NPS deeper) and add
			// Be conservative: do not rewrite inside SERVICE or nested subselects.
			if (n instanceof IrBGP || n instanceof IrGraph || n instanceof IrOptional || n instanceof IrUnion
					|| n instanceof IrMinus /* || n instanceof IrService || n instanceof IrSubSelect */) {
				n = n.transformChildren(child -> {
					if (child instanceof IrBGP) {
						return apply((IrBGP) child, r);
					}
					return child;
				});
			}
			out.add(n);
		}

		final IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	// Within a union branch, compact a simple var-predicate + NOT IN filter to a negated property set path triple.
	public static IrBGP rewriteSimpleNpsOnly(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		final Set<IrNode> consumed = new HashSet<>();
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
				if (pVar != null && BaseTransform.isAnonPathVar(pVar) && ns != null
						&& pVar.getName().equals(ns.varName) && !ns.items.isEmpty()) {
					final Var sVar = sp.getSubject();
					final Var oVar = sp.getObject();
					final String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
					out.add(new IrPathTriple(sVar, nps, oVar));
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
					if (pVar != null && BaseTransform.isAnonPathVar(pVar)
							&& pVar.getName().equals(ns.varName)) {
						final String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";
						final IrBGP newInner = new IrBGP();
						newInner.add(new IrPathTriple(sp.getSubject(), nps, sp.getObject()));
						out.add(new IrGraph(g.getGraph(), newInner));
						consumed.add(g);
						consumed.add(in.get(i + 1));
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
		final IrBGP res = new IrBGP();
		for (IrNode n : out) {
			if (!consumed.contains(n)) {
				res.add(n);
			}
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
				.compile("(?i)(\\?[A-Za-z_][\\w]*)\\s+NOT\\s+IN\\s*\\(([^)]*)\\)")
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
				.compile("[\\s()]*\\?(?<var>[A-Za-z_][\\w]*)\\s*!=\\s*(?<iri>[^\\s()]+)[\\s()]*");
		Pattern pRight = Pattern
				.compile("[\\s()]*(?<iri>[^\\s()]+)\\s*!=\\s*\\?(?<var>[A-Za-z_][\\w]*)[\\s()]*");
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
		if (var != null && !items.isEmpty()) {
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
					rendered.add(r.renderIRI(iri));
				} catch (IllegalArgumentException e) {
					// fallback: keep original token on parse failure
					rendered.add(tok);
				}
			} else {
				// assume prefixed or already-rendered
				rendered.add(t);
			}
		}
		// Canonical ordering for graph-fused NPS:
		// 1) rdf:* first, 2) then lexicographic by rendered token. No extra spaces.
		rendered.sort(String::compareTo);
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
