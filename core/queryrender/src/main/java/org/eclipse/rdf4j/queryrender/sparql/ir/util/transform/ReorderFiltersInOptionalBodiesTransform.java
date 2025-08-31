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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPropertyList;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;

/**
 * Within OPTIONAL bodies, move simple FILTER conditions earlier when all their variables are already available from
 * preceding lines in the same OPTIONAL body. This improves readability and can unlock later fusions.
 *
 * Safety: - Only reorders plain text FILTER conditions; structured bodies (EXISTS/NOT EXISTS) are left in place. - A
 * FILTER is moved only if every variable it references appears in lines preceding the first nested OPTIONAL. -
 * Preserves container structure and recurses conservatively.
 */
public final class ReorderFiltersInOptionalBodiesTransform extends BaseTransform {
	private ReorderFiltersInOptionalBodiesTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			if (n instanceof IrOptional) {
				final IrOptional opt = (IrOptional) n;
				IrBGP inner = apply(opt.getWhere(), r);
				inner = reorderFiltersWithin(inner, r);
				IrOptional no = new IrOptional(inner);
				no.setNewScope(opt.isNewScope());
				out.add(no);
				continue;
			}
			if (n instanceof IrGraph) {
				final IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), apply(g.getWhere(), r)));
				continue;
			}
			// Recurse into other containers conservatively
			n = n.transformChildren(child -> {
				if (child instanceof IrBGP) {
					return apply((IrBGP) child, r);
				}
				return child;
			});
			out.add(n);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	public static IrBGP reorderFiltersWithin(IrBGP inner, TupleExprIRRenderer r) {
		if (inner == null) {
			return null;
		}
		final List<IrNode> lines = inner.getLines();
		int firstOpt = -1;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i) instanceof IrOptional) {
				firstOpt = i;
				break;
			}
		}
		if (firstOpt < 0) {
			return inner; // nothing to reorder
		}
		final List<IrNode> head = new ArrayList<>(lines.subList(0, firstOpt));
		final List<IrNode> tail = new ArrayList<>(lines.subList(firstOpt, lines.size()));
		final List<IrNode> filters = new ArrayList<>();
		// collect filters from head and tail
		final List<IrNode> newHead = new ArrayList<>();
		for (IrNode ln : head) {
			if (ln instanceof IrFilter) {
				filters.add(ln);
			} else {
				newHead.add(ln);
			}
		}
		final List<IrNode> newTail = new ArrayList<>();
		for (IrNode ln : tail) {
			if (ln instanceof IrFilter) {
				filters.add(ln);
			} else {
				newTail.add(ln);
			}
		}
		if (filters.isEmpty()) {
			return inner;
		}
		// Safety: only move filters whose vars are already available in newHead
		final Set<String> avail = collectVarsFromLines(newHead, r);
		final List<IrNode> safeFilters = new ArrayList<>();
		final List<IrNode> unsafeFilters = new ArrayList<>();
		for (IrNode f : filters) {
			if (!(f instanceof IrFilter)) {
				unsafeFilters.add(f);
				continue;
			}
			final String txt = ((IrFilter) f).getConditionText();
			// Structured filter bodies (e.g., EXISTS) have no condition text; do not reorder them.
			if (txt == null) {
				unsafeFilters.add(f);
				continue;
			}
			final Set<String> fv = extractVarsFromText(txt);
			if (avail.containsAll(fv)) {
				safeFilters.add(f);
			} else {
				unsafeFilters.add(f);
			}
		}
		final IrBGP res = new IrBGP(inner.isNewScope());
		// head non-filters, then safe filters, then tail, then any unsafe filters at the end
		newHead.forEach(res::add);
		safeFilters.forEach(res::add);
		newTail.forEach(res::add);
		unsafeFilters.forEach(res::add);
		res.setNewScope(inner.isNewScope());
		return res;
	}

	public static Set<String> collectVarsFromLines(List<IrNode> lines, TupleExprIRRenderer r) {
		final Set<String> out = new LinkedHashSet<>();
		if (lines == null) {
			return out;
		}
		for (IrNode ln : lines) {
			if (ln instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) ln;
				addVarName(out, sp.getSubject());
				addVarName(out, sp.getObject());
				continue;
			}
			if (ln instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) ln;
				addVarName(out, pt.getSubject());
				addVarName(out, pt.getObject());
				continue;
			}
			if (ln instanceof IrPropertyList) {
				IrPropertyList pl = (IrPropertyList) ln;
				addVarName(out, pl.getSubject());
				for (IrPropertyList.Item it : pl.getItems()) {
					for (Var v : it.getObjects()) {
						addVarName(out, v);
					}
				}
				continue;
			}
			if (ln instanceof IrGraph) {
				IrGraph g = (IrGraph) ln;
				out.addAll(collectVarsFromLines(
						g.getWhere() == null ? Collections.emptyList() : g.getWhere().getLines(), r));
			}
		}
		return out;
	}

	public static Set<String> extractVarsFromText(String s) {
		final Set<String> out = new LinkedHashSet<>();
		if (s == null) {
			return out;
		}
		Matcher m = Pattern.compile("\\?([A-Za-z_][\\w]*)").matcher(s);
		while (m.find()) {
			out.add(m.group(1));
		}
		return out;
	}

	public static void addVarName(Set<String> out, Var v) {
		if (v == null || v.hasValue()) {
			return;
		}
		final String n = v.getName();
		if (n != null && !n.isEmpty()) {
			out.add(n);
		}
	}

}
