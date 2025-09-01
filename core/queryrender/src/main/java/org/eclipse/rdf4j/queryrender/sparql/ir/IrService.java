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
 * Textual IR node for a SERVICE block.
 *
 * The reference is kept as already-rendered text to allow either a variable, IRI, or complex expression (as produced by
 * the renderer) and to preserve SILENT when present.
 */
public class IrService extends IrNode {
	private final String serviceRefText;
	private final boolean silent;
	private IrBGP bgp;

	public IrService(String serviceRefText, boolean silent, IrBGP bgp, boolean newScope) {
		super(newScope);
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

	@Override
	public void print(IrPrinter p) {
		p.startLine();
		p.append("SERVICE ");
		if (silent) {
			p.append("SILENT ");
		}
		p.append(serviceRefText);
		p.append(" ");
		IrBGP inner = bgp;
		// Rely solely on the transform pipeline for structural rewrites. Printing preserves
		// whatever grouping/GRAPH context the IR carries at this point.
		// Seriously, leave this alone! Let the inner section print itself.
		inner.print(p); // IrBGP prints braces

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
		return new IrService(this.serviceRefText, this.silent, newWhere, this.isNewScope());
	}
}
