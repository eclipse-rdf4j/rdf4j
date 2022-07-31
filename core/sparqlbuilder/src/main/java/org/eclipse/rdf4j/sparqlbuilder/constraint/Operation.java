/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.constraint;

/**
 * A SPARQL Operation. Differs from a {@link Function} in the way operators and arguments are printed.
 *
 * @param <T> The type of operation. Used to support fluency.
 */
abstract class Operation<T extends Operation<T>> extends Expression<T> {
	protected int operandLimit;

	Operation(SparqlOperator operator) {
		this(operator, -1);
	}

	Operation(SparqlOperator operator, int operandLimit) {
		super(operator);
		this.operandLimit = operandLimit;
	}

	@SuppressWarnings("unchecked")
	T addOperand(Operand operand) /* throws Exception */ {
		if (isBelowOperatorLimit()) {
			return super.addOperand(operand);
		}
		// TODO: throw exception for out of bounds?
		// throw new Exception();
		return (T) this;
	}

	@Override
	public String getQueryString() {
		return isAtOperatorLimit() ? super.getQueryString() : "";
	}

	protected boolean isBelowOperatorLimit() {
		return operandLimit < 0 || elements.size() < operandLimit;
	}

	protected boolean isAtOperatorLimit() {
		return operandLimit < 0 || elements.size() == operandLimit;
	}
}
