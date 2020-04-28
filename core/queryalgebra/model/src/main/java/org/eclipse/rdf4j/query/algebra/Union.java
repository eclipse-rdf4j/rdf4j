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

/**
 * The UNION set operator, which return the union of the result sets of two tuple expressions.
 */
public class Union extends BinaryTupleOperator {

	// Used to differentiate between Unions introduced by the user that require a new scope and unions introduced for
	// evaluation or performance purposes that do not introduce a new scope. This means that they can be executed more
	// efficiently.
	private boolean newScope;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Union() {
		this(true);
	}

	public Union(boolean newScope) {
		this.newScope = newScope;
	}

	/**
	 * Creates a new union operator that operates on the two specified arguments.
	 *
	 * @param leftArg  The left argument of the union operator.
	 * @param rightArg The right argument of the union operator.
	 */
	public Union(TupleExpr leftArg, TupleExpr rightArg) {
		this(leftArg, rightArg, true);
	}

	public Union(TupleExpr leftArg, TupleExpr rightArg, boolean newScope) {
		super(leftArg, rightArg);
		this.newScope = newScope;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Set<String> getBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<>(16);
		bindingNames.addAll(getLeftArg().getBindingNames());
		bindingNames.addAll(getRightArg().getBindingNames());
		return bindingNames;
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<>(16);
		bindingNames.addAll(getLeftArg().getAssuredBindingNames());
		bindingNames.retainAll(getRightArg().getAssuredBindingNames());
		return bindingNames;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Union && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Union".hashCode();
	}

	@Override
	public Union clone() {
		Union clone = (Union) super.clone();
		clone.newScope = this.newScope;
		return clone;
	}

	public boolean isNewScope() {
		return newScope;
	}

	public void setNewScope(boolean newScope) {
		this.newScope = newScope;
	}
}
