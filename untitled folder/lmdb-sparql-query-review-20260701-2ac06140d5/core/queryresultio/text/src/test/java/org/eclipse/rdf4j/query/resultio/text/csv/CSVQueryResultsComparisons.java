/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.csv;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * Value equality testing for CSV-based query result serializations.
 * <p>
 * CSV formats require slightly special result comparison because its format is slightly "lossy" in information detail.
 * <p>
 * FIXME: This code is an adapted copy of the relevant parts of QueryEvaluationUtil. That util can not be used here
 * directly because of circular dependency issues between the rdf4j and rdf4j-storage repositories.
 *
 * @author Jeen Broekstra
 */
class CSVQueryResultsComparisons {

	/**
	 * Verifies if two values are considered equal for CSV result processing.
	 */
	static boolean equals(Value value1, Value value2) {
		try {
			return compare(value1, value2);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static boolean compare(Value leftVal, Value rightVal) {
		if (leftVal instanceof Literal && rightVal instanceof Literal) {
			// Both left and right argument is a Literal
			return compareLiterals((Literal) leftVal, (Literal) rightVal);
		} else {
			return valuesEqual(leftVal, rightVal);
		}
	}

	private static boolean valuesEqual(Value leftVal, Value rightVal) {
		return leftVal != null && rightVal != null && leftVal.equals(rightVal);
	}

	private static boolean compareLiterals(Literal leftLit, Literal rightLit) {
		// type precendence:
		// - simple literal
		// - numeric
		// - xsd:boolean
		// - xsd:dateTime
		// - xsd:string
		// - RDF term (equal and unequal only)

		boolean strict = true;
		IRI leftDatatype = leftLit.getDatatype();
		IRI rightDatatype = rightLit.getDatatype();

		boolean leftLangLit = Literals.isLanguageLiteral(leftLit);
		boolean rightLangLit = Literals.isLanguageLiteral(rightLit);

		// for purposes of query evaluation in SPARQL, simple literals and
		// string-typed literals with the same lexical value are considered equal.
		IRI commonDatatype = null;
		if (isSimpleLiteral(leftLit) && isSimpleLiteral(rightLit)) {
			commonDatatype = XSD.STRING;
		}

		Integer compareResult = null;

		if (isSimpleLiteral(leftLit) && isSimpleLiteral(rightLit)) {
			compareResult = leftLit.getLabel().compareTo(rightLit.getLabel());
		} else if ((!leftLangLit && !rightLangLit) || commonDatatype != null) {
			if (commonDatatype == null) {
				if (leftDatatype.equals(rightDatatype)) {
					commonDatatype = leftDatatype;
				} else if (XMLDatatypeUtil.isNumericDatatype(leftDatatype)
						&& XMLDatatypeUtil.isNumericDatatype(rightDatatype)) {
					// left and right arguments have different datatypes, try to find
					// a
					// more general, shared datatype
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

						// Note: XMLGregorianCalendar.compare() returns compatible
						// values
						// (-1, 0, 1) but INDETERMINATE needs special treatment
						if (compareResult == DatatypeConstants.INDETERMINATE) {
							// If we compare two xsd:dateTime we should use the specific comparison specified in SPARQL
							// 1.1
							if (leftDatatype.equals(XSD.DATETIME) && rightDatatype.equals(XSD.DATETIME)) {
								throw new RuntimeException("Indeterminate result for date/time comparison");
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
					// One of the basic-type method calls failed, try syntactic match
					// before throwing an error
					if (leftLit.equals(rightLit)) {
						return true;
					}

					throw e;
				}
			}
		}

		if (compareResult != null) {
			return compareResult.intValue() == 0;
		} else {
			// All other cases, e.g. literals with languages, unequal or
			// unordered datatypes, etc. These arguments can only be compared
			// using the operators 'EQ' and 'NE'. See SPARQL's RDFterm-equal
			// operator

			boolean literalsEqual = leftLit.equals(rightLit);

			if (!literalsEqual) {
				if (!leftLangLit && !rightLangLit && isSupportedDatatype(leftDatatype)
						&& isSupportedDatatype(rightDatatype)) {
					// left and right arguments have incompatible but supported
					// datatypes

					// we need to check that the lexical-to-value mapping for both
					// datatypes succeeds
					if (!XMLDatatypeUtil.isValidValue(leftLit.getLabel(), leftDatatype)) {
						throw new IllegalArgumentException("not a valid datatype value: " + leftLit);
					}

					if (!XMLDatatypeUtil.isValidValue(rightLit.getLabel(), rightDatatype)) {
						throw new IllegalArgumentException("not a valid datatype value: " + rightLit);
					}
					boolean leftString = leftDatatype.equals(XSD.STRING);
					boolean rightString = rightDatatype.equals(XSD.STRING);
					boolean leftNumeric = XMLDatatypeUtil.isNumericDatatype(leftDatatype);
					boolean rightNumeric = XMLDatatypeUtil.isNumericDatatype(rightDatatype);
					boolean leftDate = XMLDatatypeUtil.isCalendarDatatype(leftDatatype);
					boolean rightDate = XMLDatatypeUtil.isCalendarDatatype(rightDatatype);

					if (leftString != rightString) {
						throw new IllegalArgumentException("Unable to compare strings with other supported types");
					}
					if (leftNumeric != rightNumeric) {
						throw new IllegalArgumentException(
								"Unable to compare numeric types with other supported types");
					}
					if (leftDate != rightDate) {
						throw new IllegalArgumentException("Unable to compare date types with other supported types");
					}
				} else if (!leftLangLit && !rightLangLit) {
					// For literals with unsupported datatypes we don't know if their
					// values are equal
					throw new IllegalArgumentException("Unable to compare literals with unsupported types");
				}
			}

			return literalsEqual;
		}
	}

	private static boolean isSupportedDatatype(IRI datatype) {
		return (XSD.STRING.equals(datatype) || XMLDatatypeUtil.isNumericDatatype(datatype)
				|| XMLDatatypeUtil.isCalendarDatatype(datatype));
	}

	/**
	 * Checks whether the supplied literal is a "simple literal". A "simple literal" is a literal with no language tag
	 * and the datatype {@link XSD#STRING}.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#simple_literal">SPARQL Simple Literal Documentation</a>
	 */
	private static boolean isSimpleLiteral(Literal l) {
		return !Literals.isLanguageLiteral(l) && l.getDatatype().equals(XSD.STRING);
	}
}
