/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * @author Arjohn Kampman
 */
public class BadlyDesignedLeftJoinIterator extends LeftJoinIterator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final BindingSet inputBindings;

	private final Set<String> problemVars;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public BadlyDesignedLeftJoinIterator(EvaluationStrategy strategy, LeftJoin join, BindingSet inputBindings,
			Set<String> problemVars) throws QueryEvaluationException {
		super(strategy, join, getFilteredBindings(inputBindings, problemVars));
		this.inputBindings = inputBindings;
		this.problemVars = problemVars;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		BindingSet result = super.getNextElement();

		// Ignore all results that are not compatible with the input bindings
		while (result != null && !QueryResults.bindingSetsCompatible(inputBindings, result)) {
			result = super.getNextElement();
		}

		if (result != null) {
			// Make sure the provided problemVars are part of the returned results
			// (necessary in case of e.g. LeftJoin and Union arguments)
			QueryBindingSet extendedResult = null;

			for (String problemVar : problemVars) {
				if (!result.hasBinding(problemVar)) {
					if (extendedResult == null) {
						extendedResult = new QueryBindingSet(result);
					}
					extendedResult.addBinding(problemVar, inputBindings.getValue(problemVar));
				}
			}

			if (extendedResult != null) {
				result = extendedResult;
			}
		}

		return result;
	}

	/*--------------------*
	 * Static util method *
	 *--------------------*/

	private static QueryBindingSet getFilteredBindings(BindingSet bindings, Set<String> problemVars) {
		QueryBindingSet filteredBindings = new QueryBindingSet(bindings);
		filteredBindings.removeAll(problemVars);
		return filteredBindings;
	}
}
