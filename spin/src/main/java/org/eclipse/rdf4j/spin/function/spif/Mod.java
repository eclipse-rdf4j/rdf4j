/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function.spif;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.BinaryFunction;

public class Mod extends BinaryFunction {

	@Override
	public String getURI() {
		return SPIF.MOD_FUNCTION.toString();
	}

	@Override
	protected Value evaluate(ValueFactory valueFactory, Value arg1, Value arg2)
		throws ValueExprEvaluationException
	{
		if (!(arg1 instanceof Literal) || !(arg2 instanceof Literal)) {
			throw new ValueExprEvaluationException("Both arguments must be numeric literals");
		}
		Literal leftLit = (Literal)arg1;
		Literal rightLit = (Literal)arg2;
		IRI leftDatatype = leftLit.getDatatype();
		IRI rightDatatype = rightLit.getDatatype();

		// Only numeric value can be used in math expressions
		if (!XMLDatatypeUtil.isNumericDatatype(leftDatatype)) {
			throw new ValueExprEvaluationException("Not a number: " + leftLit);
		}
		if (!XMLDatatypeUtil.isNumericDatatype(rightDatatype)) {
			throw new ValueExprEvaluationException("Not a number: " + rightLit);
		}

		// Determine most specific datatype that the arguments have in common,
		// choosing from xsd:integer, xsd:decimal, xsd:float and xsd:double as
		// per the SPARQL/XPATH spec
		IRI commonDatatype;

		if (leftDatatype.equals(XMLSchema.DOUBLE) || rightDatatype.equals(XMLSchema.DOUBLE)) {
			commonDatatype = XMLSchema.DOUBLE;
		}
		else if (leftDatatype.equals(XMLSchema.FLOAT) || rightDatatype.equals(XMLSchema.FLOAT)) {
			commonDatatype = XMLSchema.FLOAT;
		}
		else if (leftDatatype.equals(XMLSchema.DECIMAL) || rightDatatype.equals(XMLSchema.DECIMAL)) {
			commonDatatype = XMLSchema.DECIMAL;
		}
		else {
			commonDatatype = XMLSchema.INTEGER;
		}
		if (XMLSchema.DOUBLE.equals(commonDatatype)) {
			double left = leftLit.doubleValue();
			double right = rightLit.doubleValue();
			return valueFactory.createLiteral(left % right);
		}
		else if (XMLSchema.FLOAT.equals(commonDatatype)) {
			float left = leftLit.floatValue();
			float right = rightLit.floatValue();
			return valueFactory.createLiteral(left % right);
		}
		else if (XMLSchema.DECIMAL.equals(commonDatatype)) {
			BigDecimal left = leftLit.decimalValue();
			BigDecimal right = rightLit.decimalValue();
			return valueFactory.createLiteral(left.remainder(right, MathContext.UNLIMITED));
		}
		else {
			BigInteger left = leftLit.integerValue();
			BigInteger right = rightLit.integerValue();
			return valueFactory.createLiteral(left.remainder(right));
		}
	}
}
