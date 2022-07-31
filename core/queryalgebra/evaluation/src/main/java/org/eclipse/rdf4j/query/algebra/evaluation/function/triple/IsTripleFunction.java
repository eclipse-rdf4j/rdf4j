/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.triple;

import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * Function that return "true"^^xsd:boolean if its argument is RDF-star Triple otherwise return "false"^^xsd:boolean the
 * function's IRI uses RDF namespace to match the other functions in the package
 *
 * @author damyan.ognyanov
 *
 */
public class IsTripleFunction implements Function {
	@Override
	public String getURI() {
		return RDF.NAMESPACE + "isTriple";
	}

	@Override
	public Value evaluate(ValueFactory vf, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("expect exactly 1 argument");
		}
		return vf.createLiteral((args[0] instanceof Triple));
	}
}
