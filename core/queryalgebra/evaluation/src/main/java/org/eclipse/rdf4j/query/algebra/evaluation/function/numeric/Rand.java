/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.numeric;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * The SPARQL built-in {@link Function} RAND, as defined in
 * <a href="http://www.w3.org/TR/sparql11-query/#func-rand">SPARQL Query Language for RDF</a>
 *
 * @author Jeen Broekstra
 */
public class Rand implements Function {

	@Override
	public String getURI() {
		return "RAND";
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 0) {
			throw new ValueExprEvaluationException("RAND requires 0 arguments, got " + args.length);
		}

		return valueFactory.createLiteral(Math.random());
	}

	@Override
	public boolean mustReturnDifferentResult() {
		return true;
	}

}
