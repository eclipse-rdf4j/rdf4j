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
 * Textual IR node for a FILTER line.
 */
public class IrFilter extends IrNode {
	private final String conditionText;

	public IrFilter(String conditionText) {
		this.conditionText = conditionText;
	}

	public String getConditionText() {
		return conditionText;
	}
}
