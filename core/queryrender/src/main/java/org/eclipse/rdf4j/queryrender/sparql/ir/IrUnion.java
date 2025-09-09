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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Textual IR node representing a UNION with multiple branches.
 *
 * Notes: - Each branch is an {@link IrBGP} printed as its own braced group. The printer will insert a centered UNION
 * line between groups to match canonical style. - {@code newScope} can be used by transforms as a hint that this UNION
 * represents an explicit user UNION that introduced a new variable scope; some fusions avoid re-association across such
 * boundaries.
 */
public class IrUnion extends IrNode {
	private List<IrBGP> branches = new ArrayList<>();

	public IrUnion(boolean newScope) {
		super(newScope);
	}

	public List<IrBGP> getBranches() {
		return branches;
	}

	public void addBranch(IrBGP w) {
		if (w != null) {
			branches.add(w);
		}
	}

	@Override
	public void print(IrPrinter p) {
		for (int i = 0; i < branches.size(); i++) {
			IrBGP b = branches.get(i);
			if (b != null) {
				IrBGP toPrint = b;
				// Avoid double braces from branch-level new scope: print with newScope=false
				if (toPrint.isNewScope()) {
					toPrint = new IrBGP(toPrint.getLines(), false);
				}
				// Also flatten a redundant single-child inner BGP to prevent nested braces
				if (toPrint.getLines().size() == 1 && toPrint.getLines().get(0) instanceof IrBGP) {
					IrBGP inner = (IrBGP) toPrint.getLines().get(0);
					new IrBGP(inner.getLines(), false).print(p);
				} else {
					toPrint.print(p);
				}
			}
			if (i + 1 < branches.size()) {
				p.line("UNION");
			}
		}
	}

	@Override
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		IrUnion u = new IrUnion(this.isNewScope());
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
				"branches=\n" + sb +
				", newScope=" + isNewScope() +
				'}';
	}

	@Override
	public Set<Var> getVars() {
		HashSet<Var> out = new HashSet<>();
		for (IrBGP b : branches) {
			if (b != null) {
				out.addAll(b.getVars());
			}
		}
		return out;
	}
}
