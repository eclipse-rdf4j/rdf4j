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
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.Cache;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.SparqlConstraintSelect;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class SparqlConstraintComponent extends AbstractConstraintComponent {

	private Shape shape;
	public boolean produceValidationReports;
	private String select;
	private String originalSelect;
	private String message;
	private Boolean deactivated;
	private final List<Namespace> namespaces = new ArrayList<>();

	private final Model prefixes = new DynamicModel(new LinkedHashModelFactory());

	public SparqlConstraintComponent(Resource id, ShapeSource shapeSource, Cache cache, ShaclSail shaclSail,
			Shape shape) {
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
				if (message != null) {
					throw new IllegalStateException("Multiple sh:message values found for constraint component " + id);
				}
				if (!(literal.isLiteral())) {
					throw new IllegalStateException("sh:message must be a literal for constraint component " + id);
				}
				message = literal.stringValue();
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

		try (Stream<Value> objects = shapeSource.getObjects(id, ShapeSource.Predicates.PREFIXES)) {
			objects.forEach(prefix -> {
				if (!(prefix instanceof Resource)) {
					throw new IllegalStateException("sh:prefixes must be an Resource for constraint component " + id);
				}
				prefixes.add(id, SHACL.PREFIXES, prefix);

				try (Stream<Value> declareObjects = shapeSource.getObjects(((Resource) prefix),
						ShapeSource.Predicates.DECLARE)) {
					declareObjects.forEach(declaration -> {
						if (!(declaration instanceof Resource)) {
							throw new IllegalStateException("sh:declare must be a Resource for " + prefix);
						}

						prefixes.add((Resource) prefix, SHACL.DECLARE, declaration);

						String namespacePrefix = null;
						String namespaceName = null;

						try (Stream<Value> prefixPropObjects = shapeSource.getObjects(((Resource) declaration),
								ShapeSource.Predicates.PREFIX_PROP)) {
							namespacePrefix = prefixPropObjects
									.map(literal -> {
										if (!(literal instanceof Literal)) {
											throw new IllegalStateException(
													"sh:prefix must be a Literal for " + declaration);
										}
										prefixes.add((Resource) declaration, SHACL.PREFIX_PROP, literal);
										return literal.stringValue();
									})
									.findFirst()
									.orElseThrow(() -> new IllegalStateException(
											"sh:prefix must have a value for " + declaration));
						}

						try (Stream<Value> namespacePropObjects = shapeSource.getObjects(((Resource) declaration),
								ShapeSource.Predicates.NAMESPACE_PROP)) {
							namespaceName = namespacePropObjects
									.map(literal -> {
										if (!(literal instanceof Literal)) {
											throw new IllegalStateException(
													"sh:namespace must be a Literal for " + declaration);
										}
										prefixes.add((Resource) declaration, SHACL.NAMESPACE_PROP, literal);
										return literal.stringValue();
									})
									.findFirst()
									.orElseThrow(() -> new IllegalStateException(
											"sh:namespace must have a value for " + declaration));
						}

						namespaces.add(new SimpleNamespace(namespacePrefix, namespaceName));

					});
				}

			});
		}

		StringBuilder sb = new StringBuilder();
		namespaces.forEach(namespace -> sb.append("PREFIX ")
				.append(namespace.getPrefix())
				.append(": <")
				.append(namespace.getName())
				.append(">\n"));

		select = sb + "\n" + select;
	}

	public SparqlConstraintComponent(Resource id, Shape shape, boolean produceValidationReports, String select,
			String originalSelect, String message, Boolean deactivated) {
		super(id);
		this.shape = shape;
		this.produceValidationReports = produceValidationReports;
		this.select = select;
		this.originalSelect = originalSelect;
		this.message = message;
		this.deactivated = deactivated;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.SPARQL, getId());
		model.add(getId(), SHACL.SELECT, Values.literal(originalSelect));
		model.add(getId(), RDF.TYPE, SHACL.SPARQL_CONSTRAINT);

		if (message != null) {
			model.add(getId(), SHACL.MESSAGE, Values.literal(message));
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

		PlanNode allTargets = effectiveTarget.getAllTargets(connectionsGroup, validationSettings.getDataGraph(), scope);

		return new SparqlConstraintSelect(connectionsGroup.getBaseConnection(),
				allTargets, select, scope, validationSettings.getDataGraph(), produceValidationReports, this, shape);

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		return getTargetChain()
				.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
				.getAllTargets(connectionsGroup, dataGraph, scope);
	}

	@Override
	public ConstraintComponent deepClone() {
		return new SparqlConstraintComponent(getId(), shape, produceValidationReports, select, originalSelect, message,
				deactivated);
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
		return List.of();
	}

}
