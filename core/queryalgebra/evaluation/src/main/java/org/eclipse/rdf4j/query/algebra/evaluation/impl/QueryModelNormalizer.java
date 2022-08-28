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

import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;

/**
 * A query optimizer that (partially) normalizes query models to a canonical form. Note: this implementation does not
 * yet cover all query node types.
 *
 * @author Arjohn Kampman
 * @deprecated since 4.1.0. Use
 *             {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryModelNormalizerOptimizer} instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class QueryModelNormalizer extends
		org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryModelNormalizerOptimizer implements QueryOptimizer {
}
