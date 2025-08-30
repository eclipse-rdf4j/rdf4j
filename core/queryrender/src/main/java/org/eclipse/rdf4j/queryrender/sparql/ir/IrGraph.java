/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.ir;

import java.util.function.UnaryOperator;

import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Textual IR node representing a GRAPH block with an inner group.
 *
 * The graph reference is modelled as a {@link Var} so it can be either a bound IRI
 * (rendered via {@code <...>} or prefix) or an unbound variable name. The body is a nested {@link IrBGP}.
 */
public class IrGraph extends IrNode {
	private Var graph;
	private IrBGP bgp;

	public IrGraph(Var graph, IrBGP bgp) {
		this.graph = graph;
		this.bgp = bgp;
	}

	public Var getGraph() {
		return graph;
	}

	public void setGraph(Var graph) {
		this.graph = graph;
	}

	public IrBGP getWhere() {
		return bgp;
	}

	public void setWhere(IrBGP bgp) {
		this.bgp = bgp;
	}

	@Override
	public void print(IrPrinter p) {
		p.startLine();
		p.append("GRAPH " + p.renderVarOrValue(getGraph()) + " ");
		IrBGP inner = getWhere();
		if (inner != null) {
			inner.print(p); // IrBGP prints braces
		} else {
			p.openBlock();
			p.closeBlock();
		}
	}

	@Override
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		IrBGP newWhere = this.bgp;
		if (newWhere != null) {
			IrNode t = op.apply(newWhere);
			t = t.transformChildren(op);
			if (t instanceof IrBGP) {
				newWhere = (IrBGP) t;
			}
		}
		return new IrGraph(this.graph, newWhere);
	}
}
