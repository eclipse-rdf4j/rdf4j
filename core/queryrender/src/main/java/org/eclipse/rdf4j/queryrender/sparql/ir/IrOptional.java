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
 * Textual IR node for an OPTIONAL block. The body is always printed with braces even when it contains a single line to
 * keep output shape stable for subsequent transforms and tests.
 */
public class IrOptional extends IrNode {
	private IrBGP bgp;

	public IrOptional(IrBGP bgp) {
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
		p.startLine();
		p.append("OPTIONAL ");
		if (ow != null) {
			ow.print(p); // IrBGP is responsible for braces
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
		return new IrOptional(newWhere);
	}
}
