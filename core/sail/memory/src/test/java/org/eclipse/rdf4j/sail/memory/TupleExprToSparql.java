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

package org.eclipse.rdf4j.sail.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * TupleExprToSparql: render a practical subset of RDF4J algebra back into SPARQL text.
 *
 * Supported: - SELECT [DISTINCT] vars | * - WHERE with BGPs (StatementPattern / Join), OPTIONAL (LeftJoin), UNION,
 * FILTER, BIND (Extension) - ORDER BY - VALUES (BindingSetAssignment) - GRAPH, SERVICE [SILENT] - Property paths:
 * ArbitraryLengthPath (+, *, ?, {m,n}) and ZeroLengthPath - Prefix compaction (longest namespace match) - Canonical
 * whitespace toggle for stable, diffable output
 *
 * Design goals: - Deterministic, readable output; safe fallbacks instead of brittle "smart" guessing - Minimal,
 * dependency-free (beyond RDF4J), Java 11 compatible
 */
public class TupleExprToSparql {

	// ---------------- Configuration ----------------

	public static final class Config {
		/** Indentation used per nesting level. */
		public String indent = "  ";
		/** Emit PREFIX declarations if prefixes are provided. */
		public boolean printPrefixes = true;
		/** Use prefix/QName compaction for IRIs when possible. */
		public boolean usePrefixCompaction = true;
		/** Canonical whitespace: one-triple-per-line, stable braces/newlines. */
		public boolean canonicalWhitespace = true;
		/** Optional BASE IRI. */
		public String baseIRI = null;
		/**
		 * Map of prefix -> namespace IRI (e.g., "foaf" -> "http://xmlns.com/foaf/0.1/"). Longest namespace match is
		 * used for compaction.
		 */
		public LinkedHashMap<String, String> prefixes = new LinkedHashMap<>();
	}

	private final Config cfg;
	private final PrefixIndex prefixIndex;

	public TupleExprToSparql() {
		this(new Config());
	}

	public TupleExprToSparql(final Config cfg) {
		this.cfg = cfg == null ? new Config() : cfg;
		this.prefixIndex = new PrefixIndex(this.cfg.prefixes);
	}

	/** Render a TupleExpr into SPARQL. Thread-safe for concurrent calls (no shared mutable state). */
	public String render(final TupleExpr tupleExpr) {
		Objects.requireNonNull(tupleExpr, "tupleExpr");
		final StringBuilder out = new StringBuilder(256);

		final Normalized n = normalize(tupleExpr);

		// Prefix/BASE header
		if (cfg.printPrefixes && !cfg.prefixes.isEmpty()) {
			cfg.prefixes.forEach((pfx, ns) -> out.append("PREFIX ").append(pfx).append(": <").append(ns).append(">\n"));
		}
		if (cfg.baseIRI != null && !cfg.baseIRI.isEmpty()) {
			out.append("BASE <").append(cfg.baseIRI).append(">\n");
		}

		// SELECT header
		out.append("SELECT ");
		if (n.distinct) {
			out.append("DISTINCT ");
		}
		if (n.projection != null) {
			final String vars = projectVars(n.projection.getProjectionElemList());
			out.append(vars.isEmpty() ? "*" : vars);
		} else {
			out.append("*");
		}

		// WHERE block
		out.append(cfg.canonicalWhitespace ? "\nWHERE " : " WHERE ");
		final BlockPrinter bp = new BlockPrinter(out, this, cfg);
		bp.openBlock();

		// Hoisted BINDs (immediately above Projection)
		if (!n.preBinds.isEmpty()) {
			for (final ExtensionElem ee : n.preBinds) {
				bp.line("BIND(" + renderExpr(ee.getExpr()) + " AS ?" + ee.getName() + ")");
			}
		}

		n.where.visit(bp);
		bp.closeBlock();

		// ORDER BY
		if (!n.orderBy.isEmpty()) {
			out.append("\nORDER BY");
			for (final OrderElem oe : n.orderBy) {
				final String expr = renderExpr(oe.getExpr());
				if (oe.isAscending()) {
					out.append(' ').append(expr);
				} else {
					out.append(" DESC(").append(expr).append(')');
				}
			}
		}

		// LIMIT/OFFSET
		if (n.limit >= 0) {
			out.append("\nLIMIT ").append(n.limit);
		}
		if (n.offset >= 0) {
			out.append("\nOFFSET ").append(n.offset);
		}

		return out.toString().trim();
	}

