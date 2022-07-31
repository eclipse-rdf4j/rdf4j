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
package org.eclipse.rdf4j.federated.algebra;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Interface for any expression that can be evaluated
 *
 * @author Andreas Schwarte
 *
 * @see StatementSourcePattern
 * @see ExclusiveStatement
 * @see ExclusiveGroup
 */
public interface StatementTupleExpr extends FedXTupleExpr {

	/**
	 * @return the id of this expr
	 */
	String getId();

	/**
	 * @return a list of sources that are relevant for evaluation of this expression
	 */
	List<StatementSource> getStatementSources();

	/**
	 * returns true iff this statement has free variables in the presence of the specified binding set
	 *
	 * @param binding
	 * @return whether the statement has free vars
	 */
	boolean hasFreeVarsFor(BindingSet binding);

	/**
	 * Evaluate this expression using the provided bindings
	 *
	 * @param bindings
	 * @return the result iteration
	 *
	 * @throws QueryEvaluationException
	 */
	CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings)
			throws QueryEvaluationException;

}
