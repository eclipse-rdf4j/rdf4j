/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.queryrender.sparql.ir.util.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
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
import org.eclipse.rdf4j.queryrender.sparql.ir.IrTripleLike;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Shared helpers and small utilities for IR transform passes.
 *
 * Conventions and invariants: - Transforms are functional: they do not mutate input nodes; instead they build new IR
 * blocks as needed. - Path/chain fusions are conservative and only cross intermediate variables that the parser created
 * for property paths (variable names prefixed with {@code _anon_path_}). This prevents accidental elimination or
 * inversion of user-defined variables. - Text helpers respect property path precedence and add parentheses only when
 * required for correctness. - Container nodes (GRAPH/OPTIONAL/MINUS/UNION/SERVICE) are preserved, and recursion uses
 * {@code transformChildren} to keep transform code small and predictable.
 */
public class BaseTransform {
	/*
	 * =============================== ===== Union Merge Policy ====== ===============================
	 *
	 * Several transforms can merge a UNION of two branches into a single path expression (an alternation) or a single
	 * negated property set (NPS). This is valuable for readability and streaming-friendly output, but it must be done
	 * conservatively to never change query semantics nor collapse user-visible variables.
	 *
	 * Parser-provided hints: the RDF4J parser introduces anonymous bridge variables when decoding property paths. These
	 * variables use a reserved prefix: - _anon_path_* (forward-oriented bridge) - _anon_path_inverse_*
	 * (inverse-oriented bridge)
	 *
	 * We use these names as a safety signal that fusing across the bridge does not remove a user variable.
	 *
	 * High-level rules applied by union-fusing transforms: 1) No new scope (i.e., the UNION node is not marked as
	 * introducing a new scope): - The UNION may be merged only if EACH branch contains at least one anonymous path
	 * bridge variable (either prefix). See unionBranchesAllHaveAnonPathBridge().
	 *
	 * 2) New scope (i.e., the UNION node carries explicit variable-scope change): - By default, do NOT merge such a
	 * UNION. - Special exception: if both branches share at least one COMMON variable name that starts with the
	 * _anon_path_ prefix (either orientation), the UNION may still be merged. This indicates the new-scope originated
	 * from path decoding and is safe to compact. See unionBranchesShareCommonAnonPathVarName().
	 *
	 * Additional per-transform constraints remain in place (e.g., fusing only bare NPS, or simple single-step triples,
	 * identical endpoints, identical GRAPH reference), and transforms preserve explicit grouping braces when the input
	 * UNION marked a new scope (by wrapping the fused result in a grouped IrBGP as needed).
	 */

	// Local copy of parser's _anon_path_ naming hint for safe path fusions
	public static final String ANON_PATH_PREFIX = "_anon_path_";
	// Additional hint used by the parser for inverse-oriented anonymous path variables.
	public static final String ANON_PATH_INVERSE_PREFIX = "_anon_path_inverse_";

	// --------------- Path text helpers: add parens only when needed ---------------

	/**
	 * Normalize compact negated-property-set forms into the canonical parenthesized variant. Examples: "!ex:p" ->
	 * "!(ex:p)", "!^ex:p" -> "!(^ex:p)". Leaves already-canonical and non-NPS text unchanged.
	 */
	public static String normalizeCompactNps(String path) {
		if (path == null) {
			return null;
		}
		String t = path.trim();
		if (t.isEmpty()) {
			return t;
		}
		if (t.startsWith("!(") && t.endsWith(")")) {
			return t;
		}
		if (t.startsWith("!^")) {
			return "!(" + t.substring(1) + ")"; // !^ex:p -> !(^ex:p)
		}
		if (t.startsWith("!") && (t.length() == 1 || t.charAt(1) != '(')) {
			return "!(" + t.substring(1) + ")"; // !ex:p -> !(ex:p)
		}
		return t;
	}

	/** Merge NPS members of two canonical strings '!(...)', returning '!(a|b)'. Falls back to 'a' when malformed. */
	public static String mergeNpsMembers(String a, String b) {
		if (a == null || b == null) {
			return a;
		}
		int a1 = a.indexOf('('), a2 = a.lastIndexOf(')');
		int b1 = b.indexOf('('), b2 = b.lastIndexOf(')');
		if (a1 < 0 || a2 < 0 || b1 < 0 || b2 < 0) {
			return a;
		}
		String ia = a.substring(a1 + 1, a2).trim();
		String ib = b.substring(b1 + 1, b2).trim();
		if (ia.isEmpty()) {
			return b;
		}
		if (ib.isEmpty()) {
			return a;
		}
		return "!(" + ia + "|" + ib + ")";
	}

	/**
	 * Universal safeguard for explicit user UNIONs: true iff the UNION is marked as new scope and all its branches are
	 * also marked as new scope. Such a UNION should never be fused into a single path expression.
	 */
	public static boolean unionIsExplicitAndAllBranchesScoped(final IrUnion u) {
		if (u == null || !u.isNewScope()) {
			return false;
		}
		if (u.getBranches() == null || u.getBranches().isEmpty()) {
			return false;
		}

		for (IrBGP b : u.getBranches()) {
			if (!b.isNewScope()) {
				if (b.getLines().size() != 1 || !b.getLines().get(0).isNewScope()) {
					return false;
				}

			}
		}
		return true;
	}

