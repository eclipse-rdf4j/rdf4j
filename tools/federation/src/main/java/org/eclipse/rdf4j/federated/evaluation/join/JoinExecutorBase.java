/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutorBase;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Base class for any join parallel join executor.
 *
 * Note that this class extends {@link LookAheadIteration} and thus any implementation of this class is applicable for
 * pipelining when used in a different thread (access to shared variables is synchronized).
 *
 * @author Andreas Schwarte
 */
public abstract class JoinExecutorBase<T> extends ParallelExecutorBase<T> {

	/* Constants */
	protected final TupleExpr rightArg; // the right argument for the join
	protected final BindingSet bindings; // the bindings

	/* Variables */
	protected Set<String> joinVars; // might be unknown (i.e. null for some implementations)
	protected CloseableIteration<T, QueryEvaluationException> leftIter;

	public JoinExecutorBase(FederationEvalStrategy strategy, CloseableIteration<T, QueryEvaluationException> leftIter,
			TupleExpr rightArg,
			BindingSet bindings, QueryInfo queryInfo) throws QueryEvaluationException {
		super(queryInfo);
		this.leftIter = leftIter;
		this.rightArg = rightArg;
		this.bindings = bindings;
	}

	@Override
	protected final void performExecution() throws Exception {

		handleBindings();

	}

	/**
	 * Implementations must implement this method to handle bindings.
	 *
	 * Use the following as a template <code>
	 * while (!closed && leftIter.hasNext()) {
	 * 		// your code
	 * }
	 * </code>
	 *
	 * and add results to rightQueue. Note that addResult() is implemented synchronized and thus thread safe. In case
	 * you can guarantee sequential access, it is also possible to directly access rightQueue
	 *
	 *
	 * Note that the implementation must block until the entire join is executed.
	 */
	protected abstract void handleBindings() throws Exception;

	@Override
	public void handleClose() throws QueryEvaluationException {

		try {
			super.handleClose();
		} finally {
			leftIter.close();
		}
	}

	@Override
	protected String getExecutorType() {
		return "Join";
	}

	/**
	 * @return the join variables, might be <code>null</code> if unknown in the concrete implementation
	 */
	public Set<String> getJoinVars() {
		return joinVars;
	}

	/**
	 * Set the join variables
	 *
	 * @param joinVars the join variables
	 */
	public void setJoinVars(Set<String> joinVars) {
		this.joinVars = joinVars;
	}
}
