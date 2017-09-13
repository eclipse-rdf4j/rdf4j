/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;

/**
 * A natural join between two tuple expressions.
 */
public class Join extends BinaryTupleOperator {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Join() {
	}

	/**
	 * Creates a new natural join operator.
	 */
	public Join(TupleExpr leftArg, TupleExpr rightArg) {
		super(leftArg, rightArg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * @deprecated Since 2.7.3. Use {@link TupleExprs#containsProjection(TupleExpr)} instead.
	 * @return <code>true</code> if the right argument of this Join contains a projection, <code>false</code>
	 *         otherwise.
	 */
	@Deprecated
	public boolean hasSubSelectInRightArg() {
		return TupleExprs.containsSubquery(rightArg);
	}

	public Set<String> getBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<String>(16);
		bindingNames.addAll(getLeftArg().getBindingNames());
		bindingNames.addAll(getRightArg().getBindingNames());
		return bindingNames;
	}

	public Set<String> getAssuredBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<String>(16);
		bindingNames.addAll(getLeftArg().getAssuredBindingNames());
		bindingNames.addAll(getRightArg().getAssuredBindingNames());
		return bindingNames;
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Join && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Join".hashCode();
	}

	@Override
	public Join clone() {
		return (Join)super.clone();
	}

}