	/** Return true if the string has the given character at top level (not inside parentheses). */
	public static boolean hasTopLevel(final String s, final char ch) {
		if (s == null) {
			return false;
		}
		final String t = s.trim();
		int depth = 0;
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
			} else if (c == ch && depth == 0) {
				return true;
			}
		}
		return false;
	}

	/** True if the text is wrapped by a single pair of outer parentheses. */
	public static boolean isWrapped(final String s) {
		if (s == null) {
			return false;
		}
		final String t = s.trim();
		if (t.length() < 2 || t.charAt(0) != '(' || t.charAt(t.length() - 1) != ')') {
			return false;
		}
		int depth = 0;
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
			}
			if (depth == 0 && i < t.length() - 1) {
				return false; // closes too early
			}
		}
		return true;
	}

	/** Rough atomic check for a property path text: no top-level '|' or '/', NPS, or already wrapped. */
	public static boolean isAtomicPathText(final String s) {
		if (s == null) {
			return true;
		}
		final String t = s.trim();
		if (t.isEmpty()) {
			return true;
		}
		if (isWrapped(t)) {
			return true;
		}
		if (t.startsWith("!(")) {
			return true; // negated property set is atomic
		}
		if (t.startsWith("^")) {
			final String rest = t.substring(1).trim();
			// ^IRI or ^( ... )
			return rest.startsWith("(") || (!hasTopLevel(rest, '|') && !hasTopLevel(rest, '/'));
		}
		return !hasTopLevel(t, '|') && !hasTopLevel(t, '/');
	}

	/**
	 * When using a part inside a sequence with '/', only wrap it if it contains a top-level alternation '|'.
	 */
	public static String wrapForSequence(final String part) {
		if (part == null) {
			return null;
		}
		final String t = part.trim();
		if (isWrapped(t) || !hasTopLevel(t, '|')) {
			return t;
		}
		return "(" + t + ")";
	}

	/** Prefix with '^', wrapping if the inner is not atomic. */
	public static String wrapForInverse(final String inner) {
		if (inner == null) {
			return "^()";
		}
		final String t = inner.trim();
		return "^" + (isAtomicPathText(t) ? t : ("(" + t + ")"));
	}

	/** Apply a quantifier to a path, wrapping only when the inner is not atomic. */
	public static String applyQuantifier(final String inner, final char quant) {
		if (inner == null) {
			return "()" + quant;
		}
		final String t = inner.trim();
		return (isAtomicPathText(t) ? t : ("(" + t + ")")) + quant;
	}

	public static void copyAllExcept(IrBGP from, IrBGP to, IrNode except) {
		if (from == null) {
			return;
		}
		for (IrNode ln : from.getLines()) {
			if (ln == except) {
				continue;
			}
			to.add(ln);
		}
	}

	/** Fuse adjacent IrPathTriple nodes when the first's object equals the second's subject. */
	public static IrBGP fuseAdjacentPtThenPt(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = bgp.getLines();
		List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (n instanceof IrPathTriple && i + 1 < in.size() && in.get(i + 1) instanceof IrPathTriple) {
				IrPathTriple a = (IrPathTriple) n;
				IrPathTriple b = (IrPathTriple) in.get(i + 1);
				Var bridge = a.getObject();
				if (sameVar(bridge, b.getSubject()) && isAnonPathVar(bridge)) {
					// Merge a and b: s -(a.path/b.path)-> o. Keep explicit grouping to enable later canonicalization.
					String fusedPath = "(" + a.getPathText() + ")/(" + b.getPathText() + ")";
					out.add(new IrPathTriple(a.getSubject(), a.getSubjectOverride(), fusedPath, b.getObject(),
							b.getObjectOverride(), IrPathTriple.mergePathVars(a, b), false));
					i += 1; // consume b
				} else if (sameVar(bridge, b.getObject()) && isAnonPathVar(bridge)) {
					// Merge a and b with inverse join on b. Keep explicit grouping.
					String fusedPath = "(" + a.getPathText() + ")/^(" + b.getPathText() + ")";
					out.add(new IrPathTriple(a.getSubject(), a.getSubjectOverride(), fusedPath, b.getSubject(),
							b.getSubjectOverride(), IrPathTriple.mergePathVars(a, b), false));
					i += 1; // consume b
				} else {
					// Additional cases: the bridge variable occurs as the subject of the first path triple.
					Var aSubj = a.getSubject();
					if (isAnonPathVar(aSubj)) {
						// Avoid inverting NPS members: if 'a' is a bare negated property set, do not
						// attempt subject-shared composition which requires inverting 'a'. Leave to other
						// fusers that do not alter the NPS text.
						String aPath = a.getPathText();
						boolean aIsNps = aPath != null && aPath.trim().startsWith("!(");
						if (aIsNps) {
							out.add(n);
							continue;
						}
						// Case: a.subject == b.subject -> compose by inverting 'a' and chaining forward with 'b'
						if (sameVar(aSubj, b.getSubject())) {
							String left = invertNegatedPropertySet(aPath);
							if (left == null) {
								left = wrapForInverse(aPath);
							}
							String fusedPath = left + "/" + wrapForSequence(b.getPathText());
							out.add(new IrPathTriple(a.getObject(), a.getObjectOverride(), fusedPath, b.getObject(),
									b.getObjectOverride(), IrPathTriple.mergePathVars(a, b), false));
							i += 1; // consume b
							continue;
						}

						// Case: a.subject == b.object -> compose by inverting both 'a' and 'b'
						if (sameVar(aSubj, b.getObject())) {
							String left = invertNegatedPropertySet(aPath);
							if (left == null) {
								left = wrapForInverse(aPath);
							}
							String right = wrapForInverse(b.getPathText());
							String fusedPath = left + "/" + right;
							out.add(new IrPathTriple(a.getObject(), a.getObjectOverride(), fusedPath, b.getSubject(),
									b.getSubjectOverride(), IrPathTriple.mergePathVars(a, b), false));
							i += 1; // consume b
							continue;
						}
					}
					out.add(n);
				}
			} else {
				out.add(n);
			}
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	/**
	 * Fuse a three-line sequence: IrPathTriple (A), IrStatementPattern (B), IrPathTriple (C) into A then ( ^B.p / C ).
	 *
	 * Pattern constraints: - A.object equals B.object (inverse join candidate) and A.object is an _anon_path_* var. -
	 * B.subject equals C.subject and both B.subject and B.object are _anon_path_* vars.
	 */
	public static IrBGP fusePtSpPtSequence(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = bgp.getLines();
		List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode a = in.get(i);
			if (a instanceof IrPathTriple && i + 2 < in.size() && in.get(i + 1) instanceof IrStatementPattern
					&& in.get(i + 2) instanceof IrPathTriple) {
				IrPathTriple ptA = (IrPathTriple) a;
				IrStatementPattern spB = (IrStatementPattern) in.get(i + 1);
				IrPathTriple ptC = (IrPathTriple) in.get(i + 2);
				Var bPred = spB.getPredicate();
				if (bPred != null && bPred.hasValue() && bPred.getValue() instanceof IRI) {
					if (sameVar(ptA.getObject(), spB.getObject()) && isAnonPathVar(ptA.getObject())
							&& sameVar(spB.getSubject(), ptC.getSubject()) && isAnonPathVar(spB.getSubject())
							&& isAnonPathVar(spB.getObject())) {
						String fusedPath = "^" + r.convertIRIToString((IRI) bPred.getValue()) + "/" + ptC.getPathText();
						IrPathTriple d = new IrPathTriple(spB.getObject(), spB.getObjectOverride(), fusedPath,
								ptC.getObject(), ptC.getObjectOverride(), IrPathTriple.mergePathVars(ptC), false);
						// Keep A; then D replaces B and C
						out.add(ptA);
						out.add(d);
						i += 2; // consume B and C
						continue;
					}
				}
			}
			out.add(a);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	/**
	 * Re-orient a bare negated property set path "!(...)" so that its object matches the subject of the immediately
	 * following triple when possible, enabling chaining: prefer s !(...) ?x when the next line starts with ?x ...
	 */
	public static IrBGP orientBareNpsForNext(IrBGP bgp) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = bgp.getLines();
		List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				String ptxt = pt.getPathText();
				if (ptxt != null) {
					String s = ptxt.trim();
					if (s.startsWith("!(") && s.endsWith(")")) {
						// Do not re-orient bare NPS here. Flipping NPS to chain with the following
						// triple inverts individual members (ex:g <-> ^ex:g), which breaks
						// idempotence on round-trips. Other fusion passes can still chain without
						// altering the NPS semantics.
					}
				}
				out.add(pt);
				continue;
			}
			// Recurse
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), orientBareNpsForNext(g.getWhere()), g.isNewScope()));
				continue;
			}
			if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(orientBareNpsForNext(o.getWhere()), o.isNewScope());
				no.setNewScope(o.isNewScope());
				out.add(no);
				continue;
			}
			if (n instanceof IrMinus) {
				IrMinus m = (IrMinus) n;
				out.add(new IrMinus(orientBareNpsForNext(m.getWhere()), m.isNewScope()));
				continue;
			}
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(orientBareNpsForNext(b));
				}
				out.add(u2);
				continue;
			}
			if (n instanceof IrService) {
				IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), orientBareNpsForNext(s.getWhere()),
						s.isNewScope()));
				continue;
			}
			out.add(n);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	public static IrBGP fuseAdjacentSpThenPt(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = bgp.getLines();
		List<IrNode> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (i + 1 < in.size() && n instanceof IrStatementPattern && in.get(i + 1) instanceof IrPathTriple) {
				IrStatementPattern sp = (IrStatementPattern) n;
				Var p = sp.getPredicate();
				if (p != null && p.hasValue() && p.getValue() instanceof IRI) {
					IrPathTriple pt = (IrPathTriple) in.get(i + 1);
					if (sameVar(sp.getObject(), pt.getSubject()) && isAnonPathVar(pt.getSubject())) {
						String fused = r.convertIRIToString((IRI) p.getValue()) + "/" + pt.getPathText();
						out.add(new IrPathTriple(sp.getSubject(), sp.getSubjectOverride(), fused, pt.getObject(),
								pt.getObjectOverride(), IrPathTriple.mergePathVars(pt), false));
						i += 1;
						continue;
					} else if (sameVar(sp.getSubject(), pt.getObject()) && isAnonPathVar(pt.getObject())) {
						String fused = pt.getPathText() + "/^" + r.convertIRIToString((IRI) p.getValue());
						out.add(new IrPathTriple(pt.getSubject(), pt.getSubjectOverride(), fused, sp.getObject(),
								sp.getObjectOverride(), IrPathTriple.mergePathVars(pt), false));
						i += 1;
						continue;
					}
				}
			}
			out.add(n);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	public static IrBGP joinPathWithLaterSp(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}
		List<IrNode> in = new ArrayList<>(bgp.getLines());
		List<IrNode> out = new ArrayList<>();
		Set<IrNode> removed = new HashSet<>();
		for (int i = 0; i < in.size(); i++) {
			IrNode n = in.get(i);
			if (removed.contains(n)) {
				continue;
			}
			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				Var objVar = pt.getObject();
				if (isAnonPathVar(objVar)) {
					IrStatementPattern join = null;
					boolean inverse = false;
					for (int j = i + 1; j < in.size(); j++) {
						IrNode m = in.get(j);
						if (!(m instanceof IrStatementPattern)) {
							continue;
						}
						IrStatementPattern sp = (IrStatementPattern) m;
						Var pv = sp.getPredicate();
						if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
							continue;
						}
						// If this SP is immediately followed by a PathTriple that shares SP.subject as its subject,
						// prefer the later SP+PT fusion instead of attaching the SP here. This preserves canonical
						// grouping like ...*/(^ex:d/(...)).
						if (j + 1 < in.size() && in.get(j + 1) instanceof IrPathTriple) {
							IrPathTriple nextPt = (IrPathTriple) in.get(j + 1);
							if (sameVar(sp.getSubject(), nextPt.getSubject())
									|| sameVar(sp.getObject(), nextPt.getSubject())) {
								continue; // skip this SP; allow SP+PT rule to handle
							}
						}
						if (sameVar(objVar, sp.getSubject()) && isAnonPathVar(sp.getObject())) {
							join = sp;
							inverse = false;
							break;
						}
						if (sameVar(objVar, sp.getObject()) && isAnonPathVar(sp.getSubject())) {
							join = sp;
							inverse = true;
							break;
						}
					}
					if (join != null) {
						String step = r.convertIRIToString((IRI) join.getPredicate().getValue());
						String newPath = pt.getPathText() + "/" + (inverse ? "^" : "") + step;
						Var newEnd = inverse ? join.getSubject() : join.getObject();
						IrNode newEndOverride = inverse ? join.getSubjectOverride() : join.getObjectOverride();
						pt = new IrPathTriple(pt.getSubject(), pt.getSubjectOverride(), newPath, newEnd, newEndOverride,
								pt.getPathVars(), pt.isNewScope());
						removed.add(join);
					}
				}
				out.add(pt);
				continue;
			}
			// Recurse within nested BGPs
			if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				IrBGP inner = g.getWhere();
				inner = joinPathWithLaterSp(inner, r);
				inner = fuseAltInverseTailBGP(inner, r);
				out.add(new IrGraph(g.getGraph(), inner, g.isNewScope()));
				continue;
			}
			if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(joinPathWithLaterSp(o.getWhere(), r), o.isNewScope());
				out.add(no);
				continue;
			}
			if (n instanceof IrMinus) {
				IrMinus m = (IrMinus) n;
				out.add(new IrMinus(joinPathWithLaterSp(m.getWhere(), r), m.isNewScope()));
				continue;
			}
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(joinPathWithLaterSp(b, r));
				}
				out.add(u2);
				continue;
			}
			if (n instanceof IrService) {
				IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), joinPathWithLaterSp(s.getWhere(), r),
						s.isNewScope()));
				continue;
			}
			if (n instanceof IrSubSelect) {
				out.add(n); // keep raw subselects
				continue;
			}
			out.add(n);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		for (IrNode n2 : out) {
			if (!removed.contains(n2)) {
				res.add(n2);
			}
		}
		return res;
	}

	public static boolean sameVar(Var a, Var b) {
		if (a == null || b == null) {
			return false;
		}
		if (a.hasValue() || b.hasValue()) {
			return false;
		}
		return Objects.equals(a.getName(), b.getName());
	}

	/**
	 * True when both variables denote the same term: compares names if both are variables without value, or compares
	 * values if both are constants. Returns false when one has a value and the other does not.
	 */
	public static boolean sameVarOrValue(Var a, Var b) {
		if (a == null || b == null) {
			return false;
		}
		final boolean av = a.hasValue();
		final boolean bv = b.hasValue();
		if (av && bv) {
			return Objects.equals(a.getValue(), b.getValue());
		}
		if (!av && !bv) {
			return Objects.equals(a.getName(), b.getName());
		}
		return false;
	}

	public static boolean isAnonPathVar(Var v) {
		if (v == null || v.hasValue()) {
			return false;
		}
		String n = v.getName();
		return n != null && (n.startsWith(ANON_PATH_PREFIX));
	}

	/** True when the anonymous path var explicitly encodes inverse orientation. */
	public static boolean isAnonPathInverseVar(Var v) {
		return v != null && !v.hasValue() && v.getName() != null && v.getName().startsWith(ANON_PATH_INVERSE_PREFIX);
	}

	/**
	 * True if the given branch contains at least one variable with the parser-generated _anon_path_ (or inverse
	 * variant) prefix anywhere in its simple triple-like structures. Used as a safety valve to allow certain fusions
	 * across UNION branches that were marked as introducing a new scope in the algebra: if every branch contains an
	 * anonymous path bridge var, the fusion is considered safe and preserves user-visible bindings.
	 */
	public static boolean branchHasAnonPathBridge(IrBGP branch) {
		if (branch == null) {
			return false;
		}
		for (IrNode ln : branch.getLines()) {
			if (ln instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) ln;
				Var s = sp.getSubject();
				Var o = sp.getObject();
				Var p = sp.getPredicate();
				if (isAnonPathVar(s) || isAnonPathInverseVar(s) || isAnonPathVar(o) || isAnonPathInverseVar(o)
						|| isAnonPathVar(p) || isAnonPathInverseVar(p)) {
					return true;
				}
			} else if (ln instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) ln;
				if (isAnonPathVar(pt.getSubject()) || isAnonPathInverseVar(pt.getSubject())
						|| isAnonPathVar(pt.getObject())
						|| isAnonPathInverseVar(pt.getObject())) {
					return true;
				}
			} else if (ln instanceof IrGraph) {
				IrGraph g = (IrGraph) ln;
				if (branchHasAnonPathBridge(g.getWhere())) {
					return true;
				}
			} else if (ln instanceof IrOptional) {
				IrOptional o = (IrOptional) ln;
				if (branchHasAnonPathBridge(o.getWhere())) {
					return true;
				}
			} else if (ln instanceof IrMinus) {
				IrMinus m = (IrMinus) ln;
				if (branchHasAnonPathBridge(m.getWhere())) {
					return true;
				}
			} else if (ln instanceof IrBGP) {
				if (branchHasAnonPathBridge((IrBGP) ln)) {
					return true;
				}
			}
		}
		return false;
	}

	/** True if all UNION branches contain at least one _anon_path_* variable (or inverse variant). */
	/**
	 * True if all UNION branches contain at least one _anon_path_* variable (or inverse variant).
	 *
	 * Rationale: when there is no explicit UNION scope, this safety gate ensures branch bodies are derived from
	 * path-decoding internals rather than user variables, so fusing to an alternation/NPS preserves semantics.
	 */
	public static boolean unionBranchesAllHaveAnonPathBridge(IrUnion u) {
		if (unionIsExplicitAndAllBranchesScoped(u)) {
			return false;
		}
		if (u == null || u.getBranches().isEmpty()) {
			return false;
		}
		for (IrBGP b : u.getBranches()) {
			if (!branchHasAnonPathBridge(b)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * True if all UNION branches share at least one common variable name that starts with the _anon_path_ prefix. The
	 * check descends into simple triple-like structures and container blocks.
	 */
	/**
	 * True if all UNION branches share at least one common variable name that starts with the _anon_path_ prefix. The
	 * check descends into simple triple-like structures and container blocks.
	 *
	 * Rationale: used for the special-case where a UNION is marked as a new variable scope but still eligible for
	 * merging — only when we can prove the scope originates from a shared parser-generated bridge variable rather than
	 * a user variable. This keeps merges conservative and avoids collapsing distinct user bindings.
	 */
	public static boolean unionBranchesShareCommonAnonPathVarName(IrUnion u) {
		if (unionIsExplicitAndAllBranchesScoped(u)) {
			return false;
		}
		if (u == null || u.getBranches().isEmpty()) {
			return false;
		}
		Set<String> common = null;
		for (IrBGP b : u.getBranches()) {
			Set<String> names = new HashSet<>();
			collectAnonPathVarNames(b, names);
			if (names.isEmpty()) {
				return false; // a branch without anon-path vars cannot share a common one
			}
			if (common == null) {
				common = new HashSet<>(names);
			} else {
				common.retainAll(names);
				if (common.isEmpty()) {
					return false;
				}
			}
		}
		return common != null && !common.isEmpty();
	}

	/**
	 * New-scope UNION safety: true iff the two UNION branches share at least one _anon_path_* variable name.
	 *
	 * Implementation uses the IR getVars() API to collect all Vars from each branch (including nested nodes) and then
	 * checks for intersection on names that start with the parser bridge prefixes. This captures subject/object,
	 * predicate vars, as well as IrPathTriple.pathVars contributed during path rewrites.
	 */
	public static boolean unionBranchesShareAnonPathVarWithAllowedRoleMapping(IrUnion u) {
		if (unionIsExplicitAndAllBranchesScoped(u)) {
			return false;
		}
		if (u == null || u.getBranches().size() != 2) {
			return false;
		}
		Set<Var> aVars = u.getBranches().get(0).getVars();
		Set<Var> bVars = u.getBranches().get(1).getVars();
		if (aVars == null || bVars == null || aVars.isEmpty() || bVars.isEmpty()) {
			return false;
		}
		Set<String> aNames = new HashSet<>();
		Set<String> bNames = new HashSet<>();
		for (Var v : aVars) {
			if (v != null && !v.hasValue() && v.getName() != null
					&& (v.getName().startsWith(ANON_PATH_PREFIX) || v.getName().startsWith(ANON_PATH_INVERSE_PREFIX))) {
				aNames.add(v.getName());
			}
		}
		for (Var v : bVars) {
			if (v != null && !v.hasValue() && v.getName() != null
					&& (v.getName().startsWith(ANON_PATH_PREFIX) || v.getName().startsWith(ANON_PATH_INVERSE_PREFIX))) {
				bNames.add(v.getName());
			}
		}
		if (!aNames.isEmpty() && !bNames.isEmpty() && intersects(aNames, bNames)) {
			return true;
		}
		return false;
	}

	/**
	 * Determine if a UNION’s branches reduce to a safe alternation over identical endpoints (optionally inside the same
	 * GRAPH). Each branch must be exactly one triple-like (IrStatementPattern or IrPathTriple), or such a triple-like
	 * wrapped in a single IrGraph with the same graph reference across branches. The predicate/path text must be atomic
	 * (no top-level '|' or '/', and no quantifiers), or a simple canonical NPS '!(...)'. Endpoints must align, allowing
	 * a simple inversion for statement patterns or for bare NPS path triples.
	 *
	 * This predicate is intentionally conservative and does not construct any fused node; it only checks structural
	 * eligibility for safe alternation.
	 */
	public static boolean unionBranchesFormSafeAlternation(final IrUnion u, final TupleExprIRRenderer r) {
		if (unionIsExplicitAndAllBranchesScoped(u)) {
			return false;
		}

		if (u == null || u.getBranches() == null || u.getBranches().isEmpty()) {
			return false;
		}
		Var subj = null, obj = null, graphRef = null;
		boolean ok = true;
		for (IrBGP b : u.getBranches()) {
			if (!ok) {
				break;
			}
			if (b == null || b.getLines() == null || b.getLines().isEmpty()) {
				ok = false;
				break;
			}
			IrNode only = (b.getLines().size() == 1) ? b.getLines().get(0) : null;
			IrTripleLike tl = null;
			Var branchGraph = null;
			if (only instanceof IrGraph) {
				IrGraph g = (IrGraph) only;
				IrBGP w = g.getWhere();
				if (w == null || w.getLines() == null || w.getLines().size() != 1
						|| !(w.getLines().get(0) instanceof IrTripleLike)) {
					ok = false;
					break;
				}
				branchGraph = g.getGraph();
				ttl: tl = (IrTripleLike) w.getLines().get(0);
			} else if (only instanceof IrTripleLike) {
				tl = (IrTripleLike) only;
			} else {
				ok = false;
				break;
			}

			if (branchGraph != null) {
				if (graphRef == null) {
					graphRef = branchGraph;
				} else if (!sameVarOrValue(graphRef, branchGraph)) {
					ok = false;
					break;
				}
			} else if (graphRef != null) {
				ok = false;
				break; // mixture of GRAPH and non-GRAPH branches
			}

			final Var s = tl.getSubject();
			final Var o = tl.getObject();
			String piece = tl.getPredicateOrPathText(r);
			if (piece == null) {
				ok = false;
				break;
			}
			// Require atomic or NPS path text
			final String norm = normalizeCompactNps(piece);
			final boolean atomic = isAtomicPathText(piece)
					|| (norm != null && norm.startsWith("!(") && norm.endsWith(")"));
			if (!atomic) {
				ok = false;
				break;
			}

			if (subj == null && obj == null) {
				// Choose canonical endpoints preferring non-anon subject
				if (isAnonPathVar(s) && !isAnonPathVar(o)) {
					subj = o;
					obj = s;
				} else {
					subj = s;
					obj = o;
				}
			}
			if (!(sameVar(subj, s) && sameVar(obj, o))) {
				// Allow inversion when endpoints are reversed
				if (!(sameVar(subj, o) && sameVar(obj, s))) {
					ok = false;
					break;
				}
			}
		}
		return ok;
	}

	private static boolean intersects(Set<String> a, Set<String> b) {
		if (a == null || b == null) {
			return false;
		}
		for (String x : a) {
			if (b.contains(x)) {
				return true;
			}
		}
		return false;
	}

	private static final class BranchRoles {
		final Set<String> s = new HashSet<>();
		final Set<String> o = new HashSet<>();
		final Set<String> p = new HashSet<>();
	}

	private static BranchRoles collectBranchRoles(IrBGP b) {
		if (b == null) {
			return null;
		}
		BranchRoles out = new BranchRoles();
		collectRolesRecursive(b, out);
		// If nothing collected, return null to signal ineligibility
		if (out.s.isEmpty() && out.o.isEmpty() && out.p.isEmpty()) {
			return null;
		}
		return out;
	}

	private static void collectRolesRecursive(IrBGP w, BranchRoles out) {
		if (w == null) {
			return;
		}
		for (IrNode ln : w.getLines()) {
			if (ln instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) ln;
				Var s = sp.getSubject();
				Var o = sp.getObject();
				Var p = sp.getPredicate();
				if (isAnonPathVar(s) || isAnonPathInverseVar(s)) {
					out.s.add(s.getName());
				}
				if (isAnonPathVar(o) || isAnonPathInverseVar(o)) {
					out.o.add(o.getName());
				}
				if (p != null && !p.hasValue() && (isAnonPathVar(p) || isAnonPathInverseVar(p))) {
					out.p.add(p.getName());
				}
			} else if (ln instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) ln;
				Var s = pt.getSubject();
				Var o = pt.getObject();
				if (isAnonPathVar(s) || isAnonPathInverseVar(s)) {
					out.s.add(s.getName());
				}
				if (isAnonPathVar(o) || isAnonPathInverseVar(o)) {
					out.o.add(o.getName());
				}
			} else if (ln instanceof IrGraph) {
				collectRolesRecursive(((IrGraph) ln).getWhere(), out);
			} else if (ln instanceof IrBGP) {
				collectRolesRecursive((IrBGP) ln, out);
			}
		}
	}

	/** Collect names of variables recorded in IrPathTriple.pathVars within a BGP subtree. */
	private static void collectPathVarsNames(IrBGP b, Set<String> out) {
		if (b == null) {
			return;
		}
		for (IrNode ln : b.getLines()) {
			if (ln instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) ln;
				Set<Var> pvs = pt.getPathVars();
				if (pvs != null) {
					for (Var v : pvs) {
						if (v != null && !v.hasValue() && v.getName() != null && !v.getName().isEmpty()) {
							out.add(v.getName());
						}
					}
				}
			} else if (ln instanceof IrGraph) {
				collectPathVarsNames(((IrGraph) ln).getWhere(), out);
			} else if (ln instanceof IrOptional) {
				collectPathVarsNames(((IrOptional) ln).getWhere(), out);
			} else if (ln instanceof IrMinus) {
				collectPathVarsNames(((IrMinus) ln).getWhere(), out);
			} else if (ln instanceof IrUnion) {
				for (IrBGP br : ((IrUnion) ln).getBranches()) {
					collectPathVarsNames(br, out);
				}
			} else if (ln instanceof IrBGP) {
				collectPathVarsNames((IrBGP) ln, out);
			}
		}
	}

	/** Unwrap a branch to a single bare-NPS IrPathTriple when present; otherwise return null. */
	private static IrPathTriple extractSingleBareNpsPathTriple(IrBGP b) {
		if (b == null) {
			return null;
		}
		IrNode node;
		if (b.getLines() == null || b.getLines().size() != 1) {
			return null;
		}
		node = b.getLines().get(0);
		while (node instanceof IrBGP) {
			IrBGP bb = (IrBGP) node;
			if (bb.getLines() == null || bb.getLines().size() != 1) {
				break;
			}
			node = bb.getLines().get(0);
		}
		if (node instanceof IrGraph) {
			IrGraph g = (IrGraph) node;
			IrBGP where = g.getWhere();
			if (where == null || where.getLines() == null || where.getLines().size() != 1) {
				return null;
			}
			node = where.getLines().get(0);
			while (node instanceof IrBGP) {
				IrBGP bb = (IrBGP) node;
				if (bb.getLines() == null || bb.getLines().size() != 1) {
					break;
				}
				node = bb.getLines().get(0);
			}
		}
		if (!(node instanceof IrPathTriple)) {
			return null;
		}
		IrPathTriple pt = (IrPathTriple) node;
		String raw = pt.getPathText();
		String norm = normalizeCompactNps(raw);
		if (norm == null || !norm.startsWith("!(") || !norm.endsWith(")")) {
			return null;
		}
		return pt;
	}

	private static void collectAnonPathVarNames(IrBGP b, Set<String> out) {
		if (b == null) {
			return;
		}
		for (IrNode ln : b.getLines()) {
			if (ln instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) ln;
				Var s = sp.getSubject();
				Var o = sp.getObject();
				Var p = sp.getPredicate();
				if (isAnonPathVar(s) || isAnonPathInverseVar(s)) {
					out.add(s.getName());
				}
				if (isAnonPathVar(o) || isAnonPathInverseVar(o)) {
					out.add(o.getName());
				}
				if (isAnonPathVar(p) || isAnonPathInverseVar(p)) {
					out.add(p.getName());
				}
			} else if (ln instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) ln;
				Var s = pt.getSubject();
				Var o = pt.getObject();
				if (isAnonPathVar(s) || isAnonPathInverseVar(s)) {
					out.add(s.getName());
				}
				if (isAnonPathVar(o) || isAnonPathInverseVar(o)) {
					out.add(o.getName());
				}
			} else if (ln instanceof IrGraph) {
				collectAnonPathVarNames(((IrGraph) ln).getWhere(), out);
			} else if (ln instanceof IrOptional) {
				collectAnonPathVarNames(((IrOptional) ln).getWhere(), out);
			} else if (ln instanceof IrMinus) {
				collectAnonPathVarNames(((IrMinus) ln).getWhere(), out);
			} else if (ln instanceof IrUnion) {
				for (IrBGP br : ((IrUnion) ln).getBranches()) {
					collectAnonPathVarNames(br, out);
				}
			} else if (ln instanceof IrBGP) {
				collectAnonPathVarNames((IrBGP) ln, out);
			}
		}
	}

	/**
	 * If the given path text is a negated property set of the form !(a|b|...), return a version where each member is
	 * inverted by toggling the leading '^' (i.e., a -> ^a, ^a -> a). Returns null when the input is not a simple NPS.
	 */
	public static String invertNegatedPropertySet(String npsText) {
		if (npsText == null) {
			return null;
		}
		String s = npsText.trim();
		if (!s.startsWith("!(") || !s.endsWith(")")) {
			return null;
		}
		String inner = s.substring(2, s.length() - 1);
		if (inner.isEmpty()) {
			return s;
		}
		String[] toks = inner.split("\\|");
		List<String> out = new ArrayList<>(toks.length);
		for (String tok : toks) {
			String t = tok.trim();
			if (t.isEmpty()) {
				continue;
			}
			if (t.startsWith("^")) {
				out.add(t.substring(1));
			} else {
				out.add("^" + t);
			}
		}
		if (out.isEmpty()) {
			return s; // fallback: unchanged
		}
		return "!(" + String.join("|", out) + ")";
	}

	/**
	 * Fuse a path triple whose object is a bridge var with a constant-IRI tail triple that also uses the bridge var,
	 * producing a new path with an added '/^p' or '/p' segment. This version indexes join candidates and works inside
	 * GRAPH bodies as well. It is conservative: only constant predicate tails are fused and containers are preserved.
	 */
	public static IrBGP fuseAltInverseTailBGP(IrBGP bgp, TupleExprIRRenderer r) {
		if (bgp == null) {
			return null;
		}

		final List<IrNode> in = bgp.getLines();
		final List<IrNode> out = new ArrayList<>();
		final Set<IrNode> removed = new HashSet<>();

		// Build index of potential tail-join SPs keyed by the bridge var text ("?name"). We store both
		// subject-joins and object-joins, and prefer object-join (inverse tail) to match expectations.
		final Map<String, List<IrStatementPattern>> bySubject = new HashMap<>();
		final Map<String, List<IrStatementPattern>> byObject = new HashMap<>();
		for (IrNode n : in) {
			if (!(n instanceof IrStatementPattern)) {
				continue;
			}
			final IrStatementPattern sp = (IrStatementPattern) n;
			final Var pv = sp.getPredicate();
			if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
				continue;
			}
			// Only index when the non-bridge end is not an anon_path_* var (safety)
			final String sTxt = varOrValue(sp.getSubject(), r);
			final String oTxt = varOrValue(sp.getObject(), r);
			if (sp.getObject() != null && !isAnonPathVar(sp.getSubject()) && oTxt != null && oTxt.startsWith("?")) {
				byObject.computeIfAbsent(oTxt, k -> new ArrayList<>()).add(sp);
			}
			if (sp.getSubject() != null && !isAnonPathVar(sp.getObject()) && sTxt != null && sTxt.startsWith("?")) {
				bySubject.computeIfAbsent(sTxt, k -> new ArrayList<>()).add(sp);
			}
		}

		for (IrNode n : in) {
			if (removed.contains(n)) {
				continue;
			}

			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				// HEAD fusion: if a SP shares the subject with pt and uses a constant IRI predicate, prefix ^p/ or p/
				final String headBridge = varOrValue(pt.getSubject(), r);
				if (headBridge != null && headBridge.startsWith("?") && isAnonPathVar(pt.getSubject())) {
					IrStatementPattern head = null;
					boolean headInverse = true; // (?mid p ?x) => ^p/
					final List<IrStatementPattern> hs = bySubject.get(headBridge);
					if (hs != null) {
						for (IrStatementPattern sp : hs) {
							if (removed.contains(sp)) {
								continue;
							}
							if (sp.getPredicate() == null || !sp.getPredicate().hasValue()
									|| !(sp.getPredicate().getValue() instanceof IRI)) {
								continue;
							}
							head = sp;
							headInverse = true;
							break;
						}
					}
					if (head == null) {
						final List<IrStatementPattern> ho = byObject.get(headBridge);
						if (ho != null) {
							for (IrStatementPattern sp : ho) {
								if (removed.contains(sp)) {
									continue;
								}
								if (sp.getPredicate() == null || !sp.getPredicate().hasValue()
										|| !(sp.getPredicate().getValue() instanceof IRI)) {
									continue;
								}
								head = sp;
								headInverse = false; // (?x p ?mid) => p/
								break;
							}
						}
					}
					if (head != null) {
						final String ptxt = r.convertIRIToString((IRI) head.getPredicate().getValue());
						final String prefix = (headInverse ? "^" : "") + ptxt + "/";
						final Var newStart = headInverse ? head.getObject() : head.getSubject();
						final IrNode newStartOverride = headInverse ? head.getObjectOverride()
								: head.getSubjectOverride();
						pt = new IrPathTriple(newStart, newStartOverride, prefix + pt.getPathText(), pt.getObject(),
								pt.getObjectOverride(), pt.getPathVars(), pt.isNewScope());
						removed.add(head);
					}
				}

				// TAIL fusion: attach a constant predicate SP that shares the object
				final String bridge = varOrValue(pt.getObject(), r);
				if (bridge != null && bridge.startsWith("?")) {
					// Only join when the bridge var is an _anon_path_* variable, to avoid eliminating user vars
					if (!isAnonPathVar(pt.getObject())) {
						out.add(pt);
						continue;
					}
					IrStatementPattern join = null;
					boolean inverse = true; // prefer inverse tail (?y p ?mid) => '^p'
					final List<IrStatementPattern> byObj = byObject.get(bridge);
					if (byObj != null) {
						for (IrStatementPattern sp : byObj) {
							if (!removed.contains(sp)) {
								join = sp;
								inverse = true;
								break;
							}
						}
					}
					if (join == null) {
						final List<IrStatementPattern> bySub = bySubject.get(bridge);
						if (bySub != null) {
							for (IrStatementPattern sp : bySub) {
								if (!removed.contains(sp)) {
									join = sp;
									inverse = false;
									break;
								}
							}
						}
					}
					if (join != null) {
						final String step = r.convertIRIToString((IRI) join.getPredicate().getValue());
						final String newPath = pt.getPathText() + "/" + (inverse ? "^" : "") + step;
						final Var newEnd = inverse ? join.getSubject() : join.getObject();
						final IrNode newEndOverride = inverse ? join.getSubjectOverride() : join.getObjectOverride();
						pt = new IrPathTriple(pt.getSubject(), pt.getSubjectOverride(), newPath, newEnd, newEndOverride,
								pt.getPathVars(), pt.isNewScope());
						removed.add(join);
					}
				}
				out.add(pt);
				continue;
			}

			// Recurse into containers
			if (n instanceof IrGraph) {
				final IrGraph g = (IrGraph) n;
				out.add(new IrGraph(g.getGraph(), fuseAltInverseTailBGP(g.getWhere(), r), g.isNewScope()));
				continue;
			}
			if (n instanceof IrOptional) {
				final IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(fuseAltInverseTailBGP(o.getWhere(), r), o.isNewScope());
				no.setNewScope(o.isNewScope());
				out.add(no);
				continue;
			}
			if (n instanceof IrMinus) {
				final IrMinus m = (IrMinus) n;
				out.add(new IrMinus(fuseAltInverseTailBGP(m.getWhere(), r), m.isNewScope()));
				continue;
			}
			if (n instanceof IrUnion) {
				final IrUnion u = (IrUnion) n;
				final IrUnion u2 = new IrUnion(u.isNewScope());
				for (IrBGP b : u.getBranches()) {
					u2.addBranch(fuseAltInverseTailBGP(b, r));
				}
				out.add(u2);
				continue;
			}
			if (n instanceof IrService) {
				final IrService s = (IrService) n;
				out.add(new IrService(s.getServiceRefText(), s.isSilent(), fuseAltInverseTailBGP(s.getWhere(), r),
						s.isNewScope()));
				continue;
			}
			// Subselects: keep as-is
			out.add(n);
		}

		final IrBGP res = new IrBGP(bgp.isNewScope());
		for (IrNode n2 : out) {
			if (!removed.contains(n2)) {
				res.add(n2);
			}
		}
		res.setNewScope(bgp.isNewScope());
		return res;
	}

	public static String varOrValue(Var v, TupleExprIRRenderer r) {
		if (v == null) {
			return "?_";
		}
		if (v.hasValue()) {
			return r.convertValueToString(v.getValue());
		}
		return "?" + v.getName();
	}

}
