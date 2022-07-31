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
package org.eclipse.rdf4j.query.algebra.evaluation.function.rdfterm;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * The SPARQL built-in {@link Function} STRLANG, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-strlang">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class StrLang implements Function {

	@Override
	public String getURI() {
		return "STRLANG";
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 2) {
			throw new ValueExprEvaluationException("STRLANG requires 2 arguments, got " + args.length);
		}

		Value lexicalValue = args[0];
		Value languageValue = args[1];

		if (QueryEvaluationUtility.isSimpleLiteral(lexicalValue)) {
			Literal lit = (Literal) lexicalValue;

			if (languageValue instanceof Literal) {
				return valueFactory.createLiteral(lit.getLabel(), ((Literal) languageValue).getLabel());
			} else {
				throw new ValueExprEvaluationException("illegal value for operand: " + languageValue);
			}
		} else {
			throw new ValueExprEvaluationException("illegal value for operand: " + lexicalValue);
		}

	}

}
