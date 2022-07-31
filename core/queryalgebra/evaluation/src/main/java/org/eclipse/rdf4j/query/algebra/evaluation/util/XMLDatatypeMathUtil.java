/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * A utility class for evaluation of extended "mathematical" expressions on RDF literals. They do not work only on
 * numeric literals but also on durations
 *
 * @author Thomas Pellissier Tanon
 */
public class XMLDatatypeMathUtil {

	/**
	 * Computes the result of applying the supplied math operator on the supplied left and right operand.
	 *
	 * @param leftLit  a datatype literal
	 * @param rightLit a datatype literal
	 * @param op       a mathematical operator, as definied by MathExpr.MathOp.
	 * @return a datatype literal
	 */
	public static Literal compute(Literal leftLit, Literal rightLit, MathOp op) throws ValueExprEvaluationException {
		CoreDatatype.XSD leftDatatype = leftLit.getCoreDatatype().asXSDDatatype().orElse(null);
		CoreDatatype.XSD rightDatatype = rightLit.getCoreDatatype().asXSDDatatype().orElse(null);

		if (leftDatatype != null && rightDatatype != null) {
			if (leftDatatype.isNumericDatatype() && rightDatatype.isNumericDatatype()) {
				return MathUtil.compute(leftLit, rightLit, op);
			} else if (leftDatatype.isDurationDatatype() && rightDatatype.isDurationDatatype()) {
				return operationsBetweenDurations(leftLit, rightLit, op);
			} else if (leftDatatype.isDecimalDatatype() && rightDatatype.isDurationDatatype()) {
				return operationsBetweenDurationAndDecimal(rightLit, leftLit, op);
			} else if (leftDatatype.isDurationDatatype() && rightDatatype.isDecimalDatatype()) {
				return operationsBetweenDurationAndDecimal(leftLit, rightLit, op);
			} else if (leftDatatype.isCalendarDatatype() && rightDatatype.isDurationDatatype()) {
				return operationsBetweenCalendarAndDuration(leftLit, rightLit, op);
			} else if (leftDatatype.isDurationDatatype() && rightDatatype.isCalendarDatatype()) {
				return operationsBetweenDurationAndCalendar(leftLit, rightLit, op);
			}
		}

		throw new ValueExprEvaluationException("Mathematical operators are not supported on these operands");

	}

	private static Literal operationsBetweenDurations(Literal leftLit, Literal rightLit, MathOp op) {
		Duration left = XMLDatatypeUtil.parseDuration(leftLit.getLabel());
		Duration right = XMLDatatypeUtil.parseDuration(rightLit.getLabel());
		try {
			switch (op) {
			case PLUS:
				// op:add-yearMonthDurations and op:add-dayTimeDurations
				return buildLiteral(left.add(right));
			case MINUS:
				// op:subtract-yearMonthDurations and op:subtract-dayTimeDurations
				return buildLiteral(left.subtract(right));
			case MULTIPLY:
				throw new ValueExprEvaluationException("Multiplication is not defined on xsd:duration.");
			case DIVIDE:
				throw new ValueExprEvaluationException("Division is not defined on xsd:duration.");
			default:
				throw new IllegalArgumentException("Unknown operator: " + op);
			}
		} catch (IllegalStateException e) {
			throw new ValueExprEvaluationException(e);
		}
	}

	private static Literal operationsBetweenDurationAndDecimal(Literal durationLit, Literal decimalLit, MathOp op) {
		Duration duration = XMLDatatypeUtil.parseDuration(durationLit.getLabel());

		try {
			if (op == MathOp.MULTIPLY) {
				// op:multiply-dayTimeDuration and op:multiply-yearMonthDuration
				return buildLiteral(duration.multiply(decimalLit.decimalValue()));
			} else {
				throw new ValueExprEvaluationException(
						"Only multiplication is defined between xsd:decimal and xsd:duration.");
			}
		} catch (IllegalStateException e) {
			throw new ValueExprEvaluationException(e);
		}
	}

	private static Literal operationsBetweenCalendarAndDuration(Literal calendarLit, Literal durationLit, MathOp op) {
		XMLGregorianCalendar calendar = (XMLGregorianCalendar) calendarLit.calendarValue().clone();
		Duration duration = XMLDatatypeUtil.parseDuration(durationLit.getLabel());

		try {
			switch (op) {
			case PLUS:
				// op:add-yearMonthDuration-to-dateTime and op:add-dayTimeDuration-to-dateTime and
				// op:add-yearMonthDuration-to-date and op:add-dayTimeDuration-to-date and
				// op:add-dayTimeDuration-to-time
				calendar.add(duration);
				return SimpleValueFactory.getInstance().createLiteral(calendar);
			case MINUS:
				// op:subtract-yearMonthDuration-from-dateTime and op:subtract-dayTimeDuration-from-dateTime and
				// op:subtract-yearMonthDuration-from-date and op:subtract-dayTimeDuration-from-date and
				// op:subtract-dayTimeDuration-from-time
				calendar.add(duration.negate());
				return SimpleValueFactory.getInstance().createLiteral(calendar);
			case MULTIPLY:
				throw new ValueExprEvaluationException(
						"Multiplication is not defined between xsd:duration and calendar values.");
			case DIVIDE:
				throw new ValueExprEvaluationException(
						"Division is not defined between xsd:duration and calendar values.");
			default:
				throw new IllegalArgumentException("Unknown operator: " + op);
			}
		} catch (IllegalStateException e) {
			throw new ValueExprEvaluationException(e);
		}
	}

	private static Literal operationsBetweenDurationAndCalendar(Literal durationLit, Literal calendarLit, MathOp op) {
		Duration duration = XMLDatatypeUtil.parseDuration(durationLit.getLabel());
		XMLGregorianCalendar calendar = (XMLGregorianCalendar) calendarLit.calendarValue().clone();

		try {
			if (op == MathOp.PLUS) {
				// op:add-yearMonthDuration-to-dateTime and op:add-dayTimeDuration-to-dateTime and
				// op:add-yearMonthDuration-to-date and op:add-dayTimeDuration-to-date and
				// op:add-dayTimeDuration-to-time
				calendar.add(duration);
				return SimpleValueFactory.getInstance().createLiteral(calendar);
			} else {
				throw new ValueExprEvaluationException(
						"Only addition is defined between xsd:duration and calendar datatypes.");
			}
		} catch (IllegalStateException e) {
			throw new ValueExprEvaluationException(e);
		}
	}

	private static Literal buildLiteral(Duration duration) {
		return SimpleValueFactory.getInstance().createLiteral(duration.toString(), getDatatypeForDuration(duration));
	}

	private static CoreDatatype.XSD getDatatypeForDuration(Duration duration) {
		// Could not be implemented with Duration.getXMLSchemaType that is too strict ("P1Y" is considered invalid)

		boolean yearSet = duration.isSet(DatatypeConstants.YEARS);
		boolean monthSet = duration.isSet(DatatypeConstants.MONTHS);
		boolean daySet = duration.isSet(DatatypeConstants.DAYS);
		boolean hourSet = duration.isSet(DatatypeConstants.HOURS);
		boolean minuteSet = duration.isSet(DatatypeConstants.MINUTES);
		boolean secondSet = duration.isSet(DatatypeConstants.SECONDS);

		if (!yearSet && !monthSet) {
			return CoreDatatype.XSD.DAYTIMEDURATION;
		}
		if (!daySet && !hourSet && !minuteSet && !secondSet) {
			return CoreDatatype.XSD.YEARMONTHDURATION;
		}
		return CoreDatatype.XSD.DURATION;
	}
}
