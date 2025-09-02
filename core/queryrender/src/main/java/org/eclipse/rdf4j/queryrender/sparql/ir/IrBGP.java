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
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Textual IR for a WHERE/group block: ordered list of lines/nodes.
 *
 * Semantics: - Lines typically include triples ({@link IrStatementPattern} or {@link IrPathTriple}), modifiers
 * ({@link IrFilter}, {@link IrBind}, {@link IrValues}), and container blocks such as {@link IrGraph},
 * {@link IrOptional}, {@link IrMinus}, {@link IrUnion}, {@link IrService}. - Order matters: most transforms preserve
 * relative order except where a local, safe rewrite explicitly requires adjacency. - Printing is delegated to
 * {@link IrPrinter}; indentation and braces are handled there.
 */
public class IrBGP extends IrNode {
	private List<IrNode> lines = new ArrayList<>();

	public IrBGP(boolean newScope) {
		super(newScope);
	}

	public IrBGP(IrBGP where, boolean b) {
		super(b);
		add(where);
	}

	public List<IrNode> getLines() {
		return lines;
	}

	public void add(IrNode node) {
		if (node != null) {
			lines.add(node);
		}
	}

	@Override
	public void print(IrPrinter p) {
		p.openBlock();
		if (isNewScope()) {
			p.openBlock();
		}
		p.printLines(lines);
		if (isNewScope()) {
			p.closeBlock();
		}
		p.closeBlock();
	}

	@Override
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		IrBGP w = new IrBGP(this.isNewScope());
		for (IrNode ln : this.lines) {
			IrNode t = op.apply(ln);
			t = t.transformChildren(op);
			w.add(t == null ? ln : t);
		}
		w.setNewScope(this.isNewScope());
		return w;
	}

	@Override
	public String toString() {
		return "IrBGP{" +
				"lines=" + Arrays.toString(lines.toArray()) +
				'}';
	}
}
