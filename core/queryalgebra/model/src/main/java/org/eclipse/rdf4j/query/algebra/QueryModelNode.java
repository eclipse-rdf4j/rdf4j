/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.io.Serializable;

/**
 * Main interface for all query model nodes.
 */
public interface QueryModelNode extends Cloneable, Serializable {

	/**
	 * Visits this node. The node reports itself to the visitor with the proper
	 * runtime type.
	 */
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X;

	/**
	 * Visits the children of this node. The node calls
	 * {@link #visit(QueryModelVisitor)} on all of its child nodes.
	 */
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X;

	/**
	 * Gets the node's parent.
	 * 
	 * @return The parent node, if any.
	 */
	public QueryModelNode getParentNode();

	/**
	 * Sets the node's parent.
	 * 
	 * @param parent
	 *        The parent node for this node.
	 */
	public void setParentNode(QueryModelNode parent);

	/**
	 * Replaces one of the child nodes with a new node.
	 * 
	 * @param current
	 *        The current child node.
	 * @param replacement
	 *        The new child node.
	 * @throws IllegalArgumentException
	 *         If <tt>current</tt> is not one of node's children.
	 * @throws ClassCastException
	 *         If <tt>replacement</tt> is of an incompatible type.
	 */
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement);

	/**
	 * Substitutes this node with a new node in the query model tree.
	 * 
	 * @param replacement
	 *        The new node.
	 * @throws IllegalStateException
	 *         If this node does not have a parent node.
	 * @throws ClassCastException
	 *         If <tt>replacement</tt> is of an incompatible type.
	 */
	public void replaceWith(QueryModelNode replacement);

	/**
	 * Returns <tt>true</tt> if this query model node and its children are
	 * recursively equal to <tt>o</tt> and its children.
	 */
	public boolean equals(Object o);

	/**
	 * Returns an indented print of the node tree, starting from this node.
	 */
	public String toString();

	/**
	 * Returns the signature of this query model node. Signatures normally
	 * include the node's name and any parameters, but not parent or child nodes.
	 * This method is used by {@link #toString()}.
	 * 
	 * @return The node's signature, e.g. <tt>SLICE (offset=10, limit=10)</tt>.
	 */
	public String getSignature();

	/**
	 * Returns a (deep) clone of this query model node. This method recursively
	 * clones the entire node tree, starting from this nodes.
	 * 
	 * @return A deep clone of this query model node.
	 */
	public QueryModelNode clone();
}