	// ---------------- Normalization of the algebra "shell" ----------------

	private static final class Normalized {
		Projection projection; // SELECT vars
		TupleExpr where; // WHERE pattern
		boolean distinct = false;
		long limit = -1, offset = -1;
		final List<ExtensionElem> preBinds = new ArrayList<>();
		final List<OrderElem> orderBy = new ArrayList<>();
	}

	/**
	 * Peel standard wrappers—Slice, Distinct/Reduced, Order, Extension (hoist binds above projection), Projection— to
	 * locate the core WHERE tuple expression. Order is robust: repeat until fixed point.
	 */
	private Normalized normalize(final TupleExpr root) {
		final Normalized n = new Normalized();
		TupleExpr cur = root;

		boolean changed;
		do {
			changed = false;

			if (cur instanceof Slice) {
				final Slice s = (Slice) cur;
				n.limit = s.getLimit();
				n.offset = s.getOffset();
				cur = s.getArg();
				changed = true;
				continue;
			}

			if (cur instanceof Distinct) {
				n.distinct = true;
				cur = ((Distinct) cur).getArg();
				changed = true;
				continue;
			}

			if (cur instanceof org.eclipse.rdf4j.query.algebra.Reduced) {
				cur = ((org.eclipse.rdf4j.query.algebra.Reduced) cur).getArg();
				changed = true;
				continue;
			}

			if (cur instanceof Order) {
				final Order o = (Order) cur;
				n.orderBy.addAll(o.getElements());
				cur = o.getArg();
				changed = true;
				continue;
			}

			if (cur instanceof Extension) {
				final Extension ext = (Extension) cur;
				if (ext.getArg() instanceof Projection) {
					n.preBinds.addAll(ext.getElements());
					cur = ext.getArg();
					changed = true;
					continue;
				}
				// Otherwise: render this Extension inside WHERE; stop hoisting
			}

			if (cur instanceof Projection) {
				n.projection = (Projection) cur;
				cur = n.projection.getArg();
				changed = true;
			}

		} while (changed);

		n.where = cur;
		return n;
	}

	private String projectVars(final ProjectionElemList pel) {
		if (pel == null) {
			return "";
		}
		final List<String> vars = new ArrayList<>(pel.getElements().size());
		for (final ProjectionElem pe : pel.getElements()) {
			final String name = pe.getProjectionAlias().orElse(pe.getName());
			if (name != null && !name.isEmpty()) {
				vars.add("?" + name);
			}
		}
		return String.join(" ", vars);
	}

	// ---------------- Block/Node printer ----------------

	private static final class BlockPrinter extends AbstractQueryModelVisitor<RuntimeException> {
		private final StringBuilder out;
		private final TupleExprToSparql r;
		private final Config cfg;
		private final String indentUnit;
		private int level = 0;

		BlockPrinter(final StringBuilder out, final TupleExprToSparql renderer, final Config cfg) {
			this.out = out;
			this.r = renderer;
			this.cfg = cfg;
			this.indentUnit = cfg.indent == null ? "  " : cfg.indent;
		}

		void openBlock() {
			out.append("{");
			newline();
			level++;
		}

		void closeBlock() {
			level--;
			indent();
			out.append("}");
		}

		void line(final String s) {
			indent();
			out.append(s);
			newline();
		}

		void raw(final String s) {
			out.append(s);
		}

		void newline() {
			out.append('\n');
		}

		void indent() {
			for (int i = 0; i < level; i++) {
				out.append(indentUnit);
			}
		}

		// ---- Canonical, one-triple-per-line BGPs ----

		@Override
		public void meet(final StatementPattern sp) {
			final String s = r.renderVarOrValue(sp.getSubjectVar());
			final String p = r.renderVarOrValue(sp.getPredicateVar());
			final String o = r.renderVarOrValue(sp.getObjectVar());
			line(s + " " + p + " " + o + " .");
		}

