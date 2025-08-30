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

package org.eclipse.rdf4j.queryrender.sparql;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Coalesce;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGroupByElem;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOrderSpec;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPrinter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrProjectionItem;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPropertyList;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.IrTransforms;

/**
 * TupleExprIRRenderer: render RDF4J algebra back into SPARQL text (via a compact internal normalization/IR step), with:
 *
 * <ul>
 * <li>SELECT / ASK / DESCRIBE / CONSTRUCT forms</li>
 * <li>BGPs, OPTIONALs, UNIONs, MINUS, GRAPH, SERVICE, VALUES</li>
 * <li>Property paths, plus safe best-effort reassembly for simple cases</li>
 * <li>Aggregates, GROUP BY, HAVING (with _anon_having_* substitution)</li>
 * <li>Subselects in WHERE</li>
 * <li>ORDER BY, LIMIT, OFFSET</li>
 * <li>Prefix compaction and nice formatting</li>
 * </ul>
 *
 * How it works (big picture):
 * <ul>
 * <li>Normalize the TupleExpr (peel Order/Slice/Distinct/etc., detect HAVING) into a lightweight {@code Normalized}
 * carrier.</li>
 * <li>Build a textual Intermediate Representation (IR) that mirrors SPARQL’s shape: a header (projection), a list-like
 * WHERE block ({@link IrBGP}), and trailing modifiers. The IR tries to be a straightforward, low-logic mirror of the
 * TupleExpr tree.</li>
 * <li>Run a small, ordered pipeline of IR transforms ({@link IrTransforms}) that are deliberately side‑effect‑free and
 * compositional. Each transform is narrowly scoped (e.g., property path fusions, negated property sets, collections)
 * and uses simple heuristics like only fusing across parser‑generated bridge variables named with the
 * {@code _anon_path_} prefix.</li>
 * <li>Print the transformed IR using a tiny printer interface ({@link IrPrinter}) that centralizes indentation, IRI
 * compaction, and child printing.</li>
 * </ul>
 *
 * Policy/decisions:
 * <ul>
 * <li>Do <b>not</b> rewrite a single inequality {@code ?p != <iri>} into {@code ?p NOT IN (<iri>)}. Only reconstruct
 * NOT IN when multiple {@code !=} terms share the same variable.</li>
 * <li>Do <b>not</b> fuse {@code ?s ?p ?o . FILTER (?p != <iri>)} into a negated path {@code ?s !(<iri>) ?o}.</li>
 * <li>Use {@code a} for {@code rdf:type} consistently, incl. inside property lists.</li>
 * </ul>
 *
 * Naming hints from the RDF4J parser:
 * <ul>
 * <li>{@code _anon_path_*}: anonymous intermediate variables introduced when parsing property paths. Transforms only
 * compose chains across these bridge variables to avoid altering user bindings.</li>
 * <li>{@code _anon_having_*}: marks variables synthesized for HAVING extraction.</li>
 * <li>{@code _anon_bnode_*}: placeholder variables for [] that should render as an empty blank node.</li>
 * </ul>
 */
@Experimental
public class TupleExprIRRenderer {

	// ---------------- Public API helpers ----------------

	private static final String FN_NS = "http://www.w3.org/2005/xpath-functions#";
	/** Map of function identifier (either bare name or full IRI) → SPARQL built-in name. */
	private static final Map<String, String> BUILTIN;
	// ---- Naming hints provided by the parser ----

	// ---------------- Configuration ----------------
	/** Anonymous blank node variables (originating from [] in the original query). */
	private static final String ANON_BNODE_PREFIX = "_anon_bnode_";
	// Pattern used for conservative Turtle PN_LOCAL acceptance per segment; overall check also prohibits trailing dots.
	private static final Pattern PN_LOCAL_CHUNK = Pattern.compile("(?:%[0-9A-Fa-f]{2}|[-\\p{L}\\p{N}_\\u00B7]|:)+");

	static {
		Map<String, String> m = new HashMap<>();

		// --- XPath/XQuery function IRIs → SPARQL built-ins ---
		m.put(FN_NS + "string-length", "STRLEN");
		m.put(FN_NS + "lower-case", "LCASE");
		m.put(FN_NS + "upper-case", "UCASE");
		m.put(FN_NS + "substring", "SUBSTR");
		m.put(FN_NS + "contains", "CONTAINS");
		m.put(FN_NS + "concat", "CONCAT");
		m.put(FN_NS + "replace", "REPLACE");
		m.put(FN_NS + "encode-for-uri", "ENCODE_FOR_URI");
		m.put(FN_NS + "starts-with", "STRSTARTS");
		m.put(FN_NS + "ends-with", "STRENDS");

		m.put(FN_NS + "numeric-abs", "ABS");
		m.put(FN_NS + "numeric-ceil", "CEIL");
		m.put(FN_NS + "numeric-floor", "FLOOR");
		m.put(FN_NS + "numeric-round", "ROUND");

		m.put(FN_NS + "year-from-dateTime", "YEAR");
		m.put(FN_NS + "month-from-dateTime", "MONTH");
		m.put(FN_NS + "day-from-dateTime", "DAY");
		m.put(FN_NS + "hours-from-dateTime", "HOURS");
		m.put(FN_NS + "minutes-from-dateTime", "MINUTES");
		m.put(FN_NS + "seconds-from-dateTime", "SECONDS");
		m.put(FN_NS + "timezone-from-dateTime", "TIMEZONE");

		// --- Bare SPARQL built-ins RDF4J may surface as "URIs" ---
		for (String k : new String[] {
				"RAND", "NOW",
				"ABS", "CEIL", "FLOOR", "ROUND",
				"YEAR", "MONTH", "DAY", "HOURS", "MINUTES", "SECONDS", "TZ", "TIMEZONE",
				"MD5", "SHA1", "SHA224", "SHA256", "SHA384", "SHA512",
				"UCASE", "LCASE", "SUBSTR", "STRLEN", "CONTAINS", "CONCAT", "REPLACE", "ENCODE_FOR_URI",
				"STRSTARTS", "STRENDS", "STRBEFORE", "STRAFTER",
				"REGEX",
				"UUID", "STRUUID",
				"STRDT", "STRLANG", "BNODE",
				"URI" // alias -> IRI
		}) {
			m.put(k, k);
		}

		BUILTIN = Collections.unmodifiableMap(m);
	}

