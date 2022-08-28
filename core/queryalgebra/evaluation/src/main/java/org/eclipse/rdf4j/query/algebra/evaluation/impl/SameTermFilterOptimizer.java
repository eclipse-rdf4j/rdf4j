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

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;

/**
 * A query optimizer that embeds {@link Filter}s with {@link SameTerm} operators in statement patterns as much as
 * possible. Operators like sameTerm(X, Y) are processed by renaming X to Y (or vice versa). Operators like sameTerm(X,
 * <someURI>) are processed by assigning the URI to all occurring variables with name X.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 * 
 * @deprecated since 4.1.0. Use {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.SameTermFilterOptimizer}
 *             instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class SameTermFilterOptimizer
		extends org.eclipse.rdf4j.query.algebra.evaluation.optimizer.SameTermFilterOptimizer implements QueryOptimizer {
}
