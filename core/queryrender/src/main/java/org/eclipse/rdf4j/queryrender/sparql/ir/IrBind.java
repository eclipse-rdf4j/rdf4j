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
 * Textual IR node for a BIND assignment.
 */
public class IrBind extends IrNode {
	private final String exprText;
	private final String varName;

	public IrBind(String exprText, String varName) {
		this(exprText, varName, false);
	}

	public IrBind(String exprText, String varName, boolean newScope) {
		super(newScope);
		this.exprText = exprText;
		this.varName = varName;
	}

	public String getExprText() {
		return exprText;
	}

	public String getVarName() {
		return varName;
	}

	@Override
	public void print(IrPrinter p) {
		p.line("BIND(" + exprText + " AS ?" + varName + ")");
	}
}
