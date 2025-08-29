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

	public void setWhere(IrBGP where) {
		this.where = where;
	}

	@Override
	public void print(IrPrinter p) {
		// Render inline-friendly header then body
		p.append("EXISTS {");
		p.endLine();
		p.pushIndent();
		if (where != null) {
			// Heuristic: if the EXISTS body mixes a triple-like line with a nested EXISTS or VALUES,
			// wrap the body in an inner grouping block to preserve expected brace structure.
			if (shouldGroupInner(where)) {
				p.openBlock();
				p.printLines(where.getLines());
				p.closeBlock();
			} else {
				p.printLines(where.getLines());
			}
		}
		p.popIndent();
		p.line("}");
	}

	private static boolean shouldGroupInner(IrBGP w) {
		if (w == null)
			return false;
		final List<IrNode> ls = w.getLines();
		if (ls.size() < 2)
			return false;
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
		return hasTripleLike && hasNestedExistsOrValues;
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
