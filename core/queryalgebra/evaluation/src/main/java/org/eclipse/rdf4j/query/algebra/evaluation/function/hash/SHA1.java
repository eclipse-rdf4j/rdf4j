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
package org.eclipse.rdf4j.query.algebra.evaluation.function.hash;

import java.security.NoSuchAlgorithmException;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * The SPARQL built-in {@link Function} SHA1, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-sha1">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class SHA1 extends HashFunction {

	@Override
	public String getURI() {
		return "SHA1";
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("SHA1 requires exactly 1 argument, got " + args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal) args[0];

			if (QueryEvaluationUtility.isSimpleLiteral(literal) || XSD.STRING.equals(literal.getDatatype())) {
				String lexValue = literal.getLabel();

				try {
					return valueFactory.createLiteral(hash(lexValue, "SHA1"));
				} catch (NoSuchAlgorithmException e) {
					// SHA1 should always be available.
					throw new RuntimeException(e);
				}
			} else {
				throw new ValueExprEvaluationException("Invalid argument for SHA1: " + literal);
			}
		} else {
			throw new ValueExprEvaluationException("Invalid argument for SHA1: " + args[0]);
		}
	}

}
