/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * Recognizes query shapes whose answer is the set of distinct combinations of some fields of a single statement
 * pattern, and evaluates them with a prefix-run scan (see {@link LmdbPrefixRunIterator}) instead of visiting every
 * matching statement:
 * <ul>
 * <li>{@code DISTINCT}/{@code REDUCED} over a projection of a statement pattern, e.g. {@code SELECT DISTINCT ?p WHERE {
 * ?s ?p ?o }};</li>
 * <li>{@code COUNT(DISTINCT ?x)} aggregates (without {@code GROUP BY}) over a statement pattern, e.g. {@code SELECT
 * (COUNT(DISTINCT ?type) AS ?c) WHERE { ?s rdf:type ?type }}; here the aggregate is computed from the scan and the
 * {@link Group} is replaced by a {@link BindingSetAssignment} holding the result, so that the remaining query is
 * evaluated as usual.</li>
 * </ul>
 * A shape only qualifies when it is evaluated without initial bindings, without a query dataset and outside a
 * transaction that could hold uncommitted changes, and when an index supports the required prefix-run.
 */
final class LmdbPrefixRunQuery {

	private LmdbPrefixRunQuery() {
	}

	/**
	 * Evaluates {@code tupleExpr} with a prefix-run scan when it is a {@code DISTINCT}/{@code REDUCED} projection of a
	 * single statement pattern.
	 *
	 * @return the result, or {@code null} when the query does not have that shape or no index supports it
	 */
	static CloseableIteration<BindingSet> evaluateDistinct(LmdbSailStore store, TupleExpr tupleExpr,
			boolean explicit) throws IOException {
		TupleExpr expr = unwrapRoot(tupleExpr);
		if (!(expr instanceof Distinct) && !(expr instanceof Reduced)) {
			return null;
		}
		TupleExpr arg = expr instanceof Distinct ? ((Distinct) expr).getArg() : ((Reduced) expr).getArg();
		StatementPattern pattern;
		List<ProjectionElem> projection;
		if (arg instanceof Projection) {
			Projection proj = (Projection) arg;
			if (!(proj.getArg() instanceof StatementPattern)) {
				return null;
			}
			pattern = (StatementPattern) proj.getArg();
			projection = proj.getProjectionElemList().getElements();
		} else if (arg instanceof StatementPattern) {
			pattern = (StatementPattern) arg;
			projection = new ArrayList<>();
			for (Var var : pattern.getVarList()) {
				if (!var.hasValue()) {
					projection.add(new ProjectionElem(var.getName()));
				}
			}
		} else {
			return null;
		}
		PatternFields fields = PatternFields.of(pattern);
		if (fields == null) {
			return null;
		}
		// projected field -> binding name; every projected variable must come from the pattern
		Set<Integer> distinctFields = new LinkedHashSet<>();
		List<String> bindingNames = new ArrayList<>();
		List<Integer> bindingFields = new ArrayList<>();
		for (ProjectionElem elem : projection) {
			Integer field = fields.fieldOf(elem.getName());
			if (field == null) {
				return null;
			}
			distinctFields.add(field);
			bindingNames.add(elem.getProjectionAlias().orElse(elem.getName()));
			bindingFields.add(field);
		}
		int[] prefixFields = distinctFields.stream().mapToInt(Integer::intValue).toArray();
		LmdbPrefixRunScan scan = store.openPrefixRunScan(explicit, prefixFields, fields.subj, fields.pred, fields.obj,
				fields.context, false);
		if (scan == null) {
			return null;
		}
		String[] names = bindingNames.toArray(new String[0]);
		int[] nameFields = bindingFields.stream().mapToInt(Integer::intValue).toArray();
		return new LookAheadIteration<>() {
			@Override
			protected BindingSet getNextElement() throws QueryEvaluationException {
				try {
					if (!scan.next()) {
						return null;
					}
					QueryBindingSet result = new QueryBindingSet(names.length);
					for (int i = 0; i < names.length; i++) {
						Value value = scan.getValue(nameFields[i]);
						if (value != null) {
							result.addBinding(names[i], value);
						}
					}
					return result;
				} catch (IOException e) {
					throw new QueryEvaluationException(e);
				}
			}

			@Override
			protected void handleClose() throws QueryEvaluationException {
				scan.close();
			}
		};
	}

