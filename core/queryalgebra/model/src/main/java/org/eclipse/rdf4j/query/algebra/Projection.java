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

import java.util.Set;

/**
 * A generalized projection (allowing the bindings to be renamed) on a tuple expression.
 */
public class Projection extends UnaryTupleOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private ProjectionElemList projElemList;

	private Var projectionContext = null;

	private boolean subquery = true;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Projection() {
		projElemList = new ProjectionElemList();
	}

	public Projection(TupleExpr arg) {
		super(arg);
		projElemList = new ProjectionElemList();
	}

	public Projection(TupleExpr arg, ProjectionElemList elements) {
		super(arg);
		setProjectionElemList(elements);
	}

	public Projection(TupleExpr arg, ProjectionElemList elements, boolean subquery) {
		this(arg, elements);
		this.subquery = subquery;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public ProjectionElemList getProjectionElemList() {
		return projElemList;
	}

	public void setProjectionElemList(ProjectionElemList projElemList) {
		this.projElemList = projElemList;
		projElemList.setParentNode(this);
	}

	@Override
	public Set<String> getBindingNames() {
		return projElemList.getProjectedNames();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		// Return all target binding names for which the source binding is assured
		// by the argument
		return projElemList.getProjectedNamesFor(getArg().getAssuredBindingNames());
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		projElemList.visit(visitor);
		super.visitChildren(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (projElemList == current) {
			setProjectionElemList((ProjectionElemList) replacement);
		} else {
			super.replaceChildNode(current, replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Projection && super.equals(other)) {
			Projection o = (Projection) other;
			return projElemList.equals(o.getProjectionElemList());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ projElemList.hashCode();
	}

	@Override
	public Projection clone() {
		Projection clone = (Projection) super.clone();
		clone.setProjectionElemList(getProjectionElemList().clone());
		return clone;
	}

	/**
	 * @return Returns the projectionContext.
	 */
	public Var getProjectionContext() {
		return projectionContext;
	}

	/**
	 * @param projectionContext The projectionContext to set.
	 */
	public void setProjectionContext(Var projectionContext) {
		this.projectionContext = projectionContext;
	}

	public boolean isSubquery() {
		return subquery;
	}

	public void setSubquery(boolean subquery) {
		this.subquery = subquery;
	}
}
