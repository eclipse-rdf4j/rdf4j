/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;

/**
 * An Iteration that returns the results of an Iteration (the left argument) MINUS any results that are compatible with
 * results of another Iteration (the right argument) or that have no shared variables. This iteration uses the formal
 * definition of the SPARQL 1.1 MINUS operator to determine which BindingSets to return.
 *
 * @see <a href="http://www.w3.org/TR/sparql11-query/#sparqlAlgebra">SPARQL Algebra Documentation</a>
 * @author Jeen
 */
@Deprecated(since = "4.1.0")
public class SPARQLMinusIteration<X extends Exception> extends FilterIteration<BindingSet, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Iteration<BindingSet, X> rightArg;

	private boolean initialized;

	private Set<BindingSet> excludeSet;

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
	public SPARQLMinusIteration(Iteration<BindingSet, X> leftArg, Iteration<BindingSet, X> rightArg) {
		super(leftArg);

		assert rightArg != null;

		this.rightArg = rightArg;
		this.initialized = false;
	}

	/**
	 * Creates a new MinusIteration that returns the results of the left argument minus the results of the right
	 * argument.
	 *
	 * @param leftArg  An Iteration containing the main set of elements.
	 * @param rightArg An Iteration containing the set of elements that should be filtered from the main set.
	 * @param distinct This argument is ignored
	 */
	@Deprecated(since = "4.0.0", forRemoval = true)
	public SPARQLMinusIteration(Iteration<BindingSet, X> leftArg, Iteration<BindingSet, X> rightArg, boolean distinct) {
		this(leftArg, rightArg);
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	// implements LookAheadIteration.getNextElement()
	@Override
	protected boolean accept(BindingSet bindingSet) throws X {
		if (!initialized) {
			// Build set of elements-to-exclude from right argument
			excludeSet = makeSet(getRightArg());
			initialized = true;
		}

		for (BindingSet excluded : excludeSet) {
			Set<String> bindingNames = bindingSet.getBindingNames();
			boolean hasSharedBindings = false;

			for (String bindingName : excluded.getBindingNames()) {
				if (bindingNames.contains(bindingName)) {
					hasSharedBindings = true;
					break;
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

	protected Set<BindingSet> makeSet() throws X {
		return new LinkedHashSet<>();
	}

	protected Set<String> makeSet(Set<String> set) throws X {
		return new HashSet<>(set);
	}

	protected Set<BindingSet> makeSet(Iteration<BindingSet, X> rightArg) throws X {
		return Iterations.asSet(rightArg);
	}

	@Override
	protected void handleClose() throws X {
		try {
			super.handleClose();
		} finally {
			Iterations.closeCloseable(getRightArg());
		}
	}

	/**
	 * @return Returns the rightArg.
	 */
	protected Iteration<BindingSet, X> getRightArg() {
		return rightArg;
	}

	protected long clearExcludeSet() {
		int size = excludeSet.size();
		excludeSet.clear();
		return size;
	}
}
