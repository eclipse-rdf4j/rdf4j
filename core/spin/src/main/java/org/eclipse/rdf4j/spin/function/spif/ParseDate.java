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
package org.eclipse.rdf4j.spin.function.spif;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.BinaryFunction;

public class ParseDate extends BinaryFunction {

	private static final DatatypeFactory datatypeFactory;

	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new Error("Could not instantiate javax.xml.datatype.DatatypeFactory", e);
		}
	}

	@Override
	public String getURI() {
		return SPIF.PARSE_DATE_FUNCTION.toString();
	}

	@Override
	protected Value evaluate(ValueFactory valueFactory, Value arg1, Value arg2) throws ValueExprEvaluationException {
		if (!(arg1 instanceof Literal) || !(arg2 instanceof Literal)) {
			throw new ValueExprEvaluationException("Both arguments must be literals");
		}
		Literal value = (Literal) arg1;
		Literal format = (Literal) arg2;

		FieldAwareGregorianCalendar cal = new FieldAwareGregorianCalendar();

		SimpleDateFormat formatter = new SimpleDateFormat(format.getLabel());
		formatter.setCalendar(cal);
		try {
			formatter.parse(value.getLabel());
		} catch (ParseException e) {
			throw new ValueExprEvaluationException(e);
		}

		XMLGregorianCalendar xmlCal = datatypeFactory.newXMLGregorianCalendar(cal);
		if (!cal.isDateSet()) {
			xmlCal.setYear(DatatypeConstants.FIELD_UNDEFINED);
			xmlCal.setMonth(DatatypeConstants.FIELD_UNDEFINED);
			xmlCal.setDay(DatatypeConstants.FIELD_UNDEFINED);
		}
		if (!cal.isTimeSet()) {
			xmlCal.setHour(DatatypeConstants.FIELD_UNDEFINED);
			xmlCal.setMinute(DatatypeConstants.FIELD_UNDEFINED);
			xmlCal.setSecond(DatatypeConstants.FIELD_UNDEFINED);
		}
		if (!cal.isMillisecondSet()) {
			xmlCal.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
		}

		String dateValue = xmlCal.toXMLFormat();
		QName dateType = xmlCal.getXMLSchemaType();
		if (!cal.isTimezoneSet()) {
			int len = dateValue.length();
			if (dateValue.endsWith("Z")) {
				dateValue = dateValue.substring(0, len - 1);
			} else if (dateValue.charAt(len - 6) == '+' || dateValue.charAt(len - 6) == '-') {
				dateValue = dateValue.substring(0, len - 6);
			}
		}

		return valueFactory.createLiteral(dateValue, XMLDatatypeUtil.qnameToCoreDatatype(dateType));
	}

	static final class FieldAwareGregorianCalendar extends GregorianCalendar {

		Set<Integer> fieldsSet = new HashSet<>();

		@Override
		public void set(int field, int value) {
			super.set(field, value);
			fieldsSet.add(field);
		}

		boolean isDateSet() {
			return fieldsSet.contains(Calendar.YEAR) || fieldsSet.contains(Calendar.MONTH)
					|| fieldsSet.contains(Calendar.DAY_OF_MONTH) || fieldsSet.contains(Calendar.DAY_OF_WEEK)
					|| fieldsSet.contains(Calendar.DAY_OF_WEEK_IN_MONTH) || fieldsSet.contains(Calendar.DAY_OF_YEAR);
		}

		boolean isTimeSet() {
			return fieldsSet.contains(Calendar.HOUR_OF_DAY) || fieldsSet.contains(Calendar.HOUR)
					|| fieldsSet.contains(Calendar.MINUTE) || fieldsSet.contains(Calendar.SECOND);
		}

		boolean isMillisecondSet() {
			return fieldsSet.contains(Calendar.MILLISECOND);
		}

		boolean isTimezoneSet() {
			return fieldsSet.contains(Calendar.ZONE_OFFSET);
		}
	}
}
