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
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * The SPARQL built-in {@link Function} STRSTARTS, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-strstarts">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class StrStarts implements Function {

	@Override
	public String getURI() {
		return FN.STARTS_WITH.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 2) {
			throw new ValueExprEvaluationException("STRSTARTS requires 2 arguments, got " + args.length);
		}

		Value leftVal = args[0];
		Value rightVal = args[1];

		if (leftVal instanceof Literal && rightVal instanceof Literal) {
			Literal leftLit = (Literal) leftVal;
			Literal rightLit = (Literal) rightVal;

			if (QueryEvaluationUtility.compatibleArguments(leftLit, rightLit)) {

				String leftLexVal = leftLit.getLabel();
				String rightLexVal = rightLit.getLabel();

				return BooleanLiteral.valueOf(leftLexVal.startsWith(rightLexVal));
			} else {
				throw new ValueExprEvaluationException("incompatible operands for STRSTARTS function");
			}
		} else {
			throw new ValueExprEvaluationException("STRSTARTS function expects literal operands");
		}

	}

}
