/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.XMLDatatypeMathUtil;

/**
 * SPARQL 1.1 extended query evaluation strategy. This strategy adds the use of virtual properties, as well as extended
 * comparison and mathematical operators to the minimally-conforming {@link StrictEvaluationStrategy}.
 * 
 * @author Jeen Broekstra
 */
public class ExtendedEvaluationStrategy extends TupleFunctionEvaluationStrategy {

	public ExtendedEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncThreshold) {
		super(tripleSource, dataset, serviceResolver, iterationCacheSyncThreshold);
	}

	@Override
	public Value evaluate(Compare node, BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException {
		Value leftVal = evaluate(node.getLeftArg(), bindings);
		Value rightVal = evaluate(node.getRightArg(), bindings);

		// return result of non-strict comparison.
		return BooleanLiteral.valueOf(QueryEvaluationUtil.compare(leftVal, rightVal, node.getOperator(), false));
	}

	@Override
	public Value evaluate(MathExpr node, BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException {
		Value leftVal = evaluate(node.getLeftArg(), bindings);
		Value rightVal = evaluate(node.getRightArg(), bindings);

		if (leftVal instanceof Literal && rightVal instanceof Literal) {
			return XMLDatatypeMathUtil.compute((Literal) leftVal, (Literal) rightVal, node.getOperator());
		}

		throw new ValueExprEvaluationException("Both arguments must be literals");
	}

}
