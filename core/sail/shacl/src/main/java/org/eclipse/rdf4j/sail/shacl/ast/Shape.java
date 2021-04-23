/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/
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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
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

abstract public class Shape implements ConstraintComponent, Identifiable, Exportable, TargetChainInterface {
	Resource id;
	TargetChain targetChain;

	List<Target> target = new ArrayList<>();

	boolean deactivated;
	List<Literal> message;
	Severity severity = Severity.Violation;

	List<ConstraintComponent> constraintComponents = new ArrayList<>();

	public Shape() {
	}

	public Shape(Shape shape) {
		this.deactivated = shape.deactivated;
		this.message = shape.message;
		this.severity = shape.severity;
		this.id = shape.id;
		this.targetChain = shape.targetChain;
	}

	public void populate(ShaclProperties properties, RepositoryConnection connection,
			Cache cache, ShaclSail shaclSail) {
		this.deactivated = properties.isDeactivated();
		this.message = properties.getMessage();
		this.id = properties.getId();

		if (!properties.getTargetClass().isEmpty()) {
			target.add(new TargetClass(properties.getTargetClass()));
		}
		if (!properties.getTargetNode().isEmpty()) {
			target.add(new TargetNode(properties.getTargetNode()));
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
					.map(targetShape -> new RSXTargetShape(targetShape, connection, shaclSail))
					.forEach(target::add);

		}

		if (!properties.getTarget().isEmpty()) {
			properties.getTarget()
					.forEach(target -> {
//									if (connection.hasStatement(sparqlTarget, RDF.TYPE, SHACL.SPARQL_TARGET, true)) {
//										propertyShapes.add(new SparqlTarget(shapeId, shaclSail, connection,
//												shaclProperties.isDeactivated(), target));
//									}
						if (shaclSail.isDashDataShapes() && connection.hasStatement(target,
								RDF.TYPE, DASH.AllObjectsTarget, true)) {
							this.target.add(new DashAllObjects(target));
						}
						if (shaclSail.isDashDataShapes() && connection.hasStatement(target,
								RDF.TYPE, DASH.AllSubjectsTarget, true)) {
							this.target.add(new DashAllSubjects(target));
						}

					});

		}

	}

	@Override
	public Resource getId() {
		return id;
	}

	protected abstract Shape shallowClone();

	/**
	 *
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

	List<ConstraintComponent> getConstraintComponents(ShaclProperties properties, RepositoryConnection connection,
			Cache cache, ShaclSail shaclSail) {

		List<ConstraintComponent> constraintComponent = new ArrayList<>();

		properties.getProperty()
				.stream()
				.map(r -> new ShaclProperties(r, connection))
				.map(p -> PropertyShape.getInstance(p, connection, cache, shaclSail))
				.forEach(constraintComponent::add);

		properties.getNode()
				.stream()
				.map(r -> new ShaclProperties(r, connection))
				.map(p -> NodeShape.getInstance(p, connection, cache, true, shaclSail))
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
			constraintComponent.add(new LanguageInConstraintComponent(connection, properties.getLanguageIn()));
		}

		if (properties.getIn() != null) {
			constraintComponent.add(new InConstraintComponent(connection, properties.getIn()));
		}

		if (properties.getNodeKind() != null) {
			constraintComponent.add(new NodeKindConstraintComponent(properties.getNodeKind()));
		}

		if (properties.isClosed()) {
			constraintComponent.add(new ClosedConstraintComponent(connection, properties.getProperty(),
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
						properties.getQualifiedValueShape(), connection, cache, shaclSail,
						properties.getQualifiedValueShapesDisjoint(), properties.getQualifiedMaxCount());
				constraintComponent.add(qualifiedMaxCountConstraintComponent);
			}

			if (properties.getQualifiedMinCount() != null) {
				QualifiedMinCountConstraintComponent qualifiedMinCountConstraintComponent = new QualifiedMinCountConstraintComponent(
						properties.getQualifiedValueShape(), connection, cache, shaclSail,
						properties.getQualifiedValueShapesDisjoint(), properties.getQualifiedMinCount());
				constraintComponent.add(qualifiedMinCountConstraintComponent);
			}
		}

		if (shaclSail.isDashDataShapes()) {
			properties.getHasValueIn()
					.stream()
					.map(hasValueIn -> new DashHasValueInConstraintComponent(hasValueIn, connection))
					.forEach(constraintComponent::add);
		}

		properties.getOr()
				.stream()
				.map(or -> new OrConstraintComponent(or, connection, cache, shaclSail))
				.forEach(constraintComponent::add);

		properties.getXone()
				.stream()
				.map(xone -> new XoneConstraintComponent(xone, connection, cache, shaclSail))
				.forEach(constraintComponent::add);

		properties.getAnd()
				.stream()
				.map(and -> new AndConstraintComponent(and, connection, cache, shaclSail))
				.forEach(constraintComponent::add);

		properties.getNot()
				.stream()
				.map(or -> new NotConstraintComponent(or, connection, cache, shaclSail))
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

	public PlanNode generatePlans(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			boolean validateEntireBaseSail) {
		assert constraintComponents.size() == 1;

		ValidationApproach validationApproach = ValidationApproach.SPARQL;
		if (!validateEntireBaseSail) {
			validationApproach = constraintComponents.stream()
					.map(ConstraintComponent::getPreferedValidationApproach)
					.reduce(ValidationApproach::reduce)
					.get();
		}

		if (validationApproach == ValidationApproach.SPARQL) {
			if (Shape.this.getSupportedValidationApproaches().contains(ValidationApproach.SPARQL)) {
				return Shape.this.generateSparqlValidationPlan(connectionsGroup, logValidationPlans, false, false,
						Scope.none);
			} else {

				return Shape.this.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans,
						() -> Shape.this.getTargetChain()
								.getEffectiveTarget("_target",
										this instanceof NodeShape ? Scope.nodeShape : Scope.propertyShape,
										connectionsGroup.getRdfsSubClassOfReasoner())
								.getAllTargets(connectionsGroup,
										this instanceof NodeShape ? Scope.nodeShape : Scope.propertyShape),
						Scope.none);
			}

		} else if (validationApproach == ValidationApproach.Transactional) {
			if (this.requiresEvaluation(connectionsGroup, Scope.none)) {
				return Shape.this.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans, null,
						Scope.none);
			} else {
				return new EmptyNode();
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
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope) {
		return constraintComponents.stream().anyMatch(c -> c.requiresEvaluation(connectionsGroup, scope));
	}

	/**
	 * For rsx:targetShape
	 *
	 * @return
	 */
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {
		throw new UnsupportedOperationException(this.getClass().getSimpleName());
	}

