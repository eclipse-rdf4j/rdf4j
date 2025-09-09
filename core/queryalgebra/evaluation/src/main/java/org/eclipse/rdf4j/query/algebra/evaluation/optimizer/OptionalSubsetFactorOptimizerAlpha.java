/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

/**
 * Sibling-OPTIONAL subset factoring with α-equivalence and FILTER/BIND handling.
 *
 * Matches LeftJoin( LeftJoin(L, A), R ) where R is either BGP-like with Aα subset, or UNION of arms each with Aα
 * subset. Rewrites to LeftJoin( L, LeftJoin( A, Tail ) [cond] ).
 *
 * Now wrapper-aware: will unwrap outer Filter/Extension around R or around the UNION inside R.
 */
public final class OptionalSubsetFactorOptimizerAlpha implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr expr, Dataset dataset, BindingSet bindings) {
		expr.visit(new Visitor());
	}

	// ---- Small record for unwrapping Filters/Extensions
	private static final class FEWrap {
		final List<Filter> filters = new ArrayList<>();
		final List<Extension> exts = new ArrayList<>();
		TupleExpr core;
	}

	private static FEWrap unwrapFE(TupleExpr e) {
		FEWrap w = new FEWrap();
		TupleExpr cur = e;
		boolean changed = true;
		while (changed) {
			changed = false;
			if (cur instanceof Filter) {
				var f = (Filter) cur;
				w.filters.add(f);
				cur = f.getArg();
				changed = true;
				continue;
			}
			if (cur instanceof Extension) {
				var ex = (Extension) cur;
				w.exts.add(ex);
				cur = ex.getArg();
				changed = true;
				continue;
			}
		}
		w.core = cur;
		return w;
	}

	private static final class Visitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {
		@Override
		public void meet(LeftJoin lj2) {
			super.meet(lj2);

			if (!(lj2.getLeftArg() instanceof LeftJoin)) {
				return;
			}
			LeftJoin lj1 = (LeftJoin) lj2.getLeftArg();

			// Conservative if conditions already present on the matched nodes
			if (lj1.getCondition() != null || lj2.getCondition() != null) {
				return;
			}

			TupleExpr L = lj1.getLeftArg();
			TupleExpr Aexpr = lj1.getRightArg();
			TupleExpr Rraw = lj2.getRightArg();

			BranchDecomposer.Parts Ap = BranchDecomposer.decompose(Aexpr);
			if (Ap == null || Ap.triples.isEmpty()) {
				return;
			}

			// Unwrap R for filter/extension wrappers
			FEWrap wrapR = unwrapFE(Rraw);
			TupleExpr Rcore = wrapR.core;

			boolean ok;
			if (Rcore instanceof Union) {
				var u = (Union) Rcore;
				ok = rewriteUnionCase(lj2, L, Aexpr, Ap, u, wrapR);
			} else {
				ok = rewriteSingleCase(lj2, L, Aexpr, Ap, wrapR);
			}
			if (!ok) {
			}
		}
	}

	// ---------- single-branch R (with possible wrapper filters/exts)
	private static boolean rewriteSingleCase(LeftJoin host, TupleExpr L, TupleExpr Aexpr,
			BranchDecomposer.Parts Ap, FEWrap wrapR) {
		BranchDecomposer.Parts Rp = BranchDecomposer.decompose(wrapR.core);
		if (Rp == null || Rp.triples.isEmpty()) {
			return false;
		}

		AlphaEquivalenceUtil.Result m = AlphaEquivalenceUtil.unifyBaseAsSubset(Ap.triples, Rp.triples);
		if (m.matchedLen != Ap.triples.size()) {
			return false;
		}

		// rename R to A's var names
		List<StatementPattern> Rtrip = Rp.triples.stream().map(sp -> sp.clone()).collect(Collectors.toList());
		for (StatementPattern sp : Rtrip) {
			VarRenamer.renameInPlace(sp, m.renameCandToBase);
		}
		List<Filter> Rfilters = new ArrayList<>();
		for (Filter f : Rp.filters) {
			Rfilters.add(VarRenamer.renameClone(f, m.renameCandToBase));
		}
		for (Filter f : wrapR.filters) {
			Rfilters.add(VarRenamer.renameClone(f, m.renameCandToBase));
		}
		List<Extension> Rexts = new ArrayList<>();
		for (Extension e : Rp.extensions) {
			Rexts.add(VarRenamer.renameClone(e, m.renameCandToBase));
		}
		for (Extension e : wrapR.exts) {
			Rexts.add(VarRenamer.renameClone(e, m.renameCandToBase));
		}

		// Tail = Rtrip \ Atrip
		Set<String> Aeq = Ap.triples.stream().map(Object::toString).collect(Collectors.toSet());
		List<StatementPattern> tailTriples = Rtrip.stream()
				.filter(sp -> !Aeq.contains(sp.toString()))
				.collect(Collectors.toList());

		// scopes
		Set<String> headVars = varsOf(Aexpr);
		Set<String> tailVars = new HashSet<>();
		for (StatementPattern sp : tailTriples) {
			tailVars.addAll(VarNameCollector.process(sp));
		}

		// classify BINDs: both head-only and tail-only remain on tail; crossing aborts
		List<Extension> tailExts = new ArrayList<>();
		Set<String> tailDefined = new HashSet<>();
		for (Extension e : Rexts) {
			boolean headOnly = true, tailOnly = true;
			for (ExtensionElem ee : e.getElements()) {
				Set<String> deps = VarNameCollector.process(ee.getExpr());
				if (!headVars.containsAll(deps)) {
					headOnly = false;
				}
				if (!tailVars.containsAll(deps)) {
					tailOnly = false;
				}
			}
			if (!headOnly && !tailOnly && !e.getElements().isEmpty()) {
				return false; // crossing BIND
			}
			tailExts.add(e);
			for (ExtensionElem ee : e.getElements()) {
				tailDefined.add(ee.getName());
			}
		}
		Set<String> tailScope = new HashSet<>(tailVars);
		tailScope.addAll(tailDefined);

		// classify FILTERs
		ValueExpr joinCond = null;
		List<Filter> tailFilters = new ArrayList<>();
		for (Filter f : Rfilters) {
			Set<String> deps = VarNameCollector.process(f.getCondition());
			boolean inHead = headVars.containsAll(deps);
			boolean inTail = tailScope.containsAll(deps);
			if (inHead && !inTail || deps.isEmpty()) {
				joinCond = and(joinCond, f.getCondition().clone());
			} else if (!inHead && inTail) {
				tailFilters.add(f);
			} else {
				// crossing filter -> inner left-join condition (allowed in single-branch case)
				joinCond = and(joinCond, f.getCondition().clone());
			}
		}

		// Build tail expr
		TupleExpr tail = buildJoin(tailTriples);
		for (Extension e : tailExts) {
			Extension c = e.clone();
			c.setArg(tail == null ? new SingletonSet() : tail);
			tail = c;
		}
		for (Filter f : tailFilters) {
			tail = new Filter(tail == null ? new SingletonSet() : tail, f.getCondition().clone());
		}
		if (tail == null) {
			tail = new SingletonSet();
		}

		// Inner LeftJoin(A, tail ; joinCond)
		LeftJoin inner = new LeftJoin(Aexpr.clone(), tail, joinCond);
		host.replaceWith(new LeftJoin(L.clone(), inner, null));
		return true;
	}

	// ---------- UNION arms (2+) with possible outer wrapper filters/exts
	private static boolean rewriteUnionCase(LeftJoin host, TupleExpr L, TupleExpr Aexpr,
			BranchDecomposer.Parts Ap, Union unionCore, FEWrap wrapR) {
		// wrapper EXTENSIONS above a UNION are not supported (would require duplicating per-arm)
		if (!wrapR.exts.isEmpty()) {
			return false;
		}

		List<TupleExpr> arms = flattenUnion(unionCore);
		if (arms.size() < 2) {
			return false;
		}

		List<BranchDecomposer.Parts> parts = new ArrayList<>(arms.size());
		for (TupleExpr arm : arms) {
			BranchDecomposer.Parts p = BranchDecomposer.decompose(arm);
			if (p == null || p.triples.isEmpty()) {
				return false;
			}
			parts.add(p);
		}

		// Each arm must contain A (α-equivalent) as subset
		List<Map<String, String>> renames = new ArrayList<>(arms.size());
		for (BranchDecomposer.Parts p : parts) {
			AlphaEquivalenceUtil.Result r = AlphaEquivalenceUtil.unifyBaseAsSubset(Ap.triples, p.triples);
			if (r.matchedLen != Ap.triples.size()) {
				return false;
			}
			renames.add(r.renameCandToBase);
		}

		Set<String> headVars = varsOf(Aexpr);

		// Global head-only filters (outside arms but inside the OPTIONAL R)
		List<ValueExpr> globalHeadFilters = new ArrayList<>();
		for (Filter f : wrapR.filters) {
			Set<String> deps = VarNameCollector.process(f.getCondition());
			if (!headVars.containsAll(deps)) {
				return false; // wrapper filter must be head-only
			}
			globalHeadFilters.add(f.getCondition().clone());
		}

		List<ValueExpr> canonicalArmHeadFilters = null;
		List<TupleExpr> newTails = new ArrayList<>(arms.size());

		for (int i = 0; i < parts.size(); i++) {
			var p = parts.get(i);
			var map = renames.get(i);

			// rename and subtract head
			List<StatementPattern> trip = p.triples.stream().map(sp -> sp.clone()).collect(Collectors.toList());
			for (StatementPattern sp : trip) {
				VarRenamer.renameInPlace(sp, map);
			}
			Set<String> Aeq = Ap.triples.stream().map(Object::toString).collect(Collectors.toSet());
			List<StatementPattern> tailTriples = trip.stream()
					.filter(sp -> !Aeq.contains(sp.toString()))
					.collect(Collectors.toList());

			// rename filters/exts
			List<Filter> filters = p.filters.stream()
					.map(f -> VarRenamer.renameClone(f, map))
					.collect(Collectors.toList());
			List<Extension> exts = p.extensions.stream()
					.map(e -> VarRenamer.renameClone(e, map))
					.collect(Collectors.toList());

			// classify BINDs (keep all on tail; crossing abort)
			List<Extension> tailExts = new ArrayList<>();
			Set<String> tailVars = new HashSet<>();
			for (StatementPattern sp : tailTriples) {
				tailVars.addAll(VarNameCollector.process(sp));
			}
			Set<String> tailDefined = BranchDecomposer.extensionDefinedVars(exts);
			Set<String> tailScope = new HashSet<>(tailVars);
			tailScope.addAll(tailDefined);

			for (Extension e : exts) {
				boolean headOnly = true, tailOnly = true;
				for (ExtensionElem ee : e.getElements()) {
					Set<String> deps = VarNameCollector.process(ee.getExpr());
					if (!headVars.containsAll(deps)) {
						headOnly = false;
					}
					if (!tailScope.containsAll(deps)) {
						tailOnly = false;
					}
				}
				if (!headOnly && !tailOnly && !e.getElements().isEmpty()) {
					return false; // crossing BIND
				}
				tailExts.add(e);
				for (ExtensionElem ee : e.getElements()) {
					tailScope.add(ee.getName());
				}
			}

			// classify FILTERs (head-only identical across arms; tail-only stay; crossing abort)
			List<ValueExpr> headFiltersArm = new ArrayList<>();
			List<Filter> tailFilters = new ArrayList<>();
			for (Filter f : filters) {
				Set<String> deps = VarNameCollector.process(f.getCondition());
				boolean inHead = headVars.containsAll(deps);
				boolean inTail = tailScope.containsAll(deps);
				if (inHead && !inTail || deps.isEmpty()) {
					headFiltersArm.add(f.getCondition().clone());
				} else if (!inHead && inTail) {
					tailFilters.add(f);
				} else {
					return false; // crossing filter not supported across arms
				}
			}
			if (canonicalArmHeadFilters == null) {
				canonicalArmHeadFilters = headFiltersArm;
			} else if (!sameExprList(canonicalArmHeadFilters, headFiltersArm)) {
				return false;
			}

			// build tail expr
			TupleExpr tail = buildJoin(tailTriples);
			for (Extension e : tailExts) {
				Extension c = e.clone();
				c.setArg(tail == null ? new SingletonSet() : tail);
				tail = c;
			}
			for (Filter f : tailFilters) {
				tail = new Filter(tail == null ? new SingletonSet() : tail, f.getCondition().clone());
			}
			if (tail == null) {
				tail = new SingletonSet();
			}
			newTails.add(tail);
		}

		TupleExpr union = foldUnion(newTails);
		// condition = global head-only (wrappers) AND identical per-arm head-only
		ValueExpr cond = andAll(concat(globalHeadFilters, canonicalArmHeadFilters));

		LeftJoin inner = new LeftJoin(Aexpr.clone(), union, cond);
		host.replaceWith(new LeftJoin(L.clone(), inner, null));
		return true;
	}

	// helpers
	private static List<TupleExpr> flattenUnion(Union u) {
		List<TupleExpr> out = new ArrayList<>();
		Deque<TupleExpr> dq = new ArrayDeque<>();
		dq.add(u);
		while (!dq.isEmpty()) {
			TupleExpr x = dq.removeFirst();
			if (x instanceof Union) {
				var uu = (Union) x;
				dq.addFirst(uu.getRightArg());
				dq.addFirst(uu.getLeftArg());
			} else {
				out.add(x);
			}
		}
		return out;
	}

	private static TupleExpr buildJoin(List<StatementPattern> sps) {
		if (sps == null || sps.isEmpty()) {
			return null;
		}
		TupleExpr acc = sps.get(0).clone();
		for (int i = 1; i < sps.size(); i++) {
			acc = new Join(acc, sps.get(i).clone());
		}
		return acc;
	}

	private static TupleExpr foldUnion(List<TupleExpr> items) {
		if (items.isEmpty()) {
			return new SingletonSet();
		}
		TupleExpr acc = items.get(0);
		for (int i = 1; i < items.size(); i++) {
			acc = new Union(acc, items.get(i));
		}
		return acc;
	}

	private static Set<String> varsOf(TupleExpr e) {
		Set<String> vs = new HashSet<>(VarNameCollector.process(e));
		e.visit(new AbstractSimpleQueryModelVisitor<>() {
			@Override
			public void meet(Extension ext) {
				for (ExtensionElem ee : ext.getElements()) {
					vs.add(ee.getName());
				}
			}
		});
		return vs;
	}

	private static boolean sameExprList(List<ValueExpr> a, List<ValueExpr> b) {
		if (a.size() != b.size()) {
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (!a.get(i).equals(b.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static <T> List<T> concat(List<T> a, List<T> b) {
		List<T> out = new ArrayList<>(a.size() + (b == null ? 0 : b.size()));
		out.addAll(a);
		if (b != null) {
			out.addAll(b);
		}
		return out;
	}

	private static ValueExpr and(ValueExpr a, ValueExpr b) {
		return a == null ? b : (b == null ? a : new And(a, b));
	}

	private static ValueExpr andAll(List<ValueExpr> exprs) {
		ValueExpr acc = null;
		if (exprs != null) {
			for (ValueExpr e : exprs) {
				acc = and(acc, e);
			}
		}
		return acc;
	}
}
