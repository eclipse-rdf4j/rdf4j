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
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
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

/** Hoists a common α-equivalent head out of UNION inside an OPTIONAL, with FILTER/BIND constraints. */
public final class OptionalUnionHoistOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new AbstractSimpleQueryModelVisitor<RuntimeException>() {
			@Override
			public void meet(LeftJoin lj) {
				super.meet(lj);
				TupleExpr right = lj.getRightArg();
				if (!(right instanceof Union)) {
					return;
				}

				// flatten the union arms
				List<TupleExpr> arms = flattenUnion((Union) right);
				if (arms.size() < 2) {
					return;
				}

				// decompose all arms
				List<BranchDecomposer.Parts> parts = new ArrayList<>(arms.size());
				for (TupleExpr arm : arms) {
					BranchDecomposer.Parts p = BranchDecomposer.decompose(arm);
					if (p == null || p.triples.isEmpty()) {
						return;
					}
					parts.add(p);
				}

				// α-unify common prefix against the first arm
				List<StatementPattern> baseTriples = parts.get(0).triples;
				int headLen = Integer.MAX_VALUE;
				List<Map<String, String>> renamings = new ArrayList<>(arms.size());
				renamings.add(Collections.emptyMap());
				for (int i = 1; i < parts.size(); i++) {
					AlphaEquivalenceUtil.Result r = AlphaEquivalenceUtil.unifyCommonPrefix(baseTriples,
							parts.get(i).triples);
					headLen = Math.min(headLen, r.matchedLen);
					renamings.add(r.renameCandToBase);
				}
				if (headLen <= 0) {
					return;
				}

				// canonical head vars (from base arm prefix)
				Set<String> headVarsCanon = new HashSet<>(VarNameCollector.process(baseTriples.subList(0, headLen)));

				List<TupleExpr> tails = new ArrayList<>();
				List<ValueExpr> canonicalHeadFilters = null;

				for (int i = 0; i < parts.size(); i++) {
					var p = parts.get(i);
					var map = renamings.get(i);

					// rename a clone of arm’s triples to base vars
					List<StatementPattern> triples = p.triples.stream()
							.map(sp -> sp.clone())
							.collect(Collectors.toList());
					for (int j = 0; j < Math.min(headLen, triples.size()); j++) {
						VarRenamer.renameInPlace(triples.get(j), map);
					}

					// tail triples (renamed)
					List<StatementPattern> tailTriples = new ArrayList<>();
					for (int j = headLen; j < triples.size(); j++) {
						StatementPattern s = triples.get(j).clone();
						VarRenamer.renameInPlace(s, map);
						tailTriples.add(s);
					}

					// rename filters/exts
					List<Filter> filters = p.filters.stream()
							.map(f -> VarRenamer.renameClone(f, map))
							.collect(Collectors.toList());
					List<Extension> exts = p.extensions.stream()
							.map(e -> VarRenamer.renameClone(e, map))
							.collect(Collectors.toList());

					// classify exts: keep on tail; crossing abort
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
							if (!headVarsCanon.containsAll(deps)) {
								headOnly = false;
							}
							if (!tailScope.containsAll(deps)) {
								tailOnly = false;
							}
						}
						if (!headOnly && !tailOnly && !e.getElements().isEmpty()) {
							return; // crossing BIND
						}
						tailExts.add(e);
						for (ExtensionElem ee : e.getElements()) {
							tailScope.add(ee.getName());
						}
					}

					// classify filters
					List<ValueExpr> headFilters = new ArrayList<>();
					List<Filter> tailFilters = new ArrayList<>();
					for (Filter f : filters) {
						Set<String> deps = VarNameCollector.process(f.getCondition());
						boolean inHead = headVarsCanon.containsAll(deps);
						boolean inTail = tailScope.containsAll(deps);
						if (inHead && !inTail || deps.isEmpty()) {
							headFilters.add(f.getCondition().clone());
						} else if (!inHead && inTail) {
							tailFilters.add(f);
						} else {
							return; // crossing filter across head/tail -> abort
						}
					}
					if (canonicalHeadFilters == null) {
						canonicalHeadFilters = headFilters;
					} else if (!sameExprList(canonicalHeadFilters, headFilters)) {
						return;
					}

					// build tail
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
					tails.add(tail);
				}

				// assemble Join(head, Union(tails)) with head-only filters on head
				TupleExpr head = buildJoin(baseTriples.subList(0, headLen));
				for (ValueExpr f : canonicalHeadFilters) {
					head = new Filter(head, f.clone());
				}
				TupleExpr union = foldUnion(tails);
				lj.setRightArg(new Join(head, union));
			}
		});
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
			return new SingletonSet();
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
}
