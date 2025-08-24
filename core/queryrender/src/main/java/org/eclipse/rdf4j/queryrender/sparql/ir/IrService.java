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
 * Textual IR node for a SERVICE block.
 */
public class IrService extends IrNode {
	private final String serviceRefText;
	private final boolean silent;
	private IrBGP bgp;

	public IrService(String serviceRefText, boolean silent, IrBGP bgp) {
		this.serviceRefText = serviceRefText;
		this.silent = silent;
		this.bgp = bgp;
	}

	public String getServiceRefText() {
		return serviceRefText;
	}

	public boolean isSilent() {
		return silent;
	}

	public IrBGP getWhere() {
		return bgp;
	}

	public void setWhere(IrBGP bgp) {
		this.bgp = bgp;
	}

	@Override
	public void print(IrPrinter p) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVICE ");
		if (silent) {
			sb.append("SILENT ");
		}
		sb.append(serviceRefText).append(" {");
		p.line(sb.toString());
		p.pushIndent();
		p.printLines(bgp.getLines());
		p.popIndent();
		p.line("}");
	}

	@Override
	public IrNode transformChildren(java.util.function.UnaryOperator<IrNode> op) {
		IrBGP newWhere = this.bgp;
		if (newWhere != null) {
			IrNode t = op.apply(newWhere);
			t = t.transformChildren(op);

			if (t instanceof IrBGP) {
				newWhere = (IrBGP) t;
			}
		}
		return new IrService(this.serviceRefText, this.silent, newWhere);
	}
}
