/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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
 * The SPARQL built-in {@link Function} HOURS, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-hours">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class Hours implements Function {

	@Override
	public String getURI() {
		return FN.HOURS_FROM_DATETIME.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("HOURS requires 1 argument, got " + args.length);
		}

		Value argValue = args[0];
		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			IRI datatype = literal.getDatatype();

			if (datatype != null && XMLDatatypeUtil.isCalendarDatatype(datatype)) {
				try {
					XMLGregorianCalendar calValue = literal.calendarValue();

					int hours = calValue.getHour();
					if (DatatypeConstants.FIELD_UNDEFINED != hours) {
						return valueFactory.createLiteral(String.valueOf(hours), XSD.INTEGER);
					} else {
						throw new ValueExprEvaluationException("can not determine hours from value: " + argValue);
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
