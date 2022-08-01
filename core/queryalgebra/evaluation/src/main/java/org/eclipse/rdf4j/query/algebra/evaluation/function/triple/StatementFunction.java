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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * Function constructing RDF-star Triple from its 3 arguments reused the IRI of rdf:Statement as name
 *
 * @author damyan.ognyanov
 *
 */
public class StatementFunction implements Function {
	@Override
	public String getURI() {
		return RDF.STATEMENT.toString();
	}

	@Override
	public Value evaluate(ValueFactory vf, Value... args) throws ValueExprEvaluationException {
		if (args.length != 3) {
			throw new ValueExprEvaluationException("expect exactly 3 arguments");
		}
		if (!(args[0] instanceof Resource)) {
			throw new ValueExprEvaluationException("arg1 must be Resource");
		}
		if (!(args[1] instanceof IRI)) {
			throw new ValueExprEvaluationException("arg2 must be IRI");
		}
		if (!(args[2] instanceof Value)) {
			throw new ValueExprEvaluationException("arg3 must be Value");
		}
		return vf.createTriple((Resource) args[0], (IRI) args[1], args[2]);
	}
}
