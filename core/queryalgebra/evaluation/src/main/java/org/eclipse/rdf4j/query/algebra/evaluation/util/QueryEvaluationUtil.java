/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import java.util.Optional;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * @author Arjohn Kampman
 */
public class QueryEvaluationUtil {

	/**
	 * Determines the effective boolean value (EBV) of the supplied value as defined in the
	 * <a href="http://www.w3.org/TR/rdf-sparql-query/#ebv">SPARQL specification</a>:
	 * <ul>
	 * <li>The EBV of any literal whose type is xsd:boolean or numeric is false if the lexical form is not valid for
	 * that datatype (e.g. "abc"^^xsd:integer).
	 * <li>If the argument is a typed literal with a datatype of xsd:boolean, the EBV is the value of that argument.
	 * <li>If the argument is a plain literal or a typed literal with a datatype of xsd:string, the EBV is false if the
	 * operand value has zero length; otherwise the EBV is true.
	 * <li>If the argument is a numeric type or a typed literal with a datatype derived from a numeric type, the EBV is
	 * false if the operand value is NaN or is numerically equal to zero; otherwise the EBV is true.
	 * <li>All other arguments, including unbound arguments, produce a type error.
	 * </ul>
	 *
	 * @param value Some value.
	 * @return The EBV of <tt>value</tt>.
	 * @throws ValueExprEvaluationException In case the application of the EBV algorithm results in a type error.
	 */
	public static boolean getEffectiveBooleanValue(Value value) throws ValueExprEvaluationException {
		if (value instanceof Literal) {
			Literal literal = (Literal) value;
			String label = literal.getLabel();
			IRI datatype = literal.getDatatype();

			if (datatype.equals(XSD.STRING)) {
				return label.length() > 0;
			} else if (datatype.equals(XSD.BOOLEAN)) {
				// also false for illegal values
				return "true".equals(label) || "1".equals(label);
			} else if (datatype.equals(XSD.DECIMAL)) {
				try {
					String normDec = XMLDatatypeUtil.normalizeDecimal(label);
					return !normDec.equals("0.0");
				} catch (IllegalArgumentException e) {
					return false;
				}
			} else if (XMLDatatypeUtil.isIntegerDatatype(datatype)) {
				try {
					String normInt = XMLDatatypeUtil.normalize(label, datatype);
					return !normInt.equals("0");
				} catch (IllegalArgumentException e) {
					return false;
				}
			} else if (XMLDatatypeUtil.isFloatingPointDatatype(datatype)) {
				try {
					String normFP = XMLDatatypeUtil.normalize(label, datatype);
					return !normFP.equals("0.0E0") && !normFP.equals("NaN");
				} catch (IllegalArgumentException e) {
					return false;
				}
			}
		}

		throw new ValueExprEvaluationException();
	}

	public static boolean compare(Value leftVal, Value rightVal, CompareOp operator)
			throws ValueExprEvaluationException {
		return compare(leftVal, rightVal, operator, true);
	}

	public static boolean compare(Value leftVal, Value rightVal, CompareOp operator, boolean strict)
			throws ValueExprEvaluationException {
		if (leftVal instanceof Literal && rightVal instanceof Literal) {
			// Both left and right argument is a Literal
			return compareLiterals((Literal) leftVal, (Literal) rightVal, operator, strict);
		} else {
			// All other value combinations
			switch (operator) {
			case EQ:
				return valuesEqual(leftVal, rightVal);
			case NE:
				return !valuesEqual(leftVal, rightVal);
			default:
				throw new ValueExprEvaluationException(
						"Only literals with compatible, ordered datatypes can be compared using <, <=, > and >= operators");
			}
		}
	}

	private static boolean valuesEqual(Value leftVal, Value rightVal) {
		return leftVal != null && rightVal != null && leftVal.equals(rightVal);
	}

