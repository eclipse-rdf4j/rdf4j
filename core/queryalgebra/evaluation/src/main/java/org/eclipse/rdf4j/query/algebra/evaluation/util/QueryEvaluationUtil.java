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

import java.util.Objects;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * @author Arjohn Kampman
 */
public class QueryEvaluationUtil {

	public static final ValueExprEvaluationException INDETERMINATE_DATE_TIME_EXCEPTION = new ValueExprEvaluationException(
			"Indeterminate result for date/time comparison");
	public static final ValueExprEvaluationException STRING_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION = new ValueExprEvaluationException(
			"Unable to compare strings with other supported types");
	public static final ValueExprEvaluationException NUMERIC_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION = new ValueExprEvaluationException(
			"Unable to compare numeric types with other supported types");
	public static final ValueExprEvaluationException DATE_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION = new ValueExprEvaluationException(
			"Unable to compare date types with other supported types");
	public static final ValueExprEvaluationException UNSUPPOERTED_TYPES_EXCEPTION = new ValueExprEvaluationException(
			"Unable to compare literals with unsupported types");
	public static final ValueExprEvaluationException NOT_COMPATIBLE_AND_ORDERED_EXCEPTION = new ValueExprEvaluationException(
			"Only literals with compatible, ordered datatypes can be compared using <, <=, > and >= operators");

