/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclPrefixParser;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.AllTargetsPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class SparqlTarget extends Target {

	private final Resource id;
	private Literal originalSelect;
	private String select;
	private final Set<Namespace> namespaces;
	private final Model prefixes;

	public SparqlTarget(Resource id, ShapeSource shapeSource) {
		this.id = id;

		try (Stream<Value> objects = shapeSource.getObjects(id, ShapeSource.Predicates.SELECT)) {
			objects.forEach(literal -> {
				if (select != null) {
					throw new IllegalStateException("Multiple sh:select queries found for constraint component " + id);
				}
				if (!(literal.isLiteral())) {
					throw new IllegalStateException("sh:select must be a literal for constraint component " + id);
				}
				select = literal.stringValue();
				originalSelect = ((Literal) literal);
			});
		}

		if (select == null) {
			throw new IllegalStateException("No sh:select query found for constraint component " + id);
		}

		var shaclNamespaces = ShaclPrefixParser.extractNamespaces(id, shapeSource);
		prefixes = shaclNamespaces.getModel();
		namespaces = shaclNamespaces.getNamespaces();
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.TARGET_PROP, id);
		model.add(id, SHACL.SELECT, originalSelect);
		model.add(id, RDF.TYPE, SHACL.SPARQL_TARGET);
		model.addAll(prefixes);
	}

	@Override
	public SparqlFragment getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, Set<String> inheritedVarNames) {

		String query = "{ select (?this AS " + object.asSparqlVariable() + ") where {\n" +
				"{\n" + select + "\n}\n" +
				"} }";

		return SparqlFragment.bgp(namespaces, query, false);
	}

	@Override
	public IRI getPredicate() {
		return SHACL.SPARQL_TARGET;
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph, ConstraintComponent.Scope scope) {

		SparqlFragment sparqlFragment = SparqlFragment.bgp(namespaces, select, false);

		List<String> varNames = List.of("this");

		return new Select(connectionsGroup.getBaseConnection(), sparqlFragment, null,
				new AllTargetsPlanNode.AllTargetsBindingSetMapper(varNames, scope, false, dataGraph), dataGraph);
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, Resource[] dataGraph, PlanNode parent) {

		SparqlFragment sparqlFragment = SparqlFragment.bgp(namespaces, select, false);

		// TODO: this is a slow way to solve this problem! We should use bulk operations.
		return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), dataGraph, parent, sparqlFragment,
				StatementMatcher.Variable.THIS,
				ValidationTuple::getActiveTarget, null, connectionsGroup).getTrueNode(UnBufferedPlanNode.class);
	}

	@Override
	public Set<Namespace> getNamespaces() {
		return namespaces;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SparqlTarget that = (SparqlTarget) o;

		return select.equals(that.select);
	}

	@Override
	public int hashCode() {
		return select.hashCode() + "SparqlTarget".hashCode();
	}
}
