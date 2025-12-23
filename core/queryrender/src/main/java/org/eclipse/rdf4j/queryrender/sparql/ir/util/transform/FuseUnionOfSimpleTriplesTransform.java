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
import java.util.List;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Fuse a UNION whose branches are each a single simple triple (optionally inside the same GRAPH) into a single path
 * alternation: ?s (p1|^p2|...) ?o . If branches are GRAPH-wrapped with identical graph var/IRI, the alternation is
 * produced inside that GRAPH block.
 *
 * Scope/safety: - This transform only merges UNIONs that are NOT marked as introducing a new scope. We do not apply the
 * new-scope special case here because these are not NPS branches, and there is no guarantee that the scope originates
 * from parser-generated path bridges; being conservative avoids collapsing user-visible variables. - Each branch must
 * be a single IrStatementPattern (or GRAPH with a single IrStatementPattern), endpoints must align (forward or
 * inverse), and graph refs must match.
 */
public final class FuseUnionOfSimpleTriplesTransform extends BaseTransform {

	private FuseUnionOfSimpleTriplesTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				// Preserve explicit UNION (new variable scope) as-is; do not fuse into a single path alternation.
				if (u.isNewScope()) {
					IrUnion u2 = new IrUnion(u.isNewScope());
					for (IrBGP b : u.getBranches()) {
						u2.addBranch(apply(b, r));
					}
					m = u2;
				} else {
					Fused f = tryFuseUnion(u, r);
					if (f != null) {
						// Deduplicate and parenthesize alternation when multiple members
						ArrayList<String> alts = new ArrayList<>(f.steps);
						String alt = String.join("|", alts);
						if (alts.size() > 1) {
							alt = "(" + alt + ")";
						}
						if (f.graph != null) {
							IrBGP inner = new IrBGP(false);
							IrPathTriple np = new IrPathTriple(f.s, alt, f.o, u.isNewScope(), Collections.emptySet());
							// simple triples have no anon bridge vars; leave empty
							inner.add(np);
							m = new IrGraph(f.graph, inner, false);
						} else {
							IrPathTriple npTop = new IrPathTriple(f.s, alt, f.o, u.isNewScope(),
									Collections.emptySet());
							m = npTop;
						}
					} else {
						// Recurse into branches
						IrUnion u2 = new IrUnion(u.isNewScope());
						for (IrBGP b : u.getBranches()) {
							u2.addBranch(apply(b, r));
						}
						m = u2;
					}
				}
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			} else {
				// Generic recursion into containers
				m = BaseTransform.rewriteContainers(n, child -> apply(child, r));
			}
			out.add(m);
		}
		return BaseTransform.bgpWithLines(bgp, out);
	}

	static final class Fused {
		final Var graph; // may be null
		final Var s;
		final Var o;
		final List<String> steps = new ArrayList<>();

		Fused(Var graph, Var s, Var o) {
			this.graph = graph;
			this.s = s;
			this.o = o;
		}
	}

	private static Fused tryFuseUnion(IrUnion u, TupleExprIRRenderer r) {
		if (u == null || u.getBranches().size() < 2) {
			return null;
		}
		Var graphRef = null;
		Var sCommon = null;
		Var oCommon = null;
		final List<String> steps = new ArrayList<>();

		for (IrBGP b : u.getBranches()) {
			// Only accept branches that are a single simple SP, optionally wrapped in a GRAPH with a single SP
			IrStatementPattern sp;
			Var g = null;
			if (b.getLines().size() == 1 && b.getLines().get(0) instanceof IrStatementPattern) {
				sp = (IrStatementPattern) b.getLines().get(0);
			} else if (b.getLines().size() == 1 && b.getLines().get(0) instanceof IrGraph) {
				IrGraph gb = (IrGraph) b.getLines().get(0);
				if (gb.getWhere() != null && gb.getWhere().getLines().size() == 1
						&& gb.getWhere().getLines().get(0) instanceof IrStatementPattern) {
					sp = (IrStatementPattern) gb.getWhere().getLines().get(0);
					g = gb.getGraph();
				} else {
					return null;
				}
			} else {
				return null;
			}

			if (!isConstantIriPredicate(sp)) {
				return null;
			}
			String step = iri(sp.getPredicate(), r);

			Var sVar;
			Var oVar;
			if (sCommon == null && oCommon == null) {
				// Initialize endpoints orientation using first branch
				sVar = sp.getSubject();
				oVar = sp.getObject();
				sCommon = sVar;
				oCommon = oVar;
				graphRef = g;
				steps.add(step);
			} else {
				// Endpoints must match either forward or inverse
				if (sameVar(sCommon, sp.getSubject()) && sameVar(oCommon, sp.getObject())) {
					steps.add(step);
				} else if (sameVar(sCommon, sp.getObject()) && sameVar(oCommon, sp.getSubject())) {
					steps.add("^" + step);
				} else {
					return null;
				}
				// Graph ref must be identical (both null or same var/value)
				if ((graphRef == null && g != null) || (graphRef != null && g == null)
						|| (graphRef != null && !sameVarOrValue(graphRef, g))) {
					return null;
				}
			}
		}

		if (steps.size() >= 2) {
			Fused f = new Fused(graphRef, sCommon, oCommon);
			f.steps.addAll(steps);
			return f;
		}
		return null;
	}
}
