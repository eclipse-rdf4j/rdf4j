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

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.Experimental;

/**
 * A natural join between two tuple expressions.
 */
public class Join extends BinaryTupleOperator {

	/**
	 * Indicates whether a join can use merge join.
	 */
	private boolean mergeJoin = false;

	/**
	 * Indicates whether a join can be cached. This also entails that no bindings from the parent node are actually used
	 * in this join.
	 */
	private boolean cacheable;

	public Join() {
	}

	public Join(TupleExpr leftArg, TupleExpr rightArg) {
		super(leftArg, rightArg);
	}

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
		bindingNames.addAll(getRightArg().getAssuredBindingNames());
		return bindingNames;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
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
		return (Join) super.clone();
	}

	@Experimental
	public boolean isMergeJoin() {
		return mergeJoin;
	}

	@Experimental
	public void setMergeJoin(boolean mergeJoin) {
		this.mergeJoin = mergeJoin;
	}

	@Experimental
	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}
}
