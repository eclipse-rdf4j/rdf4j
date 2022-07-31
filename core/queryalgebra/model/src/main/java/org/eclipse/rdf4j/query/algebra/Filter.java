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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The FILTER operator, as defined in <a href="http://www.w3.org/TR/rdf-sparql-query/#defn_algFilter">SPARQL Query
 * Language for RDF</a>. The FILTER operator filters specific results from the underlying tuple expression based on a
 * configurable condition.
 *
 * @author Arjohn Kampman
 */
public class Filter extends UnaryTupleOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private ValueExpr condition;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Filter() {
	}

	public Filter(TupleExpr arg, ValueExpr condition) {
		super(arg);
		setCondition(condition);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public ValueExpr getCondition() {
		return condition;
	}

	public void setCondition(ValueExpr condition) {
		assert condition != null : "condition must not be null";
		condition.setParentNode(this);
		this.condition = condition;
	}

	@Override
	public Set<String> getBindingNames() {
		Set<String> result = getArg().getBindingNames();
		if (condition instanceof SubQueryValueOperator) {
			result = Stream
					.concat(result.stream(),
							((SubQueryValueOperator) condition).getSubQuery().getBindingNames().stream())
					.collect(Collectors.toSet());
		}
		return result;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		condition.visit(visitor);
		super.visitChildren(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (condition == current) {
			setCondition((ValueExpr) replacement);
		} else {
			super.replaceChildNode(current, replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Filter && super.equals(other)) {
			Filter o = (Filter) other;
			return condition.equals(o.getCondition());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ condition.hashCode();
	}

	@Override
	public Filter clone() {
		Filter clone = (Filter) super.clone();
		clone.setCondition(getCondition().clone());
		return clone;
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(128);

		sb.append(super.getSignature());

		return sb.toString();
	}

}