		@Override
		public void meet(final Join join) {
			join.getLeftArg().visit(this);
			join.getRightArg().visit(this);
		}

		@Override
		public void meet(final LeftJoin lj) {
			lj.getLeftArg().visit(this);
			indent();
			raw("OPTIONAL ");
			openBlock();
			lj.getRightArg().visit(this);
			if (lj.getCondition() != null) {
				line("FILTER (" + r.renderExpr(lj.getCondition()) + ")");
			}
			closeBlock();
			newline();
		}

		@Override
		public void meet(final Union union) {
			indent();
			openBlock();
			union.getLeftArg().visit(this);
			closeBlock();
			newline();
			indent();
			line("UNION");
			indent();
			openBlock();
			union.getRightArg().visit(this);
			closeBlock();
			newline();
		}

		@Override
		public void meet(final Filter filter) {
			filter.getArg().visit(this);
			line("FILTER (" + r.renderExpr(filter.getCondition()) + ")");
		}

		@Override
		public void meet(final Extension ext) {
			ext.getArg().visit(this);
			for (final ExtensionElem ee : ext.getElements()) {
				line("BIND(" + r.renderExpr(ee.getExpr()) + " AS ?" + ee.getName() + ")");
			}
		}

//		@Override
//		public void meet(final Graph graph) {
//			indent(); raw("GRAPH " + r.renderVarOrValue(graph.getContextVar()) + " ");
//			openBlock();
//			graph.getArg().visit(this);
//			closeBlock(); newline();
//		}

		@Override
		public void meet(final Service svc) {
			indent();
			raw("SERVICE ");
			if (svc.isSilent()) {
				raw("SILENT ");
			}
			raw(r.renderVarOrValue(svc.getServiceRef()) + " ");
			openBlock();
			svc.getArg().visit(this);
			closeBlock();
			newline();
		}

		@Override
		public void meet(final BindingSetAssignment bsa) {
			// Deterministic variable order for stable output
			final List<String> names = new ArrayList<>(bsa.getBindingNames());
			Collections.sort(names);
			if (names.isEmpty()) {
				return;
			}

			final String head = names.stream().map(n -> "?" + n).collect(Collectors.joining(" "));
			indent();
			raw("VALUES (" + head + ") ");
			openBlock();
			for (final BindingSet bs : bsa.getBindingSets()) {
				indent();
				raw("(");
				for (int i = 0; i < names.size(); i++) {
					final String n = names.get(i);
					final Value v = bs.getValue(n);
					raw(v == null ? "UNDEF" : r.renderValue(v));
					if (i + 1 < names.size()) {
						raw(" ");
					}
				}
				raw(")");
				newline();
			}
			closeBlock();
			newline();
		}

		// ---- Property paths (instanceof, not stringly-typed) ----

		@Override
		public void meet(final ArbitraryLengthPath p) {
			final String subj = r.renderVarOrValue(p.getSubjectVar());
			final String obj = r.renderVarOrValue(p.getObjectVar());
			final String path = r.renderPathAtom(p.getPathExpression());
			final long min = p.getMinLength();
			final long max = -1; // -1 means unbounded in RDF4J

			final String q = quantifier(min, max);
			final String pathAtom = (path != null) ? path : "/* complex-path */";
			line(subj + " " + pathAtom + q + " " + obj + " .");
		}

		@Override
		public void meet(final ZeroLengthPath p) {
			// SPARQL doesn't have a naked zero-length path operator; encode as term equality
			line("FILTER (sameTerm(" + r.renderVarOrValue(p.getSubjectVar()) + ", "
					+ r.renderVarOrValue(p.getObjectVar()) + "))");
		}

		@Override
		public void meetOther(final org.eclipse.rdf4j.query.algebra.QueryModelNode node) {
			// Unknown node: leave a helpful, minimal breadcrumb; avoid throwing.
			line("/* unsupported-node:" + node.getClass().getSimpleName() + " */");
		}

		// ---- helpers ----

