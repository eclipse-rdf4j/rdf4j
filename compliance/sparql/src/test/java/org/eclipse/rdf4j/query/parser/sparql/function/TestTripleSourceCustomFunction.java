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
package org.eclipse.rdf4j.query.parser.sparql.function;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * A test-only function that evaluates against the supplied triple source. It looks up the 'ex:related' relations for
 * the given input IRI and outputs a single literal of the form "related to v1, v2, v3".
 *
 * @author Jeen Broekstra
 *
 */
public class TestTripleSourceCustomFunction implements Function {

	@Override
	public String getURI() {
		return "urn:triplesourceCustomFunction";
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		throw new UnsupportedOperationException("can only evaluate with triplesource");
	}

	@Override
	public Value evaluate(TripleSource tripleSource, Value... args) throws ValueExprEvaluationException {
		IRI subject = (IRI) args[0];
		IRI related = tripleSource.getValueFactory().createIRI("ex:related");
		List<? extends Statement> relatedStatements = QueryResults
				.asList(tripleSource.getStatements(subject, related, null));

		StringBuilder functionResult = new StringBuilder();
		functionResult.append("related to ");
		for (Statement st : relatedStatements) {
			functionResult.append(st.getObject().stringValue());
			functionResult.append(", ");
		}
		functionResult.setLength(functionResult.length() - 2);

		return tripleSource.getValueFactory().createLiteral(functionResult.toString());
	}

}