	/**
	 * Determines the effective boolean value (EBV) of the supplied value as defined in the
	 * <a href="http://www.w3.org/TR/rdf-sparql-query/#ebv">SPARQL specification</a>:
	 * <ul>
	 * <li>The EBV of any literal whose type is CoreDatatype.XSD:boolean or numeric is false if the lexical form is not
	 * valid for that datatype (e.g. "abc"^^xsd:integer).
	 * <li>If the argument is a typed literal with a datatype of CoreDatatype.XSD:boolean, the EBV is the value of that
	 * argument.
	 * <li>If the argument is a plain literal or a typed literal with a datatype of CoreDatatype.XSD:string, the EBV is
	 * false if the operand value has zero length; otherwise the EBV is true.
	 * <li>If the argument is a numeric type or a typed literal with a datatype derived from a numeric type, the EBV is
	 * false if the operand value is NaN or is numerically equal to zero; otherwise the EBV is true.
	 * <li>All other arguments, including unbound arguments, produce a type error.
	 * </ul>
	 *
	 * @param value Some value.
	 * @return The EBV of <var>value</var>.
	 * @throws ValueExprEvaluationException In case the application of the EBV algorithm results in a type error.
	 */
	public static boolean getEffectiveBooleanValue(Value value) throws ValueExprEvaluationException {

		if (value == BooleanLiteral.TRUE) {
			return true;
		} else if (value == BooleanLiteral.FALSE) {
			return false;
		}

		if (value.isLiteral()) {
			Literal literal = (Literal) value;
			String label = literal.getLabel();
			CoreDatatype.XSD datatype = literal.getCoreDatatype().asXSDDatatypeOrNull();

			if (datatype == CoreDatatype.XSD.STRING) {
				return !label.isEmpty();
			} else if (datatype == CoreDatatype.XSD.BOOLEAN) {
				// also false for illegal values
				return "true".equals(label) || "1".equals(label);
			} else if (datatype == CoreDatatype.XSD.DECIMAL) {
				try {
					String normDec = XMLDatatypeUtil.normalizeDecimal(label);
					return !normDec.equals("0.0");
				} catch (IllegalArgumentException e) {
					return false;
				}
			} else if (datatype != null && datatype.isIntegerDatatype()) {
				try {
					String normInt = XMLDatatypeUtil.normalize(label, datatype);
					return !normInt.equals("0");
				} catch (IllegalArgumentException e) {
					return false;
				}
			} else if (datatype != null && datatype.isFloatingPointDatatype()) {
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
		if (leftVal == rightVal) {
			switch (operator) {
			case EQ:
				return true;
			case NE:
				return false;
			}
		}

		if (leftVal != null && leftVal.isLiteral() && rightVal != null && rightVal.isLiteral()) {
			// Both left and right argument is a Literal
			return compareLiterals((Literal) leftVal, (Literal) rightVal, operator, strict);
		} else {
			// All other value combinations
			switch (operator) {
			case EQ:
				return Objects.equals(leftVal, rightVal);
			case NE:
				return !Objects.equals(leftVal, rightVal);
			default:
				throw new ValueExprEvaluationException(
						"Only literals with compatible, ordered datatypes can be compared using <, <=, > and >= operators");
			}
		}
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
		// - CoreDatatype.XSD:boolean
		// - CoreDatatype.XSD:dateTime
		// - CoreDatatype.XSD:string
		// - RDF term (equal and unequal only)

		if (leftLit == rightLit) {
			switch (operator) {
			case EQ:
				return true;
			case NE:
				return false;
			}
		}

		CoreDatatype.XSD leftCoreDatatype = leftLit.getCoreDatatype().asXSDDatatypeOrNull();
		CoreDatatype.XSD rightCoreDatatype = rightLit.getCoreDatatype().asXSDDatatypeOrNull();

		boolean leftLangLit = Literals.isLanguageLiteral(leftLit);
		boolean rightLangLit = Literals.isLanguageLiteral(rightLit);

		// for purposes of query evaluation in SPARQL, simple literals and string-typed literals with the same lexical
		// value are considered equal.

		if (QueryEvaluationUtil.isSimpleLiteral(leftLangLit, leftCoreDatatype)
				&& QueryEvaluationUtil.isSimpleLiteral(rightLangLit, rightCoreDatatype)) {
			return compareWithOperator(operator, leftLit.getLabel().compareTo(rightLit.getLabel()));
		} else if (!(leftLangLit || rightLangLit)) {

			CoreDatatype.XSD commonDatatype = getCommonDatatype(strict, leftCoreDatatype, rightCoreDatatype);

			if (commonDatatype != null) {
				try {
					if (commonDatatype == CoreDatatype.XSD.DOUBLE) {
						return compareWithOperator(operator,
								Double.compare(leftLit.doubleValue(), rightLit.doubleValue()));
					} else if (commonDatatype == CoreDatatype.XSD.FLOAT) {
						return compareWithOperator(operator,
								Float.compare(leftLit.floatValue(), rightLit.floatValue()));
					} else if (commonDatatype == CoreDatatype.XSD.DECIMAL) {
						return compareWithOperator(operator, leftLit.decimalValue().compareTo(rightLit.decimalValue()));
					} else if (commonDatatype.isIntegerDatatype()) {
						return compareWithOperator(operator, leftLit.integerValue().compareTo(rightLit.integerValue()));
					} else if (commonDatatype == CoreDatatype.XSD.BOOLEAN) {
						return compareWithOperator(operator,
								Boolean.compare(leftLit.booleanValue(), rightLit.booleanValue()));
					} else if (commonDatatype.isCalendarDatatype()) {
						XMLGregorianCalendar left = leftLit.calendarValue();
						XMLGregorianCalendar right = rightLit.calendarValue();

						int compare = left.compare(right);

						// Note: XMLGregorianCalendar.compare() returns compatible values (-1, 0, 1) but INDETERMINATE
						// needs special treatment
						if (compare == DatatypeConstants.INDETERMINATE) {
							// If we compare two CoreDatatype.XSD:dateTime we should use the specific comparison
							// specified in SPARQL
							// 1.1
							if (leftCoreDatatype == CoreDatatype.XSD.DATETIME
									&& rightCoreDatatype == CoreDatatype.XSD.DATETIME) {
								throw INDETERMINATE_DATE_TIME_EXCEPTION;
							}
						} else {
							return compareWithOperator(operator, compare);
						}

					} else if (!strict && commonDatatype.isDurationDatatype()) {
						Duration left = XMLDatatypeUtil.parseDuration(leftLit.getLabel());
						Duration right = XMLDatatypeUtil.parseDuration(rightLit.getLabel());
						int compare = left.compare(right);
						if (compare != DatatypeConstants.INDETERMINATE) {
							return compareWithOperator(operator, compare);
						} else {
							return otherCases(leftLit, rightLit, operator, leftCoreDatatype, rightCoreDatatype,
									leftLangLit, rightLangLit, strict);
						}

					} else if (commonDatatype == CoreDatatype.XSD.STRING) {
						return compareWithOperator(operator, leftLit.getLabel().compareTo(rightLit.getLabel()));
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

		// All other cases, e.g. literals with languages, unequal or
		// unordered datatypes, etc. These arguments can only be compared
		// using the operators 'EQ' and 'NE'. See SPARQL's RDFterm-equal
		// operator

		return otherCases(leftLit, rightLit, operator, leftCoreDatatype, rightCoreDatatype, leftLangLit, rightLangLit,
				strict);

	}

	private static boolean otherCases(Literal leftLit, Literal rightLit, CompareOp operator,
			CoreDatatype.XSD leftCoreDatatype, CoreDatatype.XSD rightCoreDatatype, boolean leftLangLit,
			boolean rightLangLit, boolean strict) {
		boolean literalsEqual = leftLit.equals(rightLit);

		if (!literalsEqual) {
			if (!leftLangLit && !rightLangLit && isSupportedDatatype(leftCoreDatatype)
					&& isSupportedDatatype(rightCoreDatatype)) {
				// left and right arguments have incompatible but supported datatypes

				// we need to check that the lexical-to-value mapping for both datatypes succeeds
				if (!XMLDatatypeUtil.isValidValue(leftLit.getLabel(), leftCoreDatatype)) {
					throw new ValueExprEvaluationException("not a valid datatype value: " + leftLit);
				}

				if (!XMLDatatypeUtil.isValidValue(rightLit.getLabel(), rightCoreDatatype)) {
					throw new ValueExprEvaluationException("not a valid datatype value: " + rightLit);
				}

				validateDatatypeCompatibility(strict, leftCoreDatatype, rightCoreDatatype);
			} else if (!leftLangLit && !rightLangLit) {
				// For literals with unsupported datatypes we don't know if their values are equal
				throw UNSUPPOERTED_TYPES_EXCEPTION;
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
			throw NOT_COMPATIBLE_AND_ORDERED_EXCEPTION;
		default:
			throw new IllegalArgumentException("Unknown operator: " + operator);
		}
	}

	/**
	 * Validate if we are comparing supported but incompatible datatypes. Throws a {@link ValueExprEvaluationException}
	 * if this is the case.
	 * <p>
	 * Used in a <i>strict / minimally-conforming</i> interpretation of the SPARQL specification. In the
	 * <a href="https://www.w3.org/TR/sparql11-query/#OperatorMapping">SPARQL 1.1 operator mapping table</a>, when
	 * comparing two literals with different datatypes (that cannot be cast to a common type), the only mapping that
	 * applies is comparison using RDF term-equality:
	 *
	 * <table>
	 * <tr>
	 * <td>A != B</td>
	 * <td>RDF term</td>
	 * <td>RDF term</td>
	 * <td>fn:not(RDFterm-equal(A, B))</td>
	 * <td>xsd:boolean</td>
	 * </tr>
	 * </table>
	 *
	 * <a href="https://www.w3.org/TR/sparql11-query/#func-RDFterm-equal">RDFterm-equal</a> is defined as follows:
	 *
	 * <blockquote> Returns TRUE if term1 and term2 are the same RDF term as defined in
	 * <a href="http://www.w3.org/TR/rdf-concepts/">Resource Description Framework (RDF): Concepts and Abstract Syntax
	 * [CONCEPTS]</a>; <strong>produces a type error if the arguments are both literal but are not the same RDF
	 * term</strong>; returns FALSE otherwise. term1 and term2 are the same if any of the following is true:
	 *
	 * <ul>
	 * <li>term1 and term2 are equivalent IRIs as defined in
	 * <a href="http://www.w3.org/TR/rdf-concepts/#section-Graph-URIref">6.4 RDF URI References of [CONCEPTS]</a>.
	 * <li>term1 and term2 are equivalent literals as defined in
	 * <a href="http://www.w3.org/TR/rdf-concepts/#section-Literal-Equality">6.5.1 Literal Equality of [CONCEPTS]</a>.
	 * <li>term1 and term2 are the same blank node as described in
	 * <a href="http://www.w3.org/TR/rdf-concepts/#section-blank-nodes">6.6 Blank Nodes of [CONCEPTS]</a>.
	 * </ul>
	 * </blockquote>
	 * <p>
	 * (emphasis ours)
	 * <p>
	 * When applying the SPARQL specification in a minimally-conforming manner, RDFterm-equal is supposed to return a
	 * type error whenever we compare two literals with incompatible datatypes: we have two literals, but they are not
	 * the same RDF term (as they are not equivalent literals as defined in the linked section in RDF Concepts). This
	 * holds <i>even if</i> those two datatypes that fully supported and understood (say, when comparing an xsd:string
	 * and an xsd:boolean).
	 * <p>
	 * In a non-strict interpretation, however, we allow comparing comparing two literals with incompatible but
	 * supported datatypes (string, numeric, calendar): An equality comparison will result in <code>false</code>, and an
	 * inequality comparison will result in <code>true</code>. Note that this does not violate the SPARQL specification
	 * as it falls under <a href="https://www.w3.org/TR/sparql11-query/#operatorExtensibility">operator extensibility
	 * (section 17.3.1)</a>.
	 *
	 * @param strict            flag indicating if query evaluation is operating in strict/minimally-conforming mode.
	 * @param leftCoreDatatype  the left datatype to compare
	 * @param rightCoreDatatype the right datatype to compare
	 * @throws ValueExprEvaluationException if query evaluation is operating in strict mode, and the two supplied
	 *                                      datatypes are both supported datatypes but not comparable.
	 * @see <a href="https://github.com/eclipse/rdf4j/issues/3947">Github issue #3947</a>
	 */
	private static void validateDatatypeCompatibility(boolean strict, CoreDatatype.XSD leftCoreDatatype,
			CoreDatatype.XSD rightCoreDatatype) throws ValueExprEvaluationException {
		if (!strict) {
			return;
		}

		boolean leftString = leftCoreDatatype == CoreDatatype.XSD.STRING;
		boolean rightString = rightCoreDatatype == CoreDatatype.XSD.STRING;
		if (leftString != rightString) {
			throw STRING_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION;
		}

		boolean leftNumeric = leftCoreDatatype.isNumericDatatype();
		boolean rightNumeric = rightCoreDatatype.isNumericDatatype();
		if (leftNumeric != rightNumeric) {
			throw NUMERIC_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION;
		}

		boolean leftDate = leftCoreDatatype.isCalendarDatatype();
		boolean rightDate = rightCoreDatatype.isCalendarDatatype();
		if (leftDate != rightDate) {
			throw DATE_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION;
		}
	}

	private static CoreDatatype.XSD getCommonDatatype(boolean strict, CoreDatatype.XSD leftCoreDatatype,
			CoreDatatype.XSD rightCoreDatatype) {
		if (leftCoreDatatype != null && rightCoreDatatype != null) {
			if (leftCoreDatatype == rightCoreDatatype) {
				return leftCoreDatatype;
			} else if (leftCoreDatatype.isNumericDatatype() && rightCoreDatatype.isNumericDatatype()) {
				// left and right arguments have different datatypes, try to find a more general, shared datatype
				if (leftCoreDatatype == CoreDatatype.XSD.DOUBLE || rightCoreDatatype == CoreDatatype.XSD.DOUBLE) {
					return CoreDatatype.XSD.DOUBLE;
				} else if (leftCoreDatatype == CoreDatatype.XSD.FLOAT || rightCoreDatatype == CoreDatatype.XSD.FLOAT) {
					return CoreDatatype.XSD.FLOAT;
				} else if (leftCoreDatatype == CoreDatatype.XSD.DECIMAL
						|| rightCoreDatatype == CoreDatatype.XSD.DECIMAL) {
					return CoreDatatype.XSD.DECIMAL;
				} else {
					return CoreDatatype.XSD.INTEGER;
				}
			} else if (!strict && leftCoreDatatype.isCalendarDatatype() && rightCoreDatatype.isCalendarDatatype()) {
				// We're not running in strict eval mode so we use extended datatype comparsion.
				return CoreDatatype.XSD.DATETIME;
			} else if (!strict && leftCoreDatatype.isDurationDatatype() && rightCoreDatatype.isDurationDatatype()) {
				return CoreDatatype.XSD.DURATION;
			}
		}
		return null;
	}

	private static boolean compareWithOperator(CompareOp operator, int i) {
		switch (operator) {
		case LT:
			return i < 0;
		case LE:
			return i <= 0;
		case EQ:
			return i == 0;
		case NE:
			return i != 0;
		case GE:
			return i >= 0;
		case GT:
			return i > 0;
		default:
			throw new IllegalArgumentException("Unknown operator: " + operator);
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
		if (v.isLiteral()) {
			return isPlainLiteral((Literal) v);
		}
		return false;
	}

	public static boolean isPlainLiteral(Literal l) {
		assert l.getLanguage().isEmpty() || l.getCoreDatatype() == CoreDatatype.RDF.LANGSTRING;
		return l.getCoreDatatype() == CoreDatatype.XSD.STRING || l.getCoreDatatype() == CoreDatatype.RDF.LANGSTRING;
	}

//	public static boolean isPlainLiteral(Literal l) {
//		return l.getCoreDatatype().filter(d -> d == CoreDatatype.XSD.STRING).isPresent();
////		return l.getCoreDatatype().orElse(null) == CoreDatatype.XSD.STRING;
//	}

	/**
	 * Checks whether the supplied value is a "simple literal". A "simple literal" is a literal with no language tag nor
	 * datatype.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#simple_literal">SPARQL Simple Literal Documentation</a>
	 */
	public static boolean isSimpleLiteral(Value v) {
		if (v.isLiteral()) {
			return isSimpleLiteral((Literal) v);
		}

		return false;
	}

	/**
	 * Checks whether the supplied literal is a "simple literal". A "simple literal" is a literal with no language tag
	 * and the datatype {@link CoreDatatype.XSD#STRING}.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#simple_literal">SPARQL Simple Literal Documentation</a>
	 */
	public static boolean isSimpleLiteral(Literal l) {
		return l.getCoreDatatype() == CoreDatatype.XSD.STRING && !Literals.isLanguageLiteral(l);
	}

	/**
	 * Checks whether the supplied literal is a "simple literal". A "simple literal" is a literal with no language tag
	 * and the datatype {@link CoreDatatype.XSD#STRING}.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#simple_literal">SPARQL Simple Literal Documentation</a>
	 */
	public static boolean isSimpleLiteral(boolean isLang, CoreDatatype datatype) {
		return !isLang && datatype == CoreDatatype.XSD.STRING;
	}

	/**
	 * Checks whether the supplied literal is a "string literal". A "string literal" is either a simple literal, a plain
	 * literal with language tag, or a literal with datatype CoreDatatype.XSD:string.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#func-string">SPARQL Functions on Strings Documentation</a>
	 */
	public static boolean isStringLiteral(Value v) {
		if (v.isLiteral()) {
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
		// 1. The arguments are literals typed as CoreDatatype.XSD:string
		// 2. The arguments are language literals with identical language tags
		// 3. The first argument is a language literal and the second
		// argument is a literal typed as CoreDatatype.XSD:string

		return isSimpleLiteral(arg1) && isSimpleLiteral(arg2)
				|| Literals.isLanguageLiteral(arg1) && Literals.isLanguageLiteral(arg2)
						&& arg1.getLanguage().equals(arg2.getLanguage())
				|| Literals.isLanguageLiteral(arg1) && isSimpleLiteral(arg2);
	}

	/**
	 * Checks whether the supplied literal is a "string literal". A "string literal" is either a simple literal, a plain
	 * literal with language tag, or a literal with datatype CoreDatatype.XSD:string.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#func-string">SPARQL Functions on Strings Documentation</a>
	 */
	public static boolean isStringLiteral(Literal l) {
		return l.getCoreDatatype() == CoreDatatype.XSD.STRING || Literals.isLanguageLiteral(l);
	}

	private static boolean isSupportedDatatype(CoreDatatype.XSD datatype) {
		return datatype != null && (datatype == CoreDatatype.XSD.STRING ||
				datatype.isNumericDatatype() ||
				datatype.isCalendarDatatype());
	}
}
