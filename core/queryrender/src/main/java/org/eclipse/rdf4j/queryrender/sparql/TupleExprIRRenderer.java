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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
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
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Coalesce;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBind;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGroupByElem;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOrderSpec;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPrinter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrProjectionItem;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrText;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrValues;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.IrDebug;
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
 * Policy/decisions:
 * <ul>
 * <li>Do <b>not</b> rewrite {@code ?p != <iri>} into {@code ?p NOT IN (<iri>)}.</li>
 * <li>Do <b>not</b> fuse {@code ?s ?p ?o . FILTER (?p != <iri>)} into a negated path {@code ?s !(<iri>) ?o}.</li>
 * <li>Use {@code a} for {@code rdf:type} consistently, incl. inside property lists.</li>
 * </ul>
 */
@Experimental
public class TupleExprIRRenderer {

	// ---------------- Public API helpers ----------------

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

	// ---------------- Configuration ----------------

	public static final class Config {
		public final String indent = "  ";
		public final boolean printPrefixes = true;
		public final boolean usePrefixCompaction = true;
		public final boolean canonicalWhitespace = true;
		public final LinkedHashMap<String, String> prefixes = new LinkedHashMap<>();
		public boolean debugIR = false; // print IR before and after transforms

		// Flags
		public final boolean strict = true; // throw on unsupported
		public boolean valuesPreserveOrder = false; // keep VALUES column order as given by BSA iteration

		// Optional dataset (top-level only) if you never pass a DatasetView at render().
		// These are rarely used, but offered for completeness.
		public final List<IRI> defaultGraphs = new ArrayList<>();
		public final List<IRI> namedGraphs = new ArrayList<>();
	}

	private final Config cfg;
	private final PrefixIndex prefixIndex;

	private static final String FN_NS = "http://www.w3.org/2005/xpath-functions#";

	/** Map of function identifier (either bare name or full IRI) → SPARQL built-in name. */
	private static final Map<String, String> BUILTIN;

	// ---- Naming hints provided by the parser ----
	private static final String ANON_COLLECTION_PREFIX = "_anon_collection_";
	private static final String ANON_PATH_PREFIX = "_anon_path_";
	private static final String ANON_HAVING_PREFIX = "_anon_having_";
	/** Anonymous blank node variables (originating from [] in the original query). */
	private static final String ANON_BNODE_PREFIX = "_anon_bnode_";

	private static boolean isAnonCollectionVar(Var v) {
		return v != null && !v.hasValue() && v.getName() != null && v.getName().startsWith(ANON_COLLECTION_PREFIX);
	}

	private static boolean isAnonPathVar(Var v) {
		return v != null && !v.hasValue() && v.getName() != null && v.getName().startsWith(ANON_PATH_PREFIX);
	}

	private static boolean isAnonHavingName(String name) {
		return name != null && name.startsWith(ANON_HAVING_PREFIX);
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

	public TupleExprIRRenderer() {
		this(new Config());
	}

	public TupleExprIRRenderer(final Config cfg) {
		this.cfg = cfg == null ? new Config() : cfg;
		this.prefixIndex = new PrefixIndex(this.cfg.prefixes);
	}

	// ---------------- Experimental textual IR API ----------------

	/**
	 * Build a best-effort textual IR for a SELECT-form query. The IR mirrors how the query looks textually (projection
	 * header, a list-like WHERE group, and trailing modifiers). This does not affect the normal rendering path; it is
	 * provided to consumers that prefer a structured representation.
	 */
	public IrSelect toIRSelect(final TupleExpr tupleExpr) {
		suppressedSubselects.clear();
		final Normalized n = normalize(tupleExpr);
		applyAggregateHoisting(n);
		final IrSelect ir = new IrSelect();
		ir.setDistinct(n.distinct);
		ir.setReduced(n.reduced);
		ir.setLimit(n.limit);
		ir.setOffset(n.offset);

		// Projection header
		if (n.projection != null && n.projection.getProjectionElemList() != null
				&& !n.projection.getProjectionElemList().getElements().isEmpty()) {
			for (ProjectionElem pe : n.projection.getProjectionElemList().getElements()) {
				final String alias = pe.getProjectionAlias().orElse(pe.getName());
				final ValueExpr expr = n.selectAssignments.get(alias);
				if (expr != null) {
					ir.getProjection()
							.add(new IrProjectionItem(renderExpr(expr), alias));
				} else {
					ir.getProjection().add(new IrProjectionItem(null, alias));
				}
			}
		} else if (!n.selectAssignments.isEmpty()) {
			// Synthesize: group-by vars first (if any), then explicit assignments
			if (!n.groupByTerms.isEmpty()) {
				for (GroupByTerm t : n.groupByTerms) {
					ir.getProjection()
							.add(new IrProjectionItem(null, t.var));
				}
			} else {
				for (String v : n.syntheticProjectVars) {
					ir.getProjection().add(new IrProjectionItem(null, v));
				}
			}
			for (Entry<String, ValueExpr> e : n.selectAssignments.entrySet()) {
				ir.getProjection()
						.add(new IrProjectionItem(renderExpr(e.getValue()),
								e.getKey()));
			}
		}

		// WHERE as textual-IR
		final IRBuilder builder = new IRBuilder();
		ir.setWhere(builder.build(n.where));

		if (cfg.debugIR) {
			System.out.println("# IR (raw)\n" + IrDebug.dump(ir));
		}

		// Transformations: use function-style child transforms on BGPs (paths/collections/etc.)
		final IrSelect irTransformed = IrTransforms
				.transformUsingChildren(ir, this);
		ir.setWhere(irTransformed.getWhere());

		if (cfg.debugIR) {
			System.out.println("# IR (transformed)\n" + IrDebug.dump(ir));
		}

		// GROUP BY
		for (GroupByTerm t : n.groupByTerms) {
			ir.getGroupBy()
					.add(new IrGroupByElem(
							t.expr == null ? null : renderExpr(t.expr), t.var));
		}

		// HAVING
		for (ValueExpr cond : n.havingConditions) {
			ir.getHaving().add(stripRedundantOuterParens(renderExprForHaving(cond, n)));
		}

		// ORDER BY
		for (OrderElem oe : n.orderBy) {
			ir.getOrderBy()
					.add(new IrOrderSpec(renderExpr(oe.getExpr()),
							oe.isAscending()));
		}

		return ir;
	}

	/** Build IrSelect without running IR transforms (used for nested subselects where we keep raw structure). */
	private IrSelect toIRSelectRaw(final TupleExpr tupleExpr) {
		suppressedSubselects.clear();
		final Normalized n = normalize(tupleExpr);
		applyAggregateHoisting(n);
		final IrSelect ir = new IrSelect();
		ir.setDistinct(n.distinct);
		ir.setReduced(n.reduced);
		ir.setLimit(n.limit);
		ir.setOffset(n.offset);

		if (n.projection != null && n.projection.getProjectionElemList() != null
				&& !n.projection.getProjectionElemList().getElements().isEmpty()) {
			for (ProjectionElem pe : n.projection.getProjectionElemList().getElements()) {
				final String alias = pe.getProjectionAlias().orElse(pe.getName());
				final ValueExpr expr = n.selectAssignments.get(alias);
				if (expr != null) {
					ir.getProjection()
							.add(new IrProjectionItem(renderExpr(expr), alias));
				} else {
					ir.getProjection().add(new IrProjectionItem(null, alias));
				}
			}
		} else if (!n.selectAssignments.isEmpty()) {
			if (!n.groupByTerms.isEmpty()) {
				for (GroupByTerm t : n.groupByTerms) {
					ir.getProjection()
							.add(new IrProjectionItem(null, t.var));
				}
			} else {
				for (String v : n.syntheticProjectVars) {
					ir.getProjection().add(new IrProjectionItem(null, v));
				}
			}
			for (Entry<String, ValueExpr> e : n.selectAssignments.entrySet()) {
				ir.getProjection()
						.add(new IrProjectionItem(renderExpr(e.getValue()),
								e.getKey()));
			}
		}

		final IRBuilder builder = new IRBuilder();
		ir.setWhere(builder.build(n.where));

		for (GroupByTerm t : n.groupByTerms) {
			ir.getGroupBy()
					.add(new IrGroupByElem(
							t.expr == null ? null : renderExpr(t.expr), t.var));
		}

		for (ValueExpr cond : n.havingConditions) {
			ir.getHaving().add(stripRedundantOuterParens(renderExprForHaving(cond, n)));
		}

		for (OrderElem oe : n.orderBy) {
			ir.getOrderBy()
					.add(new IrOrderSpec(renderExpr(oe.getExpr()),
							oe.isAscending()));
		}

		return ir;
	}

	/** Render a textual SELECT query from an {@code IrSelect} model. */

	public String render(final IrSelect ir,
			final DatasetView dataset) {
		return render(ir, dataset, false);
	}

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
		out.append(cfg.canonicalWhitespace ? "\nWHERE " : " WHERE ");
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
				out.append(" (").append(cond).append(")");
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

	/** Simple IR→text pretty-printer using renderer helpers. */
	private final class IRTextPrinter implements IrPrinter {
		private final StringBuilder out;
		private int level = 0;
		private final String indentUnit = cfg.indent;
		private final Map<String, String> currentOverrides = Collections.emptyMap();

		IRTextPrinter(StringBuilder out) {
			this.out = out;
		}

		public void printWhere(final IrBGP w) {
			if (w == null) {
				openBlock();
				closeBlock();
				return;
			}
			w.print(this);
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

		// (legacy printing-time fusions removed; transforms handle path/collection rewrites)

		private String applyOverridesToText(final String termText, final Map<String, String> overrides) {
			if (termText == null) {
				return termText;
			}
			if (overrides == null || overrides.isEmpty()) {
				return termText;
			}
			if (termText.startsWith("?")) {
				final String name = termText.substring(1);
				final String repl = overrides.get(name);
				if (repl != null) {
					return repl;
				}
			}
			return termText;
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
					return repl;
				}
			}
			return renderVarOrValue(v);
		}

		@Override
		public String renderTermWithOverrides(final Var v) {
			return renderTermWithOverrides(v, this.currentOverrides);
		}

		private void indent() {
			out.append(indentUnit.repeat(Math.max(0, level)));
		}

		@Override
		public void line(String s) {
			indent();
			out.append(s).append('\n');
		}

		@Override
		public void openBlock() {
			out.append('{').append('\n');
			level++;
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

	/** Build a linear textual-IR for a TupleExpr WHERE tree (best effort). */
	private final class IRBuilder extends AbstractQueryModelVisitor<RuntimeException> {
		private final IrBGP where = new IrBGP();

		IrBGP build(final TupleExpr t) {
			if (t != null) {
				t.visit(this);
			}
			return where;
		}

		@Override
		public void meet(final StatementPattern sp) {
			final Var ctx = getContextVarSafe(sp);
			final IrStatementPattern node = new IrStatementPattern(
					sp.getSubjectVar(), sp.getPredicateVar(),
					sp.getObjectVar());
			if (ctx != null && (ctx.hasValue() || (ctx.getName() != null && !ctx.getName().isEmpty()))) {
				IrBGP inner = new IrBGP();
				inner.add(node);
				where.add(new IrGraph(ctx, inner));
			} else {
				where.add(node);
			}
		}

		@Override
		public void meet(final Join join) {
			join.getLeftArg().visit(this);
			join.getRightArg().visit(this);
		}

		@Override
		public void meet(final LeftJoin lj) {
			lj.getLeftArg().visit(this);
			final IRBuilder rightBuilder = new IRBuilder();
			final IrBGP right = rightBuilder.build(lj.getRightArg());
			if (lj.getCondition() != null) {
				final String cond = stripRedundantOuterParens(renderExpr(lj.getCondition()));
				right.add(new IrFilter(cond));
			}
			where.add(new IrOptional(right));
		}

		@Override
		public void meet(final Filter f) {
			// Try to order FILTER before a trailing subselect when the condition only mentions
			// variables already bound by the head of the join (to match expected formatting).
			final TupleExpr arg = f.getArg();
			Projection trailingProj = null;
			List<TupleExpr> head = null;
			if (arg instanceof Join) {
				final List<TupleExpr> flat = new ArrayList<>();
				TupleExprIRRenderer.flattenJoin(arg, flat);
				if (!flat.isEmpty()) {
					TupleExpr last = flat.get(flat.size() - 1);
					// recognize Distinct->Projection or plain Projection
					if (last instanceof Projection) {
						trailingProj = (Projection) last;
					} else if (last instanceof Distinct && ((Distinct) last).getArg() instanceof Projection) {
						trailingProj = (Projection) ((Distinct) last).getArg();
					}
					if (trailingProj != null) {
						head = new ArrayList<>(flat);
						head.remove(head.size() - 1);
					}
				}
			}

			if (trailingProj != null) {
				final Set<String> headVars = new LinkedHashSet<>();
				for (TupleExpr n : head) {
					collectFreeVars(n, headVars);
				}
				final Set<String> condVars = freeVars(f.getCondition());
				if (headVars.containsAll(condVars)) {
					// Emit head, then FILTER, then subselect
					for (TupleExpr n : head) {
						n.visit(this);
					}
					final String cond = stripRedundantOuterParens(renderExpr(f.getCondition()));
					where.add(new IrFilter(cond));
					trailingProj.visit(this);
					return;
				}
			}

			// Default order: argument followed by the FILTER line
			arg.visit(this);
			final String cond = stripRedundantOuterParens(renderExpr(f.getCondition()));
			where.add(new IrFilter(cond));
		}

		@Override
		public void meet(final Union u) {
			// Heuristic: if both operands are UNIONs, preserve grouping as two top-level branches
			// each of which may contain its own inner UNION. Otherwise, flatten the UNION chain
			// into a single IrUnion with N simple branches.
			final boolean leftIsU = u.getLeftArg() instanceof Union;
			final boolean rightIsU = u.getRightArg() instanceof Union;
			if (leftIsU && rightIsU) {
				final IrUnion irU = new IrUnion();
				IRBuilder left = new IRBuilder();
				irU.addBranch(left.build(u.getLeftArg()));
				IRBuilder right = new IRBuilder();
				irU.addBranch(right.build(u.getRightArg()));
				where.add(irU);
				return;
			}

			final List<TupleExpr> branches = new ArrayList<>();
			flattenUnion(u, branches);
			final IrUnion irU = new IrUnion();
			for (TupleExpr b : branches) {
				IRBuilder bld = new IRBuilder();
				irU.addBranch(bld.build(b));
			}
			where.add(irU);
		}

		@Override
		public void meet(final Service svc) {
			IRBuilder inner = new IRBuilder();
			IrBGP w = inner.build(svc.getArg());
			where.add(new IrService(renderVarOrValue(svc.getServiceRef()),
					svc.isSilent(), w));
		}

		@Override
		public void meet(final BindingSetAssignment bsa) {
			IrValues v = new IrValues();
			List<String> names = new ArrayList<>(bsa.getBindingNames());
			if (!cfg.valuesPreserveOrder) {
				Collections.sort(names);
			}
			v.getVarNames().addAll(names);
			for (BindingSet bs : bsa.getBindingSets()) {
				List<String> row = new ArrayList<>(names.size());
				for (String nm : names) {
					Value val = bs.getValue(nm);
					row.add(val == null ? "UNDEF" : renderValue(val));
				}
				v.getRows().add(row);
			}
			where.add(v);
		}

		@Override
		public void meet(final Extension ext) {
			ext.getArg().visit(this);
			for (ExtensionElem ee : ext.getElements()) {
				final ValueExpr expr = ee.getExpr();
				if (expr instanceof AggregateOperator) {
					continue; // hoisted to SELECT
				}
				where.add(new IrBind(renderExpr(expr), ee.getName()));
			}
		}

		@Override
		public void meet(final Projection p) {
			// Try RDF4J's zero-or-one path subselect expansion (simple IRI case)
			ZeroOrOneDirect z1 = parseZeroOrOneProjectionDirect(p);
			if (z1 != null) {
				final String s = renderVarOrValue(z1.start);
				final String o = renderVarOrValue(z1.end);
				final PathNode q = new PathQuant(new PathAtom(z1.pred, false), 0, 1);
				final String expr = q.render();
				where.add(new IrPathTriple(s, expr, o));
				return;
			}

			// Try a more general zero-or-one path expansion where the non-zero-length branch is a
			// chain/sequence of constant IRI steps (ex:knows/foaf:knows)? represented as a JOIN of
			// StatementPatterns. We detect: SELECT ?s ?o WHERE { { FILTER sameTerm(?s,?o) } UNION { chain } }
			// and convert to a single IrPathTriple with a "?" quantifier on the sequence.
			if (tryParseZeroOrOneSequenceProjection(p)) {
				return;
			}

			// Nested subselect: convert to typed IR without applying transforms
			IrSelect sub = toIRSelectRaw(p);
			where.add(new IrSubSelect(sub));
		}

		// Attempt to parse a complex zero-or-one over one or more non-zero branches (alternation),
		// where each branch is a chain/sequence of constant IRI steps (possibly mixed with inverse
		// direction). The Projection is expected to have a Union of a ZeroLengthPath and one or
		// more non-zero branches. Each non-zero branch is parsed into a PathNode sequence and
		// then alternated; finally a zero-or-one quantifier is applied.
		private boolean tryParseZeroOrOneSequenceProjection(Projection proj) {
			TupleExpr arg = proj.getArg();
			List<TupleExpr> leaves = new ArrayList<>();
			flattenUnion(arg, leaves);
			// Expect at least two leaves: one ZeroLengthPath and >=1 non-zero branch
			if (leaves.size() < 2) {
				return false;
			}
			ZeroLengthPath zlp = null;
			List<TupleExpr> nonZero = new ArrayList<>();
			for (TupleExpr leaf : leaves) {
				if (leaf instanceof ZeroLengthPath) {
					if (zlp != null) {
						return false; // more than one zero-length branch -> bail out
					}
					zlp = (ZeroLengthPath) leaf;
				} else {
					nonZero.add(leaf);
				}
			}
			if (zlp == null || nonZero.isEmpty()) {
				return false;
			}
			Var s = zlp.getSubjectVar();
			Var o = zlp.getObjectVar();
			if (s == null || o == null) {
				return false;
			}
			// Build PathNode for each non-zero branch
			List<PathNode> alts = new ArrayList<>();
			for (TupleExpr branch : nonZero) {
				PathNode seq = buildPathSequenceFromChain(branch, s, o);
				if (seq == null) {
					return false; // give up if any branch is not a simple chain of constant IRI steps
				}
				alts.add(seq);
			}
			// Combine alternatives (if more than one)
			PathNode inner = (alts.size() == 1) ? alts.get(0) : new PathAlt(alts);
			PathNode q = new PathQuant(inner, 0, 1);
			String sTxt = renderVarOrValue(s);
			String oTxt = renderVarOrValue(o);
			String expr = (q.prec() < PREC_SEQ ? "(" + q.render() + ")" : q.render());
			where.add(new IrPathTriple(sTxt, expr, oTxt));
			return true;
		}

		// Build a PathNode sequence from a JOIN chain that connects s -> o via _anon_path_* variables.
		// Accepts forward or inverse steps; allows the last step to directly reach the endpoint 'o'.
		private PathNode buildPathSequenceFromChain(TupleExpr chain, Var s, Var o) {
			List<TupleExpr> flat = new ArrayList<>();
			TupleExprIRRenderer.flattenJoin(chain, flat);
			List<StatementPattern> sps = new ArrayList<>();
			for (TupleExpr t : flat) {
				if (t instanceof StatementPattern) {
					sps.add((StatementPattern) t);
				} else {
					return null; // only simple statement patterns supported here
				}
			}
			if (sps.isEmpty()) {
				return null;
			}
			List<PathAtom> steps = new ArrayList<>();
			Var cur = s;
			Set<StatementPattern> used = new LinkedHashSet<>();
			int guard = 0;
			while (!sameVar(cur, o)) {
				if (++guard > 10000) {
					return null;
				}
				boolean advanced = false;
				for (StatementPattern sp : sps) {
					if (used.contains(sp))
						continue;
					Var pv = sp.getPredicateVar();
					if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI))
						continue;
					Var ss = sp.getSubjectVar();
					Var oo = sp.getObjectVar();
					if (sameVar(cur, ss) && (isAnonPathVar(oo) || sameVar(oo, o))) {
						steps.add(new PathAtom((IRI) pv.getValue(), false));
						cur = oo;
						used.add(sp);
						advanced = true;
						break;
					} else if (sameVar(cur, oo) && (isAnonPathVar(ss) || sameVar(ss, o))) {
						steps.add(new PathAtom((IRI) pv.getValue(), true));
						cur = ss;
						used.add(sp);
						advanced = true;
						break;
					}
				}
				if (!advanced) {
					return null;
				}
			}
			if (used.size() != sps.size()) {
				return null; // extra statements not part of the chain
			}
			if (steps.isEmpty()) {
				return null;
			}
			return (steps.size() == 1) ? steps.get(0) : new PathSeq(new ArrayList<>(steps));
		}

		@Override
		public void meet(final Difference diff) {
			// Print left side in sequence, then add a MINUS block for the right
			diff.getLeftArg().visit(this);
			IRBuilder right = new IRBuilder();
			IrBGP rightWhere = right.build(diff.getRightArg());
			where.add(new IrMinus(rightWhere));
		}

		@Override
		public void meet(final ArbitraryLengthPath p) {
			final String subj = renderVarOrValue(p.getSubjectVar());
			final String obj = renderVarOrValue(p.getObjectVar());
			final PathNode inner = parseAPathInner(p.getPathExpression(), p.getSubjectVar(), p.getObjectVar());
			if (inner == null) {
				where.add(new IrText("# unsupported path"));
				return;
			}
			final long min = p.getMinLength();
			final long max = getMaxLengthSafe(p);
			final PathNode q = new PathQuant(inner, min, max);
			final String expr = (q.prec() < PREC_SEQ ? "(" + q.render() + ")" : q.render());
			where.add(new IrPathTriple(subj, expr, obj));
		}

		@Override
		public void meet(final ZeroLengthPath p) {
			where.add(new IrText(
					"FILTER (sameTerm(" + renderVarOrValue(p.getSubjectVar()) + ", "
							+ renderVarOrValue(p.getObjectVar())
							+ "))"));
		}

		@Override
		public void meetOther(final QueryModelNode node) {
			where.add(new IrText("# unsupported node: "
					+ node.getClass().getSimpleName()));
		}
	}

