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

import java.util.ArrayList;
import java.util.List;

/**
 * Textual IR node for a VALUES block.
 */
public class IrValues extends IrNode {
	private final List<String> varNames = new ArrayList<>();
	private final List<List<String>> rows = new ArrayList<>();

	public List<String> getVarNames() {
		return varNames;
	}

	public List<List<String>> getRows() {
		return rows;
	}

	@Override
	public void print(IrPrinter p) {
		if (varNames.isEmpty()) {
			p.line("VALUES () {");
			p.pushIndent();
			for (int i = 0; i < rows.size(); i++) {
				p.line("()");
			}
			p.popIndent();
			p.line("}");
			return;
		}
		StringBuilder head = new StringBuilder();
		head.append("VALUES (");
		for (int i = 0; i < varNames.size(); i++) {
			if (i > 0) {
				head.append(' ');
			}
			head.append('?').append(varNames.get(i));
		}
		head.append(") {");
		p.line(head.toString());
		p.pushIndent();
		for (List<String> row : rows) {
			StringBuilder sb = new StringBuilder();
			sb.append('(');
			for (int i = 0; i < row.size(); i++) {
				if (i > 0) {
					sb.append(' ');
				}
				sb.append(row.get(i));
			}
			sb.append(')');
			p.line(sb.toString());
		}
		p.popIndent();
		p.line("}");
	}
}
