/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Base class for any join parallel join executor. Note that this class extends {@link LookAheadIteration} and thus any
 * implementation of this class is applicable for pipelining when used in a different thread (access to shared variables
 * is synchronized).
 *
 * @author Andreas Schwarte
 * @deprecated since 2.3 use {@link org.eclipse.rdf4j.repository.sparql.federation.JoinExecutorBase}
 */
@Deprecated
public abstract class JoinExecutorBase<T> extends org.eclipse.rdf4j.repository.sparql.federation.JoinExecutorBase<T> {

	public JoinExecutorBase(CloseableIteration<T, QueryEvaluationException> leftIter, TupleExpr rightArg,
			BindingSet bindings) throws QueryEvaluationException {
		super(leftIter, rightArg, bindings);
	}
}
