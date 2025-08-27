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

import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;

/**
 * Apply path-related transforms repeatedly until the WHERE block reaches a textual fixed point. The fingerprint is
 * computed by rendering the WHERE as a subselect so non-WHERE text does not affect convergence.
 *
 * Guarded to a small iteration budget to avoid accidental oscillations.
 */
public final class ApplyPathsFixedPointTransform extends BaseTransform {
	private ApplyPathsFixedPointTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		String prev = null;
		IrBGP cur = bgp;
		int guard = 0;
		while (true) {
			// Render WHERE to a stable string fingerprint
			final String fp = fingerprintWhere(cur, r);
			if (fp.equals(prev)) {
				break; // reached fixed point
			}
			if (++guard > 12) { // safety to avoid infinite cycling
				break;
			}
			prev = fp;
			// Single iteration: apply path fusions and normalizations that can unlock each other
			IrBGP next = ApplyPathsTransform.apply(cur, r);
			// Fuse a pure UNION of simple triples (possibly GRAPH-wrapped) to a single alternation path
			next = FuseUnionOfSimpleTriplesTransform.apply(next, r);
			// Fuse a path followed by UNION of opposite-direction tail triples into an alternation tail
			next = FusePathPlusTailAlternationUnionTransform.apply(next, r);
			// Fuse a pre-path triple followed by a UNION of two tail branches into a single alternation tail
			next = FusePrePathThenUnionAlternationTransform.apply(next, r);
			// Merge adjacent GRAPH blocks with the same graph ref so that downstream fusers see a single body
			next = CoalesceAdjacentGraphsTransform.apply(next);
			// Within UNIONs, partially fuse compatible path-triple branches into a single alternation branch
			next = FuseUnionOfPathTriplesPartialTransform.apply(next, r);
			// Now that adjacent GRAPHs are coalesced, normalize inner GRAPH bodies for SP/PT fusions
			next = ApplyNormalizeGraphInnerPathsTransform.apply(next, r);
			// (disabled) Canonicalize grouping around split middle steps
			cur = next;
		}
		return cur;
	}

	/** Build a stable text fingerprint of a WHERE block for fixed-point detection. */
	public static String fingerprintWhere(IrBGP where, TupleExprIRRenderer r) {
		final IrSelect tmp = new IrSelect();
		tmp.setWhere(where);
		// Render as a subselect to avoid prologue/dataset noise; header is constant (SELECT *)
		return r.render(tmp, null, true);
	}
}
