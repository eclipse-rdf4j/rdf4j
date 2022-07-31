/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.Set;

import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Interface for a cost model used in {@link StatementGroupAndJoinOptimizer}.
 *
 * @author Andreas Schwarte
 *
 */
public interface FedXCostModel {

	/**
	 * Return the estimated cost for the given {@link TupleExpr}
	 *
	 * @param tupleExpr
	 * @param joinVars
	 * @return the cost associated to the tupleExpr
	 */
	double estimateCost(TupleExpr tupleExpr, Set<String> joinVars);
}
