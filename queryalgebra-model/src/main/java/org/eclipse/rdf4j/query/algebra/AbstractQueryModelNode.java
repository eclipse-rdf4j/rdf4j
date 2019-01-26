/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.List;
import java.util.ListIterator;

import org.eclipse.rdf4j.query.algebra.helpers.QueryModelTreePrinter;

/**
 * Base implementation of {@link QueryModelNode}.
 */
public abstract class AbstractQueryModelNode implements QueryModelNode {

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final long serialVersionUID = 3006199552086476178L;

	private QueryModelNode parent;

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public QueryModelNode getParentNode() {
		return parent;
	}

	@Override
	public void setParentNode(QueryModelNode parent) {
		this.parent = parent;
	}

	/**
	 * Dummy implementation of {@link QueryModelNode#visitChildren} that does nothing. Subclasses should
	 * override this method when they have child nodes.
	 */
	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X
	{
	}

	/**
	 * Default implementation of {@link QueryModelNode#replaceChildNode(QueryModelNode, QueryModelNode)} that
	 * throws an {@link IllegalArgumentException} indicating that <tt>current</tt> is not a child node of this
	 * node.
	 */
	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		throw new IllegalArgumentException("Node is not a child node: " + current);
	}

	/**
	 * Default implementation of {@link QueryModelNode#replaceWith(QueryModelNode)} that throws an
	 * {@link IllegalArgumentException} indicating that <tt>current</tt> is not a child node of this node.
	 */
	@Override
	public void replaceWith(QueryModelNode replacement) {
		if (parent == null) {
			throw new IllegalStateException("Node has no parent");
		}

		parent.replaceChildNode(this, replacement);
	}

	/**
	 * Default implementation of {@link QueryModelNode#getSignature()} that prints the name of the node's
	 * class.
	 */
	@Override
	public String getSignature() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String toString() {
		QueryModelTreePrinter treePrinter = new QueryModelTreePrinter();
		this.visit(treePrinter);
		return treePrinter.getTreeString();
	}

	@Override
	public AbstractQueryModelNode clone() {
		try {
			return (AbstractQueryModelNode)super.clone();
		}
		catch (CloneNotSupportedException e) {
			throw new RuntimeException("Query model nodes are required to be cloneable", e);
		}
	}

	protected <T extends QueryModelNode> boolean replaceNodeInList(List<T> list, QueryModelNode current,
			QueryModelNode replacement)
	{
		ListIterator<T> iter = list.listIterator();
		while (iter.hasNext()) {
			if (iter.next() == current) {
				iter.set((T)replacement);
				replacement.setParentNode(this);
				return true;
			}
		}

		return false;
	}

	protected boolean nullEquals(Object o1, Object o2) {
		return o1 == o2 || o1 != null && o1.equals(o2);
	}
}
