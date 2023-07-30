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
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import static javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED;

import static org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil.isValidDate;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * A {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function} that tries to cast its argument to an
 * <var>xsd:date</var>.
 */
public class DateCast extends CastFunction {

	private static final String ZERO = "0";

	@Override
	protected CoreDatatype.XSD getCoreXsdDatatype() {
		return CoreDatatype.XSD.DATE;
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return isValidDate(lexicalValue);
	}

	@Override
	protected Literal convert(ValueFactory vf, Value value) throws ValueExprEvaluationException {
		if (value instanceof Literal) {
			Literal literal = (Literal) value;
			CoreDatatype datatype = literal.getCoreDatatype();

			if (CoreDatatype.XSD.STRING == datatype || CoreDatatype.XSD.DATETIME == datatype) {
				try {
					XMLGregorianCalendar calValue = literal.calendarValue();
					int year = calValue.getYear();
					int month = calValue.getMonth();
					int day = calValue.getDay();
					int timezoneOffset = calValue.getTimezone();

					if (FIELD_UNDEFINED != year && FIELD_UNDEFINED != month && FIELD_UNDEFINED != day) {
						StringBuilder builder = new StringBuilder();
						builder.append(year).append("-");
						addZeroIfNeeded(month, builder);
						builder.append(month).append("-");
						addZeroIfNeeded(day, builder);
						builder.append(day);

						if (FIELD_UNDEFINED != timezoneOffset) {
							int minutes = Math.abs(timezoneOffset);
							int hours = minutes / 60;
							minutes = minutes - hours * 60;
							builder.append(timezoneOffset > 0 ? "+" : "-");
							addZeroIfNeeded(hours, builder);
							builder.append(hours);
							builder.append(":");
							addZeroIfNeeded(minutes, builder);
							builder.append(minutes);
						}

						return vf.createLiteral(builder.toString(), CoreDatatype.XSD.DATE);
					} else {
						throw typeError(literal, null);
					}
				} catch (IllegalArgumentException e) {
					throw typeError(literal, e);
				}
			}
		}
		throw typeError(value, null);
	}

	private static void addZeroIfNeeded(int value, StringBuilder builder) {
		if (value < 10) {
			builder.append(ZERO);
		}
	}
}
