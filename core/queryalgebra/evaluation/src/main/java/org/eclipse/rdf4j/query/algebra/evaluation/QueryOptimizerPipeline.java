/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A pipeline of {@link QueryOptimizer}s that, when executed in order on a {@link TupleExpr}, convert that
 * {@link TupleExpr} to a more optimal query execution plan.
 *
 * @author Jeen Broekstra
 *
 */
public interface QueryOptimizerPipeline {

	/**
	 * Get the optimizers that make up this pipeline.
	 *
	 * @return an {@link Iterable} of {@link QueryOptimizer}s
	 */
	Iterable<QueryOptimizer> getOptimizers();
}
