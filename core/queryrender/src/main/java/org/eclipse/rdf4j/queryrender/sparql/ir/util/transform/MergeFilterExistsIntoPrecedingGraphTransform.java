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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrValues;

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
		if (bgp == null) {
			return null;
		}
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();

		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			// Pattern: IrGraph(g1) immediately followed by IrFilter(EXISTS { ... }) where the EXISTS
			// body wraps its content in GRAPH blocks with the same graph ref. Move the FILTER inside
			// the GRAPH and unwrap the inner GRAPH(s), grouping with braces.
			if (i + 1 < in.size() && n instanceof IrGraph && in.get(i + 1) instanceof IrFilter) {
				final IrGraph g1 = (IrGraph) n;
				final IrFilter f = (IrFilter) in.get(i + 1);
				// Move a following FILTER EXISTS inside the preceding GRAPH when safe, even if the
				// original FILTER did not explicitly introduce a new scope. We will add an explicit
				// grouped scope inside the GRAPH to preserve the intended grouping.
				if (f.getBody() instanceof IrExists) {
					final IrExists ex = (IrExists) f.getBody();
					// Only perform this merge when the EXISTS node indicates the original query
					// had explicit grouping/scope around its body. This preserves the algebra/text
					// of queries where the FILTER EXISTS intentionally sits outside the GRAPH.
					if (!(ex.isNewScope() || f.isNewScope())) {
						// Keep as-is
						out.add(n);
						continue;
					}
					final IrBGP exWhere = ex.getWhere();
					if (exWhere != null) {
						IrBGP unwrapped = new IrBGP(false);
						boolean canUnwrap = unwrapInto(exWhere, g1.getGraph(), unwrapped);
						if (canUnwrap && !unwrapped.getLines().isEmpty()) {
							// Build new GRAPH body: a single BGP containing the triple and FILTER
							IrBGP inner = new IrBGP(false);
							if (g1.getWhere() != null) {
								for (IrNode ln : g1.getWhere().getLines()) {
									inner.add(ln);
								}
							}
							IrExists newExists = new IrExists(unwrapped, ex.isNewScope());
							IrFilter newFilter = new IrFilter(newExists, false);
							inner.add(newFilter);
							out.add(new IrGraph(g1.getGraph(), inner, g1.isNewScope()));
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

	// Recursively unwrap nodes inside an EXISTS body into 'out', provided all GRAPH refs match 'graphRef'.
	// Returns false if a node cannot be safely unwrapped.
	private static boolean unwrapInto(IrNode node, Var graphRef, IrBGP out) {
		if (node == null) {
			return false;
		}
		if (node instanceof IrBGP) {
			IrBGP w = (IrBGP) node;
			for (IrNode ln : w.getLines()) {
				if (!unwrapInto(ln, graphRef, out)) {
					return false;
				}
			}
			return true;
		}
		if (node instanceof IrGraph) {
			IrGraph ig = (IrGraph) node;
			if (!sameVarOrValue(graphRef, ig.getGraph())) {
				return false;
			}
			if (ig.getWhere() != null) {
				for (IrNode ln : ig.getWhere().getLines()) {
					out.add(ln);
				}
			}
			return true;
		}
		if (node instanceof IrOptional) {
			IrOptional o = (IrOptional) node;
			IrBGP ow = o.getWhere();
			if (ow != null && ow.getLines().size() == 1 && ow.getLines().get(0) instanceof IrGraph) {
				IrGraph ig = (IrGraph) ow.getLines().get(0);
				if (!sameVarOrValue(graphRef, ig.getGraph())) {
					return false;
				}
				IrOptional no = new IrOptional(ig.getWhere(), o.isNewScope());
				no.setNewScope(o.isNewScope());
				out.add(no);
				return true;
			}
			// Allow nested optional with a grouped BGP that contains only a single IrGraph line
			if (ow != null && ow.getLines().size() == 1 && ow.getLines().get(0) instanceof IrBGP) {
				IrBGP inner = (IrBGP) ow.getLines().get(0);
				if (inner.getLines().size() == 1 && inner.getLines().get(0) instanceof IrGraph) {
					IrGraph ig = (IrGraph) inner.getLines().get(0);
					if (!sameVarOrValue(graphRef, ig.getGraph())) {
						return false;
					}
					IrOptional no = new IrOptional(ig.getWhere(), o.isNewScope());
					no.setNewScope(o.isNewScope());
					out.add(no);
					return true;
				}
			}
			return false;
		}
		// Pass through VALUES blocks unchanged: they are not tied to a specific GRAPH and
		// can be safely retained when the FILTER EXISTS is merged into the enclosing GRAPH.
		if (node instanceof IrValues) {
			out.add(node);
			return true;
		}
		return false;
	}
}
