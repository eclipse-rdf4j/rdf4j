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

import java.util.Objects;

/**
 * @author jeen
 */
public class Load extends AbstractQueryModelNode implements UpdateExpr {

	private ValueConstant source;

	private ValueConstant graph;

	private boolean silent;

	public Load(ValueConstant source) {
		setSource(source);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		if (source != null) {
			source.visit(visitor);
		}
		if (graph != null) {
			graph.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (source == current) {
			setSource((ValueConstant) current);
		} else if (graph == current) {
			setGraph((ValueConstant) current);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Load) {
			Load o = (Load) other;
			return silent == o.silent && Objects.equals(source, o.source) && Objects.equals(graph, o.graph);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = silent ? 1 : 0;
		if (source != null) {
			result ^= source.hashCode();
		}
		if (graph != null) {
			result ^= graph.hashCode();
		}
		return result;
	}

	@Override
	public Load clone() {
		Load clone = new Load(source.clone());
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
	 * @param source The source to set.
	 */
	public void setSource(ValueConstant source) {
		this.source = source;
	}

	/**
	 * @return Returns the source.
	 */
	public ValueConstant getSource() {
		return source;
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