	// ---------------- Public entry points ----------------

	/** Backward-compatible: render as SELECT query (no dataset). */
	public String render(final TupleExpr tupleExpr) {
		suppressedSubselects.clear();
		return renderSelectInternal(tupleExpr, RenderMode.TOP_LEVEL_SELECT, null);
	}

	/** SELECT with dataset (FROM/FROM NAMED). */
	public String render(final TupleExpr tupleExpr, final DatasetView dataset) {
		suppressedSubselects.clear();
		return renderSelectInternal(tupleExpr, RenderMode.TOP_LEVEL_SELECT, dataset);
	}

	/** ASK query (top-level). */
	public String renderAsk(final TupleExpr tupleExpr, final DatasetView dataset) {
		suppressedSubselects.clear();
		final StringBuilder out = new StringBuilder(256);
		final Normalized n = normalize(tupleExpr);
		// Prologue
		printPrologueAndDataset(out, dataset);
		out.append("ASK");
		// WHERE
		out.append(cfg.canonicalWhitespace ? "\nWHERE " : " WHERE ");
		final BlockPrinter bp = new BlockPrinter(out, this, cfg);
		bp.openBlock();
		n.where.visit(bp);
		bp.closeBlock();
		return mergeAdjacentGraphBlocks(out.toString()).trim();
	}

	// ---------------- Core SELECT and subselect ----------------

