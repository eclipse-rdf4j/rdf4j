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

import java.util.ArrayList;
import java.util.List;

/**
 * Textual IR for a WHERE/group block: ordered list of lines/nodes.
 */
public class IrBGP extends IrNode {
	private List<IrNode> lines = new ArrayList<>();

	public List<IrNode> getLines() {
		return lines;
	}

	public void add(IrNode node) {
		if (node != null) {
			lines.add(node);
		}
	}

	public void setLines(List<IrNode> newLines) {
		this.lines = (newLines == null) ? new ArrayList<>() : new ArrayList<>(newLines);
	}

	@Override
	public void print(IrPrinter p) {
		p.openBlock();
		p.printLines(lines);
		p.closeBlock();
	}

	@Override
	public IrNode transformChildren(java.util.function.UnaryOperator<IrNode> op) {
		IrBGP w = new IrBGP();
		for (IrNode ln : this.lines) {
			IrNode t = op.apply(ln);
			w.add(t == null ? ln : t);
		}
		return w;
	}
}