	/**
	 * Compares the supplied {@link Literal} arguments using the supplied operator, using strict (minimally-conforming)
	 * SPARQL 1.1 operator behavior.
	 *
	 * @param leftLit  the left literal argument of the comparison.
	 * @param rightLit the right literal argument of the comparison.
	 * @param operator the comparison operator to use.
	 * @return {@code true} if execution of the supplied operator on the supplied arguments succeeds, {@code false}
	 *         otherwise.
	 * @throws ValueExprEvaluationException if a type error occurred.
	 */
	public static boolean compareLiterals(Literal leftLit, Literal rightLit, CompareOp operator)
			throws ValueExprEvaluationException {
		return compareLiterals(leftLit, rightLit, operator, true);
	}

	/**
	 * Compares the supplied {@link Literal} arguments using the supplied operator.
	 *
	 * @param leftLit  the left literal argument of the comparison.
	 * @param rightLit the right literal argument of the comparison.
	 * @param operator the comparison operator to use.
	 * @param strict   boolean indicating whether comparison should use strict (minimally-conforming) SPARQL 1.1
	 *                 operator behavior, or extended behavior.
	 * @return {@code true} if execution of the supplied operator on the supplied arguments succeeds, {@code false}
	 *         otherwise.
	 * @throws ValueExprEvaluationException if a type error occurred.
	 */
	public static boolean compareLiterals(Literal leftLit, Literal rightLit, CompareOp operator, boolean strict)
			throws ValueExprEvaluationException {
		// type precendence:
		// - simple literal
		// - numeric
		// - xsd:boolean
		// - xsd:dateTime
		// - xsd:string
		// - RDF term (equal and unequal only)

		IRI leftDatatype = leftLit.getDatatype();
		IRI rightDatatype = rightLit.getDatatype();

		XSD.Datatype leftXsdDatatype = Literals.getXsdDatatype(leftLit).orElse(null);
		XSD.Datatype rightXsdDatatype = Literals.getXsdDatatype(rightLit).orElse(null);

		boolean leftLangLit = Literals.isLanguageLiteral(leftLit);
		boolean rightLangLit = Literals.isLanguageLiteral(rightLit);

		// for purposes of query evaluation in SPARQL, simple literals and string-typed literals with the same lexical
		// value are considered equal.
		IRI commonDatatype = null;
		if (QueryEvaluationUtil.isSimpleLiteral(leftLit) && QueryEvaluationUtil.isSimpleLiteral(rightLit)) {
			commonDatatype = XSD.STRING;
		}

		Integer compareResult = null;

		if (QueryEvaluationUtil.isSimpleLiteral(leftLit) && QueryEvaluationUtil.isSimpleLiteral(rightLit)) {
			compareResult = leftLit.getLabel().compareTo(rightLit.getLabel());
		} else if ((!leftLangLit && !rightLangLit) || commonDatatype != null) {
			if (commonDatatype == null && (leftXsdDatatype != null && rightXsdDatatype != null)) {
				if (leftXsdDatatype == rightXsdDatatype || leftDatatype.equals(rightDatatype)) {
					commonDatatype = leftDatatype;
				} else if (leftXsdDatatype.isNumericDatatype() && rightXsdDatatype.isNumericDatatype()) {
					// left and right arguments have different datatypes, try to find a more general, shared datatype
					if (leftXsdDatatype == XSD.Datatype.DOUBLE || rightXsdDatatype == XSD.Datatype.DOUBLE) {
						commonDatatype = XSD.DOUBLE;
					} else if (leftXsdDatatype == XSD.Datatype.FLOAT || rightXsdDatatype == XSD.Datatype.FLOAT) {
						commonDatatype = XSD.FLOAT;
					} else if (leftXsdDatatype == XSD.Datatype.DECIMAL
							|| rightXsdDatatype == XSD.Datatype.DECIMAL) {
						commonDatatype = XSD.DECIMAL;
					} else {
						commonDatatype = XSD.INTEGER;
					}
				} else if (!strict && leftXsdDatatype.isCalendarDatatype() && rightXsdDatatype.isCalendarDatatype()) {
					// We're not running in strict eval mode so we use extended datatype comparsion.
					commonDatatype = XSD.DATETIME;
				} else if (!strict && leftXsdDatatype.isDurationDatatype() && rightXsdDatatype.isDurationDatatype()) {
					commonDatatype = XSD.DURATION;
				}
			} else if (commonDatatype == null && (leftXsdDatatype == null || rightXsdDatatype == null)) {
				if (leftDatatype.equals(rightDatatype)) {
					commonDatatype = leftDatatype;
				} else if (XMLDatatypeUtil.isNumericDatatype(leftDatatype)
						&& XMLDatatypeUtil.isNumericDatatype(rightDatatype)) {
					// left and right arguments have different datatypes, try to find a more general, shared datatype
					if (leftDatatype.equals(XSD.DOUBLE) || rightDatatype.equals(XSD.DOUBLE)) {
						commonDatatype = XSD.DOUBLE;
					} else if (leftDatatype.equals(XSD.FLOAT) || rightDatatype.equals(XSD.FLOAT)) {
						commonDatatype = XSD.FLOAT;
					} else if (leftDatatype.equals(XSD.DECIMAL) || rightDatatype.equals(XSD.DECIMAL)) {
						commonDatatype = XSD.DECIMAL;
					} else {
						commonDatatype = XSD.INTEGER;
					}
				} else if (!strict && XMLDatatypeUtil.isCalendarDatatype(leftDatatype)
						&& XMLDatatypeUtil.isCalendarDatatype(rightDatatype)) {
					// We're not running in strict eval mode so we use extended datatype comparsion.
					commonDatatype = XSD.DATETIME;
				} else if (!strict && XMLDatatypeUtil.isDurationDatatype(leftDatatype)
						&& XMLDatatypeUtil.isDurationDatatype(rightDatatype)) {
					commonDatatype = XSD.DURATION;
				}
			}

			if (commonDatatype != null) {
				try {
					if (commonDatatype.equals(XSD.DOUBLE)) {
						compareResult = Double.compare(leftLit.doubleValue(), rightLit.doubleValue());
					} else if (commonDatatype.equals(XSD.FLOAT)) {
						compareResult = Float.compare(leftLit.floatValue(), rightLit.floatValue());
					} else if (commonDatatype.equals(XSD.DECIMAL)) {
						compareResult = leftLit.decimalValue().compareTo(rightLit.decimalValue());
					} else if (XMLDatatypeUtil.isIntegerDatatype(commonDatatype)) {
						compareResult = leftLit.integerValue().compareTo(rightLit.integerValue());
					} else if (commonDatatype.equals(XSD.BOOLEAN)) {
						Boolean leftBool = leftLit.booleanValue();
						Boolean rightBool = rightLit.booleanValue();
						compareResult = leftBool.compareTo(rightBool);
					} else if (XMLDatatypeUtil.isCalendarDatatype(commonDatatype)) {
						XMLGregorianCalendar left = leftLit.calendarValue();
						XMLGregorianCalendar right = rightLit.calendarValue();

						compareResult = left.compare(right);

						// Note: XMLGregorianCalendar.compare() returns compatible values (-1, 0, 1) but INDETERMINATE
						// needs special treatment
						if (compareResult == DatatypeConstants.INDETERMINATE) {
							// If we compare two xsd:dateTime we should use the specific comparison specified in SPARQL
							// 1.1
							if ((leftXsdDatatype == XSD.Datatype.DATETIME
									|| (leftXsdDatatype == null && leftDatatype.equals(XSD.DATETIME)))
									&& (rightXsdDatatype == XSD.Datatype.DATETIME
											|| (rightXsdDatatype == null && rightDatatype.equals(XSD.DATETIME)))) {
								throw new ValueExprEvaluationException("Indeterminate result for date/time comparison");
							} else {
								// We fallback to the regular RDF term compare
								compareResult = null;
							}

						}
					} else if (!strict && XMLDatatypeUtil.isDurationDatatype(commonDatatype)) {
						Duration left = XMLDatatypeUtil.parseDuration(leftLit.getLabel());
						Duration right = XMLDatatypeUtil.parseDuration(rightLit.getLabel());
						compareResult = left.compare(right);
						if (compareResult == DatatypeConstants.INDETERMINATE) {
							compareResult = null; // We fallback to regular term comparison
						}
					} else if (commonDatatype.equals(XSD.STRING)) {
						compareResult = leftLit.getLabel().compareTo(rightLit.getLabel());
					}
				} catch (IllegalArgumentException e) {
					// One of the basic-type method calls failed, try syntactic match before throwing an error
					if (leftLit.equals(rightLit)) {
						switch (operator) {
						case EQ:
							return true;
						case NE:
							return false;
						}
					}

					throw new ValueExprEvaluationException(e);
				}
			}
		}

		if (compareResult != null) {
			// Literals have compatible ordered datatypes
			switch (operator) {
			case LT:
				return compareResult.intValue() < 0;
			case LE:
				return compareResult.intValue() <= 0;
			case EQ:
				return compareResult.intValue() == 0;
			case NE:
				return compareResult.intValue() != 0;
			case GE:
				return compareResult.intValue() >= 0;
			case GT:
				return compareResult.intValue() > 0;
			default:
				throw new IllegalArgumentException("Unknown operator: " + operator);
			}
		} else {
			// All other cases, e.g. literals with languages, unequal or
			// unordered datatypes, etc. These arguments can only be compared
			// using the operators 'EQ' and 'NE'. See SPARQL's RDFterm-equal
			// operator

			boolean literalsEqual = leftLit.equals(rightLit);

			if (!literalsEqual) {
				if (!leftLangLit && !rightLangLit && isSupportedDatatype(leftDatatype)
						&& isSupportedDatatype(rightDatatype)) {
					// left and right arguments have incompatible but supported datatypes

					// we need to check that the lexical-to-value mapping for both datatypes succeeds
					if (!XMLDatatypeUtil.isValidValue(leftLit.getLabel(), leftDatatype)) {
						throw new ValueExprEvaluationException("not a valid datatype value: " + leftLit);
					}

					if (!XMLDatatypeUtil.isValidValue(rightLit.getLabel(), rightDatatype)) {
						throw new ValueExprEvaluationException("not a valid datatype value: " + rightLit);
					}

					boolean leftString;
					boolean rightString;
					boolean leftNumeric;
					boolean rightNumeric;
					boolean leftDate;
					boolean rightDate;

					if (leftXsdDatatype != null) {
						leftString = leftXsdDatatype == XSD.Datatype.STRING;
						leftNumeric = leftXsdDatatype.isNumericDatatype();
						leftDate = leftXsdDatatype.isCalendarDatatype();
					} else {
						leftString = leftDatatype.equals(XSD.STRING);
						leftNumeric = XMLDatatypeUtil.isNumericDatatype(leftDatatype);
						leftDate = XMLDatatypeUtil.isCalendarDatatype(leftDatatype);
					}

					if (rightXsdDatatype != null) {
						rightString = rightXsdDatatype == XSD.Datatype.STRING;
						rightNumeric = rightXsdDatatype.isNumericDatatype();
						rightDate = rightXsdDatatype.isCalendarDatatype();
					} else {
						rightString = rightDatatype.equals(XSD.STRING);
						rightNumeric = XMLDatatypeUtil.isNumericDatatype(rightDatatype);
						rightDate = XMLDatatypeUtil.isCalendarDatatype(rightDatatype);
					}

					if (leftString != rightString) {
						throw new ValueExprEvaluationException("Unable to compare strings with other supported types");
					}
					if (leftNumeric != rightNumeric) {
						throw new ValueExprEvaluationException(
								"Unable to compare numeric types with other supported types");
					}
					if (leftDate != rightDate) {
						throw new ValueExprEvaluationException(
								"Unable to compare date types with other supported types");
					}
				} else if (!leftLangLit && !rightLangLit) {
					// For literals with unsupported datatypes we don't know if their values are equal
					throw new ValueExprEvaluationException("Unable to compare literals with unsupported types");
				}
			}

			switch (operator) {
			case EQ:
				return literalsEqual;
			case NE:
				return !literalsEqual;
			case LT:
			case LE:
			case GE:
			case GT:
				throw new ValueExprEvaluationException(
						"Only literals with compatible, ordered datatypes can be compared using <, <=, > and >= operators");
			default:
				throw new IllegalArgumentException("Unknown operator: " + operator);
			}
		}
	}

