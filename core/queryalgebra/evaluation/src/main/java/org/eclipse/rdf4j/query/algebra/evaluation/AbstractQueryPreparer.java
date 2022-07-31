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
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.impl.IteratingGraphQueryResult;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.query.parser.impl.AbstractParserQuery;
import org.eclipse.rdf4j.query.parser.impl.AbstractParserUpdate;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

public abstract class AbstractQueryPreparer implements QueryPreparer {

	private final TripleSource tripleSource;

	public AbstractQueryPreparer(TripleSource tripleSource) {
		this.tripleSource = tripleSource;
	}

	@Override
	public BooleanQuery prepare(ParsedBooleanQuery q) {
		return new BooleanQueryImpl(q);
	}

	@Override
	public TupleQuery prepare(ParsedTupleQuery q) {
		return new TupleQueryImpl(q);
	}

	@Override
	public GraphQuery prepare(ParsedGraphQuery q) {
		return new GraphQueryImpl(q);
	}

	@Override
	public Update prepare(ParsedUpdate u) {
		return new UpdateImpl(u);
	}

	@Override
	public TripleSource getTripleSource() {
		return tripleSource;
	}

	protected abstract CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred, int maxExecutionTime)
			throws QueryEvaluationException;

	protected abstract void execute(UpdateExpr updateExpr, Dataset dataset, BindingSet bindings,
			boolean includeInferred, int maxExecutionTime) throws UpdateExecutionException;

	class BooleanQueryImpl extends AbstractParserQuery implements BooleanQuery {

		BooleanQueryImpl(ParsedBooleanQuery query) {
			super(query);
		}

		@Override
		public ParsedBooleanQuery getParsedQuery() {
			return (ParsedBooleanQuery) super.getParsedQuery();
		}

		@Override
		public boolean evaluate() throws QueryEvaluationException {
			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter1 = null;
			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter2 = null;
			try {
				ParsedBooleanQuery parsedBooleanQuery = getParsedQuery();
				TupleExpr tupleExpr = parsedBooleanQuery.getTupleExpr();
				Dataset dataset = getDataset();
				if (dataset == null) {
					// No external dataset specified, use query's own dataset (if any)
					dataset = parsedBooleanQuery.getDataset();
				}

				bindingsIter1 = AbstractQueryPreparer.this.evaluate(tupleExpr, dataset, getBindings(),
						getIncludeInferred(), getMaxExecutionTime());
				bindingsIter2 = enforceMaxQueryTime(bindingsIter1);

				return bindingsIter2.hasNext();
			} finally {
				try {
					if (bindingsIter2 != null) {
						bindingsIter2.close();
					}
				} finally {
					if (bindingsIter1 != null) {
						bindingsIter1.close();
					}
				}
			}
		}

		@Override
		public Explanation explain(Explanation.Level level) {
			throw new UnsupportedOperationException();
		}
	}

	class TupleQueryImpl extends AbstractParserQuery implements TupleQuery {

		TupleQueryImpl(ParsedTupleQuery query) {
			super(query);
		}

		@Override
		public ParsedTupleQuery getParsedQuery() {
			return (ParsedTupleQuery) super.getParsedQuery();
		}

		@Override
		public TupleQueryResult evaluate() throws QueryEvaluationException {
			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter1 = null;
			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter2 = null;
			IteratingTupleQueryResult result = null;
			boolean allGood = false;
			try {
				TupleExpr tupleExpr = getParsedQuery().getTupleExpr();
				bindingsIter1 = AbstractQueryPreparer.this.evaluate(tupleExpr, getActiveDataset(), getBindings(),
						getIncludeInferred(), getMaxExecutionTime());
				bindingsIter2 = enforceMaxQueryTime(bindingsIter1);
				result = new IteratingTupleQueryResult(new ArrayList<>(tupleExpr.getBindingNames()), bindingsIter2);
				allGood = true;
				return result;
			} finally {
				if (!allGood) {
					try {
						if (result != null) {
							result.close();
						}
					} finally {
						try {
							if (bindingsIter2 != null) {
								bindingsIter2.close();
							}
						} finally {
							if (bindingsIter1 != null) {
								bindingsIter1.close();
							}
						}
					}
				}
			}
		}

		@Override
		public void evaluate(TupleQueryResultHandler handler)
				throws QueryEvaluationException, TupleQueryResultHandlerException {
			TupleQueryResult queryResult = evaluate();
			QueryResults.report(queryResult, handler);
		}

		@Override
		public Explanation explain(Explanation.Level level) {
			throw new UnsupportedOperationException();
		}
	}

	class GraphQueryImpl extends AbstractParserQuery implements GraphQuery {

		GraphQueryImpl(ParsedGraphQuery query) {
			super(query);
		}

		@Override
		public ParsedGraphQuery getParsedQuery() {
			return (ParsedGraphQuery) super.getParsedQuery();
		}

		@Override
		public GraphQueryResult evaluate() throws QueryEvaluationException {
			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter1 = null;
			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter2 = null;
			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter3 = null;
			CloseableIteration<? extends Statement, QueryEvaluationException> stIter = null;
			IteratingGraphQueryResult result = null;

			boolean allGood = false;
			try {
				TupleExpr tupleExpr = getParsedQuery().getTupleExpr();
				bindingsIter1 = AbstractQueryPreparer.this.evaluate(tupleExpr, getActiveDataset(), getBindings(),
						getIncludeInferred(), getMaxExecutionTime());

				// Filters out all partial and invalid matches
				bindingsIter2 = new FilterIteration<BindingSet, QueryEvaluationException>(bindingsIter1) {

					@Override
					protected boolean accept(BindingSet bindingSet) {
						Value context = bindingSet.getValue("context");

						return bindingSet.getValue("subject") instanceof Resource
								&& bindingSet.getValue("predicate") instanceof IRI
								&& bindingSet.getValue("object") instanceof Value
								&& (context == null || context instanceof Resource);
					}
				};

				bindingsIter3 = enforceMaxQueryTime(bindingsIter2);

				// Convert the BindingSet objects to actual RDF statements
				stIter = new ConvertingIteration<BindingSet, Statement, QueryEvaluationException>(bindingsIter3) {

					private final ValueFactory vf = tripleSource.getValueFactory();

					@Override
					protected Statement convert(BindingSet bindingSet) {
						Resource subject = (Resource) bindingSet.getValue("subject");
						IRI predicate = (IRI) bindingSet.getValue("predicate");
						Value object = bindingSet.getValue("object");
						Resource context = (Resource) bindingSet.getValue("context");

						if (context == null) {
							return vf.createStatement(subject, predicate, object);
						} else {
							return vf.createStatement(subject, predicate, object, context);
						}
					}
				};

				result = new IteratingGraphQueryResult(getParsedQuery().getQueryNamespaces(), stIter);
				allGood = true;
				return result;
			} finally {
				if (!allGood) {
					try {
						if (result != null) {
							result.close();
						}
					} finally {
						try {
							if (stIter != null) {
								stIter.close();
							}
						} finally {
							try {
								if (bindingsIter3 != null) {
									bindingsIter3.close();
								}
							} finally {
								try {
									if (bindingsIter2 != null) {
										bindingsIter2.close();
									}
								} finally {
									if (bindingsIter1 != null) {
										bindingsIter1.close();
									}
								}
							}
						}
					}
				}
			}
		}

		@Override
		public void evaluate(RDFHandler handler) throws QueryEvaluationException, RDFHandlerException {
			GraphQueryResult queryResult = evaluate();
			QueryResults.report(queryResult, handler);
		}

		@Override
		public Explanation explain(Explanation.Level level) {
			throw new UnsupportedOperationException();
		}
	}

	class UpdateImpl extends AbstractParserUpdate {

		UpdateImpl(ParsedUpdate update) {
			super(update);
		}

		@Override
		public void execute() throws UpdateExecutionException {
			ParsedUpdate parsedUpdate = getParsedUpdate();
			List<UpdateExpr> updateExprs = parsedUpdate.getUpdateExprs();
			Map<UpdateExpr, Dataset> datasetMapping = parsedUpdate.getDatasetMapping();
			for (UpdateExpr updateExpr : updateExprs) {
				Dataset activeDataset = getMergedDataset(datasetMapping.get(updateExpr));

				try {
					AbstractQueryPreparer.this.execute(updateExpr, activeDataset, getBindings(), getIncludeInferred(),
							getMaxExecutionTime());
				} catch (UpdateExecutionException e) {
					if (!updateExpr.isSilent()) {
						throw e;
					}
				}
			}
		}
	}
}
