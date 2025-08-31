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
		IrBGP inner = bgp; // rely strictly on pipeline transforms; no print‑time rewrites
		// Special-case: fuse UNION of two bare-NPS path triples into a single NPS when printing a SERVICE body.
		if (inner != null && inner.getLines().size() == 1 && inner.getLines().get(0) instanceof IrUnion) {
			IrUnion u = (IrUnion) inner.getLines().get(0);
			if (u.getBranches().size() == 2) {
				IrPathTriple p1 = unwrapToPathTriple(u.getBranches().get(0));
				IrPathTriple p2 = unwrapToPathTriple(u.getBranches().get(1));
				if (p1 != null && p2 != null) {
					String m1 = normalizeCompactNpsLocal(p1.getPathText());
					String m2 = normalizeCompactNpsLocal(p2.getPathText());
					if (m1 != null && m2 != null) {
						Var sCanon = p1.getSubject();
						Var oCanon = p1.getObject();
						String add2 = m2;
						if (eqVarOrValue(sCanon, p2.getObject()) && eqVarOrValue(oCanon, p2.getSubject())) {
							String inv = invertNegatedPropertySetLocal(m2);
							if (inv != null) {
								add2 = inv;
							}
						} else if (!(eqVarOrValue(sCanon, p2.getSubject()) && eqVarOrValue(oCanon, p2.getObject()))) {
							add2 = null; // cannot align
						}
						if (add2 != null) {
							String merged = mergeMembersLocal(m1, add2);
							p.openBlock();
							String sTxt = p.renderTermWithOverrides(sCanon);
							String oTxt = p.renderTermWithOverrides(oCanon);
							String pathTxt = p.applyOverridesToText(merged);
							p.line(sTxt + " " + pathTxt + " " + oTxt + " .");
							p.closeBlock();
							return;
						}
					}
				}
			}
		}
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
		java.util.List<IrNode> ls = b.getLines();
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
		java.util.List<String> out = new java.util.ArrayList<>(toks.length);
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
