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

import java.util.Set;

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * A query which has a single relevant source. These queries can be sent entirely 
 * to the endpoint as-is.
 * 
 * @author Andreas Schwarte
 */
public class SingleSourceQuery extends AbstractQueryModelNode implements TupleExpr, QueryRef
{
	private static final long serialVersionUID = 5745172129911897271L;

	private final TupleExpr parsedQuery;
	private final transient Endpoint source;
	private final transient QueryInfo queryInfo;
		
	/**
	 * @param parsedQuery
	 * @param source
	 * @param queryInfo
	 */
	public SingleSourceQuery(TupleExpr parsedQuery, Endpoint source,
			QueryInfo queryInfo)
	{
		super();
		this.parsedQuery = parsedQuery;
		this.source = source;
		this.queryInfo = queryInfo;
	}

	public Endpoint getSource()	{
		return source;
	}

	public String getQueryString()	{
		return queryInfo.getQuery();
	}	

	@Override
	public QueryInfo getQueryInfo()
	{
		return queryInfo;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X
	{
		visitor.meetOther(this);
	}	
	
	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
			throws X {
		parsedQuery.visit(visitor);
		super.visitChildren(visitor);
	}
	

	@Override
	public String getSignature() {
		return super.getSignature() + " @" + source.getId();
	}

	@Override
	public Set<String> getBindingNames()
	{
		return parsedQuery.getBindingNames();
	}

	@Override
	public Set<String> getAssuredBindingNames()
	{
		return parsedQuery.getAssuredBindingNames();
	}

	@Override
	public SingleSourceQuery clone() {
		return (SingleSourceQuery)super.clone();
	}

}
