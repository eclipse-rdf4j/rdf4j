/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.GraphPatternGroupable;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.VariableScopeChange;

/**
 * Utility methods for {@link TupleExpr} objects.
 *
 * @author Jeen Broekstra
 */
public class TupleExprs {

	/**
	 * Verifies if the supplied {@link TupleExpr} contains a {@link Projection} with the subquery flag set to true
	 * (default). If the supplied TupleExpr is a {@link Join} or contains a {@link Join}, projections inside that Join's
	 * arguments will not be taken into account.
	 *
	 * @param t a tuple expression.
	 * @return <code>true</code> if the TupleExpr contains a subquery projection (outside of a Join), <code>false</code>
	 *         otherwise.
	 */
	public static boolean containsSubquery(TupleExpr t) {
		Deque<TupleExpr> queue = new ArrayDeque<>();
		queue.add(t);
		while (!queue.isEmpty()) {
			TupleExpr n = queue.removeFirst();
			if (n instanceof Projection && ((Projection) n).isSubquery()) {
				return true;
			} else if (n instanceof Join) {
				// projections already inside a Join need not be
				// taken into account
				return false;
			} else {
				queue.addAll(getChildren(n));
			}
		}
		return false;
	}

	/**
	 * Verifies if the supplied {@link TupleExpr} represents a variable scope change.
	 *
	 * @param expr a {@link TupleExpr}
	 * @return <code>true</code> if the {@link TupleExpr} implements {@link VariableScopeChange} and has its scope
	 *         change flag set to <code>true</code>, <code>false</code> otherwise.
	 */
	public static boolean isVariableScopeChange(TupleExpr expr) {
		if (expr instanceof VariableScopeChange) {
			return ((VariableScopeChange) expr).isVariableScopeChange();
		}
		return false;
	}

	/**
	 * Verifies if the supplied {@link TupleExpr} represents a group graph pattern.
	 *
	 * @param expr a {@link TupleExpr}
	 * @return <code>true</code> if the {@link TupleExpr} is {@link GraphPatternGroupable} and has its graph pattern
	 *         group flag set to <code>true</code>, <code>false</code> otherwise.
	 *
	 * @deprecated since 3.2. Use {@link #isVariableScopeChange(TupleExpr)} instead.
	 */
	@Deprecated
	public static boolean isGraphPatternGroup(TupleExpr expr) {
		if (expr instanceof GraphPatternGroupable) {
			return ((GraphPatternGroupable) expr).isGraphPatternGroup();
		}
		return false;
	}

	/**
	 * Verifies if the supplied {@link TupleExpr} contains a {@link Projection}. If the supplied TupleExpr is a
	 * {@link Join} or contains a {@link Join}, projections inside that Join's arguments will not be taken into account.
	 *
	 * @param t a tuple expression.
	 * @return <code>true</code> if the TupleExpr contains a projection (outside of a Join), <code>false</code>
	 *         otherwise.
	 * @deprecated since 2.0. Use {@link #containsSubquery(TupleExpr)} instead.
	 */
	@Deprecated
	public static boolean containsProjection(TupleExpr t) {
		Deque<TupleExpr> queue = new ArrayDeque<>();
		queue.add(t);
		while (!queue.isEmpty()) {
			TupleExpr n = queue.removeFirst();
			if (n instanceof Projection) {
				return true;
			} else if (n instanceof Join) {
				// projections already inside a Join need not be
				// taken into account
				return false;
			} else {
				queue.addAll(getChildren(n));
			}
		}
		return false;
	}

	/**
	 * Returns {@link TupleExpr} children of the given node.
	 *
	 * @param t a tuple expression.
	 * @return a list of TupleExpr children.
	 */
	public static List<TupleExpr> getChildren(TupleExpr t) {
		final List<TupleExpr> children = new ArrayList<>(4);
		t.visitChildren(new AbstractQueryModelVisitor<RuntimeException>() {

			@Override
			public void meetNode(QueryModelNode node) {
				if (node instanceof TupleExpr) {
					children.add((TupleExpr) node);
				}
			}
		});
		return children;
	}

	/**
	 * Creates an (anonymous) Var representing a constant value. The variable name will be derived from the actual value
	 * to guarantee uniqueness.
	 *
	 * @param value
	 * @return an (anonymous) Var representing a constant value.
	 */
	public static Var createConstVar(Value value) {
		String varName = getConstVarName(value);
		Var var = new Var(varName);
		var.setConstant(true);
		var.setAnonymous(true);
		var.setValue(value);
		return var;
	}

	public static String getConstVarName(Value value) {
		if (value == null) {
			throw new IllegalArgumentException("value can not be null");
		}

		// We use toHexString to get a more compact stringrep.
		String uniqueStringForValue = Integer.toHexString(value.stringValue().hashCode());

		if (value instanceof Literal) {
			uniqueStringForValue += "_lit";

			// we need to append datatype and/or language tag to ensure a unique
			// var name (see SES-1927)
			Literal lit = (Literal) value;
			if (lit.getDatatype() != null) {
				uniqueStringForValue += "_" + Integer.toHexString(lit.getDatatype().hashCode());
			}
			if (lit.getLanguage() != null) {
				uniqueStringForValue += "_" + Integer.toHexString(lit.getLanguage().hashCode());
			}
		} else if (value instanceof BNode) {
			uniqueStringForValue += "_node";
		} else {
			uniqueStringForValue += "_uri";
		}

		return "_const_" + uniqueStringForValue;
	}

	/**
	 * Verifies if the supplied expression is a FILTER (NOT) EXISTS operation
	 *
	 * @param expr a tuple expression
	 * @return true if the supplied expression is a FILTER (NOT) EXISTS operation, false otherwise.
	 */
	public static boolean isFilterExistsFunction(TupleExpr expr) {
		if (expr instanceof Filter) {
			Filter filter = (Filter) expr;
			if (filter.getCondition() instanceof Exists) {
				return true;
			} else if (filter.getCondition() instanceof Not) {
				Not n = (Not) filter.getCondition();
				return (n.getArg() instanceof Exists);
			}
		}
		return false;
	}
}