	public static class Factory {

		public static List<Shape> getShapes(RepositoryConnection connection, ShaclSail shaclSail) {

			List<Shape> parsed = parse(connection, shaclSail);
			List<Shape> split = split(parsed);
			calculateTargetChain(split);

			return split;
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
							((PropertyShape) constraintComponent).constraintComponents
									.forEach(propertyConstraintComponent -> {
										PropertyShape clonedConstraintComponent = (PropertyShape) ((PropertyShape) constraintComponent)
												.shallowClone();
										clonedConstraintComponent.constraintComponents
												.add(propertyConstraintComponent.deepClone());

										Shape shape = s.shallowClone();
										shape.target.add(target);
										shape.constraintComponents.add(clonedConstraintComponent);
										temp.add(shape);
									});

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

		private static List<Shape> parse(RepositoryConnection connection, ShaclSail shaclSail) {
			Cache cache = new Cache();

			Set<Resource> resources = getTargetableShapes(connection);

			return resources.stream()
					.map(r -> new ShaclProperties(r, connection))
					.map(p -> {
						if (p.getType() == SHACL.NODE_SHAPE) {
							return NodeShape.getInstance(p, connection, cache, true, shaclSail);
						} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
							return PropertyShape.getInstance(p, connection, cache, shaclSail);
						}
						throw new IllegalStateException("Unknown shape type for " + p.getId());
					})
					.collect(Collectors.toList());
		}

		private static Set<Resource> getTargetableShapes(RepositoryConnection connection) {
			Set<Resource> collect;
			try (Stream<Statement> TARGET_NODE = connection.getStatements(null, SHACL.TARGET_NODE, null, true)
					.stream()) {
				try (Stream<Statement> TARGET_CLASS = connection.getStatements(null, SHACL.TARGET_CLASS, null, true)
						.stream()) {
					try (Stream<Statement> TARGET_SUBJECTS_OF = connection
							.getStatements(null, SHACL.TARGET_SUBJECTS_OF, null, true)
							.stream()) {
						try (Stream<Statement> TARGET_OBJECTS_OF = connection
								.getStatements(null, SHACL.TARGET_OBJECTS_OF, null, true)
								.stream()) {
							try (Stream<Statement> TARGET = connection
									.getStatements(null, SHACL.TARGET_PROP, null, true)
									.stream()) {
								try (Stream<Statement> RSX_TARGET_SHAPE = connection
										.getStatements(null, RSX.targetShape, null, true)
										.stream()) {

									collect = Stream
											.of(TARGET_CLASS, TARGET_NODE, TARGET_OBJECTS_OF, TARGET_SUBJECTS_OF,
													TARGET, RSX_TARGET_SHAPE)
											.reduce(Stream::concat)
											.get()
											.map(Statement::getSubject)
											.collect(Collectors.toSet());
								}
							}
						}
					}
				}
			}
			return collect;
		}
	}

	@Override
	public String toString() {
		Model statements = toModel(new DynamicModel(new LinkedHashModelFactory()));
		statements.setNamespace(SHACL.NS);
		statements.setNamespace(XSD.NS);
		WriterConfig writerConfig = new WriterConfig();
		writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
		writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		StringWriter stringWriter = new StringWriter();
		Rio.write(statements, stringWriter, RDFFormat.TURTLE, writerConfig);
		return stringWriter.toString()
				.replace("@prefix sh: <http://www.w3.org/ns/shacl#> .", "")
				.replace("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .", "")
				.trim();
	}

}
