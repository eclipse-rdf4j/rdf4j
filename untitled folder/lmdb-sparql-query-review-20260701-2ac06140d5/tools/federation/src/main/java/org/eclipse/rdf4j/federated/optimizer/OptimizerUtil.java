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
package org.eclipse.rdf4j.federated.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.federated.algebra.NJoin;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

public class OptimizerUtil {

	/**
	 * Flatten the join to one layer, i.e. collect all join arguments
	 *
	 * @param join
	 * @param queryInfo
	 * @return the flattened {@link NJoin}
	 */
	public static NJoin flattenJoin(Join join, QueryInfo queryInfo) {
		List<TupleExpr> joinArgs = new ArrayList<>();
		collectJoinArgs(join, joinArgs);
		return new NJoin(joinArgs, queryInfo);
	}

	/**
	 * Collect join arguments by descending the query tree (recursively).
	 *
	 * @param node
	 * @param joinArgs
	 */
	protected static void collectJoinArgs(TupleExpr node, List<TupleExpr> joinArgs) {

		if (node instanceof Join) {
			collectJoinArgs(((Join) node).getLeftArg(), joinArgs);
			collectJoinArgs(((Join) node).getRightArg(), joinArgs);
		} else {
			joinArgs.add(node);
		}
	}
}
