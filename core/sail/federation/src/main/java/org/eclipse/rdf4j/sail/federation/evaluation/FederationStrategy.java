/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.evaluation;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SimpleEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.BadlyDesignedLeftJoinIterator;
import org.eclipse.rdf4j.sail.federation.algebra.NaryJoin;
import org.eclipse.rdf4j.sail.federation.algebra.OwnedTupleExpr;

/**
 * Evaluates Join, LeftJoin and Union in parallel and only evaluate if
 * {@link OwnedTupleExpr} is the given member.
 * 
 * @see ParallelJoinCursor
 * @see ParallelLeftJoinCursor
 * @author James Leigh
 */
public class FederationStrategy extends SimpleEvaluationStrategy {

	private final Executor executor;

	public FederationStrategy(Executor executor, TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceManager)
	{
		super(tripleSource, dataset, serviceManager);
		this.executor = executor;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleExpr expr,
			BindingSet bindings)
		throws QueryEvaluationException
	{
		CloseableIteration<BindingSet, QueryEvaluationException> result;
		if (expr instanceof NaryJoin) {
			result = evaluate((NaryJoin)expr, bindings);
		}
		else if (expr instanceof OwnedTupleExpr) {
			result = evaluate((OwnedTupleExpr)expr, bindings);
		}
		else {
			result = super.evaluate(expr, bindings);
		}
		return result;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Join join, BindingSet bindings)
		throws QueryEvaluationException
	{
		CloseableIteration<BindingSet, QueryEvaluationException> result = evaluate(join.getLeftArg(), bindings);
		for (int i = 1, n = 2; i < n; i++) {
			result = new ParallelJoinCursor(this, result, join.getRightArg()); // NOPMD
			executor.execute((Runnable)result);
		}
		return result;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(NaryJoin join, BindingSet bindings)
		throws QueryEvaluationException
	{
		assert join.getNumberOfArguments() > 0;
		CloseableIteration<BindingSet, QueryEvaluationException> result = evaluate(join.getArg(0), bindings);
		for (int i = 1, n = join.getNumberOfArguments(); i < n; i++) {
			result = new ParallelJoinCursor(this, result, join.getArg(i)); // NOPMD
			executor.execute((Runnable)result);
		}
		return result;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(LeftJoin leftJoin,
			final BindingSet bindings)
		throws QueryEvaluationException
	{
		// Check whether optional join is "well designed" as defined in section
		// 4.2 of "Semantics and Complexity of SPARQL", 2006, Jorge Pérez et al.
		Set<String> boundVars = bindings.getBindingNames();
		Set<String> leftVars = leftJoin.getLeftArg().getBindingNames();
		Set<String> optionalVars = leftJoin.getRightArg().getBindingNames();

		final Set<String> problemVars = new HashSet<String>(boundVars);
		problemVars.retainAll(optionalVars);
		problemVars.removeAll(leftVars);

		CloseableIteration<BindingSet, QueryEvaluationException> result;
		if (problemVars.isEmpty()) {
			// left join is "well designed"
			result = new ParallelLeftJoinCursor(this, leftJoin, bindings);
			executor.execute((Runnable)result);
		}
		else {
			result = new BadlyDesignedLeftJoinIterator(this, leftJoin, bindings, problemVars);
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Union union, BindingSet bindings)
		throws QueryEvaluationException
	{
		CloseableIteration<BindingSet, QueryEvaluationException>[] iters = new CloseableIteration[2];
		iters[0] = evaluate(union.getLeftArg(), bindings);
		iters[1] = evaluate(union.getRightArg(), bindings);
		return new UnionIteration<BindingSet, QueryEvaluationException>(iters);
	}

	private CloseableIteration<BindingSet, QueryEvaluationException> evaluate(OwnedTupleExpr expr,
			BindingSet bindings)
		throws QueryEvaluationException
	{
		CloseableIteration<BindingSet, QueryEvaluationException> result = expr.evaluate(dataset, bindings);
		if (result == null) {
			TripleSource source = new RepositoryTripleSource(expr.getOwner());
			EvaluationStrategy eval = new FederationStrategy(executor, source, dataset, serviceResolver);
			result = eval.evaluate(expr.getArg(), bindings);
		}
		return result;
	}

}
