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
 * An Iteration that returns the results of an Iteration (the left argument)
 * MINUS any results that are compatible with results of another Iteration (the
 * right argument) or that have no shared variables. This iteration uses the
 * formal definition of the SPARQL 1.1 MINUS operator to determine which
 * BindingSets to return.
 * 
 * @see <a href="http://www.w3.org/TR/sparql11-query/#sparqlAlgebra">SPARQL
 *      Algebra Documentation</a>
 * @author Jeen
 */
public class SPARQLMinusIteration<X extends Exception> extends FilterIteration<BindingSet, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Iteration<BindingSet, X> rightArg;

	private final boolean distinct;

	private boolean initialized;

	private Set<BindingSet> excludeSet;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MinusIteration that returns the results of the left argument
	 * minus the results of the right argument. By default, duplicates are
	 * <em>not</em> filtered from the results.
	 * 
	 * @param leftArg
	 *        An Iteration containing the main set of elements.
	 * @param rightArg
	 *        An Iteration containing the set of elements that should be filtered
	 *        from the main set.
	 */
	public SPARQLMinusIteration(Iteration<BindingSet, X> leftArg, Iteration<BindingSet, X> rightArg) {
		this(leftArg, rightArg, false);
	}

	/**
	 * Creates a new MinusIteration that returns the results of the left argument
	 * minus the results of the right argument.
	 * 
	 * @param leftArg
	 *        An Iteration containing the main set of elements.
	 * @param rightArg
	 *        An Iteration containing the set of elements that should be filtered
	 *        from the main set.
	 * @param distinct
	 *        Flag indicating whether duplicate elements should be filtered from
	 *        the result.
	 */
	public SPARQLMinusIteration(Iteration<BindingSet, X> leftArg, Iteration<BindingSet, X> rightArg,
			boolean distinct)
	{
		super(leftArg);

		assert rightArg != null;

		this.rightArg = rightArg;
		this.distinct = distinct;
		this.initialized = false;
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	// implements LookAheadIteration.getNextElement()
	protected boolean accept(BindingSet object)
		throws X
	{
		if (!initialized) {
			// Build set of elements-to-exclude from right argument
			excludeSet = makeSet(getRightArg());
			initialized = true;
		}

		boolean compatible = false;

		for (BindingSet excluded : excludeSet) {

			// build set of shared variable names
			Set<String> sharedBindingNames = makeSet(excluded.getBindingNames());
			sharedBindingNames.retainAll(object.getBindingNames());

			// two bindingsets that share no variables are compatible by
			// definition, however, the formal
			// definition of SPARQL MINUS indicates that such disjoint sets should
			// be filtered out.
			// See http://www.w3.org/TR/sparql11-query/#sparqlAlgebra
			if (!sharedBindingNames.isEmpty()) {
				if (QueryResults.bindingSetsCompatible(excluded, object)) {
					// at least one compatible bindingset has been found in the
					// exclude set, therefore the object is compatible, therefore it
					// should not be accepted.
					compatible = true;
					break;
				}
			}
		}

		return !compatible;
	}

	protected Set<BindingSet> makeSet()
		throws X
	{
		return new LinkedHashSet<BindingSet>();
	}

	protected Set<String> makeSet(Set<String> set)
		throws X
	{
		return new HashSet<String>(set);
	}

	protected Set<BindingSet> makeSet(Iteration<BindingSet, X> rightArg2)
		throws X
	{
		return Iterations.addAll(rightArg, makeSet());
	}

	@Override
	protected void handleClose()
		throws X
	{
		super.handleClose();
		Iterations.closeCloseable(getRightArg());
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
