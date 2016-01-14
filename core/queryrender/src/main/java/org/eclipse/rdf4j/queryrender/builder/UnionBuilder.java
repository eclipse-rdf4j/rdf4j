/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.builder;

import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.parser.ParsedQuery;

/**
 * <p>
 * Builder class for creating Unioned groups
 * </p>
 * 
 * @author Michael Grove
 * @since 2.7.0
 */
public class UnionBuilder<T extends ParsedQuery, E extends SupportsGroups> implements
		SupportsGroups<UnionBuilder<T, E>>, Group
{

	/**
	 * Left operand
	 */
	private Group mLeft;

	/**
	 * Right operand
	 */
	private Group mRight;

	/**
	 * Parent builder
	 */
	private GroupBuilder<T, E> mParent;

	public UnionBuilder(final GroupBuilder<T, E> theParent) {
		mParent = theParent;
	}

	/**
	 * Return a builder for creating the left operand of the union
	 * 
	 * @return builder for left operand
	 */
	public GroupBuilder<T, UnionBuilder<T, E>> left() {
		return new GroupBuilder<T, UnionBuilder<T, E>>(this);
	}

	/**
	 * Return a builder for creating the right operand of the union
	 * 
	 * @return builder for right operand
	 */
	public GroupBuilder<T, UnionBuilder<T, E>> right() {
		return new GroupBuilder<T, UnionBuilder<T, E>>(this);
	}

	/**
	 * Close this union and return it's parent group builder.
	 * 
	 * @return the parent builder
	 */
	public GroupBuilder<T, E> closeUnion() {
		return mParent;
	}

	/**
	 * @inheritDoc
	 */
	public int size() {
		return (mLeft == null ? 0 : mLeft.size()) + (mRight == null ? 0 : mRight.size());
	}

	/**
	 * @inheritDoc
	 */
	public UnionBuilder<T, E> addGroup(final Group theGroup) {
		if (mLeft == null) {
			mLeft = theGroup;
		}
		else if (mRight == null) {
			mRight = theGroup;
		}
		else {
			throw new IllegalArgumentException("Cannot set left or right arguments of union, both already set");
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	public UnionBuilder<T, E> removeGroup(final Group theGroup) {
		if (mLeft != null && mLeft.equals(theGroup)) {
			mLeft = null;
		}
		else if (mRight != null && mRight.equals(theGroup)) {
			mRight = null;
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	public void addChild(final Group theGroup) {
		addGroup(theGroup);
	}

	/**
	 * @inheritDoc
	 */
	public TupleExpr expr() {
		if (mLeft != null && mRight != null) {
			return new Union(mLeft.expr(), mRight.expr());
		}
		else if (mLeft != null && mRight == null) {
			return mLeft.expr();

		}
		else if (mRight != null && mLeft == null) {
			return mRight.expr();
		}
		else {
			return null;
		}
	}

	/**
	 * @inheritDoc
	 */
	public boolean isOptional() {
		return false;
	}
}
