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

import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Textual IR node representing a GRAPH block with an inner group.
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

	public IrBGP getWhere() {
		return bgp;
	}

	public void setGraph(Var graph) {
		this.graph = graph;
	}

	public void setWhere(IrBGP bgp) {
		this.bgp = bgp;
	}

	@Override
	public void print(IrPrinter p) {
		p.line("GRAPH " + p.renderVarOrValue(getGraph()) + " {");
		p.pushIndent();
		p.printLines(getWhere().getLines());
		p.popIndent();
		p.line("}");
	}

	@Override
	public IrNode transformChildren(java.util.function.UnaryOperator<IrNode> op) {
		IrBGP newWhere = this.bgp;
		if (newWhere != null) {
			IrNode t = op.apply(newWhere);
			if (t instanceof IrBGP) {
				newWhere = (IrBGP) t;
			}
		}
		return new IrGraph(this.graph, newWhere);
	}
}
