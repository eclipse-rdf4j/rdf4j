/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.limited.iterator;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SimpleEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ZeroLengthPathIteration;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
public class LimitedSizeZeroLengthPathIteration extends ZeroLengthPathIteration {

	private final AtomicLong used;

	private final long maxSize;

	/**
	 * @param evaluationStrategyImpl
	 * @param subjectVar
	 * @param objVar
	 * @param subj
	 * @param obj
	 * @param contextVar
	 * @param bindings
	 */
	public LimitedSizeZeroLengthPathIteration(SimpleEvaluationStrategy evaluationStrategyImpl, Var subjectVar,
			Var objVar, Value subj, Value obj, Var contextVar, BindingSet bindings, AtomicLong used, long maxSize)
	{
		super(evaluationStrategyImpl, subjectVar, objVar, subj, obj, contextVar, bindings);
		this.used = used;
		this.maxSize = maxSize;
	}

	@Override
	protected boolean add(Set<Value> reportedValues2, Value v)
		throws QueryEvaluationException
	{
		return LimitedSizeIteratorUtil.<Value>add(v, reportedValues2, used, maxSize);
	}

}