	private final Config cfg;
	private final PrefixIndex prefixIndex;
	// Overrides collected during IR transforms (e.g., collections) to affect term rendering in IR printer
	private final Map<String, String> irOverrides = new HashMap<>();
	// Legacy suppression tracking removed; IR transforms rewrite structures directly in-place.

	public TupleExprIRRenderer() {
		this(new Config());
	}

	public TupleExprIRRenderer(final Config cfg) {
		this.cfg = cfg == null ? new Config() : cfg;
		this.prefixIndex = new PrefixIndex(this.cfg.prefixes);
	}

	/** Identify anonymous blank-node placeholder variables (to render as "[]"). */
	private static boolean isAnonBNodeVar(Var v) {
		if (v == null || v.hasValue()) {
			return false;
		}
		final String name = v.getName();
		if (name == null || !name.startsWith(ANON_BNODE_PREFIX)) {
			return false;
		}
		// Prefer Var#isAnonymous() when present; fall back to prefix heuristic
		try {
			Method m = Var.class.getMethod("isAnonymous");
			Object r = m.invoke(v);
			if (r instanceof Boolean) {
				return (Boolean) r;
			}
		} catch (ReflectiveOperationException ignore) {
		}
		return true;
	}

	// ---------------- Experimental textual IR API ----------------

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

