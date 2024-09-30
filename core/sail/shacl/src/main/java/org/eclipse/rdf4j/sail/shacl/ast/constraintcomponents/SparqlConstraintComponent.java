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

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.CanProduceValidationReport;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclPrefixParser;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.AllTargetsPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.SparqlConstraintSelect;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class SparqlConstraintComponent extends AbstractConstraintComponent implements CanProduceValidationReport {

	private final Shape shape;
	public boolean produceValidationReports;
	private String select;
	private String originalSelect;
	private List<Literal> message = new ArrayList<>();
	private Boolean deactivated;
	private final Set<Namespace> namespaces;
	private final Model prefixes;

	public SparqlConstraintComponent(Resource id, ShapeSource shapeSource, Shape shape) {
		super(id);
		this.shape = shape;
		try (Stream<Value> objects = shapeSource.getObjects(id, ShapeSource.Predicates.SELECT)) {
			objects.forEach(literal -> {
				if (select != null) {
					throw new IllegalStateException("Multiple sh:select queries found for constraint component " + id);
				}
				if (!(literal.isLiteral())) {
					throw new IllegalStateException("sh:select must be a literal for constraint component " + id);
				}
				select = literal.stringValue();
				originalSelect = select;
			});
		}

		if (select == null) {
			throw new IllegalStateException("No sh:select query found for constraint component " + id);
		}

		try (Stream<Value> objects = shapeSource.getObjects(id, ShapeSource.Predicates.MESSAGE)) {
			objects.forEach(literal -> {
				if (!(literal.isLiteral())) {
					throw new IllegalStateException("sh:message must be a literal for constraint component " + id);
				}
				message.add((Literal) literal);
			});
		}

		try (Stream<Value> objects = shapeSource.getObjects(id, ShapeSource.Predicates.DEACTIVATED)) {
			objects.forEach(literal -> {
				if (deactivated != null) {
					throw new IllegalStateException("Multiple sh:deactivated found for constraint component " + id);
				}
				if (!(literal.isLiteral())) {
					throw new IllegalStateException("sh:deactivated must be a literal for constraint component " + id);
				}
				deactivated = ((Literal) literal).booleanValue();
			});
		}

		var shaclNamespaces = ShaclPrefixParser.extractNamespaces(id, shapeSource);
		prefixes = shaclNamespaces.getModel();
		namespaces = shaclNamespaces.getNamespaces();

		select = ShaclPrefixParser.toSparqlPrefixes(namespaces) + "\n" + select;
	}

	public SparqlConstraintComponent(Resource id, Shape shape, boolean produceValidationReports, String select,
			String originalSelect, List<Literal> message, Boolean deactivated, Set<Namespace> namespaces,
			Model prefixes) {
		super(id);
		this.shape = shape;
		this.produceValidationReports = produceValidationReports;
		this.select = select;
		this.originalSelect = originalSelect;
		this.message = message;
		this.deactivated = deactivated;
		this.prefixes = prefixes;
		this.namespaces = namespaces;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.SPARQL, getId());
		model.add(getId(), SHACL.SELECT, Values.literal(originalSelect));
		model.add(getId(), RDF.TYPE, SHACL.SPARQL_CONSTRAINT);

		for (Literal literal : message) {
			model.add(getId(), SHACL.MESSAGE, literal);
		}

		if (deactivated != null) {
			model.add(getId(), SHACL.DEACTIVATED, Values.literal(deactivated));
		}

		model.addAll(prefixes);

	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.SPARQLConstraintComponent;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);

		String select = this.select;
		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();
			String s = path.toSparqlPathString();
			select = select.replace(" $PATH ", " " + s + " ");
		}

		PlanNode allTargets;
		if (overrideTargetNode != null) {
			allTargets = getPlanNodeForOverrideTargetNode(
					connectionsGroup,
					validationSettings,
					overrideTargetNode,
					scope,
					effectiveTarget
			);

		} else {
			allTargets = effectiveTarget.getAllTargets(connectionsGroup, validationSettings.getDataGraph(), scope);
		}

		if (effectiveTarget.size() > 1) {
			allTargets = Unique.getInstance(allTargets, true, connectionsGroup);
		}

		return new SparqlConstraintSelect(connectionsGroup.getBaseConnection(), allTargets, select, scope,
				validationSettings.getDataGraph(), produceValidationReports, this, shape);

	}

	private PlanNode getPlanNodeForOverrideTargetNode(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, PlanNodeProvider overrideTargetNode, Scope scope,
			EffectiveTarget effectiveTarget) {
		PlanNode planNode;
		assert scope != null;

		PlanNode overrideTargetPlanNode = overrideTargetNode.getPlanNode();

		if (scope == Scope.nodeShape) {

			if (overrideTargetPlanNode instanceof AllTargetsPlanNode) {
				return effectiveTarget.getAllTargets(connectionsGroup, validationSettings.getDataGraph(), scope);
			} else {
				return effectiveTarget.extend(
						overrideTargetPlanNode,
						connectionsGroup,
						validationSettings.getDataGraph(),
						scope,
						EffectiveTarget.Extend.right,
						false,
						null
				);

			}

		} else {

			if (overrideTargetPlanNode instanceof AllTargetsPlanNode) {
				return effectiveTarget.getAllTargets(connectionsGroup, validationSettings.getDataGraph(), scope);
			} else {

				overrideTargetPlanNode = effectiveTarget.extend(overrideTargetPlanNode, connectionsGroup,
						validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.right, false, null);

				planNode = connectionsGroup.getCachedNodeFor(overrideTargetPlanNode);
			}
		}

		return planNode;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider,
			ValidationSettings validationSettings) {
		return getTargetChain()
				.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
				.getAllTargets(connectionsGroup, dataGraph, scope);
	}

	@Override
	public ConstraintComponent deepClone() {
		return new SparqlConstraintComponent(getId(), shape, produceValidationReports, select, originalSelect, message,
				deactivated, namespaces, prefixes);
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {
		return null;
	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.Transactional;
	}

	@Override
	public List<Literal> getDefaultMessage() {
		return message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SparqlConstraintComponent that = (SparqlConstraintComponent) o;

		if (produceValidationReports != that.produceValidationReports) {
			return false;
		}
		if (!select.equals(that.select)) {
			return false;
		}
		if (!originalSelect.equals(that.originalSelect)) {
			return false;
		}
		if (!Objects.equals(message, that.message)) {
			return false;
		}
		return Objects.equals(deactivated, that.deactivated);
	}

	@Override
	public int hashCode() {
		int result = (produceValidationReports ? 1 : 0);
		result = 31 * result + select.hashCode();
		result = 31 * result + originalSelect.hashCode();
		result = 31 * result + (message != null ? message.hashCode() : 0);
		result = 31 * result + (deactivated != null ? deactivated.hashCode() : 0);
		return result + "SparqlConstraintComponent".hashCode();
	}

	@Override
	public void setProducesValidationReport(boolean producesValidationReport) {
		this.produceValidationReports = producesValidationReport;
	}

	@Override
	public boolean producesValidationReport() {
		return produceValidationReports;
	}

}
