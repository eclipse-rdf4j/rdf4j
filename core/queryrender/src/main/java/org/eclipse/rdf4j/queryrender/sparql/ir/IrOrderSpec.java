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
 * Textual IR order specification (ORDER BY), including direction.
 */
public class IrOrderSpec {
	private final String exprText;
	private final boolean ascending;

	public IrOrderSpec(String exprText, boolean ascending) {
		this.exprText = exprText;
		this.ascending = ascending;
	}

	public String getExprText() {
		return exprText;
	}

	public boolean isAscending() {
		return ascending;
	}
}
