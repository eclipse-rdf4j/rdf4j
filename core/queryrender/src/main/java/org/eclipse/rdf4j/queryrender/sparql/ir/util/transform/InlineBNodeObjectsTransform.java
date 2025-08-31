/**
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
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
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Inline simple anonymous blank nodes used as the object of a single triple into bracket notation on that triple, using
 * any subject-equal triples as the content of the bracket property list.
 *
 * Example (variables elided for brevity): _:b ex:pB _:x . and _:x ex:pC ?o . becomes _:b ex:pB [ ex:pC ?o ] .
 *
 * Safety heuristics: - Only inline variables named with the parser hint prefix "_anon_bnode_" that do not have a bound
 * value. - The candidate must occur exactly once as an object in this BGP and never as a predicate. - The candidate
 * must occur one or more times as a subject; all such subject-equal triples are used to form the bracket's property
 * list (constant-IRI predicates are rendered compactly; rdf:type renders as "a"). - Other occurrences (e.g., in nested
 * containers) are handled recursively per container.
 */
public final class InlineBNodeObjectsTransform extends BaseTransform {
	private InlineBNodeObjectsTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null)
			return null;

		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();

		// Recurse first so nested blocks get their own inlining before we compute local maps
		final List<IrNode> pre = new ArrayList<>(in.size());
		for (IrNode n : in) {
			if (n instanceof IrBGP) {
				pre.add(apply((IrBGP) n, r));
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				pre.add(new IrGraph(g.getGraph(), apply(g.getWhere(), r)));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere(), r));
				no.setNewScope(o.isNewScope());
				pre.add(no);
			} else if (n instanceof IrMinus) {
				IrMinus m = (IrMinus) n;
				pre.add(new IrMinus(apply(m.getWhere(), r)));
			} else if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion();
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b, r));
				}
				pre.add(u2);
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				pre.add(new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere(), r)));
			} else if (n instanceof IrSubSelect) {
				pre.add(n); // keep raw subselects unchanged
			} else {
				pre.add(n);
			}
		}

		// Build role indexes for this local BGP
		final Map<String, Integer> subjCount = new LinkedHashMap<>();
		final Map<String, Integer> objCount = new LinkedHashMap<>();
		final Set<String> predNames = new LinkedHashSet<>();
		final Map<String, List<IrStatementPattern>> bySubject = new LinkedHashMap<>();
		final Map<String, IrStatementPattern> parentByObject = new LinkedHashMap<>();

		for (IrNode n : pre) {
			if (!(n instanceof IrStatementPattern))
				continue;
			final IrStatementPattern sp = (IrStatementPattern) n;
			final Var s = sp.getSubject();
			final Var p = sp.getPredicate();
			final Var o = sp.getObject();
			if (s != null && !s.hasValue() && s.getName() != null) {
				subjCount.merge(s.getName(), 1, Integer::sum);
				bySubject.computeIfAbsent(s.getName(), k -> new ArrayList<>()).add(sp);
			}
			if (o != null && !o.hasValue() && o.getName() != null) {
				objCount.merge(o.getName(), 1, Integer::sum);
				// only record first parent by object to prefer earliest occurrence for readability
				parentByObject.putIfAbsent(o.getName(), sp);
			}
			if (p != null && !p.hasValue() && p.getName() != null) {
				predNames.add(p.getName());
			}
		}

		// Phase 1: decide candidates and capture their parents and properties
		final Map<String, IrStatementPattern> parentFor = new LinkedHashMap<>();
		final Map<String, List<IrStatementPattern>> propsFor = new LinkedHashMap<>();
		for (Map.Entry<String, List<IrStatementPattern>> e : bySubject.entrySet()) {
			final String vName = e.getKey();
			if (!isAnonBNodeName(vName))
				continue;
			final int oCount = objCount.getOrDefault(vName, 0);
			final int sCount = subjCount.getOrDefault(vName, 0);
			if (oCount != 1 || sCount < 1)
				continue;
			if (predNames.contains(vName))
				continue;
			final IrStatementPattern parent = parentByObject.get(vName);
			if (parent == null)
				continue;
			// Conservative guard as above
			boolean parentHasSibling = false;
			for (IrNode n2 : pre) {
				if (n2 instanceof IrStatementPattern) {
					IrStatementPattern sp2 = (IrStatementPattern) n2;
					if (sp2 != parent && sameVar(parent.getSubject(), sp2.getSubject())) {
						parentHasSibling = true;
						break;
					}
				}
			}
			if (!parentHasSibling)
				continue;
			parentFor.put(vName, parent);
			propsFor.put(vName, e.getValue());
		}

		// Phase 2: build overrides and replacements; ensure nested candidates are referenced via placeholders
		final Map<String, String> overrides = new LinkedHashMap<>();
		final Set<IrNode> consumed = new LinkedHashSet<>();
		final Map<IrStatementPattern, Var> parentReplacements = new LinkedHashMap<>();
		final Map<String, Var> replacementByObjVarName = new LinkedHashMap<>();
		final Set<IrStatementPattern> replacedParents = new LinkedHashSet<>();
		for (Map.Entry<String, List<IrStatementPattern>> e : propsFor.entrySet()) {
			final String vName = e.getKey();
			final IrStatementPattern parent = parentFor.get(vName);
			final List<IrStatementPattern> props = e.getValue();
			if (props == null || props.isEmpty())
				continue;

			// Build predicate -> list(objects) with nested placeholders for known candidates
			final LinkedHashMap<String, List<String>> objsByPredText = new LinkedHashMap<>();
			for (IrStatementPattern sp : props) {
				final Var pv = sp.getPredicate();
				final Var ov = sp.getObject();
				final String predText;
				if (pv != null && pv.hasValue() && pv.getValue() instanceof IRI && RDF.TYPE.equals(pv.getValue())) {
					predText = "a";
				} else {
					predText = varOrValue(pv, r);
				}
				final String objText;
				if (ov != null && !ov.hasValue() && ov.getName() != null && parentFor.containsKey(ov.getName())) {
					objText = "?__inline_bnode__" + ov.getName();
				} else {
					objText = varOrValue(ov, r);
				}
				objsByPredText.computeIfAbsent(predText, k -> new ArrayList<>()).add(objText);
				consumed.add(sp);
			}
			if (objsByPredText.isEmpty())
				continue;
			final List<String> parts = new ArrayList<>(objsByPredText.size());
			for (Map.Entry<String, List<String>> it : objsByPredText.entrySet()) {
				final String pred = it.getKey();
				final List<String> objs = it.getValue();
				final String objTxt = objs.size() <= 1 ? (objs.isEmpty() ? "?_" : objs.get(0))
						: String.join(", ", objs);
				parts.add(pred + " " + objTxt);
			}
			final String bracket = "[ " + String.join(" ; ", parts) + " ]";
			final String placeholderName = "__inline_bnode__" + vName;
			final Var placeholder = new Var(placeholderName);
			overrides.put(placeholderName, bracket);
			// Replace the parent triple only once; nested candidates share the same parent
			if (!replacedParents.contains(parent)) {
				parentReplacements.put(parent, placeholder);
				replacedParents.add(parent);
				if (parent.getObject() != null && !parent.getObject().hasValue()
						&& parent.getObject().getName() != null) {
					replacementByObjVarName.put(parent.getObject().getName(), placeholder);
				}
			}
		}

		if (!overrides.isEmpty()) {
			r.addOverrides(overrides);
		}

		// Emit all lines except those consumed as bracket contents; replace parent triples
		for (IrNode n : pre) {
			if (consumed.contains(n)) {
				continue;
			}
			if (n instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) n;
				// Prefer identity match first
				Var repl = parentReplacements.get(sp);
				if (repl == null) {
					Var obj = sp.getObject();
					if (obj != null && !obj.hasValue() && obj.getName() != null) {
						repl = replacementByObjVarName.get(obj.getName());
					}
				}
				if (repl != null) {
					out.add(new IrStatementPattern(sp.getSubject(), sp.getPredicate(), repl));
					continue;
				}
			}
			out.add(n);
		}

		final IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	private static boolean isAnonBNodeName(final String name) {
		return name != null && name.startsWith("_anon_bnode_");
	}
}
