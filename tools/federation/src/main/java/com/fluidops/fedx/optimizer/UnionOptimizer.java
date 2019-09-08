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

import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

import com.fluidops.fedx.algebra.EmptyNUnion;
import com.fluidops.fedx.algebra.EmptyResult;
import com.fluidops.fedx.algebra.NUnion;
import com.fluidops.fedx.exception.OptimizationException;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * Optimizer to flatten the UNION operations.
 * 
 * @author Andreas Schwarte
 *
 */
public class UnionOptimizer extends AbstractQueryModelVisitor<OptimizationException> implements FedXOptimizer
{

	protected final QueryInfo queryInfo;
		
	public UnionOptimizer(QueryInfo queryInfo) {
		super();
		this.queryInfo = queryInfo;
	}

	@Override
	public void optimize(TupleExpr tupleExpr) {
		tupleExpr.visit(this);
	}
	
	
	@Override
	public void meet(Union union) {
		
		// retrieve the union arguments, also those of nested unions
		List<TupleExpr> args = new ArrayList<TupleExpr>();
		handleUnionArgs(union, args);
		
		// remove any tuple expressions that do not produce any result
		List<TupleExpr> filtered = new ArrayList<TupleExpr>(args.size());
		for (TupleExpr arg : args) {
			if (arg instanceof EmptyResult)
				continue;
			filtered.add(arg);
		}
		
		// create a NUnion having the arguments in one layer
		// however, check if we only have zero or one argument first
		if (filtered.size()==0) {
			union.replaceWith(new EmptyNUnion(args, queryInfo));
		}
		
		else if (filtered.size()==1) {
			union.replaceWith(filtered.get(0));
		}
		
		else {
			union.replaceWith( new NUnion(filtered, queryInfo) );			
		}
	}
	
	/**
	 * Add the union arguments to the args list, includes a recursion 
	 * step for nested unions.
	 * 
	 * @param union
	 * @param args
	 */
	protected void handleUnionArgs(Union union, List<TupleExpr> args) {
		
		if (union.getLeftArg() instanceof Union) {
			handleUnionArgs((Union)union.getLeftArg(), args);
		} else {
			args.add(union.getLeftArg());
		}
		
		if (union.getRightArg() instanceof Union) {
			handleUnionArgs((Union)union.getRightArg(), args);
		} else {
			args.add(union.getRightArg());
		}
	}

}
