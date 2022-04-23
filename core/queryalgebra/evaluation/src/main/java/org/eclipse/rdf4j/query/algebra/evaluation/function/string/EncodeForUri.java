/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.string;

import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * The SPARQL built-in {@link Function} ENCODE_FOR_URI, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-encode">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 */
public class EncodeForUri implements Function {

	@Override
	public String getURI() {
		return FN.ENCODE_FOR_URI.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("ENCODE_FOR_URI requires exactly 1 argument, got " + args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal) args[0];

			if (QueryEvaluationUtility.isStringLiteral(literal)) {
				String lexValue = literal.getLabel();

				return valueFactory.createLiteral(encodeUri(lexValue));
			} else {
				throw new ValueExprEvaluationException("Invalid argument for ENCODE_FOR_URI: " + literal);
			}
		} else {
			throw new ValueExprEvaluationException("Invalid argument for ENCODE_FOR_URI: " + args[0]);
		}
	}

	private String encodeUri(String uri) {

		StringBuilder buf = new StringBuilder(uri.length() * 2);

		int uriLen = uri.length();
		for (int i = 0; i < uriLen; i++) {
			char c = uri.charAt(i);

			if (isUnreserved(c)) {
				buf.append(c);
			} else {
				// use UTF-8 hex encoding for character.
				byte[] utf8 = Character.toString(c).getBytes(StandardCharsets.UTF_8);

				for (byte b : utf8) {
					// Escape character
					buf.append('%');

					char cb = (char) (b & 0xFF);

					String hexVal = Integer.toHexString(cb).toUpperCase();

					// Ensure use of two characters
					if (hexVal.length() == 1) {
						buf.append('0');
					}

					buf.append(hexVal);
				}

			}
		}

		return buf.toString();
	}

	private boolean isUnreserved(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '.' || c == '_'
				|| c == '~';
	}
}
