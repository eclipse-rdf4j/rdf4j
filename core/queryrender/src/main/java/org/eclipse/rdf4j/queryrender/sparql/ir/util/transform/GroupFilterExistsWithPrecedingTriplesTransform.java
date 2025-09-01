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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrTripleLike;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrValues;

/**
 * When a FILTER EXISTS is immediately preceded by a single triple, and the EXISTS body itself contains an explicit
 * grouped block (i.e., its where has a single IrBGP line), wrap the preceding triple and the FILTER together in a
 * group. This mirrors the original grouped shape often produced by path alternation rewrites and preserves textual
 * stability for tests that expect braces.
 */
public final class GroupFilterExistsWithPrecedingTriplesTransform extends BaseTransform {

	private GroupFilterExistsWithPrecedingTriplesTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		return apply(bgp, false, false);
	}

	/**
	 * Internal entry that carries context flags: - insideExists: true when traversing an EXISTS body - insideContainer:
	 * true when traversing inside a container (GRAPH/OPTIONAL/MINUS/UNION/SERVICE or nested BGP), i.e., not the
	 * top-level WHERE. We allow grouping in these nested scopes to match expected brace structure.
	 */
	private static IrBGP apply(IrBGP bgp, boolean insideExists, boolean insideContainer) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		int i = 0;
		// When inside an EXISTS body that already mixes a triple-like with a nested EXISTS/VALUES,
		// IrExists#print will synthesize an extra outer grouping to preserve intent. Avoid adding yet
		// another inner grouping here to prevent double braces.
		boolean avoidWrapInsideExists = false;
		if (insideExists) {
			boolean hasTripleLike = false;
			boolean hasNestedExistsOrValues = false;
			for (IrNode ln : in) {
				if (ln instanceof IrTripleLike) {
					hasTripleLike = true;
				} else if (ln instanceof IrFilter) {
					IrFilter fx = (IrFilter) ln;
					if (fx.getBody() instanceof IrExists) {
						hasNestedExistsOrValues = true;
					}
				} else if (ln instanceof IrValues) {
					hasNestedExistsOrValues = true;
				}
			}
			avoidWrapInsideExists = in.size() >= 2 && hasTripleLike && hasNestedExistsOrValues;
		}
		while (i < in.size()) {
			IrNode n = in.get(i);
			// Pattern: SP, FILTER(EXISTS { BODY })
			// If BODY is explicitly grouped (i.e., IrBGP nested) OR if BODY consists of multiple
			// lines and contains a nested FILTER EXISTS, wrap the SP and FILTER in an outer group
			// to preserve the expected brace structure and textual stability.
			if (i + 1 < in.size() && n instanceof IrStatementPattern
					&& in.get(i + 1) instanceof IrFilter) {
				IrFilter f = (IrFilter) in.get(i + 1);
				boolean allowHere = insideExists || insideContainer || f.isNewScope();
				if (allowHere && f.getBody() instanceof IrExists) {
					// Top-level: when the FILTER introduces a new scope, always wrap to
					// preserve explicit outer grouping from the original query.
					// Inside EXISTS: always wrap a preceding triple with the FILTER EXISTS to
					// preserve expected brace grouping in nested EXISTS tests.
					boolean doWrap = (f.isNewScope() || insideExists) && !(insideExists && avoidWrapInsideExists);
					if (doWrap) {
						IrBGP grp = new IrBGP(true);
						// Preserve original local order: preceding triple(s) before the FILTER EXISTS
						grp.add(n);
						grp.add(f);
						out.add(grp);
						i += 2;
						continue;
					}
				}
			}

			// Recurse into containers
			if (n instanceof IrBGP) {
				out.add(apply((IrBGP) n, insideExists, true));
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), apply(g.getWhere(), insideExists, true), g.isNewScope()));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere(), insideExists, true), o.isNewScope());
				no.setNewScope(o.isNewScope());
				out.add(no);
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				out.add(new IrMinus(apply(mi.getWhere(), insideExists, true), mi.isNewScope()));
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere(), insideExists, true),
						s.isNewScope()));
			} else if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b, insideExists, true));
				}
				out.add(u2);
			} else if (n instanceof IrSubSelect) {
				out.add(n); // keep
			} else if (n instanceof IrFilter) {
				// Recurse into EXISTS body if present
				IrFilter f2 = (IrFilter) n;
				IrNode body = f2.getBody();
				if (body instanceof IrExists) {
					IrExists ex = (IrExists) body;
					IrFilter nf = new IrFilter(new IrExists(apply(ex.getWhere(), true, true), ex.isNewScope()),
							f2.isNewScope());
					out.add(nf);
				} else {
					out.add(n);
				}
			} else {
				out.add(n);
			}
			i++;
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}
}
