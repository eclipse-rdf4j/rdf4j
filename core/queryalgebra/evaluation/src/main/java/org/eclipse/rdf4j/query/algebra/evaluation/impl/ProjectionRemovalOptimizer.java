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
 * If a projection node in the algebra does not contribute or change the results it can be removed from the tree.
 *
 * For example <code>
 * SELECT ?s ?p ?o
 * WHERE {?s ?p ?o }
 * </code> Does not need a projection as the inner statement pattern returns the same result.
 *
 * While <code>
 *  * SELECT ?s ?p
 * WHERE {?s ?p ?o }
 * </code> Does as the statement pattern has one more variable in use than the projection.
 *
 * Note: this optimiser should run after optimisations ran that depend on Projections. e.g.
 *
 * @see UnionScopeChangeOptimizer
 *
 * @author Jerven Bolleman
 * 
 * @deprecated since 4.1.0. Use {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.ProjectionRemovalOptimizer}
 *             instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class ProjectionRemovalOptimizer extends
		org.eclipse.rdf4j.query.algebra.evaluation.optimizer.ProjectionRemovalOptimizer implements QueryOptimizer {

}
