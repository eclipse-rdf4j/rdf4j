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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Textual IR for a WHERE/group block: ordered list of lines/nodes.
 *
 * Semantics: - Lines typically include triples ({@link IrStatementPattern} or {@link IrPathTriple}), modifiers
 * ({@link IrFilter}, {@link IrBind}, {@link IrValues}), and container blocks such as {@link IrGraph},
 * {@link IrOptional}, {@link IrMinus}, {@link IrUnion}, {@link IrService}. - Order matters: most transforms preserve
 * relative order except where a local, safe rewrite explicitly requires adjacency. - Printing is delegated to
 * {@link IrPrinter}; indentation and braces are handled there.
 */
public class IrBGP extends IrNode {
	private static final boolean DEBUG_PROPERTY_LISTS = Boolean
			.getBoolean("rdf4j.queryrender.debugPropertyLists");
	private List<IrNode> lines = new ArrayList<>();

	public IrBGP(boolean newScope) {
		super(newScope);
	}

	public IrBGP(IrBGP where, boolean newScope) {
		super(newScope);
		add(where);
	}

	public IrBGP(List<IrNode> lines, boolean newScope) {
		super(newScope);
		this.lines = lines;
	}

	public List<IrNode> getLines() {
		return lines;
	}

	public void add(IrNode node) {
		if (node != null) {
			lines.add(node);
		}
	}

	@Override
	public void print(IrPrinter p) {
		p.openBlock();
		if (isNewScope()) {
			p.openBlock();
		}
		List<IrNode> ordered = stablyOrdered(lines);
		printWithPropertyLists(p, ordered);
		if (isNewScope()) {
			p.closeBlock();
		}
		p.closeBlock();
	}

