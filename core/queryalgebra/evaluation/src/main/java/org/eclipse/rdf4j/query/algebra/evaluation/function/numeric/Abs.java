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
package org.eclipse.rdf4j.query.algebra.evaluation.function.numeric;

import java.math.BigDecimal;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * The SPARQL built-in {@link Function} ABS, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-abs">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class Abs implements Function {

	@Override
	public String getURI() {
		return FN.NUMERIC_ABS.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("ABS requires exactly 1 argument, got " + args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal) args[0];

			CoreDatatype.XSD datatype = literal.getCoreDatatype().asXSDDatatypeOrNull();

			// ABS function accepts only numeric literals
			if (datatype != null && datatype.isNumericDatatype()) {
				if (datatype.isDecimalDatatype()) {
					BigDecimal absoluteValue = literal.decimalValue().abs();

					return valueFactory.createLiteral(absoluteValue.toPlainString(), datatype);
				} else if (datatype.isFloatingPointDatatype()) {
					double absoluteValue = Math.abs(literal.doubleValue());
					return valueFactory.createLiteral(Double.toString(absoluteValue), datatype);
				} else {
					throw new ValueExprEvaluationException("unexpected datatype for function operand: " + args[0]);
				}
			} else {
				throw new ValueExprEvaluationException("unexpected input value for function: " + args[0]);
			}
		} else {
			throw new ValueExprEvaluationException("unexpected input value for function: " + args[0]);
		}

	}

}
