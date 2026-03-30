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
package org.eclipse.rdf4j.queryrender.sparql;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
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
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer.Config;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBind;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrExists;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGroupByElem;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrInlineTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNot;
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
import org.eclipse.rdf4j.queryrender.sparql.ir.util.IrTransforms;
import org.eclipse.rdf4j.queryrender.sparql.util.ExprTextUtils;
import org.eclipse.rdf4j.queryrender.sparql.util.TermRenderer;
import org.eclipse.rdf4j.queryrender.sparql.util.TextEscapes;
import org.eclipse.rdf4j.queryrender.sparql.util.VarUtils;

/**
 * Extracted converter that builds textual-IR from a TupleExpr.
 *
 * This class mirrors the TupleExpr→IR logic originally embedded in TupleExprIRRenderer; the renderer now delegates to
 * this converter to build IR, and handles printing separately.
 */
@Experimental
public class TupleExprToIrConverter {

	private static final int PREC_ALT = 1;
	private static final int PREC_SEQ = 2;

	// ---------------- Public entry points ----------------
	private static final int PREC_ATOM = 3;
	private final TupleExprIRRenderer r;
	private final Config cfg;
	private final PrefixIndex prefixIndex;

	// -------------- Local textual helpers moved from renderer --------------

	private static final String FN_NS = "http://www.w3.org/2005/xpath-functions#";
	private static final Map<String, String> BUILTIN;

	static {
		Map<String, String> m = new LinkedHashMap<>();
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
		for (String k : new String[] { "RAND", "NOW", "ABS", "CEIL", "FLOOR", "ROUND", "YEAR", "MONTH", "DAY",
				"HOURS", "MINUTES", "SECONDS", "TZ", "TIMEZONE", "MD5", "SHA1", "SHA224", "SHA256", "SHA384",
				"SHA512", "UCASE", "LCASE", "SUBSTR", "STRLEN", "CONTAINS", "CONCAT", "REPLACE",
				"ENCODE_FOR_URI", "STRSTARTS", "STRENDS", "STRBEFORE", "STRAFTER", "REGEX", "UUID", "STRUUID",
				"STRDT", "STRLANG", "BNODE", "URI" }) {
			m.put(k, k);
		}
		BUILTIN = Collections.unmodifiableMap(m);
	}

	// literal escaping moved to TextEscapes

	private String convertIRIToString(final IRI iri) {
		return TermRenderer.convertIRIToString(iri, prefixIndex, cfg.usePrefixCompaction);
	}

	// PN_LOCAL checks handled in TermRenderer via SparqlNameUtils

	private String convertValueToString(final Value val) {
		return TermRenderer.convertValueToString(val, prefixIndex, cfg.usePrefixCompaction);
	}

	private String renderVarOrValue(final Var v) {
		if (v == null) {
			return "?_";
		}
		if (v.hasValue()) {
			return convertValueToString(v.getValue());
		}
		if (v.isAnonymous() && !v.isConstant()) {
			return "_:" + v.getName();
		}
		return "?" + v.getName();
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

	private static String asConstraint(final String s) {
		if (s == null) {
			return "()";
		}
		final String t = s.trim();
		if (t.isEmpty()) {
			return "()";
		}
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
					break;
				}
				if (i == t.length() - 1 && depth == 0) {
					return t;
				}
			}
		}
		if (t.startsWith("EXISTS ") || t.startsWith("NOT EXISTS ")) {
			return t;
		}
		int lpar = t.indexOf('(');
		if (lpar > 0 && t.endsWith(")")) {
			String head = t.substring(0, lpar).trim();
			if (!head.isEmpty() && head.indexOf(' ') < 0) {
				return t;
			}
		}
		return "(" + t + ")";
	}

