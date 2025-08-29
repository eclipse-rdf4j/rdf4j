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
 * Structured FILTER body representing logical NOT applied to an inner body (e.g., NOT EXISTS {...}).
 */
public class IrNot extends IrNode {
	private IrNode inner;

	public IrNot(IrNode inner) {
		this.inner = inner;
	}

	public IrNode getInner() {
		return inner;
	}

	public void setInner(IrNode inner) {
		this.inner = inner;
	}

	@Override
	public void print(IrPrinter p) {
		p.append("NOT ");
		if (inner != null) {
			inner.print(p);
		} else {
			p.endLine();
		}
	}

	@Override
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		IrNode n = this.inner;
		if (n != null) {
			IrNode t = op.apply(n);
			t = t.transformChildren(op);
			n = t;
		}
		return new IrNot(n);
	}
}
