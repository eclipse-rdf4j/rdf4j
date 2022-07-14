/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ZeroLengthPathIteration;

public final class ZeroLengthPathEvaluationStep implements QueryEvaluationStep {
	private final Var subjectVar;
	private final Var objVar;
	private final Var contextVar;
	private final QueryValueEvaluationStep subPrep;
	private final QueryValueEvaluationStep objPrep;
	private final EvaluationStrategy strategy;
	private final QueryEvaluationContext context;

	public ZeroLengthPathEvaluationStep(Var subjectVar, Var objVar, Var contextVar, QueryValueEvaluationStep subPrep,
			QueryValueEvaluationStep objPrep, EvaluationStrategy strategy, QueryEvaluationContext context) {
		this.subjectVar = subjectVar;
		this.objVar = objVar;
		this.contextVar = contextVar;
		this.subPrep = subPrep;
		this.objPrep = objPrep;
		this.strategy = strategy;
		this.context = context;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		Value subj = null;
		try {
			subj = subPrep.evaluate(bindings);
		} catch (QueryEvaluationException ignored) {
		}

		Value obj = null;
		try {
			obj = objPrep.evaluate(bindings);
		} catch (QueryEvaluationException ignored) {
		}

		if (subj != null && obj != null) {
			if (!subj.equals(obj)) {
				return EMPTY_ITERATION;
			}
		}
		return getZeroLengthPathIterator(bindings, subjectVar, objVar, contextVar, subj, obj, context);
	}

	protected ZeroLengthPathIteration getZeroLengthPathIterator(final BindingSet bindings, final Var subjectVar,
			final Var objVar, final Var contextVar, Value subj, Value obj, QueryEvaluationContext context) {
		return new ZeroLengthPathIteration(strategy, subjectVar, objVar, subj, obj, contextVar, bindings, context);
	}
}
