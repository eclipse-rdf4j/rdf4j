/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.BinaryFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

public class And extends BinaryFunction {

	@Override
	public String getURI() {
		return SP.AND.toString();
	}

	@Override
	protected Value evaluate(ValueFactory valueFactory, Value arg1, Value arg2) throws ValueExprEvaluationException {
		return BooleanLiteral.valueOf(QueryEvaluationUtil.getEffectiveBooleanValue(arg1)
				&& QueryEvaluationUtil.getEffectiveBooleanValue(arg2));
	}
}
