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
package org.eclipse.rdf4j.query.algebra.evaluation.function.datetime;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * The SPARQL built-in {@link Function} TIMEZONE, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-timezone">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class Timezone implements Function {

	@Override
	public String getURI() {
		return FN.TIMEZONE_FROM_DATETIME.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("TIMEZONE requires 1 argument, got " + args.length);
		}

		Value argValue = args[0];
		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			IRI datatype = literal.getDatatype();

			if (datatype != null && XMLDatatypeUtil.isCalendarDatatype(datatype)) {
				try {
					XMLGregorianCalendar calValue = literal.calendarValue();

					int timezoneOffset = calValue.getTimezone();

					if (DatatypeConstants.FIELD_UNDEFINED != timezoneOffset) {
						// TODO creating xsd:dayTimeDuration lexical representation
						// manually. Surely there is a better way to do this?
						int minutes = Math.abs(timezoneOffset);
						int hours = minutes / 60;
						minutes = minutes - (hours * 60);

						StringBuilder tzDuration = new StringBuilder();
						if (timezoneOffset < 0) {
							tzDuration.append("-");
						}
						tzDuration.append("PT");
						if (hours > 0) {
							tzDuration.append(hours + "H");
						}
						if (minutes > 0) {
							tzDuration.append(minutes + "M");
						}
						if (timezoneOffset == 0) {
							tzDuration.append("0S");
						}
						return valueFactory.createLiteral(tzDuration.toString(), XSD.DAYTIMEDURATION);
					} else {
						throw new ValueExprEvaluationException("can not determine timezone from value: " + argValue);
					}
				} catch (IllegalArgumentException e) {
					throw new ValueExprEvaluationException("illegal calendar value: " + argValue);
				}
			} else {
				throw new ValueExprEvaluationException("unexpected input value for function: " + argValue);
			}
		} else {
			throw new ValueExprEvaluationException("unexpected input value for function: " + args[0]);
		}
	}

}
