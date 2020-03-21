/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLFederatedService;

/**
 * Evaluates {@link TupleExpr}s and {@link ValueExpr}s.
 * 
 * @author Arjohn Kampman
 * @author James Leigh
 */
public interface EvaluationStrategy extends FederatedServiceResolver {

	/**
	 * Retrieve the {@link FederatedService} registered for serviceUrl. If there is no service registered for
	 * serviceUrl, a new {@link SPARQLFederatedService} is created and registered.
	 * 
	 * @param serviceUrl URL of the service.
	 * @return the {@link FederatedService} registered for the serviceUrl.
	 * @throws QueryEvaluationException
	 * @see org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver#getService(java.lang.String)
	 */
	@Override
	public FederatedService getService(String serviceUrl) throws QueryEvaluationException;

	/**
	 * Set the {@link QueryOptimizerPipeline} to use for optimizing any incoming queries.
	 * 
	 * @param pipeline the {@link QueryOptimizerPipeline}.
	 * @see #optimize(TupleExpr, EvaluationStatistics, BindingSet)
	 * @since 3.0
	 */
	public void setOptimizerPipeline(QueryOptimizerPipeline pipeline);

	/**
	 * Execute the {@link QueryOptimizerPipeline} on the given {@link TupleExpr} to optimize its execution plan.
	 * 
	 * @param expr                 the {@link TupleExpr} to optimize.
	 * @param evaluationStatistics the {@link EvaluationStatistics} of the data source, to be used for query planning.
	 * @param bindings             a-priori bindings supplied for the query, which can potentially be inlined.
	 * @return the optimized {@link TupleExpr}.
	 * @see #setOptimizerPipeline(QueryOptimizerPipeline)
	 * @since 3.0
	 */
	public TupleExpr optimize(TupleExpr expr, EvaluationStatistics evaluationStatistics,
			BindingSet bindings);

	/**
	 * Evaluates the tuple expression against the supplied triple source with the specified set of variable bindings as
	 * input.
	 * 
	 * @param expr       The Service Expression to evaluate
	 * @param serviceUri TODO
	 * @param bindings   The variables bindings iterator to use for evaluating the expression, if applicable.
	 * @return A closeable iterator over all of variable binding sets that match the tuple expression.
	 */
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service expr, String serviceUri,
			CloseableIteration<BindingSet, QueryEvaluationException> bindings) throws QueryEvaluationException;

	/**
	 * Evaluates the tuple expression against the supplied triple source with the specified set of variable bindings as
	 * input.
	 * 
	 * @param expr     The Tuple Expression to evaluate
	 * @param bindings The variables bindings to use for evaluating the expression, if applicable.
	 * @return A closeable iterator over the variable binding sets that match the tuple expression.
	 */
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleExpr expr, BindingSet bindings)
			throws QueryEvaluationException;

	/**
	 * Gets the value of this expression.
	 * 
	 * @param expr
	 * @param bindings The variables bindings to use for evaluating the expression, if applicable.
	 * @return The Value that this expression evaluates to, or <tt>null</tt> if the expression could not be evaluated.
	 */
	public Value evaluate(ValueExpr expr, BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException;

	/**
	 * Evaluates the boolean expression on the supplied TripleSource object.
	 * 
	 * @param expr
	 * @param bindings The variables bindings to use for evaluating the expression, if applicable.
	 * @return The result of the evaluation.
	 * @throws ValueExprEvaluationException If the value expression could not be evaluated, for example when comparing
	 *                                      two incompatible operands. When thrown, the result of the boolean expression
	 *                                      is neither <tt>true</tt> nor <tt>false</tt> , but unknown.
	 */
	public boolean isTrue(ValueExpr expr, BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException;
}
