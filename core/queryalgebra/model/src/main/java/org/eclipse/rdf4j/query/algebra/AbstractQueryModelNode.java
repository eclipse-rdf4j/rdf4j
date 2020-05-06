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
public abstract class AbstractQueryModelNode implements QueryModelNode, VariableScopeChange, GraphPatternGroupable {

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final long serialVersionUID = 3006199552086476178L;

	private QueryModelNode parent;

	private boolean isVariableScopeChange;

	private double resultSizeEstimate = -1;
	private long resultSizeActual = -1;
	private double costEstimate = -1;
	private long totalTimeNanosActual = -1;

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public QueryModelNode getParentNode() {
		return parent;
	}

	@Override
	public void setParentNode(QueryModelNode parent) {
		if (this.parent != null && this.parent != parent) {
			// debug here
			System.out.println();
		}
		this.parent = parent;
	}

	@Override
	public boolean isVariableScopeChange() {
		return isVariableScopeChange;
	}

	@Override
	public void setVariableScopeChange(boolean isVariableScopeChange) {
		this.isVariableScopeChange = isVariableScopeChange;
	}

	@Override
	@Deprecated
	public boolean isGraphPatternGroup() {
		return isVariableScopeChange();
	}

	@Override
	@Deprecated
	public void setGraphPatternGroup(boolean isGraphPatternGroup) {
		setVariableScopeChange(isGraphPatternGroup);
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
			AbstractQueryModelNode clone = (AbstractQueryModelNode) super.clone();
			clone.setVariableScopeChange(this.isVariableScopeChange());
			return clone;
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

	@Override
	public long getResultSizeActual() {
		return resultSizeActual;
	}

	@Override
	public void setResultSizeActual(long resultSizeActual) {
		this.resultSizeActual = resultSizeActual;
	}

	@Override
	public double getCostEstimate() {
		return costEstimate;
	}

	@Override
	public void setCostEstimate(double costEstimate) {
		this.costEstimate = costEstimate;
	}

	@Override
	public long getTotalTimeNanosActual() {
		return totalTimeNanosActual;
	}

	@Override
	public void setTotalTimeNanosActual(long totalTimeNanosActual) {
		this.totalTimeNanosActual = totalTimeNanosActual;
	}

	/**
	 *
	 * @return Human readable number. Eg. 12.1M for 1212213.4 and UNKNOWN for -1.
	 */
	static String toHumanReadbleNumber(double number) {
		String humanReadbleString;
		if (number == Double.POSITIVE_INFINITY) {
			humanReadbleString = "∞";
		} else if (number > 1_000_000) {
			humanReadbleString = Math.round(number / 100_000) / 10.0 + "M";
		} else if (number > 1_000) {
			humanReadbleString = Math.round(number / 100) / 10.0 + "K";
		} else if (number >= 0) {
			humanReadbleString = Math.round(number) + "";
		} else {
			humanReadbleString = "UNKNOWN";
		}

		return humanReadbleString;
	}
}
