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
 * Textual IR node for an OPTIONAL block.
 */
public class IrOptional extends IrNode {
	private final IrWhere where;

	public IrOptional(IrWhere where) {
		this.where = where;
	}

	public IrWhere getWhere() {
		return where;
	}

	@Override
	public void print(IrPrinter p) {
		IrWhere ow = getWhere();
		if (ow != null && ow.getLines().size() == 1) {
			IrNode only = ow.getLines().get(0);
			if (only instanceof IrPathTriple || only instanceof IrStatementPattern) {
				StringBuilder sb = new StringBuilder();
				sb.append("OPTIONAL { ");
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
		p.raw("OPTIONAL ");
		p.openBlock();
		p.printLines(ow.getLines());
		p.closeBlock();
	}
}
