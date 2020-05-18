/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.string;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

/**
 * The SPARQL built-in {@link Function} STRAFTER, as defined in
 * <a href="https://www.w3.org/TR/sparql11-query/#func-strafter">SPARQL Query Language for RDF</a>.
 *
 * @author Jeen Broekstra
 */
public class StrAfter implements Function {

	@Override
	public String getURI() {
		return FN.SUBSTRING_AFTER.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 2) {
			throw new ValueExprEvaluationException("Incorrect number of arguments for STRAFTER: " + args.length);
		}

		Value leftArg = args[0];
		Value rightArg = args[1];

		if (leftArg instanceof Literal && rightArg instanceof Literal) {
			Literal leftLit = (Literal) leftArg;
			Literal rightLit = (Literal) rightArg;

			if (QueryEvaluationUtil.compatibleArguments(leftLit, rightLit)) {
				String lexicalValue = leftLit.getLabel();
				String substring = rightLit.getLabel();

				Optional<String> leftLanguage = leftLit.getLanguage();
				IRI leftDt = leftLit.getDatatype();

				int index = lexicalValue.indexOf(substring);

				String substringAfter = "";
				if (index > -1) {
					index += substring.length() - 1;
					substringAfter = lexicalValue.substring(index + 1, lexicalValue.length());
				} else {
					// no match, return empty string with no language or datatype
					leftLanguage = Optional.empty();
					leftDt = null;
				}

				if (leftLanguage.isPresent()) {
					return valueFactory.createLiteral(substringAfter, leftLanguage.get());
				} else if (leftDt != null) {
					return valueFactory.createLiteral(substringAfter, leftDt);
				} else {
					return valueFactory.createLiteral(substringAfter);
				}
			} else {
				throw new ValueExprEvaluationException(
						"incompatible operands for STRAFTER: " + leftArg + ", " + rightArg);
			}
		} else {
			throw new ValueExprEvaluationException("incompatible operands for STRAFTER: " + leftArg + ", " + rightArg);
		}
	}
}
