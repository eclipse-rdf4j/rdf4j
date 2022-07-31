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
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * The SPARQL built-in {@link Function} REPLACE, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-substr">SPARQL Query Language for RDF</a>.
 *
 * @author Jeen Broekstra
 */
public class Replace implements Function {

	@Override
	public String getURI() {
		return FN.REPLACE.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length < 3 || args.length > 4) {
			throw new ValueExprEvaluationException("Incorrect number of arguments for REPLACE: " + args.length);
		}

		try {
			Literal arg = (Literal) args[0];
			Literal pattern = (Literal) args[1];
			Literal replacement = (Literal) args[2];
			Literal flags = null;
			if (args.length == 4) {
				flags = (Literal) args[3];
			}

			if (!QueryEvaluationUtility.isStringLiteral(arg)) {
				throw new ValueExprEvaluationException("incompatible operand for REPLACE: " + arg);
			}

			if (!QueryEvaluationUtility.isSimpleLiteral(pattern)) {
				throw new ValueExprEvaluationException("incompatible operand for REPLACE: " + pattern);
			}

			if (!QueryEvaluationUtility.isSimpleLiteral(replacement)) {
				throw new ValueExprEvaluationException("incompatible operand for REPLACE: " + replacement);
			}

			String flagString = null;
			if (flags != null) {
				if (!QueryEvaluationUtility.isSimpleLiteral(flags)) {
					throw new ValueExprEvaluationException("incompatible operand for REPLACE: " + flags);
				}
				flagString = flags.getLabel();
			}

			String argString = arg.getLabel();
			String patternString = pattern.getLabel();
			String replacementString = replacement.getLabel();

			int f = 0;
			if (flagString != null) {
				for (char c : flagString.toCharArray()) {
					switch (c) {
					case 's':
						f |= Pattern.DOTALL;
						break;
					case 'm':
						f |= Pattern.MULTILINE;
						break;
					case 'i':
						f |= Pattern.CASE_INSENSITIVE;
						break;
					case 'x':
						f |= Pattern.COMMENTS;
						break;
					case 'd':
						f |= Pattern.UNIX_LINES;
						break;
					case 'u':
						f |= Pattern.UNICODE_CASE;
						break;
					default:
						throw new ValueExprEvaluationException(flagString);
					}
				}
			}

			Pattern p = Pattern.compile(patternString, f);
			String result = p.matcher(argString).replaceAll(replacementString);

			Optional<String> lang = arg.getLanguage();
			IRI dt = arg.getDatatype();

			if (lang.isPresent()) {
				return valueFactory.createLiteral(result, lang.get());
			} else if (dt != null) {
				return valueFactory.createLiteral(result, dt);
			} else {
				return valueFactory.createLiteral(result);
			}
		} catch (ClassCastException e) {
			throw new ValueExprEvaluationException("literal operands expected", e);
		}

	}
}
