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
 * Base class for textual SPARQL Intermediate Representation (IR) nodes.
 *
 * Design goals: - Keep IR nodes small and predictable; they are close to the final SPARQL surface form and
 * intentionally avoid carrying evaluation semantics. - Favour immutability from the perspective of transforms:
 * implementors should not mutate existing instances inside transforms but instead build new nodes as needed. - Provide
 * a single {@link #print(IrPrinter)} entry point so pretty-printing concerns are centralized in the {@link IrPrinter}
 * implementation.
 */
public abstract class IrNode {

	/** Default no-op printing; concrete nodes override. */
	public void print(IrPrinter p) {
		p.line("# unknown IR node: " + getClass().getSimpleName());
	}

	/**
	 * Function-style child transformation hook used by the transform pipeline to descend into nested structures.
	 *
	 * Contract: - Leaf nodes return {@code this} unchanged. - Container nodes return a new instance with their
	 * immediate children transformed using the provided operator. - Implementations must not mutate {@code this} or its
	 * existing children.
	 */
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		return this;
	}

}
