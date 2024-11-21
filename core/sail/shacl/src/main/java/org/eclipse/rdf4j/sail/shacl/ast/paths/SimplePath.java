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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class SimplePath extends Path {

	private final IRI predicate;

	public SimplePath(IRI predicate) {
		super(predicate);
		this.predicate = predicate;
	}

	@Override
	public Resource getId() {
		return predicate;
	}

	@Override
	public PlanNode getAllAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {
		PlanNode unorderedSelect = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, predicate, null,
				dataGraph, UnorderedSelect.Mapper.SubjectObjectPropertyShapeMapper.getFunction(), null);

		if (planNodeWrapper != null) {
			unorderedSelect = planNodeWrapper.apply(unorderedSelect);
		}

		return connectionsGroup.getCachedNodeFor(unorderedSelect);
	}

	@Override
	public PlanNode getAnyAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {
		return getAllAdded(connectionsGroup, dataGraph, planNodeWrapper);
	}

	@Override
	public String toString() {
		return "SimplePath{ <" + predicate + "> }";
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
	}

	@Override
	public SparqlFragment getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, Set<String> inheritedVarNames) {

		StatementMatcher statementMatcher = new StatementMatcher(subject, new StatementMatcher.Variable(predicate),
				object, this, inheritedVarNames);

		return SparqlFragment.bgp(List.of(),
				subject.asSparqlVariable() + " <" + predicate + "> " + object.asSparqlVariable() + " .",
				statementMatcher, (connectionsGroup, dataGraph, path, currentStatementMatcher, currentStatements) -> {
					if (currentStatementMatcher.getOrigin() == this) {
						if (currentStatementMatcher != statementMatcher) {
							return null;
						}
						return Stream.of(
								new EffectiveTarget.SubjectObjectAndMatcher(currentStatements,
										currentStatementMatcher));
					} else {
						if (currentStatementMatcher.hasSubject(object)) {
							var newStatements = currentStatements.stream()
									.map(currentStatement -> {
										try (CloseableIteration<? extends Statement> statements = connectionsGroup
												.getBaseConnection()
												.getStatements(null, predicate, currentStatement.getSubject(), true,
														dataGraph)) {
											return QueryResults.asList(statements);
										}
									})
									.flatMap(List::stream)
									.map(EffectiveTarget.SubjectObjectAndMatcher.SubjectObject::new)
									.collect(Collectors.toList());
							return Stream
									.of(new EffectiveTarget.SubjectObjectAndMatcher(newStatements, statementMatcher));
						} else if (currentStatementMatcher.hasObject(object)) {
							var newStatements = currentStatements.stream()
									.map(currentStatement -> {
										try (CloseableIteration<? extends Statement> statements = connectionsGroup
												.getBaseConnection()
												.getStatements(null, predicate, currentStatement.getObject(), true,
														dataGraph)) {
											return QueryResults.asList(statements);
										}
									})
									.flatMap(List::stream)
									.map(EffectiveTarget.SubjectObjectAndMatcher.SubjectObject::new)
									.collect(Collectors.toList());
							return Stream
									.of(new EffectiveTarget.SubjectObjectAndMatcher(newStatements, statementMatcher));
						}

						return null;
					}

				});
	}

	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public String toSparqlPathString() {
		return "<" + predicate + ">";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SimplePath that = (SimplePath) o;

		return predicate.equals(that.predicate);
	}

	@Override
	public int hashCode() {
		return predicate.hashCode();
	}
}
