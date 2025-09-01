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
import java.util.function.UnaryOperator;

/**
 * Structured FILTER body for an EXISTS { ... } block holding a raw BGP.
 */
public class IrExists extends IrNode {
	private IrBGP where;

	public IrExists(IrBGP where, boolean newScope) {
		super(newScope);
		this.where = where;
	}

	public IrBGP getWhere() {
		return where;
	}

	@Override
	public void print(IrPrinter p) {
		// EXISTS keyword, then delegate braces to inner IrBGP. Do not start a new line here so
		// that callers (e.g., IrFilter) can render "... . FILTER EXISTS {" on a single line.
		p.append("EXISTS ");
		if (where != null) {
			IrBGP content = where;
			// If the EXISTS expression itself was marked as a variable-scope change
			// (e.g., original query used an extra group: EXISTS { { GRAPH ... } }),
			// ensure we preserve that explicit grouping even if later transforms
			// rewrote the inner body and dropped the BGP.newScope flag.
			if (this.isNewScope() && !content.isNewScope()) {
				// Only synthesize an outer grouping when the EXISTS body is a single GRAPH block.
				// This matches cases where the original query wrote EXISTS { { GRAPH ... { ... } } }
				// and avoids over-grouping more complex bodies (which can change algebraic scope markers).
				boolean singleGraph = content.getLines().size() == 1 && content.getLines().get(0) instanceof IrGraph;
				if (singleGraph) {
					IrBGP wrap = new IrBGP(true);
					wrap.add(content);
					content = wrap;
				}
			}
			toPrint(content).print(p);
		} else {
			p.openBlock();
			p.closeBlock();
		}
	}

	private static IrBGP toPrint(IrBGP w) {
		if (w == null)
			return null;
		// Preserve inner grouping when the body mixes a triple-like with nested EXISTS/VALUES
		final List<IrNode> ls = w.getLines();
		boolean hasTripleLike = false;
		boolean hasNestedExistsOrValues = false;
		for (IrNode ln : ls) {
			if (ln instanceof IrStatementPattern || ln instanceof IrPathTriple || ln instanceof IrPropertyList) {
				hasTripleLike = true;
			} else if (ln instanceof IrFilter) {
				IrFilter f = (IrFilter) ln;
				if (f.getBody() instanceof IrExists)
					hasNestedExistsOrValues = true;
			} else if (ln instanceof IrValues) {
				hasNestedExistsOrValues = true;
			}
		}
		if (ls.size() >= 2 && hasTripleLike && hasNestedExistsOrValues) {
			IrBGP wrap = new IrBGP(false);
			wrap.add(w);
			return wrap;
		}
		return w;
	}

	@Override
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		IrBGP newWhere = this.where;
		if (newWhere != null) {
			IrNode t = op.apply(newWhere);
			t = t.transformChildren(op);
			if (t instanceof IrBGP) {
				newWhere = (IrBGP) t;
			}
		}
		return new IrExists(newWhere, this.isNewScope());
	}
}
