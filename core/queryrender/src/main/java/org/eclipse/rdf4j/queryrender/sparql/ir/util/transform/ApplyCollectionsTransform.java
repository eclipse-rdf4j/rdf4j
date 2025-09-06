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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrCollection;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Recognize RDF collection encodings (rdf:first/rdf:rest/... rdf:nil) headed by an anonymous collection variable and
 * rewrite them to SPARQL collection syntax in text, e.g., {@code ?s ex:list (1 2 3)}.
 *
 * Details: - Scans the WHERE lines for contiguous rdf:first/rdf:rest chains and records the textual value sequence. -
 * Exposes overrides via the renderer so that the head variable prints as the compact "(item1 item2 ...)" form. -
 * Removes the consumed rdf:first/rest triples from the IR; recursion preserves container structure.
 */
public final class ApplyCollectionsTransform extends BaseTransform {
	private ApplyCollectionsTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		// Collect FIRST/REST triples by subject
		final Map<String, IrStatementPattern> firstByS = new LinkedHashMap<>();
		final Map<String, IrStatementPattern> restByS = new LinkedHashMap<>();
		for (IrNode n : bgp.getLines()) {
			if (!(n instanceof IrStatementPattern)) {
				continue;
			}
			IrStatementPattern sp = (IrStatementPattern) n;
			Var s = sp.getSubject();
			Var p = sp.getPredicate();
			if (s == null || p == null || s.getName() == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
				continue;
			}
			IRI pred = (IRI) p.getValue();
			if (RDF.FIRST.equals(pred)) {
				firstByS.put(s.getName(), sp);
			} else if (RDF.REST.equals(pred)) {
				restByS.put(s.getName(), sp);
			}
		}
		// Build structural collections and record consumed list triples
		final Map<String, IrCollection> collections = new LinkedHashMap<>();
		final Set<IrNode> consumed = new LinkedHashSet<>();

		for (String head : firstByS.keySet()) {
			if (head == null || (!head.startsWith("_anon_collection_") && !restByS.containsKey(head))) {
				continue;
			}
			List<Var> items = new ArrayList<>();
			Set<String> spine = new LinkedHashSet<>();
			String cur = head;
			int guard = 0;
			boolean ok = true;
			while (true) {
				if (++guard > 10000) {
					ok = false;
					break;
				}
				IrStatementPattern f = firstByS.get(cur);
				IrStatementPattern rSp = restByS.get(cur);
				if (f == null || rSp == null) {
					ok = false;
					break;
				}
				spine.add(cur);
				Var o = f.getObject();
				if (o != null) {
					items.add(o);
				}
				consumed.add(f);
				consumed.add(rSp);
				Var ro = rSp.getObject();
				if (ro == null) {
					ok = false;
					break;
				}
				if (ro.hasValue()) {
					if (!(ro.getValue() instanceof IRI) || !RDF.NIL.equals(ro.getValue())) {
						ok = false;
					}
					break; // end of list
				}
				cur = ro.getName();
				if (cur == null || cur.isEmpty() || spine.contains(cur)) {
					ok = false;
					break;
				}
			}
			if (ok && !items.isEmpty()) {
				IrCollection col = new IrCollection(false);
				for (Var v : items) {
					col.addItem(v);
				}
				collections.put(head, col);
			}
		}
		// Rewrite lines: replace occurrences of the collection head variable with an IrCollection node when used as
		// subject or object in triple/path triples; remove consumed list triples
		List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			if (consumed.contains(n)) {
				continue;
			}
			if (n instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) n;
				// Subject replacement if the subject is a collection head
				Var subj = sp.getSubject();
				if (subj != null && !subj.hasValue() && subj.getName() != null
						&& collections.containsKey(subj.getName())) {
					IrCollection col = collections.get(subj.getName());
					sp.setSubjectOverride(col);
				}

				// Object replacement if the object is a collection head
				Var obj = sp.getObject();
				if (obj != null && !obj.hasValue() && obj.getName() != null && collections.containsKey(obj.getName())) {
					IrCollection col = collections.get(obj.getName());
					sp.setObjectOverride(col);
					out.add(sp);
					continue;
				}
			} else if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				// Subject replacement for path triple
				Var subj = pt.getSubject();
				if (subj != null && !subj.hasValue() && subj.getName() != null
						&& collections.containsKey(subj.getName())) {
					IrCollection col = collections.get(subj.getName());
					pt.setSubjectOverride(col);
				}
				// Object replacement for path triple
				Var obj = pt.getObject();
				if (obj != null && !obj.hasValue() && obj.getName() != null && collections.containsKey(obj.getName())) {
					IrCollection col = collections.get(obj.getName());
					pt.setObjectOverride(col);
				}
			} else if (n instanceof IrBGP || n instanceof IrGraph || n instanceof IrOptional || n instanceof IrUnion
					|| n instanceof IrMinus || n instanceof IrService || n instanceof IrSubSelect) {
				n = n.transformChildren(child -> {
					if (child instanceof IrBGP) {
						return apply((IrBGP) child);
					}
					return child;
				});
			}
			out.add(n);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}
}
