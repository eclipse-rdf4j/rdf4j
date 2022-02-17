/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * A BNode generator, which generates a new BNode each time it needs to supply a value.
 */
public class BNodeGenerator extends AbstractQueryModelNode implements ValueExpr {

	private ValueExpr nodeIdExpr = null;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public BNodeGenerator() {
		super();
	}

	public BNodeGenerator(ValueExpr nodeIdExpr) {
		super();
		setNodeIdExpr(nodeIdExpr);
	}
	/*---------*
	 * Methods *
	 *---------*/

	public ValueExpr getNodeIdExpr() {
		return nodeIdExpr;
	}

	public void setNodeIdExpr(ValueExpr nodeIdExpr) {
		this.nodeIdExpr = nodeIdExpr;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		// no-op
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BNodeGenerator;
	}

	@Override
	public int hashCode() {
		return "BNodeGenerator".hashCode();
	}

	@Override
	public BNodeGenerator clone() {
		return (BNodeGenerator) super.clone();
	}
}
