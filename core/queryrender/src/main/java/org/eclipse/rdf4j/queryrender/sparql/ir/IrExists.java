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
		// EXISTS keyword, then delegate braces to inner IrBGP
		p.startLine();
		p.append("EXISTS ");
		if (where != null) {
			toPrint(where).print(p);
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
			IrBGP wrap = new IrBGP();
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
