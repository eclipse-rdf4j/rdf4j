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

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * An abstract superclass for binary tuple operators which, by definition, has two arguments.
 */
public abstract class BinaryTupleOperator extends AbstractQueryModelNode implements TupleExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The operator's left argument.
	 */
	protected TupleExpr leftArg;

	/**
	 * The operator's right argument.
	 */
	protected TupleExpr rightArg;

	// the name of the algorithm used to combine leftArg and rightArg
	private String algorithmName;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected BinaryTupleOperator() {
	}

	/**
	 * Creates a new binary tuple operator.
	 *
	 * @param leftArg  The operator's left argument, must not be <var>null</var>.
	 * @param rightArg The operator's right argument, must not be <var>null</var>.
	 */
	protected BinaryTupleOperator(TupleExpr leftArg, TupleExpr rightArg) {
		setLeftArg(leftArg);
		setRightArg(rightArg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the left argument of this binary tuple operator.
	 *
	 * @return The operator's left argument.
	 */
	public TupleExpr getLeftArg() {
		return leftArg;
	}

	/**
	 * Sets the left argument of this binary tuple operator.
	 *
	 * @param leftArg The (new) left argument for this operator, must not be <var>null</var>.
	 */
	public void setLeftArg(TupleExpr leftArg) {
		assert leftArg != null : "leftArg must not be null";
		assert leftArg != this : "leftArg must not be itself";
		leftArg.setParentNode(this);
		this.leftArg = leftArg;
	}

	/**
	 * Gets the right argument of this binary tuple operator.
	 *
	 * @return The operator's right argument.
	 */
	public TupleExpr getRightArg() {
		return rightArg;
	}

	/**
	 * Sets the right argument of this binary tuple operator.
	 *
	 * @param rightArg The (new) right argument for this operator, must not be <var>null</var>.
	 */
	public void setRightArg(TupleExpr rightArg) {
		assert rightArg != null : "rightArg must not be null";
		assert rightArg != this : "rightArg must not be itself";
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
			setLeftArg((TupleExpr) replacement);
		} else if (rightArg == current) {
			setRightArg((TupleExpr) replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BinaryTupleOperator) {
			BinaryTupleOperator o = (BinaryTupleOperator) other;
			return leftArg.equals(o.getLeftArg()) && rightArg.equals(o.getRightArg());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return leftArg.hashCode() ^ rightArg.hashCode();
	}

	@Override
	public BinaryTupleOperator clone() {
		BinaryTupleOperator clone = (BinaryTupleOperator) super.clone();

		TupleExpr leftArgClone = getLeftArg().clone();
		leftArgClone.setParentNode(clone);
		clone.leftArg = leftArgClone;

		TupleExpr rightArgClone = getRightArg().clone();
		rightArgClone.setParentNode(clone);
		clone.rightArg = rightArgClone;

		return clone;
	}

	@Experimental
	public void setAlgorithm(CloseableIteration<?, ?> iteration) {
		this.algorithmName = iteration.getClass().getSimpleName();
	}

	@Experimental
	public void setAlgorithm(String classSimpleName) {
		this.algorithmName = classSimpleName;
	}

	@Experimental
	public String getAlgorithmName() {
		return algorithmName;
	}
}
