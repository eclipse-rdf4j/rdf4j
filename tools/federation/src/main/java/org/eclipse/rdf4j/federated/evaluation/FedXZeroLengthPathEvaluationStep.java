/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.FedXZeroLengthPath;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.evaluation.iterator.FedXZeroLengthPathIteration;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.ZeroLengthPathEvaluationStep;

/**
 * An evaluation step used for {@link FedXZeroLengthPath}.
 *
 * @see ZeroLengthPathEvaluationStep
 */
public final class FedXZeroLengthPathEvaluationStep implements QueryEvaluationStep {
	private final Var subjectVar;
	private final Var objVar;
	private final Var contextVar;
	private final QueryValueEvaluationStep subPrep;
	private final QueryValueEvaluationStep objPrep;
	private final EvaluationStrategy strategy;
	private final QueryEvaluationContext context;

	private final Supplier<List<StatementSource>> statementSources;
	private final Supplier<QueryInfo> queryInfo;

	public FedXZeroLengthPathEvaluationStep(Var subjectVar, Var objVar, Var contextVar,
			QueryValueEvaluationStep subPrep,
			QueryValueEvaluationStep objPrep, EvaluationStrategy strategy, QueryEvaluationContext context,
			Supplier<List<StatementSource>> statementSources, Supplier<QueryInfo> queryInfo) {
		this.subjectVar = subjectVar;
		this.objVar = objVar;
		this.contextVar = contextVar;
		this.subPrep = subPrep;
		this.objPrep = objPrep;
		this.strategy = strategy;
		this.context = context;

		this.statementSources = statementSources;
		this.queryInfo = queryInfo;
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
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

	protected FedXZeroLengthPathIteration getZeroLengthPathIterator(final BindingSet bindings, final Var subjectVar,
			final Var objVar, final Var contextVar, Value subj, Value obj, QueryEvaluationContext context) {
		return new FedXZeroLengthPathIteration(strategy, subjectVar, objVar, subj, obj, contextVar, bindings, context,
				queryInfo.get(), statementSources.get());
	}
}
