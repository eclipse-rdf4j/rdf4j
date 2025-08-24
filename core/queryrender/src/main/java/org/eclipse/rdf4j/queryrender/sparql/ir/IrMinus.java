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
 * Textual IR node for a MINUS { ... } block.
 */
public class IrMinus extends IrNode {
	private IrBGP bgp;

	public IrMinus(IrBGP bgp) {
		this.bgp = bgp;
	}

	public IrBGP getWhere() {
		return bgp;
	}

	public void setWhere(IrBGP bgp) {
		this.bgp = bgp;
	}

	@Override
	public void print(IrPrinter p) {
		IrBGP ow = getWhere();
		if (ow != null && ow.getLines().size() == 1) {
			IrNode only = ow.getLines().get(0);
			if (only instanceof IrPathTriple || only instanceof IrStatementPattern) {
				StringBuilder sb = new StringBuilder();
				sb.append("MINUS { ");
				if (only instanceof IrPathTriple) {
					IrPathTriple pt = (IrPathTriple) only;
					sb.append(p.applyOverridesToText(pt.getSubjectText()))
							.append(' ')
							.append(pt.getPathText())
							.append(' ')
							.append(p.applyOverridesToText(pt.getObjectText()))
							.append(" . ");
				} else {
					IrStatementPattern sp = (IrStatementPattern) only;
					sb.append(p.renderTermWithOverrides(sp.getSubject()))
							.append(' ')
							.append(p.renderPredicateForTriple(sp.getPredicate()))
							.append(' ')
							.append(p.renderTermWithOverrides(sp.getObject()))
							.append(" . ");
				}
				sb.append('}');
				p.line(sb.toString());
				return;
			}
		}
		p.line("MINUS {");
		p.pushIndent();
		p.printLines(ow.getLines());
		p.popIndent();
		p.line("}");
	}

	@Override
	public IrNode transformChildren(java.util.function.UnaryOperator<IrNode> op) {
		IrBGP newWhere = this.bgp;
		if (newWhere != null) {
			IrNode t = op.apply(newWhere);
			if (t instanceof IrBGP) {
				newWhere = (IrBGP) t;
			}
		}
		return new IrMinus(newWhere);
	}
}
