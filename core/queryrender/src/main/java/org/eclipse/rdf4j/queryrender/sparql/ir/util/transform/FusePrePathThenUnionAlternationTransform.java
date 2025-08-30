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
package org.eclipse.rdf4j.queryrender.sparql.ir.util.transform;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Fuse pattern: [PathTriple pre] followed by a UNION with two branches that each represent a tail path from pre.object
 * to a common end variable. Produces a single PathTriple with pre.pathText/(altTail), enabling subsequent tail join
 * with a following constant triple.
 */
public final class FusePrePathThenUnionAlternationTransform extends BaseTransform {
	static final class Tail {
		final Var end;
		final String path;

		Tail(Var end, String path) {
			this.end = end;
			this.path = path;
		}
	}

	private FusePrePathThenUnionAlternationTransform() {
	}

	public static IrBGP apply(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null)
			return null;
		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			// Recurse early
			n = n.transformChildren(child -> {
				if (child instanceof IrBGP)
					return apply((IrBGP) child, r);
				return child;
			});

			if (n instanceof IrPathTriple && i + 1 < in.size() && in.get(i + 1) instanceof IrUnion) {
				IrPathTriple pre = (IrPathTriple) n;
				Var mid = pre.getObject();
				if (!isAnonPathVar(mid)) {
					out.add(n);
					continue;
				}
				IrUnion u = (IrUnion) in.get(i + 1);
				// Allow fusing across a new-scope UNION only when both branches clearly use
				// parser-generated anon-path bridge variables. Otherwise, preserve the scope.
				if ((u.isNewScope() && !unionBranchesAllHaveAnonPathBridge(u)) || u.getBranches().size() != 2) {
					out.add(n);
					continue;
				}
				Tail t0 = parseTail(u.getBranches().get(0), mid, r);
				Tail t1 = parseTail(u.getBranches().get(1), mid, r);
				if (t0 != null && t1 != null && sameVar(t0.end, t1.end)) {
					String alt = (t0.path.equals(t1.path)) ? t0.path : ("(" + t0.path + "|" + t1.path + ")");
					String preTxt = normalizePrePrefix(pre.getPathText());
					String fused = preTxt + "/" + alt;
					Var endVar = t0.end;
					// Try to also consume an immediate tail triple (e.g., foaf:name) so that it appears outside the
					// alternation parentheses
					if (i + 2 < in.size() && in.get(i + 2) instanceof IrStatementPattern) {
						IrStatementPattern tail = (IrStatementPattern) in.get(i + 2);
						if (tail.getPredicate() != null && tail.getPredicate().hasValue()
								&& FOAF.NAME.equals(tail.getPredicate().getValue())
								&& sameVar(endVar, tail.getSubject())) {
							// Append tail step directly
							fused = fused + "/" + r.renderIRI(FOAF.NAME);
							endVar = tail.getObject();
							out.add(new IrPathTriple(pre.getSubject(), fused, endVar));
							i += 2; // consume union and tail
							continue;
						}
					}
					out.add(new IrPathTriple(pre.getSubject(), fused, endVar));
					i += 1; // consume union
					continue;
				}
			}

			// Recurse into containers not already handled
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), apply(g.getWhere(), r)));
				continue;
			}
			if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere(), r));
				no.setNewScope(o.isNewScope());
				out.add(no);
				continue;
			}
			if (n instanceof IrMinus) {
				IrMinus m = (IrMinus) n;
				out.add(new IrMinus(apply(m.getWhere(), r)));
				continue;
			}
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion();
				u2.setNewScope(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(apply(b, r));
				}
				out.add(u2);
				continue;
			}
			if (n instanceof IrService) {
				IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere(), r)));
				continue;
			}
			if (n instanceof IrSubSelect) {
				out.add(n);
				continue;
			}
			out.add(n);
		}
		IrBGP res = new IrBGP();
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	private static Tail parseTail(IrBGP b, Var mid, TupleExprIRRenderer r) {
		if (b == null)
			return null;
		if (b.getLines().size() == 1) {
			IrNode only = b.getLines().get(0);
			if (only instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) only;
				if (sameVar(mid, pt.getSubject())) {
					return new Tail(pt.getObject(), pt.getPathText());
				}
				if (sameVar(mid, pt.getObject())) {
					return new Tail(pt.getSubject(), "^(" + pt.getPathText() + ")");
				}
			} else if (only instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) only;
				if (sp.getPredicate() != null && sp.getPredicate().hasValue()
						&& sp.getPredicate().getValue() instanceof IRI) {
					String step = r.renderIRI((IRI) sp.getPredicate().getValue());
					if (sameVar(mid, sp.getSubject())) {
						return new Tail(sp.getObject(), step);
					}
					if (sameVar(mid, sp.getObject())) {
						return new Tail(sp.getSubject(), "^" + step);
					}
				}
			}
		}
		if (b.getLines().size() == 2 && b.getLines().get(0) instanceof IrStatementPattern
				&& b.getLines().get(1) instanceof IrStatementPattern) {
			IrStatementPattern a = (IrStatementPattern) b.getLines().get(0);
			IrStatementPattern c = (IrStatementPattern) b.getLines().get(1);
			if (a.getPredicate() == null || !a.getPredicate().hasValue()
					|| !(a.getPredicate().getValue() instanceof IRI))
				return null;
			if (c.getPredicate() == null || !c.getPredicate().hasValue()
					|| !(c.getPredicate().getValue() instanceof IRI))
				return null;
			if (sameVar(mid, a.getSubject()) && sameVar(a.getObject(), c.getSubject())) {
				// forward-forward
				String step1 = r.renderIRI((IRI) a.getPredicate().getValue());
				String step2 = r.renderIRI((IRI) c.getPredicate().getValue());
				return new Tail(c.getObject(), step1 + "/" + step2);
			}
			if (sameVar(mid, a.getObject()) && sameVar(a.getSubject(), c.getObject())) {
				// inverse-inverse
				String step1 = "^" + r.renderIRI((IRI) a.getPredicate().getValue());
				String step2 = "^" + r.renderIRI((IRI) c.getPredicate().getValue());
				return new Tail(c.getSubject(), step1 + "/" + step2);
			}
		}
		return null;
	}

	// Normalize a common pre-path shape: ((!(A)))/(((B))?) → (!(A)/(B)?)
	static String normalizePrePrefix(String s) {
		if (s == null)
			return null;
		String t = s.trim();
		if (!t.startsWith("((")) {
			return t;
		}
		int sep = t.indexOf(")/(");
		if (sep <= 0) {
			return t;
		}
		String left = t.substring(2, sep); // content inside the leading "(("
		String rightWithParens = t.substring(sep + 2);
		// If right side is double-parenthesized with an optional quantifier, collapse one layer:
		// "((X))?" -> "(X)?" and "((X))" -> "(X)".
		if (rightWithParens.length() >= 2 && rightWithParens.charAt(0) == '(') {
			// Case: ends with ")?" and also has an extra ")" before the '?'
			if (rightWithParens.endsWith(")?") && rightWithParens.length() >= 3
					&& rightWithParens.charAt(rightWithParens.length() - 3) == ')') {
				String inner = rightWithParens.substring(1, rightWithParens.length() - 3);
				rightWithParens = "(" + inner + ")?";
			} else if (rightWithParens.charAt(rightWithParens.length() - 1) == ')') {
				// Collapse a single outer pair of parentheses
				String inner = rightWithParens.substring(1, rightWithParens.length() - 1);
				rightWithParens = "(" + inner + ")";
			}
		}
		return "((" + left + ")/" + rightWithParens;
	}
}
