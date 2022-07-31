/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Base class for any nary-tuple expression
 *
 * @author Andreas Schwarte
 *
 * @see NJoin
 * @see NUnion
 */
public abstract class NTuple extends AbstractQueryModelNode implements TupleExpr, QueryRef {

	private static final long serialVersionUID = -4899531533519154174L;

	protected final List<TupleExpr> args;
	protected final QueryInfo queryInfo;

	/**
	 * Construct an nary-tuple. Note that the parentNode of all arguments is set to this instance.
	 *
	 * @param args
	 */
	public NTuple(List<TupleExpr> args, QueryInfo queryInfo) {
		super();
		this.queryInfo = queryInfo;
		this.args = args;
		for (TupleExpr expr : args) {
			expr.setParentNode(this);
		}
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
		for (TupleExpr expr : args) {
			expr.visit(visitor);
		}
	}

	@Override
	public NTuple clone() {
		return (NTuple) super.clone();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		Set<String> res = new LinkedHashSet<>(16);
		for (TupleExpr e : args) {
			res.addAll(e.getAssuredBindingNames());
		}
		return res;
	}

	@Override
	public Set<String> getBindingNames() {
		Set<String> res = new LinkedHashSet<>(16);
		for (TupleExpr e : args) {
			res.addAll(e.getBindingNames());
		}
		return res;
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		int index = args.indexOf(current);

		if (index >= 0) {
			args.set(index, (TupleExpr) replacement);
			replacement.setParentNode(this);
		} else {
			super.replaceChildNode(current, replacement);
		}
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
