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

/**
 * Textual IR node for a nested subselect inside WHERE.
 */
public class IrSubSelect extends IrNode {
	private IrSelect select;

	public IrSubSelect(IrSelect select) {
		this.select = select;
	}

	public IrSelect getSelect() {
		return select;
	}

	public void setSelect(IrSelect select) {
		this.select = select;
	}

	@Override
	public void print(IrPrinter p) {
		final String text = p.renderSubselect(select);
		p.line("{");
		p.pushIndent();
		for (String ln : text.split("\\R", -1)) {
			p.line(ln);
		}
		p.popIndent();
		p.line("}");
	}

	@Override
	public IrNode transformChildren(java.util.function.UnaryOperator<IrNode> op) {
		IrSelect newSel = this.select;
		if (newSel != null) {
			IrNode t = op.apply(newSel);
			if (t instanceof IrSelect) {
				newSel = (IrSelect) t;
			} else if (newSel.getWhere() != null) {
				IrNode tw = op.apply(newSel.getWhere());
				if (tw instanceof IrBGP) {
					IrSelect copy = new IrSelect();
					copy.setDistinct(newSel.isDistinct());
					copy.setReduced(newSel.isReduced());
					copy.setWhere((IrBGP) tw);
					copy.getProjection().addAll(newSel.getProjection());
					copy.getGroupBy().addAll(newSel.getGroupBy());
					copy.getHaving().addAll(newSel.getHaving());
					copy.getOrderBy().addAll(newSel.getOrderBy());
					copy.setLimit(newSel.getLimit());
					copy.setOffset(newSel.getOffset());
					newSel = copy;
				}
			}
		}
		return new IrSubSelect(newSel);
	}
}
