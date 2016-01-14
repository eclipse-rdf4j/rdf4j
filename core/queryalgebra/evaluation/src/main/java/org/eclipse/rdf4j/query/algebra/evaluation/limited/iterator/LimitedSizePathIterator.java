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

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SimpleEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.PathIteration;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
public class LimitedSizePathIterator extends PathIteration {

	private final AtomicLong used;
	private final long maxSize;

	/**
	 * @param evaluationStrategyImpl
	 * @param scope
	 * @param startVar
	 * @param pathExpression
	 * @param endVar
	 * @param contextVar
	 * @param minLength
	 * @param bindings
	 * @throws QueryEvaluationException
	 */
	public LimitedSizePathIterator(SimpleEvaluationStrategy evaluationStrategyImpl, Scope scope, Var startVar,
			TupleExpr pathExpression, Var endVar, Var contextVar, long minLength, BindingSet bindings,
			AtomicLong used, long maxSize)
		throws QueryEvaluationException
	{
		super(evaluationStrategyImpl, scope, startVar, pathExpression, endVar, contextVar, minLength, bindings);
		this.used = used;
		this.maxSize = maxSize;
	}

	@Override
	protected boolean add(Set<ValuePair> valueSet, ValuePair vp)
		throws QueryEvaluationException
	{
		return LimitedSizeIteratorUtil.<ValuePair>add(vp, valueSet, used, maxSize);
	}

}
