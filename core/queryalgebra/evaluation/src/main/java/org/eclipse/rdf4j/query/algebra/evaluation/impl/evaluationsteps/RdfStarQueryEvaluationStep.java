/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.RDFStarTripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;

public class RdfStarQueryEvaluationStep implements QueryEvaluationStep {
	private final Var extVar;
	private final Var predVar;
	private final Var objVar;
	private final Var subjVar;
	private final RDFStarTripleSource tripleSource;
	private final QueryEvaluationContext context;

	public RdfStarQueryEvaluationStep(Var subjVar, Var predVar, Var objVar, Var extVar,
			RDFStarTripleSource tripleSource, QueryEvaluationContext context) {
		this.extVar = extVar;
		this.predVar = predVar;
		this.objVar = objVar;
		this.subjVar = subjVar;
		this.tripleSource = tripleSource;
		this.context = context;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		final Value subjValue = StrictEvaluationStrategy.getVarValue(subjVar, bindings);
		final Value predValue = StrictEvaluationStrategy.getVarValue(predVar, bindings);
		final Value objValue = StrictEvaluationStrategy.getVarValue(objVar, bindings);
		final Value extValue = StrictEvaluationStrategy.getVarValue(extVar, bindings);

		// case1: when we have a binding for extVar we use it in the reified nodes lookup
		// case2: in which we have unbound extVar
		// in both cases:
		// 1. iterate over all statements matching ((* | extValue), rdf:type, rdf:Statement)
		// 2. construct a look ahead iteration and filter these solutions that do not match the
		// bindings for the subject, predicate and object vars (if these are bound)
		// return set of solution where the values of the statements (extVar, rdf:subject/predicate/object,
		// value)
		// are bound to the variables of the respective TripleRef variables for subject, predicate, object
		// NOTE: if the tripleSource is extended to allow for lookup over asserted Triple values in the
		// underlying sail
		// the evaluation of the TripleRef should be suitably forwarded down the sail and filter/construct
		// the correct solution out of the results of that call
		if (extValue != null && !(extValue instanceof Resource)) {
			return EMPTY_ITERATION;
		}

		// in case the
		CloseableIteration<? extends Triple, QueryEvaluationException> sourceIter = tripleSource
				.getRdfStarTriples((Resource) subjValue, (IRI) predValue, objValue);

		FilterIteration<Triple, QueryEvaluationException> filterIter = new FilterIteration<Triple, QueryEvaluationException>(
				sourceIter) {
			@Override
			protected boolean accept(Triple triple) throws QueryEvaluationException {
				if (subjValue != null && !subjValue.equals(triple.getSubject())) {
					return false;
				}
				if (predValue != null && !predValue.equals(triple.getPredicate())) {
					return false;
				}
				if (objValue != null && !objValue.equals(triple.getObject())) {
					return false;
				}
				if (extValue != null && !extValue.equals(triple)) {
					return false;
				}
				return true;
			}
		};

		return new ConvertingIteration<>(filterIter) {
			@Override
			protected BindingSet convert(Triple triple) throws QueryEvaluationException {
				MutableBindingSet result = context.createBindingSet(bindings);
				if (subjValue == null) {
					result.addBinding(subjVar.getName(), triple.getSubject());
				}
				if (predValue == null) {
					result.addBinding(predVar.getName(), triple.getPredicate());
				}
				if (objValue == null) {
					result.addBinding(objVar.getName(), triple.getObject());
				}
				// add the extVar binding if we do not have a value bound.
				if (extValue == null) {
					result.addBinding(extVar.getName(), triple);
				}
				return result;
			}
		};

	}
}
