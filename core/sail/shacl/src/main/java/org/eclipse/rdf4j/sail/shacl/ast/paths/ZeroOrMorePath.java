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
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlQueryParserCache;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeHelper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.SingletonBindingSet;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
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
		return false;
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

		SparqlFragment targetQueryFragmentZeroOrOne = SparqlFragment.bgp(List.of(),
				subject.asSparqlVariable() + " (" + sparqlPathString + ")? " + object.asSparqlVariable(),
				List.of(new StatementMatcher(subject, null, object, this, inheritedVarNames)),
				(connectionsGroup, dataGraph, path, currentStatementMatcher, currentStatements) -> {
					// TODO: We don't have any tests that cover this! Maybe take a look at minCount/oneOrMorePath?
					if (currentStatementMatcher.hasSubject(subject)) {
						System.out.println();
						throw new NotImplementedException("This should not happen!");
					}

					return Stream.empty();
				}
		);

		String oneOrMore = subject.asSparqlVariable() + " (" + sparqlPathString + ")* " + pathStart.asSparqlVariable()
				+ " .\n" +
				targetQueryFragmentMiddle.getFragment() + "\n" +
				pathEnd.asSparqlVariable() + " (" + sparqlPathString + ")* " + object.asSparqlVariable() + " .\n";

		SparqlFragment oneOrMoreBgp = SparqlFragment.bgp(List.of(), oneOrMore,
				targetQueryFragmentMiddle.getStatementMatchers());

		var temp = inheritedVarNames;

		return SparqlFragment.union(List.of(targetQueryFragmentZeroOrOne, oneOrMoreBgp),
				(connectionsGroup, dataGraph, path, currentStatementMatcher, currentStatements) -> {

					Stream<EffectiveTarget.SubjectObjectAndMatcher> statementsAndMatcherStream1 = targetQueryFragmentZeroOrOne
							.getRoot(connectionsGroup, dataGraph, path, currentStatementMatcher, currentStatements)
							.filter(EffectiveTarget.SubjectObjectAndMatcher::hasStatements);

					Stream<EffectiveTarget.SubjectObjectAndMatcher> peek = targetQueryFragmentMiddle
							.getRoot(connectionsGroup, dataGraph, path, currentStatementMatcher, currentStatements)
							.filter(EffectiveTarget.SubjectObjectAndMatcher::hasStatements)
							.map(a -> {
								SailConnection baseConnection = connectionsGroup.getBaseConnection();

								String subjectName = a.getStatementMatcher().getSubjectName();
								assert subjectName.equals(pathStart.getName());

								String query = "select distinct " + subject.asSparqlVariable() + " where {\n"
										+ subject.asSparqlVariable() + " (" + sparqlPathString + ")* "
										+ pathStart.asSparqlVariable() + "\n}";

								TupleExpr tupleExpr = SparqlQueryParserCache.get(query);

								List<EffectiveTarget.SubjectObjectAndMatcher.SubjectObject> statements = new ArrayList<>();

								for (EffectiveTarget.SubjectObjectAndMatcher.SubjectObject statement : a
										.getStatements()) {
									try (CloseableIteration<? extends BindingSet> evaluate = baseConnection.evaluate(
											tupleExpr, PlanNodeHelper.asDefaultGraphDataset(dataGraph),
											new SingletonBindingSet(subjectName, statement.getSubject()), true)) {
										while (evaluate.hasNext()) {
											BindingSet next = evaluate.next();
											statements.add(new EffectiveTarget.SubjectObjectAndMatcher.SubjectObject(
													((Resource) next.getValue(subject.getName())), null));
										}
									}
								}

								StatementMatcher statementMatcher = new StatementMatcher(subject, null, null, path,
										temp);

								EffectiveTarget.SubjectObjectAndMatcher effectiveTarget = new EffectiveTarget.SubjectObjectAndMatcher(
										statements, statementMatcher);

								return effectiveTarget;
							});

					return Stream.concat(statementsAndMatcherStream1, peek);

				});

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
