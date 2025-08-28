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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
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
 * Merge a simple OPTIONAL body that explicitly targets the same GRAPH as the preceding GRAPH block into that block,
 * i.e.,
 *
 * GRAPH ?g { ... } OPTIONAL { GRAPH ?g { simple } }
 *
 * → GRAPH ?g { ... OPTIONAL { simple } }
 *
 * Only applies to "simple" OPTIONAL bodies to avoid changing intended scoping or reordering more complex shapes.
 */
public final class MergeOptionalIntoPrecedingGraphTransform extends BaseTransform {
	private MergeOptionalIntoPrecedingGraphTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (n instanceof IrGraph && i + 1 < in.size() && in.get(i + 1) instanceof IrOptional) {
				IrGraph g = (IrGraph) n;
				// Only merge when the preceding GRAPH has a single simple line. This preserves cases where the
				// original query intentionally kept OPTIONAL outside the GRAPH that already groups multiple lines.
				final IrBGP gInner = g.getWhere();
				if (gInner == null || gInner.getLines().size() != 1) {
					// do not merge; keep original placement
					out.add(n);
					continue;
				}
				IrOptional opt = (IrOptional) in.get(i + 1);
				IrBGP ow = opt.getWhere();
				IrBGP simpleOw = null;
				// Only merge when OPTIONAL body explicitly targets the same GRAPH context. Do not merge a plain
				// OPTIONAL body without an explicit GRAPH wrapper; keep it outside to match original structure.
				if (ow != null && ow.getLines().size() == 1 && ow.getLines().get(0) instanceof IrGraph) {
					// Handle OPTIONAL { GRAPH ?g { simple } } → OPTIONAL { simple } when graph matches
					IrGraph inner = (IrGraph) ow.getLines().get(0);
					if (sameVarOrValue(g.getGraph(), inner.getGraph()) && isSimpleOptionalBody(inner.getWhere())) {
						simpleOw = inner.getWhere();
					}
				} else if (ow != null && ow.getLines().size() >= 1) {
					// Handle OPTIONAL bodies that contain exactly one GRAPH ?g { simple } plus one or more FILTER
					// lines.
					// Merge into the preceding GRAPH and keep the FILTER(s) inside the OPTIONAL block.
					IrGraph innerGraph = null;
					final List<IrFilter> filters = new ArrayList<>();
					boolean ok = true;
					for (IrNode ln : ow.getLines()) {
						if (ln instanceof IrGraph) {
							if (innerGraph != null) {
								ok = false; // more than one graph inside OPTIONAL -> bail
								break;
							}
							innerGraph = (IrGraph) ln;
							if (!sameVarOrValue(g.getGraph(), innerGraph.getGraph())) {
								ok = false;
								break;
							}
							continue;
						}
						if (ln instanceof IrFilter) {
							filters.add((IrFilter) ln);
							continue;
						}
						ok = false; // unexpected node type inside OPTIONAL body
						break;
					}
					if (ok && innerGraph != null && isSimpleOptionalBody(innerGraph.getWhere())) {
						IrBGP body = new IrBGP();
						// simple triples/paths first, then original FILTER lines
						for (IrNode gln : innerGraph.getWhere().getLines()) {
							body.add(gln);
						}
						for (IrFilter fl : filters) {
							body.add(fl);
						}
						simpleOw = body;
					}
				}
				if (simpleOw != null) {
					// Build merged graph body
					IrBGP merged = new IrBGP();
					for (IrNode gl : g.getWhere().getLines()) {
						merged.add(gl);
					}
					merged.add(new IrOptional(simpleOw));
					// Debug marker (harmless): indicate we applied the merge
					// System.out.println("# IrTransforms: merged OPTIONAL into preceding GRAPH");
					out.add(new IrGraph(g.getGraph(), merged));
					i += 1;
					continue;
				}
			}
			// Recurse into containers
			if (n instanceof IrBGP || n instanceof IrGraph || n instanceof IrOptional || n instanceof IrUnion
					|| n instanceof IrMinus || n instanceof IrService || n instanceof IrSubSelect) {
				n = n.transformChildren(child -> {
					if (child instanceof IrBGP) {
						return MergeOptionalIntoPrecedingGraphTransform.apply((IrBGP) child);
					}
					return child;
				});
			}
			out.add(n);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	public static boolean isSimpleOptionalBody(IrBGP ow) {
		if (ow == null) {
			return false;
		}
		if (ow.getLines().isEmpty()) {
			return false;
		}
		for (IrNode ln : ow.getLines()) {
			if (!(ln instanceof IrStatementPattern || ln instanceof IrPathTriple)) {
				return false;
			}
		}
		return true;
	}

}
