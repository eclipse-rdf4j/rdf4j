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
import java.util.function.UnaryOperator;

import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Textual IR node for a SERVICE block.
 *
 * The reference is kept as already-rendered text to allow either a variable, IRI, or complex expression (as produced by
 * the renderer) and to preserve SILENT when present.
 */
public class IrService extends IrNode {
	private final String serviceRefText;
	private final boolean silent;
	private IrBGP bgp;

	public IrService(String serviceRefText, boolean silent, IrBGP bgp, boolean newScope) {
		super(newScope);
		this.serviceRefText = serviceRefText;
		this.silent = silent;
		this.bgp = bgp;
	}

	public String getServiceRefText() {
		return serviceRefText;
	}

	public boolean isSilent() {
		return silent;
	}

	public IrBGP getWhere() {
		return bgp;
	}

	@Override
	public void print(IrPrinter p) {
		p.startLine();
		p.append("SERVICE ");
		if (silent) {
			p.append("SILENT ");
		}
		p.append(serviceRefText);
		p.append(" ");
		IrBGP inner = bgp;
		// Rely solely on the transform pipeline for structural rewrites. Printing preserves
		// whatever grouping/GRAPH context the IR carries at this point.
		if (inner != null) {
			inner.print(p); // IrBGP prints braces
		} else {
			p.openBlock();
			p.closeBlock();
		}
	}

	private static IrPathTriple unwrapToPathTriple(IrBGP b) {
		if (b == null)
			return null;
		IrNode node = singleChild(b);
		while (node instanceof IrBGP) {
			IrNode inner = singleChild((IrBGP) node);
			if (inner == null)
				break;
			node = inner;
		}
		if (node instanceof IrGraph) {
			IrGraph g = (IrGraph) node;
			node = singleChild(g.getWhere());
			while (node instanceof IrBGP) {
				IrNode inner = singleChild((IrBGP) node);
				if (inner == null)
					break;
				node = inner;
			}
		}
		return (node instanceof IrPathTriple) ? (IrPathTriple) node : null;
	}

	private static IrNode singleChild(IrBGP b) {
		if (b == null)
			return null;
		List<IrNode> ls = b.getLines();
		if (ls == null || ls.size() != 1)
			return null;
		return ls.get(0);
	}

	private static boolean eqVarOrValue(Var a, Var b) {
		if (a == b)
			return true;
		if (a == null || b == null)
			return false;
		if (a.hasValue() && b.hasValue())
			return a.getValue().equals(b.getValue());
		if (!a.hasValue() && !b.hasValue()) {
			String an = a.getName();
			String bn = b.getName();
			return an != null && an.equals(bn);
		}
		return false;
	}

	private static String normalizeCompactNpsLocal(String path) {
		if (path == null)
			return null;
		String t = path.trim();
		if (t.isEmpty())
			return null;
		if (t.startsWith("!(") && t.endsWith(")"))
			return t;
		if (t.startsWith("!^"))
			return "!(" + t.substring(1) + ")";
		if (t.startsWith("!") && (t.length() == 1 || t.charAt(1) != '('))
			return "!(" + t.substring(1) + ")";
		return null;
	}

	private static String invertNegatedPropertySetLocal(String nps) {
		if (nps == null)
			return null;
		String s = nps.trim();
		if (!s.startsWith("!(") || !s.endsWith(")"))
			return null;
		String inner = s.substring(2, s.length() - 1);
		if (inner.isEmpty())
			return s;
		String[] toks = inner.split("\\|");
		List<String> out = new ArrayList<>(toks.length);
		for (String tok : toks) {
			String t = tok.trim();
			if (t.isEmpty())
				continue;
			if (t.startsWith("^")) {
				out.add(t.substring(1));
			} else {
				out.add("^" + t);
			}
		}
		if (out.isEmpty())
			return s;
		return "!(" + String.join("|", out) + ")";
	}

	private static String mergeMembersLocal(String a, String b) {
		int a1 = a.indexOf('('), a2 = a.lastIndexOf(')');
		int b1 = b.indexOf('('), b2 = b.lastIndexOf(')');
		if (a1 < 0 || a2 < 0 || b1 < 0 || b2 < 0)
			return a;
		String ia = a.substring(a1 + 1, a2).trim();
		String ib = b.substring(b1 + 1, b2).trim();
		if (ia.isEmpty())
			return b;
		if (ib.isEmpty())
			return a;
		return "!(" + ia + "|" + ib + ")";
	}

	@Override
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		IrBGP newWhere = this.bgp;
		if (newWhere != null) {
			IrNode t = op.apply(newWhere);
			t = t.transformChildren(op);

			if (t instanceof IrBGP) {
				newWhere = (IrBGP) t;
			}
		}
		return new IrService(this.serviceRefText, this.silent, newWhere, this.isNewScope());
	}
}
