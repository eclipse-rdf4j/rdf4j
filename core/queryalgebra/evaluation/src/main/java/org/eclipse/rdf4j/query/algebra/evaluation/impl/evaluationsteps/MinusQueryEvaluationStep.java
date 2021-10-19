/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.SPARQLMinusIteration;

public class MinusQueryEvaluationStep implements QueryEvaluationStep {
	private final QueryEvaluationStep leftQes;
	private final QueryEvaluationStep rightQes;
	private final Supplier<Set<BindingSet>> setMaker;

	public MinusQueryEvaluationStep(QueryEvaluationStep leftQes, QueryEvaluationStep rightQes,
			Supplier<Set<BindingSet>> setMaker) {
		this.setMaker = setMaker;
		this.leftQes = bs -> new QueryEvaluationStep.DelayedEvaluationIteration(leftQes, bs);
		this.rightQes = bs -> new QueryEvaluationStep.DelayedEvaluationIteration(rightQes, bs);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		return new SPARQLMinusIteration<>(leftQes.evaluate(bindings), rightQes.evaluate(bindings), setMaker);
	}
}