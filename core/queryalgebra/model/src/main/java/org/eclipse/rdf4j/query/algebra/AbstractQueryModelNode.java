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
public abstract class AbstractQueryModelNode implements QueryModelNode, GraphPatternGroupable {

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final long serialVersionUID = 3006199552086476178L;

	private QueryModelNode parent;

	private boolean isGraphPatternGroup;

	private double resultSizeEstimate = -1;

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

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.rdf4j.query.algebra.GraphPatternGroupable#isGraphPatternGroup()
	 */
	@Override
	public boolean isGraphPatternGroup() {
		return isGraphPatternGroup;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.rdf4j.query.algebra.GraphPatternGroupable#setGraphPatternGroup(boolean)
	 */
	@Override
	public void setGraphPatternGroup(boolean isGraphPatternGroup) {
		this.isGraphPatternGroup = isGraphPatternGroup;
	}

	/**
	 * Dummy implementation of {@link QueryModelNode#visitChildren} that does nothing. Subclasses should override this
	 * method when they have child nodes.
	 */
	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
	}

	/**
	 * Default implementation of {@link QueryModelNode#replaceChildNode(QueryModelNode, QueryModelNode)} that throws an
	 * {@link IllegalArgumentException} indicating that <tt>current</tt> is not a child node of this node.
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
	 * Default implementation of {@link QueryModelNode#getSignature()} that prints the name of the node's class.
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
			return (AbstractQueryModelNode) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Query model nodes are required to be cloneable", e);
		}
	}

	protected <T extends QueryModelNode> boolean replaceNodeInList(List<T> list, QueryModelNode current,
			QueryModelNode replacement) {
		ListIterator<T> iter = list.listIterator();
		while (iter.hasNext()) {
			if (iter.next() == current) {
				iter.set((T) replacement);
				replacement.setParentNode(this);
				return true;
			}
		}

		return false;
	}

	protected boolean nullEquals(Object o1, Object o2) {
		return o1 == o2 || o1 != null && o1.equals(o2);
	}

	@Override
	public double getResultSizeEstimate() {
		return resultSizeEstimate;
	}

	@Override
	public void setResultSizeEstimate(double resultSizeEstimate) {
		this.resultSizeEstimate = resultSizeEstimate;
	}

	/**
	 *
	 * @return Human readable number. Eg. 12.1M for 1212213.4 and UNKNOWN for -1.
	 */
	static String toHumanReadbleNumber(double number) {
		String humanReadbleString;
		if (number > 1_000_000) {
			humanReadbleString = Math.round(number / 100_000) / 10.0 + "M";
		} else if (number > 1_000) {
			humanReadbleString = Math.round(number / 100) / 10.0 + "K";
		} else if (number > 0) {
			humanReadbleString = Math.round(number) + "";
		} else {
			humanReadbleString = "UNKNOWN";
		}

		return humanReadbleString;
	}

}
