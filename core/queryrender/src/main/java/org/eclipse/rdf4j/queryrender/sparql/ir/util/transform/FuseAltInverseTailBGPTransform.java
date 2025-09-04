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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Fuse a path triple with adjacent constant-predicate triples that share its subject (head prefix) or object (tail
 * suffix). Produces a single path triple with a {@code p/} or {@code /^p} segment, preferring inverse tails to match
 * expected rendering in tests. Works inside containers and preserves UNION scope.
 */
public final class FuseAltInverseTailBGPTransform extends BaseTransform {
	private FuseAltInverseTailBGPTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
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

				// 1) Try to fuse a HEAD step using a leading SP that shares the path subject
				final String headBridge = varOrValue(pt.getSubject(), r);
				if (headBridge != null && headBridge.startsWith("?") && isAnonPathVar(pt.getSubject())) {
					IrStatementPattern headJoin = null;
					boolean headInverse = true; // prefer ^p when SP is (?mid p ?x)
					final List<IrStatementPattern> headBySub = bySubject.get(headBridge);
					if (headBySub != null) {
						for (IrStatementPattern sp : headBySub) {
							if (removed.contains(sp)) {
								continue;
							}
							// Constant predicate only
							if (sp.getPredicate() == null || !sp.getPredicate().hasValue()
									|| !(sp.getPredicate().getValue() instanceof IRI)) {
								continue;
							}
							headJoin = sp;
							headInverse = true; // (?mid p ?x) => ^p/ ... starting from ?x
							break;
						}
					}
					if (headJoin == null) {
						final List<IrStatementPattern> headByObj = byObject.get(headBridge);
						if (headByObj != null) {
							for (IrStatementPattern sp : headByObj) {
								if (removed.contains(sp)) {
									continue;
								}
								if (sp.getPredicate() == null || !sp.getPredicate().hasValue()
										|| !(sp.getPredicate().getValue() instanceof IRI)) {
									continue;
								}
								headJoin = sp;
								headInverse = false; // (?x p ?mid) => p/ ... starting from ?x
								break;
							}
						}
					}
					if (headJoin != null) {
						final String step = r.convertIRIToString((IRI) headJoin.getPredicate().getValue());
						final String prefix = (headInverse ? "^" : "") + step + "/";
						final Var newStart = headInverse ? headJoin.getObject() : headJoin.getSubject();
						final IrNode newStartOverride = headInverse
								? headJoin.getObjectOverride()
								: headJoin.getSubjectOverride();
						IrPathTriple np = new IrPathTriple(newStart, newStartOverride, prefix + pt.getPathText(),
								pt.getObject(), pt.getObjectOverride(), pt.getPathVars(), pt.isNewScope());
						pt = np;
						removed.add(headJoin);
					}
				}

				// 2) Try to fuse a TAIL step using a trailing SP that shares the path object
				final String tailBridge = varOrValue(pt.getObject(), r);
				if (tailBridge != null && tailBridge.startsWith("?")) {
					// Only join when the bridge var is an _anon_path_* variable, to avoid eliminating user vars
					if (isAnonPathVar(pt.getObject())) {
						IrStatementPattern join = null;
						boolean inverse = true; // prefer inverse tail (?y p ?mid) => '^p'
						final List<IrStatementPattern> byObj = byObject.get(tailBridge);
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
							final List<IrStatementPattern> bySub = bySubject.get(tailBridge);
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
							final String step = r.convertIRIToString((IRI) join.getPredicate().getValue());
							final String newPath = pt.getPathText() + "/" + (inverse ? "^" : "") + step;
							final Var newEnd = inverse ? join.getSubject() : join.getObject();
							final IrNode newEndOverride = inverse
									? join.getSubjectOverride()
									: join.getObjectOverride();
							IrPathTriple np2 = new IrPathTriple(pt.getSubject(), pt.getSubjectOverride(), newPath,
									newEnd,
									newEndOverride, pt.getPathVars(), pt.isNewScope());
							pt = np2;
							removed.add(join);
						}
					}
				}

				out.add(pt);
				continue;
			}

			// Recurse into containers
			if (n instanceof IrGraph) {
				final IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), fuseAltInverseTailBGP(g.getWhere(), r), g.isNewScope()));
				continue;
			}
			if (n instanceof IrOptional) {
				final IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(fuseAltInverseTailBGP(o.getWhere(), r), o.isNewScope());
				no.setNewScope(o.isNewScope());
				out.add(no);
				continue;
			}
			if (n instanceof IrMinus) {
				final IrMinus m = (IrMinus) n;
				out.add(new IrMinus(fuseAltInverseTailBGP(m.getWhere(), r), m.isNewScope()));
				continue;
			}
			if (n instanceof IrUnion) {
				final IrUnion u = (IrUnion) n;
				final IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(fuseAltInverseTailBGP(b, r));
				}
				out.add(u2);
				continue;
			}
			if (n instanceof IrService) {
				final IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), fuseAltInverseTailBGP(s.getWhere(), r),
						s.isNewScope()));
				continue;
			}
			// Subselects: keep as-is
			out.add(n);
		}

		final IrBGP res = new IrBGP(bgp.isNewScope());
		for (IrNode n2 : out) {
			if (!removed.contains(n2)) {
				res.add(n2);
			}
		}
		res.setNewScope(bgp.isNewScope());
		return res;
	}
}
