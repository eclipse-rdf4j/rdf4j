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

/**
 * Fuse a UNION whose branches are each a single simple triple (optionally inside the same GRAPH) into a single path
 * alternation: ?s (p1|^p2|...) ?o . If branches are GRAPH-wrapped with identical graph var/IRI, the alternation is
 * produced inside that GRAPH block.
 */
public final class FuseUnionOfSimpleTriplesTransform extends BaseTransform {

	private FuseUnionOfSimpleTriplesTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null)
			return null;
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				Fused f = tryFuseUnion(u, r);
				if (f != null) {
					if (f.graph != null) {
						IrBGP inner = new IrBGP();
						inner.add(new IrPathTriple(f.s, String.join("|", f.steps), f.o));
						m = new IrGraph(f.graph, inner);
					} else {
						m = new IrPathTriple(f.s, String.join("|", f.steps), f.o);
					}
				} else {
					// Recurse into branches
					IrUnion u2 = new IrUnion();
					u2.setNewScope(u.isNewScope());
					for (IrBGP b : u.getBranches()) {
						u2.addBranch(apply(b, r));
					}
					m = u2;
				}
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere(), r));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				m = new IrOptional(apply(o.getWhere(), r));
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere(), r));
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere(), r));
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			}
			out.add(m);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
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
		if (u == null || u.getBranches().size() < 2)
			return null;
		Var graphRef = null;
		Var sCommon = null;
		Var oCommon = null;
		final List<String> steps = new ArrayList<>();

		for (IrBGP b : u.getBranches()) {
			// Only accept branches that are a single simple SP, optionally wrapped in a GRAPH with a single SP
			IrStatementPattern sp = null;
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

			if (sp.getPredicate() == null || !sp.getPredicate().hasValue()
					|| !(sp.getPredicate().getValue() instanceof IRI)) {
				return null;
			}
			String step = r.renderIRI((IRI) sp.getPredicate().getValue());

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
					sVar = sp.getSubject();
					oVar = sp.getObject();
					steps.add(step);
				} else if (sameVar(sCommon, sp.getObject()) && sameVar(oCommon, sp.getSubject())) {
					sVar = sp.getObject();
					oVar = sp.getSubject();
					steps.add("^" + step);
				} else {
					return null;
				}
				// Graph ref must be identical (both null or same var)
				if ((graphRef == null && g != null) || (graphRef != null && g == null)
						|| (graphRef != null && !sameVar(graphRef, g))) {
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
