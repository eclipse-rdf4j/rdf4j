/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.SPARQLMinusIteration;

public class MinusQueryEvaluationStep implements QueryEvaluationStep {
	private final QueryEvaluationStep leftQes;
	private final Function<BindingSet, DelayedEvaluationIteration> rightQes;

	public MinusQueryEvaluationStep(QueryEvaluationStep leftQes, QueryEvaluationStep rightQes) {
		this.leftQes = leftQes;
		this.rightQes = bs -> new DelayedEvaluationIteration(rightQes, bs);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		return new SPARQLMinusIteration<>(leftQes.evaluate(bindings), rightQes.apply(bindings));
	}
}
