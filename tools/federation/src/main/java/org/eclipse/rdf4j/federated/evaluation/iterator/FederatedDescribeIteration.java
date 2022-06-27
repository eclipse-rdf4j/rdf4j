/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.algebra.StatementSource.StatementSourceType;
import org.eclipse.rdf4j.federated.algebra.StatementSourcePattern;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.DescribeIteration;

import com.google.common.collect.Lists;

/**
 * Specialized {@link DescribeIteration} for evaluation of DESCRIBE queries in the federation. ‚
 *
 * @author Andreas Schwarte
 *
 */
@Deprecated(since = "4.1.0")
public class FederatedDescribeIteration extends DescribeIteration {

	private final QueryInfo queryInfo;

	private final List<StatementSource> allSources;

	@Deprecated(since = "4.1.0", forRemoval = true)
	public FederatedDescribeIteration(Iteration<BindingSet, QueryEvaluationException> sourceIter,
			FederationEvalStrategy strategy, Set<String> describeExprNames, BindingSet parentBindings,
			QueryInfo queryInfo) {
		super(sourceIter, strategy, describeExprNames, parentBindings);
		this.queryInfo = queryInfo;

		// initialize StatementSources for all federation members
		allSources = Lists.newArrayList();
		for (Endpoint member : queryInfo.getFederationContext().getFederation().getMembers()) {
			allSources.add(new StatementSource(member.getId(), StatementSourceType.REMOTE));
		}
	}

	@Override
	protected CloseableIteration<BindingSet, QueryEvaluationException> createNextIteration(Value subject, Value object)
			throws QueryEvaluationException {
		if (subject == null && object == null) {
			return new EmptyIteration<>();
		}

		Var subjVar = new Var(VARNAME_SUBJECT, subject);
		Var predVar = new Var(VARNAME_PREDICATE);
		Var objVar = new Var(VARNAME_OBJECT, object);

		StatementPattern pattern = new StatementPattern(subjVar, predVar, objVar);

		// associate all federation members as sources for this pattern
		// Note: for DESCRIBE we currently do not perform any extra source selection,
		// i.e. we assume all members to be relevant for describing the resource
		StatementSourcePattern stmtSourcePattern = new StatementSourcePattern(pattern, queryInfo);
		allSources.forEach(stmtSourcePattern::addStatementSource);

		CloseableIteration<BindingSet, QueryEvaluationException> res = stmtSourcePattern.evaluate(parentBindings);

		// we need to make sure that subject or object are added to the binding set
		// Note: FedX uses prepared SELECT queries to evaluate a statement pattern and
		// thus does not add bound values to the result bindingset
		return new ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException>(res) {

			@Override
			protected BindingSet convert(BindingSet sourceObject) throws QueryEvaluationException {
				QueryBindingSet bs = new QueryBindingSet(sourceObject);
				if (subject != null && !bs.hasBinding(VARNAME_SUBJECT)) {
					bs.addBinding(VARNAME_SUBJECT, subject);
				}
				if (object != null && !bs.hasBinding(VARNAME_OBJECT)) {
					bs.addBinding(VARNAME_OBJECT, object);
				}
				return bs;
			}
		};
	}
}
