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
public class Add extends AbstractQueryModelNode implements UpdateExpr {

	private ValueConstant sourceGraph;

	private ValueConstant destinationGraph;

	private boolean silent;

	public Add() {
		super();
	}

	public Add(ValueConstant graph) {
		super();
		setSourceGraph(graph);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		if (sourceGraph != null) {
			sourceGraph.visit(visitor);
		}
		if (destinationGraph != null) {
			destinationGraph.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (sourceGraph == current) {
			setSourceGraph((ValueConstant) replacement);
		} else if (destinationGraph == current) {
			setDestinationGraph((ValueConstant) replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Add) {
			Add o = (Add) other;
			return silent == o.silent && nullEquals(sourceGraph, o.sourceGraph)
					&& nullEquals(destinationGraph, o.destinationGraph);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = silent ? 1 : 0;
		if (sourceGraph != null) {
			result ^= sourceGraph.hashCode();
		}
		if (destinationGraph != null) {
			result ^= destinationGraph.hashCode();
		}
		return result;
	}

	@Override
	public Add clone() {
		Add clone = new Add();
		clone.setSilent(isSilent());
		if (getSourceGraph() != null) {
			clone.setSourceGraph(getSourceGraph().clone());
		}
		return clone;
	}

	/**
	 * @param graph The graph to set.
	 */
	public void setSourceGraph(ValueConstant graph) {
		this.sourceGraph = graph;
	}

	/**
	 * The named graph from which to add. If null, the default graph should be used.
	 *
	 * @return Returns the graph.
	 */
	public ValueConstant getSourceGraph() {
		return sourceGraph;
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

	/**
	 * @param destinationGraph The destinationGraph to set.
	 */
	public void setDestinationGraph(ValueConstant destinationGraph) {
		this.destinationGraph = destinationGraph;
	}

	/**
	 * The named graph to which to add. If null, the default graph should be used.
	 *
	 * @return Returns the destinationGraph.
	 */
	public ValueConstant getDestinationGraph() {
		return destinationGraph;
	}

}
