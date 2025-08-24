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
	private final IrWhere where;

	public IrService(String serviceRefText, boolean silent, IrWhere where) {
		this.serviceRefText = serviceRefText;
		this.silent = silent;
		this.where = where;
	}

	public String getServiceRefText() {
		return serviceRefText;
	}

	public boolean isSilent() {
		return silent;
	}

	public IrWhere getWhere() {
		return where;
	}

	@Override
	public void print(IrPrinter p) {
		p.raw("SERVICE ");
		if (silent) {
			p.raw("SILENT ");
		}
		p.raw(serviceRefText);
		p.raw(" ");
		p.openBlock();
		p.printLines(where.getLines());
		p.closeBlock();
	}
}
