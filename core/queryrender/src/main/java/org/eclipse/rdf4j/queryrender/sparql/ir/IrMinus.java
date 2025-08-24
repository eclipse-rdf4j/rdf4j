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

/**
 * Textual IR node for a MINUS { ... } block.
 */
public class IrMinus extends IrNode {
	private IrBGP bgp;

	public IrMinus(IrBGP bgp) {
		this.bgp = bgp;
	}

	public IrBGP getWhere() {
		return bgp;
	}

	public void setWhere(IrBGP bgp) {
		this.bgp = bgp;
	}

	@Override
	public void print(IrPrinter p) {
		IrBGP ow = getWhere();
		p.line("MINUS {");
		p.pushIndent();
		if (ow != null) {
			p.printLines(ow.getLines());
		}
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
		return new IrMinus(newWhere);
	}
}
