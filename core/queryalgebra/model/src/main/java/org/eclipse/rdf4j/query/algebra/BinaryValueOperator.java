/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * An abstract superclass for binary value operators which, by definition, has two arguments.
 */
public abstract class BinaryValueOperator extends AbstractQueryModelNode implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The operator's left argument.
	 */
	protected ValueExpr leftArg;

	/**
	 * The operator's right argument.
	 */
	protected ValueExpr rightArg;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected BinaryValueOperator() {
	}

	/**
	 * Creates a new binary value operator.
	 *
	 * @param leftArg  The operator's left argument, must not be <var>null</var>.
	 * @param rightArg The operator's right argument, must not be <var>null</var>.
	 */
	protected BinaryValueOperator(ValueExpr leftArg, ValueExpr rightArg) {
		setLeftArg(leftArg);
		setRightArg(rightArg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the left argument of this binary value operator.
	 *
	 * @return The operator's left argument.
	 */
	public ValueExpr getLeftArg() {
		return leftArg;
	}

	/**
	 * Sets the left argument of this binary value operator.
	 *
	 * @param leftArg The (new) left argument for this operator, must not be <var>null</var>.
	 */
	public void setLeftArg(ValueExpr leftArg) {
		assert leftArg != null : "leftArg must not be null";
		leftArg.setParentNode(this);
		this.leftArg = leftArg;
	}

	/**
	 * Gets the right argument of this binary value operator.
	 *
	 * @return The operator's right argument.
	 */
	public ValueExpr getRightArg() {
		return rightArg;
	}

	/**
	 * Sets the right argument of this binary value operator.
	 *
	 * @param rightArg The (new) right argument for this operator, must not be <var>null</var>.
	 */
	public void setRightArg(ValueExpr rightArg) {
		assert rightArg != null : "rightArg must not be null";
		rightArg.setParentNode(this);
		this.rightArg = rightArg;
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		leftArg.visit(visitor);
		rightArg.visit(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (leftArg == current) {
			setLeftArg((ValueExpr) replacement);
		} else if (rightArg == current) {
			setRightArg((ValueExpr) replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BinaryValueOperator) {
			BinaryValueOperator o = (BinaryValueOperator) other;
			return leftArg.equals(o.getLeftArg()) && rightArg.equals(o.getRightArg());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return leftArg.hashCode() ^ rightArg.hashCode();
	}

	@Override
	public BinaryValueOperator clone() {
		BinaryValueOperator clone = (BinaryValueOperator) super.clone();
		clone.setLeftArg(getLeftArg().clone());
		clone.setRightArg(getRightArg().clone());
		return clone;
	}
}