	private String renderSubselect(final TupleExpr subtree) {
		return renderSelectInternal(subtree, RenderMode.SUBSELECT, null);
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

	// ---------------- Normalization shell ----------------

	private static final class GroupByTerm {
		final String var; // ?var
		final ValueExpr expr; // null => plain ?var; otherwise (expr AS ?var)

		GroupByTerm(String var, ValueExpr expr) {
			this.var = var;
			this.expr = expr;
		}
	}

	private static final class Normalized {
		Projection projection; // SELECT vars/exprs
		TupleExpr where; // WHERE pattern (group peeled)
		boolean distinct = false;
		boolean reduced = false;
		long limit = -1, offset = -1;
		final List<OrderElem> orderBy = new ArrayList<>();
		final LinkedHashMap<String, ValueExpr> selectAssignments = new LinkedHashMap<>(); // alias -> expr
		final List<GroupByTerm> groupByTerms = new ArrayList<>(); // explicit terms (var or (expr AS ?var))
		final List<String> syntheticProjectVars = new ArrayList<>(); // synthesized bare SELECT vars
		final List<ValueExpr> havingConditions = new ArrayList<>();
		boolean hadExplicitGroup = false; // true if a Group wrapper was present
		final Set<String> groupByVarNames = new LinkedHashSet<>();
		final Set<String> aggregateOutputNames = new LinkedHashSet<>();
	}

	/**
	 * Peel wrappers until fixed point, with special handling for Filter(Group(...)) → HAVING.
	 */
	private Normalized normalize(final TupleExpr root) {
		final Normalized n = new Normalized();
		TupleExpr cur = root;

		boolean changed;
		do {
			changed = false;

			if (cur instanceof QueryRoot) {
				cur = ((QueryRoot) cur).getArg();
				changed = true;
				continue;
			}

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

			if (cur instanceof Reduced) {
				n.reduced = true;
				cur = ((Reduced) cur).getArg();
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

			// Handle Filter → HAVING
			if (cur instanceof Filter) {
				final Filter f = (Filter) cur;
				final TupleExpr arg = f.getArg();

				// Marker-based: any _anon_having_* var -> HAVING
				{
					Set<String> fv = freeVars(f.getCondition());
					boolean hasHavingMarker = false;
					for (String vn : fv) {
						if (isAnonHavingName(vn)) {
							hasHavingMarker = true;
							break;
						}
					}
					if (hasHavingMarker) {
						n.havingConditions.add(f.getCondition());
						cur = f.getArg();
						changed = true;
						continue;
					}
				}

				// Group underneath
				if (arg instanceof Group) {
					final Group g = (Group) arg;
					n.hadExplicitGroup = true;

					n.groupByVarNames.clear();
					n.groupByVarNames.addAll(new LinkedHashSet<>(g.getGroupBindingNames()));

					TupleExpr afterGroup = g.getArg();
					Map<String, ValueExpr> groupAliases = new LinkedHashMap<>();
					while (afterGroup instanceof Extension) {
						final Extension ext = (Extension) afterGroup;
						for (ExtensionElem ee : ext.getElements()) {
							if (n.groupByVarNames.contains(ee.getName())) {
								groupAliases.put(ee.getName(), ee.getExpr());
							}
						}
						afterGroup = ext.getArg();
					}

					n.groupByTerms.clear();
					for (String nm : n.groupByVarNames) {
						n.groupByTerms.add(new GroupByTerm(nm, groupAliases.getOrDefault(nm, null)));
					}

					for (GroupElem ge : g.getGroupElements()) {
						n.selectAssignments.putIfAbsent(ge.getName(), ge.getOperator());
						n.aggregateOutputNames.add(ge.getName());
					}

					ValueExpr cond = f.getCondition();
					if (containsAggregate(cond) || isHavingCandidate(cond, n.groupByVarNames, n.aggregateOutputNames)) {
						n.havingConditions.add(cond);
						cur = afterGroup;
						changed = true;
						continue;
					} else {
						cur = new Filter(afterGroup, cond); // keep as WHERE filter
						changed = true;
						continue;
					}
				}

				// Aggregate filter at top-level → HAVING
				if (containsAggregate(f.getCondition())) {
					n.havingConditions.add(f.getCondition());
					cur = f.getArg();
					changed = true;
					continue;
				}

				// else: leave the Filter in place
			}

			// Projection (record it and peel)
			if (cur instanceof Projection) {
				n.projection = (Projection) cur;
				cur = n.projection.getArg();
				changed = true;
				continue;
			}

			// SELECT-level assignments
			if (cur instanceof Extension) {
				final Extension ext = (Extension) cur;
				for (final ExtensionElem ee : ext.getElements()) {
					n.selectAssignments.put(ee.getName(), ee.getExpr());
				}
				cur = ext.getArg();
				changed = true;
				continue;
			}

			// GROUP outside Filter
			if (cur instanceof Group) {
				final Group g = (Group) cur;
				n.hadExplicitGroup = true;

				n.groupByVarNames.clear();
				n.groupByVarNames.addAll(new LinkedHashSet<>(g.getGroupBindingNames()));

				TupleExpr afterGroup = g.getArg();
				Map<String, ValueExpr> groupAliases = new LinkedHashMap<>();
				while (afterGroup instanceof Extension) {
					final Extension ext = (Extension) afterGroup;
					for (ExtensionElem ee : ext.getElements()) {
						if (n.groupByVarNames.contains(ee.getName())) {
							groupAliases.put(ee.getName(), ee.getExpr());
						}
					}
					afterGroup = ext.getArg();
				}

				n.groupByTerms.clear();
				for (String nm : n.groupByVarNames) {
					n.groupByTerms.add(new GroupByTerm(nm, groupAliases.getOrDefault(nm, null)));
				}

				for (GroupElem ge : g.getGroupElements()) {
					n.selectAssignments.putIfAbsent(ge.getName(), ge.getOperator());
					n.aggregateOutputNames.add(ge.getName());
				}

				cur = afterGroup;
				changed = true;
			}

		} while (changed);

		n.where = cur;
		return n;
	}

	private boolean isHavingCandidate(ValueExpr cond, Set<String> groupVars, Set<String> aggregateAliasVars) {
		Set<String> free = freeVars(cond);
		if (free.isEmpty()) {
			return true; // constant condition → valid HAVING
		}
		Set<String> allowed = new HashSet<>(groupVars);
		allowed.addAll(aggregateAliasVars);
		return allowed.containsAll(free);
	}

	// ---------------- Aggregate hoisting & inference ----------------

	private void applyAggregateHoisting(final Normalized n) {
		final AggregateScan scan = new AggregateScan();
		n.where.visit(scan);

		// Promote aggregates found as BINDs inside WHERE
		if (!scan.hoisted.isEmpty()) {
			for (Entry<String, ValueExpr> e : scan.hoisted.entrySet()) {
				n.selectAssignments.putIfAbsent(e.getKey(), e.getValue());
			}
		}

		boolean hasAggregates = !scan.hoisted.isEmpty();
		for (Entry<String, ValueExpr> e : n.selectAssignments.entrySet()) {
			if (e.getValue() instanceof AggregateOperator) {
				hasAggregates = true;
				scan.aggregateOutputNames.add(e.getKey());
				collectVarNames(e.getValue(), scan.aggregateArgVars);
			}
		}

		if (!hasAggregates) {
			return;
		}
		if (n.hadExplicitGroup) {
			return;
		}

		// Projection-driven grouping
		if (n.groupByTerms.isEmpty() && n.projection != null && n.projection.getProjectionElemList() != null) {
			final List<GroupByTerm> terms = new ArrayList<>();
			for (ProjectionElem pe : n.projection.getProjectionElemList().getElements()) {
				final String name = pe.getProjectionAlias().orElse(pe.getName());
				if (name != null && !name.isEmpty() && !n.selectAssignments.containsKey(name)) {
					terms.add(new GroupByTerm(name, null));
				}
			}
			if (!terms.isEmpty()) {
				n.groupByTerms.addAll(terms);
				return;
			}
		}

		// Usage-based inference
		if (n.groupByTerms.isEmpty()) {
			Set<String> candidates = new LinkedHashSet<>(scan.varCounts.keySet());
			candidates.removeAll(scan.aggregateOutputNames);
			candidates.removeAll(scan.aggregateArgVars);

			List<String> multiUse = candidates.stream()
					.filter(v -> scan.varCounts.getOrDefault(v, 0) > 1)
					.collect(Collectors.toList());

			List<String> chosen;
			if (!multiUse.isEmpty()) {
				chosen = multiUse;
			} else {
				chosen = new ArrayList<>(1);
				if (!candidates.isEmpty()) {
					candidates.stream().min((a, b) -> {
						int as = scan.subjCounts.getOrDefault(a, 0);
						int bs = scan.subjCounts.getOrDefault(b, 0);
						if (as != bs) {
							return Integer.compare(bs, as);
						}
						int ao = scan.objCounts.getOrDefault(a, 0);
						int bo = scan.objCounts.getOrDefault(b, 0);
						if (ao != bo) {
							return Integer.compare(bo, ao);
						}
						int ap = scan.predCounts.getOrDefault(a, 0);
						int bp = scan.predCounts.getOrDefault(b, 0);
						if (ap != bp) {
							return Integer.compare(bp, ap);
						}
						return a.compareTo(b);
					}).ifPresent(chosen::add);
				}
			}

			n.syntheticProjectVars.clear();
			n.syntheticProjectVars.addAll(chosen);

			if (n.projection == null || n.projection.getProjectionElemList().getElements().isEmpty()) {
				n.groupByTerms.clear();
				for (String v : n.syntheticProjectVars) {
					n.groupByTerms.add(new GroupByTerm(v, null));
				}
			}
		}
	}

	private static final class AggregateScan extends AbstractQueryModelVisitor<RuntimeException> {
		final LinkedHashMap<String, ValueExpr> hoisted = new LinkedHashMap<>();
		final Map<String, Integer> varCounts = new HashMap<>();
		final Map<String, Integer> subjCounts = new HashMap<>();
		final Map<String, Integer> predCounts = new HashMap<>();
		final Map<String, Integer> objCounts = new HashMap<>();
		final Set<String> aggregateArgVars = new HashSet<>();
		final Set<String> aggregateOutputNames = new HashSet<>();

		@Override
		public void meet(StatementPattern sp) {
			count(sp.getSubjectVar(), subjCounts);
			count(sp.getPredicateVar(), predCounts);
			count(sp.getObjectVar(), objCounts);
		}

		@Override
		public void meet(Projection subqueryProjection) {
			// Do not descend into subselects when scanning for aggregates.
		}

		@Override
		public void meet(Extension ext) {
			ext.getArg().visit(this);
			for (ExtensionElem ee : ext.getElements()) {
				ValueExpr expr = ee.getExpr();
				if (expr instanceof AggregateOperator) {
					hoisted.putIfAbsent(ee.getName(), expr);
					aggregateOutputNames.add(ee.getName());
					collectVarNames(expr, aggregateArgVars);
				}
			}
		}

		private void count(Var v, Map<String, Integer> roleMap) {
			if (v == null || v.hasValue()) {
				return;
			}
			final String name = v.getName();
			if (name == null || name.isEmpty()) {
				return;
			}
			varCounts.merge(name, 1, Integer::sum);
			roleMap.merge(name, 1, Integer::sum);
		}
	}

	// ---------------- Utilities: vars, aggregates, free vars ----------------

	private static boolean containsAggregate(ValueExpr e) {
		if (e == null) {
			return false;
		}
		if (e instanceof AggregateOperator) {
			return true;
		}
		if (e instanceof Not) {
			return containsAggregate(((Not) e).getArg());
		}
		if (e instanceof Bound) {
			return containsAggregate(((Bound) e).getArg());
		}
		if (e instanceof Str) {
			return containsAggregate(((Str) e).getArg());
		}
		if (e instanceof Datatype) {
			return containsAggregate(((Datatype) e).getArg());
		}
		if (e instanceof Lang) {
			return containsAggregate(((Lang) e).getArg());
		}
		if (e instanceof IsURI) {
			return containsAggregate(((IsURI) e).getArg());
		}
		if (e instanceof IsLiteral) {
			return containsAggregate(((IsLiteral) e).getArg());
		}
		if (e instanceof IsBNode) {
			return containsAggregate(((IsBNode) e).getArg());
		}
		if (e instanceof IsNumeric) {
			return containsAggregate(((IsNumeric) e).getArg());
		}
		if (e instanceof IRIFunction) {
			return containsAggregate(((IRIFunction) e).getArg());
		}
		if (e instanceof If) {
			If iff = (If) e;
			return containsAggregate(iff.getCondition()) || containsAggregate(iff.getResult())
					|| containsAggregate(iff.getAlternative());
		}
		if (e instanceof Coalesce) {
			for (ValueExpr a : ((Coalesce) e).getArguments()) {
				if (containsAggregate(a)) {
					return true;
				}
			}
			return false;
		}
		if (e instanceof FunctionCall) {
			for (ValueExpr a : ((FunctionCall) e).getArgs()) {
				if (containsAggregate(a)) {
					return true;
				}
			}
			return false;
		}
		if (e instanceof And) {
			return containsAggregate(((And) e).getLeftArg())
					|| containsAggregate(((And) e).getRightArg());
		}
		if (e instanceof Or) {
			return containsAggregate(((Or) e).getLeftArg())
					|| containsAggregate(((Or) e).getRightArg());
		}
		if (e instanceof Compare) {
			return containsAggregate(((Compare) e).getLeftArg())
					|| containsAggregate(((Compare) e).getRightArg());
		}
		if (e instanceof SameTerm) {
			return containsAggregate(((SameTerm) e).getLeftArg())
					|| containsAggregate(((SameTerm) e).getRightArg());
		}
		if (e instanceof LangMatches) {
			return containsAggregate(((LangMatches) e).getLeftArg())
					|| containsAggregate(((LangMatches) e).getRightArg());
		}
		if (e instanceof Regex) {
			Regex r = (Regex) e;
			return containsAggregate(r.getArg()) || containsAggregate(r.getPatternArg())
					|| (r.getFlagsArg() != null && containsAggregate(r.getFlagsArg()));
		}
		if (e instanceof ListMemberOperator) {
			for (ValueExpr a : ((ListMemberOperator) e).getArguments()) {
				if (containsAggregate(a)) {
					return true;
				}
			}
			return false;
		}
		if (e instanceof MathExpr) {
			return containsAggregate(((MathExpr) e).getLeftArg())
					|| containsAggregate(((MathExpr) e).getRightArg());
		}
		return false;
	}

	private static Set<String> freeVars(ValueExpr e) {
		Set<String> out = new HashSet<>();
		collectVarNames(e, out);
		return out;
	}

	private static void collectVarNames(ValueExpr e, Set<String> acc) {
		if (e == null) {
			return;
		}
		if (e instanceof Var) {
			final Var v = (Var) e;
			if (!v.hasValue() && v.getName() != null && !v.getName().isEmpty()) {
				acc.add(v.getName());
			}
			return;
		}
		if (e instanceof ValueConstant) {
			return;
		}

		if (e instanceof Not) {
			collectVarNames(((Not) e).getArg(), acc);
			return;
		}
		if (e instanceof Bound) {
			collectVarNames(((Bound) e).getArg(), acc);
			return;
		}
		if (e instanceof Str) {
			collectVarNames(((Str) e).getArg(), acc);
			return;
		}
		if (e instanceof Datatype) {
			collectVarNames(((Datatype) e).getArg(), acc);
			return;
		}
		if (e instanceof Lang) {
			collectVarNames(((Lang) e).getArg(), acc);
			return;
		}
		if (e instanceof IsURI) {
			collectVarNames(((IsURI) e).getArg(), acc);
			return;
		}
		if (e instanceof IsLiteral) {
			collectVarNames(((IsLiteral) e).getArg(), acc);
			return;
		}
		if (e instanceof IsBNode) {
			collectVarNames(((IsBNode) e).getArg(), acc);
			return;
		}
		if (e instanceof IsNumeric) {
			collectVarNames(((IsNumeric) e).getArg(), acc);
			return;
		}
		if (e instanceof IRIFunction) {
			collectVarNames(((IRIFunction) e).getArg(), acc);
			return;
		}

		if (e instanceof And) {
			collectVarNames(((And) e).getLeftArg(), acc);
			collectVarNames(((And) e).getRightArg(), acc);
			return;
		}
		if (e instanceof Or) {
			collectVarNames(((Or) e).getLeftArg(), acc);
			collectVarNames(((Or) e).getRightArg(), acc);
			return;
		}
		if (e instanceof Compare) {
			collectVarNames(((Compare) e).getLeftArg(), acc);
			collectVarNames(((Compare) e).getRightArg(), acc);
			return;
		}
		if (e instanceof SameTerm) {
			collectVarNames(((SameTerm) e).getLeftArg(), acc);
			collectVarNames(((SameTerm) e).getRightArg(), acc);
			return;
		}
		if (e instanceof LangMatches) {
			collectVarNames(((LangMatches) e).getLeftArg(), acc);
			collectVarNames(((LangMatches) e).getRightArg(), acc);
			return;
		}
		if (e instanceof Regex) {
			final Regex r = (Regex) e;
			collectVarNames(r.getArg(), acc);
			collectVarNames(r.getPatternArg(), acc);
			if (r.getFlagsArg() != null) {
				collectVarNames(r.getFlagsArg(), acc);
			}
			return;
		}
		if (e instanceof FunctionCall) {
			for (ValueExpr a : ((FunctionCall) e).getArgs()) {
				collectVarNames(a, acc);
			}
			return;
		}
		if (e instanceof ListMemberOperator) {
			final List<ValueExpr> args = ((ListMemberOperator) e).getArguments();
			if (args != null) {
				for (ValueExpr a : args) {
					collectVarNames(a, acc);
				}
			}
		}
		if (e instanceof MathExpr) {
			collectVarNames(((MathExpr) e).getLeftArg(), acc);
			collectVarNames(((MathExpr) e).getRightArg(), acc);
		}
		if (e instanceof If) {
			final If iff = (If) e;
			collectVarNames(iff.getCondition(), acc);
			collectVarNames(iff.getResult(), acc);
			collectVarNames(iff.getAlternative(), acc);
		}
		if (e instanceof Coalesce) {
			for (ValueExpr a : ((Coalesce) e).getArguments()) {
				collectVarNames(a, acc);
			}
		}
	}

	// ---------------- Block/Node printer ----------------

	/** Projections that must be suppressed (already rewritten into path). */
	private final Set<Object> suppressedSubselects = Collections.newSetFromMap(new IdentityHashMap<>());

	/** Unions that must be suppressed (already rewritten into alternation path). */
	private final Set<Object> suppressedUnions = Collections.newSetFromMap(new IdentityHashMap<>());

	private void suppressProjectionSubselect(final TupleExpr container) {
		if (container instanceof Projection) {
			suppressedSubselects.add(container);
		} else if (container instanceof Distinct) {
			TupleExpr arg = ((Distinct) container).getArg();
			if (arg instanceof Projection) {
				suppressedSubselects.add(arg);
			}
		}
	}

	private boolean isProjectionSuppressed(final Projection p) {
		return suppressedSubselects.contains(p);
	}

	private void suppressUnion(final TupleExpr u) {
		suppressedUnions.add(u);
	}

	private boolean isUnionSuppressed(final Union u) {
		return suppressedUnions.contains(u);
	}

	private final class BlockPrinter extends AbstractQueryModelVisitor<RuntimeException> {
		private final StringBuilder out;
		private final TupleExprIRRenderer r;
		private final Config cfg;

		private final String indentUnit;
		private int level = 0;
		// Persistent GRAPH grouping across multiple IR passes
		private String openGraphRef = null;
		private final List<String> openGraphLines = new ArrayList<>();
		private final boolean suppressGraph; // when true, print triples without wrapping GRAPH even if context present

		BlockPrinter(final StringBuilder out, final TupleExprIRRenderer renderer, final Config cfg) {
			this.out = out;
			this.r = renderer;
			this.cfg = cfg;
			this.indentUnit = cfg.indent;
			this.suppressGraph = false;
		}

		BlockPrinter(final StringBuilder out, final TupleExprIRRenderer renderer, final Config cfg,
				final boolean suppressGraph) {
			this.out = out;
			this.r = renderer;
			this.cfg = cfg;
			this.indentUnit = cfg.indent;
			this.suppressGraph = suppressGraph;
		}

		void openBlock() {
			out.append("{");
			newline();
			level++;
		}

		void closeBlock() {
			// Always flush any pending GRAPH grouping when closing a block to keep
			// GRAPH content scoped inside the current block (e.g., OPTIONAL, UNION branches, SERVICE).
			flushOpenGraph();
			level--;
			indent();
			out.append("}");
		}

		void closeBlockDirect() {
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

		void emitGraphLine(final String graphRef, final String text) {
			// When suppressGraph is enabled (used by a temporary printer to inline
			// subtrees detected to share a single GRAPH context), never create or
			// buffer GRAPH groupings here. Just emit the given text as a normal line.
			if (suppressGraph) {
				line(text);
				return;
			}
			final boolean plain = text.endsWith(" .");
			if (!plain) {
				flushOpenGraph();
				line(text);
				return;
			}
			if (graphRef == null) {
				flushOpenGraph();
				line(text);
				return;
			}
			if (openGraphRef == null) {
				openGraphRef = graphRef;
			}
			if (!openGraphRef.equals(graphRef)) {
				flushOpenGraph();
				openGraphRef = graphRef;
			}
			openGraphLines.add(text);
		}

		void flushOpenGraph() {
			if (openGraphRef != null && !openGraphLines.isEmpty()) {
				indent();
				raw("GRAPH " + openGraphRef + " ");
				openBlock();
				for (String ln : openGraphLines) {
					line(ln);
				}
				closeBlockDirect();
				newline();
			}
			openGraphLines.clear();
			openGraphRef = null;
		}

		void newline() {
			out.append('\n');
		}

		void indent() {
			out.append(indentUnit.repeat(Math.max(0, level)));
		}

		@Override
		public void meet(final StatementPattern sp) {
			final Var ctx = sp.getContextVar();
			if (!suppressGraph && ctx != null
					&& (ctx.hasValue() || (ctx.getName() != null && !ctx.getName().isEmpty()))) {
				final String triple = r.renderVarOrValue(sp.getSubjectVar()) + " "
						+ r.renderPredicateForTriple(sp.getPredicateVar()) + " "
						+ r.renderVarOrValue(sp.getObjectVar()) + " .";
				emitGraphLine(r.renderVarOrValue(ctx), triple);
				return;
			}

			// Inverse-path heuristic for single triples: if predicate is constant IRI and subject/object are
			// free vars named 'o'/'s', prefer printing '?s ^p ?o'
			final Var pVar = sp.getPredicateVar();
			if (pVar != null && pVar.hasValue() && pVar.getValue() instanceof IRI) {
				final Var sVar = sp.getSubjectVar();
				final Var oVar = sp.getObjectVar();
				if (sVar != null && oVar != null && !sVar.hasValue() && !oVar.hasValue()) {
					final String sName = sVar.getName();
					final String oName = oVar.getName();
					if ("o".equals(sName) && "s".equals(oName)) {
						line("?s ^" + r.renderIRI((IRI) pVar.getValue()) + " ?o .");
						return;
					}
				}
			}

			line(r.renderVarOrValue(sp.getSubjectVar()) + " " + r.renderPredicateForTriple(sp.getPredicateVar()) + " "
					+ r.renderVarOrValue(sp.getObjectVar()) + " .");
		}

		@Override
		public void meet(final Projection p) {
			// Special-case: detect RDF4J's subselect expansion of a simple zero-or-one path and
			// render it as a compact property path triple instead of a subselect block.
			{
				final ZeroOrOneDirect z1 = r.parseZeroOrOneProjectionDirect(p);
				if (z1 != null) {
					final String s = r.renderVarOrValue(z1.start);
					final String o = r.renderVarOrValue(z1.end);
					final String path = new PathQuant(new PathAtom(z1.pred, false), 0, 1).render();
					line(s + " " + path + " " + o + " .");
					return;
				}
			}

			// Nested Projection inside WHERE => subselect (unless it has been consumed by path fusion)
			if (r.isProjectionSuppressed(p)) {
				return;
			}
			String sub = r.renderSubselect(p);
			// Ensure any pending GRAPH block is closed before starting a subselect block
			flushOpenGraph();
			indent();
			raw("{");
			newline();
			level++;
			for (String ln : sub.split("\\R", -1)) {
				indent();
				raw(ln);
				newline();
			}
			level--;
			indent();
			raw("}");
			newline();
		}

		@Override
		public void meet(final Join join) {
			// Flatten subtree
			final List<TupleExpr> flat = new ArrayList<>();
			TupleExprIRRenderer.flattenJoin(join, flat);

			// Detect RDF collections -> overrides & consumed
			final CollectionResult col = r.detectCollections(flat);

			// Ordered pass with rewrites + property list compaction
			if (r.tryRenderBestEffortPathChain(flat, this, col.overrides, col.consumed)) {
				return;
			}

			// Fallback (should not happen now): print remaining nodes in-order
			for (TupleExpr n : flat) {
				if (col.consumed.contains(n)) {
					continue;
				}
				if (n instanceof StatementPattern) {
					printStatementWithOverrides((StatementPattern) n, col.overrides, this);
				} else {
					n.visit(this);
				}
			}
		}

		@Override
		public void meet(final LeftJoin lj) {
			lj.getLeftArg().visit(this);
			// Flush any pending GRAPH lines from the outer scope before opening OPTIONAL block
			flushOpenGraph();
			indent();
			raw("OPTIONAL ");
			openBlock();
			lj.getRightArg().visit(this);
			if (lj.getCondition() != null) {
				String cond = r.renderExpr(lj.getCondition());
				cond = TupleExprIRRenderer.stripRedundantOuterParens(cond);
				flushOpenGraph();
				line("FILTER (" + cond + ")");
			}
			closeBlock();
			newline();
		}

		@Override
		public void meet(final Union union) {
			if (r.isUnionSuppressed(union)) {
				return;
			}
			// Try compact alternation when both sides are simple triples with identical endpoints
			if (tryRenderUnionAsPathAlternation(union)) {
				return;
			}

			// Flatten nested UNION chains to print a clean, single-level sequence of branches
			final List<TupleExpr> branches = new ArrayList<>();
			flattenUnion(union, branches);
			for (int i = 0; i < branches.size(); i++) {
				// Flush any pending GRAPH group before starting a new UNION branch block
				flushOpenGraph();
				indent();
				openBlock();
				printSubtreeWithBestEffort(branches.get(i));
				closeBlock();
				newline();
				if (i + 1 < branches.size()) {
					indent();
					line("UNION");
				}
			}
		}

		private void printSubtreeWithBestEffort(final TupleExpr subtree) {
			final List<TupleExpr> flat = new ArrayList<>();
			if (subtree instanceof Join) {
				TupleExprIRRenderer.flattenJoin(subtree, flat);
			} else {
				flat.add(subtree);
			}
			final CollectionResult col = r.detectCollections(flat);
			r.tryRenderBestEffortPathChain(flat, this, col.overrides, col.consumed);
		}

		private boolean tryRenderUnionAsPathAlternation(final Union u) {
			final List<TupleExpr> leaves = new ArrayList<>();
			flattenUnion(u, leaves);
			if (leaves.isEmpty()) {
				return false;
			}
			Var subj = null, obj = null;
			Var ctxRef = null;
			final List<IRI> iris = new ArrayList<>();
			for (TupleExpr leaf : leaves) {
				if (!(leaf instanceof StatementPattern)) {
					return false;
				}
				final StatementPattern sp = (StatementPattern) leaf;
				final Var ctx = getContextVarSafe(sp);
				if (ctxRef == null) {
					ctxRef = ctx;
				} else if (contextsIncompatible(ctxRef, ctx)) {
					return false;
				}
				final Var pv = sp.getPredicateVar();
				if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
					return false;
				}
				final Var s = sp.getSubjectVar();
				final Var o = sp.getObjectVar();
				if (subj == null && obj == null) {
					subj = s;
					obj = o;
				} else if (!(sameVar(s, subj) && sameVar(o, obj))) {
					return false;
				}
				iris.add((IRI) pv.getValue());
			}
			final String sStr = r.renderVarOrValue(subj);
			final String oStr = r.renderVarOrValue(obj);
			final String alt = new PathAlt(
					iris.stream().map(iri -> new PathAtom(iri, false)).collect(Collectors.toList())).render();
			final String triple = sStr + " " + (iris.size() > 1 ? "(" + alt + ")" : alt) + " " + oStr + " .";
			if (ctxRef != null && (ctxRef.hasValue() || (ctxRef.getName() != null && !ctxRef.getName().isEmpty()))) {
				emitGraphLine(r.renderVarOrValue(ctxRef), triple);
			} else {
				line(triple);
			}
			return true;
		}

		@Override
		public void meet(final Difference diff) {
			diff.getLeftArg().visit(this);
			// Flush any pending GRAPH group before starting MINUS block
			flushOpenGraph();
			indent();
			raw("MINUS ");
			openBlock();
			diff.getRightArg().visit(this);
			closeBlock();
			newline();
		}

		@Override
		public void meet(final Filter filter) {
			// Prefer printing FILTER before a trailing subselect when the filter does not depend on
			// variables produced by that subselect.
			final TupleExpr arg = filter.getArg();
			Projection trailingProj = null;
			List<TupleExpr> head = null;
			if (arg instanceof Join) {
				final List<TupleExpr> flat = new ArrayList<>();
				TupleExprIRRenderer.flattenJoin(arg, flat);
				if (!flat.isEmpty()) {
					TupleExpr last = flat.get(flat.size() - 1);
					Projection maybe = extractProjection(last);
					if (maybe != null && !r.isProjectionSuppressed(maybe)) {
						trailingProj = maybe;
						head = new ArrayList<>(flat);
						head.remove(head.size() - 1);
					}
				}
			}

			if (trailingProj != null) {
				// Decide dependency based on what variables are already available from the head (left part of the
				// join).
				// If the filter's variables are all bound by the head, we can safely print the FILTER before the
				// trailing subselect regardless of overlapping projection names.
				final Set<String> headVars = new LinkedHashSet<>();
				for (TupleExpr n : head) {
					collectFreeVars(n, headVars);
				}
				final Set<String> condVars = freeVars(filter.getCondition());
				final boolean canMoveBefore = headVars.containsAll(condVars);

				if (canMoveBefore) {
					// Print head first, then FILTER, then trailing subselect
					final CollectionResult col = r.detectCollections(head);
					r.tryRenderBestEffortPathChain(head, this, col.overrides, col.consumed);
					String cond = r.renderExpr(filter.getCondition());
					cond = TupleExprIRRenderer.stripRedundantOuterParens(cond);
					flushOpenGraph();
					line("FILTER (" + cond + ")");
					trailingProj.visit(this);
					return;
				}
			}

			// Default: print argument, then the FILTER
			arg.visit(this);
			String cond = r.renderExpr(filter.getCondition());
			cond = TupleExprIRRenderer.stripRedundantOuterParens(cond);
			flushOpenGraph();
			line("FILTER (" + cond + ")");
		}

		private Projection extractProjection(TupleExpr node) {
			if (node instanceof Projection) {
				return (Projection) node;
			}
			if (node instanceof Distinct && ((Distinct) node).getArg() instanceof Projection) {
				return (Projection) ((Distinct) node).getArg();
			}
			return null;
		}

		@Override
		public void meet(final Extension ext) {
			ext.getArg().visit(this);
			for (final ExtensionElem ee : ext.getElements()) {
				final ValueExpr expr = ee.getExpr();
				if (expr instanceof AggregateOperator) {
					continue; // hoisted to SELECT
				}
				line("BIND(" + r.renderExpr(expr) + " AS ?" + ee.getName() + ")");
			}
		}

		@Override
		public void meet(final Service svc) {
			// Flush any pending GRAPH lines from outer scope before entering SERVICE block
			flushOpenGraph();
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
			// Flush before starting VALUES block to avoid mixing into GRAPH groups
			flushOpenGraph();
			List<String> names = new ArrayList<>(bsa.getBindingNames());
			if (!cfg.valuesPreserveOrder) {
				Collections.sort(names);
			}

			indent();
			if (names.isEmpty()) {
				raw("VALUES () ");
				openBlock();
				int rows = getRows(bsa);
				for (int i = 0; i < rows; i++) {
					indent();
					raw("()");
					newline();
				}
				closeBlock();
				newline();
				return;
			}

			final String head = names.stream().map(n -> "?" + n).collect(Collectors.joining(" "));
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

		@Override
		public void meet(final ArbitraryLengthPath p) {
			final String subj = r.renderVarOrValue(p.getSubjectVar());
			final String obj = r.renderVarOrValue(p.getObjectVar());
			final Var ctx = getContextVarSafe(p);

			final PathNode inner = r.parseAPathInner(p.getPathExpression(), p.getSubjectVar(), p.getObjectVar());
			if (inner == null) {
				r.handleUnsupported("complex ArbitraryLengthPath without simple/alternation atom");
				return;
			}
			final long min = p.getMinLength();
			final long max = getMaxLengthSafe(p);
			final PathNode q = new PathQuant(inner, min, max);

			final String expr = (q.prec() < PREC_SEQ ? "(" + q.render() + ")" : q.render());
			final String triple = subj + " " + expr + " " + obj + " .";

			if (!suppressGraph && ctx != null
					&& (ctx.hasValue() || (ctx.getName() != null && !ctx.getName().isEmpty()))) {
				emitGraphLine(r.renderVarOrValue(ctx), triple);
			} else {
				line(triple);
			}
		}

		@Override
		public void meet(final ZeroLengthPath p) {
			line("FILTER (sameTerm(" + r.renderVarOrValue(p.getSubjectVar()) + ", " +
					r.renderVarOrValue(p.getObjectVar()) + "))");
		}

		@Override
		public void meetOther(final QueryModelNode node) {
			r.handleUnsupported("unsupported node in WHERE: " + node.getClass().getSimpleName());
		}

	}

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

	private static long getMaxLengthSafe(final ArbitraryLengthPath p) {
		try {
			final Method m = ArbitraryLengthPath.class.getMethod("getMaxLength");
			final Object v = m.invoke(p);
			if (v instanceof Number) {
				return ((Number) v).longValue();
			}
		} catch (ReflectiveOperationException ignore) {
		}
		return -1L;
	}

	private static int getRows(BindingSetAssignment bsa) {
		Iterable<BindingSet> bindingSets = bsa.getBindingSets();
		if (bindingSets instanceof List) {
			return ((List<BindingSet>) bindingSets).size();
		}
		if (bindingSets instanceof Set) {
			return ((Set<BindingSet>) bindingSets).size();
		}

		int count = 0;
		for (BindingSet ignored : bindingSets) {
			count++;
		}

		return count;
	}

	// ---------------- Rendering helpers (prefix-aware) ----------------

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

	private static Var getContextVarSafe(StatementPattern sp) {
		try {
			Method m = StatementPattern.class.getMethod("getContextVar");
			Object ctx = m.invoke(sp);
			if (ctx instanceof Var) {
				return (Var) ctx;
			}
		} catch (ReflectiveOperationException ignore) {
		}
		return null;
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

	// Rough but much more complete PN_LOCAL acceptance + “no trailing dot”
	private static final Pattern PN_LOCAL_CHUNK = Pattern.compile("(?:%[0-9A-Fa-f]{2}|[-\\p{L}\\p{N}_\\u00B7]|:)+");

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
			return "!(" + inner + ")";
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
			// Try to reconstruct NOT IN from a conjunction of "?v != const" terms
			final String maybeNotIn = tryRenderNotInFromAnd(e);
			if (maybeNotIn != null) {
				return maybeNotIn;
			}
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

	/**
	 * Best-effort reconstruction of "?v NOT IN (c1, c2, ...)" from a flattened And-expression of Compare(!=) terms
	 * against the same variable. Returns null if the expression does not match this pattern, or if it only contains a
	 * single inequality (we avoid rewriting a single term).
	 */
	private String tryRenderNotInFromAnd(final ValueExpr expr) {
		final List<ValueExpr> terms = flattenAnd(expr);
		if (terms.isEmpty()) {
			return null;
		}
		Var var = null;
		final List<Value> constants = new ArrayList<>();
		for (ValueExpr t : terms) {
			if (!(t instanceof Compare)) {
				return null;
			}
			final Compare c = (Compare) t;
			if (c.getOperator() != CompareOp.NE) {
				return null;
			}
			final ValueExpr L = c.getLeftArg();
			final ValueExpr R = c.getRightArg();
			Var v;
			Value val;
			if (L instanceof Var && R instanceof ValueConstant) {
				v = (Var) L;
				val = ((ValueConstant) R).getValue();
			} else if (R instanceof Var && L instanceof ValueConstant) {
				v = (Var) R;
				val = ((ValueConstant) L).getValue();
			} else {
				return null;
			}
			if (v.hasValue() || val == null) {
				return null;
			}
			if (var == null) {
				var = v;
			} else if (!Objects.equals(var.getName(), v.getName())) {
				return null; // different variables involved
			}
			constants.add(val);
		}
		if (constants.size() < 2) {
			return null; // don't rewrite a single inequality into NOT IN
		}
		final String head = var.hasValue() ? renderValue(var.getValue()) : ("?" + var.getName());
		final String list = constants.stream().map(this::renderValue).collect(Collectors.joining(", "));
		return head + " NOT IN (" + list + ")";
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

	/** Use BlockPrinter to render a subpattern inline for EXISTS. */
	private String renderInlineGroup(final TupleExpr pattern) {
		final StringBuilder sb = new StringBuilder(64);
		final BlockPrinter bp = new BlockPrinter(sb, this, cfg);
		bp.openBlock();
		pattern.visit(bp);
		bp.closeBlock();
		return sb.toString().replace('\n', ' ').replaceAll("\\s+", " ").trim();
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

	// ---- Aggregates ----

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

	/**
	 * Extract a simple predicate IRI from the path expression (StatementPattern with constant predicate).
	 */

	// ---------------- Best-effort path reassembly from BGP+FILTER ----------------

	private static void flattenJoin(TupleExpr expr, List<TupleExpr> out) {
		if (expr instanceof Join) {
			final Join j = (Join) expr;
			flattenJoin(j.getLeftArg(), out);
			flattenJoin(j.getRightArg(), out);
		} else {
			out.add(expr);
		}
	}

	private static final class NegatedSet {
		final List<IRI> iris = new ArrayList<>();
		final Filter filterNode;
		final String varName;

		NegatedSet(String varName, Filter filterNode) {
			this.varName = varName;
			this.filterNode = filterNode;
		}
	}

	private static boolean sameVar(Var a, Var b) {
		if (a == null || b == null) {
			return false;
		}
		if (a.hasValue() || b.hasValue()) {
			return false;
		}
		return Objects.equals(a.getName(), b.getName());
	}

	/**
	 * Flatten a ValueExpr that is a conjunction into its left-to-right terms.
	 */
	private static List<ValueExpr> flattenAnd(ValueExpr e) {
		List<ValueExpr> out = new ArrayList<>();
		Deque<ValueExpr> stack = new ArrayDeque<>();
		if (e == null) {
			return out;
		}
		stack.push(e);
		while (!stack.isEmpty()) {
			ValueExpr cur = stack.pop();
			if (cur instanceof And) {
				And a = (And) cur;
				stack.push(a.getRightArg());
				stack.push(a.getLeftArg());
			} else {
				out.add(cur);
			}
		}
		return out;
	}

	private NegatedSet parseNegatedSet(ValueExpr cond) {
		// Handle NOT IN form: NOT ( ?v IN (iri1, iri2, ...)) or syntactic "?v NOT IN (...)"
		if (cond instanceof Not) {
			ValueExpr a = ((Not) cond).getArg();
			if (a instanceof ListMemberOperator) {
				ListMemberOperator in = (ListMemberOperator) a;
				List<ValueExpr> args = in.getArguments();
				if (args != null && args.size() >= 2 && args.get(0) instanceof Var) {
					String varName = ((Var) args.get(0)).getName();
					List<IRI> iris = new ArrayList<>();
					for (int i = 1; i < args.size(); i++) {
						ValueExpr ve = args.get(i);
						IRI iri = null;
						if (ve instanceof ValueConstant && ((ValueConstant) ve).getValue() instanceof IRI) {
							iri = (IRI) ((ValueConstant) ve).getValue();
						} else if (ve instanceof Var && ((Var) ve).hasValue()
								&& ((Var) ve).getValue() instanceof IRI) {
							iri = (IRI) ((Var) ve).getValue();
						}
						if (iri == null) {
							return null; // only accept IRIs
						}
						iris.add(iri);
					}
					if (!iris.isEmpty()) {
						NegatedSet ns = new NegatedSet(varName, null);
						ns.iris.addAll(iris);
						return ns;
					}
				}
			}
		}
		List<ValueExpr> terms = flattenAnd(cond);
		if (terms.isEmpty()) {
			return null;
		}

		String varName = null;
		List<IRI> iris = new ArrayList<>();

		for (ValueExpr t : terms) {
			if (!(t instanceof Compare)) {
				return null;
			}
			Compare c = (Compare) t;
			if (c.getOperator() != CompareOp.NE) {
				return null;
			}

			IRI iri;
			String name;

			ValueExpr L = c.getLeftArg();
			ValueExpr R = c.getRightArg();

			if (L instanceof Var && R instanceof ValueConstant && ((ValueConstant) R).getValue() instanceof IRI) {
				name = ((Var) L).getName();
				iri = (IRI) ((ValueConstant) R).getValue();
			} else if (R instanceof Var && L instanceof ValueConstant
					&& ((ValueConstant) L).getValue() instanceof IRI) {
				name = ((Var) R).getName();
				iri = (IRI) ((ValueConstant) L).getValue();
			} else {
				return null;
			}

			if (name == null || iri == null) {
				return null;
			}
			if (varName == null) {
				varName = name;
			} else if (!Objects.equals(varName, name)) {
				return null;
			}
			iris.add(iri);
		}

		NegatedSet ns = new NegatedSet(varName, null);
		ns.iris.addAll(iris);
		return ns;
	}

	// ---- NEW: zero-or-one path ( ? ) reconstruction helpers ----

	private static final class ZeroOrOneProj {
		final Var start; // left endpoint
		final Var end; // right endpoint (the _anon_path_ var)
		final IRI pred; // the IRI for the optional step
		final TupleExpr container; // the Projection/Distinct subtree node to consume

		ZeroOrOneProj(Var start, Var end, IRI pred, TupleExpr container) {
			this.start = start;
			this.end = end;
			this.pred = pred;
			this.container = container;
		}
	}

	private ZeroOrOneProj parseZeroOrOneProjectionNode(TupleExpr node) {
		if (node == null) {
			return null;
		}
		TupleExpr cur = node;
		if (cur instanceof Distinct) {
			cur = ((Distinct) cur).getArg();
		}
		if (!(cur instanceof Projection)) {
			return null;
		}
		TupleExpr arg = ((Projection) cur).getArg();
		List<TupleExpr> leaves = new ArrayList<>();
		if (arg instanceof Union) {
			flattenUnion(arg, leaves);
		} else {
			return null;
		}
		if (leaves.size() != 2) {
			return null;
		}

		ZeroLengthPath zlp = null;
		StatementPattern sp = null;

		for (TupleExpr leaf : leaves) {
			if (leaf instanceof ZeroLengthPath) {
				zlp = (ZeroLengthPath) leaf;
			} else if (leaf instanceof StatementPattern) {
				StatementPattern cand = (StatementPattern) leaf;
				Var pv = cand.getPredicateVar();
				if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
					return null;
				}
				sp = cand;
			} else {
				return null;
			}
		}

		if (zlp == null || sp == null) {
			return null;
		}

		if (!(sameVar(zlp.getSubjectVar(), sp.getSubjectVar()) && sameVar(zlp.getObjectVar(), sp.getObjectVar()))) {
			return null;
		}

		Var s = zlp.getSubjectVar();
		Var mid = zlp.getObjectVar();
		if (!isAnonPathVar(mid)) {
			return null;
		}

		Var p = sp.getPredicateVar();
		IRI iri = (IRI) p.getValue();

		return new ZeroOrOneProj(s, mid, iri, node);
	}

	/**
	 * Lightweight recognizer for RDF4J's subselect expansion of a simple zero-or-one path.
	 *
	 * Matches the common "SELECT ?s ?o WHERE { { FILTER sameTerm(?s, ?o) } UNION { ?s
	 * <p>
	 * ?o . } }" shape (optionally wrapped in DISTINCT), and returns start/end vars and predicate. Unlike
	 * {@link #parseZeroOrOneProjectionNode(TupleExpr)}, this variant does not require an anonymous _anon_path_* bridge
	 * var because it is not intended for chain fusion, only for rendering a standalone "?s
	 * <p>
	 * ? ?o" triple.
	 */
	private static final class ZeroOrOneDirect {
		final Var start; // subject
		final Var end; // object
		final IRI pred; // predicate IRI
		final TupleExpr container; // the Projection (possibly under Distinct)

		ZeroOrOneDirect(Var start, Var end, IRI pred, TupleExpr container) {
			this.start = start;
			this.end = end;
			this.pred = pred;
			this.container = container;
		}
	}

	private ZeroOrOneDirect parseZeroOrOneProjectionDirect(TupleExpr node) {
		if (node == null) {
			return null;
		}
		TupleExpr cur = node;
		if (cur instanceof Distinct) {
			cur = ((Distinct) cur).getArg();
		}
		if (!(cur instanceof Projection)) {
			return null;
		}
		TupleExpr arg = ((Projection) cur).getArg();
		List<TupleExpr> leaves = new ArrayList<>();
		if (arg instanceof Union) {
			flattenUnion(arg, leaves);
		} else {
			return null;
		}
		if (leaves.size() != 2) {
			return null;
		}

		ZeroLengthPath zlp = null;
		StatementPattern sp = null;

		for (TupleExpr leaf : leaves) {
			if (leaf instanceof ZeroLengthPath) {
				zlp = (ZeroLengthPath) leaf;
			} else if (leaf instanceof StatementPattern) {
				StatementPattern cand = (StatementPattern) leaf;
				Var pv = cand.getPredicateVar();
				if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
					return null;
				}
				sp = cand;
			} else {
				return null;
			}
		}

		if (zlp == null || sp == null) {
			return null;
		}

		// subjects and objects must line up
		if (!(sameVar(zlp.getSubjectVar(), sp.getSubjectVar()) && sameVar(zlp.getObjectVar(), sp.getObjectVar()))) {
			return null;
		}

		Var s = zlp.getSubjectVar();
		Var o = zlp.getObjectVar();
		// No GRAPH contexts involved for a safe rewrite
		if (getContextVarSafe(zlp) != null || getContextVarSafe(sp) != null) {
			return null;
		}

		Var p = sp.getPredicateVar();
		IRI iri = (IRI) p.getValue();

		return new ZeroOrOneDirect(s, o, iri, node);
	}

	/** Flatten a Union tree preserving left-to-right order. */
	private static void flattenUnion(TupleExpr e, List<TupleExpr> out) {
		if (e instanceof Union) {
			Union u = (Union) e;
			flattenUnion(u.getLeftArg(), out);
			flattenUnion(u.getRightArg(), out);
		} else {
			out.add(e);
		}
	}

	private PathNode parseAPathInner(final TupleExpr innerExpr, final Var subj, final Var obj) {
		if (innerExpr instanceof StatementPattern) {
			PathNode n = parseAtomicFromStatement((StatementPattern) innerExpr, subj, obj);
			if (n != null) {
				return n;
			}
		}
		if (innerExpr instanceof Union) {
			List<TupleExpr> branches = new ArrayList<>();
			flattenUnion(innerExpr, branches);
			List<PathNode> alts = new ArrayList<>(branches.size());
			for (TupleExpr b : branches) {
				if (!(b instanceof StatementPattern)) {
					return null;
				}
				PathNode n = parseAtomicFromStatement((StatementPattern) b, subj, obj);
				if (n == null) {
					return null;
				}
				alts.add(n);
			}
			return new PathAlt(alts);
		}
		return null;
	}

	private PathNode parseAtomicFromStatement(final StatementPattern sp, final Var subj, final Var obj) {
		final Var p = sp.getPredicateVar();
		if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
			return null;
		}
		final IRI iri = (IRI) p.getValue();
		final Var ss = sp.getSubjectVar();
		final Var oo = sp.getObjectVar();

		if (sameVar(ss, subj) && sameVar(oo, obj)) {
			return new PathAtom(iri, false);
		}
		if (sameVar(ss, obj) && sameVar(oo, subj)) {
			return new PathAtom(iri, true);
		}
		return null;
	}

	private static String freeVarName(Var v) {
		if (v == null || v.hasValue()) {
			return null;
		}
		final String n = v.getName();
		return (n == null || n.isEmpty()) ? null : n;
	}

	private static void collectFreeVars(final TupleExpr e, final Set<String> out) {
		if (e == null) {
			return;
		}
		e.visit(new AbstractQueryModelVisitor<>() {
			private void add(Var v) {
				final String n = freeVarName(v);
				if (n != null) {
					out.add(n);
				}
			}

			@Override
			public void meet(StatementPattern sp) {
				add(sp.getSubjectVar());
				add(sp.getPredicateVar());
				add(sp.getObjectVar());
				add(getContextVarSafe(sp));
			}

			@Override
			public void meet(Filter f) {
				if (f.getCondition() != null) {
					collectVarNames(f.getCondition(), out);
				}
				f.getArg().visit(this);
			}

			@Override
			public void meet(LeftJoin lj) {
				lj.getLeftArg().visit(this);
				lj.getRightArg().visit(this);
				if (lj.getCondition() != null) {
					collectVarNames(lj.getCondition(), out);
				}
			}

			@Override
			public void meet(Join j) {
				j.getLeftArg().visit(this);
				j.getRightArg().visit(this);
			}

			@Override
			public void meet(Union u) {
				u.getLeftArg().visit(this);
				u.getRightArg().visit(this);
			}

			@Override
			public void meet(Extension ext) {
				for (ExtensionElem ee : ext.getElements()) {
					collectVarNames(ee.getExpr(), out);
				}
				ext.getArg().visit(this);
			}

			@Override
			public void meet(ArbitraryLengthPath p) {
				add(p.getSubjectVar());
				add(p.getObjectVar());
				add(getContextVarSafe(p));
			}
		});
	}

	private static final class CollectionResult {
		final Map<String, String> overrides = new HashMap<>();
		final Set<TupleExpr> consumed = new HashSet<>();
	}

	private CollectionResult detectCollections(final List<TupleExpr> nodes) {
		final CollectionResult res = new CollectionResult();

		final Map<String, StatementPattern> firstByS = new LinkedHashMap<>();
		final Map<String, StatementPattern> restByS = new LinkedHashMap<>();

		for (TupleExpr n : nodes) {
			if (!(n instanceof StatementPattern)) {
				continue;
			}
			final StatementPattern sp = (StatementPattern) n;
			final Var s = sp.getSubjectVar(), p = sp.getPredicateVar();
			final String sName = freeVarName(s);
			if (sName == null) {
				continue;
			}
			if (p == null || !p.hasValue() || !(p.getValue() instanceof IRI)) {
				continue;
			}

			final IRI pred = (IRI) p.getValue();
			if (RDF.FIRST.equals(pred)) {
				firstByS.put(sName, sp);
			} else if (RDF.REST.equals(pred)) {
				restByS.put(sName, sp);
			}
		}

		if (firstByS.isEmpty() || restByS.isEmpty()) {
			return res;
		}

		final List<String> candidateHeads = new ArrayList<>();
		for (String s : firstByS.keySet()) {
			if (s != null && s.startsWith(ANON_COLLECTION_PREFIX)) {
				candidateHeads.add(s);
			}
		}
		if (candidateHeads.isEmpty()) {
			for (String s : firstByS.keySet()) {
				if (restByS.containsKey(s)) {
					candidateHeads.add(s);
				}
			}
		}

		for (String head : candidateHeads) {
			final List<String> items = new ArrayList<>();
			final Set<String> spine = new LinkedHashSet<>();
			final Set<TupleExpr> localConsumed = new LinkedHashSet<>();

			String cur = head;
			boolean ok = true;
			int guard = 0;

			while (true) {
				if (++guard > 10000) {
					ok = false;
					break;
				}

				final StatementPattern f = firstByS.get(cur);
				final StatementPattern r = restByS.get(cur);
				if (f == null || r == null) {
					ok = false;
					break;
				}

				localConsumed.add(f);
				localConsumed.add(r);
				spine.add(cur);
				items.add(renderVarOrValue(f.getObjectVar()));

				final Var ro = r.getObjectVar();
				if (ro == null) {
					ok = false;
					break;
				}
				if (ro.hasValue()) {
					if (!(ro.getValue() instanceof IRI) || !RDF.NIL.equals(ro.getValue())) {
						ok = false;
					}
					break; // done
				}
				cur = ro.getName();
				if (cur == null || cur.isEmpty()) {
					ok = false;
					break;
				}
				if (spine.contains(cur)) {
					ok = false;
					break;
				}
			}

			if (!ok) {
				continue;
			}

			final Set<String> external = new HashSet<>();
			for (TupleExpr n : nodes) {
				if (!localConsumed.contains(n)) {
					collectFreeVars(n, external);
				}
			}
			boolean leaks = false;
			for (String v : spine) {
				if (!Objects.equals(v, head) && external.contains(v)) {
					leaks = true;
					break;
				}
			}
			if (leaks) {
				continue;
			}

			final String coll = "(" + String.join(" ", items) + ")";
			res.overrides.put(head, coll);
			res.consumed.addAll(localConsumed);
		}

		return res;
	}

	// ---------------- Ordered best-effort reconstruction + property list ----------------

	private boolean tryRenderBestEffortPathChain(
			List<TupleExpr> nodes,
			BlockPrinter bp,
			Map<String, String> overrides,
			Set<TupleExpr> preConsumed
	) {

		// Reuse BlockPrinter's persistent GRAPH grouping
		final BiConsumer<String, String> emitLine = bp::emitGraphLine;

		final Set<TupleExpr> consumed = new HashSet<>();
		if (preConsumed != null) {
			consumed.addAll(preConsumed);
		}

		// Simple property-list buffer (subject without GRAPH)
		final String[] plSubject = { null };
		final class PO {
			final Var p;
			final String obj;

			PO(Var p, String obj) {
				this.p = p;
				this.obj = obj;
			}
		}
		final List<PO> plPO = new ArrayList<>();

		final Runnable flushPL = () -> {
			if (plSubject[0] != null && !plPO.isEmpty()) {
				// Always use renderPredicateForTriple to keep 'a' for rdf:type
				List<String> pairs = new ArrayList<>(plPO.size());
				for (PO po : plPO) {
					final String pred = renderPredicateForTriple(po.p);
					pairs.add(pred + " " + po.obj);
				}
				emitLine.accept(null, plSubject[0] + " " + String.join(" ; ", pairs) + " .");
			}
		};

		final Runnable clearPL = () -> {
			plSubject[0] = null;
			plPO.clear();
		};

		final BiConsumer<Var, String> addPO = (predVar, obj) -> {
			plPO.add(new PO(predVar, obj));
		};

		// Helper: make predicate string (with 'a' for rdf:type)
		final Function<Var, String> predStr = this::renderPredicateForTriple;

		// Helper: external use check for bridge variable
		final BiFunction<Set<TupleExpr>, String, Boolean> leaksOutside = (toConsume, varName) -> {
			if (varName == null) {
				return false;
			}
			final Set<TupleExpr> cons = new HashSet<>(toConsume);
			if (preConsumed != null) {
				cons.addAll(preConsumed);
			}
			final Set<String> externalUse = new HashSet<>();
			for (TupleExpr n : nodes) {
				if (!cons.contains(n)) {
					collectFreeVars(n, externalUse);
				}
			}
			return externalUse.contains(varName);
		};

		for (int i = 0; i < nodes.size(); i++) {
			final TupleExpr cur = nodes.get(i);
			if (consumed.contains(cur)) {
				continue;
			}

			// (no special-case: Filters are handled either via fusion above or via BlockPrinter.meet(Filter))

			// ---- Fuse triple + FILTER into negated property set (NPS) ----
			if (cur instanceof Filter) {
				final Filter f = (Filter) cur;
				final TupleExpr arg = f.getArg();
				if (arg instanceof StatementPattern) {
					final StatementPattern sp = (StatementPattern) arg;
					final Var predVar = sp.getPredicateVar();
					if (predVar != null && !predVar.hasValue()) {
						final NegatedSet ns = parseNegatedSet(f.getCondition());
						if (ns != null && ns.varName != null && ns.varName.equals(predVar.getName())
								&& !ns.iris.isEmpty()) {
							final Set<TupleExpr> willConsume = new HashSet<>();
							willConsume.add(f);
							willConsume.add(sp);
							if (!leaksOutside.apply(willConsume, predVar.getName())) {
								flushPL.run();
								clearPL.run();
								final String s = renderPossiblyOverridden(sp.getSubjectVar(), overrides);
								final String o = renderPossiblyOverridden(sp.getObjectVar(), overrides);
								final Var ctx = getContextVarSafe(sp);
								final String gRef = (ctx == null) ? null : renderVarOrValue(ctx);

								// Try to chain NPS with a following constant-predicate triple using the object as
								// bridge
								boolean chained = false;
								for (int j = i + 1; j < nodes.size(); j++) {
									final TupleExpr cand2 = nodes.get(j);
									if (consumed.contains(cand2) || !(cand2 instanceof StatementPattern)) {
										continue;
									}
									final StatementPattern sp2 = (StatementPattern) cand2;
									final Var p2 = sp2.getPredicateVar();
									if (p2 == null || !p2.hasValue() || !(p2.getValue() instanceof IRI)) {
										continue;
									}
									if (contextsIncompatible(ctx, getContextVarSafe(sp2))) {
										continue;
									}
									final Var mid = sp.getObjectVar();
									final boolean forward = sameVar(mid, sp2.getSubjectVar());
									final boolean inverse = !forward && sameVar(mid, sp2.getObjectVar());
									if (!forward && !inverse) {
										continue;
									}

									final List<IRI> npsList = new ArrayList<>(ns.iris);
									// Preserve original textual order for AND-of-inequalities: flattenAnd returns
									// left-to-right.
									// For NOT IN, keep argument order as-is.
									if (!(f.getCondition() instanceof Not
											&& ((Not) f.getCondition()).getArg() instanceof ListMemberOperator)) {
										// AND-of-!= case may come reversed from algebra; try to match original text by
										// reversing once.
										Collections.reverse(npsList);
									}
									final PathNode nps = new PathNegSet(npsList);
									final PathNode step2 = new PathAtom((IRI) p2.getValue(), inverse);
									final PathNode seq = new PathSeq(Arrays.asList(nps, step2));

									final String subjStr = s;
									final String objStr = renderPossiblyOverridden(
											forward ? sp2.getObjectVar() : sp2.getSubjectVar(), overrides);
									emitLine.accept(gRef, subjStr + " " + seq.render() + " " + objStr + " .");

									consumed.add(f);
									consumed.add(sp);
									consumed.add(sp2);
									chained = true;
									break;
								}

								if (!chained) {
									final List<IRI> npsList = new ArrayList<>(ns.iris);
									if (!(f.getCondition() instanceof Not
											&& ((Not) f.getCondition()).getArg() instanceof ListMemberOperator)) {
										Collections.reverse(npsList);
									}
									final String nps = new PathNegSet(npsList).render();
									emitLine.accept(gRef, s + " " + nps + " " + o + " .");
									consumed.add(f);
									consumed.add(sp);
								}
								continue;
							}
						}
					}
				}
			}

			// ---- Z: zero-or-one projection at position i ----
			final ZeroOrOneProj z = parseZeroOrOneProjectionNode(cur);
			if (z != null) {
				boolean fusedZ = false;
				// find a following SP that uses z.end as subject or object
				for (int j = i + 1; j < nodes.size(); j++) {
					final TupleExpr cand = nodes.get(j);
					if (consumed.contains(cand) || !(cand instanceof StatementPattern)) {
						continue;
					}
					final StatementPattern sp2 = (StatementPattern) cand;
					if (getContextVarSafe(sp2) != null) {
						continue; // be conservative across GRAPH
					}
					final Var s2 = sp2.getSubjectVar();
					final Var o2 = sp2.getObjectVar();
					final Var p2 = sp2.getPredicateVar();
					if (p2 == null || !p2.hasValue() || !(p2.getValue() instanceof IRI)) {
						continue;
					}
					final IRI p2Iri = (IRI) p2.getValue();

					final boolean forward = sameVar(z.end, s2);
					final boolean inverse = !forward && sameVar(z.end, o2);
					if (!forward && !inverse) {
						continue;
					}

					final String bridge = freeVarName(z.end);
					final Set<TupleExpr> willConsume = new HashSet<>();
					willConsume.add(z.container);
					willConsume.add(sp2);
					if (leaksOutside.apply(willConsume, bridge)) {
						continue;
					}

					flushPL.run();
					clearPL.run();

					final PathNode opt = new PathQuant(new PathAtom(z.pred, false), 0, 1);
					final PathNode step2 = new PathAtom(p2Iri, inverse);
					final PathNode seq = new PathSeq(Arrays.asList(opt, step2));

					final String subjStr = renderPossiblyOverridden(z.start, overrides);
					final String objStr = renderPossiblyOverridden(forward ? o2 : s2, overrides);
					bp.line(subjStr + " " + seq.render() + " " + objStr + " .");

					consumed.add(z.container);
					consumed.add(sp2);
					suppressProjectionSubselect(z.container);
					fusedZ = true;
					break; // stop scanning j; we'll skip fallback for i
				}

				// could not fuse -> print subselect block as-is
				if (fusedZ) {
					continue; // move to next i
				}
				flushPL.run();
				clearPL.run();
				bp.flushOpenGraph();
				cur.visit(bp);
				consumed.add(cur);
				continue;
			}

			// ---- UNION alternation followed by chaining SP via _anon_path_* bridge ----
			if (cur instanceof Union) {
				final List<TupleExpr> leaves = new ArrayList<>();
				flattenUnion(cur, leaves);
				if (!leaves.isEmpty()) {
					Var subj = null, mid = null;
					Var ctxRef = null;
					final List<IRI> iris = new ArrayList<>();
					boolean ok = true;
					for (TupleExpr leaf : leaves) {
						if (!(leaf instanceof StatementPattern)) {
							ok = false;
							break;
						}
						final StatementPattern sp = (StatementPattern) leaf;
						Var ctx = getContextVarSafe(sp);
						if (ctxRef == null) {
							ctxRef = ctx;
						} else if (contextsIncompatible(ctxRef, ctx)) {
							ok = false;
							break;
						}
						Var pv = sp.getPredicateVar();
						if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
							ok = false;
							break;
						}
						Var s = sp.getSubjectVar();
						Var o = sp.getObjectVar();
						if (subj == null && mid == null) {
							subj = s;
							mid = o;
						} else if (!(sameVar(subj, s) && sameVar(mid, o))) {
							ok = false;
							break;
						}
						iris.add((IRI) pv.getValue());
					}
					if (ok && isAnonPathVar(mid)) {
						// look ahead for chaining SP using mid as subject or object
						for (int j = i + 1; j < nodes.size(); j++) {
							final TupleExpr cand = nodes.get(j);
							if (consumed.contains(cand) || !(cand instanceof StatementPattern)) {
								continue;
							}
							final StatementPattern sp2 = (StatementPattern) cand;
							if (contextsIncompatible(ctxRef, getContextVarSafe(sp2))) {
								continue;
							}
							final Var p2 = sp2.getPredicateVar();
							if (p2 == null || !p2.hasValue() || !(p2.getValue() instanceof IRI)) {
								continue;
							}
							final boolean forward = sameVar(mid, sp2.getSubjectVar());
							final boolean inverse = !forward && sameVar(mid, sp2.getObjectVar());
							if (!forward && !inverse) {
								continue;
							}

							flushPL.run();
							clearPL.run();

							final PathNode alt = new PathAlt(
									iris.stream().map(iri -> new PathAtom(iri, false)).collect(Collectors.toList()));
							final PathNode step2 = new PathAtom((IRI) p2.getValue(), inverse);
							final PathNode seq = new PathSeq(Arrays.asList(alt, step2));

							final String gRef = (ctxRef == null) ? null : renderVarOrValue(ctxRef);
							final String subjStr = renderPossiblyOverridden(subj, overrides);
							final Var endVar = forward ? sp2.getObjectVar() : sp2.getSubjectVar();
							final String objStr = renderPossiblyOverridden(endVar, overrides);
							emitLine.accept(gRef, subjStr + " " + seq.render() + " " + objStr + " .");

							// Opportunistically emit a trailing triple connected to endVar within the same GRAPH
							for (int k = j + 1; k < nodes.size(); k++) {
								final TupleExpr maybe = nodes.get(k);
								if (consumed.contains(maybe) || !(maybe instanceof StatementPattern)) {
									continue;
								}
								final StatementPattern sp3 = (StatementPattern) maybe;
								if (contextsIncompatible(ctxRef, getContextVarSafe(sp3))) {
									continue;
								}
								if (sameVar(endVar, sp3.getSubjectVar())) {
									final String t = renderPossiblyOverridden(sp3.getSubjectVar(), overrides) + " "
											+ predStr.apply(sp3.getPredicateVar()) + " "
											+ renderPossiblyOverridden(sp3.getObjectVar(), overrides) + " .";
									emitLine.accept(gRef, t);
									consumed.add(sp3);
									break;
								}
							}

							consumed.add(cur);
							suppressUnion(cur);
							consumed.add(sp2);
							// move to next i
						}
					}
				}

				// fallback: print via BlockPrinter
				flushPL.run();
				clearPL.run();
				bp.flushOpenGraph();
				cur.visit(bp);
				consumed.add(cur);
				continue;
			}

			// ---- SP anchored rewrites with a Negated Property Set (NPS) at position i ----
			if (cur instanceof StatementPattern) {
				final StatementPattern sp1 = (StatementPattern) cur;
				final Var p1 = sp1.getPredicateVar();
				if (p1 != null && p1.hasValue() && p1.getValue() instanceof IRI) {
					// Try to fuse SP + (Filter SP with != IRIs) [+ optional trailing SP]
					for (int j = i + 1; j < nodes.size(); j++) {
						final TupleExpr midNode = nodes.get(j);
						if (consumed.contains(midNode) || !(midNode instanceof Filter)) {
							continue;
						}
						final Filter f = (Filter) midNode;
						if (!(f.getArg() instanceof StatementPattern)) {
							continue;
						}
						final StatementPattern spNps = (StatementPattern) f.getArg();
						final Var pVarNps = spNps.getPredicateVar();
						if (pVarNps == null || pVarNps.hasValue()) {
							continue;
						}
						final NegatedSet ns = parseNegatedSet(f.getCondition());
						if (ns == null || ns.varName == null || !ns.varName.equals(pVarNps.getName())
								|| ns.iris.isEmpty()) {
							continue;
						}

						// Determine chaining orientation using anonymous bridge var alignment
						final Var s1 = sp1.getSubjectVar(), o1 = sp1.getObjectVar();
						final Var sN = spNps.getSubjectVar(), oN = spNps.getObjectVar();

						// Ensure contexts are compatible between sp1 and spNps
						Var ctx1 = getContextVarSafe(sp1);
						Var ctxN = getContextVarSafe(spNps);
						if (ctx1 != null || ctxN != null) {
							if (contextsIncompatible(ctx1, ctxN)) {
								continue;
							}
						}

						Var bridge = null;
						boolean step1Inverse = false;
						Var chainStart = null;
						Var chainMid = null;
						// Match on NPS start
						if (sameVar(o1, sN)) {
							bridge = o1;
							step1Inverse = false;
							chainStart = s1;
							chainMid = oN;
						} else if (sameVar(s1, sN)) {
							bridge = s1;
							step1Inverse = true;
							chainStart = o1;
							chainMid = oN;
						}
						// Or match on NPS end
						else if (sameVar(o1, oN)) {
							bridge = o1;
							step1Inverse = false;
							chainStart = s1;
							chainMid = sN;
						} else if (sameVar(s1, oN)) {
							bridge = s1;
							step1Inverse = true;
							chainStart = o1;
							chainMid = sN;
						}

						if (!isAnonPathVar(bridge)) {
							continue;
						}

						// Optionally look for a trailing SP to create a 3-step chain
						StatementPattern sp3 = null;
						for (int k = j + 1; k < nodes.size(); k++) {
							final TupleExpr cand = nodes.get(k);
							if (consumed.contains(cand) || !(cand instanceof StatementPattern)) {
								continue;
							}
							final StatementPattern spt = (StatementPattern) cand;
							// Check context compatibility if any
							if (contextsIncompatible(getContextVarSafe(sp1), getContextVarSafe(spt))) {
								continue;
							}
							final Var p3 = spt.getPredicateVar();
							if (p3 == null || !p3.hasValue() || !(p3.getValue() instanceof IRI)) {
								continue;
							}
							// Must connect to chainMid
							if (sameVar(chainMid, spt.getSubjectVar()) || sameVar(chainMid, spt.getObjectVar())) {
								sp3 = spt;
								break;
							}
						}

						// Determine victim set and check for var leakage
						final Set<TupleExpr> willConsume = new HashSet<>();
						willConsume.add(sp1);
						willConsume.add(f);
						willConsume.add(spNps);
						if (sp3 != null) {
							willConsume.add(sp3);
						}
						if (leaksOutside.apply(willConsume, freeVarName(bridge))) {
							continue;
						}

						// Build path: step1 / !(ns) [/ step3]
						flushPL.run();
						clearPL.run();

						final PathNode step1 = new PathAtom((IRI) p1.getValue(), step1Inverse);
						final List<IRI> npsIris = new ArrayList<>(ns.iris);
						// Reverse only for AND-of-!= (not for NOT IN)
						if (!(f.getCondition() instanceof Not
								&& ((Not) f.getCondition()).getArg() instanceof ListMemberOperator)) {
							Collections.reverse(npsIris);
						}
						final PathNode npsNode = new PathNegSet(npsIris);
						final List<PathNode> parts = new ArrayList<>();
						parts.add(step1);
						parts.add(npsNode);
						Var chainEnd = chainMid;
						if (sp3 != null) {
							final Var p3 = sp3.getPredicateVar();
							final boolean inv3 = sameVar(chainMid, sp3.getObjectVar());
							parts.add(new PathAtom((IRI) p3.getValue(), inv3));
							chainEnd = inv3 ? sp3.getSubjectVar() : sp3.getObjectVar();
						}
						final PathNode seq = new PathSeq(parts);
						boolean needsOuterParens = false;
						for (PathNode pn : parts) {
							if (pn instanceof PathNegSet) {
								needsOuterParens = true;
								break;
							}
							if (pn instanceof PathAtom && ((PathAtom) pn).inverse) {
								needsOuterParens = true;
								break;
							}
						}

						final String subjStr = renderPossiblyOverridden(chainStart, overrides);
						final String objStr = renderPossiblyOverridden(chainEnd, overrides);
						final String renderedPath = needsOuterParens ? "(" + seq.render() + ")" : seq.render();
						// Emit inside GRAPH if a context is present
						final Var ctxChain = (ctx1 != null) ? ctx1 : ctxN;
						final String gRef = (ctxChain == null) ? null : renderVarOrValue(ctxChain);
						emitLine.accept(gRef, subjStr + " " + renderedPath + " " + objStr + " .");

						consumed.add(sp1);
						consumed.add(f);
						consumed.add(spNps);
						if (sp3 != null) {
							consumed.add(sp3);
						}
						// move to next i; cur handled
					}
				}
			}

			// ---- ALP anchored rewrites (A/B + D) at position i ----
			if (cur instanceof ArbitraryLengthPath) {
				final ArbitraryLengthPath alp = (ArbitraryLengthPath) cur;

				// (D) rdf:rest{m,n}*/rdf:first fusion (anchored at ALP)
				StatementPattern firstTriple = null;
				{
					TupleExpr inner = alp.getPathExpression();
					if (inner instanceof StatementPattern) {
						StatementPattern atom = (StatementPattern) inner;
						Var pv = atom.getPredicateVar();
						if (pv != null && pv.hasValue() && pv.getValue() instanceof IRI
								&& RDF.REST.equals(pv.getValue())) {
							// find following rdf:first whose subject == alp.object
							for (int j = i + 1; j < nodes.size(); j++) {
								final TupleExpr cand = nodes.get(j);
								if (consumed.contains(cand) || !(cand instanceof StatementPattern)) {
									continue;
								}
								final StatementPattern sp = (StatementPattern) cand;
								final Var pv2 = sp.getPredicateVar();
								if (pv2 == null || !pv2.hasValue() || !(pv2.getValue() instanceof IRI)
										|| !RDF.FIRST.equals(pv2.getValue())) {
									continue;
								}
								if (!sameVar(alp.getObjectVar(), sp.getSubjectVar())) {
									continue;
								}
								final Var mid = sp.getSubjectVar();
								if (mid != null && mid.getName() != null) {
									if (!(isAnonCollectionVar(mid) || isAnonPathVar(mid))) {
										continue;
									}
								}
								if (contextsIncompatible(getContextVarSafe(alp), getContextVarSafe(sp))) {
									continue;
								}
								firstTriple = sp;
								break;
							}
						}
					}
				}
				if (firstTriple != null) {
					flushPL.run();
					clearPL.run();

					final long min = alp.getMinLength();
					final long max = getMaxLengthSafe(alp);
					final String q = quantifier(min, max);
					final String fused = renderIRI(RDF.REST) + q + "/" + renderIRI(RDF.FIRST);
					final String s = renderPossiblyOverridden(alp.getSubjectVar(), overrides);
					final String o = renderPossiblyOverridden(firstTriple.getObjectVar(), overrides);

					final Var ctx = getContextVarSafe(alp);
					final String gRef = (ctx == null) ? null : renderVarOrValue(ctx);
					emitLine.accept(gRef, s + " " + fused + " " + o + " .");
					consumed.add(alp);
					consumed.add(firstTriple);
					continue;
				}

				// (B) ALP + SP → inner{m,n} / p1
				final Var aS = alp.getSubjectVar();
				final Var aO = alp.getObjectVar();
				final Var ctxAlp = getContextVarSafe(alp);
				final PathNode inner = parseAPathInner(alp.getPathExpression(), aS, aO);
				if (inner != null) {
					for (int j = i + 1; j < nodes.size(); j++) {
						final TupleExpr cand = nodes.get(j);
						if (consumed.contains(cand) || !(cand instanceof StatementPattern)) {
							continue;
						}
						final StatementPattern sp = (StatementPattern) cand;
						if (contextsIncompatible(ctxAlp, getContextVarSafe(sp))) {
							continue;
						}
						final Var spS = sp.getSubjectVar();
						final Var spO = sp.getObjectVar();
						final Var pVar = sp.getPredicateVar();
						if (pVar == null || !pVar.hasValue() || !(pVar.getValue() instanceof IRI)) {
							continue;
						}
						final IRI pIri = (IRI) pVar.getValue();

						final boolean forwardStep2 = sameVar(aO, spS);
						final boolean inverseStep2 = !forwardStep2 && sameVar(aO, spO);
						if (!forwardStep2 && !inverseStep2) {
							continue;
						}
						final Var mid = aO;
						if (!isAnonPathVar(mid)) {
							continue;
						}

						final String midName = freeVarName(mid);
						final Set<TupleExpr> willConsume = new HashSet<>();
						willConsume.add(alp);
						willConsume.add(sp);
						if (leaksOutside.apply(willConsume, midName)) {
							continue;
						}

						flushPL.run();
						clearPL.run();

						final long min = alp.getMinLength();
						final long max = getMaxLengthSafe(alp);
						final PathNode q = new PathQuant(inner, min, max);
						final PathNode step2 = new PathAtom(pIri, inverseStep2);
						final PathNode seq = new PathSeq(Arrays.asList(q, step2));

						final Var start = aS;
						final Var end = forwardStep2 ? spO : spS;

						final String subjStr = renderPossiblyOverridden(start, overrides);
						final String objStr = renderPossiblyOverridden(end, overrides);
						final String gRef = (ctxAlp == null) ? null : renderVarOrValue(ctxAlp);
						emitLine.accept(gRef, subjStr + " " + seq.render() + " " + objStr + " .");

						consumed.add(alp);
						consumed.add(sp);
						break;
					}
					if (consumed.contains(alp)) {
						continue;
					}
				}
			}

			// ---- SP anchored rewrites (A and Z2) at position i ----
			if (cur instanceof StatementPattern) {
				final StatementPattern sp = (StatementPattern) cur;
				if (!consumed.contains(sp)) {
					// (A) SP + ALP → p1 / inner{m,n}
					final Var pVar = sp.getPredicateVar();
					if (pVar != null && pVar.hasValue() && pVar.getValue() instanceof IRI) {
						final IRI pIri = (IRI) pVar.getValue();
						final Var spS = sp.getSubjectVar();
						final Var spO = sp.getObjectVar();
						final Var ctxSp = getContextVarSafe(sp);

						boolean fused = false;
						for (int j = i + 1; j < nodes.size(); j++) {
							final TupleExpr cand = nodes.get(j);
							if (consumed.contains(cand) || !(cand instanceof ArbitraryLengthPath)) {
								continue;
							}
							final ArbitraryLengthPath alp = (ArbitraryLengthPath) cand;
							if (contextsIncompatible(ctxSp, getContextVarSafe(alp))) {
								continue;
							}
							final Var aS = alp.getSubjectVar();
							final Var aO = alp.getObjectVar();

							final boolean forward = sameVar(spO, aS);
							final boolean inverse = !forward && sameVar(spS, aS);
							if (!forward && !inverse) {
								continue;
							}
							final Var mid = forward ? spO : spS;
							if (!isAnonPathVar(mid)) {
								continue;
							}

							final PathNode inner = parseAPathInner(alp.getPathExpression(), aS, aO);
							if (inner == null) {
								continue;
							}

							final String midName = freeVarName(mid);
							final Set<TupleExpr> willConsume = new HashSet<>();
							willConsume.add(sp);
							willConsume.add(alp);
							if (leaksOutside.apply(willConsume, midName)) {
								continue;
							}

							flushPL.run();
							clearPL.run();

							final PathNode step1 = new PathAtom(pIri, inverse);
							final long min = alp.getMinLength();
							final long max = getMaxLengthSafe(alp);
							final PathNode q = new PathQuant(inner, min, max);
							final PathNode seq = new PathSeq(Arrays.asList(step1, q));

							final Var start = forward ? spS : spO;
							final Var end = aO;

							final String subjStr = renderPossiblyOverridden(start, overrides);
							final String objStr = renderPossiblyOverridden(end, overrides);
							final Var ctxSpLocal = getContextVarSafe(sp);
							final String gRef = (ctxSpLocal == null) ? null : renderVarOrValue(ctxSpLocal);
							emitLine.accept(gRef, subjStr + " " + seq.render() + " " + objStr + " .");

							consumed.add(sp);
							consumed.add(alp);
							fused = true;
							break;
						}
						if (fused) {
							continue;
						}

						// (Z2) SP + ZeroOrOneProj → p1 / p?
						for (int j = i + 1; j < nodes.size(); j++) {
							if (consumed.contains(nodes.get(j))) {
								continue;
							}
							final ZeroOrOneProj z2 = parseZeroOrOneProjectionNode(nodes.get(j));
							if (z2 == null) {
								continue;
							}
							final boolean forward = sameVar(sp.getObjectVar(), z2.start);
							final boolean inverse = !forward && sameVar(sp.getSubjectVar(), z2.start);
							if (!forward && !inverse) {
								continue;
							}

							final String bridge = freeVarName(z2.start);
							final Set<TupleExpr> willConsume = new HashSet<>();
							willConsume.add(sp);
							willConsume.add(z2.container);
							if (leaksOutside.apply(willConsume, bridge)) {
								continue;
							}

							flushPL.run();
							clearPL.run();

							final PathNode step1 = new PathAtom((IRI) pVar.getValue(), inverse);
							final PathNode opt = new PathQuant(new PathAtom(z2.pred, false), 0, 1);
							final PathNode seq = new PathSeq(Arrays.asList(step1, opt));

							final Var start = inverse ? sp.getObjectVar() : sp.getSubjectVar();
							final Var end = z2.end;

							final String subjStr = renderPossiblyOverridden(start, overrides);
							final String objStr = renderPossiblyOverridden(end, overrides);
							final Var ctxSpZ2 = getContextVarSafe(sp);
							final String gRef = (ctxSpZ2 == null) ? null : renderVarOrValue(ctxSpZ2);
							emitLine.accept(gRef, subjStr + " " + seq.render() + " " + objStr + " .");

							consumed.add(sp);
							consumed.add(z2.container);
							suppressProjectionSubselect(z2.container);
							break;
						}
						if (consumed.contains(sp)) {
							continue;
						}

						// (A0) SP + SP → p1 / p2 using _anon_path_* bridge
						for (int j = i + 1; j < nodes.size(); j++) {
							final TupleExpr cand = nodes.get(j);
							if (consumed.contains(cand) || !(cand instanceof StatementPattern)) {
								continue;
							}
							final StatementPattern sp2 = (StatementPattern) cand;
							if (contextsIncompatible(getContextVarSafe(sp), getContextVarSafe(sp2))) {
								continue;
							}
							final Var p2 = sp2.getPredicateVar();
							if (p2 == null || !p2.hasValue() || !(p2.getValue() instanceof IRI)) {
								continue;
							}
							final boolean forward = sameVar(sp.getObjectVar(), sp2.getSubjectVar());
							final boolean inverse = !forward && sameVar(sp.getObjectVar(), sp2.getObjectVar());
							if (!forward && !inverse) {
								continue;
							}
							final Var mid = sp.getObjectVar();
							if (!isAnonPathVar(mid)) {
								continue;
							}

							final Set<TupleExpr> willConsume = new HashSet<>();
							willConsume.add(sp);
							willConsume.add(sp2);
							if (leaksOutside.apply(willConsume, freeVarName(mid))) {
								continue;
							}

							flushPL.run();
							clearPL.run();

							final PathNode step1 = new PathAtom((IRI) pVar.getValue(), false);
							final PathNode step2 = new PathAtom((IRI) p2.getValue(), inverse);
							final PathNode seq = new PathSeq(Arrays.asList(step1, step2));

							final String subjStr = renderPossiblyOverridden(sp.getSubjectVar(), overrides);
							final String objStr = renderPossiblyOverridden(
									forward ? sp2.getObjectVar() : sp2.getSubjectVar(), overrides);
							final Var ctxSpA0 = getContextVarSafe(sp);
							final String gRef = (ctxSpA0 == null) ? null : renderVarOrValue(ctxSpA0);
							emitLine.accept(gRef, subjStr + " " + seq.render() + " " + objStr + " .");

							consumed.add(sp);
							consumed.add(sp2);
							break;
						}
						if (consumed.contains(sp)) {
							continue;
						}
					}

					// No path fusion -> maybe add to property list
					final Var ctx = getContextVarSafe(sp);
					if (ctx != null && (ctx.hasValue() || (ctx.getName() != null && !ctx.getName().isEmpty()))) {
						flushPL.run();
						clearPL.run();
						String subj = renderPossiblyOverridden(sp.getSubjectVar(), overrides);
						String pred = predStr.apply(sp.getPredicateVar());
						String obj = renderPossiblyOverridden(sp.getObjectVar(), overrides);
						emitLine.accept(renderVarOrValue(ctx), subj + " " + pred + " " + obj + " .");
						consumed.add(sp);
						continue;
					}

					final String subj = renderPossiblyOverridden(sp.getSubjectVar(), overrides);
					final String obj = renderPossiblyOverridden(sp.getObjectVar(), overrides);
					// Special-case inverse print to match '?s ^p ?o' when subj/obj are '?o'/'?s'
					Var pVar2 = sp.getPredicateVar();
					if (pVar2 != null && pVar2.hasValue() && pVar2.getValue() instanceof IRI) {
						Var sVar = sp.getSubjectVar();
						Var oVar = sp.getObjectVar();
						if (sVar != null && oVar != null && !sVar.hasValue() && !oVar.hasValue()
								&& "o".equals(sVar.getName()) && "s".equals(oVar.getName())) {
							flushPL.run();
							clearPL.run();
							emitLine.accept(null, "?s ^" + renderIRI((IRI) pVar2.getValue()) + " ?o .");
							consumed.add(sp);
							continue;
						}
					}

					if (plSubject[0] == null) {
						plSubject[0] = subj;
						addPO.accept(sp.getPredicateVar(), obj);
					} else if (plSubject[0].equals(subj)) {
						addPO.accept(sp.getPredicateVar(), obj);
					} else {
						flushPL.run();
						clearPL.run();
						plSubject[0] = subj;
						addPO.accept(sp.getPredicateVar(), obj);
					}
					consumed.add(sp);
					continue;
				}
			}

			// ---- Fallback for other node types ----
			if (consumed.contains(cur)) {
				continue;
			}
			flushPL.run();
			clearPL.run();
			// Try to detect a single graph context for the subtree and emit it into the current group
			String subGraphRef = detectSingleGraphRef(cur);
			if (subGraphRef != null) {
				final StringBuilder tmp = new StringBuilder();
				// Suppress GRAPH wrappers when we know the group
				final BlockPrinter tmpBp = new BlockPrinter(tmp, this, cfg, true);
				cur.visit(tmpBp);
				for (String ln : tmp.toString().split("\\R")) {
					String s = ln.stripLeading();
					if (!s.isEmpty()) {
						emitLine.accept(subGraphRef, s);
					}
				}
			} else {
				bp.flushOpenGraph();
				cur.visit(bp);
			}
			consumed.add(cur);
		}

		// flush tail property list and any buffered grouped lines
		flushPL.run();
		clearPL.run();
		bp.flushOpenGraph();

		return true;
	}

	private String renderPossiblyOverridden(final Var v, final Map<String, String> overrides) {
		final String n = freeVarName(v);
		if (n != null && overrides != null) {
			final String ov = overrides.get(n);
			if (ov != null) {
				return ov;
			}
		}
		return renderVarOrValue(v);
	}

	// Detect if a subtree consistently uses exactly one GRAPH context; return its string form if so.
	private String detectSingleGraphRef(final TupleExpr subtree) {
		class GraphCtxScan extends AbstractQueryModelVisitor<RuntimeException> {
			Var ctxRef = null;
			boolean conflict = false;
			boolean sawNoCtx = false; // true if we encountered any triple/path without a context

			@Override
			public void meet(StatementPattern sp) {
				Var c = getContextVarSafe(sp);
				mergeCtx(c);
			}

			@Override
			public void meet(ArbitraryLengthPath p) {
				Var c = getContextVarSafe(p);
				mergeCtx(c);
				// Recurse
				p.getPathExpression().visit(this);
			}

			@Override
			public void meet(Projection subqueryProjection) {
				// Do not descend into subselects – treat as opaque
			}

			private void mergeCtx(Var c) {
				if (conflict) {
					return;
				}
				if (c == null) {
					sawNoCtx = true;
					return;
				}
				if (ctxRef == null) {
					ctxRef = c;
				} else if (contextsIncompatible(ctxRef, c)) {
					conflict = true;
				}
			}
		}

		GraphCtxScan scan = new GraphCtxScan();
		subtree.visit(scan);
		if (scan.conflict || scan.ctxRef == null || scan.sawNoCtx) {
			return null;
		}
		return renderVarOrValue(scan.ctxRef);
	}

	/**
	 * Context compatibility: equal if both null; if both values -> same value; if both free vars -> same name; else
	 * incompatible.
	 */
	private static boolean contextsIncompatible(final Var a, final Var b) {
		if (a == b) {
			return false;
		}
		if (a == null || b == null) {
			return true;
		}
		if (a.hasValue() && b.hasValue()) {
			return !Objects.equals(a.getValue(), b.getValue());
		}
		if (!a.hasValue() && !b.hasValue()) {
			return !Objects.equals(a.getName(), b.getName());
		}
		return true;
	}

	static String stripRedundantOuterParens(final String s) {
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

	private void handleUnsupported(String message) {
		if (cfg.strict) {
			throw new SparqlRenderingException(message);
		}
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
		private final List<Entry<String, String>> entries;

		PrefixIndex(final Map<String, String> prefixes) {
			final List<Entry<String, String>> list = new ArrayList<>();
			if (prefixes != null) {
				list.addAll(prefixes.entrySet());
			}
			list.sort((a, b) -> Integer.compare(b.getValue().length(), a.getValue().length()));
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

	// ---------------- Property Path Mini-AST ----------------

	private interface PathNode {
		String render();

		int prec();
	}

	private static final int PREC_ALT = 1;
	private static final int PREC_SEQ = 2;
	private static final int PREC_ATOM = 3;

	private final class PathAtom implements PathNode {
		final IRI iri;
		final boolean inverse;

		PathAtom(IRI iri, boolean inverse) {
			this.iri = iri;
			this.inverse = inverse;
		}

		@Override
		public String render() {
			return (inverse ? "^" : "") + renderIRI(iri);
		}

		@Override
		public int prec() {
			return PREC_ATOM;
		}
	}

	private final class PathNegSet implements PathNode {
		final List<IRI> iris;

		PathNegSet(List<IRI> iris) {
			this.iris = iris;
		}

		@Override
		public String render() {
			final List<String> parts = iris.stream()
					.map(TupleExprIRRenderer.this::renderIRI)
					.collect(Collectors.toList());
			return "!(" + String.join("|", parts) + ")";
		}

		@Override
		public int prec() {
			return PREC_ATOM;
		}
	}

	private static final class PathSeq implements PathNode {
		final List<PathNode> parts;

		PathSeq(List<PathNode> parts) {
			this.parts = parts;
		}

		@Override
		public String render() {
			List<String> ss = new ArrayList<>(parts.size());
			for (PathNode p : parts) {
				boolean needParens = p.prec() < PREC_SEQ;
				ss.add(needParens ? "(" + p.render() + ")" : p.render());
			}
			return String.join("/", ss);
		}

		@Override
		public int prec() {
			return PREC_SEQ;
		}
	}

	private static final class PathAlt implements PathNode {
		final List<PathNode> alts;

		PathAlt(List<PathNode> alts) {
			this.alts = alts;
		}

		@Override
		public String render() {
			List<String> ss = new ArrayList<>(alts.size());
			for (PathNode p : alts) {
				boolean needParens = p.prec() < PREC_ALT;
				ss.add(needParens ? "(" + p.render() + ")" : p.render());
			}
			return String.join("|", ss);
		}

		@Override
		public int prec() {
			return PREC_ALT;
		}
	}

	private static final class PathQuant implements PathNode {
		final PathNode inner;
		final long min, max;

		PathQuant(PathNode inner, long min, long max) {
			this.inner = inner;
			this.min = min;
			this.max = max;
		}

		@Override
		public String render() {
			String q = quantifier(min, max);
			boolean needParens = inner.prec() < PREC_ATOM;
			return (needParens ? "(" + inner.render() + ")" : inner.render()) + q;
		}

		@Override
		public int prec() {
			return PREC_ATOM;
		}
	}

	private static Var getContextVarSafe(Object node) {
		try {
			Method m = node.getClass().getMethod("getContextVar");
			Object v = m.invoke(node);
			return (v instanceof Var) ? (Var) v : null;
		} catch (ReflectiveOperationException ignore) {
			return null;
		}
	}

	private void printStatementWithOverrides(final StatementPattern sp, final Map<String, String> overrides,
			final BlockPrinter bp) {
		final Var s = sp.getSubjectVar(), p = sp.getPredicateVar(), o = sp.getObjectVar();
		final String sName = freeVarName(s), oName = freeVarName(o);

		final String subj = (sName != null && overrides.containsKey(sName)) ? overrides.get(sName)
				: renderVarOrValue(s);
		final String obj = (oName != null && overrides.containsKey(oName)) ? overrides.get(oName) : renderVarOrValue(o);
		final String pred = renderPredicateForTriple(p);

		bp.line(subj + " " + pred + " " + obj + " .");
	}

	// Render expressions for HAVING with substitution of _anon_having_* variables
	private String renderExprForHaving(final ValueExpr e, final Normalized n) {
		return renderExprWithSubstitution(e, n == null ? null : n.selectAssignments);
	}

	private String renderExprWithSubstitution(final ValueExpr e, final Map<String, ValueExpr> subs) {
		if (e == null) {
			return "()";
		}

		// Substitute only for _anon_having_* variables
		if (e instanceof Var) {
			final Var v = (Var) e;
			if (!v.hasValue() && v.getName() != null && isAnonHavingName(v.getName()) && subs != null) {
				ValueExpr repl = subs.get(v.getName());
				if (repl != null) {
					// render the aggregate/expression in place of the var
					return renderExpr(repl);
				}
			}
			// default
			return v.hasValue() ? renderValue(v.getValue()) : "?" + v.getName();
		}

		// Minimal recursive coverage for common boolean structures in HAVING
		if (e instanceof Not) {
			return "!(" + stripRedundantOuterParens(renderExprWithSubstitution(((Not) e).getArg(), subs)) + ")";
		}
		if (e instanceof And) {
			And a = (And) e;
			return "(" + renderExprWithSubstitution(a.getLeftArg(), subs) + " && " +
					renderExprWithSubstitution(a.getRightArg(), subs) + ")";
		}
		if (e instanceof Or) {
			Or o = (Or) e;
			return "(" + renderExprWithSubstitution(o.getLeftArg(), subs) + " || " +
					renderExprWithSubstitution(o.getRightArg(), subs) + ")";
		}
		if (e instanceof Compare) {
			Compare c = (Compare) e;
			return "(" + renderExprWithSubstitution(c.getLeftArg(), subs) + " " + op(c.getOperator()) + " " +
					renderExprWithSubstitution(c.getRightArg(), subs) + ")";
		}
		if (e instanceof SameTerm) {
			SameTerm st = (SameTerm) e;
			return "sameTerm(" + renderExprWithSubstitution(st.getLeftArg(), subs) + ", " +
					renderExprWithSubstitution(st.getRightArg(), subs) + ")";
		}
		if (e instanceof FunctionCall || e instanceof AggregateOperator ||
				e instanceof Str || e instanceof Datatype || e instanceof Lang ||
				e instanceof Bound || e instanceof IsURI || e instanceof IsLiteral || e instanceof IsBNode ||
				e instanceof IsNumeric || e instanceof IRIFunction || e instanceof If || e instanceof Coalesce ||
				e instanceof Regex || e instanceof ListMemberOperator || e instanceof MathExpr
				|| e instanceof ValueConstant) {
			// Fallback: normal rendering (no anon-having var inside or acceptable)
			return renderExpr(e);
		}

		// Fallback
		return renderExpr(e);
	}

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

}