	/**
	 * Checks whether the supplied value is a "plain literal". A "plain literal" is a literal with no datatype and
	 * optionally a language tag.
	 *
	 * @see <a href="http://www.w3.org/TR/2004/REC-rdf-concepts-20040210/#dfn-plain-literal">RDF Literal
	 *      Documentation</a>
	 */
	public static boolean isPlainLiteral(Value v) {
		if (v instanceof Literal) {
			return isPlainLiteral(((Literal) v));
		}
		return false;
	}

	public static boolean isPlainLiteral(Literal l) {
		Optional<XSD.Datatype> xsdDatatype = Literals.getXsdDatatype(l);
		return xsdDatatype
				.map(datatype -> datatype == XSD.Datatype.STRING)
				.orElseGet(() -> (l.getDatatype().equals(XSD.STRING)));

	}

	/**
	 * Checks whether the supplied value is a "simple literal". A "simple literal" is a literal with no language tag nor
	 * datatype.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#simple_literal">SPARQL Simple Literal Documentation</a>
	 */
	public static boolean isSimpleLiteral(Value v) {
		if (v instanceof Literal) {
			return isSimpleLiteral((Literal) v);
		}

		return false;
	}

	/**
	 * Checks whether the supplied literal is a "simple literal". A "simple literal" is a literal with no language tag
	 * and the datatype {@link XSD#STRING}.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#simple_literal">SPARQL Simple Literal Documentation</a>
	 */
	public static boolean isSimpleLiteral(Literal l) {
		return !Literals.isLanguageLiteral(l) && l.getDatatype().equals(XSD.STRING);
	}

