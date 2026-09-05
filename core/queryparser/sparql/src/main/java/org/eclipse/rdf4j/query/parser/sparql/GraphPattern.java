/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;

/**
 * A graph pattern consisting of (required and optional) tuple expressions, binding assignments and boolean constraints.
 *
 * @author Arjohn Kampman
 * @apiNote This feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class GraphPattern {

	/**
	 * The context of this graph pattern.
	 */
	private Var contextVar;

	/**
	 * The StatementPattern-scope of this graph pattern.
	 */
	private StatementPattern.Scope spScope = StatementPattern.Scope.DEFAULT_CONTEXTS;

	/**
	 * The required tuple expressions in this graph pattern.
	 */
	private final List<TupleExpr> requiredTEs = new ArrayList<>();

	/**
	 * The optional tuple expressions in this graph pattern, as a list of Key-Value pairs with the tuple expression as
	 * the key and a list of constraints applicable to the tuple expression as the value.
	 */
	private final List<Map.Entry<TupleExpr, List<ValueExpr>>> optionalTEs = new ArrayList<>();

	/**
	 * The boolean constraints in this graph pattern.
	 */
	private List<ValueExpr> constraints = new ArrayList<>();

	/**
	 * Extension elements that must be bound before this group's constraints are evaluated but that are not tied to a
	 * position within the group: expression-valued components of a triple term used in a FILTER. They are applied over
	 * the group's tuple expression when it is built (or, for the ones registered while parsing a BIND expression, by
	 * the BIND itself).
	 */
	private final List<ExtensionElem> pendingExtensionElems = new ArrayList<>();

	private boolean built;

	/**
	 * Creates a new graph pattern.
	 */
	public GraphPattern() {
	}

	/**
	 * Creates a new graph pattern that inherits the context and scope from a parent graph pattern.
	 */
	public GraphPattern(GraphPattern parent) {
		contextVar = parent.contextVar;
		spScope = parent.spScope;
	}

	public void setContextVar(Var contextVar) {
		this.contextVar = contextVar;
	}

	public Var getContextVar() {
		return contextVar;
	}

	public void setStatementPatternScope(StatementPattern.Scope spScope) {
		this.spScope = spScope;
	}

	public StatementPattern.Scope getStatementPatternScope() {
		return spScope;
	}

	public void addRequiredTE(TupleExpr te) {
		requiredTEs.add(te);
	}

	/**
	 * Remove all values for required tuple expressions in this {@link GraphPattern}
	 */
	void clearRequiredTEs() {
		requiredTEs.clear();
		optionalTEs.clear();
	}

	public void addRequiredSP(Var subjVar, Var predVar, Var objVar) {

		addRequiredTE(new StatementPattern(spScope, subjVar, predVar, objVar,
				contextVar != null ? contextVar.clone() : null));
	}

	public List<TupleExpr> getRequiredTEs() {
		return Collections.unmodifiableList(requiredTEs);
	}

	/**
	 * add the supplied tuple expression as an optional expression, with a list of constraints that hold as conditions.
	 *
	 * @param te          a tuple expression
	 * @param constraints a list of constraints that form a condition for the LeftJoin to be formed from the optional
	 *                    TE.
	 */
	public void addOptionalTE(TupleExpr te, List<ValueExpr> constraints) {

		Map.Entry<TupleExpr, List<ValueExpr>> entry = new AbstractMap.SimpleImmutableEntry<>(te, constraints);
		optionalTEs.add(entry);
	}

	/**
	 * Retrieves the optional tuple expressions as a list of tuples with the tuple expression as the key and the list of
	 * value expressions as the value.
	 *
	 * @return a list of Map entries.
	 */
	public List<Map.Entry<TupleExpr, List<ValueExpr>>> getOptionalTEs() {
		return Collections.unmodifiableList(optionalTEs);
	}

	public void addConstraint(ValueExpr constraint) {
		constraints.add(constraint);
	}

	public void addConstraints(Collection<ValueExpr> constraints) {
		this.constraints.addAll(constraints);
	}

	public List<ValueExpr> getConstraints() {
		return Collections.unmodifiableList(constraints);
	}

	/**
	 * Registers an expression whose value must be bound to a (fresh) variable before this group's constraints are
	 * evaluated; see {@link #applyPendingExtensionElems(TupleExpr, int)}.
	 */
	public void addPendingExtensionElem(ExtensionElem elem) {
		pendingExtensionElems.add(elem);
	}

	public int getPendingExtensionElemCount() {
		return pendingExtensionElems.size();
	}

	/**
	 * Wraps {@code result} in one {@link Extension} per pending extension element registered at or after
	 * {@code fromIndex}, nested in registration order (the first registered element innermost, so later components may
	 * refer to earlier ones), and removes them from the pending list.
	 */
	public TupleExpr applyPendingExtensionElems(TupleExpr result, int fromIndex) {
		if (fromIndex >= pendingExtensionElems.size()) {
			return result;
		}
		List<ExtensionElem> elems = pendingExtensionElems.subList(fromIndex, pendingExtensionElems.size());
		for (ExtensionElem elem : elems) {
			result = new Extension(result, elem);
		}
		elems.clear();
		return result;
	}

	/**
	 * Whether {@link #buildTupleExpr()} has been called: expressions processed afterwards (SELECT, GROUP BY, HAVING,
	 * ORDER BY) can no longer contribute pending extension elements to this group.
	 */
	public boolean isBuilt() {
		return built;
	}

	public List<ValueExpr> removeAllConstraints() {
		List<ValueExpr> constraints = this.constraints;
		this.constraints = new ArrayList<>();
		return constraints;
	}

	/**
	 * Removes all tuple expressions and constraints.
	 */
	public void clear() {
		requiredTEs.clear();
		optionalTEs.clear();
		constraints.clear();
		pendingExtensionElems.clear();
		built = false;
	}

	/**
	 * Builds a combined tuple expression from the tuple expressions and constraints in this graph pattern.
	 *
	 * @return A tuple expression for this graph pattern.
	 */
	public TupleExpr buildTupleExpr() {
		TupleExpr result = buildJoinFromRequiredTEs();

		result = buildOptionalTE(result);

		// filters are position-independent within the group, so their triple-term component expressions are
		// bound over the complete group
		result = applyPendingExtensionElems(result, 0);

		for (ValueExpr constraint : constraints) {
			result = new Filter(result, constraint);
		}
		built = true;
		return result;
	}

	/**
	 * Build optionals to the supplied TE
	 */
	public TupleExpr buildOptionalTE(TupleExpr result) {
		for (Map.Entry<TupleExpr, List<ValueExpr>> entry : optionalTEs) {
			List<ValueExpr> constraints = entry.getValue();
			if (constraints != null && !constraints.isEmpty()) {
				ValueExpr condition = constraints.get(0);
				for (int i = 1; i < constraints.size(); i++) {
					condition = new And(condition, constraints.get(i));
				}

				result = new LeftJoin(result, entry.getKey(), condition);
			} else {
				result = new LeftJoin(result, entry.getKey());
			}
		}
		return result;
	}

	/**
	 * Build a single tuple expression representing _only_ the basic graph pattern, by joining the required TEs
	 */
	TupleExpr buildJoinFromRequiredTEs() {
		TupleExpr result;

		if (requiredTEs.isEmpty()) {
			result = new SingletonSet();
		} else {
			result = requiredTEs.get(0);

			for (int i = 1; i < requiredTEs.size(); i++) {
				TupleExpr te = requiredTEs.get(i);
				result = new Join(result, te);
			}
		}
		return result;
	}
}
