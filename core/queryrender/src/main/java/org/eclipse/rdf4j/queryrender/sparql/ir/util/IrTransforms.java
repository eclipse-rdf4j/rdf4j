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
 * IR transformation pipeline (best-effort). Keep it simple and side-effect free when possible.
 */
public final class IrTransforms {
	private IrTransforms() {
	}

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

					// Collections and options later; first ensure path alternations are extended when possible
					// Merge OPTIONAL into preceding GRAPH only when it is clearly a single-step adjunct and safe.
					w = MergeOptionalIntoPrecedingGraphTransform.apply(w);
					w = FuseAltInverseTailBGPTransform.apply(w, r);
					w = FlattenSingletonUnionsTransform.apply(w);
					// Reorder OPTIONAL-level filters before nested OPTIONALs when safe (variable-availability
					// heuristic)
					w = ReorderFiltersInOptionalBodiesTransform.apply(w, r);
					w = ApplyPropertyListsTransform.apply(w, r);

					// Ensure bare NPS triples use a stable subject/object orientation for idempotence
					w = CanonicalizeBareNpsOrientationTransform.apply(w);
					w = NormalizeZeroOrOneSubselectTransform.apply(w, r);

					w = ApplyPathsFixedPointTransform.apply(w, r);

					// (skip) string-level path parentheses simplification; keep structurally safe output
					// (skip) final NPS member order normalization to preserve original order

					// (disabled) Late normalization of grouped tail steps

					return w;
				}
				return child;
			});
		}

		return (IrSelect) irNode;
	}

}
