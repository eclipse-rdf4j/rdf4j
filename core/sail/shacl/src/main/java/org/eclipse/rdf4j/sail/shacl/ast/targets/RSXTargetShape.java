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

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ast.Cache;
import org.eclipse.rdf4j.sail.shacl.ast.NodeShape;
import org.eclipse.rdf4j.sail.shacl.ast.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class RSXTargetShape extends Target {

	private final Shape targetShape;
	private final StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider(
			"rsx_");

	public RSXTargetShape(Resource targetShape, ShapeSource shapeSource, ShaclSail shaclSail) {
		ShaclProperties p = new ShaclProperties(targetShape, shapeSource);

		if (p.getType() == SHACL.NODE_SHAPE) {
			this.targetShape = NodeShape.getInstance(p, shapeSource, new Cache(), shaclSail);
		} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
			this.targetShape = PropertyShape.getInstance(p, shapeSource, new Cache(), shaclSail);
		} else {
			throw new IllegalStateException("Unknown shape type for " + p.getId());
		}

		this.targetShape.setTargetChain(new TargetChain());

	}

	@Override
	public IRI getPredicate() {
		return RSX.targetShape;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		targetShape.toModel(subject, getPredicate(), model, cycleDetection);
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph, ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup, dataGraph, scope);
	}

	private PlanNode getAddedRemovedInner(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		StatementMatcher.Variable object = stableRandomVariableProvider.next();

		SparqlFragment sparqlFragment = this.targetShape.buildSparqlValidNodes_rsx_targetShape(null, object,
				connectionsGroup.getRdfsSubClassOfReasoner(), null, stableRandomVariableProvider);

		List<StatementMatcher> statementMatchers = sparqlFragment.getStatementMatchers();

		String query = sparqlFragment.getFragment();

		List<StatementMatcher.Variable> vars = Collections.singletonList(object);

		return Unique.getInstance(new TargetChainRetriever(
				connectionsGroup,
				dataGraph, statementMatchers,
				statementMatchers,
				query,
				vars,
				scope
		), false);

	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		throw new UnsupportedOperationException(this.getClass().getSimpleName());

	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, Resource[] dataGraph, PlanNode parent) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();
		StatementMatcher.Variable variable = stableRandomVariableProvider.next();

		String query = getTargetQueryFragment(null, variable, connectionsGroup.getRdfsSubClassOfReasoner(),
				stableRandomVariableProvider);

		// TODO: this is a slow way to solve this problem! We should use bulk operations.
		return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), dataGraph, parent, query, variable,
				ValidationTuple::getActiveTarget).getTrueNode(UnBufferedPlanNode.class);
	}

	@Override
	public Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject,
			StatementMatcher.Variable object, RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		return this.targetShape
				.buildSparqlValidNodes_rsx_targetShape(subject, object, rdfsSubClassOfReasoner, null,
						stableRandomVariableProvider)
				.getStatementMatchers()
				.stream();
	}

	@Override
	public String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		assert (subject == null);

		return this.targetShape
				.buildSparqlValidNodes_rsx_targetShape(subject, object, rdfsSubClassOfReasoner, null,
						stableRandomVariableProvider)
				.getFragment();

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RSXTargetShape that = (RSXTargetShape) o;
		return targetShape.equals(that.targetShape);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetShape);
	}
}
