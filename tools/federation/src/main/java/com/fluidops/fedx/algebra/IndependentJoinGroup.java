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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.fluidops.fedx.structures.QueryInfo;

public class IndependentJoinGroup extends AbstractQueryModelNode implements TupleExpr, QueryRef, BoundJoinTupleExpr
{
	private static final long serialVersionUID = -12440690622448600L;

	protected final List<StatementTupleExpr> members;
	protected final transient QueryInfo queryInfo;
		
	public IndependentJoinGroup(List<StatementTupleExpr> members, QueryInfo queryInfo) {
		super();
		this.queryInfo = queryInfo;
		this.members = members;
	}
	
	public IndependentJoinGroup(StatementTupleExpr stmt_a, StatementTupleExpr stmt_b, QueryInfo queryInfo) {
		super();
		this.queryInfo = queryInfo;
		this.members = new ArrayList<StatementTupleExpr>(2);
		this.members.add(stmt_a);
		this.members.add(stmt_b);
	}
	

	public List<StatementTupleExpr> getMembers() {
		return members;
	}
	
	public int getMemberCount() {
		return members.size();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> getBindingNames() {
		return Collections.emptySet();
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X {
		
		for (StatementTupleExpr s : members)
			s.visit(visitor);
	}
	
	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}
	
	@Override
	public IndependentJoinGroup clone() {
		throw new RuntimeException("Operation not supported on this node!");
	}

	@Override
	public QueryInfo getQueryInfo()	{
		return queryInfo;
	}
}
