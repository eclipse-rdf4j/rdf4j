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
package org.eclipse.rdf4j.spin.function;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.BinaryFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.util.MathUtil;

public class Add extends BinaryFunction {

	@Override
	public String getURI() {
		return SP.ADD.toString();
	}

	@Override
	protected Value evaluate(ValueFactory valueFactory, Value arg1, Value arg2) throws ValueExprEvaluationException {
		if (arg1 instanceof Literal && arg2 instanceof Literal) {
			return MathUtil.compute((Literal) arg1, (Literal) arg2, MathOp.PLUS);
		}

		throw new ValueExprEvaluationException("Both arguments must be numeric literals");
	}
}