		private static String quantifier(final long min, final long max) {
			final boolean unbounded = max < 0 || max == Integer.MAX_VALUE;
			if (min == 0 && unbounded) {
				return "*";
			}
			if (min == 1 && unbounded) {
				return "+";
			}
			if (min == 0 && max == 1) {
				return "?";
			}
			if (unbounded) {
				return "{" + min + ",}";
			}
			if (min == max) {
				return "{" + min + "}";
			}
			return "{" + min + "," + max + "}";
		}
	}

	// ---------------- Rendering helpers (instance methods; prefix-aware) ----------------

	private String renderVarOrValue(final Var v) {
		if (v == null) {
			return "?_";
		}
		if (v.hasValue()) {
			return renderValue(v.getValue());
		}
		return "?" + v.getName();
	}

	private String renderValue(final Value val) {
		if (val instanceof IRI) {
			return renderIRI((IRI) val);
		} else if (val instanceof Literal) {
			final Literal lit = (Literal) val;
			final String escaped = escapeLiteral(lit.getLabel());
			if (lit.getLanguage().isPresent()) {
				return "\"" + escaped + "\"@" + lit.getLanguage().get();
			}
			final IRI dt = lit.getDatatype();
			if (dt != null && !XSD.STRING.equals(dt)) {
				return "\"" + escaped + "\"^^" + renderIRI(dt);
			}
			return "\"" + escaped + "\"";
		} else if (val instanceof BNode) {
			return "_:" + ((BNode) val).getID();
		}
		return "\"" + escapeLiteral(String.valueOf(val)) + "\"";
	}

	private String renderIRI(final IRI iri) {
		final String s = iri.stringValue();
		if (cfg.usePrefixCompaction) {
			final PrefixHit hit = prefixIndex.longestMatch(s);
			if (hit != null) {
				final String local = s.substring(hit.namespace.length());
				if (isPN_LOCAL(local)) {
					return hit.prefix + ":" + local;
				}
				// local contains characters that would make an illegal QName -> fall back
			}
		}
		return "<" + s + ">";
	}

	private static final Pattern PN_LOCAL = Pattern.compile("[A-Za-z_][A-Za-z0-9_\\-\\.]*");

	private boolean isPN_LOCAL(final String s) {
		return s != null && !s.isEmpty() && PN_LOCAL.matcher(s).matches();
	}

	private static String escapeLiteral(final String s) {
		final StringBuilder b = new StringBuilder(Math.max(16, s.length()));
		for (int i = 0; i < s.length(); i++) {
			final char c = s.charAt(i);
			switch (c) {
			case '\\':
				b.append("\\\\");
				break;
			case '\"':
				b.append("\\\"");
				break;
			case '\n':
				b.append("\\n");
				break;
			case '\r':
				b.append("\\r");
				break;
			case '\t':
				b.append("\\t");
				break;
			default:
				b.append(c);
			}
		}
		return b.toString();
	}

