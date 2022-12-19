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

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;

/**
 * @author Jeen Broekstra
 * @deprecated Use {@link StrictEvaluationStrategy} instead.
 */
@Deprecated(since = "4.0")
public class EvaluationStrategyImpl extends StrictEvaluationStrategy {

	public EvaluationStrategyImpl(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver) {
		super(tripleSource, dataset, serviceResolver);
	}
}
