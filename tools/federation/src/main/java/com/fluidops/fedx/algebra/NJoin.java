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
package com.fluidops.fedx.algebra;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.fluidops.fedx.optimizer.JoinOrderOptimizer;
import com.fluidops.fedx.structures.QueryInfo;

/**
 * A tuple expression that represents an nary-Join.
 * 
 * @author Andreas Schwarte
 *
 */
public class NJoin extends NTuple implements TupleExpr {
	
	private static final long serialVersionUID = -8646701006458860154L;

	/**
	 * Construct an nary-tuple. Note that the parentNode of all arguments is
	 * set to this instance.
	 * 
	 * @param args
	 */
	public NJoin(List<TupleExpr> args, QueryInfo queryInfo) {
		super(args, queryInfo);
	}
	
	
	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);		
	}
	
	
	@Override
	public NJoin clone() {
		return (NJoin)super.clone();
	}
	
	/**
	 * Returns the commons variables of the join with the given index.
	 * 
	 * @param joinIndex the join index, starting with 1
	 * @return the set of join variables
	 */
	public Set<String> getJoinVariables(int joinIndex) {

		Set<String> joinVars = new HashSet<>();
		joinVars.addAll(JoinOrderOptimizer.getFreeVars(getArg(joinIndex - 1)));
		joinVars.retainAll(JoinOrderOptimizer.getFreeVars(getArg(joinIndex)));
		return joinVars;
	}
}
