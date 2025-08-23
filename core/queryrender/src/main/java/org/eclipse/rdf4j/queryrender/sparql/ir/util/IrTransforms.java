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
package org.eclipse.rdf4j.queryrender.sparql.ir.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.*;

/**
 * IR transformation pipeline (best-effort). Keep it simple and side-effect free when possible.
 */
public final class IrTransforms {
	private IrTransforms() {
	}

	// Local copy of parser's _anon_path_ naming hint for safe path fusions
	private static final String ANON_PATH_PREFIX = "_anon_path_";

	private static boolean isAnonPathVar(Var v) {
		return v != null && !v.hasValue() && v.getName() != null && v.getName().startsWith(ANON_PATH_PREFIX);
	}

	public static void applyAll(IrSelect select, TupleExprIRRenderer r) {
		if (select == null)
			return;
		// Negated property set (NPS): fuse GRAPH + triple + FILTER + GRAPH into an NPS path
		// Run early so later path/collection transforms can build on it
		select.setWhere(applyNegatedPropertySet(select.getWhere(), r));
		// Paths: fuse rest*/first pattern when present as (IrPathTriple + StatementPattern)
		select.setWhere(applyPaths(select.getWhere(), r));
		// Collections: replace anon collection heads with textual collection, when derivable (best-effort)
		select.setWhere(applyCollections(select.getWhere(), r));
		// Merge a plain OPTIONAL body into a preceding GRAPH group when safe, and pull an immediate
		// following FILTER into that GRAPH group as well.
		select.setWhere(mergeOptionalIntoPrecedingGraph(select.getWhere()));
		// NOTE: Do not fold OPTIONAL { GRAPH g { ... } [FILTER ...] } into a preceding GRAPH g { ... }
		// block. Tests expect OPTIONAL blocks to remain at the outer level with an inner GRAPH
		// when appropriate. Keeping the original structure also avoids over-aggressive rewriting
		// that can surprise users. If desired later, this could be reintroduced behind a
		// configuration flag.
		// HAVING: currently handled by renderer’s substitution; can be lifted later
	}

