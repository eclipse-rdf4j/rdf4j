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

/**
 * Textual IR node for a MINUS { ... } block. Similar to OPTIONAL and GRAPH, this is a container around a nested BGP.
 */
public class IrMinus extends IrNode {
	private IrBGP bgp;

	public IrMinus(IrBGP bgp, boolean newScope) {
		super(newScope);
		this.bgp = bgp;
	}

	public IrBGP getWhere() {
		return bgp;
	}

	@Override
	public void print(IrPrinter p) {
		IrBGP ow = getWhere();
		p.startLine();
		p.append("MINUS ");
		if (ow != null) {
			IrBGP body = ow;
			// Flatten a single nested IrBGP (no explicit new scope) to avoid redundant braces
			if (body.getLines().size() == 1 && body.getLines().get(0) instanceof IrBGP) {
				IrBGP inner = (IrBGP) body.getLines().get(0);
				if (!inner.isNewScope()) {
					body = inner;
				}
			}
			body.print(p); // IrBGP prints braces
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
		return new IrMinus(newWhere, this.isNewScope());
	}
}
