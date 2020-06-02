/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * The IF function, as defined in SPARQL 1.1 Query.
 *
 * @author Jeen Broekstra
 */
public class If extends AbstractQueryModelNode implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The operator's arguments.
	 */
	private ValueExpr condition;

	private ValueExpr result;

	private ValueExpr alternative;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public If() {
	}

	public If(ValueExpr condition) {
		setCondition(condition);
	}

	public If(ValueExpr condition, ValueExpr result) {
		setCondition(condition);
		setResult(result);
	}

	public If(ValueExpr condition, ValueExpr result, ValueExpr alternative) {
		setCondition(condition);
		setResult(result);
		setAlternative(alternative);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the argument of this unary value operator.
	 *
	 * @return The operator's argument.
	 */
	public ValueExpr getCondition() {
		return condition;
	}

	/**
	 * Sets the condition argument of this unary value operator.
	 *
	 * @param condition The (new) condition argument for this operator, must not be <tt>null</tt>.
	 */
	public void setCondition(ValueExpr condition) {
		assert condition != null : "arg must not be null";
		condition.setParentNode(this);
		this.condition = condition;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		condition.visit(visitor);
		if (result != null) {
			result.visit(visitor);
		}
		if (alternative != null) {
			alternative.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (condition == current) {
			setCondition((ValueExpr) replacement);
		} else if (result == current) {
			setResult((ValueExpr) replacement);
		} else if (alternative == current) {
			setAlternative((ValueExpr) replacement);
		} else {
			super.replaceChildNode(current, replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof If) {
			If o = (If) other;

			boolean equal = condition.equals(o.getCondition());
			if (!equal) {
				return equal;
			}

			equal = (result == null) ? o.getResult() == null : result.equals(o.getResult());
			if (!equal) {
				return equal;
			}

			equal = (alternative == null) ? o.getAlternative() == null : alternative.equals(o.getAlternative());

			return equal;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = condition.hashCode();

		if (result != null) {
			hashCode = hashCode ^ result.hashCode();
		}
		if (alternative != null) {
			hashCode = hashCode ^ alternative.hashCode();
		}

		hashCode = hashCode ^ "If".hashCode();

		return hashCode;
	}

	@Override
	public If clone() {
		If clone = (If) super.clone();
		clone.setCondition(condition.clone());
		if (result != null) {
			clone.setResult(result.clone());
		}
		if (alternative != null) {
			clone.setAlternative(alternative.clone());
		}
		return clone;
	}

	/**
	 * @param result The result to set.
	 */
	public void setResult(ValueExpr result) {
		result.setParentNode(this);
		this.result = result;

	}

	/**
	 * @return Returns the result.
	 */
	public ValueExpr getResult() {
		return result;
	}

	/**
	 * @param alternative The alternative to set.
	 */
	public void setAlternative(ValueExpr alternative) {
		alternative.setParentNode(this);
		this.alternative = alternative;
	}

	/**
	 * @return Returns the alternative.
	 */
	public ValueExpr getAlternative() {
		return alternative;
	}
}
