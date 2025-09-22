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
import java.util.function.Function;

import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Simple IR→text pretty‑printer using renderer helpers. Responsible only for layout/indentation and delegating term/IRI
 * rendering back to the renderer; it does not perform structural rewrites (those happen in IR transforms).
 */
public final class IRTextPrinter implements IrPrinter {
	private final StringBuilder out;
	private final Function<Var, String> varFormatter;
	private final org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer.Config cfg;
	private int level = 0;
	private boolean inlineActive = false;

	public IRTextPrinter(StringBuilder out, Function<Var, String> varFormatter,
			org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer.Config cfg) {
		this.out = out;
		this.varFormatter = varFormatter;
		this.cfg = cfg;
	}

	/** Print only a WHERE block body. */
	public void printWhere(final IrBGP w) {
		if (w == null) {
			openBlock();
			closeBlock();
			return;
		}
		w.print(this);
	}

	/** Print a sequence of IR lines (helper for containers). */
	public void printLines(final List<IrNode> lines) {
		if (lines == null) {
			return;
		}
		for (IrNode line : lines) {
			line.print(this);
		}
	}

	private void indent() {
		out.append(cfg.indent.repeat(Math.max(0, level)));
	}

	@Override
	public void startLine() {
		if (!inlineActive) {
			indent();
			inlineActive = true;
		}
	}

	@Override
	public void append(final String s) {
		if (!inlineActive) {
			int len = out.length();
			if (len == 0 || out.charAt(len - 1) == '\n') {
				indent();
			}
		}
		out.append(s);
	}

	@Override
	public void endLine() {
		out.append('\n');
		inlineActive = false;
	}

	@Override
	public void line(String s) {
		if (inlineActive) {
			out.append(s).append('\n');
			inlineActive = false;
			return;
		}
		indent();
		out.append(s).append('\n');
	}

	@Override
	public void openBlock() {
		if (!inlineActive) {
			indent();
		}
		out.append('{').append('\n');
		level++;
		inlineActive = false;
	}

	@Override
	public void closeBlock() {
		level--;
		indent();
		out.append('}').append('\n');
	}

	@Override
	public void pushIndent() {
		level++;
	}

	@Override
	public void popIndent() {
		level--;
	}

	@Override
	public String convertVarToString(Var v) {
		return varFormatter.apply(v);
	}
}
