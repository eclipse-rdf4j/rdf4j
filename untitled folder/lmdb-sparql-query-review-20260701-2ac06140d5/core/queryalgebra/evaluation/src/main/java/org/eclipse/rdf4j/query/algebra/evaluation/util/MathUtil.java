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
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * A utility class for evaluation of mathematical expressions on RDF literals.
 *
 * @author Jeen Broekstra
 */
public class MathUtil {

	/**
	 * The default expansion scale used in division operations resulting in a decimal value with non-terminating decimal
	 * expansion. The OpenRDF default is 24 digits, a value used in various other SPARQL implementations, to make
	 * comparison between these systems easy.
	 */
	public static final int DEFAULT_DECIMAL_EXPANSION_SCALE = 24;

	private static int decimalExpansionScale = DEFAULT_DECIMAL_EXPANSION_SCALE;

	public static Literal compute(Literal leftLit, Literal rightLit, MathOp op) throws ValueExprEvaluationException {
		final ValueFactory vf = SimpleValueFactory.getInstance();
		return compute(leftLit, rightLit, op, vf);
	}

	/**
	 * Computes the result of applying the supplied math operator on the supplied left and right operand.
	 *
	 * @param leftLit  a numeric datatype literal
	 * @param rightLit a numeric datatype literal
	 * @param op       a mathematical operator, as definied by MathExpr.MathOp.
	 * @param vf       a ValueFactory used to create the result
	 * @return a numeric datatype literal containing the result of the operation. The result will be datatype according
	 *         to the most specific data type the two operands have in common per the SPARQL/XPath spec.
	 * @throws ValueExprEvaluationException
	 */
	public static Literal compute(Literal leftLit, Literal rightLit, MathOp op, ValueFactory vf)
			throws ValueExprEvaluationException {

		CoreDatatype leftDatatype = leftLit.getCoreDatatype();
		CoreDatatype rightDatatype = rightLit.getCoreDatatype();

		CoreDatatype.XSD leftDatatypeXSD = validateNumericArgument(leftLit, leftDatatype);
		CoreDatatype.XSD rightDatatypeXSD = validateNumericArgument(rightLit, rightDatatype);

		CoreDatatype.XSD commonDatatype = determineCommonDatatype(op, leftDatatypeXSD, rightDatatypeXSD);

		// Note: Java already handles cases like divide-by-zero appropriately
		// for floats and doubles, see:
		// http://www.particle.kth.se/~lindsey/JavaCourse/Book/Part1/Tech/
		// Chapter02/floatingPt2.html

		try {
			switch (commonDatatype) {
			case DOUBLE:
				return computeForXsdDouble(leftLit, rightLit, op, vf);
			case FLOAT:
				return computeForXsdFloat(leftLit, rightLit, op, vf);
			case DECIMAL:
				return computeForXsdDecimal(leftLit, rightLit, op, vf);
			default:
				return computeForXsdInteger(leftLit, rightLit, op, vf);
			}
		} catch (NumberFormatException | ArithmeticException e) {
			throw new ValueExprEvaluationException(e);
		}
	}

	private static CoreDatatype.XSD validateNumericArgument(Literal lit, CoreDatatype datatype) {
		// Only numeric value can be used in math expressions
		if (!datatype.isXSDDatatype()) {
			throw new ValueExprEvaluationException("Not a number: " + lit);
		}
		CoreDatatype.XSD leftDatatypeXSD = (CoreDatatype.XSD) datatype;
		if (!leftDatatypeXSD.isNumericDatatype()) {
			throw new ValueExprEvaluationException("Not a number: " + lit);
		}
		return leftDatatypeXSD;
	}

	private static Literal computeForXsdInteger(Literal leftLit, Literal rightLit, MathOp op, ValueFactory vf) {
		BigInteger left = leftLit.integerValue();
		BigInteger right = rightLit.integerValue();

		switch (op) {
		case PLUS:
			return vf.createLiteral(left.add(right));
		case MINUS:
			return vf.createLiteral(left.subtract(right));
		case MULTIPLY:
			return vf.createLiteral(left.multiply(right));
		case DIVIDE:
			throw new RuntimeException("Integer divisions should be processed as decimal divisions");
		default:
			throw new IllegalArgumentException("Unknown operator: " + op);
		}
	}

