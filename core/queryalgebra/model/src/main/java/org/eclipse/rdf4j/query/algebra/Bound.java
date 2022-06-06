/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * The BOUND function, as defined in <a href="http://www.w3.org/TR/rdf-sparql-query/#func-bound">SPARQL Query Language
 * for RDF</a>; checks if a variable is bound.
 *
 * @author Arjohn Kampman
 */
public class Bound extends AbstractQueryModelNode implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The operator's argument.
	 */
	protected Var arg;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Bound() {
	}

	public Bound(Var arg) {
		setArg(arg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the argument of this unary value operator.
	 *
	 * @return The operator's argument.
	 */
	public Var getArg() {
		return arg;
	}

	/**
	 * Sets the argument of this unary value operator.
	 *
	 * @param arg The (new) argument for this operator, must not be <var>null</var>.
	 */
	public void setArg(Var arg) {
		assert arg != null : "arg must not be null";
		arg.setParentNode(this);
		this.arg = arg;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		arg.visit(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (arg == current) {
			setArg((Var) replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Bound) {
			Bound o = (Bound) other;
			return arg.equals(o.getArg());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return arg.hashCode() ^ "Bound".hashCode();
	}

	@Override
	public Bound clone() {
		Bound clone = (Bound) super.clone();
		clone.setArg(getArg().clone());
		return clone;
	}
}
