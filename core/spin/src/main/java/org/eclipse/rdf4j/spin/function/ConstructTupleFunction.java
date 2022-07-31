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

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.spin.SpinParser;

public class ConstructTupleFunction extends AbstractSpinFunction implements TupleFunction {

	private SpinParser parser;

	public ConstructTupleFunction() {
		super(SPIN.CONSTRUCT_PROPERTY.stringValue());
	}

	public ConstructTupleFunction(SpinParser parser) {
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
			ParsedGraphQuery graphQuery = parser.parseConstructQuery((Resource) args[0], qp.getTripleSource());
			GraphQuery queryOp = qp.prepare(graphQuery);
			addBindings(queryOp, args);
			final GraphQueryResult queryResult = queryOp.evaluate();
			return new GraphQueryResultIteration(queryResult);
		} catch (QueryEvaluationException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new ValueExprEvaluationException(e);
		}
	}

	static class GraphQueryResultIteration extends AbstractCloseableIteration<List<Value>, QueryEvaluationException> {

		private final GraphQueryResult queryResult;

		GraphQueryResultIteration(GraphQueryResult queryResult) {
			this.queryResult = queryResult;
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
				Statement stmt = queryResult.next();
				Resource ctx = stmt.getContext();
				if (ctx != null) {
					return Arrays.asList(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(), ctx);
				} else {
					return Arrays.asList(stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
				}
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
