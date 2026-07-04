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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * The SPARQL built-in {@link Function} NOW, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-now">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class Now implements Function {

	@Override
	public String getURI() {
		return "NOW";
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 0) {
			throw new ValueExprEvaluationException("NOW requires 0 argument, got " + args.length);
		}

		Calendar cal = Calendar.getInstance();

		Date now = cal.getTime();
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(now);
		try {
			XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);

			return valueFactory.createLiteral(date);
		} catch (DatatypeConfigurationException e) {
			throw new ValueExprEvaluationException(e);
		}

	}

}
