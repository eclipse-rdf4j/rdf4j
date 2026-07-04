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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;

/**
 * Minimally-conforming SPARQL 1.1 Query Evaluation strategy, to evaluate one {@link TupleExpr} on the given
 * {@link TripleSource}, optionally using the given {@link Dataset}.
 *
 * @author Jeen Broekstra
 * @author James Leigh
 * @author Arjohn Kampman
 * @author David Huynh
 * @author Andreas Schwarte
 * @see ExtendedEvaluationStrategy
 *
 * @deprecated since 4.3.0. Use {@link DefaultEvaluationStrategy} instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class StrictEvaluationStrategy extends DefaultEvaluationStrategy {
	public StrictEvaluationStrategy(TripleSource tripleSource, FederatedServiceResolver serviceResolver) {
		this(tripleSource, null, serviceResolver);
	}

	public StrictEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver) {
		this(tripleSource, dataset, serviceResolver, 0, new EvaluationStatistics());
	}

	public StrictEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold,
			EvaluationStatistics evaluationStatistics) {
		this(tripleSource, dataset, serviceResolver, iterationCacheSyncTreshold, evaluationStatistics, false);
	}

	public StrictEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold,
			EvaluationStatistics evaluationStatistics, boolean trackResultSize) {
		this(tripleSource, dataset, serviceResolver, iterationCacheSyncTreshold, evaluationStatistics, trackResultSize,
				TupleFunctionRegistry.getInstance());
	}

	public StrictEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold,
			EvaluationStatistics evaluationStatistics, boolean trackResultSize,
			TupleFunctionRegistry tupleFunctionRegistry) {
		super(tripleSource, dataset, serviceResolver, iterationCacheSyncTreshold, evaluationStatistics, trackResultSize,
				tupleFunctionRegistry);
		this.setQueryEvaluationMode(QueryEvaluationMode.STRICT);
	}

}
