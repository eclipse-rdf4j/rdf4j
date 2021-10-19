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
import org.eclipse.rdf4j.common.iteration.IntersectIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;

/**
 * A step that prepares the arguments of an Intersection operator before execution.
 */
public class IntersectionQueryEvaluationStep implements QueryEvaluationStep {

	private final QueryEvaluationStep leftArgDelayed;
	private final QueryEvaluationStep rightArgDelayed;
	private final Supplier<Set<BindingSet>> setMaker;

	public IntersectionQueryEvaluationStep(QueryEvaluationStep leftArg, QueryEvaluationStep rightArg,
			Supplier<Set<BindingSet>> setMaker) {
		this.setMaker = setMaker;
		leftArgDelayed = bs -> new QueryEvaluationStep.DelayedEvaluationIteration(leftArg, bs);
		rightArgDelayed = bs -> new QueryEvaluationStep.DelayedEvaluationIteration(rightArg, bs);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
		return new IntersectIteration<>(leftArgDelayed.evaluate(bs), rightArgDelayed.evaluate(bs), setMaker);
	}
}