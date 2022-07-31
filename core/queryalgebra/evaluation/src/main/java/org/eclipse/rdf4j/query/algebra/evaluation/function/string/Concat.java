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
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * The SPARQL built-in {@link Function} CONCAT, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-concat">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class Concat implements Function {

	@Override
	public String getURI() {
		return FN.CONCAT.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length == 0) {
			throw new ValueExprEvaluationException("CONCAT requires at least 1 argument, got " + args.length);
		}

		StringBuilder concatBuilder = new StringBuilder();
		String commonLanguageTag = null;
		boolean useLanguageTag = true;

		for (Value arg : args) {
			if (arg instanceof Literal) {
				Literal lit = (Literal) arg;

				if (!QueryEvaluationUtility.isStringLiteral(lit)) {
					throw new ValueExprEvaluationException("unexpected datatype for CONCAT operand: " + lit);
				}

				// verify that every literal argument has the same language tag. If
				// not, the operator result should not use a language tag.
				if (useLanguageTag && Literals.isLanguageLiteral(lit)) {
					if (commonLanguageTag == null) {
						commonLanguageTag = lit.getLanguage().get();
					} else if (!commonLanguageTag.equals(lit.getLanguage().orElse(null))) {
						commonLanguageTag = null;
						useLanguageTag = false;
					}
				} else {
					useLanguageTag = false;
				}

				concatBuilder.append(lit.getLabel());
			} else {
				throw new ValueExprEvaluationException("unexpected argument type for CONCAT operator: " + arg);
			}
		}

		Literal result;

		if (useLanguageTag) {
			result = valueFactory.createLiteral(concatBuilder.toString(), commonLanguageTag);
		} else {
			result = valueFactory.createLiteral(concatBuilder.toString());
		}

		return result;

	}

}
