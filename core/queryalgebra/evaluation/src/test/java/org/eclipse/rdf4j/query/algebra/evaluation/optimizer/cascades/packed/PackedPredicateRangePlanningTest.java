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
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.packed;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.OptimizationGoal;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.junit.jupiter.api.Test;

/**
 * Backend-neutral activation tests for predicate-range facts in packed planning, driven by a synthetic
 * {@link PackedPredicateRangeProvider}.
 */
class PackedPredicateRangePlanningTest {

	private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();
	private static final IRI PREDICATE = VF.createIRI("http://example.com/intValued");

	/** Provider proving PREDICATE holds only the canonical xsd:int value 7. */
	private static final PackedPredicateRangeProvider INT_SEVEN_PROVIDER = new PackedPredicateRangeProvider() {

		@Override
		public boolean describeObjectRange(IRI predicate, PackedPredicateRange output) {
			if (!PREDICATE.equals(predicate)) {
				return false;
			}
			output.setState(PackedPredicateRange.STATE_KNOWN);
			output.setKindBits(PackedPredicateRange.KIND_LITERAL);
			output.setLanguageBits(PackedPredicateRange.LANGUAGE_WITHOUT);
			output.addDatatype(CoreDatatype.XSD.INT);
			output.setUniversalBits(
					PackedPredicateRange.UNIVERSAL_NUMBER | PackedPredicateRange.UNIVERSAL_CANONICAL_INTEGER);
			output.setIntegerBounds(7L, 7L);
			output.setFinite(true);
			output.addFiniteValue(VF.createLiteral("7", CoreDatatype.XSD.INT));
			output.setDescription("RdfTermDomain[LITERAL, INT, integerRange=[7, 7]]");
			return true;
		}

		@Override
		public long predicateRangeVersion() {
			return 7L;
		}
	};

	@Test
	void isLiteralContradictionSelectsEmptySet() {
		PackedPredicateRangeProvider iriOnly = new PackedPredicateRangeProvider() {

			@Override
			public boolean describeObjectRange(IRI predicate, PackedPredicateRange output) {
				output.setState(PackedPredicateRange.STATE_KNOWN);
				output.setKindBits(PackedPredicateRange.KIND_IRI);
				return true;
			}

			@Override
			public long predicateRangeVersion() {
				return 1L;
			}
		};
		TupleExpr root = projection(
				new Filter(statementPattern(), new IsLiteral(new Var("o"))), "s");

		TupleExpr selected = optimize(root, iriOnly);

		assertTrue(containsNode(selected, EmptySet.class),
				"isLiteral over an IRI-only range must offer and select EmptySet: " + selected);
	}

	@Test
	void equalityOutsideCanonicalIntegerBoundsSelectsEmptySet() {
		TupleExpr root = projection(new Filter(statementPattern(),
				new Compare(new Var("o"), new ValueConstant(VF.createLiteral("9", CoreDatatype.XSD.INT)),
						Compare.CompareOp.EQ)),
				"s");

		TupleExpr selected = optimize(root, INT_SEVEN_PROVIDER);

		assertTrue(containsNode(selected, EmptySet.class),
				"equality with 9 outside the [7, 7] range must select EmptySet: " + selected);
	}

	@Test
	void anchorJoinWithDisjointFiniteDomainSelectsEmptySet() {
		BindingSetAssignment anchor = new BindingSetAssignment();
		anchor.setBindingNames(java.util.Set.of("o"));
		MapBindingSet row = new MapBindingSet();
		row.addBinding("o", VF.createLiteral("9", CoreDatatype.XSD.INT));
		anchor.setBindingSets(List.of(row));
		TupleExpr root = projection(new Join(anchor, statementPattern()), "s");

		TupleExpr selected = optimize(root, INT_SEVEN_PROVIDER);

		assertTrue(containsNode(selected, EmptySet.class),
				"anchor {9} intersected with stored domain {7} is empty; expected EmptySet: " + selected);
	}

	@Test
	void equalityInsideBoundsKeepsPlanNonEmpty() {
		TupleExpr root = projection(new Filter(statementPattern(),
				new Compare(new Var("o"), new ValueConstant(VF.createLiteral("7", CoreDatatype.XSD.INT)),
						Compare.CompareOp.EQ)),
				"s");

		TupleExpr selected = optimize(root, INT_SEVEN_PROVIDER);

		assertFalse(containsNode(selected, EmptySet.class),
				"equality with 7 inside the [7, 7] range must not be proven empty: " + selected);
	}

	@Test
	void anchorJoinContradictionAlsoWinsUnderCostModel() {
		PackedCostModel costModel = new PackedCostModel() {

			@Override
			public double estimateRows(PackedQueryView query, int relationId) {
				return query.isStatementPattern(relationId) ? 5.0d : Double.NaN;
			}

			@Override
			public double estimateLocalWork(PackedQueryView query, int relationId, double outputRows) {
				return query.isStatementPattern(relationId) ? outputRows : Double.NaN;
			}

			@Override
			public void estimate(PackedQueryView query, int relationId, PackedCostContext context,
					PackedCostEstimate output) {
				if (!query.isStatementPattern(relationId)) {
					output.setRows(Double.NaN, Double.NaN);
					return;
				}
				output.setRows(5.0d, 5.0d);
			}
		};
		BindingSetAssignment anchor = new BindingSetAssignment();
		anchor.setBindingNames(java.util.Set.of("o"));
		MapBindingSet row = new MapBindingSet();
		row.addBinding("o", VF.createLiteral("9", CoreDatatype.XSD.INT));
		anchor.setBindingSets(List.of(row));
		TupleExpr root = projection(new Join(anchor, statementPattern()), "s");

		TupleExpr selected = PackedCascadesPlanner
				.optimize(root.clone(), OptimizationGoal.root(root, null), costModel, INT_SEVEN_PROVIDER)
				.selectedPlan();

		assertTrue(containsNode(selected, EmptySet.class),
				"the contradiction must also win when a cost model contextualizes the plan: " + selected);
	}

	@Test
	void noProviderKeepsExistingBehaviour() {
		TupleExpr root = projection(new Filter(statementPattern(), new IsLiteral(new Var("o"))), "s");

		TupleExpr selected = optimize(root, null);

		assertFalse(containsNode(selected, EmptySet.class), String.valueOf(selected));
	}

	private static TupleExpr optimize(TupleExpr root, PackedPredicateRangeProvider provider) {
		return PackedCascadesPlanner
				.optimize(root.clone(), OptimizationGoal.root(root, null), null, provider)
				.selectedPlan();
	}

	private static StatementPattern statementPattern() {
		Var predicate = new Var("p", PREDICATE, true, true);
		return new StatementPattern(new Var("s"), predicate, new Var("o"));
	}

	private static Projection projection(TupleExpr arg, String binding) {
		ProjectionElemList elements = new ProjectionElemList(new ProjectionElem(binding));
		return new Projection(arg, elements);
	}

	private static boolean containsNode(TupleExpr root, Class<? extends QueryModelNode> type) {
		boolean[] found = new boolean[1];
		root.visit(new AbstractQueryModelVisitor<RuntimeException>() {
			@Override
			protected void meetNode(QueryModelNode node) {
				if (type.isInstance(node)) {
					found[0] = true;
				}
				node.visitChildren(this);
			}
		});
		return found[0];
	}
}
