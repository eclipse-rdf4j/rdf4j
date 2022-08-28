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

import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;

/**
 * A query optimizer that optimize disjunctive constraints on tuple expressions. Currently, this optimizer {@link Union
 * unions} a clone of the underlying tuple expression with the original expression for each {@link SameTerm} operator,
 * moving the SameTerm to the cloned tuple expression.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 * 
 * @deprecated since 4.1.0. Use
 *             {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.DisjunctiveConstraintOptimizer} instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class DisjunctiveConstraintOptimizer extends
		org.eclipse.rdf4j.query.algebra.evaluation.optimizer.DisjunctiveConstraintOptimizer implements QueryOptimizer {

}
