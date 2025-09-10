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
 * Within GRAPH bodies, normalize local triple/path shapes by fusing adjacent PT/SP/PT patterns and performing
 * conservative tail joins. This helps later UNION/path fusers see a stable inner structure.
 */
public final class ApplyNormalizeGraphInnerPathsTransform extends BaseTransform {
	private ApplyNormalizeGraphInnerPathsTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				IrBGP inner = g.getWhere();
				// Support both PT-then-SP and SP-then-PT fusions inside GRAPH bodies
				inner = fuseAdjacentPtThenSp(inner, r);
				inner = fuseAdjacentSpThenPt(inner, r);
				// Also collapse adjacent IrPathTriple → IrPathTriple chains
				inner = fuseAdjacentPtThenPt(inner);
				inner = joinPathWithLaterSp(inner, r);
				inner = fuseAltInverseTailBGP(inner, r);
				out.add(new IrGraph(g.getGraph(), inner, g.isNewScope()));
			} else if (n instanceof IrBGP || n instanceof IrOptional || n instanceof IrMinus || n instanceof IrUnion
					|| n instanceof IrService) {
				IrNode rec = BaseTransform.rewriteContainers(n, child -> apply(child, r));
				out.add(rec);
			} else {
				out.add(n);
			}
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;

	}

	public static IrBGP fuseAdjacentPtThenSp(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = bgp.getLines();
		List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (i + 1 < in.size() && n instanceof IrPathTriple && in.get(i + 1) instanceof IrStatementPattern) {
				IrPathTriple pt = (IrPathTriple) n;
				IrStatementPattern sp = (IrStatementPattern) in.get(i + 1);
				Var pv = sp.getPredicate();
				if (isConstantIriPredicate(sp)) {
					Var bridge = pt.getObject();
					if (isAnonPathVar(bridge)) {
						if (sameVar(bridge, sp.getSubject())) {
							String fused = pt.getPathText() + "/" + iri(pv, r);
							IrPathTriple np = new IrPathTriple(pt.getSubject(), fused, sp.getObject(), false,
									pt.getPathVars());
							out.add(np);
							i += 1;
							continue;
						} else if (sameVar(bridge, sp.getObject())) {
							String fused = pt.getPathText() + "/^" + iri(pv, r);
							IrPathTriple np2 = new IrPathTriple(pt.getSubject(), fused, sp.getSubject(), false,
									pt.getPathVars());
							out.add(np2);
							i += 1;
							continue;
						}
					}
				}
			}
			// Recurse into containers
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					IrBGP nb = fuseAdjacentPtThenSp(b, r);
					nb = fuseAdjacentSpThenPt(nb, r);
					nb = fuseAdjacentPtThenPt(nb);
					nb = joinPathWithLaterSp(nb, r);
					nb = fuseAltInverseTailBGP(nb, r);
					u2.addBranch(nb);
				}
				out.add(u2);
				continue;
			}
			IrNode rec = BaseTransform.rewriteContainers(n, child -> fuseAdjacentPtThenSp(child, r));
			out.add(rec);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

}
