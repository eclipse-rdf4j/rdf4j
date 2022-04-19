/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * A mathematical expression consisting an operator and two arguments.
 */
public class MathExpr extends BinaryValueOperator {

	/*---------------*
	 * enum Operator *
	 *---------------*/

	public enum MathOp {
		PLUS("+"),
		MINUS("-"),
		MULTIPLY("*"),
		DIVIDE("/");

		private final String symbol;

		MathOp(String symbol) {
			this.symbol = symbol;
		}

		public String getSymbol() {
			return symbol;
		}
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	private MathOp operator;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public MathExpr() {
	}

	public MathExpr(ValueExpr leftArg, ValueExpr rightArg, MathOp operator) {
		super(leftArg, rightArg);
		setOperator(operator);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public MathOp getOperator() {
		return operator;
	}

	public void setOperator(MathOp operator) {
		assert operator != null : "operator must not be null";
		this.operator = operator;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public String getSignature() {
		return super.getSignature() + " (" + operator.getSymbol() + ")";
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof MathExpr && super.equals(other)) {
			MathExpr o = (MathExpr) other;
			return operator.equals(o.getOperator());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ operator.hashCode();
	}

	@Override
	public MathExpr clone() {
		return (MathExpr) super.clone();
	}
}
