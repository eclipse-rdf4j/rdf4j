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
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdJoinIterator;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Describes an ID-binding record shape (variables and their positions) and can materialize it to a BindingSet.
 */
@InternalUseOnly
public final class IdBindingInfo implements IdAccessor {

	private final Map<String, Integer> indexByVar; // insertion order preserved
	private final Map<String, Integer> positionsMaskByVar; // aggregated from contributing patterns

	IdBindingInfo(LinkedHashMap<String, Integer> indexByVar, Map<String, Integer> positionsMaskByVar) {
		this.indexByVar = Collections.unmodifiableMap(new LinkedHashMap<>(indexByVar));
		this.positionsMaskByVar = Collections.unmodifiableMap(positionsMaskByVar);
	}

	@Override
	public Set<String> getVariableNames() {
		return indexByVar.keySet();
	}

	public int size() {
		return indexByVar.size();
	}

	public int getIndex(String name) {
		Integer idx = indexByVar.get(name);
		return idx == null ? -1 : idx;
	}

	public int getPositionsMask(String name) {
		Integer m = positionsMaskByVar.get(name);
		return m == null ? 0 : m;
	}

	@Override
	public int getRecordIndex(String varName) {
		return getIndex(varName);
	}

	@Override
	public long getId(long[] record, String varName) {
		int i = getIndex(varName);
		if (i < 0 || i >= record.length) {
			return LmdbValue.UNKNOWN_ID;
		}
		return record[i];
	}

	public boolean applyRecord(long[] record, MutableBindingSet target, ValueStore valueStore)
			throws QueryEvaluationException {
		for (Map.Entry<String, Integer> e : indexByVar.entrySet()) {
			String name = e.getKey();
			int idx = e.getValue();
			long id = record[idx];
			if (id == LmdbValue.UNKNOWN_ID) {
				continue;
			}
			int mask = getPositionsMask(name);
			if (((mask >> TripleStore.CONTEXT_IDX) & 1) == 1 && id == 0L) {
				// default graph treated as null (unbound)
				continue;
			}
			Value existing = target.getValue(name);
			Value candidate;
			try {
				candidate = valueStore.getLazyValue(id);
			} catch (IOException ex) {
				throw new QueryEvaluationException(ex);
			}
			if (existing != null && !existing.equals(candidate)) {
				return false;
			}
			if (existing == null) {
				target.setBinding(name, candidate);
			}
		}
		return true;
	}

	public static IdBindingInfo combine(IdBindingInfo left, LmdbIdJoinIterator.PatternInfo right) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
		Map<String, Integer> masks = new java.util.HashMap<>();
		// left variables first
		if (left != null) {
			for (String v : left.getVariableNames()) {
				map.put(v, map.size());
				masks.put(v, left.getPositionsMask(v));
			}
		}
		// add right-only variables and merge masks for shared
		for (String v : right.getVariableNames()) {
			Integer mask = right.getPositionsMask(v);
			if (!map.containsKey(v)) {
				map.put(v, map.size());
				masks.put(v, mask);
			} else {
				masks.put(v, masks.getOrDefault(v, 0) | mask);
			}
		}
		return new IdBindingInfo(map, masks);
	}

	public static IdBindingInfo fromFirstPattern(LmdbIdJoinIterator.PatternInfo first) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
		Map<String, Integer> masks = new java.util.HashMap<>();
		// Use SPOC order for stability
		String s = firstVarName(first, TripleStore.SUBJ_IDX);
		if (s != null) {
			map.put(s, map.size());
			masks.put(s, first.getPositionsMask(s));
		}
		String p = firstVarName(first, TripleStore.PRED_IDX);
		if (p != null && !map.containsKey(p)) {
			map.put(p, map.size());
			masks.put(p, first.getPositionsMask(p));
		}
		String o = firstVarName(first, TripleStore.OBJ_IDX);
		if (o != null && !map.containsKey(o)) {
			map.put(o, map.size());
			masks.put(o, first.getPositionsMask(o));
		}
		String c = firstVarName(first, TripleStore.CONTEXT_IDX);
		if (c != null && !map.containsKey(c)) {
			map.put(c, map.size());
			masks.put(c, first.getPositionsMask(c));
		}
		// include any remaining (e.g., repeated vars not in SPOC scan)
		for (String v : first.getVariableNames()) {
			if (!map.containsKey(v)) {
				map.put(v, map.size());
				masks.put(v, first.getPositionsMask(v));
			}
		}
		return new IdBindingInfo(map, masks);
	}

	private static String firstVarName(LmdbIdJoinIterator.PatternInfo info, int position) {
		for (String v : info.getVariableNames()) {
			int mask = info.getPositionsMask(v);
			if (((mask >> position) & 1) == 1) {
				return v;
			}
		}
		return null;
	}
}
