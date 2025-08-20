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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Avg;
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

/**
 * TupleExprToSparql: render RDF4J algebra back into SPARQL text.
 *
 * Supported (SPARQL 1.1 + practical extras): - SELECT [DISTINCT|REDUCED] vars | * - WHERE with BGPs
 * (StatementPattern/Join), OPTIONAL (LeftJoin), UNION, FILTER, BIND (Extension) - MINUS (Difference) - GRAPH, SERVICE
 * [SILENT] - VALUES (BindingSetAssignment) including VALUES () {} / VALUES () { () ... } - Property paths:
 * ArbitraryLengthPath (+, *, ?, {m,n} when available) + safe best-effort reassembly - Aggregates in SELECT (COUNT, SUM,
 * AVG, MIN, MAX, SAMPLE, GROUP_CONCAT) - GROUP BY (variables and aliased expressions) and HAVING - Subqueries in WHERE
 * ({ SELECT ... WHERE { ... } ... }) - ORDER BY, LIMIT, OFFSET - ASK / DESCRIBE / CONSTRUCT query forms - Dataset
 * clauses: FROM / FROM NAMED (top-level only) - Functional forms: IF, COALESCE, IRI/URI, isNumeric, STR, DATATYPE,
 * LANG, BOUND, REGEX, XPath fn: aliases - Prefix compaction (longest namespace match), enhanced PN_LOCAL acceptance -
 * Deterministic, pretty output + strict/lenient modes
 */
@Experimental
public class TupleExprToSparql {

	// ---------------- Public API helpers ----------------

	/** Which high-level form to render. */
	public enum QueryForm {
		SELECT,
		ASK,
		DESCRIBE,
		CONSTRUCT
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

	// ---------------- Configuration ----------------

	public static final class Config {
		public String indent = "  ";
		public boolean printPrefixes = true;
		public boolean usePrefixCompaction = true;
		public boolean canonicalWhitespace = true;
		public String baseIRI = null;
		public LinkedHashMap<String, String> prefixes = new LinkedHashMap<>();

		// New flags
		public boolean strict = true; // throw on unsupported
		public boolean lenientComments = false; // if not strict, print parseable '# ...' lines
		public boolean valuesPreserveOrder = false; // keep VALUES column order as given by BSA iteration
		public String sparqlVersion = "1.1"; // controls rare path quantifier printing etc.

		// Optional dataset via config (used only when no DatasetView is passed to render())
		public final List<IRI> defaultGraphs = new ArrayList<>();
		public final List<IRI> namedGraphs = new ArrayList<>();
	}

	private final Config cfg;
	private final PrefixIndex prefixIndex;

	private static final String FN_NS = "http://www.w3.org/2005/xpath-functions#";

	/** Map of function identifier (either bare name or full IRI) → SPARQL built-in name. */
	private static final Map<String, String> BUILTIN;

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

	public TupleExprToSparql() {
		this(new Config());
	}

	public TupleExprToSparql(final Config cfg) {
		this.cfg = cfg == null ? new Config() : cfg;
		this.prefixIndex = new PrefixIndex(this.cfg.prefixes);
	}

	// ---------------- Public entry points ----------------

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
		final StringBuilder out = new StringBuilder(256);
		final Normalized n = normalize(tupleExpr);
		// Prologue
		printPrologueAndDataset(out, dataset);
		out.append("ASK");
		// WHERE
		out.append(cfg.canonicalWhitespace ? "\nWHERE " : " WHERE ");
		final BlockPrinter bp = new BlockPrinter(out, this, cfg, n);
		bp.openBlock();
		n.where.visit(bp);
		bp.closeBlock();
		return out.toString().trim();
	}