	private String renderExpr(final ValueExpr e) {
		if (e == null) {
			return "()";
		}

		// Vars and constants
		if (e instanceof Var) {
			final Var v = (Var) e;
			return v.hasValue() ? renderValue(v.getValue()) : "?" + v.getName();
		}
		if (e instanceof ValueConstant) {
			return renderValue(((ValueConstant) e).getValue());
		}

		// Unary
		if (e instanceof Not) {
			return "!(" + renderExpr(((Not) e).getArg()) + ")";
		}
		if (e instanceof Bound) {
			return "BOUND(" + renderExpr(((Bound) e).getArg()) + ")";
		}
		if (e instanceof Str) {
			return "STR(" + renderExpr(((Str) e).getArg()) + ")";
		}
		if (e instanceof Datatype) {
			return "DATATYPE(" + renderExpr(((Datatype) e).getArg()) + ")";
		}
		if (e instanceof Lang) {
			return "LANG(" + renderExpr(((Lang) e).getArg()) + ")";
		}
		if (e instanceof IsURI) {
			return "isIRI(" + renderExpr(((IsURI) e).getArg()) + ")";
		}
		if (e instanceof IsLiteral) {
			return "isLiteral(" + renderExpr(((IsLiteral) e).getArg()) + ")";
		}
		if (e instanceof IsBNode) {
			return "isBlank(" + renderExpr(((IsBNode) e).getArg()) + ")";
		}

		// Binary / ternary
		if (e instanceof And) {
			final And a = (And) e;
			return "(" + renderExpr(a.getLeftArg()) + " && " + renderExpr(a.getRightArg()) + ")";
		}
		if (e instanceof Or) {
			final Or o = (Or) e;
			return "(" + renderExpr(o.getLeftArg()) + " || " + renderExpr(o.getRightArg()) + ")";
		}
		if (e instanceof Compare) {
			final Compare c = (Compare) e;
			return "(" + renderExpr(c.getLeftArg()) + " " + op(c.getOperator()) + " " + renderExpr(c.getRightArg())
					+ ")";
		}
		if (e instanceof SameTerm) {
			final SameTerm st = (SameTerm) e;
			return "sameTerm(" + renderExpr(st.getLeftArg()) + ", " + renderExpr(st.getRightArg()) + ")";
		}
		if (e instanceof LangMatches) {
			final LangMatches lm = (LangMatches) e;
			return "LANGMATCHES(" + renderExpr(lm.getLeftArg()) + ", " + renderExpr(lm.getRightArg()) + ")";
		}
		if (e instanceof Regex) {
			final Regex r = (Regex) e;
			final String term = renderExpr(r.getArg());
			final String patt = renderExpr(r.getPatternArg());
			if (r.getFlagsArg() != null) {
				return "REGEX(" + term + ", " + patt + ", " + renderExpr(r.getFlagsArg()) + ")";
			}
			return "REGEX(" + term + ", " + patt + ")";
		}

		// Generic function call fallback
		if (e instanceof FunctionCall) {
			final FunctionCall f = (FunctionCall) e;
			final String args = f.getArgs().stream().map(this::renderExpr).collect(Collectors.joining(", "));
			return "<" + f.getURI() + ">(" + args + ")";
		}

		// Unknown expr node: best-effort debug print.
		return "/* unsupported-expr:" + e.getClass().getSimpleName() + " */";
	}

	private static String op(final CompareOp op) {
		switch (op) {
		case EQ:
			return "=";
		case NE:
			return "!=";
		case LT:
			return "<";
		case LE:
			return "<=";
		case GT:
			return ">";
		case GE:
			return ">=";
		default:
			return "/*?*/";
		}
	}

	/**
	 * Render a simple path atom from ArbitraryLengthPath#getPathExpression(): supports IRI constants and plain
	 * variables; returns null for complex composites.
	 */
	private String renderPathAtom(final TupleExpr pathExpr) {
		if (pathExpr instanceof Var) {
			final Var v = (Var) pathExpr;
			if (v.hasValue() && v.getValue() instanceof IRI) {
				return renderIRI((IRI) v.getValue());
			}
			return "?" + v.getName();
		}
		if (pathExpr instanceof ValueConstant) {
			final Value v = ((ValueConstant) pathExpr).getValue();
			if (v instanceof IRI) {
				return renderIRI((IRI) v);
			}
		}
		return null; // signal "complex"; caller will print a safe comment
	}

	// ---------------- Prefix compaction index ----------------

	private static final class PrefixHit {
		final String prefix;
		final String namespace;

		PrefixHit(final String prefix, final String namespace) {
			this.prefix = prefix;
			this.namespace = namespace;
		}
	}

	private static final class PrefixIndex {
		private final List<Map.Entry<String, String>> entries;

		PrefixIndex(final Map<String, String> prefixes) {
			final List<Map.Entry<String, String>> list = new ArrayList<>();
			if (prefixes != null) {
				list.addAll(prefixes.entrySet());
			}
			list.sort((a, b) -> Integer.compare(b.getValue().length(), a.getValue().length())); // longest first
			this.entries = Collections.unmodifiableList(list);
		}

		PrefixHit longestMatch(final String iri) {
			if (iri == null) {
				return null;
			}
			for (final Map.Entry<String, String> e : entries) {
				final String ns = e.getValue();
				if (iri.startsWith(ns)) {
					return new PrefixHit(e.getKey(), ns);
				}
			}
			return null;
		}
	}
}
