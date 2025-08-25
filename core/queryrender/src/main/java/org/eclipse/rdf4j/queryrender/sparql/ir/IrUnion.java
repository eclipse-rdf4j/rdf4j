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
 * Textual IR node representing a UNION with multiple branches.
 */
public class IrUnion extends IrNode {
	private List<IrBGP> branches = new ArrayList<>();
	// True when this UNION originates from an explicit SPARQL UNION that introduces a new variable scope
	private boolean newScope;

	public IrUnion() {
		super();
	}

	public List<IrBGP> getBranches() {
		return branches;
	}

	public void addBranch(IrBGP w) {
		if (w != null) {
			branches.add(w);
		}
	}

	public void setBranches(List<IrBGP> newBranches) {
		this.branches = (newBranches == null) ? new ArrayList<>() : new ArrayList<>(newBranches);
	}

	public boolean isNewScope() {
		return newScope;
	}

	public void setNewScope(boolean newScope) {
		this.newScope = newScope;
	}

	@Override
	public void print(IrPrinter p) {
		for (int i = 0; i < branches.size(); i++) {
			p.line("{");
			p.pushIndent();
			p.printLines(branches.get(i).getLines());
			p.popIndent();
			p.line("}");
			if (i + 1 < branches.size()) {
				p.pushIndent();
				p.line("UNION");
				p.popIndent();
			}
		}
	}

	@Override
	public IrNode transformChildren(java.util.function.UnaryOperator<IrNode> op) {
		IrUnion u = new IrUnion();
		u.setNewScope(this.newScope);
		for (IrBGP b : this.branches) {
			IrNode t = op.apply(b);
			t = t.transformChildren(op);
			u.addBranch(t instanceof IrBGP ? (IrBGP) t : b);
		}
		return u;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (IrBGP branch : branches) {
			sb.append("  ");
			sb.append(branch);
			sb.append("\n");
		}

		return "IrUnion{" +
				"branches=\n" + sb.toString() +
				", newScope=" + newScope +
				'}';
	}
}
