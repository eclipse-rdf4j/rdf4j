/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public abstract class LogicalOperatorConstraintComponent extends AbstractConstraintComponent {
	public LogicalOperatorConstraintComponent(Resource id) {
		super(id);
	}

	/**
	 * @param subject                      the subject from buildSparqlValidNodes_rsx_targetShape
	 * @param object                       the object from buildSparqlValidNodes_rsx_targetShape
	 * @param rdfsSubClassOfReasoner       the rdfsSubClassOfReasoner from buildSparqlValidNodes_rsx_targetShape
	 * @param scope                        the scope from buildSparqlValidNodes_rsx_targetShape
	 * @param stableRandomVariableProvider
	 * @param shapes                       the shapes from from the logical constraint (eg. and, or)
	 * @param targetChain                  the current targetChain
	 * @param bgpCombiner                  the SparqlFragment combiner for bgp or union fragments (eg.
	 *                                     SparqlFragment::join for AND; SparqlFragment::union for OR)
	 * @param filterCombiner               the SparqlFragment combiner for filter condition fragments (eg.
	 *                                     SparqlFragment::and for AND; SparqlFragment::or for OR)
	 * @return the new SparqlFragment that handles sh:and or sh:or for the shapes provided
	 */
	static SparqlFragment buildSparqlValidNodes_rsx_targetShape_inner(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, List<Shape> shapes,
			TargetChain targetChain,
			Function<List<SparqlFragment>, SparqlFragment> bgpCombiner,
			Function<List<SparqlFragment>, SparqlFragment> filterCombiner) {

		List<SparqlFragment> sparqlFragments = shapes.stream()
				.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object, rdfsSubClassOfReasoner,
						scope, stableRandomVariableProvider))
				.collect(Collectors.toList());

		if (scope == Scope.nodeShape) {

			if (!SparqlFragment.isFilterCondition(sparqlFragments)) {
				return bgpCombiner.apply(sparqlFragments);
			} else {
				return filterCombiner.apply(sparqlFragments);
			}

		} else if (scope == Scope.propertyShape) {

			if (!SparqlFragment.isFilterCondition(sparqlFragments)) {
				throw new UnsupportedOperationException();
			} else {

				assert targetChain.getPath().isPresent();

				Path path = targetChain.getPath().get();

				StatementMatcher.Variable filterNotExistsVariable = stableRandomVariableProvider.next();

				SparqlFragment filterCondition = filterCombiner.apply(shapes.stream()
						.map(c -> c.buildSparqlValidNodes_rsx_targetShape(subject, filterNotExistsVariable,
								rdfsSubClassOfReasoner,
								scope, stableRandomVariableProvider))
						.collect(Collectors.toList()));

				String pathQuery1 = path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner,
						stableRandomVariableProvider);
				String pathQuery2 = path.getTargetQueryFragment(subject, filterNotExistsVariable,
						rdfsSubClassOfReasoner, stableRandomVariableProvider);
				String pathQuery3 = path.getTargetQueryFragment(subject, stableRandomVariableProvider.next(),
						rdfsSubClassOfReasoner, stableRandomVariableProvider);

				// check that all values for the path from our subject match the filter condition
				String unionCondition1 = String.join("\n",
						pathQuery1,
						"FILTER ( NOT EXISTS {",
						pathQuery2,
						"FILTER(!("
								+ filterCondition.getFragment()
								+ "))",
						"})");

				// alternately there could be no values for the path from our subject, in which case the subject would
				// also be valid
				String unionCondition2 = String.join("\n",
						subject.asSparqlVariable() + " " + stableRandomVariableProvider.next().asSparqlVariable() + " "
								+ stableRandomVariableProvider.next().asSparqlVariable() + ".",
						"FILTER(NOT EXISTS {",
						pathQuery3,
						"})");

				// same as above, except we check for statements where our subject is actually used as an object in a
				// statement
				String unionCondition3 = String.join("\n",
						stableRandomVariableProvider.next().asSparqlVariable() + " "
								+ stableRandomVariableProvider.next().asSparqlVariable() + " "
								+ subject.asSparqlVariable() + ".",
						"FILTER(NOT EXISTS {",
						pathQuery3,
						"})");

				List<StatementMatcher> statementMatchers = SparqlFragment.getStatementMatchers(sparqlFragments);

				statementMatchers.add(new StatementMatcher(subject, null, null));
				statementMatchers.add(new StatementMatcher(null, null, subject));

				SparqlFragment sparqlFragment = SparqlFragment.union(unionCondition1, unionCondition2, unionCondition3);
				sparqlFragment.addStatementMatchers(statementMatchers);

				return sparqlFragment;
			}
		} else {
			throw new UnsupportedOperationException("Unknown scope: " + scope);
		}
	}

}
