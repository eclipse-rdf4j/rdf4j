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
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.spin.SpinParser;

public class SelectTupleFunction extends AbstractSpinFunction implements TupleFunction {

	private SpinParser parser;

	public SelectTupleFunction() {
		super(SPIN.SELECT_PROPERTY.stringValue());
	}

	public SelectTupleFunction(SpinParser parser) {
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
	public CloseableIteration<? extends List<? extends Value>, QueryEvaluationException> evaluate(
			ValueFactory valueFactory, Value... args) throws QueryEvaluationException {
		QueryPreparer qp = getCurrentQueryPreparer();
		if (args.length == 0 || !(args[0] instanceof Resource)) {
			throw new QueryEvaluationException("First argument must be a resource");
		}
		if ((args.length % 2) == 0) {
			throw new QueryEvaluationException("Old number of arguments required");
		}
		try {
			ParsedQuery parsedQuery = parser.parseQuery((Resource) args[0], qp.getTripleSource());
			if (parsedQuery instanceof ParsedTupleQuery) {
				ParsedTupleQuery tupleQuery = (ParsedTupleQuery) parsedQuery;
				TupleQuery queryOp = qp.prepare(tupleQuery);
				addBindings(queryOp, args);
				final TupleQueryResult queryResult = queryOp.evaluate();
				return new TupleQueryResultIteration(queryResult);
			} else if (parsedQuery instanceof ParsedBooleanQuery) {
				ParsedBooleanQuery booleanQuery = (ParsedBooleanQuery) parsedQuery;
				BooleanQuery queryOp = qp.prepare(booleanQuery);
				addBindings(queryOp, args);
				Value result = BooleanLiteral.valueOf(queryOp.evaluate());
				return new SingletonIteration<>(Collections.singletonList(result));
			} else {
				throw new QueryEvaluationException("First argument must be a SELECT or ASK query");
			}
		} catch (QueryEvaluationException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new ValueExprEvaluationException(e);
		}
	}

	static class TupleQueryResultIteration extends AbstractCloseableIteration<List<Value>, QueryEvaluationException> {

		private final TupleQueryResult queryResult;

		private final List<String> bindingNames;

		TupleQueryResultIteration(TupleQueryResult queryResult) throws QueryEvaluationException {
			this.queryResult = queryResult;
			this.bindingNames = queryResult.getBindingNames();
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			if (isClosed()) {
				return false;
			}
			boolean result = queryResult.hasNext();
			if (!result) {
				close();
			}
			return result;
		}

		@Override
		public List<Value> next() throws QueryEvaluationException {
			if (isClosed()) {
				throw new NoSuchElementException("The iteration has been closed.");
			}
			try {
				BindingSet bs = queryResult.next();
				List<Value> values = new ArrayList<>(bindingNames.size());
				for (String bindingName : bindingNames) {
					values.add(bs.getValue(bindingName));
				}
				return values;
			} catch (NoSuchElementException e) {
				close();
				throw e;
			}
		}

		@Override
		public void remove() throws QueryEvaluationException {
			if (isClosed()) {
				throw new IllegalStateException("The iteration has been closed.");
			}
			try {
				queryResult.remove();
			} catch (IllegalStateException e) {
				close();
				throw e;
			}
		}

		@Override
		public void handleClose() throws QueryEvaluationException {
			try {
				super.handleClose();
			} finally {
				queryResult.close();
			}
		}
	}
}
