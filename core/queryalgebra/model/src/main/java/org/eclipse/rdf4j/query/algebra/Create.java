/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * @author jeen
 */
public class Create extends AbstractQueryModelNode implements UpdateExpr {

	private ValueConstant graph;

	private boolean silent;

	public Create() {
		super();
	}

	public Create(ValueConstant graph) {
		super();
		setGraph(graph);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		if (graph != null) {
			graph.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (graph == current) {
			setGraph((ValueConstant) replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Create) {
			Create o = (Create) other;
			return silent == o.silent && nullEquals(graph, o.graph);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = silent ? 1 : 0;
		if (graph != null) {
			result ^= graph.hashCode();
		}
		return result;
	}

	@Override
	public Create clone() {
		Create clone = new Create();
		clone.setSilent(isSilent());
		if (getGraph() != null) {
			clone.setGraph(getGraph().clone());
		}
		return clone;
	}

	/**
	 * @param graph The graph to set.
	 */
	public void setGraph(ValueConstant graph) {
		this.graph = graph;
	}

	/**
	 * @return Returns the graph.
	 */
	public ValueConstant getGraph() {
		return graph;
	}

	/**
	 * @param silent The silent to set.
	 */
	public void setSilent(boolean silent) {
		this.silent = silent;
	}

	/**
	 * @return Returns the silent.
	 */
	@Override
	public boolean isSilent() {
		return silent;
	}

}