	/**
	 * Checks whether the supplied literal is a "string literal". A "string literal" is either a simple literal, a plain
	 * literal with language tag, or a literal with datatype xsd:string.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#func-string">SPARQL Functions on Strings Documentation</a>
	 */
	public static boolean isStringLiteral(Value v) {
		if (v instanceof Literal) {
			return isStringLiteral((Literal) v);
		}

		return false;
	}

	/**
	 * Checks whether the supplied two literal arguments are 'argument compatible' according to the SPARQL definition.
	 *
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 * @return true iff the two supplied arguments are argument compatible, false otherwise
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#func-arg-compatibility">SPARQL Argument Compatibility
	 *      Rules</a>
	 */
	public static boolean compatibleArguments(Literal arg1, Literal arg2) {
		boolean arg1Language = Literals.isLanguageLiteral(arg1);
		boolean arg2Language = Literals.isLanguageLiteral(arg2);
		boolean arg1Simple = isSimpleLiteral(arg1);
		boolean arg2Simple = isSimpleLiteral(arg2);
		// 1. The arguments are literals typed as xsd:string
		// 2. The arguments are language literals with identical language tags
		// 3. The first argument is a language literal and the second
		// argument is a literal typed as xsd:string

		boolean compatible =

				(arg1Simple && arg2Simple)
						|| (arg1Language && arg2Language && arg1.getLanguage().equals(arg2.getLanguage()))
						|| (arg1Language && arg2Simple);

		return compatible;
	}

	/**
	 * Checks whether the supplied literal is a "string literal". A "string literal" is either a simple literal, a plain
	 * literal with language tag, or a literal with datatype xsd:string.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#func-string">SPARQL Functions on Strings Documentation</a>
	 */
	public static boolean isStringLiteral(Literal l) {
		IRI datatype = l.getDatatype();
		return Literals.isLanguageLiteral(l) || datatype.equals(XSD.STRING);
	}

	private static boolean isSupportedDatatype(IRI datatype) {
		return (XSD.STRING.equals(datatype) || XMLDatatypeUtil.isNumericDatatype(datatype)
				|| XMLDatatypeUtil.isCalendarDatatype(datatype));
	}
}
