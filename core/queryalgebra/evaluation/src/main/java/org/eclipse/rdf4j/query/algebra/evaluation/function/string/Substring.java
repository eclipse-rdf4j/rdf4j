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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * The SPARQL built-in {@link Function} SUBSTR, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-substr">SPARQL Query Language for RDF</a>.
 *
 * @author Jeen Broekstra
 */
public class Substring implements Function {

	@Override
	public String getURI() {
		return FN.SUBSTRING.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length < 2 || args.length > 3) {
			throw new ValueExprEvaluationException("Incorrect number of arguments for SUBSTR: " + args.length);
		}

		Value argValue = args[0];
		Value startIndexValue = args[1];
		Value lengthValue = null;
		if (args.length > 2) {
			lengthValue = args[2];
		}

		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			// substr function accepts string literals only.
			if (QueryEvaluationUtility.isStringLiteral(literal)) {
				String lexicalValue = literal.getLabel();

				// determine start index.
				int startIndex = 0;
				if (startIndexValue instanceof Literal) {
					// If it is not an int we need to round it per spec
					int startLiteral = intFromLiteral((Literal) startIndexValue);

					try {
						// xpath:substring startIndex is 1-based.

						startIndex = startLiteral - 1;
					} catch (NumberFormatException e) {
						throw new ValueExprEvaluationException(
								"illegal start index value (expected int value): " + startIndexValue);
					}
				} else if (startIndexValue != null) {
					throw new ValueExprEvaluationException(
							"illegal start index value (expected literal value): " + startIndexValue);
				}

				// optionally convert supplied length expression to an end index for
				// the substring.
				int endIndex = lexicalValue.length();
				if (lengthValue instanceof Literal) {
					try {
						int length = intFromLiteral((Literal) lengthValue);
						if (length < 1) {
							return convert("", literal, valueFactory);
						}
						endIndex = Math.min(startIndex + length, endIndex);
						if (endIndex < 0) {
							return convert("", literal, valueFactory);
						}
					} catch (NumberFormatException e) {
						throw new ValueExprEvaluationException(
								"illegal length value (expected int value): " + lengthValue);
					}
				} else if (lengthValue != null) {
					throw new ValueExprEvaluationException(
							"illegal length value (expected literal value): " + lengthValue);
				}

				try {
					startIndex = Math.max(startIndex, 0);
					lexicalValue = lexicalValue.substring(startIndex, endIndex);
					return convert(lexicalValue, literal, valueFactory);
				} catch (IndexOutOfBoundsException e) {
					throw new ValueExprEvaluationException(
							"could not determine substring, index out of bounds " + startIndex + "length:" + endIndex,
							e);
				}
			} else {
				throw new ValueExprEvaluationException("unexpected input value for function substring: " + argValue);
			}
		} else {
			throw new ValueExprEvaluationException("unexpected input value for function substring: " + argValue);
		}
	}

	private Literal convert(String lexicalValue, Literal literal, ValueFactory valueFactory) {
		Optional<String> language = literal.getLanguage();
		if (language.isPresent()) {
			return valueFactory.createLiteral(lexicalValue, language.get());
		} else if (XSD.STRING.equals(literal.getDatatype())) {
			return valueFactory.createLiteral(lexicalValue, XSD.STRING);
		} else {
			return valueFactory.createLiteral(lexicalValue);
		}
	}

	public static int intFromLiteral(Literal literal) throws ValueExprEvaluationException {

		IRI datatype = literal.getDatatype();

		// function accepts only numeric literals
		if (datatype != null && XMLDatatypeUtil.isNumericDatatype(datatype)) {
			if (XMLDatatypeUtil.isIntegerDatatype(datatype)) {
				return literal.intValue();
			} else {
				throw new ValueExprEvaluationException("unexpected datatype for function operand: " + literal);
			}
		} else {
			throw new ValueExprEvaluationException("unexpected input value for function: " + literal);
		}
	}
}