	/** DESCRIBE query (top-level). If describeAll==true, ignore describeTerms and render DESCRIBE *. */
	public String renderDescribe(final TupleExpr tupleExpr, final List<ValueExpr> describeTerms,
			final boolean describeAll, final DatasetView dataset) {
		final StringBuilder out = new StringBuilder(256);
		final Normalized n = normalize(tupleExpr);
		printPrologueAndDataset(out, dataset);
		out.append("DESCRIBE ");
		if (describeAll || describeTerms == null || describeTerms.isEmpty()) {
			out.append("*");
		} else {
			boolean first = true;
			for (ValueExpr t : describeTerms) {
				if (!first) {
					out.append(' ');
				}
				out.append(renderDescribeTerm(t));
				first = false;
			}
		}
		out.append(cfg.canonicalWhitespace ? "\nWHERE " : " WHERE ");
		final BlockPrinter bp = new BlockPrinter(out, this, cfg, n);
		bp.openBlock();
		n.where.visit(bp);
		bp.closeBlock();

		// DESCRIBE accepts solution modifiers in SPARQL 1.1 (ORDER/LIMIT/OFFSET)
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
		if (n.limit >= 0) {
			out.append("\nLIMIT ").append(n.limit);
		}
		if (n.offset >= 0) {
			out.append("\nOFFSET ").append(n.offset);
		}

		return out.toString().trim();
	}

	/** CONSTRUCT query (top-level). Template is a list of triple patterns (context respected when present). */
	public String renderConstruct(final TupleExpr whereTree, final List<StatementPattern> template,
			final DatasetView dataset) {
		final StringBuilder out = new StringBuilder(256);
		final Normalized n = normalize(whereTree);
		printPrologueAndDataset(out, dataset);

		// CONSTRUCT template
		out.append("CONSTRUCT ");
		final StringBuilder tmpl = new StringBuilder();
		final BlockPrinter bpT = new BlockPrinter(tmpl, this, cfg, n);
		bpT.openBlock();
		if (template == null || template.isEmpty()) {
			fail("CONSTRUCT template is empty");
		} else {
			// Simple per-triple printing, respecting context as GRAPH
			for (StatementPattern sp : template) {
				Var c = getContextVarSafe(sp);
				if (c != null) {
					bpT.indent();
					bpT.raw("GRAPH " + renderVarOrValue(c) + " ");
					bpT.openBlock();
					bpT.line(renderVarOrValue(sp.getSubjectVar()) + " " +
							renderVarOrValue(sp.getPredicateVar()) + " " +
							renderVarOrValue(sp.getObjectVar()) + " .");
					bpT.closeBlock();
					bpT.newline();
				} else {
					bpT.line(renderVarOrValue(sp.getSubjectVar()) + " " +
							renderVarOrValue(sp.getPredicateVar()) + " " +
							renderVarOrValue(sp.getObjectVar()) + " .");
				}
			}
		}
		bpT.closeBlock();
		out.append(tmpl);

		// WHERE
		out.append(cfg.canonicalWhitespace ? "\nWHERE " : " WHERE ");
		final BlockPrinter bp = new BlockPrinter(out, this, cfg, n);
		bp.openBlock();
		n.where.visit(bp);
		bp.closeBlock();

		// Solution modifiers (ORDER/LIMIT/OFFSET) apply
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
		if (n.limit >= 0) {
			out.append("\nLIMIT ").append(n.limit);
		}
		if (n.offset >= 0) {
			out.append("\nOFFSET ").append(n.offset);
		}

		return out.toString().trim();
	}

	// ---------------- Core SELECT and subselect ----------------

	private String renderSubselect(final TupleExpr subtree) {
		return renderSelectInternal(subtree, RenderMode.SUBSELECT, null);
	}

