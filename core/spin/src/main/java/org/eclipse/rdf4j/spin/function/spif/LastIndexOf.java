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
package org.eclipse.rdf4j.spin.function.spif;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

public class LastIndexOf implements Function {

	@Override
	public String getURI() {
		return SPIF.LAST_INDEX_OF_FUNCTION.toString();
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length < 2 || args.length > 3) {
			throw new ValueExprEvaluationException("Incorrect number of arguments");
		}
		if (!(args[0] instanceof Literal)) {
			throw new ValueExprEvaluationException("First argument must be a string");
		}
		if (!(args[1] instanceof Literal)) {
			throw new ValueExprEvaluationException("Second argument must be a string");
		}
		if (args.length == 3 && !(args[2] instanceof Literal)) {
			throw new ValueExprEvaluationException("Third argument must be an integer");
		}
		Literal s = (Literal) args[0];
		Literal t = (Literal) args[1];
		int pos = (args.length == 3) ? ((Literal) args[2]).intValue() : s.getLabel().length();
		int index = s.getLabel().lastIndexOf(t.getLabel(), pos);
		if (index == -1) {
			throw new ValueExprEvaluationException("Substring not found");
		}
		return valueFactory.createLiteral(index);
	}
}
