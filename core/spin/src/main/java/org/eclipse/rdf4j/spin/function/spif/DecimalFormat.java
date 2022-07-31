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

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.BinaryFunction;

public class DecimalFormat extends BinaryFunction {

	@Override
	public String getURI() {
		return SPIF.DECIMAL_FORMAT_FUNCTION.toString();
	}

	@Override
	protected Value evaluate(ValueFactory valueFactory, Value arg1, Value arg2) throws ValueExprEvaluationException {
		if (!(arg1 instanceof Literal) || !(arg2 instanceof Literal)) {
			throw new ValueExprEvaluationException("Both arguments must be literals");
		}
		Literal number = (Literal) arg1;
		Literal format = (Literal) arg2;

		java.text.DecimalFormat formatter = new java.text.DecimalFormat(format.getLabel());
		String value;
		if (XSD.INT.equals(number.getDatatype()) || XSD.LONG.equals(number.getDatatype())
				|| XSD.SHORT.equals(number.getDatatype()) || XSD.BYTE.equals(number.getDatatype())) {
			value = formatter.format(number.longValue());
		} else if (XSD.DECIMAL.equals(number.getDatatype())) {
			value = formatter.format(number.decimalValue());
		} else if (XSD.INTEGER.equals(number.getDatatype())) {
			value = formatter.format(number.integerValue());
		} else {
			value = formatter.format(number.doubleValue());
		}
		return valueFactory.createLiteral(value);
	}
}
