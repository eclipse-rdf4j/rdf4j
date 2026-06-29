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
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.NativeTripleTermSource;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;

public class NativeTripleTermQueryEvaluationStep implements QueryEvaluationStep {
	private final Var extVar;
	private final Var predVar;
	private final Var objVar;
	private final Var subjVar;
	private final NativeTripleTermSource tripleSource;
	private final QueryEvaluationContext context;

	public NativeTripleTermQueryEvaluationStep(Var subjVar, Var predVar, Var objVar, Var extVar,
			NativeTripleTermSource tripleSource, QueryEvaluationContext context) {
		this.extVar = extVar;
		this.predVar = predVar;
		this.objVar = objVar;
		this.subjVar = subjVar;
		this.tripleSource = tripleSource;
		this.context = context;
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		final Value subjValue = StrictEvaluationStrategy.getVarValue(subjVar, bindings);
		final Value predValue = StrictEvaluationStrategy.getVarValue(predVar, bindings);
		final Value objValue = StrictEvaluationStrategy.getVarValue(objVar, bindings);
		final Value extValue = StrictEvaluationStrategy.getVarValue(extVar, bindings);

		// RDF 1.2 ReifiedTripleRef evaluation (extends TripleRef):
		// ReifiedTripleRef = TripleRef + reification statement lookup
		//
		// case1: when we have a binding for extVar (can be Resource reifier or TripleTerm value)
		// case2: when extVar is unbound - need to find all matching patterns
		//
		// This handles the complete ReifiedTriple pattern which includes:
		// - Base TripleRef functionality: Direct triple term lookup <<( s p o )>>
		// - Additional reification lookup: Find reifiers that reify the triple term
		// and their associated rdf:reifies statements
		//
		// Process:
		// 1. Look up triple terms matching the s/p/o pattern from tripleSource
		// 2. Filter based on bound variables (subjVar, predVar, objVar, extVar)
		// 3. For each matching triple term:
		// - Bind the component variables (s, p, o)
		// - Bind extVar to either the TripleTerm or associated reifier Resource
		// 4. Generate bindings for both:
		// - Direct triple term references
		// - Reification statements (reifier rdf:reifies tripleTerm)
		//
		// The extVar can be bound to:
		// - TripleTerm values: For direct triple term object/subject usage
		// - Resource reifiers: For reification statement patterns
		if (extValue != null && !(extValue instanceof Resource) && !(extValue instanceof TripleTerm)) {
			return EMPTY_ITERATION;
		}

		// in case the
		CloseableIteration<? extends TripleTerm> sourceIter = tripleSource
				.getTripleTerms((Resource) subjValue, (IRI) predValue, objValue);

		FilterIteration<TripleTerm> filterIter = new FilterIteration<>(
				sourceIter) {
			@Override
			protected boolean accept(TripleTerm triple) throws QueryEvaluationException {
				if (subjValue != null && !subjValue.equals(triple.getSubject())) {
					return false;
				}
				if (predValue != null && !predValue.equals(triple.getPredicate())) {
					return false;
				}
				if (objValue != null && !objValue.equals(triple.getObject())) {
					return false;
				}
				return extValue == null || extValue.equals(triple);
			}

			@Override
			protected void handleClose() {

			}
		};

		return new ConvertingIteration<>(filterIter) {
			@Override
			protected BindingSet convert(TripleTerm tripleTerm) throws QueryEvaluationException {
				MutableBindingSet result = context.createBindingSet(bindings);
				if (subjValue == null) {
					result.addBinding(subjVar.getName(), tripleTerm.getSubject());
				}
				if (predValue == null) {
					result.addBinding(predVar.getName(), tripleTerm.getPredicate());
				}
				if (objValue == null) {
					result.addBinding(objVar.getName(), tripleTerm.getObject());
				}
				// add the extVar binding if we do not have a value bound.
				if (extValue == null) {
					result.addBinding(extVar.getName(), tripleTerm);
				}
				return result;
			}
		};

	}
}
