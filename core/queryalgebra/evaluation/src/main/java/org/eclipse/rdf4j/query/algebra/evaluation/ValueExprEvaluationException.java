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
package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.ValueExpr;

/**
 * An exception indicating that a {@link ValueExpr} could not be evaluated due to illegal or incompatible values. When
 * thrown, the result of the evaluation should be considered to be "unknown".
 *
 * @author Arjohn Kampman
 */
public class ValueExprEvaluationException extends QueryEvaluationException {

	private static final long serialVersionUID = -3633440570594631529L;

	public ValueExprEvaluationException() {
		super();
	}

	public ValueExprEvaluationException(String message) {
		super(message);
	}

	public ValueExprEvaluationException(String message, Throwable t) {
		super(message, t);
	}

	public ValueExprEvaluationException(Throwable t) {
		super(t);
	}

	@Override
	public Throwable fillInStackTrace() {
		// Exception used for excessive flow control. Collecting the stack trace is a slow operation, so skip it.
		return this;
	}
}
