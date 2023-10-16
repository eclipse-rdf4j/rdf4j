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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;

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

			initialized = true;
		}

		Set<String> bindingNames = bindingSet.getBindingNames();
		boolean hasSharedBindings = false;

		if (excludeSetBindingNamesAreAllTheSame) {
			for (String bindingName : excludeSetBindingNames) {
				if (bindingNames.contains(bindingName)) {
					hasSharedBindings = true;
					break;
				}
			}

			if (!hasSharedBindings) {
				return true;
			}
		}

		for (BindingSet excluded : excludeSet) {

			if (!excludeSetBindingNamesAreAllTheSame) {
				hasSharedBindings = false;
				for (String bindingName : excluded.getBindingNames()) {
					if (bindingNames.contains(bindingName)) {
						hasSharedBindings = true;
						break;
					}
				}

			}

			// two bindingsets that share no variables are compatible by
			// definition, however, the formal
			// definition of SPARQL MINUS indicates that such disjoint sets should
			// be filtered out.
			// See http://www.w3.org/TR/sparql11-query/#sparqlAlgebra
			if (hasSharedBindings) {
				if (QueryResults.bindingSetsCompatible(excluded, bindingSet)) {
					// at least one compatible bindingset has been found in the
					// exclude set, therefore the object is compatible, therefore it
					// should not be accepted.
					return false;
				}
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
