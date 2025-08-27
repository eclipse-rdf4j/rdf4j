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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPropertyList;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;

/**
 * Convert runs of simple subject-equal triples into a property list form, using semicolon and comma shorthand where
 * possible. Example: three SPs with the same subject and two objects for the same predicate become
 * {@code ?s p1 ?a , ?b ; p2 ?c .}
 */
public final class ApplyPropertyListsTransform extends BaseTransform {
	private ApplyPropertyListsTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = bgp.getLines();
		List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			// Recurse
			n = n.transformChildren(child -> {
				if (child instanceof IrBGP) {
					return apply((IrBGP) child, r);
				}
				return child;
			});
			if (n instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) n;
				Var subj = sp.getSubject();
				// group contiguous SPs with identical subject
				Map<String, IrPropertyList.Item> map = new LinkedHashMap<>();
				int j = i;
				while (j < in.size() && in.get(j) instanceof IrStatementPattern) {
					IrStatementPattern spj = (IrStatementPattern) in.get(j);
					if (!sameVar(subj, spj.getSubject())) {
						break;
					}
					Var pj = spj.getPredicate();
					String key;
					if (pj != null && pj.hasValue() && pj.getValue() instanceof IRI) {
						key = r.renderIRI((IRI) pj.getValue());
					} else {
						key = (pj == null || pj.getName() == null) ? "?_" : ("?" + pj.getName());
					}
					IrPropertyList.Item item = map.get(key);
					if (item == null) {
						item = new IrPropertyList.Item(pj);
						map.put(key, item);
					}
					item.getObjects().add(spj.getObject());
					j++;
				}
				boolean multiPred = map.size() > 1;
				boolean hasComma = !multiPred && !map.isEmpty()
						&& map.values().iterator().next().getObjects().size() > 1;
				if (multiPred || hasComma) {
					IrPropertyList pl = new IrPropertyList(subj);
					for (IrPropertyList.Item it : map.values()) {
						pl.addItem(it);
					}
					out.add(pl);
					i = j - 1;
					continue;
				}
			}
			out.add(n);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		return res;
	}
}
