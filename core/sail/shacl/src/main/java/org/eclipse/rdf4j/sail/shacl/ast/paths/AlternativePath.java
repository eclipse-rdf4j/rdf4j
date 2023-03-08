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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

import com.google.common.collect.Sets;

public class AlternativePath extends Path {

	private final Resource alternativePathId;
	private final List<Path> alternativePath;

	public AlternativePath(Resource id, Resource alternativePath, ShapeSource shapeSource) {
		super(id);

		this.alternativePathId = alternativePath;
		this.alternativePath = ShaclAstLists.toList(shapeSource, alternativePath, Resource.class)
				.stream()
				.map(p -> Path.buildPath(shapeSource, p))
				.collect(Collectors.toList());

	}

	@Override
	public String toString() {
		return "AlternativePath{ " + alternativePath + " }";
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.ALTERNATIVE_PATH, alternativePathId);

		List<Resource> values = alternativePath.stream().map(Path::getId).collect(Collectors.toList());

		if (!model.contains(alternativePathId, null, null)) {
			ShaclAstLists.listToRdf(values, alternativePathId, model);
		}

		alternativePath.forEach(p -> p.toModel(p.getId(), null, model, cycleDetection));
	}

	@Override
	public PlanNode getAllAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {
		return alternativePath
				.stream()
				.map(p -> p.getAllAdded(connectionsGroup, dataGraph, planNodeWrapper))
				.reduce(UnionNode::getInstance)
				.orElse(EmptyNode.getInstance());
	}

	@Override
	public PlanNode getAnyAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {
		return getAllAdded(connectionsGroup, dataGraph, planNodeWrapper);
	}

	@Override
	public boolean isSupported() {
		for (Path path : alternativePath) {
			if (!path.isSupported()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toSparqlPathString() {
		return "( " + alternativePath.stream().map(Path::toSparqlPathString).collect(Collectors.joining(" | ")) + " )";
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

		List<SparqlFragment> sparqlFragments = new ArrayList<>(alternativePath.size());

		for (Path path : alternativePath) {
			SparqlFragment targetQueryFragment = path
					.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner, stableRandomVariableProvider,
							inheritedVarNames);
			sparqlFragments.add(targetQueryFragment);
		}

		return SparqlFragment.union(sparqlFragments,
				(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Path path,
						StatementMatcher currentStatementMatcher, List<Statement> currentStatements) -> {

					return sparqlFragments.stream()
							.flatMap(sparqlFragment -> sparqlFragment.getRoot(connectionsGroup, dataGraph, path,
									currentStatementMatcher, currentStatements))
							.filter(EffectiveTarget.StatementsAndMatcher::hasStatements);

				}
		);
	}

}
