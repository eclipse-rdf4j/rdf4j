/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

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
