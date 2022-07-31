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
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * A {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function} that tries to cast its argument to an
 * <var>xsd:dateTime</var>.
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 */
public class DateTimeCast extends CastFunction {

	@Override
	protected IRI getXsdDatatype() {
		return XSD.DATETIME;
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return XMLDatatypeUtil.isValidDateTime(lexicalValue);
	}

	@Override
	protected Literal convert(ValueFactory vf, Value value) throws ValueExprEvaluationException {
		if (value instanceof Literal) {
			Literal literal = (Literal) value;
			IRI datatype = literal.getDatatype();

			if (datatype.equals(XSD.DATE)) {
				// If ST is xs:date, then let SYR be eg:convertYearToString(
				// fn:year-from-date( SV )), let SMO be eg:convertTo2CharString(
				// fn:month-from-date( SV )), let SDA be eg:convertTo2CharString(
				// fn:day-from-date( SV )) and let STZ be eg:convertTZtoString(
				// fn:timezone-from-date( SV )); TV is xs:dateTime( fn:concat(
				// SYR , '-', SMO , '-', SDA , 'T00:00:00 ', STZ ) ).
				try {
					XMLGregorianCalendar calValue = literal.calendarValue();

					int year = calValue.getYear();
					int month = calValue.getMonth();
					int day = calValue.getDay();
					int timezoneOffset = calValue.getTimezone();

					if (DatatypeConstants.FIELD_UNDEFINED != year && DatatypeConstants.FIELD_UNDEFINED != month
							&& DatatypeConstants.FIELD_UNDEFINED != day) {
						StringBuilder dtBuilder = new StringBuilder();
						dtBuilder.append(year);
						dtBuilder.append("-");
						if (month < 10) {
							dtBuilder.append("0");
						}
						dtBuilder.append(month);
						dtBuilder.append("-");
						if (day < 10) {
							dtBuilder.append("0");
						}
						dtBuilder.append(day);
						dtBuilder.append("T00:00:00");
						if (DatatypeConstants.FIELD_UNDEFINED != timezoneOffset) {
							int minutes = Math.abs(timezoneOffset);
							int hours = minutes / 60;
							minutes = minutes - (hours * 60);
							if (timezoneOffset > 0) {
								dtBuilder.append("+");
							} else {
								dtBuilder.append("-");
							}
							if (hours < 10) {
								dtBuilder.append("0");
							}
							dtBuilder.append(hours);
							dtBuilder.append(":");
							if (minutes < 10) {
								dtBuilder.append("0");
							}
							dtBuilder.append(minutes);
						}

						return vf.createLiteral(dtBuilder.toString(), XSD.DATETIME);
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
}
