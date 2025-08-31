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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Coalesce;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Group;
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
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
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
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer.Config;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBind;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrExists;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGroupByElem;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNot;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOrderSpec;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
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
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.BaseTransform;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.FuseServiceNpsUnionLateTransform;

/**
 * Extracted converter that builds textual-IR from a TupleExpr.
 *
 * This class mirrors the TupleExpr→IR logic originally embedded in TupleExprIRRenderer; the renderer now delegates to
 * this converter to build IR, and handles printing separately.
 */
public class TupleExprToIrConverter {

	private static final int PREC_ALT = 1;
	private static final int PREC_SEQ = 2;

	// ---------------- Public entry points ----------------
	private static final int PREC_ATOM = 3;
	private final TupleExprIRRenderer r;

	// ---------------- Normalization and helpers ----------------

	public TupleExprToIrConverter(TupleExprIRRenderer renderer) {
		this.r = renderer;
	}

	/** Build IrSelect without running IR transforms (used for nested subselects). */
	public static IrSelect toIRSelectRaw(final TupleExpr tupleExpr, TupleExprIRRenderer r) {
		final Normalized n = normalize(tupleExpr, true);
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
					ir.getProjection().add(new IrProjectionItem(r.renderExprPublic(expr), alias));
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
				ir.getProjection().add(new IrProjectionItem(r.renderExprPublic(e.getValue()), e.getKey()));
			}
		}

		final IRBuilder builder = new TupleExprToIrConverter(r).new IRBuilder();
		ir.setWhere(builder.build(n.where));

		for (GroupByTerm t : n.groupByTerms) {
			ir.getGroupBy().add(new IrGroupByElem(t.expr == null ? null : r.renderExprPublic(t.expr), t.var));
		}
		for (ValueExpr cond : n.havingConditions) {
			ir.getHaving().add(TupleExprIRRenderer.stripRedundantOuterParens(renderExprForHaving(cond, n, r)));
		}
		for (OrderElem oe : n.orderBy) {
			ir.getOrderBy().add(new IrOrderSpec(r.renderExprPublic(oe.getExpr()), oe.isAscending()));
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
				if (u.getLeftArg() instanceof Union && !((Union) u.getLeftArg()).isVariableScopeChange()) {
					out.add(u.getLeftArg());
				} else {
					flattenUnion(u.getLeftArg(), out);
				}
				if (u.getRightArg() instanceof Union && !((Union) u.getRightArg()).isVariableScopeChange()) {
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
		if (a == null || b == null) {
			return false;
		}
		if (a.hasValue() || b.hasValue()) {
			return false;
		}
		return Objects.equals(a.getName(), b.getName());
	}

	private static String freeVarName(Var v) {
		if (v == null || v.hasValue()) {
			return null;
		}
		final String n = v.getName();
		return (n == null || n.isEmpty()) ? null : n;
	}

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
		return v != null && !v.hasValue() && v.getName() != null && v.getName().startsWith("_anon_path_");
	}

	private static boolean isAnonHavingName(String name) {
		return name != null && name.startsWith("_anon_having_");
	}

	// Render expressions for HAVING with substitution of _anon_having_* variables
	private static String renderExprForHaving(final ValueExpr e, final Normalized n, TupleExprIRRenderer r) {
		return renderExprWithSubstitution(e, n == null ? null : n.selectAssignments, r);
	}

	private static String renderExprWithSubstitution(final ValueExpr e, final Map<String, ValueExpr> subs,
			TupleExprIRRenderer r) {
		if (e == null) {
			return "()";
		}

		if (e instanceof Var) {
			final Var v = (Var) e;
			if (!v.hasValue() && v.getName() != null && isAnonHavingName(v.getName()) && subs != null) {
				ValueExpr repl = subs.get(v.getName());
				if (repl != null) {
					return r.renderExprPublic(repl);
				}
			}
			return v.hasValue() ? r.renderValuePublic(v.getValue()) : "?" + v.getName();
		}

		if (e instanceof Not) {
			String inner = TupleExprIRRenderer
					.stripRedundantOuterParens(renderExprWithSubstitution(((Not) e).getArg(), subs, r));
			return "!" + parenthesizeIfNeeded(inner);
		}
		if (e instanceof And) {
			And a = (And) e;
			return "(" + renderExprWithSubstitution(a.getLeftArg(), subs, r) + " && "
					+ renderExprWithSubstitution(a.getRightArg(), subs, r) + ")";
		}
		if (e instanceof Or) {
			Or o = (Or) e;
			return "(" + renderExprWithSubstitution(o.getLeftArg(), subs, r) + " || "
					+ renderExprWithSubstitution(o.getRightArg(), subs, r) + ")";
		}
		if (e instanceof Compare) {
			Compare c = (Compare) e;
			return "(" + renderExprWithSubstitution(c.getLeftArg(), subs, r) + " " + op(c.getOperator()) + " "
					+ renderExprWithSubstitution(c.getRightArg(), subs, r) + ")";
		}
		if (e instanceof SameTerm) {
			SameTerm st = (SameTerm) e;
			return "sameTerm(" + renderExprWithSubstitution(st.getLeftArg(), subs, r) + ", "
					+ renderExprWithSubstitution(st.getRightArg(), subs, r) + ")";
		}

		// fallback to normal rendering
		return r.renderExprPublic(e);
	}

	private static String parenthesizeIfNeeded(String s) {
		if (s == null) {
			return "()";
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return "()";
		}
		if (t.charAt(0) == '(') {
			return t; // assume already a grouped expression
		}
		return "(" + t + ")";
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

	// ---------------- Path recognition helpers ----------------

	// Build textual path expression for an ArbitraryLengthPath using converter internals
	private String buildPathExprForArbitraryLengthPath(final ArbitraryLengthPath p) {
		final PathNode inner = parseAPathInner(p.getPathExpression(), p.getSubjectVar(), p.getObjectVar());
		if (inner == null) {
			throw new IllegalStateException(
					"Failed to parse ArbitraryLengthPath inner expression: " + p.getPathExpression());
		}
		final long min = p.getMinLength();
		final long max = getMaxLengthSafe(p);
		final PathNode q = new PathQuant(inner, min, max);
		return (q.prec() < PREC_SEQ ? "(" + q.render() + ")" : q.render());
	}

	/** Convenience for rendering inline groups: build an IrBGP for a TupleExpr pattern. */
	public IrBGP buildWhere(final TupleExpr pattern) {
		return new IRBuilder().build(pattern);
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

		final IrSelect ir = new IrSelect();
		Config cfg = r.getConfig();
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
					ir.getProjection().add(new IrProjectionItem(r.renderExprPublic(expr), alias));
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
				ir.getProjection().add(new IrProjectionItem(r.renderExprPublic(e.getValue()), e.getKey()));
			}
		}

		// WHERE as textual-IR
		final IRBuilder builder = new IRBuilder();
		ir.setWhere(builder.build(n.where));

		if (cfg.debugIR) {
			System.out.println("# IR (raw)\n" + IrDebug.dump(ir));
		}

		// Transformations
		final IrSelect irTransformed = IrTransforms.transformUsingChildren(ir, r);
		ir.setWhere(irTransformed.getWhere());
		// Extra safeguard: ensure SERVICE union-of-NPS branches are fused after all passes
		ir.setWhere(FuseServiceNpsUnionLateTransform.apply(ir.getWhere()));

		if (cfg.debugIR) {
			System.out.println("# IR (transformed)\n" + IrDebug.dump(ir));
		}

		// GROUP BY
		for (GroupByTerm t : n.groupByTerms) {
			ir.getGroupBy().add(new IrGroupByElem(t.expr == null ? null : r.renderExprPublic(t.expr), t.var));
		}

		// HAVING
		for (ValueExpr cond : n.havingConditions) {
			ir.getHaving().add(TupleExprIRRenderer.stripRedundantOuterParens(renderExprForHaving(cond, n, r)));
		}

		// ORDER BY
		for (OrderElem oe : n.orderBy) {
			ir.getOrderBy().add(new IrOrderSpec(r.renderExprPublic(oe.getExpr()), oe.isAscending()));
		}

		return ir;
	}

	private Normalized normalize(final TupleExpr root) {
		return normalize(root, false);
	}

	private void handleUnsupported(String message) {
		if (r.getConfig().strict) {
			throw new TupleExprIRRenderer.SparqlRenderingException(message);
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
				if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
					return null;
				}
				Var ss = sp.getSubjectVar();
				Var oo = sp.getObjectVar();
				if (sameVar(cur, ss) && (isAnonPathVar(oo) || (last && sameVar(oo, obj)))) {
					steps.add(new PathAtom((IRI) pv.getValue(), false));
					cur = oo;
				} else if (sameVar(cur, oo) && (isAnonPathVar(ss) || (last && sameVar(ss, obj)))) {
					steps.add(new PathAtom((IRI) pv.getValue(), true));
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
					if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
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
			Var pv = null;
			IRI bad = null;
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
		if (members.isEmpty()) {
			return null;
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
			if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
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
		return new ZeroOrOneNode(s, o, q);
	}

	private PathNode parseAtomicFromStatement(final StatementPattern sp, final Var subj, final Var obj) {
		final Var ss = sp.getSubjectVar();
		final Var oo = sp.getObjectVar();
		final Var pv = sp.getPredicateVar();
		if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
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
				if (pv == null || !pv.hasValue() || !(pv.getValue() instanceof IRI)) {
					continue;
				}
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
		final Var o;
		final PathNode node;

		ZeroOrOneNode(Var s, Var o, PathNode node) {
			this.s = s;
			this.o = o;
			this.node = node;
		}
	}

	final class IRBuilder extends AbstractQueryModelVisitor<RuntimeException> {
		private final IrBGP where = new IrBGP();
		private final TupleExprIRRenderer r = TupleExprToIrConverter.this.r;

		IrBGP build(final TupleExpr t) {
			if (t == null) {
				return where;
			}
			t.visit(this);
			return where;
		}

		private IrFilter buildFilterFromCondition(final ValueExpr condExpr) {
			if (condExpr == null) {
				return new IrFilter((String) null);
			}
			// NOT EXISTS {...}
			if (condExpr instanceof Not && ((Not) condExpr).getArg() instanceof Exists) {
				final Exists ex = (Exists) ((Not) condExpr).getArg();
				IRBuilder inner = new IRBuilder();
				IrBGP bgp = inner.build(ex.getSubQuery());
				return new IrFilter(new IrNot(new IrExists(bgp, ex.isVariableScopeChange())));
			}
			// EXISTS {...}
			if (condExpr instanceof Exists) {
				final Exists ex = (Exists) condExpr;
				final TupleExpr sub = ex.getSubQuery();
				IRBuilder inner = new IRBuilder();
				IrBGP bgp = inner.build(sub);
				boolean newScope = false;
				if (sub instanceof Filter) {
					newScope = ((Filter) sub).isVariableScopeChange();
				} else if (sub instanceof Join) {
					if (((Join) sub).isVariableScopeChange()) {
						newScope = true;
					} else {
						List<TupleExpr> parts = new ArrayList<>();
						flattenJoin(sub, parts);
						for (TupleExpr te : parts) {
							if (te instanceof Filter && ((Filter) te).isVariableScopeChange()) {
								newScope = true;
								break;
							}
						}
					}
				}
				IrExists exNode = new IrExists(bgp, ex.isVariableScopeChange());
				if (newScope) {
					exNode.setNewScope(true);
					bgp.setNewScope(true);
				}
				return new IrFilter(exNode);
			}
			final String cond = TupleExprIRRenderer.stripRedundantOuterParens(r.renderExprPublic(condExpr));
			return new IrFilter(cond);
		}

		@Override
		public void meet(final StatementPattern sp) {
			final Var ctx = getContextVarSafe(sp);
			final IrStatementPattern node = new IrStatementPattern(sp.getSubjectVar(), sp.getPredicateVar(),
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
			if (join.isVariableScopeChange()) {
				IRBuilder left = new IRBuilder();
				IrBGP wl = left.build(join.getLeftArg());
				IRBuilder right = new IRBuilder();
				IrBGP wr = right.build(join.getRightArg());
				IrBGP grp = new IrBGP();
				for (IrNode ln : wl.getLines()) {
					grp.add(ln);
				}
				for (IrNode ln : wr.getLines()) {
					grp.add(ln);
				}
				grp.setNewScope(true);
				where.add(grp);
				return;
			}
			join.getLeftArg().visit(this);
			join.getRightArg().visit(this);
		}

		@Override
		public void meet(final LeftJoin lj) {
			if (lj.isVariableScopeChange()) {
				IRBuilder left = new IRBuilder();
				IrBGP wl = left.build(lj.getLeftArg());
				IRBuilder rightBuilder = new IRBuilder();
				IrBGP wr = rightBuilder.build(lj.getRightArg());
				if (lj.getCondition() != null) {
					wr.add(buildFilterFromCondition(lj.getCondition()));
				}
				// Build outer group with the left-hand side and the OPTIONAL.
				IrBGP grp = new IrBGP();
				for (IrNode ln : wl.getLines()) {
					grp.add(ln);
				}
				// Add the OPTIONAL with its body. Only add an extra grouping scope around the OPTIONAL body
				// when the ROOT of the right argument explicitly encoded a scope change in the original algebra.
				// This avoids introducing redundant braces for containers like SERVICE while preserving cases
				// such as OPTIONAL { { ... } } present in the source query.
				IrOptional opt = new IrOptional(wr);
				if (rootHasExplicitScope(lj.getRightArg())) {
					opt.setNewScope(true);
				}
				grp.add(opt);
				// Do not mark the IrBGP itself as a new scope: IrBGP already prints a single pair of braces.
				// Setting newScope(true) here would cause an extra, redundant brace layer ({ { ... } }) that
				// does not appear in the original query text.
				where.add(grp);
				return;
			}
			lj.getLeftArg().visit(this);
			final IRBuilder rightBuilder = new IRBuilder();
			final IrBGP right = rightBuilder.build(lj.getRightArg());
			if (lj.getCondition() != null) {
				right.add(buildFilterFromCondition(lj.getCondition()));
			}
			where.add(new IrOptional(right));
		}

		@Override
		public void meet(final Filter f) {
			if (f.isVariableScopeChange() && f.getArg() instanceof SingletonSet) {
				IrBGP group = new IrBGP();
				group.add(buildFilterFromCondition(f.getCondition()));
				group.setNewScope(true);
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

			arg.visit(this);
			IrFilter irF = buildFilterFromCondition(f.getCondition());
			if (f.isVariableScopeChange()) {
				irF.setNewScope(true);
			}
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
				final IrUnion irU = new IrUnion();
				irU.setNewScope(u.isVariableScopeChange());
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
			irU.setNewScope(u.isVariableScopeChange());
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
			// No conversion-time fusion; rely on pipeline transforms to normalize SERVICE bodies
			IrService irSvc = new IrService(r.renderVarOrValuePublic(svc.getServiceRef()), svc.isSilent(), w);
			boolean scope;
			try {
				// Prefer explicit scope change from the algebra node when available
				scope = (boolean) Service.class.getMethod("isVariableScopeChange").invoke(svc);
			} catch (ReflectiveOperationException e) {
				scope = false;
			}
			if (scope) {
				IrBGP grp = new IrBGP();
				grp.add(irSvc);
				where.add(grp);
			} else {
				where.add(irSvc);
			}
		}

		private String normalizeCompactNps(String path) {
			if (path == null)
				return null;
			String t = path.trim();
			if (t.isEmpty())
				return null;
			if (t.startsWith("!(") && t.endsWith(")"))
				return t;
			if (t.startsWith("!^")) {
				return "!(" + t.substring(1) + ")";
			}
			if (t.startsWith("!") && (t.length() == 1 || t.charAt(1) != '(')) {
				return "!(" + t.substring(1) + ")";
			}
			return null;
		}

		private String mergeNpsMembers(String a, String b) {
			// a,b are of the form !(...) ; merge inner members with '|'
			int a1 = a.indexOf('('), a2 = a.lastIndexOf(')');
			int b1 = b.indexOf('('), b2 = b.lastIndexOf(')');
			if (a1 < 0 || a2 < 0 || b1 < 0 || b2 < 0)
				return a; // fallback
			String ia = a.substring(a1 + 1, a2).trim();
			String ib = b.substring(b1 + 1, b2).trim();
			if (ia.isEmpty())
				return b;
			if (ib.isEmpty())
				return a;
			return "!(" + ia + "|" + ib + ")";
		}

		@Override
		public void meet(final BindingSetAssignment bsa) {
			IrValues v = new IrValues();
			List<String> names = new ArrayList<>(bsa.getBindingNames());
			if (!r.getConfig().valuesPreserveOrder) {
				Collections.sort(names);
			}
			v.getVarNames().addAll(names);
			for (BindingSet bs : bsa.getBindingSets()) {
				List<String> row = new ArrayList<>(names.size());
				for (String nm : names) {
					Value val = bs.getValue(nm);
					row.add(val == null ? "UNDEF" : r.renderValuePublic(val));
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
				where.add(new IrBind(r.renderExprPublic(expr), ee.getName()));
			}
		}

		@Override
		public void meet(final Projection p) {
			IrSelect sub = toIRSelectRaw(p, r);
			where.add(new IrSubSelect(sub));
		}

		@Override
		public void meet(final Slice s) {
			if (s.isVariableScopeChange()) {
				IrSelect sub = toIRSelectRaw(s, r);
				where.add(new IrSubSelect(sub));
				return;
			}
			s.getArg().visit(this);
		}

		@Override
		public void meet(final Distinct d) {
			if (d.isVariableScopeChange()) {
				IrSelect sub = toIRSelectRaw(d, r);
				where.add(new IrSubSelect(sub));
				return;
			}
			d.getArg().visit(this);
		}

		@Override
		public void meet(final Difference diff) {
			// Build left and right in isolation so we can respect variable-scope changes by
			// grouping them as a unit when required.
			IRBuilder left = new IRBuilder();
			IrBGP leftWhere = left.build(diff.getLeftArg());
			IRBuilder right = new IRBuilder();
			IrBGP rightWhere = right.build(diff.getRightArg());
			if (diff.isVariableScopeChange()) {
				IrBGP group = new IrBGP();
				group.setNewScope(true);
				for (IrNode ln : leftWhere.getLines()) {
					group.add(ln);
				}
				group.add(new IrMinus(rightWhere));
				where.add(group);
			} else {
				for (IrNode ln : leftWhere.getLines()) {
					where.add(ln);
				}
				where.add(new IrMinus(rightWhere));
			}
		}

		@Override
		public void meet(final ArbitraryLengthPath p) {
			final Var subj = p.getSubjectVar();
			final Var obj = p.getObjectVar();
			final String expr = TupleExprToIrConverter.this.buildPathExprForArbitraryLengthPath(p);
			final IrPathTriple pt = new IrPathTriple(subj, expr, obj);
			final Var ctx = getContextVarSafe(p);
			if (ctx != null && (ctx.hasValue() || (ctx.getName() != null && !ctx.getName().isEmpty()))) {
				IrBGP innerBgp = new IrBGP();
				innerBgp.add(pt);
				where.add(new IrGraph(ctx, innerBgp));
			} else {
				where.add(pt);
			}
		}

		@Override
		public void meet(final ZeroLengthPath p) {
			where.add(new IrText(
					"FILTER " + TupleExprIRRenderer.asConstraint(
							"sameTerm(" + r.renderVarOrValuePublic(p.getSubjectVar()) + ", "
									+ r.renderVarOrValuePublic(p.getObjectVar())
									+ ")")));
		}

		@Override
		public void meetOther(final QueryModelNode node) {
			where.add(new IrText("# unsupported node: " + node.getClass().getSimpleName()));
		}
	}

	/** Detects if any node in the subtree explicitly marks a variable scope change. */
	private static boolean containsVariableScopeChange(final TupleExpr expr) {
		if (expr == null) {
			return false;
		}
		final boolean[] seen = new boolean[] { false };
		expr.visit(new AbstractQueryModelVisitor<RuntimeException>() {
			@Override
			protected void meetNode(QueryModelNode node) {
				try {
					Method m = node.getClass().getMethod("isVariableScopeChange");
					Object v = m.invoke(node);
					if (v instanceof Boolean && ((Boolean) v)) {
						seen[0] = true;
					}
				} catch (ReflectiveOperationException ignore) {
				}
				super.meetNode(node);
			}
		});
		if (seen[0]) {
			return true;
		}
		// Fallback: rely on algebra string marker if reflective probing failed
		try {
			String s = String.valueOf(expr);
			if (s.contains("new scope")) {
				return true;
			}
		} catch (Throwable ignore) {
		}
		return false;
	}

	/** True if the algebra root is a container that prints its own structural block. */
	private static boolean rightArgIsContainer(final TupleExpr e) {
		if (e == null) {
			return false;
		}
		return (e instanceof Service)
				|| (e instanceof Union)
				|| (e instanceof Projection)
				|| (e instanceof Slice)
				|| (e instanceof Distinct)
				|| (e instanceof Group);
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
		try {
			Method m = e.getClass().getMethod("isVariableScopeChange");
			Object v = m.invoke(e);
			if (v instanceof Boolean) {
				return (Boolean) v;
			}
		} catch (ReflectiveOperationException ignore) {
		}
		// Fallback: use algebra's textual marker if present
		try {
			String s = String.valueOf(e);
			return s.contains("(new scope)");
		} catch (Throwable ignore) {
		}
		return false;
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
			return (inverse ? "^" : "") + r.renderIRI(iri);
		}

		@Override
		public int prec() {
			return PREC_ATOM;
		}

	}
}