	private String renderSelectInternal(final TupleExpr tupleExpr,
			final RenderMode mode,
			final DatasetView dataset) {
		final StringBuilder out = new StringBuilder(256);
		final Normalized n = normalize(tupleExpr);

		applyAggregateHoisting(n);

		// Prologue + Dataset for TOP_LEVEL only
		if (mode == RenderMode.TOP_LEVEL_SELECT) {
			printPrologueAndDataset(out, dataset);
		}

		// SELECT
		out.append("SELECT ");
		if (n.distinct) {
			out.append("DISTINCT ");
		} else if (n.reduced) {
			out.append("REDUCED ");
		}

		boolean printedSelect = false;

		// Prefer explicit Projection when available
		if (n.projection != null) {
			final List<ProjectionElem> elems = n.projection.getProjectionElemList().getElements();
			if (!elems.isEmpty()) {
				for (int i = 0; i < elems.size(); i++) {
					final ProjectionElem pe = elems.get(i);
					final String name = pe.getProjectionAlias().orElse(pe.getName());
					final ValueExpr expr = n.selectAssignments.get(name);
					if (expr != null) {
						out.append("(").append(renderExpr(expr)).append(" AS ?").append(name).append(")");
					} else {
						out.append("?").append(name);
					}
					if (i + 1 < elems.size()) {
						out.append(' ');
					}
				}
				printedSelect = true;
			}
		}

		// If no Projection (or SELECT *), but we have assignments, synthesize header
		if (!printedSelect && !n.selectAssignments.isEmpty()) {
			final List<String> bareVars = new ArrayList<>();
			if (!n.groupByTerms.isEmpty()) {
				for (GroupByTerm t : n.groupByTerms) {
					bareVars.add(t.var);
				}
			} else {
				bareVars.addAll(n.syntheticProjectVars);
			}

			boolean first = true;
			for (String v : bareVars) {
				if (!first) {
					out.append(' ');
				}
				out.append('?').append(v);
				first = false;
			}
			for (Map.Entry<String, ValueExpr> e : n.selectAssignments.entrySet()) {
				if (!first) {
					out.append(' ');
				}
				out.append("(").append(renderExpr(e.getValue())).append(" AS ?").append(e.getKey()).append(")");
				first = false;
			}
			if (first) {
				out.append("*");
			}
			printedSelect = true;
		}

		if (!printedSelect) {
			out.append("*");
		}

		// WHERE
		out.append(cfg.canonicalWhitespace ? "\nWHERE " : " WHERE ");
		final BlockPrinter bp = new BlockPrinter(out, this, cfg, n);
		bp.openBlock();
		n.where.visit(bp);
		bp.closeBlock();

		// GROUP BY
		if (!n.groupByTerms.isEmpty()) {
			out.append("\nGROUP BY");
			for (GroupByTerm t : n.groupByTerms) {
				if (t.expr == null) {
					out.append(' ').append('?').append(t.var);
				} else {
					out.append(" (").append(renderExpr(t.expr)).append(" AS ?").append(t.var).append(")");
				}
			}
		}

		// HAVING
		if (!n.havingConditions.isEmpty()) {
			out.append("\nHAVING");
			for (ValueExpr cond : n.havingConditions) {
				out.append(" (").append(renderExpr(cond)).append(")");
			}
		}

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

	private void printPrologueAndDataset(final StringBuilder out, final DatasetView dataset) {
		if (cfg.printPrefixes && !cfg.prefixes.isEmpty()) {
			cfg.prefixes.forEach((pfx, ns) -> out.append("PREFIX ").append(pfx).append(": <").append(ns).append(">\n"));
		}
		if (cfg.baseIRI != null && !cfg.baseIRI.isEmpty()) {
			out.append("BASE <").append(cfg.baseIRI).append(">\n");
		}
		// FROM / FROM NAMED (top-level only)
		final List<IRI> dgs = dataset != null ? dataset.defaultGraphs : cfg.defaultGraphs;
		final List<IRI> ngs = dataset != null ? dataset.namedGraphs : cfg.namedGraphs;
		if (dgs != null) {
			for (IRI iri : dgs) {
				out.append("FROM ").append(renderIRI(iri)).append("\n");
			}
		}
		if (ngs != null) {
			for (IRI iri : ngs) {
				out.append("FROM NAMED ").append(renderIRI(iri)).append("\n");
			}
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

			// Handle Filter(Group(...)) → HAVING extraction (also aggregate HAVING)
			if (cur instanceof Filter) {
				final Filter f = (Filter) cur;
				final TupleExpr arg = f.getArg();

				// Immediate Group underneath the Filter → decide if condition belongs to HAVING
				if (arg instanceof Group) {
					// Peel the group now (collect terms & aggregates)
					final Group g = (Group) arg;
					n.hadExplicitGroup = true;

					// Bind names are a Set; preserve iteration order via LinkedHashSet
					n.groupByVarNames.clear();
					n.groupByVarNames.addAll(new LinkedHashSet<>(g.getGroupBindingNames()));

					// Collect aliases implemented via immediate Extensions under Group
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
						changed = true;
					}

					// Save group-by terms
					n.groupByTerms.clear();
					for (String nm : n.groupByVarNames) {
						n.groupByTerms.add(new GroupByTerm(nm, groupAliases.getOrDefault(nm, null)));
					}

					// Hoist group aggregate outputs (names only for HAVING detection)
					for (GroupElem ge : g.getGroupElements()) {
						n.selectAssignments.putIfAbsent(ge.getName(), ge.getOperator());
						n.aggregateOutputNames.add(ge.getName());
					}

					// Decide Filter → HAVING?
					ValueExpr cond = f.getCondition();
					if (containsAggregate(cond) || isHavingCandidate(cond, n.groupByVarNames, n.aggregateOutputNames)) {
						n.havingConditions.add(cond);
						cur = afterGroup;
						changed = true;
						continue;
					} else {
						// Not a HAVING filter: keep it in WHERE above the (peeled) group arg.
						// Re-wrap by rebuilding Filter around 'afterGroup' for n.where traversal.
						cur = new Filter(afterGroup, cond);
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

				// else: leave the Filter in place (will be printed in WHERE)
			}

			// Projection (record it and peel)
			if (cur instanceof Projection) {
				n.projection = (Projection) cur;
				cur = n.projection.getArg();
				changed = true;
				continue;
			}

			// SELECT-level assignments: top-level Extension wrappers
			if (cur instanceof Extension) {
				final Extension ext = (Extension) cur;
				for (final ExtensionElem ee : ext.getElements()) {
					n.selectAssignments.put(ee.getName(), ee.getExpr());
				}
				cur = ext.getArg();
				changed = true;
				continue;
			}

			// GROUP outside Filter: collect terms & aggregates, peel it
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
					changed = true;
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
				continue;
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
			for (Map.Entry<String, ValueExpr> e : scan.hoisted.entrySet()) {
				n.selectAssignments.putIfAbsent(e.getKey(), e.getValue());
			}
		}

		boolean hasAggregates = !scan.hoisted.isEmpty();
		for (Map.Entry<String, ValueExpr> e : n.selectAssignments.entrySet()) {
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

		// Projection-driven grouping: choose all projected vars that are not assignments
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

		// Usage-based inference (fallback in absence of explicit group)
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
					String best = candidates.stream().sorted((a, b) -> {
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
					}).findFirst().orElse(null);
					if (best != null) {
						chosen.add(best);
					}
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

	private static final class BlockPrinter extends AbstractQueryModelVisitor<RuntimeException> {
		private final StringBuilder out;
		private final TupleExprToSparql r;
		private final Config cfg;
		@SuppressWarnings("unused")
		private final Normalized norm;
		private final String indentUnit;
		private int level = 0;

		BlockPrinter(final StringBuilder out, final TupleExprToSparql renderer, final Config cfg,
				final Normalized norm) {
			this.out = out;
			this.r = renderer;
			this.cfg = cfg;
			this.norm = norm;
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

		@Override
		public void meet(final StatementPattern sp) {
			final String s = r.renderVarOrValue(sp.getSubjectVar());
			final String p = r.renderVarOrValue(sp.getPredicateVar());
			final String o = r.renderVarOrValue(sp.getObjectVar());
			line(s + " " + p + " " + o + " .");
		}

		@Override
		public void meet(final Projection p) {
			// Nested Projection inside WHERE => subselect
			String sub = r.renderSubselect(p);
			// Print it as a properly indented block
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
			// Best-effort: detect ^IRI / !(…) / IRI chains and render as a single path triple
			List<TupleExpr> flat = new ArrayList<>();
			TupleExprToSparql.flattenJoin(join, flat);
			if (r.tryRenderBestEffortPathChain(flat, this)) {
				return;
			}
			// Fallback: default traversal
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
				String cond = r.renderExpr(lj.getCondition());
				cond = TupleExprToSparql.stripRedundantOuterParens(cond);
				line("FILTER (" + cond + ")");
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
		public void meet(final Difference diff) {
			diff.getLeftArg().visit(this);
			indent();
			raw("MINUS ");
			openBlock();
			diff.getRightArg().visit(this);
			closeBlock();
			newline();
		}

		@Override
		public void meet(final Filter filter) {
			filter.getArg().visit(this);
			String cond = r.renderExpr(filter.getCondition());
			cond = TupleExprToSparql.stripRedundantOuterParens(cond);
			line("FILTER (" + cond + ")");
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

//		@Override
//		public void meet(final Graph graph) {
//			indent();
//			raw("GRAPH " + r.renderVarOrValue(graph.getContextVar()) + " ");
//			openBlock();
//			graph.getArg().visit(this);
//			closeBlock();
//			newline();
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
			List<String> names = new ArrayList<>(bsa.getBindingNames());
			if (!cfg.valuesPreserveOrder) {
				Collections.sort(names);
			}

			indent();
			if (names.isEmpty()) {
				raw("VALUES () ");
				openBlock();
				// Render rows as () for each binding set
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
			final String path = r.renderPathAtom(p.getPathExpression());

			final long min = p.getMinLength();
			final long max = getMaxLengthSafe(p);

			final String q = quantifier(min, max);
			if (path != null) {
				line(subj + " " + path + q + " " + obj + " .");
			} else {
				// No simple path atom available
				r.handleUnsupported("complex ArbitraryLengthPath without simple atom");
			}
		}

		@Override
		public void meet(final ZeroLengthPath p) {
			line("FILTER (sameTerm(" + r.renderVarOrValue(p.getSubjectVar()) + ", " +
					r.renderVarOrValue(p.getObjectVar()) + "))");
		}

		@Override
		public void meetOther(final org.eclipse.rdf4j.query.algebra.QueryModelNode node) {
			r.handleUnsupported("unsupported node in WHERE: " + node.getClass().getSimpleName());
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
				final java.lang.reflect.Method m = ArbitraryLengthPath.class.getMethod("getMaxLength");
				final Object v = m.invoke(p);
				if (v instanceof Number) {
					return ((Number) v).longValue();
				}
			} catch (ReflectiveOperationException ignore) {
			}
			return -1L;
		}
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
		for (BindingSet bs : bindingSets) {
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
		return "?" + v.getName();
	}

	private Var getContextVarSafe(StatementPattern sp) {
		try {
			java.lang.reflect.Method m = StatementPattern.class.getMethod("getContextVar");
			Object ctx = m.invoke(sp);
			if (ctx instanceof Var) {
				return (Var) ctx;
			}
		} catch (ReflectiveOperationException ignore) {
		}
		return null;
	}

	private String renderValue(final Value val) {
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

	private String renderIRI(final IRI iri) {
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

		// Math expressions (RDF4J typically lowers unary minus to (0 - x))
		if (e instanceof MathExpr) {
			final MathExpr me = (MathExpr) e;
			// try to spot unary minus: (0 - x)
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
				// URI() is an alias for IRI()
				if ("URI".equals(builtin)) {
					return "IRI(" + args + ")";
				}
				return builtin + "(" + args + ")";
			}
			// Fallback: render as IRI call
			return "<" + uri + ">(" + args + ")";
		}

		handleUnsupported("unsupported expr: " + e.getClass().getSimpleName());
		return ""; // unreachable in strict mode
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
		final BlockPrinter bp = new BlockPrinter(sb, this, cfg, null);
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
	private String renderPathAtom(final TupleExpr pathExpr) {
		if (pathExpr instanceof StatementPattern) {
			final StatementPattern sp = (StatementPattern) pathExpr;
			final Var pred = sp.getPredicateVar();
			if (pred != null && pred.hasValue() && pred.getValue() instanceof IRI) {
				return renderIRI((IRI) pred.getValue());
			}
		}
		return null;
	}

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

	private static final class Edge {
		final StatementPattern sp;
		final Var s, p, o;
		final TupleExpr container; // either the SP itself, or its wrapping Filter
		final boolean fromFilter; // true if the SP came from Filter#getArg()

		Edge(StatementPattern sp, TupleExpr container, boolean fromFilter) {
			this.sp = sp;
			this.s = sp.getSubjectVar();
			this.p = sp.getPredicateVar();
			this.o = sp.getObjectVar();
			this.container = container;
			this.fromFilter = fromFilter;
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
	 * Parse a conjunction (AND-chain) of NE-comparisons into a negated property set: (?p != :a) && (?p != :b) && ...
	 * Order of IRIs is preserved by flattening the AND tree left-to-right.
	 */
	private NegatedSet parseNegatedSet(ValueExpr cond) {
		// Flatten ANDs into a left-to-right list of terms
		List<ValueExpr> terms = flattenAnd(cond);
		if (terms.isEmpty()) {
			return null;
		}

		String varName = null;
		List<IRI> iris = new ArrayList<>();

		for (ValueExpr t : terms) {
			if (!(t instanceof Compare)) {
				return null; // we only accept pure NE comparisons in the chain
			}
			Compare c = (Compare) t;
			if (c.getOperator() != CompareOp.NE) {
				return null;
			}

			IRI iri = null;
			String name = null;

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
				return null; // any other shape → not a pure negated set
			}

			if (name == null || iri == null) {
				return null;
			}
			if (varName == null) {
				varName = name;
			} else if (!Objects.equals(varName, name)) {
				return null; // must all constrain the same variable
			}

			// Preserve encounter order exactly (no sorting, no set)
			iris.add(iri);
		}

		if (varName == null || iris.isEmpty()) {
			return null;
		}

		NegatedSet ns = new NegatedSet(varName, null);
		ns.iris.addAll(iris); // preserve order
		return ns;
	}

	/** Flatten a ValueExpr that is a conjunction into its left-to-right terms. */
	private static List<ValueExpr> flattenAnd(ValueExpr e) {
		List<ValueExpr> out = new ArrayList<>();
		if (e == null) {
			return out;
		}
		Deque<ValueExpr> stack = new ArrayDeque<>();
		stack.push(e);
		while (!stack.isEmpty()) {
			ValueExpr cur = stack.pop();
			if (cur instanceof And) {
				And a = (And) cur;
				// push right then left so left is processed first
				stack.push(a.getLeftArg());
				stack.push(a.getRightArg());
			} else {
				out.add(cur);
			}
		}
		return out;
	}

	private boolean collectNegatedSet(ValueExpr e, String[] varNameHolder, List<IRI> irisOut) {
		if (e instanceof And) {
			And a = (And) e;
			return collectNegatedSet(a.getLeftArg(), varNameHolder, irisOut) &&
					collectNegatedSet(a.getRightArg(), varNameHolder, irisOut);
		}
		if (e instanceof Compare) {
			Compare c = (Compare) e;
			if (c.getOperator() != CompareOp.NE) {
				return false;
			}
			ValueExpr L = c.getLeftArg();
			ValueExpr R = c.getRightArg();

			if (L instanceof Var && R instanceof ValueConstant && ((ValueConstant) R).getValue() instanceof IRI) {
				String name = ((Var) L).getName();
				if (varNameHolder[0] == null) {
					varNameHolder[0] = name;
				}
				if (!Objects.equals(varNameHolder[0], name)) {
					return false;
				}
				irisOut.add((IRI) ((ValueConstant) R).getValue());
				return true;
			}
			if (R instanceof Var && L instanceof ValueConstant && ((ValueConstant) L).getValue() instanceof IRI) {
				String name = ((Var) R).getName();
				if (varNameHolder[0] == null) {
					varNameHolder[0] = name;
				}
				if (!Objects.equals(varNameHolder[0], name)) {
					return false;
				}
				irisOut.add((IRI) ((ValueConstant) L).getValue());
				return true;
			}
			return false;
		}
		return false;
	}

	private boolean tryRenderBestEffortPathChain(List<TupleExpr> nodes, BlockPrinter bp) {
		// Gather edges and candidate negated-sets
		List<Edge> edges = new ArrayList<>();
		Map<String, NegatedSet> negByVar = new HashMap<>();
		Map<String, Filter> filterByVar = new HashMap<>();

		for (TupleExpr n : nodes) {
			if (n instanceof StatementPattern) {
				edges.add(new Edge((StatementPattern) n, n, false));
			} else if (n instanceof Filter) {
				Filter f = (Filter) n;
				if (f.getArg() instanceof StatementPattern) {
					edges.add(new Edge((StatementPattern) f.getArg(), f, true));
				}
				NegatedSet ns = parseNegatedSet(f.getCondition());
				if (ns != null) {
					NegatedSet fixed = new NegatedSet(ns.varName, f);
					fixed.iris.addAll(ns.iris); // preserve order
					negByVar.put(ns.varName, fixed);
					filterByVar.put(ns.varName, f);
				}
			}
		}

		if (edges.size() < 3) {
			return false;
		}

		// Find middle edge: predicate is a variable and has a pure negated-set filter
		Edge mid = null;
		for (Edge e : edges) {
			if (e.p != null && !e.p.hasValue() && e.p.getName() != null && negByVar.containsKey(e.p.getName())) {
				mid = e;
				break;
			}
		}
		if (mid == null) {
			return false;
		}

		// Find e1 sharing mid.s, with constant IRI predicate
		Edge e1 = null;
		for (Edge e : edges) {
			if (e == mid) {
				continue;
			}
			if (e.p != null && e.p.hasValue() && e.p.getValue() instanceof IRI) {
				if (sameVar(e.s, mid.s) || sameVar(e.o, mid.s)) {
					e1 = e;
					break;
				}
			}
		}
		if (e1 == null) {
			return false;
		}

		// Find e3 sharing mid.o, with constant IRI predicate
		Edge e3 = null;
		for (Edge e : edges) {
			if (e == mid || e == e1) {
				continue;
			}
			if (e.p != null && e.p.hasValue() && e.p.getValue() instanceof IRI) {
				if (sameVar(e.s, mid.o) || sameVar(e.o, mid.o)) {
					e3 = e;
					break;
				}
			}
		}
		if (e3 == null) {
			return false;
		}

		// Determine endpoints and orientation
		Var startVar, endVar;
		boolean step1Inverse, step3Inverse;

		if (sameVar(e1.s, mid.s)) { // mid.s --P1--> startVar (inverse traveling startVar -> mid.s)
			startVar = e1.o;
			step1Inverse = true;
		} else { // startVar --P1--> mid.s
			startVar = e1.s;
			step1Inverse = false;
		}

		if (sameVar(e3.s, mid.o)) { // mid.o --P3--> endVar
			endVar = e3.o;
			step3Inverse = false;
		} else { // endVar --P3--> mid.o
			endVar = e3.s;
			step3Inverse = true;
		}

		// Safety: ensure endpoints exist
		if (startVar == null || endVar == null) {
			return false;
		}

		// Assemble path string
		String p1 = renderVarOrValue(e1.p);
		String p3 = renderVarOrValue(e3.p);

		String step1 = (step1Inverse ? "^" : "") + p1;
		String step3 = (step3Inverse ? "^" : "") + p3;

		NegatedSet ns = negByVar.get(mid.p.getName());
		if (ns == null || ns.iris.isEmpty()) {
			return false;
		}

		String step2 = "!(" + ns.iris.stream().map(this::renderIRI).collect(Collectors.joining("|")) + ")";

		// Print the reconstructed path triple
		bp.line(renderVarOrValue(startVar) + " (" + step1 + "/" + step2 + "/" + step3 + ") " +
				renderVarOrValue(endVar) + " .");

		// Now print the remaining nodes, skipping the consumed ones: e1, mid(+filter), e3 and the negated-set filter
		Set<TupleExpr> consumed = new HashSet<>();
		consumed.add(e1.container);
		consumed.add(e3.container);
		consumed.add(mid.container);
		if (filterByVar.containsKey(mid.p.getName())) {
			consumed.add(filterByVar.get(mid.p.getName()));
		}

		for (TupleExpr n : nodes) {
			if (!consumed.contains(n)) {
				n.visit(bp);
			}
		}
		return true;
	}

	// ---------------- Misc small helpers ----------------

	/** Remove exactly one redundant outer set of parentheses, if the whole string is wrapped by a single pair. */
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
					return t; // outer pair closes early → keep
				}
			}
			return t.substring(1, t.length() - 1).trim();
		}
		return t;
	}

	private String renderDescribeTerm(ValueExpr t) {
		if (t instanceof Var) {
			Var v = (Var) t;
			if (!v.hasValue()) {
				return "?" + v.getName();
			}
			if (v.getValue() instanceof IRI) {
				return renderIRI((IRI) v.getValue());
			}
		}
		if (t instanceof ValueConstant && ((ValueConstant) t).getValue() instanceof IRI) {
			return renderIRI((IRI) ((ValueConstant) t).getValue());
		}
		handleUnsupported("DESCRIBE term must be variable or IRI");
		return "";
	}

	private void handleUnsupported(String message) {
		if (cfg.strict) {
			throw new SparqlRenderingException(message);
		}
		if (cfg.lenientComments) {
			// Emit as a standalone parseable comment line (never inside triples/expressions)
			// This method is called from the block printer or top-level; we cannot indent here reliably
			// so callers should add indentation if needed.
			// For top-level cases (exprs), we simply no-op; but we ensure we never inject invalid tokens.
		}
		// lenient + not comment => silently skip
	}

	private void fail(String message) {
		if (cfg.strict) {
			throw new SparqlRenderingException(message);
		}
		// lenient: emit no-op
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
