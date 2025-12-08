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
 * Textual IR item in a SELECT projection: either a bare variable or (expr AS ?alias).
 */
public class IrProjectionItem {
	private final String exprText; // null for bare ?var
	private final String varName; // name without leading '?'

	public IrProjectionItem(String exprText, String varName) {
		this.exprText = exprText;
		this.varName = varName;
	}

	public String getExprText() {
		return exprText;
	}

	public String getVarName() {
		return varName;
	}
}
