/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DualUnionIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;

public class UnionQueryEvaluationStep implements QueryEvaluationStep {

	private final QueryEvaluationStep leftQes;
	private final QueryEvaluationStep rightQes;

	public UnionQueryEvaluationStep(QueryEvaluationStep leftQes, QueryEvaluationStep rightQes) {
		this.leftQes = leftQes;
		this.rightQes = rightQes;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		CloseableIteration<BindingSet, QueryEvaluationException> evaluate = null;
		CloseableIteration<BindingSet, QueryEvaluationException> evaluate1 = null;

		try {
			evaluate = leftQes.evaluate(bindings);
			evaluate1 = rightQes.evaluate(bindings);

			if (evaluate instanceof EmptyIteration) {
				return evaluate1;
			} else if (evaluate1 instanceof EmptyIteration) {
				return evaluate;
			}

			return DualUnionIteration.getInstance(evaluate, evaluate1);
		} catch (Throwable t) {
			try {
				if (evaluate != null) {
					evaluate.close();
				}
			} finally {
				if (evaluate1 != null) {
					evaluate1.close();
				}
			}

			throw t;
		}

	}
}
