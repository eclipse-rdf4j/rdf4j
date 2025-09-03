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
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Fuse a path triple followed by a UNION of two single-step tail triples into a single path with an alternation tail.
 *
 * Shape: - Input: PT: ?s P ?mid . UNION of two branches that each connect ?mid to the same end variable via constant
 * predicates in opposite directions (forward/inverse), optionally GRAPH-wrapped with the same graph ref. - Output: ?s
 * P/(p|^p) ?end .
 *
 * Notes: - Does not fuse across UNIONs marked as new scope (explicit user UNIONs). - Requires the bridge variable
 * (?mid) to be an {@code _anon_path_*} var so we never eliminate user-visible vars.
 */
public class FusePathPlusTailAlternationUnionTransform extends BaseTransform {

	private FusePathPlusTailAlternationUnionTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		/** Fuse pattern: IrPathTriple pt; IrUnion u of two opposite-direction constant tail triples to same end var. */
		if (bgp == null) {
			return null;
		}
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			// Recurse first
			n = n.transformChildren(child -> {
				if (child instanceof IrBGP) {
					return apply((IrBGP) child, r);
				}
				return child;
			});
			if (i + 1 < in.size() && n instanceof IrPathTriple && in.get(i + 1) instanceof IrUnion) {
				IrPathTriple pt = (IrPathTriple) n;
				IrUnion u = (IrUnion) in.get(i + 1);
				// Do not merge across a UNION that represents an original query UNION (new scope)
				if (u.isNewScope()) {
					out.add(n);
					continue;
				}
				// Only safe to use the path's object as a bridge when it is an _anon_path_* variable.
				if (!isAnonPathVar(pt.getObject())) {
					out.add(n);
					continue;
				}
				// Analyze two-branch union where each branch is a single SP (or GRAPH with single SP)
				if (u.getBranches().size() == 2) {
					final BranchTriple b1 = getSingleBranchSp(u.getBranches().get(0));
					final BranchTriple b2 = getSingleBranchSp(u.getBranches().get(1));
					if (b1 != null && b2 != null && compatibleGraphs(b1.graph, b2.graph)) {
						final Var midVar = pt.getObject();
						final TripleJoin j1 = classifyTailJoin(b1, midVar, r);
						final TripleJoin j2 = classifyTailJoin(b2, midVar, r);
						if (j1 != null && j2 != null && j1.iri.equals(j2.iri) && sameVar(j1.end, j2.end)
								&& j1.inverse != j2.inverse) {
							final String step = j1.iri; // renderer already compacted IRI
							// Preserve original UNION branch order and their orientation
							final String left = (j1.inverse ? "^" : "") + step;
							final String right = (j2.inverse ? "^" : "") + step;
							final String fusedPath = pt.getPathText() + "/(" + left + "|" + right + ")";
							IrPathTriple np = new IrPathTriple(pt.getSubject(), fusedPath, j1.end, false,
									pt.getPathVars());
							out.add(np);
							i += 1; // consume union
							continue;
						}
					}
				}
			}
			out.add(n);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		return res;

	}

	public static boolean compatibleGraphs(Var a, Var b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return sameVar(a, b);
	}

	public static TripleJoin classifyTailJoin(BranchTriple bt, Var midVar, TupleExprIRRenderer r) {
		if (bt == null || bt.sp == null) {
			return null;
		}
		Var pv = bt.sp.getPredicate();
		if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
			return null;
		}
		Var sVar = bt.sp.getSubject();
		Var oVar = bt.sp.getObject();
		if (sameVar(midVar, sVar)) {
			// forward: mid p ?end
			return new TripleJoin(r.convertIRIToString((IRI) pv.getValue()), oVar, false);
		}
		if (sameVar(midVar, oVar)) {
			// inverse: ?end p mid
			return new TripleJoin(r.convertIRIToString((IRI) pv.getValue()), sVar, true);
		}
		return null;
	}

	public static BranchTriple getSingleBranchSp(IrBGP branch) {
		if (branch == null) {
			return null;
		}
		if (branch.getLines().size() != 1) {
			return null;
		}
		IrNode only = branch.getLines().get(0);
		if (only instanceof IrStatementPattern) {
			return new BranchTriple(null, (IrStatementPattern) only);
		}
		if (only instanceof IrGraph) {
			IrGraph g = (IrGraph) only;
			IrBGP inner = g.getWhere();
			if (inner != null && inner.getLines().size() == 1
					&& inner.getLines().get(0) instanceof IrStatementPattern) {
				return new BranchTriple(g.getGraph(), (IrStatementPattern) inner.getLines().get(0));
			}
		}
		return null;
	}

	public static final class TripleJoin {
		public final String iri; // compacted IRI text (using renderer)
		public final Var end; // end variable
		public final boolean inverse; // true when matching "?end p ?mid"

		TripleJoin(String iri, Var end, boolean inverse) {
			this.iri = iri;
			this.end = end;
			this.inverse = inverse;
		}
	}

	public static final class BranchTriple {
		public final Var graph; // may be null
		public final IrStatementPattern sp;

		BranchTriple(Var graph, IrStatementPattern sp) {
			this.graph = graph;
			this.sp = sp;
		}
	}

}
