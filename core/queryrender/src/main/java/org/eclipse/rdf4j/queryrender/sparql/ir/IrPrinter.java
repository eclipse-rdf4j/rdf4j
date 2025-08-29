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

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Minimal printing adapter used by IR nodes to render themselves. The implementation is provided by the
 * TupleExprIRRenderer and takes care of indentation, helper rendering, and child printing.
 *
 * Contract and conventions: - {@link #openBlock()} and {@link #closeBlock()} are used by nodes that need to emit a
 * structured block with balanced braces, such as WHERE bodies and subselects. Implementations should ensure
 * braces/indentation are balanced across these calls. - {@link #line(String)} writes a single logical line with current
 * indentation. - Rendering helpers delegate back into the renderer so IR nodes do not duplicate value/IRI formatting
 * logic.
 */
public interface IrPrinter {

	// Basic output controls
	/** Start a new logical line and prepare for inline appends. Applies indentation once. */
	void startLine();

	/** Append text to the current line (starting a new, indented line if none is active). */
	void append(String s);

	/** End the current line (no-op if none is active). */
	void endLine();

	void line(String s);

	void raw(String s);

	void openBlock();

	void closeBlock();

	void pushIndent();

	void popIndent();

	// Child printing helpers
	void printLines(List<IrNode> lines);

	void printWhere(IrBGP bgp);

	// Rendering helpers
	String renderVarOrValue(Var v);

	String renderPredicateForTriple(Var p);

	String renderIRI(IRI iri);

	// Overrides (e.g., for collections)
	String applyOverridesToText(String text);

	String renderTermWithOverrides(Var v);

	// Render a nested subselect as text
	String renderSubselect(IrSelect select);
}
