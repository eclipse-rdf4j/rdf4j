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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

public final class CanonicalizeBareNpsOrientationTransform extends BaseTransform {
	private CanonicalizeBareNpsOrientationTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			// Recurse into containers
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), apply(g.getWhere())));
				continue;
			}
			if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				out.add(new IrOptional(apply(o.getWhere())));
				continue;
			}
			if (n instanceof IrMinus) {
				IrMinus m = (IrMinus) n;
				out.add(new IrMinus(apply(m.getWhere())));
				continue;
			}
			if (n instanceof IrUnion) {
				// Do not alter orientation inside UNION branches; preserve branch subjects/objects.
				out.add(n);
				continue;
			}
			if (n instanceof IrService) {
				IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(),
						apply(s.getWhere())));
				continue;
			}
			out.add(n);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	public static String safeVarName(Var v) {
		if (v == null || v.hasValue()) {
			return null;
		}
		final String n = v.getName();
		return (n == null || n.isEmpty()) ? null : n;
	}
}
