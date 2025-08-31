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
 * Textual IR node for a nested subselect inside WHERE.
 */
public class IrSubSelect extends IrNode {
	private IrSelect select;

	public IrSubSelect(IrSelect select, boolean newScope) {
		super(newScope);
		this.select = select;
	}

	public IrSelect getSelect() {
		return select;
	}

	@Override
	public void print(IrPrinter p) {
		final String text = p.renderSubselect(select);
		// Decide if we need an extra brace layer around the subselect text.
		final boolean hasTrailing = select != null && (!select.getGroupBy().isEmpty()
				|| !select.getHaving().isEmpty() || !select.getOrderBy().isEmpty() || select.getLimit() >= 0
				|| select.getOffset() >= 0);
		final boolean wrap = isNewScope() || hasTrailing;
		if (wrap) {
			p.openBlock();
			for (String ln : text.split("\\R", -1)) {
				p.line(ln);
			}
			p.closeBlock();
		} else {
			// Print the subselect inline without adding an extra brace layer around it.
			for (String ln : text.split("\\R", -1)) {
				p.line(ln);
			}
		}
	}

	@Override
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		return this;
	}
}
