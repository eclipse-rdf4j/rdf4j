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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.spin.Argument;

import com.google.common.base.Joiner;

public class SpinFunction extends AbstractSpinFunction implements TransientFunction {

	private ParsedQuery parsedQuery;

	private final List<Argument> arguments = new ArrayList<>(4);

	public SpinFunction(String uri) {
		super(uri);
	}

	public void setParsedQuery(ParsedQuery query) {
		this.parsedQuery = query;
	}

	public ParsedQuery getParsedQuery() {
		return parsedQuery;
	}

	public void addArgument(Argument arg) {
		arguments.add(arg);
	}

	public List<Argument> getArguments() {
		return arguments;
	}

	@Override
	public String toString() {
		return getURI() + "(" + Joiner.on(", ").join(arguments) + ")";
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		QueryPreparer qp = getCurrentQueryPreparer();
		Value result;
		if (parsedQuery instanceof ParsedBooleanQuery) {
			ParsedBooleanQuery askQuery = (ParsedBooleanQuery) parsedQuery;
			BooleanQuery queryOp = qp.prepare(askQuery);
			addBindings(queryOp, arguments, args);
			try {
				result = BooleanLiteral.valueOf(queryOp.evaluate());
			} catch (QueryEvaluationException e) {
				throw new ValueExprEvaluationException(e);
			}
		} else if (parsedQuery instanceof ParsedTupleQuery) {
			ParsedTupleQuery selectQuery = (ParsedTupleQuery) parsedQuery;
			TupleQuery queryOp = qp.prepare(selectQuery);
			addBindings(queryOp, arguments, args);
			try {
				try (TupleQueryResult queryResult = queryOp.evaluate()) {
					if (queryResult.hasNext()) {
						BindingSet bs = queryResult.next();
						if (bs.size() != 1) {
							throw new ValueExprEvaluationException(
									"Only a single result variables is supported: " + bs);
						}
						result = bs.iterator().next().getValue();
					} else {
						throw new ValueExprEvaluationException("No value");
					}
				}
			} catch (QueryEvaluationException e) {
				throw new ValueExprEvaluationException(e);
			}
		} else {
			throw new IllegalStateException("Unexpected query: " + parsedQuery);
		}
		return result;
	}

	private static void addBindings(Query query, List<Argument> arguments, Value... args) {
		for (int i = 0; i < args.length; i++) {
			Argument argument = arguments.get(i);
			query.setBinding(argument.getPredicate().getLocalName(), args[i]);
		}
	}
}
