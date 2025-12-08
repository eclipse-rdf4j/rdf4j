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
 * Structured FILTER body for an EXISTS { ... } block holding a raw BGP.
 */
public class IrExists extends IrNode {
	private final IrBGP where;

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
			where.print(p);
		} else {
			p.openBlock();
			p.closeBlock();
		}
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

	@Override
	public Set<Var> getVars() {
		return where == null ? Collections.emptySet() : where.getVars();
	}
}
