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
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;


/**
 * ConjunctiveFilterExpr maintains a list of conjunctive (i.e. AND connected) constraints.
 * 
 * @author Andreas Schwarte
 *
 */
public class ConjunctiveFilterExpr extends AbstractQueryModelNode implements FilterValueExpr
{
	private static final long serialVersionUID = -4016335014136286638L;
	
	protected List<FilterExpr> expressions;
	
	public ConjunctiveFilterExpr(FilterExpr expr1, FilterExpr expr2) {
		this.expressions = new ArrayList<FilterExpr>(3);
		addExpression(expr1);
		addExpression(expr2);
	}
	
	public ConjunctiveFilterExpr(Collection<FilterExpr> expressions) {
		if (expressions.size()<2)
			throw new IllegalArgumentException("Conjunctive Expression must have at least two arguments.");
		this.expressions = new ArrayList<FilterExpr>(expressions.size());
		for (FilterExpr expr : expressions)
			addExpression(expr);
	}
	
	public void addExpression(FilterExpr expr) {
		// TODO use some priority ordering: selective filters should be evaluated first (shortcuts!)
		expressions.add(expr);
	}
	
	public List<FilterExpr> getExpressions() {
		return expressions;
	}
	
	@Override
	public ConjunctiveFilterExpr clone() {
		return (ConjunctiveFilterExpr)super.clone();
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);	
	}
	
	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X {
		super.visitChildren(visitor);
		for (FilterExpr expr : expressions)
			expr.getExpression().visit(visitor);
	}

}
