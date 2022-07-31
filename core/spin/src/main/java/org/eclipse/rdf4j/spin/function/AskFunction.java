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
package org.eclipse.rdf4j.spin.function;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.spin.SpinParser;

public class AskFunction extends AbstractSpinFunction implements Function {

	private SpinParser parser;

	public AskFunction() {
		super(SPIN.ASK_FUNCTION.stringValue());
	}

	public AskFunction(SpinParser parser) {
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
		QueryPreparer qp = getCurrentQueryPreparer();
		if (args.length == 0 || !(args[0] instanceof Resource)) {
			throw new ValueExprEvaluationException("First argument must be a resource");
		}
		if ((args.length % 2) == 0) {
			throw new ValueExprEvaluationException("Old number of arguments required");
		}
		try {
			ParsedBooleanQuery askQuery = parser.parseAskQuery((Resource) args[0], qp.getTripleSource());
			BooleanQuery queryOp = qp.prepare(askQuery);
			addBindings(queryOp, args);
			return BooleanLiteral.valueOf(queryOp.evaluate());
		} catch (ValueExprEvaluationException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new ValueExprEvaluationException(e);
		}
	}
}
