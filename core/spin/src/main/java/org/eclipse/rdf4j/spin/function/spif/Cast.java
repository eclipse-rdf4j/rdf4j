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
package org.eclipse.rdf4j.spin.function.spif;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.BinaryFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;

public class Cast extends BinaryFunction {

	@Override
	public String getURI() {
		return SPIF.CAST_FUNCTION.toString();
	}

	@Override
	protected Value evaluate(ValueFactory valueFactory, Value arg1, Value arg2) throws ValueExprEvaluationException {
		if (!(arg1 instanceof Literal)) {
			throw new ValueExprEvaluationException("First argument must be a literal");
		}
		if (!(arg2 instanceof IRI)) {
			throw new ValueExprEvaluationException("Second argument must be a datatype");
		}
		Literal value = (Literal) arg1;
		IRI targetDatatype = (IRI) arg2;
		Function func = FunctionRegistry.getInstance().get(targetDatatype.stringValue()).orElse(null);
		return (func != null) ? func.evaluate(valueFactory, value)
				: valueFactory.createLiteral(value.getLabel(), targetDatatype);
	}
}
