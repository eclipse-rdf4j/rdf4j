/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.fluidops.fedx.algebra.NJoin;
import com.fluidops.fedx.structures.QueryInfo;

public class OptimizerUtil
{

	
	/**
	 * Flatten the join to one layer, i.e. collect all join arguments
	 * 
	 * @param join
	 * @param queryInfo
	 * @return the flattened {@link NJoin}
	 */
	public static NJoin flattenJoin(Join join, QueryInfo queryInfo) {
		List<TupleExpr> joinArgs = new ArrayList<TupleExpr>();
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
			collectJoinArgs(((Join)node).getLeftArg(), joinArgs);
			collectJoinArgs(((Join)node).getRightArg(), joinArgs);
		} else
			joinArgs.add(node);
	}
}
