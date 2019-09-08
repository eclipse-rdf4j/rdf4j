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

import java.util.Collection;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

import com.fluidops.fedx.structures.QueryInfo;


public class FedXService extends AbstractQueryModelNode implements TupleExpr, BoundJoinTupleExpr
{

	private static final long serialVersionUID = 7179501550561942879L;

	protected Service expr;
	protected transient QueryInfo queryInfo;
	protected boolean simple = true;		// consists of BGPs only
	protected int nTriples = 0;

	
	public FedXService(Service expr, QueryInfo queryInfo) {
		this.expr = expr;
		this.queryInfo = queryInfo;
		expr.visit(new ServiceAnalyzer());
	}

	
	public Service getService() {
		return this.expr;
	}
	
	public QueryInfo getQueryInfo() {
		return queryInfo;
	}
	
	public int getNumberOfTriplePatterns() {
		return nTriples;
	}
	
	public boolean isSimple() {
		return simple;
	}
	
	public Collection<String> getFreeVars() {
		return expr.getServiceVars();
	}
	
	public int getFreeVarCount() {
		return expr.getServiceVars().size();
	}
	
	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);		
	}
	
	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		expr.visit(visitor);
	}
	
	
	@Override
	public FedXService clone() {
		return (FedXService)super.clone();
	}


	@Override
	public Set<String> getAssuredBindingNames()
	{
		return expr.getAssuredBindingNames();
	}


	@Override
	public Set<String> getBindingNames()
	{
		return expr.getBindingNames();
	}	
	
	
	private class ServiceAnalyzer extends AbstractQueryModelVisitor<RuntimeException>
	{

		@Override
		protected void meetNode(QueryModelNode node)
		{
			if (node instanceof StatementTupleExpr) {
				nTriples++;
			} else if (node instanceof StatementPattern) {
				nTriples++;
			} else if (node instanceof Filter) {
				simple=false;
			} else if (node instanceof Union){
				simple=false;
			}
				
			super.meetNode(node);
		}		
		
	}
}