	private static String mathOp(final MathOp op) {
		if (op == MathOp.PLUS) {
			return "+";
		}
		if (op == MathOp.MINUS) {
			return "-";
		}
		try {
			if (op.name().equals("MULTIPLY") || op.name().equals("TIMES")) {
				return "*";
			}
		} catch (Throwable ignore) {
		}
		if (op == MathOp.DIVIDE) {
			return "/";
		}
		return "?";
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

	// ---------------- Core SELECT and subselect ----------------

	/**
	 * Context compatibility: equal if both null; if both values -> same value; if both free vars -> same name; else
	 * incompatible.
	 */

	public static String stripRedundantOuterParens(final String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		if (t.length() >= 2 && t.charAt(0) == '(' && t.charAt(t.length() - 1) == ')') {
			int depth = 0;
			for (int i = 0; i < t.length(); i++) {
				char ch = t.charAt(i);
				if (ch == '(') {
					depth++;
				} else if (ch == ')') {
					depth--;
				}
				if (depth == 0 && i < t.length() - 1) {
					return t;
				}
			}
			return t.substring(1, t.length() - 1).trim();
		}
		return t;
	}

	// ---------------- Normalization shell ----------------

	/**
	 * Ensure a text snippet is valid as a SPARQL Constraint (used in FILTER/HAVING). If it already looks like a
	 * function/built-in call (e.g., isIRI(?x), REGEX(...), EXISTS { ... }), or is already bracketted, it is returned as
	 * is. Otherwise, wrap it in parentheses.
	 */
	public static String asConstraint(final String s) {
		if (s == null) {
			return "()";
		}
		final String t = s.trim();
		if (t.isEmpty()) {
			return "()";
		}
		// Already parenthesized and spanning full expression
		if (t.charAt(0) == '(' && t.charAt(t.length() - 1) == ')') {
			int depth = 0;
			for (int i = 0; i < t.length(); i++) {
				char ch = t.charAt(i);
				if (ch == '(') {
					depth++;
				} else if (ch == ')') {
					depth--;
				}
				if (depth == 0 && i < t.length() - 1) {
					// closing too early -> not a single outer pair
					break;
				}
				if (i == t.length() - 1 && depth == 0) {
					return t; // single outer pair spans whole string
				}
			}
		}

		// EXISTS / NOT EXISTS { ... }
		if (t.startsWith("EXISTS ") || t.startsWith("NOT EXISTS ")) {
			return t;
		}

		// Function/built-in-like call: head(...) with no whitespace in head
		int lpar = t.indexOf('(');
		if (lpar > 0 && t.endsWith(")")) {
			String head = t.substring(0, lpar).trim();
			if (!head.isEmpty() && head.indexOf(' ') < 0) {
				return t;
			}
		}

		// Otherwise, bracket to form a valid Constraint
		return "(" + t + ")";
	}

	/**
	 * Decide if an expression should be wrapped in parentheses and return either the original expression or a
	 * parenthesized version. Heuristic: if the expression already has surrounding parentheses or looks like a
	 * simple/atomic term (variable, IRI, literal, number, or function call), we omit additional parentheses. Otherwise
	 * we wrap the expression.
	 */
	public static String parenthesizeIfNeeded(final String expr) {
		if (expr == null) {
			return "()";
		}
		final String t = expr.trim();
		if (t.isEmpty()) {
			return "()";
		}
		// Already parenthesized: keep as-is if the outer pair spans the full expression
		if (t.charAt(0) == '(' && t.charAt(t.length() - 1) == ')') {
			int depth = 0;
			boolean spans = true;
			for (int i = 0; i < t.length(); i++) {
				char ch = t.charAt(i);
				if (ch == '(') {
					depth++;
				} else if (ch == ')') {
					depth--;
				}
				if (depth == 0 && i < t.length() - 1) {
					spans = false;
					break;
				}
			}
			if (spans) {
				return t;
			}
		}

		// Atomic checks
		// 1) Variable like ?x (no whitespace)
		if (t.charAt(0) == '?') {
			boolean ok = true;
			for (int i = 1; i < t.length(); i++) {
				char c = t.charAt(i);
				if (!(Character.isLetterOrDigit(c) || c == '_')) {
					ok = false;
					break;
				}
			}
			if (ok) {
				return t;
			}
		}
		// 2) Angle-bracketed IRI (no spaces)
		if (t.charAt(0) == '<' && t.endsWith(">") && t.indexOf(' ') < 0) {
			return t;
		}
		// 3) Prefixed name like ex:knows (no whitespace, no parens)
		int colon = t.indexOf(':');
		if (colon > 0 && t.indexOf(' ') < 0 && t.indexOf('(') < 0 && t.indexOf(')') < 0) {
			return t;
		}
		// 4) Literal (very rough: starts with quote)
		if (t.charAt(0) == '"') {
			return t;
		}
		// 5) Numeric literal (rough)
		if (looksLikeNumericLiteral(t)) {
			return t;
		}
		// 6) Function/built-in-like call: head(...) with no whitespace in head
		int lpar = t.indexOf('(');
		if (lpar > 0 && t.endsWith(")")) {
			String head = t.substring(0, lpar);
			boolean ok = head.indexOf(' ') < 0;
			if (ok) {
				return t;
			}
		}

		// Otherwise, wrap
		return "(" + t + ")";
	}

	private static boolean looksLikeNumericLiteral(final String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		int i = 0;
		if (s.charAt(0) == '+' || s.charAt(0) == '-') {
			i = 1;
			if (s.length() == 1) {
				return false;
			}
		}
		boolean hasDigit = false;
		for (; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isDigit(c)) {
				hasDigit = true;
				continue;
			}
			if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
				continue;
			}
			return false;
		}
		return hasDigit;
	}

	// ---------------- Aggregate hoisting & inference ----------------

	// Removed invertNegatedPropertySet here; transforms use BaseTransform.invertNegatedPropertySet.

	// ---------------- Utilities: vars, aggregates, free vars ----------------

	// Merge adjacent identical GRAPH blocks to improve grouping when IR emits across passes
	private static String mergeAdjacentGraphBlocks(final String s) {
		String prev;
		String cur = s;
		final Pattern p = Pattern.compile(
				"GRAPH\\s+([^\\s]+)\\s*\\{\\s*([\\s\\S]*?)\\s*}\\s*GRAPH\\s+\\1\\s*\\{\\s*([\\s\\S]*?)\\s*}",
				Pattern.MULTILINE);
		int guard = 0;
		do {
			prev = cur;
			cur = p.matcher(prev).replaceFirst("GRAPH $1 {\n$2\n$3\n}");
			guard++;
		} while (!cur.equals(prev) && guard < 50);
		return cur;
	}

	// Package-private accessors for the converter
	Config getConfig() {
		return cfg;
	}

	// ---------------- Block/Node printer ----------------

	String renderExprPublic(final ValueExpr e) {
		return renderExpr(e);
	}

	String renderVarOrValuePublic(final Var v) {
		return renderVarOrValue(v);
	}

	String renderValuePublic(final Value v) {
		return renderValue(v);
	}

	public void addOverrides(Map<String, String> overrides) {
		if (overrides != null && !overrides.isEmpty()) {
			this.irOverrides.putAll(overrides);
		}
	}

	/**
	 * Build a best‑effort textual IR for a SELECT‑form query.
	 *
	 * Steps:
	 * <ol>
	 * <li>Normalize the TupleExpr (gather LIMIT/OFFSET/ORDER, peel wrappers, detect HAVING candidates).</li>
	 * <li>Translate the remaining WHERE tree into an IR block ({@link IrBGP}) with simple, explicit nodes (statement
	 * patterns, path triples, filters, graphs, unions, etc.).</li>
	 * <li>Apply the ordered IR transform pipeline ({@link IrTransforms#transformUsingChildren}) to perform
	 * purely-textual best‑effort fusions (paths, NPS, collections, property lists) while preserving user variable
	 * bindings.</li>
	 * <li>Populate IR header sections (projection, group by, having, order by) from normalized metadata.</li>
	 * </ol>
	 *
	 * The method intentionally keeps TupleExpr → IR logic simple; most nontrivial decisions live in transform passes
	 * for clarity and testability.
	 */
	public IrSelect toIRSelect(final TupleExpr tupleExpr) {
		return new TupleExprToIrConverter(this).toIRSelect(tupleExpr);
	}

	/** Render a textual SELECT query from an {@code IrSelect} model. */

	public String render(final IrSelect ir,
			final DatasetView dataset) {
		return render(ir, dataset, false);
	}

	// ---------------- Rendering helpers (prefix-aware) ----------------

	public String render(final IrSelect ir,
			final DatasetView dataset, final boolean subselect) {
		final StringBuilder out = new StringBuilder(256);
		if (!subselect) {
			printPrologueAndDataset(out, dataset);
		}
		// SELECT header
		out.append("SELECT ");
		if (ir.isDistinct()) {
			out.append("DISTINCT ");
		} else if (ir.isReduced()) {
			out.append("REDUCED ");
		}
		if (ir.getProjection().isEmpty()) {
			out.append("*");
		} else {
			for (int i = 0; i < ir.getProjection().size(); i++) {
				final IrProjectionItem it = ir.getProjection().get(i);
				if (it.getExprText() == null) {
					out.append('?').append(it.getVarName());
				} else {
					out.append('(').append(it.getExprText()).append(" AS ?").append(it.getVarName()).append(')');
				}
				if (i + 1 < ir.getProjection().size()) {
					out.append(' ');
				}
			}
		}

		// WHERE block
		out.append(cfg.canonicalWhitespace ? " WHERE " : " WHERE ");
		new IRTextPrinter(out).printWhere(ir.getWhere());

		// GROUP BY
		if (!ir.getGroupBy().isEmpty()) {
			if (out.length() == 0 || out.charAt(out.length() - 1) != '\n') {
				out.append('\n');
			}
			out.append("GROUP BY");
			for (IrGroupByElem g : ir.getGroupBy()) {
				if (g.getExprText() == null) {
					out.append(' ').append('?').append(g.getVarName());
				} else {
					out.append(" (").append(g.getExprText()).append(" AS ?").append(g.getVarName()).append(")");
				}
			}
		}

		// HAVING
		if (!ir.getHaving().isEmpty()) {
			if (out.length() == 0 || out.charAt(out.length() - 1) != '\n') {
				out.append('\n');
			}
			out.append("HAVING");
			for (String cond : ir.getHaving()) {
				out.append(' ').append(asConstraint(cond));
			}
		}

		// ORDER BY
		if (!ir.getOrderBy().isEmpty()) {
			if (out.length() == 0 || out.charAt(out.length() - 1) != '\n') {
				out.append('\n');
			}
			out.append("ORDER BY");
			for (IrOrderSpec o : ir.getOrderBy()) {
				if (o.isAscending()) {
					out.append(' ').append(o.getExprText());
				} else {
					out.append(" DESC(").append(o.getExprText()).append(')');
				}
			}
		}

		if (ir.getLimit() >= 0) {
			if (out.length() == 0 || out.charAt(out.length() - 1) != '\n') {
				out.append('\n');
			}
			out.append("LIMIT ").append(ir.getLimit());
		}
		if (ir.getOffset() >= 0) {
			if (out.length() == 0 || out.charAt(out.length() - 1) != '\n') {
				out.append('\n');
			}
			out.append("OFFSET ").append(ir.getOffset());
		}

		return mergeAdjacentGraphBlocks(out.toString()).trim();
	}

	/** Backward-compatible: render as SELECT query (no dataset). */
	public String render(final TupleExpr tupleExpr) {
		return renderSelectInternal(tupleExpr, RenderMode.TOP_LEVEL_SELECT, null);
	}

	/** SELECT with dataset (FROM/FROM NAMED). */
	public String render(final TupleExpr tupleExpr, final DatasetView dataset) {
		return renderSelectInternal(tupleExpr, RenderMode.TOP_LEVEL_SELECT, dataset);
	}

	/** ASK query (top-level). */
	public String renderAsk(final TupleExpr tupleExpr, final DatasetView dataset) {
		// Build IR (including transforms) and then print only the WHERE block using the IR printer.
		final StringBuilder out = new StringBuilder(256);
		final IrSelect ir = toIRSelect(tupleExpr);
		// Prologue
		printPrologueAndDataset(out, dataset);
		out.append("ASK");
		// WHERE (from IR)
		out.append(cfg.canonicalWhitespace ? "\nWHERE " : " WHERE ");
		new IRTextPrinter(out).printWhere(ir.getWhere());
		return mergeAdjacentGraphBlocks(out.toString()).trim();
	}

	private String renderSelectInternal(final TupleExpr tupleExpr,
			final RenderMode mode,
			final DatasetView dataset) {
		final IrSelect ir = toIRSelect(tupleExpr);
		final boolean asSub = (mode == RenderMode.SUBSELECT);
		return render(ir, dataset, asSub);
	}

	private void printPrologueAndDataset(final StringBuilder out, final DatasetView dataset) {
		if (cfg.printPrefixes && !cfg.prefixes.isEmpty()) {
			cfg.prefixes.forEach((pfx, ns) -> out.append("PREFIX ").append(pfx).append(": <").append(ns).append(">\n"));
		}
		// FROM / FROM NAMED (top-level only)
		final List<IRI> dgs = dataset != null ? dataset.defaultGraphs : cfg.defaultGraphs;
		final List<IRI> ngs = dataset != null ? dataset.namedGraphs : cfg.namedGraphs;
		for (IRI iri : dgs) {
			out.append("FROM ").append(renderIRI(iri)).append("\n");
		}
		for (IRI iri : ngs) {
			out.append("FROM NAMED ").append(renderIRI(iri)).append("\n");
		}
	}

