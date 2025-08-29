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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

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
		if (bgp == null)
			return null;
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		int i = 0;
		while (i < in.size()) {
			IrNode n = in.get(i);
			// Pattern: SP, FILTER(EXISTS { BODY })
			// If BODY is explicitly grouped (i.e., IrBGP nested) OR if BODY consists of multiple
			// lines and contains a nested FILTER EXISTS, wrap the SP and FILTER in an outer group
			// to preserve the expected brace structure and textual stability.
			if (i + 1 < in.size() && n instanceof IrStatementPattern && in.get(i + 1) instanceof IrFilter) {
				IrFilter f = (IrFilter) in.get(i + 1);
				if (f.getBody() instanceof IrExists) {
					IrExists ex = (IrExists) f.getBody();
					IrBGP inner = ex.getWhere();
					if (inner != null && inner.getLines().size() == 1 && inner.getLines().get(0) instanceof IrBGP) {
						IrBGP grp = new IrBGP();
						grp.add(n);
						grp.add(f);
						out.add(grp);
						i += 2;
						continue;
					}
				}
			}

			// Recurse into containers
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), apply(g.getWhere())));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				out.add(new IrOptional(apply(o.getWhere())));
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				out.add(new IrMinus(apply(mi.getWhere())));
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere())));
			} else if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion();
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b));
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
					out.add(new IrFilter(new IrExists(apply(ex.getWhere()), ex.isNewScope())));
				} else {
					out.add(n);
				}
			} else {
				out.add(n);
			}
			i++;
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}
}
