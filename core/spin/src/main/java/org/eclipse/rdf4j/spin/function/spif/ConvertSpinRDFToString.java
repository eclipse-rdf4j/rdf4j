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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.queryrender.sparql.SPARQLQueryRenderer;
import org.eclipse.rdf4j.spin.SpinParser;
import org.eclipse.rdf4j.spin.function.AbstractSpinFunction;

public class ConvertSpinRDFToString extends AbstractSpinFunction implements Function {

	private SpinParser parser;

	public ConvertSpinRDFToString() {
		super(SPIF.CONVERT_SPIN_RDF_TO_STRING_FUNCTION.stringValue());
	}

	public ConvertSpinRDFToString(SpinParser parser) {
		this();
		this.parser = parser;
	}

	public SpinParser getSpinParser() {
		return parser;
	}

	public void setSpinParser(SpinParser parser) {
		this.parser = parser;
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length < 1 || args.length > 2) {
			throw new ValueExprEvaluationException("Incorrect number of arguments");
		}
		if (!(args[0] instanceof Resource)) {
			throw new ValueExprEvaluationException("First argument must be the root of a SPIN RDF query");
		}
		if (args.length == 2 && !(args[1] instanceof Literal)) {
			throw new ValueExprEvaluationException("Second argument must be a string");
		}
		Resource q = (Resource) args[0];
		boolean useHtml = (args.length == 2) ? ((Literal) args[1]).booleanValue() : false;
		String sparqlString;
		try {
			ParsedOperation op = parser.parse(q, getCurrentQueryPreparer().getTripleSource());
			sparqlString = new SPARQLQueryRenderer().render((ParsedQuery) op);
		} catch (Exception e) {
			throw new ValueExprEvaluationException(e);
		}
		return valueFactory.createLiteral(sparqlString);
	}
}
