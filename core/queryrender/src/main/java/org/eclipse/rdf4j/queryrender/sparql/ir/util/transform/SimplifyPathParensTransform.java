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
import java.util.regex.Pattern;

import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Simplify redundant parentheses in textual path expressions for readability and idempotence.
 *
 * Safe rewrites: - ((!(...))) -> (!(...)) - (((X))?) -> ((X)?)
 */
public final class SimplifyPathParensTransform extends BaseTransform {
	private SimplifyPathParensTransform() {
	}

	private static final Pattern DOUBLE_WRAP_NPS = Pattern.compile("\\(\\(\\(!\\([^()]*\\)\\)\\)\\)");
	private static final Pattern TRIPLE_WRAP_OPTIONAL = Pattern.compile("\\(\\(\\(([^()]+)\\)\\)\\?\\)\\)");

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null)
			return null;
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				String ptxt = pt.getPathText();
				String rew = simplify(ptxt);
				if (!rew.equals(ptxt)) {
					m = new IrPathTriple(pt.getSubject(), rew, pt.getObject());
				}
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere()));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				m = new IrOptional(apply(o.getWhere()));
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere()));
			} else if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion();
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b));
				}
				m = u2;
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere()));
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			}
			out.add(m);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}

	static String simplify(String s) {
		if (s == null)
			return null;
		String prev;
		String cur = s;
		int guard = 0;
		do {
			prev = cur;
			cur = DOUBLE_WRAP_NPS.matcher(cur).replaceAll("(!$1)");
			cur = TRIPLE_WRAP_OPTIONAL.matcher(cur).replaceAll("(($1)?)");
		} while (!cur.equals(prev) && ++guard < 5);
		return cur;
	}
}
