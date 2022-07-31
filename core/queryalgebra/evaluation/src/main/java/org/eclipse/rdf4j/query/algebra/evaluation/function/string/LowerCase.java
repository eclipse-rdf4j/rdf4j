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

import java.util.Optional;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * The SPARQL built-in {@link Function} LCASE, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-lcase">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class LowerCase implements Function {

	@Override
	public String getURI() {
		return FN.LOWER_CASE.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("LCASE requires exactly 1 argument, got " + args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal) args[0];

			// LowerCase function accepts only string literals.
			if (QueryEvaluationUtility.isStringLiteral(literal)) {
				String lexicalValue = literal.getLabel().toLowerCase();
				Optional<String> language = literal.getLanguage();

				if (language.isPresent()) {
					return valueFactory.createLiteral(lexicalValue, language.get());
				} else if (XSD.STRING.equals(literal.getDatatype())) {
					return valueFactory.createLiteral(lexicalValue, XSD.STRING);
				} else {
					return valueFactory.createLiteral(lexicalValue);
				}
			} else {
				throw new ValueExprEvaluationException("unexpected input value for function: " + args[0]);
			}
		} else {
			throw new ValueExprEvaluationException("unexpected input value for function: " + args[0]);
		}

	}

}
