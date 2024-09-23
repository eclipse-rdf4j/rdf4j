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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChainRetriever;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

import com.google.common.collect.Sets;

public class ZeroOrMorePath extends Path {

	private final Path path;

	public ZeroOrMorePath(Resource id, Resource path, ShapeSource shapeSource) {
		super(id);
		this.path = Path.buildPath(shapeSource, path);
	}

	public ZeroOrMorePath(Resource id, Path path) {
		super(id);
		this.path = path;
	}

	@Override
	public String toString() {
		return "ZeroOrMorePath{ " + path + " }";
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.ZERO_OR_MORE_PATH, path.getId());
		path.toModel(path.getId(), null, model, cycleDetection);
	}

	@Override
	public PlanNode getAllAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {
		var variables = List.of(new StatementMatcher.Variable<>("subject"),
				new StatementMatcher.Variable<>("value"));

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
		return getAllAdded(connectionsGroup, dataGraph, planNodeWrapper);
	}

	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public String toSparqlPathString() {
		assert path.toSparqlPathString().equals(path.toSparqlPathString().trim());
		if (path instanceof SimplePath || path instanceof AlternativePath || path instanceof SequencePath) {
			return path.toSparqlPathString() + "*";
		}
		return "(" + path.toSparqlPathString() + ")*";
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

		String sparqlPathString = path.toSparqlPathString();

		StatementMatcher.Variable pathStart = new StatementMatcher.Variable(subject, variablePrefix + "start");
		StatementMatcher.Variable pathEnd = new StatementMatcher.Variable(subject, variablePrefix + "end");

		SparqlFragment targetQueryFragmentMiddle = path.getTargetQueryFragment(pathStart, pathEnd,
				rdfsSubClassOfReasoner, stableRandomVariableProvider,
				inheritedVarNames);

		SparqlFragment targetQueryFragmentStart = path.getTargetQueryFragment(subject, pathStart,
				rdfsSubClassOfReasoner, stableRandomVariableProvider,
				inheritedVarNames);

		SparqlFragment targetQueryFragmentEnd = path.getTargetQueryFragment(pathEnd, object, rdfsSubClassOfReasoner,
				stableRandomVariableProvider,
				inheritedVarNames);

		String oneOrMore = subject.asSparqlVariable() + " (" + sparqlPathString + ")* " + pathStart.asSparqlVariable()
				+ " .\n" +
				targetQueryFragmentMiddle.getFragment() + "\n" +
				pathEnd.asSparqlVariable() + " (" + sparqlPathString + ")* " + object.asSparqlVariable() + " .\n";

		String zeroOrOne = subject.asSparqlVariable() + " (" + sparqlPathString + ")? " + object.asSparqlVariable()
				+ " .\n";

		ArrayList<StatementMatcher> statementMatchers = Stream
				.of(targetQueryFragmentStart.getStatementMatchers(), targetQueryFragmentMiddle.getStatementMatchers(),
						targetQueryFragmentEnd.getStatementMatchers())
				.flatMap(List::stream)
				.collect(Collectors.toCollection(ArrayList::new));

		SparqlFragment bgp1 = SparqlFragment.bgp(List.of(), oneOrMore, statementMatchers);

		SparqlFragment bgp2 = SparqlFragment.bgp(List.of(), zeroOrOne, statementMatchers);

		return SparqlFragment.union(List.of(bgp1, bgp2));

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ZeroOrMorePath that = (ZeroOrMorePath) o;

		return path.equals(that.path);
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}
}
