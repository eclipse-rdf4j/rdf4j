/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.AllTargetsPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChainRetriever;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

import com.google.common.collect.Sets;

public class SequencePath extends Path {

	private final List<Path> paths;

	public SequencePath(Resource id, ShapeSource shapeSource) {
		super(id);
		paths = ShaclAstLists.toList(shapeSource, id, Resource.class)
				.stream()
				.map(p -> Path.buildPath(shapeSource, p))
				.collect(Collectors.toList());

	}

	public SequencePath(Resource id, List<Path> paths) {
		super(id);
		this.paths = paths;
	}

	@Override
	public String toString() {
		return "SequencePath{ " + Arrays.toString(paths.toArray()) + " }";
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {

		List<Resource> values = paths.stream().map(Path::getId).collect(Collectors.toList());

		if (!model.contains(id, null, null)) {
			ShaclAstLists.listToRdf(values, id, model);
		}

		paths.forEach(p -> p.toModel(p.getId(), null, model, cycleDetection));

	}

	@Override
	public PlanNode getAllAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {

		var variables = List.of(new Variable<>("subject"),
				new Variable<>("value"));

		SparqlFragment targetQueryFragment = getTargetQueryFragment(variables.get(0), variables.get(1),
				connectionsGroup.getRdfsSubClassOfReasoner(), new StatementMatcher.StableRandomVariableProvider(),
				Set.of());

		PlanNode targetChainRetriever = new TargetChainRetriever(connectionsGroup, dataGraph,
				targetQueryFragment.getStatementMatchers(), List.of(), null, targetQueryFragment,
				variables,
				ConstraintComponent.Scope.propertyShape, true);

		targetChainRetriever = connectionsGroup.getCachedNodeFor(targetChainRetriever);

		if (planNodeWrapper != null) {
			targetChainRetriever = planNodeWrapper.apply(targetChainRetriever);
		}

		return connectionsGroup.getCachedNodeFor(targetChainRetriever);
	}

	@Override
	public PlanNode getAnyAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {
		List<StatementMatcher.Variable> variables = List.of(new StatementMatcher.Variable("subject"),
				new StatementMatcher.Variable("value"));

		SparqlFragment targetQueryFragment = getTargetQueryFragment(variables.get(0), variables.get(1),
				connectionsGroup.getRdfsSubClassOfReasoner(), new StatementMatcher.StableRandomVariableProvider(),
				Set.of());

		PlanNode unorderedSelect = new Select(connectionsGroup.getAddedStatements(), targetQueryFragment,
				null, new AllTargetsPlanNode.AllTargetsBindingSetMapper(List.of("subject", "value"),
						ConstraintComponent.Scope.propertyShape, true, dataGraph),
				dataGraph);

		if (planNodeWrapper != null) {
			unorderedSelect = planNodeWrapper.apply(unorderedSelect);
		}

		return connectionsGroup.getCachedNodeFor(unorderedSelect);
	}

	@Override
	public SparqlFragment getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, Set<String> inheritedVarNames) {

		if (inheritedVarNames.isEmpty()) {
			inheritedVarNames = Set.of(subject.getName());
		} else {
			inheritedVarNames = Sets.union(inheritedVarNames, Set.of(subject.getName()));
		}

		String variablePrefix = getVariablePrefix(subject, object);

		List<SparqlFragment> sparqlFragments = new ArrayList<>(paths.size());

		StatementMatcher.Variable head = subject;
		StatementMatcher.Variable tail = null;

		for (int i = 0; i < paths.size(); i++) {
			if (tail != null) {
				head = tail;
			}
			if (i + 1 == paths.size()) {
				// last element
				tail = object;
			} else {
				tail = new StatementMatcher.Variable(subject, variablePrefix + i);
			}

			Path path = paths.get(i);
			SparqlFragment targetQueryFragment = path.getTargetQueryFragment(head, tail, rdfsSubClassOfReasoner,
					stableRandomVariableProvider, inheritedVarNames);
			sparqlFragments.add(targetQueryFragment);
		}

		return SparqlFragment.join(sparqlFragments,
				(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Path path,
						StatementMatcher currentStatementMatcher,
						List<EffectiveTarget.SubjectObjectAndMatcher.SubjectObject> currentStatements) -> {
					Stream<EffectiveTarget.SubjectObjectAndMatcher> currentRoot = null;

					for (int i = sparqlFragments.size() - 1; i >= 0; i--) {
						SparqlFragment sparqlFragment = sparqlFragments.get(i);
						if (currentRoot != null) {
							currentRoot = currentRoot
									.flatMap(root -> sparqlFragment.getRoot(connectionsGroup, dataGraph, path,
											root.getStatementMatcher(),
											root.getStatements()))
									.filter(EffectiveTarget.SubjectObjectAndMatcher::hasStatements);
						} else {
							currentRoot = sparqlFragment.getRoot(connectionsGroup, dataGraph, path,
									currentStatementMatcher,
									currentStatements);
						}
					}

					return currentRoot;
				});
	}

	@Override
	public boolean isSupported() {
		for (Path path : paths) {
			if (!path.isSupported()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toSparqlPathString() {
		return "(" + paths.stream().map(Path::toSparqlPathString).collect(Collectors.joining(" / ")) + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SequencePath that = (SequencePath) o;

		return paths.equals(that.paths);
	}

	@Override
	public int hashCode() {
		return paths.hashCode();
	}
}
