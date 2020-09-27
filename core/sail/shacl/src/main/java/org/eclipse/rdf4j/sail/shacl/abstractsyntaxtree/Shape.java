package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree;

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
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.AndConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ClassConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ClosedConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.DatatypeConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.DisjointConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.EqualsConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.HasValueConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.HasValueInConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.InConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.LanguageInConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.LessThanConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.LessThanOrEqualsConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.MaxCountConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.MaxExclusiveConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.MaxInclusiveConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.MaxLengthConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.MinCountConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.MinExclusiveConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.MinInclusiveConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.MinLengthConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.NodeKindConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.NotConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.OrConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.PatternConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.UniqueLangConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.XoneConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.Target;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetClass;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetObjectsOf;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetSubjectsOf;

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
			Cache cache, ShaclSail sail) {
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

	}

	@Override
	public Resource getId() {
		return id;
	}

	protected abstract Shape shallowClone();

	public Model toModel(Model model) {
		toModel(null, null, model, new HashSet<>());
		return model;
	}

	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		ModelBuilder modelBuilder = new ModelBuilder();

		modelBuilder.subject(getId());

		if (deactivated) {
			modelBuilder.add(SHACL.DEACTIVATED, deactivated);
		}

		target.forEach(t -> {
			t.toModel(getId(), null, model, exported);
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

		if (shaclSail.isDashDataShapes()) {
			properties.getHasValueIn()
					.stream()
					.map(hasValueIn -> new HasValueInConstraintComponent(hasValueIn, connection))
					.forEach(constraintComponent::add);
		}

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
			return Shape.this.generateSparqlValidationPlan(connectionsGroup, logValidationPlans, false, false,
					Scope.none);

		} else if (validationApproach == ValidationApproach.Transactional) {
			return Shape.this.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans, null, false,
					false, Scope.none);
		} else {
			throw new ShaclUnsupportedException("Unkown validation approach: " + validationApproach);
		}

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

							collect = Stream
									.of(TARGET_CLASS, TARGET_NODE, TARGET_OBJECTS_OF, TARGET_SUBJECTS_OF)
									.reduce(Stream::concat)
									.get()
									.map(Statement::getSubject)
									.collect(Collectors.toSet());
						}

					}
				}
			}
			return collect;
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
}
