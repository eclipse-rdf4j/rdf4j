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

import java.util.Collections;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.eclipse.rdf4j.query.algebra.Var;

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

	@SuppressWarnings("unused")
	public final String _className = this.getClass().getName();

	private boolean newScope;

	public IrNode(boolean newScope) {
		this.newScope = newScope;
	}

	/** Default no-op printing; concrete nodes override. */
	abstract public void print(IrPrinter p);

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

	public boolean isNewScope() {
		return newScope;
	}

	public void setNewScope(boolean newScope) {
		this.newScope = newScope;
	}

	/**
	 * Collect variables referenced by this node and all of its children (if any).
	 *
	 * Default implementation returns an empty set; container and triple-like nodes override to include their own Vars
	 * and recurse into child nodes.
	 */
	public Set<Var> getVars() {
		return Collections.emptySet();
	}

}
