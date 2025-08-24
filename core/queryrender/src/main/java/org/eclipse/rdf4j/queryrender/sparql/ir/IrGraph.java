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
	private final Var graph;
	private final IrWhere where;

	public IrGraph(Var graph, IrWhere where) {
		this.graph = graph;
		this.where = where;
	}

	public Var getGraph() {
		return graph;
	}

	public IrWhere getWhere() {
		return where;
	}

	@Override
	public void print(IrPrinter p) {
		p.raw("GRAPH ");
		p.raw(p.renderVarOrValue(getGraph()));
		p.raw(" ");
		p.openBlock();
		p.printLines(getWhere().getLines());
		p.closeBlock();
	}
}
