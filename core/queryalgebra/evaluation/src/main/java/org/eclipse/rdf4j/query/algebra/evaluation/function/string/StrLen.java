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
package org.eclipse.rdf4j.query.algebra.evaluation.function.string;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * The SPARQL built-in {@link Function} STRLEN, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-strlen">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class StrLen implements Function {

	@Override
	public String getURI() {
		return FN.STRING_LENGTH.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("STRLEN requires 1 argument, got " + args.length);
		}

		Value argValue = args[0];
		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			// strlen function accepts only string literals
			if (QueryEvaluationUtility.isStringLiteral(literal)) {

				// TODO we jump through some hoops here to get an xsd:integer
				// literal. Shouldn't createLiteral(int) return an xsd:integer
				// rather than an xsd:int?
				Integer length = literal.getLabel().length();
				return valueFactory.createLiteral(length.toString(), XSD.INTEGER);
			} else {
				throw new ValueExprEvaluationException("unexpected input value for strlen function: " + argValue);
			}
		} else {
			throw new ValueExprEvaluationException("unexpected input value for strlen function: " + argValue);
		}
	}

}
