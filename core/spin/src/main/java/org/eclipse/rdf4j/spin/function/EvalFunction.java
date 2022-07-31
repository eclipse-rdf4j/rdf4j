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

import java.util.Set;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.TripleSources;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.spin.SpinParser;

public class EvalFunction extends AbstractSpinFunction implements Function {

	private SpinParser parser;

	public EvalFunction() {
		super(SPIN.EVAL_FUNCTION.stringValue());
	}

	public EvalFunction(SpinParser parser) {
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
		Value result;
		Resource subj = (Resource) args[0];
		try {
			ParsedQuery parsedQuery;
			if (isQuery(subj, qp.getTripleSource())) {
				parsedQuery = parser.parseQuery(subj, qp.getTripleSource());
			} else {
				ValueExpr expr = parser.parseExpression(subj, qp.getTripleSource());
				// wrap in a TupleExpr
				TupleExpr root = new Extension(new SingletonSet(), new ExtensionElem(expr, "result"));
				parsedQuery = new ParsedTupleQuery(root);
			}

			if (parsedQuery instanceof ParsedTupleQuery) {
				ParsedTupleQuery tupleQuery = (ParsedTupleQuery) parsedQuery;
				TupleQuery queryOp = qp.prepare(tupleQuery);
				addArguments(queryOp, args);
				try (TupleQueryResult queryResult = queryOp.evaluate()) {
					if (queryResult.hasNext()) {
						BindingSet bs = queryResult.next();
						Set<String> bindingNames = tupleQuery.getTupleExpr().getBindingNames();
						if (!bindingNames.isEmpty()) {
							result = bs.getValue(bindingNames.iterator().next());
						} else {
							throw new ValueExprEvaluationException("No value");
						}
					} else {
						throw new ValueExprEvaluationException("No value");
					}
				}
			} else if (parsedQuery instanceof ParsedBooleanQuery) {
				ParsedBooleanQuery booleanQuery = (ParsedBooleanQuery) parsedQuery;
				BooleanQuery queryOp = qp.prepare(booleanQuery);
				addArguments(queryOp, args);
				result = BooleanLiteral.valueOf(queryOp.evaluate());
			} else {
				throw new ValueExprEvaluationException("First argument must be a SELECT, ASK or expression");
			}
		} catch (ValueExprEvaluationException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new ValueExprEvaluationException(e);
		}
		return result;
	}

	private boolean isQuery(Resource r, TripleSource store) throws RDF4JException {
		try (CloseableIteration<? extends IRI, QueryEvaluationException> typeIter = TripleSources.getObjectURIs(r,
				RDF.TYPE,
				store)) {
			while (typeIter.hasNext()) {
				IRI type = typeIter.next();
				if (SP.SELECT_CLASS.equals(type) || SP.ASK_CLASS.equals(type) || SPIN.TEMPLATES_CLASS.equals(type)) {
					return true;
				}
			}
		}

		return false;
	}

	protected static void addArguments(Query query, Value... args) throws ValueExprEvaluationException {
		for (int i = 1; i < args.length; i += 2) {
			if (!(args[i] instanceof IRI)) {
				throw new ValueExprEvaluationException("Argument " + i + " must be a IRI");
			}
			query.setBinding(((IRI) args[i]).getLocalName(), args[i + 1]);
		}
	}
}