	/**
	 * Rewrites {@code tupleExpr} when it projects only {@code COUNT(DISTINCT ?var)} aggregates over a single statement
	 * pattern without {@code GROUP BY}: each count is computed with a prefix-run scan and the {@link Group} node is
	 * replaced by a {@link BindingSetAssignment} carrying the counts.
	 *
	 * @return a rewritten copy of the query, or {@code null} when the query does not have that shape or no index
	 *         supports it
	 */
	static TupleExpr rewriteDistinctCounts(LmdbSailStore store, TupleExpr tupleExpr, boolean explicit,
			ValueFactory vf) throws IOException {
		TupleExpr expr = unwrapRoot(tupleExpr);
		if (!(expr instanceof Projection)) {
			return null;
		}
		TupleExpr arg = ((Projection) expr).getArg();
		if (arg instanceof Extension) {
			arg = ((Extension) arg).getArg();
		}
		if (!(arg instanceof Group)) {
			return null;
		}
		Group group = (Group) arg;
		if (!group.getGroupBindingNames().isEmpty() || group.getGroupElements().isEmpty()
				|| !(group.getArg() instanceof StatementPattern)) {
			return null;
		}
		PatternFields fields = PatternFields.of((StatementPattern) group.getArg());
		if (fields == null) {
			return null;
		}
		List<String> names = new ArrayList<>();
		List<Integer> countFields = new ArrayList<>();
		for (GroupElem elem : group.getGroupElements()) {
			if (!(elem.getOperator() instanceof Count)) {
				return null;
			}
			Count count = (Count) elem.getOperator();
			if (!count.isDistinct() || !(count.getArg() instanceof Var) || ((Var) count.getArg()).hasValue()) {
				return null;
			}
			Integer field = fields.fieldOf(((Var) count.getArg()).getName());
			if (field == null) {
				return null;
			}
			names.add(elem.getName());
			countFields.add(field);
		}
		for (int field : countFields) {
			if (!store.supportsPrefixRun(new int[] { field }, fields.subj != null, fields.pred != null,
					fields.obj != null, fields.context != null)) {
				return null;
			}
		}
		QueryBindingSet counts = new QueryBindingSet(names.size());
		Map<Integer, Long> countByField = new HashMap<>();
		for (int i = 0; i < names.size(); i++) {
			int field = countFields.get(i);
			Long count = countByField.get(field);
			if (count == null) {
				count = countDistinct(store, explicit, field, fields);
				countByField.put(field, count);
			}
			counts.addBinding(names.get(i), vf.createLiteral(Long.toString(count), CoreDatatype.XSD.INTEGER));
		}

		// replace the Group in a copy of the query so that the caller's query model stays untouched
		TupleExpr copy = tupleExpr.clone();
		TupleExpr copyExpr = unwrapRoot(copy);
		TupleExpr copyArg = ((Projection) copyExpr).getArg();
		if (copyArg instanceof Extension) {
			copyArg = ((Extension) copyArg).getArg();
		}
		BindingSetAssignment assignment = new BindingSetAssignment();
		assignment.setBindingNames(new LinkedHashSet<>(names));
		assignment.setBindingSets(List.of(counts));
		copyArg.replaceWith(assignment);
		return copy;
	}

	private static long countDistinct(LmdbSailStore store, boolean explicit, int field, PatternFields fields)
			throws IOException {
		try (LmdbPrefixRunScan scan = store.openPrefixRunScan(explicit, new int[] { field }, fields.subj,
				fields.pred, fields.obj, fields.context, false)) {
			if (scan == null) {
				throw new IllegalStateException("Prefix-run support was checked before opening the scan");
			}
			long count = 0;
			while (scan.next()) {
				if (field == TripleIndex.CONTEXT_IDX && scan.quad()[field] == 0L) {
					// the null context does not bind the context variable and is not counted
					continue;
				}
				count++;
			}
			return count;
		}
	}

	static boolean hasNoGraphs(Dataset dataset) {
		return dataset == null || (dataset.getDefaultGraphs().isEmpty() && dataset.getNamedGraphs().isEmpty());
	}

	private static TupleExpr unwrapRoot(TupleExpr tupleExpr) {
		return tupleExpr instanceof QueryRoot ? ((QueryRoot) tupleExpr).getArg() : tupleExpr;
	}

	/**
	 * The constant values and variable names of a statement pattern in the default context scope, or {@code null} when
	 * the pattern cannot be served by a prefix-run scan (named-graph scope or a variable used more than once).
	 */
	private static final class PatternFields {
		final Resource subj;
		final IRI pred;
		final Value obj;
		final Resource context;
		private final String[] varNames = new String[4];

		private PatternFields(Resource subj, IRI pred, Value obj, Resource context) {
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.context = context;
		}

		static PatternFields of(StatementPattern pattern) {
			if (pattern.getScope() != StatementPattern.Scope.DEFAULT_CONTEXTS) {
				return null;
			}
			Var subjVar = pattern.getSubjectVar();
			Var predVar = pattern.getPredicateVar();
			Var objVar = pattern.getObjectVar();
			Var ctxVar = pattern.getContextVar();
			Value subj = subjVar.getValue();
			Value pred = predVar.getValue();
			Value obj = objVar.getValue();
			Value ctx = ctxVar == null ? null : ctxVar.getValue();
			if ((subj != null && !(subj instanceof Resource)) || (pred != null && !(pred instanceof IRI))
					|| (ctx != null && !(ctx instanceof Resource))) {
				return null;
			}
			PatternFields fields = new PatternFields((Resource) subj, (IRI) pred, obj, (Resource) ctx);
			if (!fields.bind(TripleIndex.SUBJ_IDX, subjVar) || !fields.bind(TripleIndex.PRED_IDX, predVar)
					|| !fields.bind(TripleIndex.OBJ_IDX, objVar) || !fields.bind(TripleIndex.CONTEXT_IDX, ctxVar)) {
				return null;
			}
			return fields;
		}

		private boolean bind(int field, Var var) {
			if (var == null || var.hasValue()) {
				return true;
			}
			if (Arrays.asList(varNames).contains(var.getName())) {
				return false;
			}
			varNames[field] = var.getName();
			return true;
		}

		Integer fieldOf(String varName) {
			for (int field = 0; field < varNames.length; field++) {
				if (varName.equals(varNames[field])) {
					return field;
				}
			}
			return null;
		}
	}
}
