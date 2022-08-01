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
 * Function returning the subject component of RDF-star Triple reused the IRI of rdf:subject as name
 *
 * @author damyan.ognyanov
 *
 */
public class TripleSubjectFunction implements Function {
	@Override
	public String getURI() {
		return RDF.SUBJECT.toString();
	}

	@Override
	public Value evaluate(ValueFactory vf, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException("expect exactly 1 argument");
		}
		if (!(args[0] instanceof Triple)) {
			throw new ValueExprEvaluationException("arg1 must be a Triple");
		}
		return ((Triple) args[0]).getSubject();
	}
}