// Removed legacy suppression checks; transforms rewrite or remove structures directly.

	private String renderVarOrValue(final Var v) {
		if (v == null) {
			return "?_";
		}
		if (v.hasValue()) {
			return renderValue(v.getValue());
		}
		// Anonymous blank-node placeholder variables are rendered as "[]"
		if (isAnonBNodeVar(v)) {
			return "[]";
		}
		return "?" + v.getName();
	}

	private String renderPredicateForTriple(final Var p) {
		if (p != null && p.hasValue() && p.getValue() instanceof IRI && RDF.TYPE.equals(p.getValue())) {
			return "a";
		}
		return renderVarOrValue(p);
	}

	public String renderValue(final Value val) {
		if (val instanceof IRI) {
			return renderIRI((IRI) val);
		} else if (val instanceof Literal) {
			final Literal lit = (Literal) val;

			// Language-tagged strings: always quoted@lang
			if (lit.getLanguage().isPresent()) {
				return "\"" + escapeLiteral(lit.getLabel()) + "\"@" + lit.getLanguage().get();
			}

			final IRI dt = lit.getDatatype();
			final String label = lit.getLabel();

			// Canonical tokens for core datatypes
			if (XSD.BOOLEAN.equals(dt)) {
				return ("1".equals(label) || "true".equalsIgnoreCase(label)) ? "true" : "false";
			}
			if (XSD.INTEGER.equals(dt)) {
				try {
					return new BigInteger(label).toString();
				} catch (NumberFormatException ignore) {
				}
			}
			if (XSD.DECIMAL.equals(dt)) {
				try {
					return new BigDecimal(label).toPlainString();
				} catch (NumberFormatException ignore) {
				}
			}

			// Other datatypes
			if (dt != null && !XSD.STRING.equals(dt)) {
				return "\"" + escapeLiteral(label) + "\"^^" + renderIRI(dt);
			}

			// Plain string
			return "\"" + escapeLiteral(label) + "\"";
		} else if (val instanceof BNode) {
			return "_:" + ((BNode) val).getID();
		}
		return "\"" + escapeLiteral(String.valueOf(val)) + "\"";
	}

	// ---- Aggregates ----

	public String renderIRI(final IRI iri) {
		final String s = iri.stringValue();
		if (cfg.usePrefixCompaction) {
			final PrefixHit hit = prefixIndex.longestMatch(s);
			if (hit != null) {
				final String local = s.substring(hit.namespace.length());
				if (isPN_LOCAL(local)) {
					return hit.prefix + ":" + local;
				}
			}
		}
		return "<" + s + ">";
	}

	private boolean isPN_LOCAL(final String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		if (s.charAt(s.length() - 1) == '.') {
			return false; // no trailing dot
		}
		// Must start with PN_CHARS_U | ':' | [0-9]
		char first = s.charAt(0);
		if (!(first == ':' || Character.isLetter(first) || first == '_' || Character.isDigit(first))) {
			return false;
		}
		// All chunks must be acceptable; dots allowed between chunks
		int i = 0;
		boolean needChunk = true;
		while (i < s.length()) {
			int j = i;
			while (j < s.length() && s.charAt(j) != '.') {
				j++;
			}
			String chunk = s.substring(i, j);
			if (needChunk && chunk.isEmpty()) {
				return false;
			}
			if (!chunk.isEmpty() && !PN_LOCAL_CHUNK.matcher(chunk).matches()) {
				return false;
			}
			i = j + 1; // skip dot (if any)
			needChunk = false;
		}
		return true;
	}

	/** Expression renderer with aggregate + functional-form support. */
	private String renderExpr(final ValueExpr e) {
		if (e == null) {
			return "()";
		}

		// Aggregates
		if (e instanceof AggregateOperator) {
			return renderAggregate((AggregateOperator) e);
		}

		// Special NOT handling
		if (e instanceof Not) {
			final ValueExpr a = ((Not) e).getArg();
			if (a instanceof Exists) {
				return "NOT " + renderExists((Exists) a);
			}
			if (a instanceof ListMemberOperator) {
				return renderIn((ListMemberOperator) a, true); // NOT IN
			}
			final String inner = stripRedundantOuterParens(renderExpr(a));
			return "!" + parenthesizeIfNeeded(inner);
		}

		// Vars and constants
		if (e instanceof Var) {
			final Var v = (Var) e;
			return v.hasValue() ? renderValue(v.getValue()) : "?" + v.getName();
		}
		if (e instanceof ValueConstant) {
			return renderValue(((ValueConstant) e).getValue());
		}

		// Functional forms
		if (e instanceof If) {
			final If iff = (If) e;
			return "IF(" + renderExpr(iff.getCondition()) + ", " + renderExpr(iff.getResult()) + ", " +
					renderExpr(iff.getAlternative()) + ")";
		}
		if (e instanceof Coalesce) {
			final List<ValueExpr> args = ((Coalesce) e).getArguments();
			final String s = args.stream().map(this::renderExpr).collect(Collectors.joining(", "));
			return "COALESCE(" + s + ")";
		}
		if (e instanceof IRIFunction) {
			return "IRI(" + renderExpr(((IRIFunction) e).getArg()) + ")";
		}
		if (e instanceof IsNumeric) {
			return "isNumeric(" + renderExpr(((IsNumeric) e).getArg()) + ")";
		}

		// EXISTS
		if (e instanceof Exists) {
			return renderExists((Exists) e);
		}

		// IN list
		if (e instanceof ListMemberOperator) {
			return renderIn((ListMemberOperator) e, false);
		}

		// Unary basics
		if (e instanceof Str) {
			return "STR(" + renderExpr(((Str) e).getArg()) + ")";
		}
		if (e instanceof Datatype) {
			return "DATATYPE(" + renderExpr(((Datatype) e).getArg()) + ")";
		}
		if (e instanceof Lang) {
			return "LANG(" + renderExpr(((Lang) e).getArg()) + ")";
		}
		if (e instanceof Bound) {
			return "BOUND(" + renderExpr(((Bound) e).getArg()) + ")";
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

		// Math expressions
		if (e instanceof MathExpr) {
			final MathExpr me = (MathExpr) e;
			// unary minus: (0 - x)
			if (me.getOperator() == MathOp.MINUS &&
					me.getLeftArg() instanceof ValueConstant &&
					((ValueConstant) me.getLeftArg()).getValue() instanceof Literal) {
				Literal l = (Literal) ((ValueConstant) me.getLeftArg()).getValue();
				if ("0".equals(l.getLabel())) {
					return "(-" + renderExpr(me.getRightArg()) + ")";
				}
			}
			return "(" + renderExpr(me.getLeftArg()) + " " + mathOp(me.getOperator()) + " " +
					renderExpr(me.getRightArg()) + ")";
		}

		// Binary/ternary
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
			return "(" + renderExpr(c.getLeftArg()) + " " + op(c.getOperator()) + " " +
					renderExpr(c.getRightArg()) + ")";
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

		// Function calls: map known bare names or IRIs to built-in names
		if (e instanceof FunctionCall) {
			final FunctionCall f = (FunctionCall) e;
			final String args = f.getArgs().stream().map(this::renderExpr).collect(Collectors.joining(", "));
			final String uri = f.getURI();
			String builtin = BUILTIN.get(uri);
			if (builtin == null && uri != null) {
				builtin = BUILTIN.get(uri.toUpperCase(Locale.ROOT));
			}
			if (builtin != null) {
				if ("URI".equals(builtin)) {
					return "IRI(" + args + ")";
				}
				return builtin + "(" + args + ")";
			}
			// Fallback: render as IRI call with prefix compaction if available
			if (uri != null) {
				try {
					IRI iri = SimpleValueFactory.getInstance()
							.createIRI(uri);
					return renderIRI(iri) + "(" + args + ")";
				} catch (IllegalArgumentException ignore) {
					// keep angle-bracketed IRI if parsing fails
					return "<" + uri + ">(" + args + ")";
				}
			}
			return "()"; // unreachable
		}

		// BNODE() / BNODE(<expr>)
		if (e instanceof BNodeGenerator) {
			final BNodeGenerator bg = (BNodeGenerator) e;
			final ValueExpr id = bg.getNodeIdExpr(); // may be null for BNODE()
			if (id == null) {
				return "BNODE()";
			}
			return "BNODE(" + renderExpr(id) + ")";
		}

		handleUnsupported("unsupported expr: " + e.getClass().getSimpleName());
		return ""; // unreachable in strict mode
	}

	// NOTE: NOT IN reconstruction moved into NormalizeFilterNotInTransform.

	/** EXISTS { ... } */
	private String renderExists(final Exists ex) {
		final String group = renderInlineGroup(ex.getSubQuery());
		return "EXISTS " + group;
	}

	/** Render (?x [NOT] IN (a, b, c)) from ListMemberOperator. */
	private String renderIn(final ListMemberOperator in, final boolean negate) {
		final List<ValueExpr> args = in.getArguments();
		if (args == null || args.isEmpty()) {
			return "/* invalid IN */";
		}
		final String left = renderExpr(args.get(0));
		final String rest = args.stream().skip(1).map(this::renderExpr).collect(Collectors.joining(", "));
		return "(" + left + (negate ? " NOT IN (" : " IN (") + rest + "))";
	}

	/** Render a TupleExpr group inline using IR + transforms (used by EXISTS). */
	private String renderInlineGroup(final TupleExpr pattern) {
		IrBGP where = new TupleExprToIrConverter(this).buildWhere(pattern);
		// Apply standard transforms for consistent property path and grouping rewrites
		IrSelect tmp = new IrSelect();
		tmp.setWhere(where);
		final IrSelect transformed = IrTransforms.transformUsingChildren(tmp, this);
		where = transformed.getWhere();

		final StringBuilder sb = new StringBuilder(64);
		new IRTextPrinter(sb).printWhere(where);
		return sb.toString().replace('\n', ' ').replaceAll("\\s+", " ").trim();
	}

	private String renderAggregate(final AggregateOperator op) {
		if (op instanceof Count) {
			final Count c = (Count) op;
			final String inner = (c.getArg() == null) ? "*" : renderExpr(c.getArg());
			return "COUNT(" + (c.isDistinct() && c.getArg() != null ? "DISTINCT " : "") + inner + ")";
		}
		if (op instanceof Sum) {
			final Sum a = (Sum) op;
			return "SUM(" + (a.isDistinct() ? "DISTINCT " : "") + renderExpr(a.getArg()) + ")";
		}
		if (op instanceof Avg) {
			final Avg a = (Avg) op;
			return "AVG(" + (a.isDistinct() ? "DISTINCT " : "") + renderExpr(a.getArg()) + ")";
		}
		if (op instanceof Min) {
			final Min a = (Min) op;
			return "MIN(" + (a.isDistinct() ? "DISTINCT " : "") + renderExpr(a.getArg()) + ")";
		}
		if (op instanceof Max) {
			final Max a = (Max) op;
			return "MAX(" + (a.isDistinct() ? "DISTINCT " : "") + renderExpr(a.getArg()) + ")";
		}
		if (op instanceof Sample) {
			final Sample a = (Sample) op;
			return "SAMPLE(" + (a.isDistinct() ? "DISTINCT " : "") + renderExpr(a.getArg()) + ")";
		}
		if (op instanceof GroupConcat) {
			final GroupConcat a = (GroupConcat) op;
			final StringBuilder sb = new StringBuilder();
			sb.append("GROUP_CONCAT(");
			if (a.isDistinct()) {
				sb.append("DISTINCT ");
			}
			sb.append(renderExpr(a.getArg()));
			final ValueExpr sepExpr = a.getSeparator();
			final String sepLex = extractSeparatorLiteral(sepExpr);
			if (sepLex != null) {
				sb.append("; SEPARATOR=").append('"').append(escapeLiteral(sepLex)).append('"');
			}
			sb.append(")");
			return sb.toString();
		}
		handleUnsupported("unsupported aggregate: " + op.getClass().getSimpleName());
		return "";
	}

	/** Returns the lexical form if the expr is a plain string literal; otherwise null. */
	private String extractSeparatorLiteral(final ValueExpr expr) {
		if (expr == null) {
			return null;
		}
		if (expr instanceof ValueConstant) {
			final Value v = ((ValueConstant) expr).getValue();
			if (v instanceof Literal) {
				Literal lit = (Literal) v;
				// Only accept plain strings / xsd:string (spec)
				IRI dt = lit.getDatatype();
				if (dt == null || XSD.STRING.equals(dt)) {
					return lit.getLabel();
				}
			}
			return null;
		}
		if (expr instanceof Var) {
			final Var var = (Var) expr;
			if (var.hasValue() && var.getValue() instanceof Literal) {
				Literal lit = (Literal) var.getValue();
				IRI dt = lit.getDatatype();
				if (dt == null || XSD.STRING.equals(dt)) {
					return lit.getLabel();
				}
			}
		}
		return null;
	}

	// Collections are handled by IR transforms (ApplyCollectionsTransform); no TupleExpr-time detection needed.

	private void handleUnsupported(String message) {
		if (cfg.strict) {
			throw new SparqlRenderingException(message);
		}
	}

	/** Rendering context: top-level query vs nested subselect. */
	private enum RenderMode {
		TOP_LEVEL_SELECT,
		SUBSELECT
	}

	/** Optional dataset input for FROM/FROM NAMED lines. */
	public static final class DatasetView {
		public final List<IRI> defaultGraphs = new ArrayList<>();
		public final List<IRI> namedGraphs = new ArrayList<>();

		public DatasetView addDefault(IRI iri) {
			if (iri != null) {
				defaultGraphs.add(iri);
			}
			return this;
		}

		public DatasetView addNamed(IRI iri) {
			if (iri != null) {
				namedGraphs.add(iri);
			}
			return this;
		}
	}

	/** Unchecked exception in strict mode. */
	public static final class SparqlRenderingException extends RuntimeException {
		public SparqlRenderingException(String msg) {
			super(msg);
		}
	}

	public static final class Config {
		public final String indent = "  ";
		public final boolean printPrefixes = true;
		public final boolean usePrefixCompaction = true;
		public final boolean canonicalWhitespace = true;
		public final LinkedHashMap<String, String> prefixes = new LinkedHashMap<>();
		// Flags
		public final boolean strict = true; // throw on unsupported
		// Optional dataset (top-level only) if you never pass a DatasetView at render().
		// These are rarely used, but offered for completeness.
		public final List<IRI> defaultGraphs = new ArrayList<>();
		public final List<IRI> namedGraphs = new ArrayList<>();
		public boolean debugIR = false; // print IR before and after transforms
		public boolean valuesPreserveOrder = false; // keep VALUES column order as given by BSA iteration
	}

	// Former CollectionResult/collection overrides are no longer needed; collection handling moved to IR transforms.

	private static final class PrefixHit {
		final String prefix;
		final String namespace;

		PrefixHit(final String prefix, final String namespace) {
			this.prefix = prefix;
			this.namespace = namespace;
		}
	}

	private static final class PrefixIndex {
		private final List<Entry<String, String>> entries;

		PrefixIndex(final Map<String, String> prefixes) {
			final List<Entry<String, String>> list = new ArrayList<>();
			if (prefixes != null) {
				list.addAll(prefixes.entrySet());
			}
			this.entries = Collections.unmodifiableList(list);
		}

		PrefixHit longestMatch(final String iri) {
			if (iri == null) {
				return null;
			}
			for (final Entry<String, String> e : entries) {
				final String ns = e.getValue();
				if (iri.startsWith(ns)) {
					return new PrefixHit(e.getKey(), ns);
				}
			}
			return null;
		}
	}

	/**
	 * Simple IR→text pretty‑printer using renderer helpers. Responsible only for layout/indentation and delegating
	 * term/IRI rendering back to the renderer; it does not perform structural rewrites (those happen in IR transforms).
	 */
	private final class IRTextPrinter implements IrPrinter {
		private final StringBuilder out;
		private final Map<String, String> currentOverrides = TupleExprIRRenderer.this.irOverrides;
		// Track anonymous bnode var usage and assign labels when a var is referenced more than once.
		private final Map<String, Integer> bnodeCounts = new LinkedHashMap<>();
		private final Map<String, String> bnodeLabels = new LinkedHashMap<>();
		private int level = 0;
		private boolean inlineActive = false;

		IRTextPrinter(StringBuilder out) {
			this.out = out;
		}

		public void printWhere(final IrBGP w) {
			if (w == null) {
				openBlock();
				closeBlock();
				return;
			}
			// Pre-scan to count anonymous bnode variables to decide when to print labels
			collectBnodeCounts(w);
			assignBnodeLabels();
			w.print(this);
		}

		private void bumpBnodeVar(Var v) {
			if (v == null || v.hasValue()) {
				return;
			}
			final String n = v.getName();
			if (n == null) {
				return;
			}
			if (!isAnonBNodeVar(v)) {
				return;
			}
			bnodeCounts.merge(n, 1, Integer::sum);
		}

		private void collectBnodeCounts(IrBGP w) {
			if (w == null) {
				return;
			}
			for (IrNode ln : w.getLines()) {
				if (ln instanceof IrStatementPattern) {
					IrStatementPattern sp = (IrStatementPattern) ln;
					bumpBnodeVar(sp.getSubject());
					bumpBnodeVar(sp.getObject());
				} else if (ln instanceof IrPropertyList) {
					IrPropertyList pl = (IrPropertyList) ln;
					bumpBnodeVar(pl.getSubject());
					for (IrPropertyList.Item it : pl.getItems()) {
						for (Var ov : it.getObjects()) {
							bumpBnodeVar(ov);
						}
					}
				} else if (ln instanceof IrBGP) {
					collectBnodeCounts((IrBGP) ln);
				} else if (ln instanceof IrGraph) {
					collectBnodeCounts(((IrGraph) ln).getWhere());
				} else if (ln instanceof IrOptional) {
					collectBnodeCounts(((IrOptional) ln).getWhere());
				} else if (ln instanceof IrMinus) {
					collectBnodeCounts(((IrMinus) ln).getWhere());
				} else if (ln instanceof IrUnion) {
					for (IrBGP b : ((IrUnion) ln).getBranches()) {
						collectBnodeCounts(b);
					}
				} else if (ln instanceof IrService) {
					collectBnodeCounts(((IrService) ln).getWhere());
				} else if (ln instanceof IrSubSelect) {
					// Do not descend into raw subselects for top-level bnode label decisions
				}
			}
		}

		private void assignBnodeLabels() {
			int idx = 1;
			for (Map.Entry<String, Integer> e : bnodeCounts.entrySet()) {
				if (e.getValue() != null && e.getValue() > 1) {
					bnodeLabels.put(e.getKey(), "b" + (idx++));
				}
			}
		}

		public void printLines(final List<IrNode> lines) {
			if (lines == null) {
				return;
			}
			for (IrNode n : lines) {
				printNodeViaIr(n);
			}
		}

		private void printNodeViaIr(final IrNode n) {
			n.print(this);
		}

		// Path/collection rewrites are handled by IR transforms; IRTextPrinter only prints IR.

		private String applyOverridesToText(final String termText, final Map<String, String> overrides) {
			if (termText == null) {
				return null;
			}
			if (overrides == null || overrides.isEmpty()) {
				return termText;
			}
			String out = termText;
			// First, whole-token replacement (exact match "?name")
			if (out.startsWith("?")) {
				final String name = out.substring(1);
				final String repl = overrides.get(name);
				if (repl != null) {
					out = repl;
				}
			}
			// Then, replace any embedded override tokens "?name" within the text.
			// Iterate to allow nested placeholders to expand in a few steps.
			for (int iter = 0; iter < 4; iter++) {
				boolean changed = false;
				for (Map.Entry<String, String> e : overrides.entrySet()) {
					final String needle = "?" + e.getKey();
					if (out.contains(needle)) {
						out = out.replace(needle, e.getValue());
						changed = true;
					}
				}
				if (!changed) {
					break;
				}
			}
			// Map any remaining anonymous bnode var tokens to either [] or a stable label using precomputed counts
			if (!bnodeCounts.isEmpty()) {
				for (Map.Entry<String, Integer> e : bnodeCounts.entrySet()) {
					final String needle = "?" + e.getKey();
					if (out.contains(needle)) {
						final String lbl = bnodeLabels.get(e.getKey());
						final String rep = (lbl != null) ? ("_:" + lbl) : "[]";
						out = out.replace(needle, rep);
					}
				}
			}
			return out;
		}

		@Override
		public String applyOverridesToText(final String termText) {
			return applyOverridesToText(termText, this.currentOverrides);
		}

		private String renderTermWithOverrides(final Var v, final Map<String, String> overrides) {
			if (v == null) {
				return "?_";
			}
			if (!v.hasValue() && v.getName() != null && overrides != null) {
				final String repl = overrides.get(v.getName());
				if (repl != null) {
					// Apply nested overrides inside the replacement text (e.g., collections inside brackets)
					return applyOverridesToText(repl, overrides);
				}
			}
			// Decide bnode rendering: if this is an anonymous bnode var referenced more than once, print a
			// stable blank node label to preserve linking; otherwise render as []
			if (isAnonBNodeVar(v)) {
				final String name = v.getName();
				final String lbl = bnodeLabels.get(name);
				if (lbl != null) {
					return "_:" + lbl;
				}
				return "[]";
			}
			return renderVarOrValue(v);
		}

		@Override
		public String renderTermWithOverrides(final Var v) {
			return renderTermWithOverrides(v, this.currentOverrides);
		}

		private void indent() {
			out.append(cfg.indent.repeat(Math.max(0, level)));
		}

		@Override
		public void startLine() {
			if (!inlineActive) {
				indent();
				inlineActive = true;
			}
		}

		@Override
		public void append(final String s) {
			if (!inlineActive) {
				// If appending at the start of a line, apply indentation first
				int len = out.length();
				if (len == 0 || out.charAt(len - 1) == '\n') {
					indent();
				}
			}
			out.append(s);
		}

		@Override
		public void endLine() {
			out.append('\n');
			inlineActive = false;
		}

		@Override
		public void line(String s) {
			if (inlineActive) {
				out.append(s).append('\n');
				inlineActive = false;
				return;
			}
			indent();
			out.append(s).append('\n');
		}

		@Override
		public void openBlock() {
			if (!inlineActive) {
				indent();
			}
			out.append('{').append('\n');
			level++;
			// Opening a block completes any inline header that preceded it (e.g., "OPTIONAL ")
			inlineActive = false;
		}

		@Override
		public void closeBlock() {
			level--;
			indent();
			out.append('}').append('\n');
		}

		@Override
		public void raw(final String s) {
			out.append(s);
		}

		@Override
		public void pushIndent() {
			level++;
		}

		@Override
		public void popIndent() {
			level--;
		}

		@Override
		public String renderVarOrValue(Var v) {
			return TupleExprIRRenderer.this.renderVarOrValue(v);
		}

		@Override
		public String renderPredicateForTriple(Var p) {
			return TupleExprIRRenderer.this.renderPredicateForTriple(p);
		}

		@Override
		public String renderIRI(IRI iri) {
			return TupleExprIRRenderer.this.renderIRI(iri);
		}

		@Override
		public String renderSubselect(IrSelect select) {
			return TupleExprIRRenderer.this.render(select, null, true);
		}
	}

}
