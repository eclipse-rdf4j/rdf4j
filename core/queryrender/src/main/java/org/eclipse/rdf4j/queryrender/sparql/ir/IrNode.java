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
 * Base class for textual SPARQL Intermediate Representation (IR) nodes.
 */
public abstract class IrNode {

	/** Default no-op printing; concrete nodes override. */
	public void print(IrPrinter p) {
		p.line("# unknown IR node: " + getClass().getSimpleName());
	}
}
