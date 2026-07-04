/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * An Iteration that returns the results of an Iteration (the left argument) MINUS any results that are compatible with
 * results of another Iteration (the right argument) or that have no shared variables. This iteration uses the formal
 * definition of the SPARQL 1.1 MINUS operator to determine which BindingSets to return.
 *
 * @author Jeen
 * @see <a href="http://www.w3.org/TR/sparql11-query/#sparqlAlgebra">SPARQL Algebra Documentation</a>
 */
public class SPARQLMinusIteration extends FilterIteration<BindingSet> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final CloseableIteration<BindingSet> rightArg;

	private boolean initialized;

	private Set<BindingSet> excludeSet;
	private Set<String> excludeSetBindingNames;
	private boolean excludeSetBindingNamesAreAllTheSame;
	private BindingSet[] excludeSetList;

	// Index of rightArg binding sets by (variable name, value) to quickly find candidates
	private Map<String, Map<Value, BindingSet[]>> rightIndex;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MinusIteration that returns the results of the left argument minus the results of the right
	 * argument. By default, duplicates are <em>not</em> filtered from the results.
	 *
	 * @param leftArg  An Iteration containing the main set of elements.
	 * @param rightArg An Iteration containing the set of elements that should be filtered from the main set.
	 */
	public SPARQLMinusIteration(CloseableIteration<BindingSet> leftArg, CloseableIteration<BindingSet> rightArg) {
		super(leftArg);

		assert rightArg != null;

		this.rightArg = rightArg;
		this.initialized = false;
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	// implements LookAheadIteration.getNextElement()
	@Override
	protected boolean accept(BindingSet bindingSet) {
		if (!initialized) {
			// Build set of elements-to-exclude from right argument
			excludeSet = makeSet(getRightArg());
			excludeSetList = excludeSet.toArray(new BindingSet[0]);
			excludeSetBindingNames = excludeSet.stream()
					.map(BindingSet::getBindingNames)
					.flatMap(Set::stream)
					.collect(Collectors.toSet());
			excludeSetBindingNamesAreAllTheSame = excludeSet.stream().allMatch(b -> {
				Set<String> bindingNames = b.getBindingNames();
				if (bindingNames.size() == excludeSetBindingNames.size()) {
					return bindingNames.containsAll(excludeSetBindingNames);
				}
				return false;
			});

			// Build right-side index by (name,value) -> list of rows
			HashMap<String, HashMap<Value, ArrayList<BindingSet>>> tmpIndex = new HashMap<>();
			for (BindingSet bs : excludeSetList) {
				for (String name : bs.getBindingNames()) {
					Value v = bs.getValue(name);
					if (v != null) {
						tmpIndex
								.computeIfAbsent(name, k -> new HashMap<>())
								.computeIfAbsent(v, k -> new ArrayList<>())
								.add(bs);
					}
				}
			}
			var built = new HashMap<String, Map<Value, BindingSet[]>>(tmpIndex.size() * 2);
			for (var e : tmpIndex.entrySet()) {
				var inner = new HashMap<Value, BindingSet[]>(e.getValue().size() * 2);
				for (var e2 : e.getValue().entrySet()) {
					inner.put(e2.getKey(), e2.getValue().toArray(new BindingSet[0]));
				}
				built.put(e.getKey(), inner);
			}
			this.rightIndex = built;

			initialized = true;
		}

		Set<String> bindingNames = bindingSet.getBindingNames();
		boolean hasSharedBindings = false;

		// Fast union check: if no variable is shared with the union of right variables, accept immediately
		if (!excludeSetBindingNames.isEmpty()) {
			final Set<String> left = bindingNames;
			final Set<String> rightUnion = excludeSetBindingNames;
			if (left.size() <= rightUnion.size()) {
				for (String name : left) {
					if (rightUnion.contains(name)) {
						hasSharedBindings = true;
						break;
					}
				}
			} else {
				for (String name : rightUnion) {
					if (left.contains(name)) {
						hasSharedBindings = true;
						break;
					}
				}
			}
		}

		if (!hasSharedBindings) {
			return true;
		}

		// Use right-side index to find only candidates that match on at least one shared (name,value)
		if (rightIndex != null && !rightIndex.isEmpty()) {
			for (String name : bindingNames) {
				Value leftVal = bindingSet.getValue(name);
				if (leftVal == null) {
					continue; // unbound on left does not participate in compatibility
				}
				Map<Value, BindingSet[]> byValue = rightIndex.get(name);
				if (byValue == null) {
					continue;
				}
				BindingSet[] candidates = byValue.get(leftVal);
				if (candidates == null) {
					continue;
				}
				for (int j = 0; j < candidates.length; j++) {
					BindingSet excluded = candidates[j];
					if (excluded.isCompatible(bindingSet)) {
						return false;
					}
				}
			}
			return true;
		}

		// Fallback: scan all (should be rare or small)
		for (BindingSet excluded : excludeSetList) {
			if (!excludeSetBindingNamesAreAllTheSame) {
				hasSharedBindings = false;
				final Set<String> excludedNames = excluded.getBindingNames();
				if (bindingNames.size() <= excludedNames.size()) {
					for (String name : bindingNames) {
						if (excludedNames.contains(name)) {
							hasSharedBindings = true;
							break;
						}
					}
				} else {
					for (String name : excludedNames) {
						if (bindingNames.contains(name)) {
							hasSharedBindings = true;
							break;
						}
					}
				}
			}
			if (hasSharedBindings && excluded.isCompatible(bindingSet)) {
				return false;
			}
		}

		return true;
	}

	protected Set<BindingSet> makeSet() {
		return new LinkedHashSet<>();
	}

	protected Set<String> makeSet(Set<String> set) {
		return new HashSet<>(set);
	}

	protected Set<BindingSet> makeSet(CloseableIteration<BindingSet> rightArg) {
		return Iterations.asSet(rightArg);
	}

	@Override
	protected void handleClose() {
		if (rightArg != null) {
			rightArg.close();
		}
	}

	/**
	 * @return Returns the rightArg.
	 */
	protected CloseableIteration<BindingSet> getRightArg() {
		return rightArg;
	}

	protected long clearExcludeSet() {
		int size = excludeSet.size();
		excludeSet.clear();
		return size;
	}
}
