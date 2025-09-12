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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;

/**
 * Merge consecutive GRAPH blocks that reference the same graph term into a single GRAPH with a concatenated body.
 *
 * Purpose: - Downstream path fusers work better when a graph body is contiguous, so this pass prepares the IR by
 * removing trivial GRAPH boundaries that arose during building or earlier rewrites.
 *
 * Notes: - Only merges when the graph reference variables/IRIs are identical (by variable name or value). - Preserves
 * other containers via recursion and leaves UNION branch scopes intact.
 */
public final class CoalesceAdjacentGraphsTransform extends BaseTransform {
	private CoalesceAdjacentGraphsTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (n instanceof IrGraph) {
				final IrGraph g1 = (IrGraph) n;
				final IrBGP merged = new IrBGP(false);
				// start with g1 inner lines
				if (g1.getWhere() != null) {
					g1.getWhere().getLines().forEach(merged::add);
				}
				int j = i + 1;
				while (j < in.size() && (in.get(j) instanceof IrGraph)) {
					final IrGraph gj = (IrGraph) in.get(j);
					if (!sameVarOrValue(g1.getGraph(), gj.getGraph())) {
						break;
					}
					if (gj.getWhere() != null) {
						gj.getWhere().getLines().forEach(merged::add);
					}
					j++;
				}
				out.add(new IrGraph(g1.getGraph(), merged, g1.isNewScope()));
				i = j - 1;
				continue;
			}

			// Recurse into other containers with shared helper
			IrNode rec = BaseTransform.rewriteContainers(n, CoalesceAdjacentGraphsTransform::apply);
			out.add(rec);
		}
        return BaseTransform.bgpWithLines(bgp, out);
	}
}
