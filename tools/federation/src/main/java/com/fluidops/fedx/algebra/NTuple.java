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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.fluidops.fedx.structures.QueryInfo;


/**
 * Base class for any nary-tuple expression
 * 
 * @author Andreas Schwarte
 * 
 * @see NJoin
 * @see NUnion
 */
public abstract class NTuple extends AbstractQueryModelNode implements TupleExpr, QueryRef
{

	private static final long serialVersionUID = -4899531533519154174L;

	protected final List<TupleExpr> args;
	protected final QueryInfo queryInfo;
	
	/**
	 * Construct an nary-tuple. Note that the parentNode of all arguments is
	 * set to this instance.
	 * 
	 * @param args
	 */
	public NTuple(List<TupleExpr> args, QueryInfo queryInfo) {
		super();
		this.queryInfo = queryInfo;
		this.args = args;
		for (TupleExpr expr : args)
			expr.setParentNode(this);
	}
	
	public TupleExpr getArg(int i) {
		return args.get(i);
	}
	
	public List<TupleExpr> getArgs() {
		return args;
	}
	
	public int getNumberOfArguments() {
		return args.size();
	}
	
	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		for (TupleExpr expr : args)
			expr.visit(visitor);
	}
	
	@Override
	public NTuple clone() {
		return (NTuple)super.clone();
	}
	
	@Override
	public Set<String> getAssuredBindingNames() {
		Set<String> res = new LinkedHashSet<String>(16);
		for (TupleExpr e : args) {
			res.addAll(e.getAssuredBindingNames());
		}
		return res;
	}

	@Override
	public Set<String> getBindingNames() {
		Set<String> res = new LinkedHashSet<String>(16);
		for (TupleExpr e : args) {
			res.addAll(e.getBindingNames());
		}
		return res;
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		int index = args.indexOf(current);

		if (index >= 0) 
			args.set(index, (TupleExpr)replacement);
		else 
			super.replaceChildNode(current, replacement);	
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meetOther(this);		
	}

	@Override
	public QueryInfo getQueryInfo() {
		return queryInfo;
	}
}
