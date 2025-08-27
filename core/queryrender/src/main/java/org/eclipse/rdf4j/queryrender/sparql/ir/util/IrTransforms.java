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
package org.eclipse.rdf4j.queryrender.sparql.ir.util;

import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ApplyCollectionsTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ApplyNegatedPropertySetTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ApplyPathsFixedPointTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ApplyPropertyListsTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.CanonicalizeBareNpsOrientationTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.CanonicalizeGroupedTailStepTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.CoalesceAdjacentGraphsTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.FlattenSingletonUnionsTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.FuseAltInverseTailBGPTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.MergeOptionalIntoPrecedingGraphTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.NormalizeNpsMemberOrderTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.NormalizeZeroOrOneSubselectTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ReorderFiltersInOptionalBodiesTransform;

/**
 * IR transformation pipeline (best-effort).
 *
 * Design: - Transform passes are small, focused, and avoid mutating existing nodes; they return new IR blocks. - Safety
 * heuristics: path fusions only occur across parser-generated bridge variables (names prefixed with
 * {@code _anon_path_}) so user-visible variables are never collapsed or inverted unexpectedly. - Ordering matters:
 * early passes normalize obvious shapes (collections, zero-or-one, simple paths), mid passes perform fusions that can
 * unlock each other, late passes apply readability and canonicalization tweaks (e.g., parentheses, NPS orientation).
 *
 * The pipeline is intentionally conservative: it prefers stable, readable output and round-trip idempotence over
 * aggressive rewriting.
 */
public final class IrTransforms {
	private IrTransforms() {
	}

	/**
	 * Apply the ordered transform pipeline to the WHERE block of a SELECT IR. This function uses
	 * IrNode#transformChildren to descend only into BGP-like containers, keeping subselects intact.
	 */
	public static IrSelect transformUsingChildren(IrSelect select, TupleExprIRRenderer r) {
		if (select == null) {
			return null;
		}

		IrNode irNode = null;
		for (int i = 0; i < 100; i++) {
			// Use transformChildren to rewrite WHERE/BGPs functionally in a single pass order
			irNode = select.transformChildren(child -> {
				if (child instanceof IrBGP) {
					IrBGP w = (IrBGP) child;
					w = NormalizeZeroOrOneSubselectTransform.apply(w, r);
					w = CoalesceAdjacentGraphsTransform.apply(w);
					w = ApplyCollectionsTransform.apply(w, r);
					w = ApplyNegatedPropertySetTransform.apply(w, r);
					w = NormalizeZeroOrOneSubselectTransform.apply(w, r);

					w = ApplyPathsFixedPointTransform.apply(w, r);

					// Normalize NPS member order for stable, expected text
					w = NormalizeNpsMemberOrderTransform.apply(w);

					// Collections and options later; first ensure path alternations are extended when possible
					// Merge OPTIONAL into preceding GRAPH only when it is clearly a single-step adjunct and safe.
					w = MergeOptionalIntoPrecedingGraphTransform.apply(w);
					w = FuseAltInverseTailBGPTransform.apply(w, r);
					w = FlattenSingletonUnionsTransform.apply(w);
					// Reorder OPTIONAL-level filters before nested OPTIONALs when safe (variable-availability
					// heuristic)
					w = ReorderFiltersInOptionalBodiesTransform.apply(w, r);
					// Normalize chained inequalities in FILTERs to NOT IN when safe
					w = org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.NormalizeFilterNotInTransform.apply(w,
							r);
					w = ApplyPropertyListsTransform.apply(w, r);

					// Preserve original orientation of bare NPS triples to match expected algebra
					w = NormalizeZeroOrOneSubselectTransform.apply(w, r);

					w = ApplyPathsFixedPointTransform.apply(w, r);

					// Normalize NPS member order after late inversions introduced by path fusions
					w = NormalizeNpsMemberOrderTransform.apply(w);

					// Canonicalize bare NPS orientation so that subject/object ordering is stable
					// for pairs of user variables (e.g., prefer ?x !(...) ?y over ?y !(^...) ?x).
					w = CanonicalizeBareNpsOrientationTransform.apply(w);

					// Late pass: re-apply NPS fusion now that earlier transforms may have
					// reordered FILTERs/triples to be adjacent (e.g., GRAPH …, FILTER …, GRAPH …).
					// This catches cases like Graph + NOT IN + Graph that only become adjacent
					// after other rewrites.
					w = ApplyNegatedPropertySetTransform.apply(w, r);

					// One more path fixed-point to allow newly formed path triples to fuse further
					w = ApplyPathsFixedPointTransform.apply(w, r);
					// And normalize member order again for stability
					w = NormalizeNpsMemberOrderTransform.apply(w);

					// Light string-level path parentheses simplification for readability/idempotence
					w = org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.SimplifyPathParensTransform.apply(w);

					// Late normalization of grouped tail steps: ensure a final tail like "/foaf:name"
					// is rendered outside the right-hand grouping when safe
					w = CanonicalizeGroupedTailStepTransform.apply(w, r);

					// Final orientation tweak for bare NPS using SELECT projection order when available
					w = org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.CanonicalizeNpsByProjectionTransform
							.apply(w, select);

					// Canonicalize UNION branch order to prefer the branch whose subject matches the first
					// projected variable (textual stability for streaming tests)
					w = org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.CanonicalizeUnionBranchOrderTransform
							.apply(w, select);

					return w;
				}
				return child;
			});
		}

		return (IrSelect) irNode;
	}

}