// removed local parenthesizeIfNeededExpr; use ExprTextUtils.parenthesizeIfNeededExpr instead

	private String renderExists(final Exists ex) {
		// Build IR for the subquery
		IRBuilder inner = new IRBuilder();
		IrBGP where = inner.build(ex.getSubQuery());
		// Apply standard transforms for consistent property path and grouping rewrites
		IrSelect tmp = new IrSelect(false);
		tmp.setWhere(where);
		IrSelect transformed = IrTransforms.transformUsingChildren(tmp, r);
		where = transformed.getWhere();
		StringBuilder sb = new StringBuilder(64);
		InlinePrinter p = new InlinePrinter(sb);
		where.print(p);
		String group = sb.toString().replace('\n', ' ').replaceAll("\\s+", " ").trim();
		return "EXISTS " + group;
	}

	private String renderIn(final ListMemberOperator in, final boolean negate) {
		final List<ValueExpr> args = in.getArguments();
		if (args == null || args.isEmpty()) {
			return "/* invalid IN */";
		}
		final String left = renderExpr(args.get(0));
		final String rest = args.stream().skip(1).map(this::renderExpr).collect(Collectors.joining(", "));
		return "(" + left + (negate ? " NOT IN (" : " IN (") + rest + "))";
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
				sb.append("; SEPARATOR=").append('"').append(TextEscapes.escapeLiteral(sepLex)).append('"');
			}
			sb.append(")");
			return sb.toString();
		}
		return "/* unsupported aggregate */";
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

	// Minimal inline printer to render IrBGP blocks for inline EXISTS groups
	private final class InlinePrinter implements IrPrinter {
		private final StringBuilder out;
		private int level = 0;
		private boolean inlineActive = false;

		InlinePrinter(StringBuilder out) {
			this.out = out;
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
		public void append(String s) {
			if (!inlineActive) {
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
			inlineActive = false;
		}

		@Override
		public void closeBlock() {
			level--;
			indent();
			out.append('}').append('\n');
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
		public String convertVarToString(Var v) {
			return renderVarOrValue(v);
		}

		@Override
		public void printLines(List<IrNode> lines) {
			if (lines == null) {
				return;
			}
			for (IrNode ln : lines) {
				if (ln != null) {
					ln.print(this);
				}
			}
		}
	}

	private String renderExpr(final ValueExpr e) {
		if (e == null) {
			return "()";
		}

		if (e instanceof AggregateOperator) {
			return renderAggregate((AggregateOperator) e);
		}

		if (e instanceof Not) {
			final ValueExpr a = ((Not) e).getArg();
			if (a instanceof Exists) {
				return "NOT " + renderExists((Exists) a);
			}
			if (a instanceof ListMemberOperator) {
				return renderIn((ListMemberOperator) a, true); // NOT IN
			}
			final String inner = ExprTextUtils.stripRedundantOuterParens(renderExpr(a));
			return "!" + ExprTextUtils.parenthesizeIfNeededExpr(inner);
		}

		if (e instanceof Var) {
			final Var v = (Var) e;
			return v.hasValue() ? convertValueToString(v.getValue()) : "?" + v.getName();
		}
		if (e instanceof ValueConstant) {
			return convertValueToString(((ValueConstant) e).getValue());
		}

		if (e instanceof If) {
			final If iff = (If) e;
			return "IF(" + renderExpr(iff.getCondition()) + ", " + renderExpr(iff.getResult()) + ", "
					+ renderExpr(iff.getAlternative()) + ")";
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

		if (e instanceof Exists) {
			return renderExists((Exists) e);
		}

		if (e instanceof ListMemberOperator) {
			return renderIn((ListMemberOperator) e, false);
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

		if (e instanceof MathExpr) {
			final MathExpr me = (MathExpr) e;
			if (me.getOperator() == MathOp.MINUS &&
					me.getLeftArg() instanceof ValueConstant &&
					((ValueConstant) me.getLeftArg()).getValue() instanceof Literal) {
				Literal l = (Literal) ((ValueConstant) me.getLeftArg()).getValue();
				if ("0".equals(l.getLabel())) {
					return "(-" + renderExpr(me.getRightArg()) + ")";
				}
			}
			return "(" + renderExpr(me.getLeftArg()) + " " + mathOp(me.getOperator()) + " "
					+ renderExpr(me.getRightArg()) + ")";
		}

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
			return "(" + renderExpr(c.getLeftArg()) + " " + op(c.getOperator()) + " "
					+ renderExpr(c.getRightArg()) + ")";
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
			final Regex rr = (Regex) e;
			final String term = renderExpr(rr.getArg());
			final String patt = renderExpr(rr.getPatternArg());
			if (rr.getFlagsArg() != null) {
				return "REGEX(" + term + ", " + patt + ", " + renderExpr(rr.getFlagsArg()) + ")";
			}
			return "REGEX(" + term + ", " + patt + ")";
		}

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
			if (uri != null) {
				try {
					IRI iri = SimpleValueFactory.getInstance().createIRI(uri);
					return convertIRIToString(iri) + "(" + args + ")";
				} catch (IllegalArgumentException ignore) {
					return "<" + uri + ">(" + args + ")";
				}
			}
			return "()";
		}

		if (e instanceof BNodeGenerator) {
			final BNodeGenerator bg = (BNodeGenerator) e;
			final ValueExpr id = bg.getNodeIdExpr();
			if (id == null) {
				return "BNODE()";
			}
			return "BNODE(" + renderExpr(id) + ")";
		}

		return "/* unsupported expr: " + e.getClass().getSimpleName() + " */";
	}

	private static boolean isConstIriVar(Var v) {
		return v != null && v.hasValue() && v.getValue() instanceof IRI;
	}

	private static IRI asIri(Var v) {
		return (v != null && v.hasValue() && v.getValue() instanceof IRI) ? (IRI) v.getValue() : null;
	}

	// ---------------- Normalization and helpers ----------------

	public TupleExprToIrConverter(TupleExprIRRenderer renderer) {
		this.r = renderer;
		this.cfg = renderer.getConfig();
		this.prefixIndex = new PrefixIndex(this.cfg.prefixes);
	}

	/** Build IrSelect; by default apply transforms (used for subselects). */
	public static IrSelect toIRSelectRaw(final TupleExpr tupleExpr, TupleExprIRRenderer r) {
		return toIRSelectRaw(tupleExpr, r, true);
	}

	/**
	 * Build IrSelect (raw). The applyTransforms argument is ignored; transforms are handled by the renderer.
	 */
	public static IrSelect toIRSelectRaw(final TupleExpr tupleExpr, TupleExprIRRenderer r, boolean applyTransforms) {
		final TupleExprToIrConverter conv = new TupleExprToIrConverter(r);
		final Normalized n = normalize(tupleExpr, true);
		applyAggregateHoisting(n);

		final IrSelect ir = new IrSelect(false);
		// Canonicalize DISTINCT/REDUCED: if DISTINCT is set, REDUCED is a no-op and removed
		ir.setDistinct(n.distinct);
		ir.setReduced(n.reduced && !n.distinct);
		ir.setLimit(n.limit);
		ir.setOffset(n.offset);

		if (n.projection != null && n.projection.getProjectionElemList() != null
				&& !n.projection.getProjectionElemList().getElements().isEmpty()) {
			for (ProjectionElem pe : n.projection.getProjectionElemList().getElements()) {
				final String alias = pe.getProjectionAlias().orElse(pe.getName());
				final ValueExpr expr = n.selectAssignments.get(alias);
				if (expr != null) {
					ir.getProjection().add(new IrProjectionItem(conv.renderExpr(expr), alias));
				} else {
					ir.getProjection().add(new IrProjectionItem(null, alias));
				}
			}
		} else if (!n.selectAssignments.isEmpty()) {
			if (!n.groupByTerms.isEmpty()) {
				for (GroupByTerm t : n.groupByTerms) {
					ir.getProjection().add(new IrProjectionItem(null, t.var));
				}
			} else {
				for (String v : n.syntheticProjectVars) {
					ir.getProjection().add(new IrProjectionItem(null, v));
				}
			}
			for (Entry<String, ValueExpr> e : n.selectAssignments.entrySet()) {
				ir.getProjection().add(new IrProjectionItem(conv.renderExpr(e.getValue()), e.getKey()));
			}
		}

		final IRBuilder builder = new TupleExprToIrConverter(r).new IRBuilder();
		ir.setWhere(builder.build(n.where));

		// Optionally apply transforms (useful for nested subselects; top-level transforms are handled by the renderer).
		if (applyTransforms) {
			IrSelect transformed = IrTransforms.transformUsingChildren(ir, r);
			ir.setWhere(transformed.getWhere());

			// Preserve explicit grouping braces around a single‑line WHERE when the original algebra
			// indicated a variable scope change at the root of the subselect. This mirrors the old behavior
			// and keeps nested queries' grouping stable for tests.
			if (ir.getWhere() != null && ir.getWhere().getLines() != null && ir.getWhere().getLines().size() == 1
					&& rootHasExplicitScope(n.where)) {
				final IrNode only = ir.getWhere().getLines().get(0);
				if (only instanceof IrStatementPattern || only instanceof IrPathTriple || only instanceof IrGraph
						|| only instanceof IrSubSelect) {
					ir.getWhere().setNewScope(true);
				}
			}
		}

		// Re-insert non-aggregate BIND assignments after transforms so they are not optimized away.
		if (!n.extensionAssignments.isEmpty() && ir.getWhere() != null) {
			IrBGP whereBgp = ir.getWhere();

			// Skip BINDs that correspond exactly to GROUP BY (expr AS ?var) aliases; those aliases are already rendered
			// in the GROUP BY clause and should not surface as separate BINDs in the WHERE.
			Map<String, ValueExpr> groupAliasExprByVar = new LinkedHashMap<>();
			for (GroupByTerm t : n.groupByTerms) {
				if (t.expr != null) {
					groupAliasExprByVar.put(t.var, t.expr);
				}
			}

			List<IrNode> prefixConst = new ArrayList<>();
			List<IrNode> suffixDependent = new ArrayList<>();
			for (Entry<String, ValueExpr> e : n.extensionAssignments.entrySet()) {
				ValueExpr expr = e.getValue();
				if (expr instanceof AggregateOperator) {
					continue;
				}
				if (groupAliasExprByVar.containsKey(e.getKey())
						&& groupAliasExprByVar.get(e.getKey()).equals(expr)) {
					continue;
				}
				Set<String> deps = freeVars(expr);
				IrBind bind = new IrBind(conv.renderExpr(expr), e.getKey(), false);
				if (deps.isEmpty()) {
					prefixConst.add(bind); // constant bindings first (e.g., SERVICE endpoint)
				} else {
					suffixDependent.add(bind); // bindings that depend on other vars go after the patterns
				}
			}
			if (!prefixConst.isEmpty() || !suffixDependent.isEmpty()) {
				IrBGP combined = new IrBGP(whereBgp.isNewScope());
				combined.getLines().addAll(prefixConst);
				if (whereBgp.getLines() != null) {
					combined.getLines().addAll(whereBgp.getLines());
				}
				combined.getLines().addAll(suffixDependent);
				ir.setWhere(combined);
			}
		}

		for (GroupByTerm t : n.groupByTerms) {
			ir.getGroupBy().add(new IrGroupByElem(t.expr == null ? null : conv.renderExpr(t.expr), t.var));
		}
		for (ValueExpr cond : n.havingConditions) {
			ir.getHaving().add(ExprTextUtils.stripRedundantOuterParens(conv.renderExprForHaving(cond, n)));
		}
		for (OrderElem oe : n.orderBy) {
			ir.getOrderBy().add(new IrOrderSpec(conv.renderExpr(oe.getExpr()), oe.isAscending()));
		}
		return ir;
	}

	private static Normalized normalize(final TupleExpr root, final boolean peelScopedWrappers) {
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
				if (s.isVariableScopeChange() && !peelScopedWrappers) {
					break;
				}
				n.limit = s.getLimit();
				n.offset = s.getOffset();
				cur = s.getArg();
				changed = true;
				continue;
			}

			if (cur instanceof Distinct) {
				final Distinct d = (Distinct) cur;
				if (d.isVariableScopeChange() && !peelScopedWrappers) {
					break;
				}
				n.distinct = true;
				cur = d.getArg();
				changed = true;
				continue;
			}

			if (cur instanceof Reduced) {
				final Reduced r = (Reduced) cur;
				if (r.isVariableScopeChange() && !peelScopedWrappers) {
					break;
				}
				n.reduced = true;
				cur = r.getArg();
				changed = true;
				continue;
			}

			if (cur instanceof Order) {
				final Order o = (Order) cur;
				if (o.isVariableScopeChange() && !peelScopedWrappers) {
					break;
				}
				n.orderBy.addAll(o.getElements());
				cur = o.getArg();
				changed = true;
				continue;
			}

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
							n.extensionAssignments.putIfAbsent(ee.getName(), ee.getExpr());
							n.extensionOutputNames.add(ee.getName());
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
			}

			// Projection (record header once, then stop peeling so nested projections become subselects)
			if (cur instanceof Projection) {
				if (n.projection != null) {
					// We've already captured the top-level SELECT header; leave this Projection in-place
					// so it is rendered as a SUBSELECT in the WHERE by the IR builder.
					break;
				}
				n.projection = (Projection) cur;
				cur = n.projection.getArg();
				changed = true;
				continue;
			}

			// Keep BIND chains inside WHERE: stop peeling when we hit the first nested Extension, otherwise peel and
			// remember bindings for reinsertion later.
			if (cur instanceof Extension) {
				if (((Extension) cur).getArg() instanceof Extension
						&& !extensionChainLeadsToHavingFilter((Extension) cur)) {
					break;
				}
				final Extension ext = (Extension) cur;
				for (final ExtensionElem ee : ext.getElements()) {
					n.selectAssignments.put(ee.getName(), ee.getExpr());
					n.extensionOutputNames.add(ee.getName());
					n.extensionAssignments.putIfAbsent(ee.getName(), ee.getExpr());
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
						n.extensionAssignments.putIfAbsent(ee.getName(), ee.getExpr());
						n.extensionOutputNames.add(ee.getName());
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

	private static boolean isHavingCandidate(ValueExpr cond, Set<String> groupVars, Set<String> aggregateAliasVars) {
		Set<String> free = freeVars(cond);
		if (free.isEmpty()) {
			return true; // constant condition → valid HAVING
		}
		// Accept conditions that only refer to GROUP BY variables or aggregate aliases
		for (String v : free) {
			if (!groupVars.contains(v) && !aggregateAliasVars.contains(v)) {
				return false;
			}
		}
		return true;
	}

	private static boolean containsExtension(TupleExpr e) {
		if (e == null) {
			return false;
		}
		class Flag extends AbstractQueryModelVisitor<RuntimeException> {
			boolean found = false;

			@Override
			public void meet(Extension node) {
				found = true;
			}

			@Override
			protected void meetNode(QueryModelNode node) {
				if (!found) {
					super.meetNode(node);
				}
			}
		}
		Flag f = new Flag();
		e.visit(f);
		return f.found;
	}

	/**
	 * Detect Extension nodes only in the current WHERE scope, ignoring nested subselects (Projection nodes) to avoid
	 * suppressing projection expressions due to bindings inside subqueries.
	 */
	private static boolean containsExtensionShallow(TupleExpr e) {
		if (e == null) {
			return false;
		}
		class Flag extends AbstractQueryModelVisitor<RuntimeException> {
			boolean found = false;

			@Override
			public void meet(Extension node) {
				found = true;
			}

			@Override
			public void meet(Projection node) {
				// Do not descend into subselects; they are rendered separately.
			}

			@Override
			protected void meetNode(QueryModelNode node) {
				if (!found) {
					super.meetNode(node);
				}
			}
		}
		Flag f = new Flag();
		e.visit(f);
		return f.found;
	}

	private static void applyAggregateHoisting(final Normalized n) {
		final AggregateScan scan = new AggregateScan();
		if (n.where != null) {
			n.where.visit(scan);
		}

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
			return containsAggregate(((And) e).getLeftArg()) || containsAggregate(((And) e).getRightArg());
		}
		if (e instanceof Or) {
			return containsAggregate(((Or) e).getLeftArg())
					|| containsAggregate(((Or) e).getRightArg());
		}
		if (e instanceof Compare) {
			return containsAggregate(((Compare) e).getLeftArg()) || containsAggregate(((Compare) e).getRightArg());
		}
		if (e instanceof SameTerm) {
			return containsAggregate(((SameTerm) e).getLeftArg()) || containsAggregate(((SameTerm) e).getRightArg());
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
			return containsAggregate(((MathExpr) e).getLeftArg()) || containsAggregate(((MathExpr) e).getRightArg());
		}
		return false;
	}

	private static Set<String> freeVars(ValueExpr e) {
		Set<String> out = new LinkedHashSet<>();
		collectVarNames(e, out);
		return out;
	}

	private static void collectVarNames(ValueExpr e, Set<String> acc) {
		if (e == null) {
			return;
		}
		if (e instanceof Var) {
			Var v = (Var) e;
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
			Regex rx = (Regex) e;
			collectVarNames(rx.getArg(), acc);
			collectVarNames(rx.getPatternArg(), acc);
			if (rx.getFlagsArg() != null) {
				collectVarNames(rx.getFlagsArg(), acc);
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
			List<ValueExpr> args = ((ListMemberOperator) e).getArguments();
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
			If iff = (If) e;
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

	private static void flattenJoin(TupleExpr expr, List<TupleExpr> out) {
		if (expr instanceof Join) {
			final Join j = (Join) expr;
			flattenJoin(j.getLeftArg(), out);
			flattenJoin(j.getRightArg(), out);
		} else {
			out.add(expr);
		}
	}

	private static void flattenUnion(TupleExpr e, List<TupleExpr> out) {
		if (e instanceof Union) {
			Union u = (Union) e;
			if (u.isVariableScopeChange()) {
				// Preserve nested UNIONs whenever either child is itself a UNION with an
				// explicit variable-scope change: keep that UNION as a branch rather than
				// flattening into this level. This retains the original grouping braces
				// expected by scope-sensitive tests.
				if (u.getLeftArg() instanceof Union && ((Union) u.getLeftArg()).isVariableScopeChange()) {
					out.add(u.getLeftArg());
				} else if (u.getLeftArg() instanceof Union && !((Union) u.getLeftArg()).isVariableScopeChange()) {
					// Child UNION without scope-change: keep as a single branch (do not inline),
					// matching how RDF4J marks grouping in pretty-printed algebra.
					out.add(u.getLeftArg());
				} else {
					flattenUnion(u.getLeftArg(), out);
				}
				if (u.getRightArg() instanceof Union && ((Union) u.getRightArg()).isVariableScopeChange()) {
					out.add(u.getRightArg());
				} else if (u.getRightArg() instanceof Union && !((Union) u.getRightArg()).isVariableScopeChange()) {
					out.add(u.getRightArg());
				} else {
					flattenUnion(u.getRightArg(), out);
				}
			} else {
				flattenUnion(u.getLeftArg(), out);
				flattenUnion(u.getRightArg(), out);
			}
		} else {
			out.add(e);
		}
	}

	private static boolean sameVar(Var a, Var b) {
		return VarUtils.sameVar(a, b);
	}

	private static String freeVarName(Var v) {
		if (v == null || v.hasValue()) {
			return null;
		}
		final String n = v.getName();
		return (n == null || n.isEmpty()) ? null : n;
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

	private static Var getContextVarSafe(Object node) {
		if (node instanceof StatementPattern) {
			return getContextVarSafe((StatementPattern) node);
		}
		try {
			Method m = node.getClass().getMethod("getContextVar");
			Object ctx = m.invoke(node);
			if (ctx instanceof Var) {
				return (Var) ctx;
			}
		} catch (ReflectiveOperationException ignore) {
		}
		return null;
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

	private static boolean isAnonPathVar(Var v) {
		return VarUtils.isAnonPathVar(v);
	}

	private static boolean isAnonHavingName(String name) {
		return name != null && name.startsWith("_anon_having_");
	}

	private static boolean extensionChainLeadsToHavingFilter(Extension ext) {
		TupleExpr cur = ext;
		while (cur instanceof Extension) {
			cur = ((Extension) cur).getArg();
		}
		if (!(cur instanceof Filter)) {
			return false;
		}
		for (String name : freeVars(((Filter) cur).getCondition())) {
			if (isAnonHavingName(name)) {
				return true;
			}
		}
		return false;
	}

	// Render expressions for HAVING with substitution of _anon_having_* variables
	private String renderExprForHaving(final ValueExpr e, final Normalized n) {
		return renderExprWithSubstitution(e, n == null ? null : n.selectAssignments);
	}

	private String renderExprWithSubstitution(final ValueExpr e, final Map<String, ValueExpr> subs) {
		if (e == null) {
			return "()";
		}

		if (e instanceof Var) {
			final Var v = (Var) e;
			if (!v.hasValue() && v.getName() != null && isAnonHavingName(v.getName()) && subs != null) {
				ValueExpr repl = subs.get(v.getName());
				if (repl != null) {
					return renderExpr(repl);
				}
			}
			return v.hasValue() ? convertValueToString(v.getValue()) : "?" + v.getName();
		}

		if (e instanceof Not) {
			String inner = ExprTextUtils
					.stripRedundantOuterParens(renderExprWithSubstitution(((Not) e).getArg(), subs));
			return "!" + ExprTextUtils.parenthesizeIfNeededSimple(inner);
		}
		if (e instanceof And) {
			And a = (And) e;
			return "(" + renderExprWithSubstitution(a.getLeftArg(), subs) + " && "
					+ renderExprWithSubstitution(a.getRightArg(), subs) + ")";
		}
		if (e instanceof Or) {
			Or o = (Or) e;
			return "(" + renderExprWithSubstitution(o.getLeftArg(), subs) + " || "
					+ renderExprWithSubstitution(o.getRightArg(), subs) + ")";
		}
		if (e instanceof Compare) {
			Compare c = (Compare) e;
			return "(" + renderExprWithSubstitution(c.getLeftArg(), subs) + " "
					+ op(c.getOperator()) + " "
					+ renderExprWithSubstitution(c.getRightArg(), subs) + ")";
		}
		if (e instanceof SameTerm) {
			SameTerm st = (SameTerm) e;
			return "sameTerm(" + renderExprWithSubstitution(st.getLeftArg(), subs) + ", "
					+ renderExprWithSubstitution(st.getRightArg(), subs) + ")";
		}

		// fallback to normal rendering
		return renderExpr(e);
	}

	// ---------------- Path recognition helpers ----------------

	// Build textual path expression for an ArbitraryLengthPath using converter internals
	private String buildPathExprForArbitraryLengthPath(final ArbitraryLengthPath p) {
		final PathNode inner = parseAPathInner(p.getPathExpression(), p.getSubjectVar(), p.getObjectVar());
		if (inner == null) {
			throw new IllegalStateException(
					"Failed to parse ArbitraryLengthPath inner expression: " + p.getPathExpression());
		}
		final long min = p.getMinLength();
		final long max = -1L;
		final PathNode q = new PathQuant(inner, min, max);
		return (q.prec() < PREC_SEQ ? "(" + q.render() + ")" : q.render());
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

	public IrSelect toIRSelect(final TupleExpr tupleExpr) {
		final Normalized n = normalize(tupleExpr, false);
		applyAggregateHoisting(n);
		final boolean whereHasExtensions = containsExtensionShallow(n.where);

		final IrSelect ir = new IrSelect(false);
		// Canonicalize DISTINCT/REDUCED: if DISTINCT is set, REDUCED is a no-op and removed
		ir.setDistinct(n.distinct);
		ir.setReduced(n.reduced && !n.distinct);
		ir.setLimit(n.limit);
		ir.setOffset(n.offset);

		// Projection header
		if (n.projection != null && n.projection.getProjectionElemList() != null
				&& !n.projection.getProjectionElemList().getElements().isEmpty()) {
			for (ProjectionElem pe : n.projection.getProjectionElemList().getElements()) {
				final String alias = pe.getProjectionAlias().orElse(pe.getName());
				ExtensionElem src = pe.getSourceExpression();
				ValueExpr expr = src != null ? src.getExpr() : n.selectAssignments.get(alias);
				boolean renderExprText = expr != null;
				ir.getProjection().add(new IrProjectionItem(renderExprText ? renderExpr(expr) : null, alias));
			}
		} else if (!n.selectAssignments.isEmpty()) {
			if (!n.groupByTerms.isEmpty()) {
				for (GroupByTerm t : n.groupByTerms) {
					ir.getProjection().add(new IrProjectionItem(null, t.var));
				}
			} else {
				for (String v : n.syntheticProjectVars) {
					ir.getProjection().add(new IrProjectionItem(null, v));
				}
			}
			for (Entry<String, ValueExpr> e : n.selectAssignments.entrySet()) {
				ir.getProjection().add(new IrProjectionItem(renderExpr(e.getValue()), e.getKey()));
			}
		}

		// WHERE as textual-IR (raw)
		final IRBuilder builder = new IRBuilder();
		ir.setWhere(builder.build(n.where));

		// Re-insert non-aggregate BIND assignments that were peeled during normalization so they remain visible in
		// the WHERE clause. Constant bindings go first; bindings that depend on other variables are appended at the
		// end.
		// Skip aliases that are already rendered in SELECT or already expressed via GROUP BY (expr AS ?var).
		if (!n.extensionAssignments.isEmpty() && ir.getWhere() != null) {
			Set<String> alreadyRendered = new LinkedHashSet<>();
			ir.getProjection().forEach(p -> {
				if (p.getExprText() != null && p.getVarName() != null) {
					alreadyRendered.add(p.getVarName());
				}
			});

			Map<String, ValueExpr> groupAliasExprByVar = new LinkedHashMap<>();
			for (GroupByTerm t : n.groupByTerms) {
				if (t.expr != null) {
					groupAliasExprByVar.put(t.var, t.expr);
				}
			}

			List<IrNode> prefixConst = new ArrayList<>();
			List<IrNode> suffixDependent = new ArrayList<>();
			for (Entry<String, ValueExpr> e : n.extensionAssignments.entrySet()) {
				ValueExpr expr = e.getValue();
				if (expr instanceof AggregateOperator) {
					continue;
				}
				if (alreadyRendered.contains(e.getKey())) {
					continue; // already captured via SELECT expression
				}
				if (groupAliasExprByVar.containsKey(e.getKey())
						&& groupAliasExprByVar.get(e.getKey()).equals(expr)) {
					continue; // already represented as GROUP BY (expr AS ?var)
				}

				Set<String> deps = freeVars(expr);
				IrBind bind = new IrBind(renderExpr(expr), e.getKey(), false);
				if (deps.isEmpty()) {
					prefixConst.add(bind);
				} else {
					suffixDependent.add(bind);
				}
			}
			if (!prefixConst.isEmpty() || !suffixDependent.isEmpty()) {
				IrBGP whereBgp = ir.getWhere();
				IrBGP combined = new IrBGP(whereBgp.isNewScope());
				combined.getLines().addAll(prefixConst);
				if (whereBgp.getLines() != null) {
					combined.getLines().addAll(whereBgp.getLines());
				}
				combined.getLines().addAll(suffixDependent);
				ir.setWhere(combined);
			}
		}

		// GROUP BY
		for (GroupByTerm t : n.groupByTerms) {
			ir.getGroupBy().add(new IrGroupByElem(t.expr == null ? null : renderExpr(t.expr), t.var));
		}

		// HAVING
		for (ValueExpr cond : n.havingConditions) {
			ir.getHaving().add(ExprTextUtils.stripRedundantOuterParens(renderExprForHaving(cond, n)));
		}

		// ORDER BY
		for (OrderElem oe : n.orderBy) {
			ir.getOrderBy().add(new IrOrderSpec(renderExpr(oe.getExpr()), oe.isAscending()));
		}

		return ir;
	}

	private PathNode parseAPathInner(final TupleExpr innerExpr, final Var subj, final Var obj) {
		if (innerExpr instanceof StatementPattern) {
			PathNode n = parseAtomicFromStatement((StatementPattern) innerExpr, subj, obj);
			if (n != null) {
				return n;
			}
		}
		if (innerExpr instanceof Union) {
			PathNode nps = tryParseNegatedPropertySetFromUnion(innerExpr, subj, obj);
			if (nps != null) {
				return nps;
			}
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
		if (innerExpr instanceof Join) {
			PathNode seq = tryParseJoinOfUnionAndZeroOrOne(innerExpr, subj);
			if (seq != null) {
				return seq;
			}
			seq = buildPathSequenceFromJoinAllowingUnions(innerExpr, subj, obj);
			if (seq != null) {
				return seq;
			}
		}
		{
			PathNode seq = buildPathSequenceFromChain(innerExpr, subj, obj);
			return seq;
		}
	}

	private PathNode buildPathSequenceFromJoinAllowingUnions(final TupleExpr expr, final Var subj, final Var obj) {
		List<TupleExpr> parts = new ArrayList<>();
		flattenJoin(expr, parts);
		if (parts.isEmpty()) {
			return null;
		}
		Var cur = subj;
		List<PathNode> steps = new ArrayList<>();
		for (int i = 0; i < parts.size(); i++) {
			TupleExpr part = parts.get(i);
			boolean last = (i == parts.size() - 1);
			if (part instanceof StatementPattern) {
				StatementPattern sp = (StatementPattern) part;
				Var pv = sp.getPredicateVar();
				if (!isConstIriVar(pv)) {
					return null;
				}
				Var ss = sp.getSubjectVar();
				Var oo = sp.getObjectVar();
				if (sameVar(cur, ss) && (isAnonPathVar(oo) || (last && sameVar(oo, obj)))) {
					steps.add(new PathAtom(asIri(pv), false));
					cur = oo;
				} else if (sameVar(cur, oo) && (isAnonPathVar(ss) || (last && sameVar(ss, obj)))) {
					steps.add(new PathAtom(asIri(pv), true));
					cur = ss;
				} else {
					return null;
				}
			} else if (part instanceof Union) {
				List<TupleExpr> unions = new ArrayList<>();
				flattenUnion(part, unions);
				Var next = null;
				List<PathNode> alts = new ArrayList<>();
				for (TupleExpr u : unions) {
					if (!(u instanceof StatementPattern)) {
						return null;
					}
					StatementPattern sp = (StatementPattern) u;
					Var pv = sp.getPredicateVar();
					if (!isConstIriVar(pv)) {
						return null;
					}
					Var ss = sp.getSubjectVar();
					Var oo = sp.getObjectVar();
					boolean inv;
					Var mid;
					if (sameVar(cur, ss) && isAnonPathVar(oo)) {
						inv = false;
						mid = oo;
					} else if (sameVar(cur, oo) && isAnonPathVar(ss)) {
						inv = true;
						mid = ss;
					} else if (last && sameVar(ss, obj) && sameVar(cur, oo)) {
						inv = true;
						mid = ss;
					} else if (last && sameVar(oo, obj) && sameVar(cur, ss)) {
						inv = false;
						mid = oo;
					} else {
						return null;
					}
					if (next == null) {
						next = mid;
					} else if (!sameVar(next, mid)) {
						return null;
					}
					alts.add(new PathAtom((IRI) pv.getValue(), inv));
				}
				if (next == null) {
					return null;
				}
				cur = next;
				steps.add(alts.size() == 1 ? alts.get(0) : new PathAlt(alts));
			} else {
				return null;
			}
		}
		if (!sameVar(cur, obj) && !isAnonPathVar(cur)) {
			return null;
		}
		return steps.size() == 1 ? steps.get(0) : new PathSeq(steps);
	}

	private PathNode tryParseNegatedPropertySetFromUnion(final TupleExpr expr, final Var subj, final Var obj) {
		List<TupleExpr> leaves = new ArrayList<>();
		flattenUnion(expr, leaves);
		if (leaves.isEmpty()) {
			return null;
		}
		List<PathNode> members = new ArrayList<>();
		for (TupleExpr leaf : leaves) {
			if (!(leaf instanceof Filter)) {
				return null; // require Filter wrapping the single triple
			}
			Filter f = (Filter) leaf;
			if (!(f.getArg() instanceof StatementPattern)) {
				return null;
			}
			StatementPattern sp = (StatementPattern) f.getArg();
			if (!(f.getCondition() instanceof Compare)) {
				return null;
			}
			Compare cmp = (Compare) f.getCondition();
			if (cmp.getOperator() != CompareOp.NE) {
				return null;
			}
			Var pv;
			IRI bad;
			if (cmp.getLeftArg() instanceof Var && cmp.getRightArg() instanceof ValueConstant
					&& ((ValueConstant) cmp.getRightArg()).getValue() instanceof IRI) {
				pv = (Var) cmp.getLeftArg();
				bad = (IRI) ((ValueConstant) cmp.getRightArg()).getValue();
			} else if (cmp.getRightArg() instanceof Var && cmp.getLeftArg() instanceof ValueConstant
					&& ((ValueConstant) cmp.getLeftArg()).getValue() instanceof IRI) {
				pv = (Var) cmp.getRightArg();
				bad = (IRI) ((ValueConstant) cmp.getLeftArg()).getValue();
			} else {
				return null;
			}
			if (!sameVar(sp.getPredicateVar(), pv)) {
				return null;
			}
			boolean forward = sameVar(sp.getSubjectVar(), subj) && sameVar(sp.getObjectVar(), obj);
			boolean inverse = sameVar(sp.getSubjectVar(), obj) && sameVar(sp.getObjectVar(), subj);
			if (!forward && !inverse) {
				return null;
			}
			members.add(new PathAtom(bad, inverse));
		}
		PathNode inner = (members.size() == 1) ? members.get(0) : new PathAlt(members);
		return new PathNeg(inner);
	}

	private PathNode tryParseJoinOfUnionAndZeroOrOne(final TupleExpr expr, final Var subj) {
		List<TupleExpr> parts = new ArrayList<>();
		flattenJoin(expr, parts);
		if (parts.size() != 2 || !(parts.get(0) instanceof Union)) {
			return null;
		}
		Union u = (Union) parts.get(0);
		TupleExpr tailExpr = parts.get(1);
		FirstStepUnion first = parseFirstStepUnion(u, subj);
		if (first == null) {
			return null;
		}
		ZeroOrOneNode tail = parseZeroOrOneProjectionNode(tailExpr);
		if (tail == null) {
			return null;
		}
		if (!sameVar(first.mid, tail.s)) {
			return null;
		}
		List<PathNode> seqParts = new ArrayList<>();
		seqParts.add(first.node);
		seqParts.add(tail.node);
		return new PathSeq(seqParts);
	}

	private FirstStepUnion parseFirstStepUnion(final TupleExpr expr, final Var subj) {
		List<TupleExpr> branches = new ArrayList<>();
		flattenUnion(expr, branches);
		Var mid = null;
		List<PathNode> alts = new ArrayList<>();
		for (TupleExpr b : branches) {
			if (!(b instanceof StatementPattern)) {
				return null;
			}
			StatementPattern sp = (StatementPattern) b;
			Var ss = sp.getSubjectVar();
			Var oo = sp.getObjectVar();
			Var pv = sp.getPredicateVar();
			if (!isConstIriVar(pv)) {
				return null;
			}
			boolean inv;
			Var m;
			if (sameVar(subj, ss) && isAnonPathVar(oo)) {
				inv = false;
				m = oo;
			} else if (sameVar(subj, oo) && isAnonPathVar(ss)) {
				inv = true;
				m = ss;
			} else {
				return null;
			}
			if (mid == null) {
				mid = m;
			} else if (!sameVar(mid, m)) {
				return null;
			}
			alts.add(new PathAtom((IRI) pv.getValue(), inv));
		}
		if (mid == null) {
			return null;
		}
		PathNode n = (alts.size() == 1) ? alts.get(0) : new PathAlt(alts);
		return new FirstStepUnion(mid, n);
	}

	private ZeroOrOneNode parseZeroOrOneProjectionNode(final TupleExpr projOrDistinct) {
		// Recognize the UNION of a ZeroLengthPath and one or more non-zero chains expanded into a Projection
		// SELECT ?s ?o WHERE { { FILTER sameTerm(?s, ?o) } UNION { ...chain... } }
		TupleExpr cur = projOrDistinct;
		if (cur instanceof Distinct) {
			cur = ((Distinct) cur).getArg();
		}
		if (!(cur instanceof Projection)) {
			return null;
		}
		Projection proj = (Projection) cur;
		TupleExpr arg = proj.getArg();
		if (!(arg instanceof Union)) {
			return null;
		}
		List<TupleExpr> branches = new ArrayList<>();
		flattenUnion(arg, branches);
		Var s = null;
		Var o = null;
		// First pass: detect endpoints via ZeroLengthPath or Filter(sameTerm)
		for (TupleExpr branch : branches) {
			if (branch instanceof ZeroLengthPath) {
				ZeroLengthPath z = (ZeroLengthPath) branch;
				if (s == null && o == null) {
					s = z.getSubjectVar();
					o = z.getObjectVar();
				} else if (!sameVar(s, z.getSubjectVar()) || !sameVar(o, z.getObjectVar())) {
					return null;
				}
			} else if (branch instanceof Filter) {
				Filter f = (Filter) branch;
				if (f.getCondition() instanceof SameTerm) {
					SameTerm st = (SameTerm) f.getCondition();
					if (st.getLeftArg() instanceof Var && st.getRightArg() instanceof Var) {
						Var ls = (Var) st.getLeftArg();
						Var rs = (Var) st.getRightArg();
						if (s == null && o == null) {
							s = ls;
							o = rs;
						} else if (!sameVar(s, ls) || !sameVar(o, rs)) {
							return null;
						}
					} else {
						return null;
					}
				}
			}
		}
		if (s == null || o == null) {
			return null;
		}
		// Second pass: collect non-zero chains
		List<PathNode> seqs = new ArrayList<>();
		for (TupleExpr branch : branches) {
			if (branch instanceof ZeroLengthPath) {
				continue;
			}
			if (branch instanceof Filter && ((Filter) branch).getCondition() instanceof SameTerm) {
				continue;
			}
			PathNode seq = buildPathSequenceFromChain(branch, s, o);
			if (seq == null) {
				return null;
			}
			seqs.add(seq);
		}
		PathNode inner = (seqs.size() == 1) ? seqs.get(0) : new PathAlt(seqs);
		PathNode q = new PathQuant(inner, 0, 1);
		return new ZeroOrOneNode(s, q);
	}

	private PathNode parseAtomicFromStatement(final StatementPattern sp, final Var subj, final Var obj) {
		final Var ss = sp.getSubjectVar();
		final Var oo = sp.getObjectVar();
		final Var pv = sp.getPredicateVar();
		if (!isConstIriVar(pv)) {
			return null;
		}
		if (sameVar(subj, ss) && sameVar(oo, obj)) {
			return new PathAtom((IRI) pv.getValue(), false);
		}
		if (sameVar(subj, oo) && sameVar(ss, obj)) {
			return new PathAtom((IRI) pv.getValue(), true);
		}
		return null;
	}

	private PathNode buildPathSequenceFromChain(TupleExpr chain, Var s, Var o) {
		List<TupleExpr> flat = new ArrayList<>();
		TupleExprToIrConverter.flattenJoin(chain, flat);
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
				if (used.contains(sp)) {
					continue;
				}
				Var pv = sp.getPredicateVar();
				if (!isConstIriVar(pv)) {
					continue;
				}
				Var ss = sp.getSubjectVar();
				Var oo = sp.getObjectVar();
				if (sameVar(cur, ss) && (isAnonPathVar(oo) || sameVar(oo, o))) {
					steps.add(new PathAtom(asIri(pv), false));
					cur = oo;
					used.add(sp);
					advanced = true;
					break;
				} else if (sameVar(cur, oo) && (isAnonPathVar(ss) || sameVar(ss, o))) {
					steps.add(new PathAtom(asIri(pv), true));
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

	private interface PathNode {
		String render();

		int prec();
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

	private static final class PathNeg implements PathNode {
		final PathNode inner;

		PathNeg(PathNode inner) {
			this.inner = inner;
		}

		@Override
		public String render() {
			return "!(" + (inner == null ? "" : inner.render()) + ")";
		}

		@Override
		public int prec() {
			return PREC_ATOM;
		}
	}

	private static final class FirstStepUnion {
		final Var mid;
		final PathNode node;

		FirstStepUnion(Var mid, PathNode node) {
			this.mid = mid;
			this.node = node;
		}
	}

	// ---------------- IR Builder ----------------

	private static final class ZeroOrOneNode {
		final Var s;
		final PathNode node;

		ZeroOrOneNode(Var s, PathNode node) {
			this.s = s;
			this.node = node;
		}
	}

	final class IRBuilder extends AbstractQueryModelVisitor<RuntimeException> {
		private final IrBGP where = new IrBGP(false);
		private final Map<String, IrInlineTriple> inlineTriples;

		IRBuilder() {
			this.inlineTriples = new LinkedHashMap<>();
		}

		IRBuilder(Map<String, IrInlineTriple> shared) {
			this.inlineTriples = shared;
		}

		IrBGP build(final TupleExpr t) {
			if (t == null) {
				return where;
			}
			t.visit(this);
			return where;
		}

		private IRBuilder childBuilder() {
			return new IRBuilder(inlineTriples);
		}

		private IrFilter buildFilterFromCondition(final ValueExpr condExpr) {
			if (condExpr == null) {
				return new IrFilter((String) null, false);
			}
			// NOT EXISTS {...}
			if (condExpr instanceof Not && ((Not) condExpr).getArg() instanceof Exists) {
				final Exists ex = (Exists) ((Not) condExpr).getArg();
				IRBuilder inner = childBuilder();
				IrBGP bgp = inner.build(ex.getSubQuery());
				return new IrFilter(new IrNot(new IrExists(bgp, ex.isVariableScopeChange()), false), false);
			}
			// EXISTS {...}
			if (condExpr instanceof Exists) {
				final Exists ex = (Exists) condExpr;
				final TupleExpr sub = ex.getSubQuery();
				IRBuilder inner = childBuilder();
				IrBGP bgp = inner.build(sub);
				// If the root of the EXISTS subquery encodes an explicit variable-scope change in the
				// algebra (e.g., StatementPattern/Join/Filter with "(new scope)"), mark the inner BGP
				// as a new scope so that EXISTS renders with an extra brace layer: EXISTS { { ... } }.
				if (rootHasExplicitScope(sub)) {
					bgp.setNewScope(true);
				}

				IrExists exNode = new IrExists(bgp, false);
				return new IrFilter(exNode, false);
			}
			final String cond = ExprTextUtils.stripRedundantOuterParens(renderExpr(condExpr));
			return new IrFilter(cond, false);
		}

		public void meet(final StatementPattern sp) {
			final Var ctx = getContextVarSafe(sp);
			final IrStatementPattern node = new IrStatementPattern(sp.getSubjectVar(), sp.getPredicateVar(),
					sp.getObjectVar(), false);
			if (sp.getSubjectVar() != null) {
				IrInlineTriple inline = inlineTriples.get(sp.getSubjectVar().getName());
				if (inline != null) {
					node.setSubjectOverride(inline);
				}
			}
			if (sp.getObjectVar() != null) {
				IrInlineTriple inline = inlineTriples.get(sp.getObjectVar().getName());
				if (inline != null) {
					node.setObjectOverride(inline);
				}
			}
			if (ctx != null && (ctx.hasValue() || (ctx.getName() != null && !ctx.getName().isEmpty()))) {
				IrBGP inner = new IrBGP(false);
				inner.add(node);
				where.add(new IrGraph(ctx, inner, false));
			} else {
				where.add(node);
			}
		}

		@Override
		public void meet(final TripleRef tr) {
			Var exprVar = tr.getExprVar();
			if (exprVar != null && exprVar.getName() != null) {
				inlineTriples.put(exprVar.getName(),
						new IrInlineTriple(tr.getSubjectVar(), tr.getPredicateVar(), tr.getObjectVar()));
			}
			// Do not emit a line; TripleRef only defines an inline RDF-star triple term.
		}

		@Override
		public void meet(final Join join) {
			// Build left/right in isolation so we can respect explicit variable-scope changes
			// on either side by wrapping that side in its own GroupGraphPattern when needed.
			IRBuilder left = childBuilder();
			IrBGP wl = left.build(join.getLeftArg());
			IRBuilder right = childBuilder();
			IrBGP wr = right.build(join.getRightArg());

			boolean wrapLeft = rootHasExplicitScope(join.getLeftArg());
			boolean wrapRight = rootHasExplicitScope(join.getRightArg());

			if (join.isVariableScopeChange()) {
				IrBGP grp = new IrBGP(false);
				// Left side
				if (wrapLeft && !wl.getLines().isEmpty()) {
					IrBGP sub = new IrBGP(false);
					for (IrNode ln : wl.getLines()) {
						sub.add(ln);
					}
					grp.add(sub);
				} else {
					for (IrNode ln : wl.getLines()) {
						grp.add(ln);
					}
				}
				// Right side
				if (wrapRight && !wr.getLines().isEmpty()) {
					IrBGP sub = new IrBGP(false);
					for (IrNode ln : wr.getLines()) {
						sub.add(ln);
					}
					grp.add(sub);
				} else {
					for (IrNode ln : wr.getLines()) {
						grp.add(ln);
					}
				}
				where.add(grp);
				return;
			}

			// No join-level scope: append sides in order, wrapping each side if it encodes
			// an explicit scope change at its root.
			if (wrapLeft && !wl.getLines().isEmpty()) {
				IrBGP sub = new IrBGP(false);
				for (IrNode ln : wl.getLines()) {
					sub.add(ln);
				}
				where.add(sub);
			} else {
				for (IrNode ln : wl.getLines()) {
					where.add(ln);
				}
			}
			if (wrapRight && !wr.getLines().isEmpty()) {
				IrBGP sub = new IrBGP(false);
				for (IrNode ln : wr.getLines()) {
					sub.add(ln);
				}
				where.add(sub);
			} else {
				for (IrNode ln : wr.getLines()) {
					where.add(ln);
				}
			}
		}

		@Override
		public void meet(final LeftJoin lj) {
			if (lj.isVariableScopeChange()) {
				IRBuilder left = childBuilder();
				IrBGP wl = left.build(lj.getLeftArg());
				IRBuilder rightBuilder = childBuilder();
				IrBGP wr = rightBuilder.build(lj.getRightArg());
				if (lj.getCondition() != null) {
					wr.add(buildFilterFromCondition(lj.getCondition()));
				}
				// Build outer group with the left-hand side and the OPTIONAL.
				IrBGP grp = new IrBGP(false);
				for (IrNode ln : wl.getLines()) {
					grp.add(ln);
				}
				// Add the OPTIONAL with its body. Only add an extra grouping scope around the OPTIONAL body
				// when the ROOT of the right argument explicitly encoded a scope change in the original algebra.
				// This avoids introducing redundant braces for containers like SERVICE while preserving cases
				// such as OPTIONAL { { ... } } present in the source query.
				IrOptional opt = new IrOptional(wr, rootHasExplicitScope(lj.getRightArg()));
				grp.add(opt);
				// Do not mark the IrBGP itself as a new scope: IrBGP already prints a single pair of braces.
				// Setting newScope(true) here would cause an extra, redundant brace layer ({ { ... } }) that
				// does not appear in the original query text.
				where.add(grp);
				return;
			}
			lj.getLeftArg().visit(this);
			final IRBuilder rightBuilder = childBuilder();
			final IrBGP right = rightBuilder.build(lj.getRightArg());
			if (lj.getCondition() != null) {
				right.add(buildFilterFromCondition(lj.getCondition()));
			}
			where.add(new IrOptional(right, false));
		}

		@Override
		public void meet(final Filter f) {
			if (f.isVariableScopeChange() && f.getArg() instanceof SingletonSet) {
				IrBGP group = new IrBGP(false);
				group.add(buildFilterFromCondition(f.getCondition()));
				where.add(group);
				return;
			}

			final TupleExpr arg = f.getArg();
			Projection trailingProj = null;
			List<TupleExpr> head = null;
			if (arg instanceof Join) {
				final List<TupleExpr> flat = new ArrayList<>();
				flattenJoin(arg, flat);
				if (!flat.isEmpty()) {
					TupleExpr last = flat.get(flat.size() - 1);
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
					for (TupleExpr n : head) {
						n.visit(this);
					}
					where.add(buildFilterFromCondition(f.getCondition()));
					trailingProj.visit(this);
					return;
				}
			}

			// If this FILTER node signals a variable-scope change, wrap the FILTER together with
			// its argument patterns in a new IrBGP to preserve the explicit grouping encoded in
			// the algebra. This ensures shapes like "FILTER EXISTS { { ... } }" are rendered
			// with the inner braces as expected when a nested filter introduces a new scope.
			if (f.isVariableScopeChange()) {
				IRBuilder inner = childBuilder();
				IrBGP innerWhere = inner.build(arg);
				IrFilter irF = buildFilterFromCondition(f.getCondition());
				innerWhere.add(irF);
				where.add(innerWhere);
				return;
			}

			// Default: render the argument first, then append the FILTER line
			arg.visit(this);
			IrFilter irF = buildFilterFromCondition(f.getCondition());
			where.add(irF);
		}

		@Override
		public void meet(final SingletonSet s) {
			// no-op
		}

		@Override
		public void meet(final Union u) {
			final boolean leftIsU = u.getLeftArg() instanceof Union;
			final boolean rightIsU = u.getRightArg() instanceof Union;
			if (leftIsU && rightIsU) {
				final IrUnion irU = new IrUnion(u.isVariableScopeChange());
				irU.setNewScope(u.isVariableScopeChange());
				IRBuilder left = childBuilder();
				IrBGP wl = left.build(u.getLeftArg());
				if (rootHasExplicitScope(u.getLeftArg()) && !wl.getLines().isEmpty()) {
					IrBGP sub = new IrBGP(true);
					for (IrNode ln : wl.getLines()) {
						sub.add(ln);
					}
					irU.addBranch(sub);
				} else {
					irU.addBranch(wl);
				}
				IRBuilder right = childBuilder();
				IrBGP wr = right.build(u.getRightArg());
				if (rootHasExplicitScope(u.getRightArg()) && !wr.getLines().isEmpty()) {
					IrBGP sub = new IrBGP(false);
					for (IrNode ln : wr.getLines()) {
						sub.add(ln);
					}
					irU.addBranch(sub);
				} else {
					irU.addBranch(wr);
				}

				// Do not override explicit UNION scope based solely on trivial alternation shape.
				// Keep irU.newScope as provided by the algebra to preserve user grouping.
				where.add(irU);
				return;
			}
			final List<TupleExpr> branches = new ArrayList<>();
			flattenUnion(u, branches);
			final IrUnion irU = new IrUnion(u.isVariableScopeChange());
			irU.setNewScope(u.isVariableScopeChange());
			for (TupleExpr b : branches) {
				IRBuilder bld = childBuilder();
				IrBGP wb = bld.build(b);
				if (rootHasExplicitScope(b) && !wb.getLines().isEmpty()) {
					IrBGP sub = new IrBGP(true);
					for (IrNode ln : wb.getLines()) {
						sub.add(ln);
					}
					irU.addBranch(sub);
				} else {
					irU.addBranch(wb);
				}
			}

			// Do not override explicit UNION scope based solely on trivial alternation shape.
			// Keep irU.newScope as provided by the algebra to preserve user grouping.
			where.add(irU);
		}

		@Override
		public void meet(final Service svc) {
			IRBuilder inner = childBuilder();
			IrBGP w = inner.build(svc.getArg());
			// No conversion-time fusion; rely on pipeline transforms to normalize SERVICE bodies
			IrService irSvc = new IrService(renderVarOrValue(svc.getServiceRef()), svc.isSilent(), w, false);
			boolean scope = svc.isVariableScopeChange();
			if (scope) {
				IrBGP grp = new IrBGP(false);
				grp.add(irSvc);
				where.add(grp);
			} else {
				where.add(irSvc);
			}
		}

		@Override
		public void meet(final BindingSetAssignment bsa) {
			IrValues v = new IrValues(false);
			List<String> names = new ArrayList<>(bsa.getBindingNames());
			if (!cfg.valuesPreserveOrder) {
				Collections.sort(names);
			}
			v.getVarNames().addAll(names);
			for (BindingSet bs : bsa.getBindingSets()) {
				List<String> row = new ArrayList<>(names.size());
				for (String nm : names) {
					Value val = bs.getValue(nm);
					row.add(val == null ? "UNDEF" : convertValueToString(val));
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
				where.add(new IrBind(renderExpr(expr), ee.getName(), false));
			}
		}

		@Override
		public void meet(final Projection p) {
			IrSelect sub = toIRSelectRaw(p, r);
			boolean wrap = false;
			wrap |= !where.getLines().isEmpty();
			if (p.isVariableScopeChange()) {
				wrap = true;
			}
			IrSubSelect node = new IrSubSelect(sub, wrap);
			where.add(node);
		}

		@Override
		public void meet(final Slice s) {
			if (s.isVariableScopeChange()) {
				IrSelect sub = toIRSelectRaw(s, r);
				IrSubSelect node = new IrSubSelect(sub, true);
				where.add(node);
				return;
			}
			s.getArg().visit(this);
		}

		@Override
		public void meet(final Distinct d) {
			if (d.isVariableScopeChange()) {
				IrSelect sub = toIRSelectRaw(d, r);
				IrSubSelect node = new IrSubSelect(sub, true);
				where.add(node);
				return;
			}
			d.getArg().visit(this);
		}

		@Override
		public void meet(final Difference diff) {
			// Build left and right in isolation so we can respect variable-scope changes by
			// grouping them as a unit when required.
			IRBuilder left = childBuilder();
			IrBGP leftWhere = left.build(diff.getLeftArg());
			IRBuilder right = childBuilder();
			IrBGP rightWhere = right.build(diff.getRightArg());
			if (diff.isVariableScopeChange()) {
				IrBGP group = new IrBGP(false);
				for (IrNode ln : leftWhere.getLines()) {
					group.add(ln);
				}
				group.add(new IrMinus(rightWhere, false));
				where.add(group);
			} else {
				for (IrNode ln : leftWhere.getLines()) {
					where.add(ln);
				}
				where.add(new IrMinus(rightWhere, false));
			}
		}

		@Override
		public void meet(final ArbitraryLengthPath p) {
			final Var subj = p.getSubjectVar();
			final Var obj = p.getObjectVar();
			final String expr = TupleExprToIrConverter.this.buildPathExprForArbitraryLengthPath(p);
			final IrPathTriple pt = new IrPathTriple(subj, null, expr, obj, null, Collections.emptySet(),
					false);
			final Var ctx = getContextVarSafe(p);
			if (ctx != null && (ctx.hasValue() || (ctx.getName() != null && !ctx.getName().isEmpty()))) {
				IrBGP innerBgp = new IrBGP(false);
				innerBgp.add(pt);
				where.add(new IrGraph(ctx, innerBgp, false));
			} else {
				where.add(pt);
			}
		}

		@Override
		public void meet(final ZeroLengthPath p) {
			where.add(new IrText("FILTER "
					+ asConstraint(
							"sameTerm(" + renderVarOrValue(p.getSubjectVar()) + ", "
									+ renderVarOrValue(p.getObjectVar()) + ")"),
					false));
		}

		@Override
		public void meetOther(final QueryModelNode node) {
			where.add(new IrText("# unsupported node: " + node.getClass().getSimpleName(), false));
		}
	}

	/**
	 * True when the algebra root node encodes an explicit variable scope change that maps to an extra GroupGraphPattern
	 * in the original query. Excludes container nodes that already introduce their own structural block in surface
	 * syntax.
	 */
	private static boolean rootHasExplicitScope(final TupleExpr e) {
		if (e == null) {
			return false;
		}
		// Exclude containers: they already carry their own block syntax
		if (e instanceof Service
				|| e instanceof Union
				|| e instanceof Projection
				|| e instanceof Slice
				|| e instanceof Distinct
				|| e instanceof Group) {
			return false;
		}

		if (e instanceof AbstractQueryModelNode) {
			return ((AbstractQueryModelNode) e).isVariableScopeChange();
		}
		return false;
	}

	/** Public helper for renderer: whether the normalized root has explicit scope change. */
	public static boolean hasExplicitRootScope(final TupleExpr root) {
		final Normalized n = normalize(root, false);
		return rootHasExplicitScope(n.where);
	}

	private static final class GroupByTerm {
		final String var; // ?var
		final ValueExpr expr; // null => plain ?var; otherwise (expr AS ?var)

		GroupByTerm(String var, ValueExpr expr) {
			this.var = var;
			this.expr = expr;
		}
	}

	// ---------------- Local carriers ----------------

	private static final class Normalized {
		final List<OrderElem> orderBy = new ArrayList<>();
		final LinkedHashMap<String, ValueExpr> selectAssignments = new LinkedHashMap<>(); // alias -> expr
		final LinkedHashMap<String, ValueExpr> extensionAssignments = new LinkedHashMap<>(); // alias -> expr from BIND
		final Set<String> extensionOutputNames = new LinkedHashSet<>(); // vars bound via Extension/BIND in WHERE
		final List<GroupByTerm> groupByTerms = new ArrayList<>(); // explicit terms (var or (expr AS ?var))
		final List<String> syntheticProjectVars = new ArrayList<>(); // synthesized bare SELECT vars
		final List<ValueExpr> havingConditions = new ArrayList<>();
		final Set<String> groupByVarNames = new LinkedHashSet<>();
		final Set<String> aggregateOutputNames = new LinkedHashSet<>();
		Projection projection; // SELECT vars/exprs
		TupleExpr where; // WHERE pattern (group peeled)
		boolean distinct = false;
		boolean reduced = false;
		long limit = -1, offset = -1;
		boolean hadExplicitGroup = false; // true if a Group wrapper was present
	}

	private static final class AggregateScan extends AbstractQueryModelVisitor<RuntimeException> {
		final LinkedHashMap<String, ValueExpr> hoisted = new LinkedHashMap<>();
		final Map<String, Integer> varCounts = new LinkedHashMap<>();
		final Map<String, Integer> subjCounts = new LinkedHashMap<>();
		final Map<String, Integer> predCounts = new LinkedHashMap<>();
		final Map<String, Integer> objCounts = new LinkedHashMap<>();
		final Set<String> aggregateArgVars = new LinkedHashSet<>();
		final Set<String> aggregateOutputNames = new LinkedHashSet<>();

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

	private final class PathAtom implements PathNode {
		final IRI iri;
		final boolean inverse;

		PathAtom(IRI iri, boolean inverse) {
			this.iri = iri;
			this.inverse = inverse;
		}

		@Override
		public String render() {
			return (inverse ? "^" : "") + convertIRIToString(iri);
		}

		@Override
		public int prec() {
			return PREC_ATOM;
		}

	}
}
