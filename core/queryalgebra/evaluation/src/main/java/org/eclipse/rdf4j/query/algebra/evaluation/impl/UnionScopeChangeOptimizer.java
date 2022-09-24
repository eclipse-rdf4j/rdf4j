/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;

/**
 * Inspect Union clauses to check if scope change can be avoided (allowing injection of pre-bound vars into union
 * arguments).
 *
 * @author Jeen Broekstra
 * @deprecated since 4.1.0. Use {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.UnionScopeChangeOptimizer}
 *             instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class UnionScopeChangeOptimizer extends
		org.eclipse.rdf4j.query.algebra.evaluation.optimizer.UnionScopeChangeOptimizer implements QueryOptimizer {
}
