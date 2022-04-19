/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * A comparison between two values.
 */
public class Compare extends BinaryValueOperator {

	/*---------------*
	 * enum Operator *
	 *---------------*/

	public enum CompareOp {
		/** equal to */
		EQ("="),

		/** not equal to */
		NE("!="),

		/** lower than */
		LT("<"),

		/** lower than or equal to */
		LE("<="),

		/** greater than or equal to */
		GE(">="),

		/** greater than */
		GT(">");

		private final String symbol;

		CompareOp(String symbol) {
			this.symbol = symbol;
		}

		public String getSymbol() {
			return symbol;
		}
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	private CompareOp operator;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Compare() {
	}

	public Compare(ValueExpr leftArg, ValueExpr rightArg) {
		this(leftArg, rightArg, CompareOp.EQ);
	}

	public Compare(ValueExpr leftArg, ValueExpr rightArg, CompareOp operator) {
		super(leftArg, rightArg);
		setOperator(operator);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public CompareOp getOperator() {
		return operator;
	}

	public void setOperator(CompareOp operator) {
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
		if (other instanceof Compare && super.equals(other)) {
			Compare o = (Compare) other;
			return operator.equals(o.getOperator());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ operator.hashCode();
	}

	@Override
	public Compare clone() {
		return (Compare) super.clone();
	}
}
