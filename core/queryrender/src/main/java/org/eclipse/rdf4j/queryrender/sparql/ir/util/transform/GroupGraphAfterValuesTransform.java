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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrValues;

/**
 * Heuristic grouping: when a VALUES block is immediately followed by a single GRAPH block inside a grouped WHERE
 * (Join), wrap the GRAPH in its own braces to preserve the parser's original scope marker on the GRAPH triple when
 * re-parsed. This improves textual stability for streaming tests that expect the second branch to be an explicit
 * grouped block.
 */
public final class GroupGraphAfterValuesTransform extends BaseTransform {

	private GroupGraphAfterValuesTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null)
			return null;

		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		int i = 0;
		while (i < in.size()) {
			IrNode n = in.get(i);

			// Pattern: VALUES, GRAPH -> insert a grouped block around GRAPH to mirror original braces
			if (n instanceof IrValues && i + 1 < in.size() && in.get(i + 1) instanceof IrGraph) {
				out.add(n);
				IrBGP wrapped = new IrBGP(true);
				wrapped.add(in.get(i + 1));
				out.add(wrapped);
				i += 2;
				continue;
			}

			// Recurse into containers conservatively
			if (n instanceof IrBGP) {
				out.add(apply((IrBGP) n));
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), apply(g.getWhere()), g.isNewScope()));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere()), o.isNewScope());
				out.add(no);
			} else if (n instanceof IrMinus) {
				IrMinus m = (IrMinus) n;
				out.add(new IrMinus(apply(m.getWhere()), m.isNewScope()));
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere()), s.isNewScope()));
			} else if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b));
				}
				out.add(u2);
			} else {
				out.add(n);
			}
			i++;
		}

		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		return res;
	}
}
