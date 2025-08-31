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

import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrExists;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * If a GRAPH block is immediately followed by a FILTER with an EXISTS body that itself wraps its content in a GRAPH of
 * the same graph reference, move the FILTER EXISTS inside the preceding GRAPH and unwrap the inner GRAPH wrapper. Also
 * introduce an explicit grouping scope around the GRAPH body so that the triple(s) and the FILTER are kept together in
 * braces, matching the source query's grouping.
 *
 * Example: GRAPH <g> { ?s ex:p ?o . } FILTER EXISTS { GRAPH <g> { ?s !(ex:a|^ex:b) ?o . } } → GRAPH <g> { { ?s ex:p ?o
 * . FILTER EXISTS { ?s !(ex:a|^ex:b) ?o . } } }
 */
public final class MergeFilterExistsIntoPrecedingGraphTransform extends BaseTransform {

	private MergeFilterExistsIntoPrecedingGraphTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null)
			return null;
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();

		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			// Pattern: IrGraph(g1), IrFilter( EXISTS { IrBGP( IrGraph(g2, inner) ) } )
			// Apply this fold only when the entire block consists of exactly these two lines
			if (in.size() == 2 && i == 0 && n instanceof IrGraph && in.get(i + 1) instanceof IrFilter) {
				final IrGraph g1 = (IrGraph) n;
				final IrFilter f = (IrFilter) in.get(i + 1);
				// Only move FILTER inside GRAPH when the FILTER explicitly introduces a new scope.
				if (f.isNewScope() && f.getBody() instanceof IrExists) {
					final IrExists ex = (IrExists) f.getBody();
					final IrBGP exWhere = ex.getWhere();
					if (exWhere != null && exWhere.getLines().size() == 1
							&& exWhere.getLines().get(0) instanceof IrGraph) {
						final IrGraph innerGraph = (IrGraph) exWhere.getLines().get(0);
						if (sameVarOrValue(g1.getGraph(), innerGraph.getGraph())) {
							// Build new GRAPH body: original inner lines + FILTER EXISTS with unwrapped body
							IrBGP newInner = new IrBGP(true); // enforce grouped braces inside GRAPH
							if (g1.getWhere() != null) {
								for (IrNode ln : g1.getWhere().getLines()) {
									newInner.add(ln);
								}
							}
							IrExists newExists = new IrExists(innerGraph.getWhere(), ex.isNewScope());
							IrFilter newFilter = new IrFilter(newExists, f.isNewScope());
							newInner.add(newFilter);
							out.add(new IrGraph(g1.getGraph(), newInner, g1.isNewScope()));
							i += 1; // consume the FILTER node
							continue;
						}
					}
				}
			}

			// Recurse into containers
			if (n instanceof IrGraph) {
				final IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), apply(g.getWhere()), g.isNewScope()));
				continue;
			}
			if (n instanceof IrOptional) {
				final IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere()), o.isNewScope());
				no.setNewScope(o.isNewScope());
				out.add(no);
				continue;
			}
			if (n instanceof IrMinus) {
				final IrMinus m = (IrMinus) n;
				out.add(new IrMinus(apply(m.getWhere()), m.isNewScope()));
				continue;
			}
			if (n instanceof IrUnion) {
				final IrUnion u = (IrUnion) n;
				final IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b));
				}
				out.add(u2);
				continue;
			}
			if (n instanceof IrService) {
				final IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere()), s.isNewScope()));
				continue;
			}
			if (n instanceof IrSubSelect) {
				out.add(n);
				continue;
			}
			if (n instanceof IrFilter) {
				IrFilter f = (IrFilter) n;
				if (f.getBody() instanceof IrExists) {
					IrExists ex = (IrExists) f.getBody();
					IrBGP inner = apply(ex.getWhere());
					out.add(new IrFilter(new IrExists(inner, ex.isNewScope()), f.isNewScope()));
					continue;
				}
			}

			out.add(n);
		}

		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}
}
