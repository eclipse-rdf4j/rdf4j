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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.StatementPatternCollector;

/**
 *
 * @apiNote This feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class ConstructorBuilder {

	public TupleExpr buildConstructor(TupleExpr bodyExpr, TupleExpr constructExpr, boolean distinct, boolean reduced) {
		return buildConstructor(bodyExpr, constructExpr, true, distinct, reduced);
	}

	public TupleExpr buildConstructor(TupleExpr bodyExpr, boolean distinct, boolean reduced)
			throws MalformedQueryException {
		// check that bodyExpr contains _only_ a basic graph pattern.
		BasicPatternVerifier verifier = new BasicPatternVerifier();
		bodyExpr.visit(verifier);

		if (!verifier.isBasicPattern()) {
			throw new MalformedQueryException(
					"can not use shorthand CONSTRUCT: graph pattern in WHERE clause is not a basic pattern.");
		}

		return buildConstructor(bodyExpr, bodyExpr, false, distinct, reduced);
	}

	private static class BasicPatternVerifier extends AbstractQueryModelVisitor<RuntimeException> {

		private boolean basicPattern = true;

		/**
		 * @param basicPattern The basicPattern to set.
		 */
		/**
		 * @return Returns the basicPattern.
		 */
		public boolean isBasicPattern() {
			return basicPattern;
		}

		@Override
		public void meet(LeftJoin node) {
			basicPattern = false;
		}

		@Override
		public void meet(Filter node) {
			basicPattern = false;
		}

		@Override
		public void meet(Extension node) {
			basicPattern = false;
		}

		@Override
		public void meet(Projection node) {
			basicPattern = false;
		}

		@Override
		public void meet(Union node) {
			basicPattern = false;
		}

		@Override
		public void meet(StatementPattern node) {
			if (!Scope.DEFAULT_CONTEXTS.equals(node.getScope())) {
				basicPattern = false;
			} else if (node.getContextVar() != null) {
				basicPattern = false;
			} else {
				super.meet(node);
			}
		}
	}

	private TupleExpr buildConstructor(TupleExpr bodyExpr, TupleExpr constructExpr, boolean explicitConstructor,
			boolean distinct, boolean reduced) {
		TupleExpr result = bodyExpr;

		// Retrieve all StatementPattern's from the construct expression
		List<StatementPattern> statementPatterns = StatementPatternCollector.process(constructExpr);

		Set<Var> constructVars = getConstructVars(statementPatterns);

		// Note: duplicate elimination is a two-step process. The first step
		// removes duplicates from the set of constructor variables. After this
		// step, any bnodes that need to be generated are added to each solution
		// and these solutions are projected to subject-predicate-object bindings.
		// Finally, the spo-bindings are again filtered for duplicates.
		if (distinct || reduced) {
			// Create projection that removes all bindings that are not used in the
			// constructor
			ProjectionElemList projElemList = new ProjectionElemList();

			for (Var var : constructVars) {
				// Ignore anonymous and constant vars, these will be handled after
				// the distinct
				if (!var.isAnonymous() && !var.hasValue()) {
					projElemList.addElement(new ProjectionElem(var.getName()));
				}
			}

			result = new Projection(result, projElemList);

			// Filter the duplicates from these projected bindings
			if (distinct) {
				result = new Distinct(result);
			} else {
				result = new Reduced(result);
			}
		}

		// Create BNodeGenerator's for all anonymous variables
		Map<Var, ExtensionElem> extElemMap = new HashMap<>();

		for (Var var : constructVars) {
			if (var.isAnonymous() && !extElemMap.containsKey(var)) {
				ValueExpr valueExpr = null;

				if (var.hasValue()) {
					valueExpr = new ValueConstant(var.getValue());
				} else if (explicitConstructor) {
					// only generate bnodes in case of an explicit constructor
					valueExpr = new BNodeGenerator();
				}

				if (valueExpr != null) {
					extElemMap.put(var, new ExtensionElem(valueExpr, var.getName()));
				}
			}
		}

		if (!extElemMap.isEmpty()) {
			result = new Extension(result, extElemMap.values());
		}

		// Create a Projection for each StatementPattern in the constructor
		List<ProjectionElemList> projections = new ArrayList<>();

		for (StatementPattern sp : statementPatterns) {
			ProjectionElemList projElemList = new ProjectionElemList();

			projElemList.addElement(new ProjectionElem(sp.getSubjectVar().getName(), "subject"));
			projElemList.addElement(new ProjectionElem(sp.getPredicateVar().getName(), "predicate"));
			projElemList.addElement(new ProjectionElem(sp.getObjectVar().getName(), "object"));

			projections.add(projElemList);
		}

		if (projections.size() == 1) {
			result = new Projection(result, projections.get(0));

			// Note: no need to apply the second duplicate elimination step if
			// there's just one projection
		} else if (projections.size() > 1) {
			result = new MultiProjection(result, projections);

			if (distinct) {
				// Add another distinct to filter duplicate statements
				result = new Distinct(result);
			} else if (reduced) {
				result = new Reduced(result);
			}
		} else {
			// Empty constructor
			result = new EmptySet();
		}

		return result;
	}

	/**
	 * Gets the set of variables that are relevant for the constructor. This method accumulates all subject, predicate
	 * and object variables from the supplied statement patterns, but ignores any context variables.
	 */
	private Set<Var> getConstructVars(Collection<StatementPattern> statementPatterns) {
		Set<Var> vars = new LinkedHashSet<>(statementPatterns.size() * 2);

		for (StatementPattern sp : statementPatterns) {
			vars.add(sp.getSubjectVar());
			vars.add(sp.getPredicateVar());
			vars.add(sp.getObjectVar());
		}

		return vars;
	}
}
