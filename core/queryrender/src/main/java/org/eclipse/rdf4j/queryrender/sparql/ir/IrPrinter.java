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
 */
public interface IrPrinter {

	// Basic output controls
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
	String renderSubselect(org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect select);
}
