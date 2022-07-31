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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * The SPARQL built-in {@link Function} TZ, as defined in <a href="http://www.w3.org/TR/sparql11-query/#func-tz">SPARQL
 * Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class Tz implements Function {

	@Override
	public String getURI() {
		return "TZ";
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("TZ requires 1 argument, got " + args.length);
		}

		Value argValue = args[0];
		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			IRI datatype = literal.getDatatype();

			if (datatype != null && XMLDatatypeUtil.isCalendarDatatype(datatype)) {
				String lexValue = literal.getLabel();

				Pattern pattern = Pattern.compile("Z|[+-]\\d\\d:\\d\\d");
				Matcher m = pattern.matcher(lexValue);

				String timeZone = "";
				if (m.find()) {
					timeZone = m.group();
				}

				return valueFactory.createLiteral(timeZone);
			} else {
				throw new ValueExprEvaluationException("unexpected input value for function: " + argValue);
			}
		} else {
			throw new ValueExprEvaluationException("unexpected input value for function: " + args[0]);
		}
	}

}
