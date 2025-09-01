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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ApplyCollectionsTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ApplyNegatedPropertySetTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ApplyPathsFixedPointTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ApplyPropertyListsTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.CanonicalizeBareNpsOrientationTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.CanonicalizeGroupedTailStepTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.CanonicalizeNpsByProjectionTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.CanonicalizeUnionBranchOrderTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.CoalesceAdjacentGraphsTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.FlattenSingletonUnionsTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.FuseAltInverseTailBGPTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.FuseServiceNpsUnionLateTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.FuseUnionOfNpsBranchesTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.GroupFilterExistsWithPrecedingTriplesTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.GroupValuesAndNpsInUnionBranchTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.InlineBNodeObjectsTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.MergeFilterExistsIntoPrecedingGraphTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.MergeOptionalIntoPrecedingGraphTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.NormalizeFilterNotInTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.NormalizeNpsMemberOrderTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.NormalizeZeroOrOneSubselectTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ReorderFiltersInOptionalBodiesTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ServiceNpsUnionFuser;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.SimplifyPathParensTransform;

/**
 * IR transformation pipeline (best‑effort).
 *
 * Design: - Transform passes are small, focused, and avoid mutating existing nodes; they return new IR blocks. - Safety
 * heuristics: path fusions only occur across parser‑generated bridge variables (names prefixed with
 * {@code _anon_path_}) so user‑visible variables are never collapsed or inverted unexpectedly. - Ordering matters:
 * early passes normalize obvious shapes (collections, zero‑or‑one, simple paths), mid passes perform fusions that can
 * unlock each other, late passes apply readability and canonicalization tweaks (e.g., parentheses, NPS orientation).
 *
 * The pipeline is intentionally conservative: it prefers stable, readable output and round‑trip idempotence over
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
		// Single application of the ordered passes via transformChildren().
		// The bounded loop is kept to make it trivial to turn this into a multi‑pass fixed‑point
		// driver in the future; current passes aim to be idempotent in one pass.
		for (int i = 0; i < 10; i++) {
			// Use transformChildren to rewrite WHERE/BGPs functionally in a single pass order
			irNode = select.transformChildren(child -> {
				if (child instanceof IrBGP) {
					IrBGP w = (IrBGP) child;
					w = NormalizeZeroOrOneSubselectTransform.apply(w, r);
					w = CoalesceAdjacentGraphsTransform.apply(w);
					// Early merge of FILTER EXISTS into preceding GRAPH when safe, so subsequent transforms
					// see the grouped shape and do not separate them again.
					w = MergeFilterExistsIntoPrecedingGraphTransform.apply(w);
					w = ApplyCollectionsTransform.apply(w, r);
					w = ApplyNegatedPropertySetTransform.apply(w, r);
					w = NormalizeZeroOrOneSubselectTransform.apply(w, r);

					w = ApplyPathsFixedPointTransform.apply(w, r);

					// Late fuse: inside SERVICE, convert UNION of two bare-NPS branches into a single NPS
					w = FuseServiceNpsUnionLateTransform
							.apply(w);

					// Normalize NPS member order for stable, expected text
					w = NormalizeNpsMemberOrderTransform.apply(w);

					// Collections and options later; first ensure path alternations are extended when possible
					// Merge OPTIONAL into preceding GRAPH only when it is clearly a single-step adjunct and safe.
					w = MergeOptionalIntoPrecedingGraphTransform.apply(w);
					w = FuseAltInverseTailBGPTransform.apply(w, r);
					w = FlattenSingletonUnionsTransform.apply(w);
					// If a FILTER EXISTS { GRAPH g { ... } } follows a GRAPH g { ... }, move the filter inside
					// the preceding GRAPH and unwrap the inner GRAPH wrapper. Add grouping braces inside the
					// GRAPH to preserve expected structure.
					w = MergeFilterExistsIntoPrecedingGraphTransform.apply(w);
					// Wrap preceding triple with FILTER EXISTS { { ... } } into a grouped block for stability
					w = GroupFilterExistsWithPrecedingTriplesTransform.apply(w);
					// After grouping, re-run a lightweight NPS rewrite inside nested groups to compact
					// simple var-predicate + inequality filters to !(...) path triples (including inside
					// EXISTS bodies).
					w = ApplyNegatedPropertySetTransform.rewriteSimpleNpsOnly(w, r);
					// Fuse UNION-of-NPS specifically under MINUS early, once branches have been rewritten to path
					// triples
					// Grouping/stability is driven by explicit newScope flags in IR; avoid heuristics here.
					// Reorder OPTIONAL-level filters before nested OPTIONALs when safe (variable-availability
					// heuristic)
					w = ReorderFiltersInOptionalBodiesTransform.apply(w, r);
					// Normalize chained inequalities in FILTERs to NOT IN when safe
					w = NormalizeFilterNotInTransform.apply(w,
							r);
					// Inline simple _anon_bnode_* object nodes as bracket property lists before grouping
					w = InlineBNodeObjectsTransform.apply(w, r);
					// Then group contiguous subject-equal triples into property lists
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

					// Re-run SERVICE NPS union fusion very late in case earlier passes
					// introduced the union shape only at this point
					w = FuseServiceNpsUnionLateTransform
							.apply(w);

					// One more UNION-of-NPS fuser after broader path refactors to catch newly-formed shapes
					w = FuseUnionOfNpsBranchesTransform.apply(w, r);

					// Light string-level path parentheses simplification for readability/idempotence
					w = SimplifyPathParensTransform.apply(w);

					// Late normalization of grouped tail steps: ensure a final tail like "/foaf:name"
					// is rendered outside the right-hand grouping when safe
					w = CanonicalizeGroupedTailStepTransform.apply(w, r);

					// Final orientation tweak for bare NPS using SELECT projection order when available
					w = CanonicalizeNpsByProjectionTransform
							.apply(w, select);

					// Canonicalize UNION branch order to prefer the branch whose subject matches the first
					// projected variable (textual stability for streaming tests)
					w = CanonicalizeUnionBranchOrderTransform
							.apply(w, select);

					// Preserve explicit grouping for UNION branches that combine VALUES with a negated
					// property path triple, to maintain textual stability expected by tests.
					w = GroupValuesAndNpsInUnionBranchTransform.apply(w);

					// Merge a following FILTER EXISTS into a preceding GRAPH with the same graph ref and
					// group them together, unwrapping inner GRAPHs inside the EXISTS body. This produces
					// the expected grouped shape "{ GRAPH g { { triple . FILTER EXISTS { ... } } } }".
					w = MergeFilterExistsIntoPrecedingGraphTransform.apply(w);

					// Final SERVICE NPS union fusion pass after all other cleanups
					w = FuseServiceNpsUnionLateTransform
							.apply(w);

					return w;
				}
				return child;
			});
		}

		// Final sweeping pass: fuse UNION-of-NPS strictly inside SERVICE bodies (handled by
		// FuseServiceNpsUnionLateTransform). Do not apply the service fuser to the whole WHERE,
		// to avoid collapsing top-level UNIONs that tests expect to remain explicit.
		IrSelect outSel = (IrSelect) irNode;
		IrBGP where = outSel.getWhere();
		where = FuseServiceNpsUnionLateTransform.apply(where);
		// Final path text normalization for readability/idempotence
		where = SimplifyPathParensTransform.apply(where);
		outSel.setWhere(where);
		return outSel;
	}

}