	@Override
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		IrBGP w = new IrBGP(this.isNewScope());
		for (IrNode ln : this.lines) {
			IrNode t = op.apply(ln);
			t = t.transformChildren(op);
			w.add(t == null ? ln : t);
		}
		return w;
	}

	@Override
	public String toString() {
		return "IrBGP{" +
				"lines=" + Arrays.toString(lines.toArray()) +
				'}';
	}

	private static List<IrNode> stablyOrdered(List<IrNode> in) {
		if (in == null || in.size() < 2) {
			return in;
		}
		// Heuristic: sort triples sharing anonymous bnode subjects so property-list intent is preserved.
		boolean allTriples = in.stream().allMatch(n -> n instanceof IrStatementPattern);
		if (!allTriples) {
			return in;
		}
		boolean allAnonSubjects = in.stream().allMatch(n -> {
			Var s = ((IrStatementPattern) n).getSubject();
			return s != null && s.isAnonymous();
		});
		if (!allAnonSubjects) {
			return in;
		}
		List<IrNode> copy = new ArrayList<>(in);
		copy.sort((a, b) -> {
			IrStatementPattern sa = (IrStatementPattern) a;
			IrStatementPattern sb = (IrStatementPattern) b;
			int c = name(sa.getSubject()).compareTo(name(sb.getSubject()));
			if (c != 0) {
				return c;
			}
			return name(sa.getPredicate()).compareTo(name(sb.getPredicate()));
		});
		return copy;
	}

	private static String name(Var v) {
		return v == null ? "" : String.valueOf(v.getName());
	}

	private void printWithPropertyLists(IrPrinter p, List<IrNode> ordered) {
		if (ordered == null || ordered.isEmpty()) {
			return;
		}

		Map<String, List<IrStatementPattern>> bySubject = new LinkedHashMap<>();
		Set<String> childSubjects = new HashSet<>();
		for (IrNode n : ordered) {
			if (n instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) n;
				if (isPropertyListCandidate(sp)) {
					String subjName = name(sp.getSubject());
					bySubject.computeIfAbsent(subjName, k -> new ArrayList<>()).add(sp);
					Var obj = sp.getObject();
					if (obj != null && obj.isAnonymous()) {
						String objName = name(obj);
						if (isAutoAnonBNodeName(objName)) {
							childSubjects.add(objName);
						}
					}
				}
			}
		}

		if (DEBUG_PROPERTY_LISTS && !bySubject.isEmpty()) {
			System.out.println("[irbgp-debug] property list subjects=" + bySubject.keySet()
					+ " childSubjects=" + childSubjects);
		}

		for (IrNode n : ordered) {
			if (n instanceof IrStatementPattern) {
				IrStatementPattern sp = (IrStatementPattern) n;
				if (isPropertyListCandidate(sp)) {
					String subjName = name(sp.getSubject());
					if (isAutoAnonBNodeName(subjName) && childSubjects.contains(subjName)
							&& bySubject.containsKey(subjName)) {
						if (DEBUG_PROPERTY_LISTS) {
							System.out.println("[irbgp-debug] deferring nested property list for " + subjName);
						}
						continue;
					}
					if (bySubject.containsKey(subjName)) {
						printPropertyList(subjName, bySubject, p);
					}
					continue;
				}
			}
			if (n != null) {
				n.print(p);
			}
		}
	}

	private void printPropertyList(String subjName, Map<String, List<IrStatementPattern>> bySubject, IrPrinter p) {
		List<IrStatementPattern> props = bySubject.remove(subjName);
		if (props == null || props.isEmpty()) {
			return;
		}

		IrStatementPattern first = props.get(0);
		String subjText = renderNodeOrVar(first.getSubjectOverride(), first.getSubject(), p);
		String align = " ".repeat(Math.max(1, subjText.length() + 1));

		for (int i = 0; i < props.size(); i++) {
			IrStatementPattern sp = props.get(i);
			StringBuilder sb = new StringBuilder();
			if (i == 0) {
				sb.append(subjText).append(" ");
			} else {
				sb.append(align);
			}
			sb.append(p.convertVarToString(sp.getPredicate())).append(" ");
			sb.append(renderObject(sp, bySubject, p));
			if (i == props.size() - 1) {
				sb.append(" .");
			} else {
				sb.append(" ;");
			}
			p.line(sb.toString());
		}
	}

	private String renderPropertyListInline(String subjName, Map<String, List<IrStatementPattern>> bySubject,
			IrPrinter p) {
		List<IrStatementPattern> props = bySubject.remove(subjName);
		if (props == null || props.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < props.size(); i++) {
			IrStatementPattern sp = props.get(i);
			if (i > 0) {
				sb.append(" ; ");
			}
			sb.append(p.convertVarToString(sp.getPredicate())).append(" ");
			sb.append(renderObject(sp, bySubject, p));
		}
		return sb.toString();
	}

	private String renderObject(IrStatementPattern sp, Map<String, List<IrStatementPattern>> bySubject, IrPrinter p) {
		if (sp.getObjectOverride() != null) {
			StringBuilder tmp = new StringBuilder();
			sp.getObjectOverride().print(new InlinePrinter(tmp, p::convertVarToString));
			return tmp.toString();
		}
		Var obj = sp.getObject();
		if (obj != null && obj.isAnonymous()) {
			List<IrStatementPattern> nested = bySubject.get(name(obj));
			if (nested != null && nested.size() >= 1) {
				// inline nested property list
				String nestedText = renderPropertyListInline(name(obj), bySubject, p);
				return "[ " + nestedText + " ]";
			}
		}
		return p.convertVarToString(obj);
	}

	private static String renderNodeOrVar(IrNode override, Var v, IrPrinter p) {
		if (override != null) {
			StringBuilder tmp = new StringBuilder();
			override.print(new InlinePrinter(tmp, p::convertVarToString));
			return tmp.toString();
		}
		if (v != null && v.isAnonymous()) {
			String name = v.getName();
			assert name == null || !name.startsWith("anon_");
			if (name != null && name.startsWith("_anon_bnode_")) {
				return "[]";
			}
		}
		return p.convertVarToString(v);
	}

	private boolean isPropertyListCandidate(IrStatementPattern sp) {
		if (sp == null || sp.getSubjectOverride() != null) {
			return false;
		}
		Var s = sp.getSubject();
		if (s == null || !s.isAnonymous()) {
			return false;
		}
		String n = s.getName();
		if (n == null) {
			return false;
		}
		assert !n.startsWith("anon_");

		if (n.startsWith("_anon_path_")) {
			return false;
		}
		return n.startsWith("_anon_bnode_") || n.startsWith("_anon_user_bnode_");
	}

	private boolean isAutoAnonBNodeName(String n) {
		if (n == null) {
			return false;
		}
		assert !n.startsWith("anon_");

		return n.startsWith("_anon_bnode_");
	}

	private static final class InlinePrinter implements IrPrinter {
		private final StringBuilder out;
		private final java.util.function.Function<Var, String> fmt;

		InlinePrinter(StringBuilder out, java.util.function.Function<Var, String> fmt) {
			this.out = out;
			this.fmt = fmt;
		}

		@Override
		public void startLine() {
		}

		@Override
		public void append(String s) {
			out.append(s);
		}

		@Override
		public void endLine() {
		}

		@Override
		public void line(String s) {
			out.append(s);
		}

		@Override
		public void openBlock() {
		}

		@Override
		public void closeBlock() {
		}

		@Override
		public void pushIndent() {
		}

		@Override
		public void popIndent() {
		}

		@Override
		public void printLines(List<IrNode> lines) {
			if (lines == null) {
				return;
			}
			for (IrNode n : lines) {
				if (n != null) {
					n.print(this);
				}
			}
		}

		@Override
		public String convertVarToString(Var v) {
			return fmt.apply(v);
		}
	}

	@Override
	public Set<Var> getVars() {
		HashSet<Var> out = new HashSet<>();
		for (IrNode ln : lines) {
			if (ln != null) {
				out.addAll(ln.getVars());
			}
		}
		return out;
	}
}