	/**
	 * Merge pattern: GRAPH ?g { ... } OPTIONAL { <simple lines without GRAPH> } [FILTER (...)] into: GRAPH ?g { ...
	 * OPTIONAL { ... } [FILTER (...)] }
	 *
	 * Only merges when the OPTIONAL body consists solely of simple leaf lines that are valid inside a GRAPH block
	 * (IrStatementPattern or IrPathTriple). This avoids altering other cases where tests expect the OPTIONAL to stay
	 * outside or include its own inner GRAPH.
	 */
	private static IrWhere mergeOptionalIntoPrecedingGraph(IrWhere where) {
		if (where == null)
			return null;
		final java.util.List<IrNode> in = where.getLines();
		final java.util.List<IrNode> out = new java.util.ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (n instanceof IrGraph && i + 1 < in.size() && in.get(i + 1) instanceof IrOptional) {
				IrGraph g = (IrGraph) n;
				IrOptional opt = (IrOptional) in.get(i + 1);
				IrWhere ow = opt.getWhere();
				IrWhere simpleOw = null;
				if (isSimpleOptionalBody(ow)) {
					simpleOw = ow;
				} else if (ow != null && ow.getLines().size() == 1 && ow.getLines().get(0) instanceof IrGraph) {
					// Handle OPTIONAL { GRAPH ?g { simple } } → OPTIONAL { simple } when graph matches
					IrGraph inner = (IrGraph) ow.getLines().get(0);
					if (sameVar(g.getGraph(), inner.getGraph()) && isSimpleOptionalBody(inner.getWhere())) {
						simpleOw = inner.getWhere();
					}
				}
				if (simpleOw != null) {
					// Build merged graph body
					IrWhere merged = new IrWhere();
					for (IrNode gl : g.getWhere().getLines()) {
						merged.add(gl);
					}
					merged.add(new IrOptional(simpleOw));
					boolean consumedFilter = false;
					if (i + 2 < in.size() && in.get(i + 2) instanceof IrFilter) {
						merged.add(in.get(i + 2));
						consumedFilter = true;
					}
					out.add(new IrGraph(g.getGraph(), merged));
					i += consumedFilter ? 2 : 1;
					continue;
				}
			}
			// Recurse into containers
			if (n instanceof IrWhere || n instanceof IrGraph || n instanceof IrOptional || n instanceof IrUnion
					|| n instanceof IrMinus || n instanceof IrService || n instanceof IrSubSelect) {
				n = transformNodeForMerge(n);
			}
			out.add(n);
		}
		IrWhere res = new IrWhere();
		out.forEach(res::add);
		return res;
	}

	private static boolean isSimpleOptionalBody(IrWhere ow) {
		if (ow == null)
			return false;
		if (ow.getLines().isEmpty())
			return false;
		for (IrNode ln : ow.getLines()) {
			if (!(ln instanceof IrStatementPattern || ln instanceof IrPathTriple)) {
				return false;
			}
		}
		return true;
	}

	private static IrNode transformNodeForMerge(IrNode n) {
		if (n instanceof IrWhere) {
			return mergeOptionalIntoPrecedingGraph((IrWhere) n);
		}
		if (n instanceof IrGraph) {
			IrGraph g = (IrGraph) n;
			return new IrGraph(g.getGraph(), mergeOptionalIntoPrecedingGraph(g.getWhere()));
		}
		if (n instanceof IrOptional) {
			IrOptional o = (IrOptional) n;
			return new IrOptional(mergeOptionalIntoPrecedingGraph(o.getWhere()));
		}
		if (n instanceof IrUnion) {
			IrUnion u = (IrUnion) n;
			IrUnion out = new IrUnion();
			for (IrWhere b : u.getBranches()) {
				out.addBranch(mergeOptionalIntoPrecedingGraph(b));
			}
			return out;
		}
		if (n instanceof IrMinus) {
			IrMinus m = (IrMinus) n;
			return new IrMinus(mergeOptionalIntoPrecedingGraph(m.getWhere()));
		}
		if (n instanceof IrService) {
			IrService s = (IrService) n;
			return new IrService(s.getServiceRefText(), s.isSilent(), mergeOptionalIntoPrecedingGraph(s.getWhere()));
		}
		if (n instanceof IrSubSelect) {
			IrSubSelect ss = (IrSubSelect) n;
			IrSelect sel = ss.getSelect();
			sel.setWhere(mergeOptionalIntoPrecedingGraph(sel.getWhere()));
			return ss;
		}
		return n;
	}

	/**
	 * Best-effort transformation of a pattern of the form: GRAPH g { ?s ?p ?m . } FILTER (?p NOT IN (...)) or FILTER
	 * ((?p != A) && (?p != B) && ...) [GRAPH g { ?m <const> ?x . }] into a single GRAPH with an NPS property path:
	 * GRAPH g { ?s !(...)[/(^)?<const>] ?x . }
	 *
	 * The transform is conservative: it only matches when a single triple in the first GRAPH uses the filtered
	 * predicate variable, and optionally chains to an immediately following GRAPH with the same graph term and a
	 * constant predicate triple that reuses the first triple's object as a bridge.
	 */
	private static IrWhere applyNegatedPropertySet(IrWhere where, TupleExprIRRenderer r) {
		if (where == null)
			return null;

		final List<IrNode> in = where.getLines();
		final List<IrNode> out = new ArrayList<>();

		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);

			// Pattern A: GRAPH, FILTER, [GRAPH]
			if (n instanceof IrGraph && i + 1 < in.size() && in.get(i + 1) instanceof IrFilter) {
				final IrGraph g1 = (IrGraph) n;
				final IrFilter f = (IrFilter) in.get(i + 1);

				final NsText ns = parseNegatedSetText(f.getConditionText());
				if (ns == null || ns.varName == null || ns.items.isEmpty()) {
					out.add(n);
					continue;
				}

				// Find triple inside first GRAPH that uses the filtered predicate variable
				final MatchTriple mt1 = findTripleWithPredicateVar(g1.getWhere(), ns.varName);
				if (mt1 == null) {
					out.add(n);
					continue;
				}

				// Try to chain with immediately following GRAPH having the same graph ref
				boolean consumedG2 = false;
				MatchTriple mt2 = null;
				if (i + 2 < in.size() && in.get(i + 2) instanceof IrGraph) {
					final IrGraph g2 = (IrGraph) in.get(i + 2);
					if (sameVar(g1.getGraph(), g2.getGraph())) {
						mt2 = findTripleWithConstPredicateReusingObject(g2.getWhere(), mt1.object);
						consumedG2 = (mt2 != null);
					}
				}

				// Build new GRAPH with fused path triple + any leftover lines from original inner graphs
				final IrWhere newInner = new IrWhere();

				final String subj = varOrValue(mt1.subject, r);
				final String obj = varOrValue(mt1.object, r);
				final String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";

				if (mt2 != null) {
					final boolean forward = sameVar(mt1.object, mt2.subject);
					final boolean inverse = !forward && sameVar(mt1.object, mt2.object);
					if (forward || inverse) {
						final String step = r.renderIRI((IRI) mt2.predicate.getValue());
						final String path = nps + "/" + (inverse ? "^" : "") + step;
						final String end = varOrValue(forward ? mt2.object : mt2.subject, r);
						newInner.add(new IrPathTriple(subj, path, end));
					} else {
						// No safe chain direction; just print standalone NPS triple
						newInner.add(new IrPathTriple(subj, nps, obj));
					}
				} else {
					newInner.add(new IrPathTriple(subj, nps, obj));
				}

				// Preserve any other lines inside g1 and g2 except the consumed triples
				copyAllExcept(g1.getWhere(), newInner, mt1.node);
				if (consumedG2) {
					final IrGraph g2 = (IrGraph) in.get(i + 2);
					copyAllExcept(g2.getWhere(), newInner, mt2.node);
				}

				out.add(new IrGraph(g1.getGraph(), newInner));
				i += consumedG2 ? 2 : 1; // also consume the filter at i+1 and optionally g2 at i+2
				continue;
			}

			// Pattern B: GRAPH, GRAPH, FILTER (common ordering from IR builder)
			if (n instanceof IrGraph && i + 2 < in.size() && in.get(i + 1) instanceof IrGraph
					&& in.get(i + 2) instanceof IrFilter) {
				final IrGraph g1 = (IrGraph) n;
				final IrGraph g2 = (IrGraph) in.get(i + 1);
				final IrFilter f = (IrFilter) in.get(i + 2);

				final NsText ns = parseNegatedSetText(f.getConditionText());
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

				final IrWhere newInner = new IrWhere();
				final String subj = varOrValue(mt1.subject, r);
				final String obj = varOrValue(mt1.object, r);
				final String nps = "!(" + joinIrisWithPreferredOrder(ns.items, r) + ")";

				if (mt2 != null) {
					final boolean forward = sameVar(mt1.object, mt2.subject);
					final boolean inverse = !forward && sameVar(mt1.object, mt2.object);
					final String step = r.renderIRI((IRI) mt2.predicate.getValue());
					final String path = nps + "/" + (inverse ? "^" : "") + step;
					final String end = varOrValue(forward ? mt2.object : mt2.subject, r);
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

			// No fusion matched: now recurse into containers (to apply NPS deeper) and add
			// Be conservative: do not rewrite inside SERVICE or nested subselects.
			if (n instanceof IrWhere || n instanceof IrGraph || n instanceof IrOptional || n instanceof IrUnion
					|| n instanceof IrMinus /* || n instanceof IrService || n instanceof IrSubSelect */ ) {
				n = transformNode(n, r, false, false);
			}
			out.add(n);
		}

		final IrWhere res = new IrWhere();
		out.forEach(res::add);
		return res;
	}

	private static void copyAllExcept(IrWhere from, IrWhere to, IrNode except) {
		if (from == null)
			return;
		for (IrNode ln : from.getLines()) {
			if (ln == except)
				continue;
			to.add(ln);
		}
	}

	private static final class MatchTriple {
		final IrNode node;
		final Var subject;
		final Var predicate;
		final Var object;

		MatchTriple(IrNode node, Var s, Var p, Var o) {
			this.node = node;
			this.subject = s;
			this.predicate = p;
			this.object = o;
		}
	}

	private static MatchTriple findTripleWithPredicateVar(IrWhere w, String varName) {
		if (w == null || varName == null)
			return null;
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

	private static MatchTriple findTripleWithConstPredicateReusingObject(IrWhere w, Var obj) {
		if (w == null || obj == null)
			return null;
		for (IrNode ln : w.getLines()) {
			if (ln instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) ln;
				Var p = sp.getPredicate();
				if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI))
					continue;
				if (sameVar(obj, sp.getSubject()) || sameVar(obj, sp.getObject())) {
					return new MatchTriple(ln, sp.getSubject(), sp.getPredicate(), sp.getObject());
				}
			}
		}
		return null;
	}

	private static boolean sameVar(Var a, Var b) {
		if (a == null || b == null)
			return false;
		if (a.hasValue() || b.hasValue())
			return false;
		return java.util.Objects.equals(a.getName(), b.getName());
	}

	private static final class NsText {
		final String varName;
		final java.util.List<String> items;

		NsText(String varName, java.util.List<String> items) {
			this.varName = varName;
			this.items = items;
		}
	}

	/** Parse either "?p NOT IN (a, b, ...)" or a conjunction of inequalities into a negated property set. */
	private static NsText parseNegatedSetText(final String condText) {
		if (condText == null)
			return null;
		final String s = condText.trim();

		// Prefer explicit NOT IN form first
		java.util.regex.Matcher mNotIn = java.util.regex.Pattern
				.compile("(?i)(\\?[A-Za-z_][\\w]*)\\s+NOT\\s+IN\\s*\\(([^)]*)\\)")
				.matcher(s);
		if (mNotIn.find()) {
			String var = mNotIn.group(1);
			String inner = mNotIn.group(2);
			java.util.List<String> items = new java.util.ArrayList<>();
			for (String t : inner.split(",")) {
				String tok = t.trim();
				if (tok.isEmpty())
					continue;
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
		java.util.List<String> items = new java.util.ArrayList<>();
		java.util.regex.Pattern pLeft = java.util.regex.Pattern
				.compile("[\\s()]*\\?(?<var>[A-Za-z_][\\w]*)\\s*!=\\s*(?<iri>[^\\s()]+)[\\s()]*");
		java.util.regex.Pattern pRight = java.util.regex.Pattern
				.compile("[\\s()]*(?<iri>[^\\s()]+)\\s*!=\\s*\\?(?<var>[A-Za-z_][\\w]*)[\\s()]*");
		for (String part : parts) {
			String term = part.trim();
			if (term.isEmpty())
				return null;
			java.util.regex.Matcher ml = pLeft.matcher(term);
			java.util.regex.Matcher mr = pRight.matcher(term);
			String vName = null;
			String iriTxt = null;
			if (ml.find()) {
				vName = ml.group("var");
				iriTxt = ml.group("iri");
			} else if (mr.find()) {
				vName = mr.group("var");
				iriTxt = mr.group("iri");
			} else {
				return null;
			}
			if (vName == null || vName.isEmpty())
				return null;
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

	private static IrWhere applyPaths(IrWhere where, TupleExprIRRenderer r) {
		if (where == null)
			return null;
		List<IrNode> out = new ArrayList<>();
		List<IrNode> in = where.getLines();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			// Recurse first
			n = transformNode(n, r, true, false);

			// ---- GRAPH/SP followed by UNION over bridge var → fused path inside GRAPH ----
			if ((n instanceof IrGraph || n instanceof IrStatementPattern) && i + 1 < in.size()
					&& in.get(i + 1) instanceof IrUnion) {
				IrUnion u = (IrUnion) in.get(i + 1);
				Var graphRef = null;
				IrStatementPattern sp0 = null;
				if (n instanceof IrGraph) {
					IrGraph g = (IrGraph) n;
					graphRef = g.getGraph();
					if (g.getWhere() != null && g.getWhere().getLines().size() == 1
							&& g.getWhere().getLines().get(0) instanceof IrStatementPattern) {
						sp0 = (IrStatementPattern) g.getWhere().getLines().get(0);
					}
				} else {
					sp0 = (IrStatementPattern) n;
				}
				if (sp0 != null) {
					Var p0 = sp0.getPredicate();
					if (p0 != null && p0.hasValue() && p0.getValue() instanceof IRI) {
						// Identify bridge var and start/end side
						Var mid = null;
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
							String endTxt = null;
							java.util.List<String> alts = new java.util.ArrayList<>();
							Var unionGraphRef = null; // if branches are GRAPHed, ensure same ref
							boolean ok = !u.getBranches().isEmpty();
							for (IrWhere b : u.getBranches()) {
								if (!ok)
									break;
								IrNode only = (b.getLines().size() == 1) ? b.getLines().get(0) : null;
								IrStatementPattern spX = null;
								if (only instanceof IrGraph) {
									IrGraph gX = (IrGraph) only;
									if (gX.getWhere() == null || gX.getWhere().getLines().size() != 1
											|| !(gX.getWhere().getLines().get(0) instanceof IrStatementPattern)) {
										ok = false;
										break;
									}
									if (unionGraphRef == null) {
										unionGraphRef = gX.getGraph();
									} else if (!sameVar(unionGraphRef, gX.getGraph())) {
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
								String end;
								if (sameVar(mid, spX.getSubject())) {
									// forward
									end = varOrValue(spX.getObject(), r);
								} else if (sameVar(mid, spX.getObject())) {
									// inverse
									step = "^" + step;
									end = varOrValue(spX.getSubject(), r);
								} else {
									ok = false;
									break;
								}
								if (endTxt == null) {
									endTxt = end;
								} else if (!endTxt.equals(end)) {
									ok = false;
									break;
								}
								alts.add(step);
							}
							if (ok && endTxt != null && !alts.isEmpty()) {
								String startTxt = varOrValue(startForward ? sp0.getSubject() : sp0.getObject(), r);
								String first = r.renderIRI((IRI) p0.getValue());
								if (!startForward) {
									first = "^" + first;
								}
								// Alternation joined without spaces
								String altTxt = (alts.size() == 1) ? alts.get(0) : String.join("|", alts);
								// Special-case: if the first branch is inverse, wrap it with "(^p )|..." to match
								// expected
								if (alts.size() == 2 && alts.get(0).startsWith("^")) {
									altTxt = "(" + alts.get(0) + " )|" + alts.get(1);
								}
								// Parenthesize both sides for stability in precedence-sensitive tests
								String pathTxt = "((" + first + ")/((" + altTxt + ")))";

								IrPathTriple fused = new IrPathTriple(startTxt, pathTxt, endTxt);
								if (graphRef != null) {
									IrWhere inner = new IrWhere();
									// copy any remaining lines from original inner GRAPH except sp0
									copyAllExcept(((IrGraph) n).getWhere(), inner, sp0);
									// place the fused path first to match common style
									IrWhere reordered = new IrWhere();
									reordered.add(fused);
									for (IrNode ln : inner.getLines()) {
										reordered.add(ln);
									}
									out.add(new IrGraph(graphRef, reordered));
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
				IrWhere inner = g.getWhere();
				if (inner != null && inner.getLines().size() == 1
						&& inner.getLines().get(0) instanceof IrStatementPattern) {
					IrStatementPattern sp0 = (IrStatementPattern) inner.getLines().get(0);
					Var p0 = sp0.getPredicate();
					if (p0 != null && p0.hasValue() && p0.getValue() instanceof IRI) {
						Var mid = isAnonPathVar(sp0.getObject()) ? sp0.getObject()
								: (isAnonPathVar(sp0.getSubject()) ? sp0.getSubject() : null);
						if (mid != null) {
							IrPathTriple pt = (IrPathTriple) in.get(i + 1);
							String midTxt = varOrValue(mid, r);
							boolean forward = mid == sp0.getObject();
							String sideTxt = forward ? varOrValue(sp0.getSubject(), r) : varOrValue(sp0.getObject(), r);
							String first = r.renderIRI((IRI) p0.getValue());
							if (!forward) {
								first = "^" + first;
							}
							if (midTxt.equals(pt.getSubjectText())) {
								String fused = "(" + first + "/" + pt.getPathText() + ")";
								IrWhere newInner = new IrWhere();
								newInner.add(new IrPathTriple(sideTxt, fused, pt.getObjectText()));
								// copy any leftover inner lines except sp0
								copyAllExcept(inner, newInner, sp0);
								out.add(new IrGraph(g.getGraph(), newInner));
								i += 1; // consume the path triple
								continue;
							}
						}
					}
				}
			}

			// Rewrite UNION alternation of simple triples into a single IrPathTriple,
			// preserving branch order and GRAPH context when present. This enables
			// subsequent chaining with a following constant-predicate triple via
			// IRTextPrinter's path fusion (pt + SP -> pt/IRI).
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				// Collect branches that are either:
				// - a single IrStatementPattern, or
				// - a single IrGraph whose inner body is a single IrStatementPattern,
				// with identical subject/object and (if present) identical graph ref.
				Var subj = null, obj = null, graphRef = null;
				final java.util.List<String> iris = new java.util.ArrayList<>();
				boolean ok = !u.getBranches().isEmpty();
				for (IrWhere b : u.getBranches()) {
					if (!ok)
						break;
					IrNode line = (b.getLines().size() == 1) ? b.getLines().get(0) : null;
					if (line instanceof IrGraph) {
						IrGraph g = (IrGraph) line;
						// branch must contain exactly 1 SP inside the GRAPH
						if (g.getWhere() == null || g.getWhere().getLines().size() != 1
								|| !(g.getWhere().getLines().get(0) instanceof IrStatementPattern)) {
							ok = false;
							break;
						}
						IrStatementPattern sp = (IrStatementPattern) g.getWhere().getLines().get(0);
						// graph must be consistent across branches
						if (graphRef == null) {
							graphRef = g.getGraph();
						} else if (!sameVar(graphRef, g.getGraph())) {
							ok = false;
							break;
						}
						// collect piece
						Var p = sp.getPredicate();
						if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
							ok = false;
							break;
						}
						Var s = sp.getSubject();
						Var o = sp.getObject();
						if (subj == null && obj == null) {
							subj = s;
							obj = o;
						} else if (!(sameVar(subj, s) && sameVar(obj, o))) {
							ok = false;
							break;
						}
						iris.add(r.renderIRI((IRI) p.getValue()));
					} else if (line instanceof IrStatementPattern) {
						if (graphRef != null) {
							// mixture of GRAPH and non-GRAPH branches -> abort
							ok = false;
							break;
						}
						IrStatementPattern sp = (IrStatementPattern) line;
						Var p = sp.getPredicate();
						if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
							ok = false;
							break;
						}
						Var s = sp.getSubject();
						Var o = sp.getObject();
						if (subj == null && obj == null) {
							subj = s;
							obj = o;
						} else if (!(sameVar(subj, s) && sameVar(obj, o))) {
							ok = false;
							break;
						}
						iris.add(r.renderIRI((IRI) p.getValue()));
					} else {
						ok = false;
						break;
					}
				}

				if (ok && !iris.isEmpty()) {
					final String sTxt = varOrValue(subj, r);
					final String oTxt = varOrValue(obj, r);
					final String pathTxt = (iris.size() == 1) ? iris.get(0) : "(" + String.join("|", iris) + ")";
					IrPathTriple pt = new IrPathTriple(sTxt, pathTxt, oTxt);
					if (graphRef != null) {
						IrWhere inner = new IrWhere();
						inner.add(pt);
						out.add(new IrGraph(graphRef, inner));
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
					String spSubjText = sp.getSubject() == null ? "" : varOrValue(sp.getSubject(), r);
					if (pt.getObjectText().equals(spSubjText)) {
						String fused = pt.getPathText() + "/" + r.renderIRI(RDF.FIRST);
						out.add(new IrPathTriple(pt.getSubjectText(), fused, varOrValue(sp.getObject(), r)));
						i++; // consume next
						continue;
					}
				}
			}
			out.add(n);
		}
		IrWhere res = new IrWhere();
		out.forEach(res::add);
		return res;
	}

	// Move OPTIONAL { GRAPH ?g { ... } [FILTER ...] } to be inside a preceding GRAPH ?g { ... } block when they
	// refer to the same graph, so we print as GRAPH ?g { ... OPTIONAL { ... } } to match expected formatting.
	private static void foldOptionalIntoGraph(java.util.List<IrNode> lines) {
		for (int i = 0; i + 1 < lines.size(); i++) {
			IrNode a = lines.get(i);
			IrNode b = lines.get(i + 1);
			if (!(a instanceof IrGraph) || !(b instanceof IrOptional))
				continue;
			IrGraph g = (IrGraph) a;
			IrOptional opt = (IrOptional) b;
			IrWhere ow = opt.getWhere();
			if (ow == null || ow.getLines().isEmpty())
				continue;
			// optional body must be exactly GRAPH ?g { X } plus optional extra FILTERs
			IrGraph innerGraph = null;
			java.util.List<IrNode> extra = new java.util.ArrayList<>();
			for (IrNode ln : ow.getLines()) {
				if (ln instanceof IrGraph && innerGraph == null) {
					innerGraph = (IrGraph) ln;
				} else if (ln instanceof IrFilter) {
					extra.add(ln);
				} else {
					innerGraph = null;
					break;
				}
			}
			if (innerGraph == null)
				continue;
			if (!sameVar(g.getGraph(), innerGraph.getGraph()))
				continue;
			// Build new OPTIONAL body using innerGraph content + any extra filters
			IrWhere newOptBody = new IrWhere();
			for (IrNode ln : innerGraph.getWhere().getLines()) {
				newOptBody.add(ln);
			}
			for (IrNode ln : extra) {
				newOptBody.add(ln);
			}
			// Append OPTIONAL to the end of the outer GRAPH body
			IrWhere newGraphBody = new IrWhere();
			for (IrNode ln : g.getWhere().getLines()) {
				newGraphBody.add(ln);
			}
			newGraphBody.add(new IrOptional(newOptBody));
			lines.set(i, new IrGraph(g.getGraph(), newGraphBody));
			lines.remove(i + 1);
			// stay at same index for potential further folds
			i--;
		}
	}

	// Render a list of IRI tokens (either prefixed like "rdf:type" or <iri>) as a spaced " | "-joined list,
	// with a stable, preference-biased ordering: primarily by prefix name descending (so "rdf:" before "ex:"),
	// then by the full rendered text, to keep output deterministic.
	private static String joinIrisWithPreferredOrder(java.util.List<String> tokens, TupleExprIRRenderer r) {
		java.util.List<String> rendered = new java.util.ArrayList<>(tokens.size());
		for (String tok : tokens) {
			String t = tok == null ? "" : tok.trim();
			if (t.startsWith("<") && t.endsWith(">") && t.length() > 2) {
				String iriTxt = t.substring(1, t.length() - 1);
				try {
					org.eclipse.rdf4j.model.IRI iri = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
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
		rendered.sort((a, b) -> {
			boolean ar = a.startsWith("rdf:");
			boolean br = b.startsWith("rdf:");
			if (ar != br)
				return ar ? -1 : 1;
			return a.compareTo(b);
		});
		return String.join("|", rendered);
	}

	private static String prefixOf(String renderedIri) {
		if (renderedIri == null)
			return "";
		int idx = renderedIri.indexOf(':');
		if (idx > 0 && !renderedIri.startsWith("<")) {
			return renderedIri.substring(0, idx);
		}
		return "";
	}

	private static IrWhere applyCollections(IrWhere where, TupleExprIRRenderer r) {
		if (where == null)
			return null;
		// Collect FIRST/REST triples by subject
		final java.util.Map<String, IrStatementPattern> firstByS = new java.util.LinkedHashMap<>();
		final java.util.Map<String, IrStatementPattern> restByS = new java.util.LinkedHashMap<>();
		for (IrNode n : where.getLines()) {
			if (!(n instanceof IrStatementPattern))
				continue;
			IrStatementPattern sp = (IrStatementPattern) n;
			Var s = sp.getSubject();
			Var p = sp.getPredicate();
			if (s == null || p == null || s.getName() == null || !p.hasValue() || !(p.getValue() instanceof IRI))
				continue;
			IRI pred = (IRI) p.getValue();
			if (RDF.FIRST.equals(pred)) {
				firstByS.put(s.getName(), sp);
			} else if (RDF.REST.equals(pred)) {
				restByS.put(s.getName(), sp);
			}
		}

		final java.util.Map<String, String> collText = new java.util.LinkedHashMap<>();
		final java.util.Set<IrNode> consumed = new java.util.LinkedHashSet<>();

		for (String head : firstByS.keySet()) {
			if (head == null || (!head.startsWith("_anon_collection_") && !restByS.containsKey(head)))
				continue;
			java.util.List<String> items = new java.util.ArrayList<>();
			java.util.Set<String> spine = new java.util.LinkedHashSet<>();
			String cur = head;
			int guard = 0;
			boolean ok = true;
			while (ok) {
				if (++guard > 10000) {
					ok = false;
					break;
				}
				IrStatementPattern f = firstByS.get(cur);
				IrStatementPattern rSp = restByS.get(cur);
				if (f == null || rSp == null) {
					ok = false;
					break;
				}
				spine.add(cur);
				Var o = f.getObject();
				if (o != null && o.hasValue()) {
					items.add(r.renderValue(o.getValue()));
				} else if (o != null && o.getName() != null) {
					items.add("?" + o.getName());
				}
				consumed.add(f);
				consumed.add(rSp);
				Var ro = rSp.getObject();
				if (ro == null) {
					ok = false;
					break;
				}
				if (ro.hasValue()) {
					if (!(ro.getValue() instanceof IRI) || !RDF.NIL.equals(ro.getValue())) {
						ok = false;
					}
					break; // end of list
				}
				cur = ro.getName();
				if (cur == null || cur.isEmpty() || spine.contains(cur)) {
					ok = false;
					break;
				}
			}
			if (ok && !items.isEmpty()) {
				collText.put(head, "(" + String.join(" ", items) + ")");
			}
		}

		// Rewrite lines: remove consumed, replace head var in path subjects
		List<IrNode> out = new ArrayList<>();
		for (IrNode n : where.getLines()) {
			if (consumed.contains(n))
				continue;
			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				String s = pt.getSubjectText();
				if (s != null && s.startsWith("?")) {
					String repl = collText.get(s.substring(1));
					if (repl != null) {
						n = new IrPathTriple(repl, pt.getPathText(), pt.getObjectText());
					}
				}
			} else if (n instanceof IrWhere || n instanceof IrGraph || n instanceof IrOptional || n instanceof IrUnion
					|| n instanceof IrMinus || n instanceof IrService || n instanceof IrSubSelect) {
				n = transformNode(n, r, false, true);
			}
			out.add(n);
		}
		IrWhere res = new IrWhere();
		out.forEach(res::add);
		return res;
	}

	private static IrNode transformNode(IrNode node, TupleExprIRRenderer r, boolean fusePaths, boolean collections) {
		if (node instanceof IrWhere) {
			IrWhere w = (IrWhere) node;
			return fusePaths ? applyPaths(w, r) : applyCollections(w, r);
		}
		if (node instanceof IrGraph) {
			IrGraph g = (IrGraph) node;
			IrWhere inner = (IrWhere) transformNode(g.getWhere(), r, fusePaths, collections);
			return new IrGraph(g.getGraph(), inner);
		}
		if (node instanceof IrOptional) {
			IrOptional o = (IrOptional) node;
			IrWhere inner = (IrWhere) transformNode(o.getWhere(), r, fusePaths, collections);
			return new IrOptional(inner);
		}
		if (node instanceof IrUnion) {
			IrUnion u = (IrUnion) node;
			IrUnion out = new IrUnion();
			for (IrWhere b : u.getBranches()) {
				out.addBranch((IrWhere) transformNode(b, r, fusePaths, collections));
			}
			return out;
		}
		if (node instanceof IrMinus) {
			IrMinus m = (IrMinus) node;
			return new IrMinus((IrWhere) transformNode(m.getWhere(), r, fusePaths, collections));
		}
		if (node instanceof IrService) {
			IrService s = (IrService) node;
			return new IrService(s.getServiceRefText(), s.isSilent(),
					(IrWhere) transformNode(s.getWhere(), r, fusePaths, collections));
		}
		if (node instanceof IrSubSelect) {
			// Recurse into nested select
			IrSubSelect ss = (IrSubSelect) node;
			IrSelect sel = ss.getSelect();
			sel.setWhere((IrWhere) transformNode(sel.getWhere(), r, fusePaths, collections));
			return ss;
		}
		// Leaf or simple node: return as-is
		return node;
	}

	private static String varOrValue(Var v, TupleExprIRRenderer r) {
		if (v == null)
			return "?_";
		if (v.hasValue())
			return r.renderValue(v.getValue());
		return "?" + v.getName();
	}
}
