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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;

/** α-equivalence unification utilities for StatementPattern sequences. */
public final class AlphaEquivalenceUtil {

	private AlphaEquivalenceUtil() {
	}

	/** Prefix unification: return length k of common α-equivalent prefix and var mapping (cand->base). */
	public static Result unifyCommonPrefix(List<StatementPattern> base, List<StatementPattern> cand) {
		int max = Math.min(base.size(), cand.size());
		Map<String, String> map = new HashMap<>(), inv = new HashMap<>();
		int k = 0;
		for (int i = 0; i < max; i++) {
			if (!unifySP(base.get(i), cand.get(i), map, inv))
				break;
			k++;
		}
		return new Result(k, map);
	}

	/** Match all SPs in 'base' as a subset of 'cand' (any order). */
	public static Result unifyBaseAsSubset(List<StatementPattern> base, List<StatementPattern> cand) {
		Map<String, String> map = new HashMap<>(), inv = new HashMap<>();
		boolean[] used = new boolean[cand.size()];
		for (StatementPattern a : base) {
			boolean matched = false;
			for (int j = 0; j < cand.size(); j++) {
				if (used[j])
					continue;
				if (unifySP(a, cand.get(j), map, inv)) {
					used[j] = true;
					matched = true;
					break;
				}
			}
			if (!matched)
				return new Result(0, Map.of());
		}
		return new Result(base.size(), map);
	}

	public static final class Result {
		public final int matchedLen;
		public final Map<String, String> renameCandToBase;

		public Result(int len, Map<String, String> ren) {
			this.matchedLen = len;
			this.renameCandToBase = ren;
		}
	}

	private static boolean unifySP(StatementPattern a, StatementPattern b,
			Map<String, String> map, Map<String, String> inv) {
		return unifyVar(a.getSubjectVar(), b.getSubjectVar(), map, inv)
				&& unifyVar(a.getPredicateVar(), b.getPredicateVar(), map, inv)
				&& unifyVar(a.getObjectVar(), b.getObjectVar(), map, inv)
				&& unifyVar(a.getContextVar(), b.getContextVar(), map, inv);
	}

	private static boolean unifyVar(Var va, Var vb, Map<String, String> map, Map<String, String> inv) {
		if (va == null || vb == null)
			return va == vb;
		if (va.hasValue() || vb.hasValue())
			return va.hasValue() && vb.hasValue() && va.getValue().equals(vb.getValue());
		String na = va.getName(), nb = vb.getName();
		String cur = map.get(nb);
		if (cur != null)
			return cur.equals(na);
		String back = inv.get(na);
		if (back != null && !back.equals(nb))
			return false; // bijection
		map.put(nb, na);
		inv.put(na, nb);
		return true;
	}
}
