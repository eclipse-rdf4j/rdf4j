/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import static javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED;

/**
 * This class will take over for QueryEvaluationUtil. Currently marked as InternalUseOnly because there may still be
 * changes to how this class works.
 *
 * @author Arjohn Kampman
 * @author Håvard M. Ottestad
 */
@InternalUseOnly()
public class QueryEvaluationUtility {

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
	 */
	public static Result getEffectiveBooleanValue(Value value) {
		if (value.isLiteral()) {
			Literal literal = (Literal) value;
			String label = literal.getLabel();
			CoreDatatype.XSD datatype = literal.getCoreDatatype().asXSDDatatypeOrNull();

			if (datatype == CoreDatatype.XSD.STRING) {
				return Result.fromBoolean(label.length() > 0);
			} else if (datatype == CoreDatatype.XSD.BOOLEAN) {
				// also false for illegal values
				return Result.fromBoolean("true".equals(label) || "1".equals(label));
			} else if (datatype == CoreDatatype.XSD.DECIMAL) {
				try {
					String normDec = XMLDatatypeUtil.normalizeDecimal(label);
					return Result.fromBoolean(!normDec.equals("0.0"));
				} catch (IllegalArgumentException e) {
					return Result.fromBoolean(false);
				}
			} else if (datatype != null && datatype.isIntegerDatatype()) {
				try {
					String normInt = XMLDatatypeUtil.normalize(label, datatype);
					return Result.fromBoolean(!normInt.equals("0"));
				} catch (IllegalArgumentException e) {
					return Result.fromBoolean(false);
				}
			} else if (datatype != null && datatype.isFloatingPointDatatype()) {
				try {
					String normFP = XMLDatatypeUtil.normalize(label, datatype);
					return Result.fromBoolean(!normFP.equals("0.0E0") && !normFP.equals("NaN"));
				} catch (IllegalArgumentException e) {
					return Result.fromBoolean(false);
				}
			}

		}

		return Result.incompatibleValueExpression;
	}

	public static Result compare(Value leftVal, Value rightVal, CompareOp operator) {
		return compare(leftVal, rightVal, operator, true);
	}