	private static Literal computeForXsdDecimal(Literal leftLit, Literal rightLit, MathOp op, ValueFactory vf) {
		BigDecimal left = leftLit.decimalValue();
		BigDecimal right = rightLit.decimalValue();

		switch (op) {
		case PLUS:
			return vf.createLiteral(left.add(right));
		case MINUS:
			return vf.createLiteral(left.subtract(right));
		case MULTIPLY:
			return vf.createLiteral(left.multiply(right));
		case DIVIDE:
			// Divide by zero handled through NumberFormatException
			BigDecimal result;
			try {
				// try to return the exact quotient if possible.
				result = left.divide(right, MathContext.UNLIMITED);
			} catch (ArithmeticException e) {
				// non-terminating decimal expansion in quotient, using
				// scaling and rounding.
				result = left.setScale(getDecimalExpansionScale(), RoundingMode.HALF_UP)
						.divide(right,
								RoundingMode.HALF_UP);
			}

			return vf.createLiteral(result);
		default:
			throw new IllegalArgumentException("Unknown operator: " + op);
		}
	}

	private static Literal computeForXsdFloat(Literal leftLit, Literal rightLit, MathOp op, ValueFactory vf) {
		float left = leftLit.floatValue();
		float right = rightLit.floatValue();

		switch (op) {
		case PLUS:
			return vf.createLiteral(left + right);
		case MINUS:
			return vf.createLiteral(left - right);
		case MULTIPLY:
			return vf.createLiteral(left * right);
		case DIVIDE:
			return vf.createLiteral(left / right);
		default:
			throw new IllegalArgumentException("Unknown operator: " + op);
		}
	}

	private static Literal computeForXsdDouble(Literal leftLit, Literal rightLit, MathOp op, ValueFactory vf) {
		double left = leftLit.doubleValue();
		double right = rightLit.doubleValue();

		switch (op) {
		case PLUS:
			return vf.createLiteral(left + right);
		case MINUS:
			return vf.createLiteral(left - right);
		case MULTIPLY:
			return vf.createLiteral(left * right);
		case DIVIDE:
			return vf.createLiteral(left / right);
		default:
			throw new IllegalArgumentException("Unknown operator: " + op);
		}
	}

	private static CoreDatatype.XSD determineCommonDatatype(MathOp op, CoreDatatype.XSD leftDatatype,
			CoreDatatype.XSD rightDatatype) {
		// Determine most specific datatype that the arguments have in common,
		// choosing from xsd:integer, xsd:decimal, xsd:float and xsd:double as
		// per the SPARQL/XPATH spec
		CoreDatatype.XSD commonDatatype;

		if (leftDatatype.equals(CoreDatatype.XSD.DOUBLE) || rightDatatype.equals(CoreDatatype.XSD.DOUBLE)) {
			commonDatatype = CoreDatatype.XSD.DOUBLE;
		} else if (leftDatatype.equals(CoreDatatype.XSD.FLOAT) || rightDatatype.equals(CoreDatatype.XSD.FLOAT)) {
			commonDatatype = CoreDatatype.XSD.FLOAT;
		} else if (leftDatatype.equals(CoreDatatype.XSD.DECIMAL) || rightDatatype.equals(CoreDatatype.XSD.DECIMAL)) {
			commonDatatype = CoreDatatype.XSD.DECIMAL;
		} else if (op == MathOp.DIVIDE) {
			// Result of integer divide is decimal and requires the arguments to
			// be handled as such, see for details:
			// http://www.w3.org/TR/xpath-functions/#func-numeric-divide
			commonDatatype = CoreDatatype.XSD.DECIMAL;
		} else {
			commonDatatype = CoreDatatype.XSD.INTEGER;
		}
		return commonDatatype;
	}

	/**
	 * Returns the decimal expansion scale used in division operations resulting in a decimal value with non-terminating
	 * decimal expansion. By default, this value is set to 24.
	 *
	 * @return The decimal expansion scale.
	 */
	public static int getDecimalExpansionScale() {
		return decimalExpansionScale;
	}

	/**
	 * Sets the decimal expansion scale used in divisions resulting in a decimal value with non-terminating decimal
	 * expansion.
	 *
	 * @param decimalExpansionScale The decimal expansion scale to set. Note that a mimimum of 18 is required to stay
	 *                              compliant with the XPath specification of xsd:decimal operations.
	 */
	public static void setDecimalExpansionScale(int decimalExpansionScale) {
		MathUtil.decimalExpansionScale = decimalExpansionScale;
	}
}
