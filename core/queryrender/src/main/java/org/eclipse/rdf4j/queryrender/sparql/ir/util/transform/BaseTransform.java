/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.queryrender.sparql.ir.util.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

public class BaseTransform {

	// Local copy of parser's _anon_path_ naming hint for safe path fusions
	public static final String ANON_PATH_PREFIX = "_anon_path_";

	public static void copyAllExcept(IrBGP from, IrBGP to, IrNode except) {
		if (from == null) {
			return;
		}
		for (IrNode ln : from.getLines()) {
			if (ln == except) {
				continue;
			}
			to.add(ln);
		}
	}

	/** Fuse adjacent IrPathTriple nodes when the first's object equals the second's subject. */
	public static IrBGP fuseAdjacentPtThenPt(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = bgp.getLines();
		List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (n instanceof IrPathTriple && i + 1 < in.size() && in.get(i + 1) instanceof IrPathTriple) {
				IrPathTriple a = (IrPathTriple) n;
				IrPathTriple b = (IrPathTriple) in.get(i + 1);
				Var bridge = a.getObject();
				if (sameVar(bridge, b.getSubject()) && isAnonPathVar(bridge)) {
					// Merge a and b: s -(a.path/b.path)-> o
					String fusedPath = "(" + a.getPathText() + ")/(" + b.getPathText() + ")";
					out.add(new IrPathTriple(a.getSubject(), fusedPath, b.getObject()));
					i += 1; // consume b
				} else if (sameVar(bridge, b.getObject()) && isAnonPathVar(bridge)) {
					// Merge a and b: s -(a.path/b.path)-> o
					String fusedPath = "(" + a.getPathText() + ")/^(" + b.getPathText() + ")";
					out.add(new IrPathTriple(a.getSubject(), fusedPath, b.getSubject()));
					i += 1; // consume b
				} else {
					// Additional cases: the bridge variable occurs as the subject of the first path triple.
					Var aSubj = a.getSubject();
					if (isAnonPathVar(aSubj)) {
						// Case: a.subject == b.subject -> compose by inverting 'a' and chaining forward with 'b'
						if (sameVar(aSubj, b.getSubject())) {
							String aPath = a.getPathText();
							String left = invertNegatedPropertySet(aPath);
							if (left == null) {
								left = "^(" + aPath + ")";
							}
							String fusedPath = left + "/(" + b.getPathText() + ")";
							out.add(new IrPathTriple(a.getObject(), fusedPath, b.getObject()));
							i += 1; // consume b
							continue;
						}

						// Case: a.subject == b.object -> compose by inverting both 'a' and 'b'
						if (sameVar(aSubj, b.getObject())) {
							String aPath = a.getPathText();
							String left = invertNegatedPropertySet(aPath);
							if (left == null) {
								left = "^(" + aPath + ")";
							}
							String right = "^(" + b.getPathText() + ")";
							String fusedPath = left + "/" + right;
							out.add(new IrPathTriple(a.getObject(), fusedPath, b.getSubject()));
							i += 1; // consume b
							continue;
						}
					}
					out.add(n);
				}
			} else {
				out.add(n);
			}
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	public static IrBGP fuseAdjacentSpThenPt(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = bgp.getLines();
		List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (i + 1 < in.size() && n instanceof IrStatementPattern && in.get(i + 1) instanceof IrPathTriple) {
				IrStatementPattern sp = (IrStatementPattern) n;
				Var p = sp.getPredicate();
				if (p != null && p.hasValue() && p.getValue() instanceof IRI) {
					IrPathTriple pt = (IrPathTriple) in.get(i + 1);
					if (sameVar(sp.getObject(), pt.getSubject()) && isAnonPathVar(pt.getSubject())) {
						String fused = r.renderIRI((IRI) p.getValue()) + "/" + pt.getPathText();
						out.add(new IrPathTriple(sp.getSubject(), fused, pt.getObject()));
						i += 1;
						continue;
					} else if (sameVar(sp.getSubject(), pt.getObject()) && isAnonPathVar(pt.getObject())) {
						String fused = pt.getPathText() + "/^" + r.renderIRI((IRI) p.getValue());
						out.add(new IrPathTriple(pt.getSubject(), fused, sp.getObject()));
						i += 1;
						continue;
					}
				}
			}
			out.add(n);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	public static IrBGP joinPathWithLaterSp(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = new ArrayList<>(bgp.getLines());
		List<IrNode> out = new ArrayList<>();
		Set<IrNode> removed = new HashSet<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (removed.contains(n)) {
				continue;
			}
			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				Var objVar = pt.getObject();
				if (isAnonPathVar(objVar)) {
					IrStatementPattern join = null;
					boolean inverse = false;
					for (int j = i + 1; j < in.size(); j++) {
						IrNode m = in.get(j);
						if (!(m instanceof IrStatementPattern)) {
							continue;
						}
						IrStatementPattern sp = (IrStatementPattern) m;
						Var pv = sp.getPredicate();
						if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
							continue;
						}
						if (sameVar(objVar, sp.getSubject()) && isAnonPathVar(sp.getObject())) {
							join = sp;
							inverse = false;
							break;
						}
						if (sameVar(objVar, sp.getObject()) && isAnonPathVar(sp.getSubject())) {
							join = sp;
							inverse = true;
							break;
						}
					}
					if (join != null) {
						String step = r.renderIRI((IRI) join.getPredicate().getValue());
						String newPath = pt.getPathText() + "/" + (inverse ? "^" : "") + step;
						Var newEnd = inverse ? join.getSubject() : join.getObject();
						pt = new IrPathTriple(pt.getSubject(), newPath, newEnd);
						removed.add(join);
					}
				}
				out.add(pt);
				continue;
			}
			// Recurse within nested BGPs
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				IrBGP inner = g.getWhere();
				inner = joinPathWithLaterSp(inner, r);
				inner = fuseAltInverseTailBGP(inner, r);
				out.add(new IrGraph(g.getGraph(), inner));
				continue;
			}
			if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				out.add(new IrOptional(joinPathWithLaterSp(o.getWhere(), r)));
				continue;
			}
			if (n instanceof IrMinus) {
				IrMinus m = (IrMinus) n;
				out.add(new IrMinus(joinPathWithLaterSp(m.getWhere(), r)));
				continue;
			}
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion();
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(joinPathWithLaterSp(b, r));
				}
				out.add(u2);
				continue;
			}
			if (n instanceof IrService) {
				IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), joinPathWithLaterSp(s.getWhere(), r)));
				continue;
			}
			if (n instanceof IrSubSelect) {
				out.add(n); // keep raw subselects
				continue;
			}
			out.add(n);
		}
		IrBGP res = new IrBGP();
		for (IrNode n2 : out) {
			if (!removed.contains(n2)) {
				res.add(n2);
			}
		}
		return res;
	}

	public static boolean sameVar(Var a, Var b) {
		if (a == null || b == null) {
			return false;
		}
		if (a.hasValue() || b.hasValue()) {
			return false;
		}
		return Objects.equals(a.getName(), b.getName());
	}

	public static boolean isAnonPathVar(Var v) {
		return v != null && !v.hasValue() && v.getName() != null && v.getName().startsWith(ANON_PATH_PREFIX);
	}

	/**
	 * If the given path text is a negated property set of the form !(a|b|...), return a version where each member is
	 * inverted by toggling the leading '^' (i.e., a -> ^a, ^a -> a). Returns null when the input is not a simple NPS.
	 */
	public static String invertNegatedPropertySet(String npsText) {
		if (npsText == null) {
			return null;
		}
		String s = npsText.trim();
		if (!s.startsWith("!(") || !s.endsWith(")")) {
			return null;
		}
		String inner = s.substring(2, s.length() - 1);
		if (inner.isEmpty()) {
			return s;
		}
		String[] toks = inner.split("\\|");
		List<String> out = new ArrayList<>(toks.length);
		for (String tok : toks) {
			String t = tok.trim();
			if (t.isEmpty()) {
				continue;
			}
			if (t.startsWith("^")) {
				out.add(t.substring(1));
			} else {
				out.add("^" + t);
			}
		}
		if (out.isEmpty()) {
			return s; // fallback: unchanged
		}
		return "!(" + String.join("|", out) + ")";
	}

	/**
	 * Fuse a path triple whose object is a bridge var with a constant-IRI tail triple that also uses the bridge var,
	 * producing a new path with an added '/^p' or '/p' segment. This version indexes join candidates and works inside
	 * GRAPH bodies as well. It is conservative: only constant predicate tails are fused and containers are preserved.
	 */
	public static IrBGP fuseAltInverseTailBGP(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}

		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		final Set<IrNode> removed = new HashSet<>();

		// Build index of potential tail-join SPs keyed by the bridge var text ("?name"). We store both
		// subject-joins and object-joins, and prefer object-join (inverse tail) to match expectations.
		final Map<String, List<IrStatementPattern>> bySubject = new HashMap<>();
		final Map<String, List<IrStatementPattern>> byObject = new HashMap<>();
		for (IrNode n : in) {
			if (!(n instanceof IrStatementPattern)) {
				continue;
			}
			final IrStatementPattern sp = (IrStatementPattern) n;
			final Var pv = sp.getPredicate();
			if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
				continue;
			}
			// Only index when the non-bridge end is not an anon_path_* var (safety)
			final String sTxt = varOrValue(sp.getSubject(), r);
			final String oTxt = varOrValue(sp.getObject(), r);
			if (sp.getObject() != null && !isAnonPathVar(sp.getSubject()) && oTxt != null && oTxt.startsWith("?")) {
				byObject.computeIfAbsent(oTxt, k -> new ArrayList<>()).add(sp);
			}
			if (sp.getSubject() != null && !isAnonPathVar(sp.getObject()) && sTxt != null && sTxt.startsWith("?")) {
				bySubject.computeIfAbsent(sTxt, k -> new ArrayList<>()).add(sp);
			}
		}

		for (IrNode n : in) {
			if (removed.contains(n)) {
				continue;
			}

			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				final String bridge = varOrValue(pt.getObject(), r);
				if (bridge != null && bridge.startsWith("?")) {
					// Only join when the bridge var is an _anon_path_* variable, to avoid eliminating user vars
					if (!isAnonPathVar(pt.getObject())) {
						out.add(pt);
						continue;
					}
					IrStatementPattern join = null;
					boolean inverse = true; // prefer inverse tail (?y p ?mid) => '^p'
					final List<IrStatementPattern> byObj = byObject.get(bridge);
					if (byObj != null) {
						for (IrStatementPattern sp : byObj) {
							if (!removed.contains(sp)) {
								join = sp;
								inverse = true;
								break;
							}
						}
					}
					if (join == null) {
						final List<IrStatementPattern> bySub = bySubject.get(bridge);
						if (bySub != null) {
							for (IrStatementPattern sp : bySub) {
								if (!removed.contains(sp)) {
									join = sp;
									inverse = false;
									break;
								}
							}
						}
					}
					if (join != null) {
						final String step = r.renderIRI((IRI) join.getPredicate().getValue());
						final String newPath = pt.getPathText() + "/" + (inverse ? "^" : "") + step;
						final Var newEnd = inverse ? join.getSubject() : join.getObject();
						pt = new IrPathTriple(pt.getSubject(), newPath, newEnd);
						removed.add(join);
					}
				}
				out.add(pt);
				continue;
			}

			// Recurse into containers
			if (n instanceof IrGraph) {
				final IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), fuseAltInverseTailBGP(g.getWhere(), r)));
				continue;
			}
			if (n instanceof IrOptional) {
				final IrOptional o = (IrOptional) n;
				out.add(new IrOptional(fuseAltInverseTailBGP(o.getWhere(), r)));
				continue;
			}
			if (n instanceof IrMinus) {
				final IrMinus m = (IrMinus) n;
				out.add(new IrMinus(fuseAltInverseTailBGP(m.getWhere(), r)));
				continue;
			}
			if (n instanceof IrUnion) {
				final IrUnion u = (IrUnion) n;
				final IrUnion u2 = new IrUnion();
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(fuseAltInverseTailBGP(b, r));
				}
				out.add(u2);
				continue;
			}
			if (n instanceof IrService) {
				final IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), fuseAltInverseTailBGP(s.getWhere(), r)));
				continue;
			}
			// Subselects: keep as-is
			out.add(n);
		}

		final IrBGP res = new IrBGP();
		for (IrNode n2 : out) {
			if (!removed.contains(n2)) {
				res.add(n2);
			}
		}
		return res;
	}

	public static String varOrValue(Var v, TupleExprIRRenderer r) {
		if (v == null) {
			return "?_";
		}
		if (v.hasValue()) {
			return r.renderValue(v.getValue());
		}
		return "?" + v.getName();
	}

}