	public static Result compare(Value leftVal, Value rightVal, CompareOp operator, boolean strict) {
		if (leftVal.isLiteral() && rightVal.isLiteral()) {
			// Both left and right argument is a Literal
			return compareLiterals((Literal) leftVal, (Literal) rightVal, operator, strict);
		} else {
			// All other value combinations
			switch (operator) {
				case EQ:
					return Result.fromBoolean(Objects.equals(leftVal, rightVal));
				case NE:
					return Result.fromBoolean(!Objects.equals(leftVal, rightVal));
				default:
					return Result.incompatibleValueExpression;
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
	 * otherwise.
	 */
	public static Result compareLiterals(Literal leftLit, Literal rightLit, CompareOp operator) {
		return compareLiterals(leftLit, rightLit, operator, true);
	}

	public static Order compareLiterals(Literal leftLit, Literal rightLit, boolean strict) {
		// type precendence:
		// - simple literal
		// - numeric
		// - CoreDatatype.XSD:boolean
		// - CoreDatatype.XSD:dateTime
		// - CoreDatatype.XSD:string
		// - RDF term (equal and unequal only)

		CoreDatatype.XSD leftCoreDatatype = leftLit.getCoreDatatype().asXSDDatatypeOrNull();
		CoreDatatype.XSD rightCoreDatatype = rightLit.getCoreDatatype().asXSDDatatypeOrNull();

		// for purposes of query evaluation in SPARQL, simple literals and string-typed literals with the same lexical
		// value are considered equal.

		if (leftCoreDatatype == CoreDatatype.XSD.STRING && rightCoreDatatype == CoreDatatype.XSD.STRING) {
			return Order.from(leftLit.getLabel().compareTo(rightLit.getLabel()));
		} else if (leftCoreDatatype != null
			&& rightCoreDatatype != null) {

			if (leftCoreDatatype.isXSDDatatype() && rightCoreDatatype.isXSDDatatype()) {

				CoreDatatype.XSD commonDatatype = getCommonDatatype(strict, leftCoreDatatype, rightCoreDatatype);

				if (commonDatatype != null) {

					try {
						Order order = handleCommonDatatype(leftLit, rightLit, strict, leftCoreDatatype,
							rightCoreDatatype,
							commonDatatype);

						if (order == Order.illegalArgument) {
							if (leftLit.equals(rightLit)) {
								return Order.equal;
							}
						}

						if (order != null) {
							return order;
						}
					} catch (IllegalArgumentException e) {
						if (leftLit.equals(rightLit)) {
							return Order.equal;
						}
					}

				}
			}

		}

		// All other cases, e.g. literals with languages, unequal or
		// unordered datatypes, etc. These arguments can only be compared
		// using the operators 'EQ' and 'NE'. See SPARQL's RDFterm-equal
		// operator

		return otherCases(leftLit, rightLit, leftCoreDatatype,
			rightCoreDatatype, leftCoreDatatype == null && leftLit.getCoreDatatype() == CoreDatatype.RDF.LANGSTRING,
			rightCoreDatatype == null && rightLit.getCoreDatatype() == CoreDatatype.RDF.LANGSTRING);

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
	 * otherwise.
	 */
	public static Result compareLiterals(Literal leftLit, Literal rightLit, CompareOp operator, boolean strict) {
		Order order = compareLiterals(leftLit, rightLit, strict);
		return order.toResult(operator);
	}

	public static Instant xmlGregorianCalendarToInstant(XMLGregorianCalendar xmlGregCal) {
		int year = xmlGregCal.getYear();
		if (year <= 1973 || year >= 2038) {
			return null;
		}

		int month = xmlGregCal.getMonth();
		if (month < 0) {
			return null;
		}

		int day = xmlGregCal.getDay();
		if (day < 0) {
			return null;
		}

		int hour = xmlGregCal.getHour();
		if (hour < 0) {
			return null;
		}

		int minute = xmlGregCal.getMinute();
		if (minute < 0) {
			return null;
		}

		int second = xmlGregCal.getSecond();
		if (second < 0) {
			return null;
		}

		int nanosecond = xmlGregCal.getFractionalSecond() != null
			? xmlGregCal.getFractionalSecond().movePointRight(9).intValue()
			: 0;
		if (nanosecond < 0) {
			return null;
		}

		LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute, second, nanosecond);
		ZoneOffset zoneOffset = xmlGregCal.getTimezone() == FIELD_UNDEFINED
			? ZoneOffset.UTC
			: ZoneOffset.ofTotalSeconds(xmlGregCal.getTimezone() * 60);
		return localDateTime.toInstant(zoneOffset);
	}

	public static Instant xmlGregorianCalendarDateToInstant(XMLGregorianCalendar xmlGregCal) {
		int year = xmlGregCal.getYear();
		if (year <= 1973 || year >= 2038) {
			return null;
		}

		int month = xmlGregCal.getMonth();
		if (month < 0) {
			return null;
		}

		int day = xmlGregCal.getDay();
		if (day < 0) {
			return null;
		}

		int timezone = xmlGregCal.getTimezone();
		if (timezone == FIELD_UNDEFINED || timezone == 0) {
			return yearMonthDayToInstant(year, month, day);
		}

		LocalDateTime localDateTime = LocalDateTime.of(year, month, day, 0, 0, 0, 0);
		ZoneOffset zoneOffset = xmlGregCal.getTimezone() == FIELD_UNDEFINED
			? ZoneOffset.UTC
			: ZoneOffset.ofTotalSeconds(xmlGregCal.getTimezone() * 60);
		return localDateTime.toInstant(zoneOffset);
	}

	private final static int[] daysInMonth = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

	public static Instant yearMonthDayToInstant(int year, int month, int day) {

		// Calculate the number of days from 1970 to the given year
		long days = (year - 1970) * 365L + (year - 1969) / 4 - (year - 1901) / 100 + (year - 1601) / 400;

		// Adjust for leap years
		if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) {
			daysInMonth[2] = 29;
		}

		// Add the number of days in each month up to the given month
		for (int i = 1; i < month; i++) {
			days += daysInMonth[i];
		}

		// Add the number of days in the given month
		days += day - 1;

		// Calculate the number of seconds and create an Instant object
		long seconds = days * 24 * 60 * 60;
		return Instant.ofEpochSecond(seconds);
	}

	public static long toEpoch(int year, int month, int day, int hour, int minute, int second) {

		long dateEpoch = ((long) day + (long) (year - ((14 - month) / 12)) + ((long) (year - ((14 - month) / 12))) / 4
			- ((long) (year - ((14 - month) / 12))) / 100 + ((long) (year - ((14 - month) / 12))) / 400
			+ (31 * ((long) (month + (12 * ((14 - month) / 12) - 2)))) / 12) - 719529;

//
//		// Calculate the number of days from 1970 to the given year
//		long days = (year - 1970) * 365L + (year - 1969) / 4 - (year - 1901) / 100 + (year - 1601) / 400;
//
//		// Adjust for leap years
//		if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) {
//			daysInMonth[2] = 29;
//		}
//
//		// Add the number of days in each month up to the given month
//		for (int i = 1; i < month; i++) {
//			days += daysInMonth[i];
//		}
//
//		// Add the number of days in the given month
//		days += day - 1;
//
//		// Calculate the number of seconds
//		long dateEpoch = days * 24 * 60 * 60;

		return dateEpoch + hour * 3600L + minute * 60L + second;

	}

	private static Order handleCommonDatatype(Literal leftLit, Literal rightLit, boolean strict,
											  CoreDatatype.XSD leftCoreDatatype, CoreDatatype.XSD rightCoreDatatype,
											  CoreDatatype.XSD commonDatatype) {
		if (commonDatatype == CoreDatatype.XSD.DOUBLE) {
			return Order.from(Double.compare(leftLit.doubleValue(), rightLit.doubleValue()));
		} else if (commonDatatype == CoreDatatype.XSD.FLOAT) {
			return Order.from(Float.compare(leftLit.floatValue(), rightLit.floatValue()));
		} else if (commonDatatype == CoreDatatype.XSD.DECIMAL) {
			return Order.from(leftLit.decimalValue().compareTo(rightLit.decimalValue()));
		} else if (commonDatatype.isIntegerDatatype()) {
			return Order.from(leftLit.integerValue().compareTo(rightLit.integerValue()));
		} else if (commonDatatype == CoreDatatype.XSD.BOOLEAN) {
			return Order.from(Boolean.compare(leftLit.booleanValue(), rightLit.booleanValue()));
		} else if (commonDatatype.isCalendarDatatype()) {

//			if (commonDatatype == CoreDatatype.XSD.DATETIME) {
//				Instant leftInstant = xmlGregorianCalendarToInstant(leftLit.calendarValue());
//				Instant rightInstant = xmlGregorianCalendarToInstant(rightLit.calendarValue());
//
//				if (leftInstant != null && rightInstant != null) {
//					return Order.from(leftInstant.compareTo(rightInstant));
//				}
//			} else if (commonDatatype == CoreDatatype.XSD.DATE) {
//				Instant leftInstant = xmlGregorianCalendarDateToInstant(leftLit.calendarValue());
//				Instant rightInstant = xmlGregorianCalendarDateToInstant(rightLit.calendarValue());
//
//				if (leftInstant != null && rightInstant != null) {
//					return Order.from(leftInstant.compareTo(rightInstant));
//				}
//			}

			XMLGregorianCalendar left = leftLit.calendarValue();
			XMLGregorianCalendar right = rightLit.calendarValue();

			int compare = compareXMLGregorianCalendar(leftCoreDatatype, rightCoreDatatype, left, right, leftLit, rightLit);

			// Note: XMLGregorianCalendar.compare() returns compatible values (-1, 0, 1) but INDETERMINATE
			// needs special treatment
			if (compare == DatatypeConstants.INDETERMINATE) {
				// If we compare two CoreDatatype.XSD:dateTime we should use the specific comparison specified in SPARQL
				// 1.1
				if (leftCoreDatatype == CoreDatatype.XSD.DATETIME && rightCoreDatatype == CoreDatatype.XSD.DATETIME) {
					return Order.incompatibleValueExpression;
				}
			} else {
				return Order.from(compare);
			}

		} else if (!strict && commonDatatype.isDurationDatatype()) {
			Duration left = XMLDatatypeUtil.parseDuration(leftLit.getLabel());
			Duration right = XMLDatatypeUtil.parseDuration(rightLit.getLabel());
			int compare = left.compare(right);
			if (compare != DatatypeConstants.INDETERMINATE) {
				return Order.from(compare);
			} else {
				return otherCases(leftLit, rightLit, leftCoreDatatype, rightCoreDatatype, false, false);
			}

		} else if (commonDatatype == CoreDatatype.XSD.STRING) {
			return Order.from(leftLit.getLabel().compareTo(rightLit.getLabel()));
		}

		return null;
	}

	private static int compareXMLGregorianCalendar(CoreDatatype.XSD leftCoreDatatype,
												   CoreDatatype.XSD rightCoreDatatype, XMLGregorianCalendar left, XMLGregorianCalendar right, Literal leftLit, Literal rightLit) {
		if (leftCoreDatatype == rightCoreDatatype && left.getTimezone() == right.getTimezone()) {
			int leftYear = left.getYear();
			int rightYear = right.getYear();
			if (leftYear > 1971 && rightYear > 1971 && leftYear < 2038 && rightYear < 2038) {

				int compare = Long.compare(
					toEpoch(leftYear, left.getMonth(), left.getDay(), left.getHour(), left.getMinute(),
						left.getSecond()),
					toEpoch(rightYear, right.getMonth(), right.getDay(), right.getHour(), right.getMinute(),
						right.getSecond())
				);
				if (compare != 0) {
					// since the values are different we can assume that we don't need to account for a higher precision
					return compare;
				} else {
					// since the values are equal we need to account for a higher precision
					if (leftLit.getLabel().equals(rightLit.getLabel())) {
						return 0;
					} else {
						return left.compare(right);
					}
				}
			} else {
				// since the values are equal we need to account for a higher precision
				if (leftLit.getLabel().equals(rightLit.getLabel())) {
					return 0;
				} else {
					return left.compare(right);
				}			}
		} else {
			return left.compare(right);
		}
	}

	private static Order otherCases(Literal leftLit, Literal rightLit, CoreDatatype leftCoreDatatype,
									CoreDatatype rightCoreDatatype, boolean leftLangLit, boolean rightLangLit) {
		boolean literalsEqual = leftLit.equals(rightLit);

		if (!literalsEqual) {
			if (!leftLangLit && !rightLangLit && isSupportedDatatype(leftCoreDatatype)
				&& isSupportedDatatype(rightCoreDatatype)) {
				// left and right arguments have incompatible but supported datatypes

				// we need to check that the lexical-to-value mapping for both datatypes succeeds
				if (!XMLDatatypeUtil.isValidValue(leftLit.getLabel(), leftCoreDatatype)) {
					return Order.incompatibleValueExpression;
				}

				if (!XMLDatatypeUtil.isValidValue(rightLit.getLabel(), rightCoreDatatype)) {
					return Order.incompatibleValueExpression;
				}

				boolean leftString = leftCoreDatatype == CoreDatatype.XSD.STRING;
				boolean rightString = rightCoreDatatype == CoreDatatype.XSD.STRING;

				if (leftString != rightString) {
					return Order.incompatibleValueExpression;
				}

				boolean leftNumeric = ((CoreDatatype.XSD) leftCoreDatatype).isNumericDatatype();
				boolean rightNumeric = ((CoreDatatype.XSD) rightCoreDatatype).isNumericDatatype();

				if (leftNumeric != rightNumeric) {
					return Order.incompatibleValueExpression;
				}

				boolean leftDate = ((CoreDatatype.XSD) leftCoreDatatype).isCalendarDatatype();
				boolean rightDate = ((CoreDatatype.XSD) rightCoreDatatype).isCalendarDatatype();

				if (leftDate != rightDate) {
					return Order.incompatibleValueExpression;
				}
			} else if (!leftLangLit && !rightLangLit) {
				// For literals with unsupported datatypes we don't know if their values are equal
				return Order.incompatibleValueExpression;
			}
		}

		if (literalsEqual) {
			return Order.equal;
		}
		return Order.notEqual;
	}

	private static CoreDatatype.XSD getCommonDatatype(boolean strict, CoreDatatype.XSD leftCoreDatatype,
													  CoreDatatype.XSD rightCoreDatatype) {
		if (leftCoreDatatype == null || rightCoreDatatype == null) {
			return null;
		}

		if (leftCoreDatatype == rightCoreDatatype) {
			return leftCoreDatatype;
		}

		if (leftCoreDatatype.isNumericDatatype() && rightCoreDatatype.isNumericDatatype()) {
			return getCommonNumericDatatype(leftCoreDatatype, rightCoreDatatype);
		}

		if (!strict) {
			if (leftCoreDatatype.isCalendarDatatype() && rightCoreDatatype.isCalendarDatatype()) {
				return CoreDatatype.XSD.DATETIME;
			} else if (leftCoreDatatype.isDurationDatatype() && rightCoreDatatype.isDurationDatatype()) {
				return CoreDatatype.XSD.DURATION;
			}
		}

		return null;
	}

	private static CoreDatatype.XSD getCommonNumericDatatype(CoreDatatype.XSD leftCoreDatatype,
															 CoreDatatype.XSD rightCoreDatatype) {
		if (leftCoreDatatype == CoreDatatype.XSD.DOUBLE || rightCoreDatatype == CoreDatatype.XSD.DOUBLE) {
			return CoreDatatype.XSD.DOUBLE;
		}
		if (leftCoreDatatype == CoreDatatype.XSD.FLOAT || rightCoreDatatype == CoreDatatype.XSD.FLOAT) {
			return CoreDatatype.XSD.FLOAT;
		}
		if (leftCoreDatatype == CoreDatatype.XSD.DECIMAL || rightCoreDatatype == CoreDatatype.XSD.DECIMAL) {
			return CoreDatatype.XSD.DECIMAL;
		}
		return CoreDatatype.XSD.INTEGER;
	}

	/**
	 * Checks whether the supplied value is a "plain literal". A "plain literal" is a literal with no datatype and
	 * optionally a language tag.
	 *
	 * @see <a href="http://www.w3.org/TR/2004/REC-rdf-concepts-20040210/#dfn-plain-literal">RDF Literal
	 * Documentation</a>
	 */
	public static boolean isPlainLiteral(Value v) {
		if (v.isLiteral()) {
			return isPlainLiteral(((Literal) v));
		}
		return false;
	}

	public static boolean isPlainLiteral(Literal l) {
		CoreDatatype coreDatatype = l.getCoreDatatype();
		return coreDatatype == CoreDatatype.XSD.STRING || coreDatatype == CoreDatatype.RDF.LANGSTRING;
	}

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

//	public static boolean isPlainLiteral(Literal l) {
//		return l.getCoreDatatype().filter(d -> d == CoreDatatype.XSD.STRING).isPresent();
////		return l.getCoreDatatype().orElse(null) == CoreDatatype.XSD.STRING;
//	}

	/**
	 * Checks whether the supplied literal is a "simple literal". A "simple literal" is a literal with no language tag
	 * and the datatype {@link CoreDatatype.XSD#STRING}.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#simple_literal">SPARQL Simple Literal Documentation</a>
	 */
	public static boolean isSimpleLiteral(Literal l) {
		return l.getCoreDatatype() == CoreDatatype.XSD.STRING;
	}

	/**
	 * Checks whether the supplied literal is a "simple literal". A "simple literal" is a literal with no language tag
	 * and the datatype {@link CoreDatatype.XSD#STRING}.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#simple_literal">SPARQL Simple Literal Documentation</a>
	 */
	public static boolean isSimpleLiteral(boolean isLang, CoreDatatype datatype) {
		return datatype == CoreDatatype.XSD.STRING;
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
	 * Rules</a>
	 */
	public static boolean compatibleArguments(Literal arg1, Literal arg2) {
		// 1. The arguments are literals typed as CoreDatatype.XSD:string
		// 2. The arguments are language literals with identical language tags
		// 3. The first argument is a language literal and the second
		// argument is a literal typed as CoreDatatype.XSD:string

		return (isSimpleLiteral(arg1) && isSimpleLiteral(arg2))
			|| (Literals.isLanguageLiteral(arg1) && Literals.isLanguageLiteral(arg2)
			&& arg1.getLanguage().equals(arg2.getLanguage()))
			|| (Literals.isLanguageLiteral(arg1) && isSimpleLiteral(arg2));
	}

	/**
	 * Checks whether the supplied literal is a "string literal". A "string literal" is either a simple literal, a plain
	 * literal with language tag, or a literal with datatype CoreDatatype.XSD:string.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#func-string">SPARQL Functions on Strings Documentation</a>
	 */
	public static boolean isStringLiteral(Literal l) {
		return l.getCoreDatatype() == CoreDatatype.XSD.STRING || l.getCoreDatatype() == CoreDatatype.RDF.LANGSTRING;
	}

	private static boolean isSupportedDatatype(CoreDatatype datatype) {
		return datatype != null && datatype.isXSDDatatype()
			&& (datatype == CoreDatatype.XSD.STRING || ((CoreDatatype.XSD) datatype).isNumericDatatype()
			|| ((CoreDatatype.XSD) datatype).isCalendarDatatype());
	}

	public enum Result {
		_true(true),
		_false(false),
		incompatibleValueExpression(),
		illegalArgument();

		static Result fromBoolean(boolean b) {
			if (b) {
				return _true;
			}
			return _false;
		}

		private final boolean value;
		private final boolean isIncompatible;

		Result(boolean value) {
			this.value = value;
			isIncompatible = false;
		}

		Result() {
			isIncompatible = true;
			value = false;
		}

		public boolean orElse(boolean alternative) {
			if (this == incompatibleValueExpression) {
				return alternative;
			} else if (this == illegalArgument) {
				throw new IllegalStateException("IllegalArgument needs to be handled");
			}
			return value;
		}
	}

	enum Order {
		smaller(-1),
		greater(1),
		equal(0),
		notEqual(0),
		incompatibleValueExpression(0),
		illegalArgument(0);

		private final int value;

		Order(int value) {
			this.value = value;
		}

		public static Order from(int value) {
			if (value < 0) {
				return smaller;
			}
			if (value > 0) {
				return greater;
			}
			return equal;
		}

		public int asInt() {
			if (!isValid() && this != notEqual) {
				throw new IllegalStateException();
			}
			return value;
		}

		public boolean isValid() {
			return !(this == incompatibleValueExpression || this == illegalArgument);
		}

		public Result toResult(CompareOp operator) {
			if (!isValid()) {
				if (this == incompatibleValueExpression) {
					return Result.incompatibleValueExpression;
				}
				if (this == illegalArgument) {
					return Result.illegalArgument;
				}
			}

			if (this == notEqual) {
				switch (operator) {
					case EQ:
						return Result._false;
					case NE:
						return Result._true;
					case LT:
					case LE:
					case GE:
					case GT:
						return Result.incompatibleValueExpression;
					default:
						return Result.illegalArgument;
				}
			}

			switch (operator) {
				case LT:
					return Result.fromBoolean(value < 0);
				case LE:
					return Result.fromBoolean(value <= 0);
				case EQ:
					return Result.fromBoolean(value == 0);
				case NE:
					return Result.fromBoolean(value != 0);
				case GE:
					return Result.fromBoolean(value >= 0);
				case GT:
					return Result.fromBoolean(value > 0);
				default:
					return Result.illegalArgument;
			}
		}
	}
}
