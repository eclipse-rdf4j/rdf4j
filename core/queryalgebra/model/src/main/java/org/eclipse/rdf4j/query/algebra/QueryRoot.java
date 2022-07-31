/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * A semantics-less query model node that is used as the root of query model trees. This is a placeholder that
 * facilitates modifications to query model trees, including the replacement of the actual (semantically relevant) root
 * node with another root node.
 *
 * @author Arjohn Kampman
 */
public class QueryRoot extends UnaryTupleOperator {

	private QueryModelNode parent;

	public QueryRoot() {
		super();
	}

	public QueryRoot(TupleExpr tupleExpr) {
		super(tupleExpr);
	}

	@Override
	public void setParentNode(QueryModelNode parent) {
		if (parent instanceof QueryRoot) {
			this.parent = parent;
		} else {
			throw new UnsupportedOperationException("Not allowed to set a parent on a QueryRoot object");
		}
	}

	@Override
	public QueryModelNode getParentNode() {
		return this.parent;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof QueryRoot && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "QueryRoot".hashCode();
	}

	@Override
	public QueryRoot clone() {
		return (QueryRoot) super.clone();
	}
}
