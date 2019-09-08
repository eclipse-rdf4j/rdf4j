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

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.ValueExpr;


/**
 * FilterExpr maintains information for a particular FILTER expression.
 * 
 * @author Andreas Schwarte
 *
 */
public class FilterExpr extends AbstractQueryModelNode implements FilterValueExpr
{

	private static final long serialVersionUID = -6594037345260846807L;

	protected ValueExpr expr;
	protected HashSet<String> vars;

	public FilterExpr(ValueExpr expr, HashSet<String> vars) {
		super();
		this.expr = expr;
		this.vars = vars;
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
		expr.visit(visitor);
	}

	public ValueExpr getExpression() {
		return expr;
	}

	public HashSet<String> getVars() {
		return vars;
	}
	
	@Override
	public FilterExpr clone() {
		return (FilterExpr)super.clone();
	}
	
	public boolean isCompareEq() {
		return expr instanceof Compare && ((Compare)expr).getOperator()==CompareOp.EQ;
	}
}
