/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.ast;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class SimpleNode implements Node {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	protected Node parent;

	protected List<Node> children;

	protected int id;

	protected SyntaxTreeBuilder parser;

	private boolean isScopeChange = false;

	public SimpleNode(int id) {
		this.id = id;
		children = new ArrayList<>();
	}

	public SimpleNode(SyntaxTreeBuilder parser, int id) {
		this(id);
		this.parser = parser;
	}

	@Override
	public void jjtOpen() {
	}

	@Override
	public void jjtClose() {
	}

	@Override
	public void jjtSetParent(Node n) {
		parent = n;
	}

	@Override
	public Node jjtGetParent() {
		return parent;
	}

	@Override
	public void jjtAddChild(Node n, int i) {
		while (i >= children.size()) {
			// Add dummy nodes
			children.add(null);
		}

		children.set(i, n);
	}

	@Override
	public void jjtAppendChild(Node n) {
		children.add(n);
	}

	@Override
	public void jjtInsertChild(Node n, int i) {
		children.add(i, n);
	}

	@Override
	public void jjtReplaceChild(Node oldNode, Node newNode) {
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) == oldNode) {
				children.set(i, newNode);
			}
		}
	}

	/**
	 * Replaces this node with the supplied one in the AST.
	 *
	 * @param newNode The replacement node.
	 */
	public void jjtReplaceWith(Node newNode) {
		if (parent != null) {
			parent.jjtReplaceChild(this, newNode);
		}

		for (Node childNode : children) {
			childNode.jjtSetParent(newNode);
		}
	}

	public List<Node> jjtGetChildren() {
		return children;
	}

	@Override
	public Node jjtGetChild(int i) {
		return children.get(i);
	}

	/**
	 * Gets the (first) child of this node that is of the specific type.
	 *
	 * @param type The type of the child node that should be returned.
	 * @return The (first) child node of the specified type, or <tt>null</tt> if no such child node was found.
	 */
	public <T extends Node> T jjtGetChild(Class<T> type) {
		for (Node n : children) {
			if (type.isInstance(n)) {
				return (T) n;
			}
		}

		return null;
	}

	public <T extends Node> List<T> jjtGetChildren(Class<T> type) {
		List<T> result = new ArrayList<>(children.size());

		for (Node n : children) {
			if (type.isInstance(n)) {
				result.add((T) n);
			}
		}

		return result;
	}

	@Override
	public int jjtGetNumChildren() {
		return children.size();
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
		return visitor.visit(this, data);
	}

	/**
	 * Accept the visitor.
	 */
	public Object childrenAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
		for (Node childNode : children) {
			// Note: modified JavaCC code, child's data no longer ignored
			data = childNode.jjtAccept(visitor, data);
		}

		return data;
	}

	/*
	 * You can override these two methods in subclasses of SimpleNode to customize the way the node appears when the
	 * tree is dumped. If your output uses more than one line you should override toString(String), otherwise overriding
	 * toString() is probably all you need to do.
	 */

	@Override
	public String toString() {
		return SyntaxTreeBuilderTreeConstants.jjtNodeName[id];
	}

	public String toString(String prefix) {
		return prefix + toString();
	}

	/**
	 * Writes a tree-like representation of this node and all of its subnodes (recursively) to the supplied Appendable.
	 */
	public void dump(String prefix, Appendable out) throws IOException {
		out.append(prefix).append(this.toString());

		for (Node childNode : children) {
			if (childNode != null) {
				out.append(LINE_SEPARATOR);
				((SimpleNode) childNode).dump(prefix + " ", out);
			}
		}
	}

	/**
	 * Writes a tree-like representation of this node and all of its subnodes (recursively) and returns it as a string.
	 */
	public String dump(String prefix) {
		StringWriter out = new StringWriter(256);
		try {
			dump(prefix, out);
			return out.toString();
		} catch (IOException e) {
			throw new RuntimeException("Unexpected I/O error while writing to StringWriter", e);
		}
	}

	/**
	 * Check if this AST node constitutes a variable scope change.
	 *
	 * @return the isScopeChange
	 */
	public boolean isScopeChange() {
		return isScopeChange;
	}

	/**
	 * @param isScopeChange the isScopeChange to set
	 */
	public void setScopeChange(boolean isScopeChange) {
		this.isScopeChange = isScopeChange;
	}
}
