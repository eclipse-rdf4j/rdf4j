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

import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrValues;

/**
 * Merge adjacent VALUES blocks under provably-safe conditions:
 *
 * - Identical variable lists (same names, same order): conjunction is equivalent to the multiset intersection of rows.
 * The merged VALUES has the same variable list and duplicates with multiplicity = m1 * m2 per identical row. - Disjoint
 * variable lists: conjunction is equivalent to a single multi-column VALUES with the cross product of rows (row
 * multiplicities multiply). Variable column order is preserved as [left vars..., right vars...].
 *
 * Overlapping-but-not-identical variable sets are left untouched.
 */
public final class MergeAdjacentValuesTransform extends BaseTransform {

	private MergeAdjacentValuesTransform() {
	}

	public static IrBGP apply(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		int i = 0;
		while (i < in.size()) {
			IrNode n = in.get(i);
			if (n instanceof IrValues && i + 1 < in.size() && in.get(i + 1) instanceof IrValues) {
				IrValues v1 = (IrValues) n;
				IrValues v2 = (IrValues) in.get(i + 1);
				IrValues merged = tryMerge(v1, v2);
				if (merged != null) {
					out.add(merged);
					i += 2;
					continue;
				}
			}
			// Recurse into containers conservatively
			out.add(BaseTransform.rewriteContainers(n, child -> apply(child)));
			i++;
		}
		return BaseTransform.bgpWithLines(bgp, out);
	}

	private static IrValues tryMerge(IrValues v1, IrValues v2) {
		List<String> a = v1.getVarNames();
		List<String> b = v2.getVarNames();
		if (a.isEmpty() && b.isEmpty()) {
			// () {} ∧ () {} = () {} with |rows| = |rows1| * |rows2|
			return crossProduct(v1, v2);
		}
		if (a.equals(b)) {
			return intersectRows(v1, v2);
		}
		Set<String> sa = new LinkedHashSet<>(a);
		Set<String> sb = new LinkedHashSet<>(b);
		Set<String> inter = new LinkedHashSet<>(sa);
		inter.retainAll(sb);
		if (inter.isEmpty()) {
			return crossProduct(v1, v2);
		}
		return null; // overlapping var sets not handled
	}

	// Cross product for disjoint variable lists
	private static IrValues crossProduct(IrValues v1, IrValues v2) {
		IrValues out = new IrValues(false);
		out.getVarNames().addAll(v1.getVarNames());
		out.getVarNames().addAll(v2.getVarNames());
		List<List<String>> r1 = v1.getRows();
		List<List<String>> r2 = v2.getRows();
		if (r1.isEmpty() || r2.isEmpty()) {
			// conjunctive semantics: empty on either side yields empty
			return out; // no rows
		}
		for (List<String> row1 : r1) {
			for (List<String> row2 : r2) {
				List<String> joined = new ArrayList<>(row1.size() + row2.size());
				joined.addAll(row1);
				joined.addAll(row2);
				out.getRows().add(joined);
			}
		}
		return out;
	}

	// Multiset intersection for identical variable lists; multiplicity = m1 * m2, order as in v1.
	private static IrValues intersectRows(IrValues v1, IrValues v2) {
		IrValues out = new IrValues(false);
		out.getVarNames().addAll(v1.getVarNames());
		Map<List<String>, Integer> c1 = multisetCounts(v1.getRows());
		Map<List<String>, Integer> c2 = multisetCounts(v2.getRows());
		if (c1.isEmpty() || c2.isEmpty()) {
			return out; // empty
		}
		for (List<String> r : v1.getRows()) {
			Integer m1 = c1.get(r);
			if (m1 == null || m1 == 0) {
				continue;
			}
			Integer m2 = c2.get(r);
			if (m2 == null || m2 == 0) {
				continue;
			}
			int mult = m1 * m2;
			// emit r exactly 'mult' times; also decrement c1 count to avoid duplicating again
			// Maintain order according to first appearance in v1
			for (int k = 0; k < mult; k++) {
				out.getRows().add(new ArrayList<>(r));
			}
			c1.put(r, 0); // so a duplicate in v1 list won’t re-emit again
		}
		return out;
	}

	private static Map<List<String>, Integer> multisetCounts(List<List<String>> rows) {
		Map<List<String>, Integer> m = new LinkedHashMap<>();
		for (List<String> r : rows) {
			// Use defensive copy to ensure stable key equality
			List<String> key = new ArrayList<>(r);
			m.put(key, m.getOrDefault(key, 0) + 1);
		}
		return m;
	}
}
