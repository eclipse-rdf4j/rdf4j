/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.numeric;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * The SPARQL built-in {@link Function} CEIL, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-ceil">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class Ceil implements Function {

	@Override
	public String getURI() {
		return FN.NUMERIC_CEIL.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("CEIL requires exactly 1 argument, got " + args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal) args[0];

			IRI datatype = literal.getDatatype();

			// CEIL function accepts only numeric literals
			if (datatype != null && XMLDatatypeUtil.isNumericDatatype(datatype)) {
				if (XMLDatatypeUtil.isIntegerDatatype(datatype)) {
					return literal;
				} else if (XMLDatatypeUtil.isDecimalDatatype(datatype)) {
					BigDecimal ceilingValue = literal.decimalValue().setScale(0, RoundingMode.CEILING);
					return valueFactory.createLiteral(ceilingValue.toPlainString(), datatype);
				} else if (XMLDatatypeUtil.isFloatingPointDatatype(datatype)) {
					double ceilingValue = Math.ceil(literal.doubleValue());
					return valueFactory.createLiteral(Double.toString(ceilingValue), datatype);
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
