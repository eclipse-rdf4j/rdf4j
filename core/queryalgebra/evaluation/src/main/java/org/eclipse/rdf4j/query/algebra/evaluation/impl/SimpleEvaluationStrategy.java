/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;

/**
 * @deprecated renamed to {@link StrictEvaluationStrategy}.
 */
@Deprecated
public class SimpleEvaluationStrategy extends StrictEvaluationStrategy {

	/**
	 * @param tripleSource
	 * @param serviceResolver
	 */
	public SimpleEvaluationStrategy(TripleSource tripleSource, FederatedServiceResolver serviceResolver) {
		super(tripleSource, serviceResolver);
	}

	/**
	 * @param tripleSource
	 * @param dataset
	 * @param serviceResolver
	 */
	public SimpleEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver) {
		super(tripleSource, dataset, serviceResolver);
	}

	/**
	 * @param tripleSource
	 * @param dataset
	 * @param serviceResolver
	 * @param iterationCacheSyncTreshold
	 */
	public SimpleEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold) {
		super(tripleSource, dataset, serviceResolver, iterationCacheSyncTreshold, new EvaluationStatistics());
	}

}
