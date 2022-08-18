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
package org.eclipse.rdf4j.sail.shacl.ast;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.AndConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ClassConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ClosedConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.DashHasValueInConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.DatatypeConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.DisjointConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.EqualsConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.HasValueConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.InConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.LanguageInConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.LessThanConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.LessThanOrEqualsConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.MaxCountConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.MaxExclusiveConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.MaxInclusiveConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.MaxLengthConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.MinCountConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.MinExclusiveConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.MinInclusiveConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.MinLengthConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.NodeKindConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.NotConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.OrConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.PatternConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.QualifiedMaxCountConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.QualifiedMinCountConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.UniqueLangConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.XoneConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.targets.DashAllObjects;
import org.eclipse.rdf4j.sail.shacl.ast.targets.DashAllSubjects;
import org.eclipse.rdf4j.sail.shacl.ast.targets.RSXTargetShape;
import org.eclipse.rdf4j.sail.shacl.ast.targets.Target;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetClass;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetNode;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetObjectsOf;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetSubjectsOf;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class Shape implements ConstraintComponent, Identifiable {

	private static final Logger logger = LoggerFactory.getLogger(Shape.class);
	protected boolean produceValidationReports;

	Resource id;
	TargetChain targetChain;

	List<Target> target = new ArrayList<>();

	boolean deactivated;
	List<Literal> message;
	Severity severity = Severity.Violation;

	List<ConstraintComponent> constraintComponents = new ArrayList<>();

	Resource[] contexts;

	public Shape() {
	}

	public Shape(Shape shape) {
		this.deactivated = shape.deactivated;
		this.message = shape.message;
		this.severity = shape.severity;
		this.id = shape.id;
		this.targetChain = shape.targetChain;
		this.contexts = shape.contexts;
		this.produceValidationReports = shape.produceValidationReports;
	}

	public void populate(ShaclProperties properties, ShapeSource shapeSource, Cache cache,
			ShaclSail shaclSail) {
		this.deactivated = properties.isDeactivated();
		this.message = properties.getMessage();
		this.id = properties.getId();
		this.contexts = shapeSource.getActiveContexts();

		if (!properties.getTargetClass().isEmpty()) {
			target.add(new TargetClass(properties.getTargetClass()));
		}
		if (!properties.getTargetNode().isEmpty()) {
			target.add(new TargetNode(properties.getTargetNode(), shapeSource.getActiveContexts()));
		}
		if (!properties.getTargetObjectsOf().isEmpty()) {
			target.add(new TargetObjectsOf(properties.getTargetObjectsOf()));
		}
		if (!properties.getTargetSubjectsOf().isEmpty()) {
			target.add(new TargetSubjectsOf(properties.getTargetSubjectsOf()));
		}

		if (shaclSail.isEclipseRdf4jShaclExtensions() && !properties.getTargetShape().isEmpty()) {

			properties.getTargetShape()
					.stream()
					.map(targetShape -> new RSXTargetShape(targetShape, shapeSource, shaclSail))
					.forEach(target::add);

		}

		if (!properties.getTarget().isEmpty()) {
			properties.getTarget()
					.forEach(target -> {
//									if (shapeSource.hasStatement(sparqlTarget, RDF.TYPE, SHACL.SPARQL_TARGET, true)) {
//										propertyShapes.add(new SparqlTarget(shapeId, shaclSail, shapeSource,
//												shaclProperties.isDeactivated(), target));
//									}
						if (shaclSail.isDashDataShapes() && shapeSource.isType(target,
								DASH.AllObjectsTarget)) {
							this.target.add(new DashAllObjects(target));
						}
						if (shaclSail.isDashDataShapes() && shapeSource.isType(target,
								DASH.AllSubjectsTarget)) {
							this.target.add(new DashAllSubjects(target));
						}

					});

		}

	}

	@Override
	public Resource getId() {
		return id;
	}

	public Resource[] getContexts() {
		return contexts;
	}

	protected abstract Shape shallowClone();

	/**
	 * @param model the model to export the shapes into
	 * @return the provided model
	 */
	public Model toModel(Model model) {
		toModel(null, null, model, new HashSet<>());
		return model;
	}

	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		ModelBuilder modelBuilder = new ModelBuilder();

		modelBuilder.subject(getId());

		if (deactivated) {
			modelBuilder.add(SHACL.DEACTIVATED, deactivated);
		}

		target.forEach(t -> {
			t.toModel(getId(), null, model, cycleDetection);
		});

		model.addAll(modelBuilder.build());
	}

	List<ConstraintComponent> getConstraintComponents(ShaclProperties properties, ShapeSource shapeSource,
			Cache cache, ShaclSail shaclSail) {

		List<ConstraintComponent> constraintComponent = new ArrayList<>();

		properties.getProperty()
				.stream()
				.map(r -> new ShaclProperties(r, shapeSource))
				.map(p -> PropertyShape.getInstance(p, shapeSource, cache, shaclSail))
				.forEach(constraintComponent::add);

		properties.getNode()
				.stream()
				.map(r -> new ShaclProperties(r, shapeSource))
				.map(p -> NodeShape.getInstance(p, shapeSource, cache, shaclSail))
				.forEach(constraintComponent::add);

		if (properties.getMinCount() != null) {
			constraintComponent.add(new MinCountConstraintComponent(properties.getMinCount()));
		}

		if (properties.getMaxCount() != null) {
			constraintComponent.add(new MaxCountConstraintComponent(properties.getMaxCount()));
		}

		if (properties.getDatatype() != null) {
			constraintComponent.add(new DatatypeConstraintComponent(properties.getDatatype()));
		}

		if (properties.getMinLength() != null) {
			constraintComponent.add(new MinLengthConstraintComponent(properties.getMinLength()));
		}

		if (properties.getMaxLength() != null) {
			constraintComponent.add(new MaxLengthConstraintComponent(properties.getMaxLength()));
		}

		if (properties.getMinInclusive() != null) {
			constraintComponent.add(new MinInclusiveConstraintComponent(properties.getMinInclusive()));
		}

		if (properties.getMaxInclusive() != null) {
			constraintComponent.add(new MaxInclusiveConstraintComponent(properties.getMaxInclusive()));
		}

		if (properties.getMinExclusive() != null) {
			constraintComponent.add(new MinExclusiveConstraintComponent(properties.getMinExclusive()));
		}

		if (properties.getMaxExclusive() != null) {
			constraintComponent.add(new MaxExclusiveConstraintComponent(properties.getMaxExclusive()));
		}

		if (properties.isUniqueLang()) {
			constraintComponent.add(new UniqueLangConstraintComponent());
		}

		properties.getPattern()
				.stream()
				.map(pattern -> new PatternConstraintComponent(pattern, properties.getFlags()))
				.forEach(constraintComponent::add);

		if (properties.getLanguageIn() != null) {
			constraintComponent.add(new LanguageInConstraintComponent(shapeSource, properties.getLanguageIn()));
		}

		if (properties.getIn() != null) {
			constraintComponent.add(new InConstraintComponent(shapeSource, properties.getIn()));
		}

		if (properties.getNodeKind() != null) {
			constraintComponent.add(new NodeKindConstraintComponent(properties.getNodeKind()));
		}

		if (properties.isClosed()) {
			constraintComponent.add(new ClosedConstraintComponent(shapeSource, properties.getProperty(),
					properties.getIgnoredProperties()));
		}

		properties.getClazz()
				.stream()
				.map(ClassConstraintComponent::new)
				.forEach(constraintComponent::add);

		properties.getHasValue()
				.stream()
				.map(HasValueConstraintComponent::new)
				.forEach(constraintComponent::add);

		properties.getEquals()
				.stream()
				.map(EqualsConstraintComponent::new)
				.forEach(constraintComponent::add);

		properties.getDisjoint()
				.stream()
				.map(DisjointConstraintComponent::new)
				.forEach(constraintComponent::add);

		properties.getLessThan()
				.stream()
				.map(LessThanConstraintComponent::new)
				.forEach(constraintComponent::add);

		properties.getLessThanOrEquals()
				.stream()
				.map(LessThanOrEqualsConstraintComponent::new)
				.forEach(constraintComponent::add);

		if (properties.getQualifiedValueShape() != null) {

			if (properties.getQualifiedMaxCount() != null) {
				QualifiedMaxCountConstraintComponent qualifiedMaxCountConstraintComponent = new QualifiedMaxCountConstraintComponent(
						properties.getQualifiedValueShape(), shapeSource, cache, shaclSail,
						properties.getQualifiedValueShapesDisjoint(), properties.getQualifiedMaxCount());
				constraintComponent.add(qualifiedMaxCountConstraintComponent);
			}

			if (properties.getQualifiedMinCount() != null) {
				QualifiedMinCountConstraintComponent qualifiedMinCountConstraintComponent = new QualifiedMinCountConstraintComponent(
						properties.getQualifiedValueShape(), shapeSource, cache, shaclSail,
						properties.getQualifiedValueShapesDisjoint(), properties.getQualifiedMinCount());
				constraintComponent.add(qualifiedMinCountConstraintComponent);
			}
		}

		if (shaclSail.isDashDataShapes()) {
			properties.getHasValueIn()
					.stream()
					.map(hasValueIn -> new DashHasValueInConstraintComponent(shapeSource, hasValueIn))
					.forEach(constraintComponent::add);
		}

		properties.getOr()
				.stream()
				.map(or -> new OrConstraintComponent(or, shapeSource, cache, shaclSail))
				.forEach(constraintComponent::add);

		properties.getXone()
				.stream()
				.map(xone -> new XoneConstraintComponent(xone, shapeSource, cache, shaclSail))
				.forEach(constraintComponent::add);

		properties.getAnd()
				.stream()
				.map(and -> new AndConstraintComponent(and, shapeSource, cache, shaclSail))
				.forEach(constraintComponent::add);

		properties.getNot()
				.stream()
				.map(or -> new NotConstraintComponent(or, shapeSource, cache, shaclSail))
				.forEach(constraintComponent::add);

		return constraintComponent;
	}

	@Override
	public TargetChain getTargetChain() {
		return targetChain;
	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		this.targetChain = targetChain;
		constraintComponents.forEach(c -> c.setTargetChain(targetChain));
	}

	public PlanNode generatePlans(ConnectionsGroup connectionsGroup, ValidationSettings validationSettings) {
		assert constraintComponents.size() == 1;

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		ValidationApproach validationApproach = ValidationApproach.SPARQL;
		if (!validationSettings.isValidateEntireBaseSail()) {
			validationApproach = constraintComponents.stream()
					.map(constraintComponent -> constraintComponent.getPreferredValidationApproach(connectionsGroup))
					.reduce(ValidationApproach::reducePreferred)
					.get();
		}

		if (validationApproach == ValidationApproach.SPARQL) {
			if (connectionsGroup.isSparqlValidation()
					&& Shape.this.getOptimalBulkValidationApproach() == ValidationApproach.SPARQL) {
				logger.debug("Use validation approach {} for shape {}", validationApproach, this);
				return Shape.this.generateSparqlValidationQuery(connectionsGroup, validationSettings, false, false,
						Scope.none)
						.getValidationPlan(connectionsGroup.getBaseConnection(), validationSettings.getDataGraph(),
								getContexts());
			} else {
				logger.debug("Use fall back validation approach for bulk validation instead of SPARQL for shape {}",
						this);

				return Shape.this.generateTransactionalValidationPlan(connectionsGroup, validationSettings,
						() -> Shape.this.getTargetChain()
								.getEffectiveTarget(
										this instanceof NodeShape ? Scope.nodeShape : Scope.propertyShape,
										connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
								.getAllTargets(connectionsGroup,
										validationSettings.getDataGraph(),
										this instanceof NodeShape ? Scope.nodeShape : Scope.propertyShape),
						Scope.none);
			}

		} else if (validationApproach == ValidationApproach.Transactional) {
			logger.debug("Use validation approach {} for shape {}", validationApproach, this);

			if (this.requiresEvaluation(connectionsGroup, Scope.none, validationSettings.getDataGraph(),
					stableRandomVariableProvider)) {
				return Shape.this.generateTransactionalValidationPlan(connectionsGroup, validationSettings, null,
						Scope.none);
			} else {
				return EmptyNode.getInstance();
			}

		} else {
			throw new ShaclUnsupportedException("Unkown validation approach: " + validationApproach);
		}

	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		throw new ShaclUnsupportedException(this.getClass().getSimpleName());
	}

	public Severity getSeverity() {
		return severity;
	}

	public boolean isDeactivated() {
		return deactivated;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		return constraintComponents.stream()
				.anyMatch(c -> c.requiresEvaluation(connectionsGroup, scope, dataGraph, stableRandomVariableProvider));
	}

	/**
	 * For rsx:targetShape
	 */
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		throw new UnsupportedOperationException(this.getClass().getSimpleName());
	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return constraintComponents.stream()
				.map(ConstraintComponent::getOptimalBulkValidationApproach)
				.reduce(ValidationApproach::reduceCompatible)
				.orElse(ValidationApproach.MOST_COMPATIBLE);

	}

	public static class Factory {

		public static List<ContextWithShapes> getShapes(ShapeSource shapeSource, ShaclSail shaclSail) {

			List<ContextWithShapes> parsed = parse(shapeSource, shaclSail);

			return parsed.stream()
					.map(contextWithShapes -> {
						List<Shape> split = split(contextWithShapes.getShapes());
						calculateTargetChain(split);
						calculateIfProducesValidationResult(split);
						return new ContextWithShapes(contextWithShapes.getDataGraph(),
								contextWithShapes.getShapeGraph(), split);

					})
					.collect(Collectors.toList());

		}

		private static void calculateIfProducesValidationResult(List<Shape> split) {
			for (Shape shape : split) {
				assert shape.constraintComponents.size() == 1;

				if (shape instanceof PropertyShape || shape.constraintComponents.get(0) instanceof PropertyShape) {

					PropertyShape propertyShape;
					if (shape instanceof PropertyShape) {
						propertyShape = (PropertyShape) shape;
					} else {
						propertyShape = (PropertyShape) shape.constraintComponents.get(0);
					}

					// Nested PropertyShape constraints only produce a validation result for the last PropertyShape in
					// the chain of PropertyShapes.
					while (propertyShape.constraintComponents.get(0) instanceof PropertyShape) {
						assert propertyShape.constraintComponents.size() == 1;
						if (propertyShape.constraintComponents.get(0) instanceof PropertyShape) {
							propertyShape = (PropertyShape) propertyShape.constraintComponents.get(0);
						}
					}

					propertyShape.produceValidationReports = true;

				} else if (shape instanceof NodeShape) {
					shape.produceValidationReports = true;
				}
			}
		}

		private static void calculateTargetChain(List<Shape> parsed) {
			for (Shape shape : parsed) {
				assert (shape.target.size() == 1);

				shape.setTargetChain(new TargetChain().add(shape.target.get(0)));

			}

		}

		private static List<Shape> split(List<Shape> collect) {
			// split into shapes by target and constraint component to be able to run them in parallel
			return collect.stream().flatMap(s -> {
				List<Shape> temp = new ArrayList<>();
				s.target.forEach(target -> {
					s.constraintComponents.forEach(constraintComponent -> {

						if (constraintComponent instanceof PropertyShape) {
							List<PropertyShape> split = splitPropertyShape(((PropertyShape) constraintComponent))
									.collect(Collectors.toList());
							for (PropertyShape propertyShape : split) {
								Shape shape = s.shallowClone();
								shape.target.add(target);
								shape.constraintComponents.add(propertyShape);
								temp.add(shape);
							}
						} else {
							Shape shape = s.shallowClone();
							shape.target.add(target);
							shape.constraintComponents.add(constraintComponent);
							temp.add(shape);

						}

					});
				});
				return temp.stream();
			}).collect(Collectors.toList());
		}

		private static Stream<PropertyShape> splitPropertyShape(PropertyShape propertyShape) {
			return propertyShape.constraintComponents.stream()
					.flatMap(constraintComponent -> {
						if (constraintComponent instanceof PropertyShape) {
							return splitPropertyShape(((PropertyShape) constraintComponent))
									.map(splitConstraintComponent -> {
										PropertyShape propertyShapeClone = (PropertyShape) propertyShape.shallowClone();
										propertyShapeClone.constraintComponents.add(splitConstraintComponent);
										return propertyShapeClone;
									});
						} else {
							PropertyShape propertyShapeClone = (PropertyShape) propertyShape.shallowClone();
							propertyShapeClone.constraintComponents.add(constraintComponent.deepClone());
							return Stream.of(propertyShapeClone);
						}
					});
		}

		private static List<ContextWithShapes> parse(ShapeSource shapeSource, ShaclSail shaclSail) {

			try (Stream<ShapeSource.ShapesGraph> allShapeContexts = shapeSource.getAllShapeContexts()) {
				return allShapeContexts
						.map(shapesGraph -> {
							Cache cache = new Cache();
							return getShapesInContext(shapeSource, shaclSail, cache, shapesGraph.getDataGraph(),
									shapesGraph.getShapesGraph());
						})
						.collect(Collectors.toList());

			}

		}

		private static ContextWithShapes getShapesInContext(ShapeSource shapeSource, ShaclSail shaclSail, Cache cache,
				Resource[] dataGraph, Resource[] shapesGraph) {
			ShapeSource shapeSourceWithContext = shapeSource.withContext(shapesGraph);

			try (Stream<Resource> resources = shapeSourceWithContext.getTargetableShape()) {
				List<Shape> shapes = resources
						.map(r -> new ShaclProperties(r, shapeSourceWithContext))
						.map(p -> {
							if (p.getType() == SHACL.NODE_SHAPE) {
								return NodeShape.getInstance(p, shapeSourceWithContext, cache, shaclSail);
							} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
								return PropertyShape.getInstance(p, shapeSourceWithContext, cache, shaclSail);
							}
							throw new IllegalStateException("Unknown shape type for " + p.getId());
						})
						.collect(Collectors.toList());

				return new ContextWithShapes(dataGraph, shapesGraph, shapes);
			}
		}

	}

	@Override
	public String toString() {
		Model statements = toModel(new DynamicModel(new LinkedHashModelFactory()));
		statements.setNamespace(SHACL.NS);
		statements.setNamespace(XSD.NS);
		statements.setNamespace(RSX.NS);
		statements.setNamespace(RDFS.NS);
		statements.setNamespace(RDF.NS);
		WriterConfig writerConfig = new WriterConfig();
		writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
		writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		StringWriter stringWriter = new StringWriter();
		Rio.write(statements, stringWriter, RDFFormat.TURTLE, writerConfig);

		return stringWriter.toString()
				.replaceAll("(?m)^(@prefix)(.*)(\\.)$", "") // remove all lines that are prefix declarations
				.trim();
	}

}
