package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Cache;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.HelperTool;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.NodeShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Shape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.DebugPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.planNodes.AbstractBulkJoinPlanNode;

public class AndConstraintComponent extends AbstractConstraintComponent {
	List<Shape> and;

	public AndConstraintComponent(Resource id, RepositoryConnection connection,
			Cache cache, ShaclSail shaclSail) {
		super(id);
		and = HelperTool.toList(connection, id, Resource.class)
				.stream()
				.map(r -> new ShaclProperties(r, connection))
				.map(p -> {
					if (p.getType() == SHACL.NODE_SHAPE) {
						return NodeShape.getInstance(p, connection, cache, false, shaclSail);
					} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
						return PropertyShape.getInstance(p, connection, cache, shaclSail);
					}
					throw new IllegalStateException("Unknown shape type for " + p.getId());
				})
				.collect(Collectors.toList());

	}

	public AndConstraintComponent(AndConstraintComponent andConstraintComponent) {
		super(andConstraintComponent.getId());
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.AND, getId());
		HelperTool.listToRdf(and.stream().map(Shape::getId).collect(Collectors.toList()), getId(), model);

		if (exported.contains(getId())) {
			return;
		}
		exported.add(getId());
		and.forEach(o -> o.toModel(null, null, model, exported));

	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		super.setTargetChain(targetChain);
		for (Shape shape : and) {
			shape.setTargetChain(targetChain.setOptimizable(false));
		}
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.AndConstraintComponent;
	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			boolean negatePlan, boolean negateChildren, Scope scope) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		PlanNode planNode = and.stream()
				.map(a -> a.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans,
						overrideTargetNode, scope))
				.reduce(UnionNode::new)
				.orElse(new EmptyNode());

		planNode = new DebugPlanNode(planNode, p -> {
			assert p != null;
		});

		return new Unique(planNode);

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		PlanNode planNode = and.stream()
				.map(c -> c.getAllTargetsPlan(connectionsGroup, scope))
				.reduce(UnionNode::new)
				.orElse(new EmptyNode());

		planNode = new Unique(planNode);

		planNode = new DebugPlanNode(planNode, "AndConstraintComponent::getAllTargetsPlan");

		return planNode;
	}

	@Override
	public ConstraintComponent deepClone() {

		AndConstraintComponent andConstraintComponent = new AndConstraintComponent(this);
		andConstraintComponent.and = and.stream()
				.map(ConstraintComponent::deepClone)
				.map(a -> ((Shape) a))
				.collect(Collectors.toList());
		return andConstraintComponent;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope) {
		return and.stream().anyMatch(c -> c.requiresEvaluation(connectionsGroup, scope));
	}



	@Override
	public String buildSparqlValidNodes_rsx_targetShape(Var subject, Var object,
														RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {

		List<? extends Class<? extends Shape>> type = and
			.stream()
			.map(s -> {
				if (s instanceof NodeShape) {
					return NodeShape.class;
				}
				if (s instanceof PropertyShape) {
					return PropertyShape.class;
				}
				throw new IllegalStateException("Unknown shape type: " + s.getClass());
			})
			.distinct()
			.collect(Collectors.toList());

		if (type.size() > 1) {
			throw new UnsupportedOperationException(
				"OrConstraintComponent found both NodeShape and PropertyShape as children");
		}

		Class<? extends Shape> aClass = type.get(0);

		if (scope == Scope.nodeShape) {

			throw  new UnsupportedOperationException();
		} else if (scope == Scope.propertyShape) {

			if (aClass == PropertyShape.class) {
				throw  new UnsupportedOperationException();
			} else {

				Path path = getTargetChain().getPath().get();
				String objectVariable = randomVariable();

				String collect = and
					.stream()
					.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object,
						rdfsSubClassOfReasoner, scope))
					.reduce((a, b) -> a + " && " + b)
					.orElse("");

				String pathQuery1 = path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner);

				String query = pathQuery1 + "\n FILTER (! EXISTS {\n" + pathQuery1.replaceAll("(?m)^", "\t")
					+ "\n\tFILTER(!(" + collect + "))\n})";

				String pathQuery2 = path.getTargetQueryFragment(subject, new Var(UUID.randomUUID().toString().replace("-", "")), rdfsSubClassOfReasoner);

				query = "{\n" +
					AbstractBulkJoinPlanNode.VALUES_INJECTION_POINT + "\n " +
					"" + query.replaceAll("(?m)^", "\t") + " \n" +
					"} UNION {\n" +
					"\t " + AbstractBulkJoinPlanNode.VALUES_INJECTION_POINT + "\n" +
					"\t ?" + subject.getName() + " " + randomVariable() + " " + randomVariable() + ".\n" +
					"\t FILTER(NOT EXISTS {\n " +
					"" + pathQuery2.replaceAll("(?m)^", "\t")
					+ " \n" +
					"})\n" +
					"}";

				return query;
			}
		} else {
			throw new UnsupportedOperationException("Unknown scope: " + scope);
		}

	}

	private String randomVariable() {
		return "?" + UUID.randomUUID().toString().replace("-", "");
	}

	@Override
	public Stream<? extends StatementPattern> getStatementPatterns_rsx_targetShape(Var subject, Var object,
																				   RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {

		StatementPattern subjectPattern = new StatementPattern(
			object,
			new Var(UUID.randomUUID().toString().replace("-", "")),
			new Var(UUID.randomUUID().toString().replace("-", ""))
		);

		StatementPattern objectPattern = new StatementPattern(
			new Var(UUID.randomUUID().toString().replace("-", "")),
			new Var(UUID.randomUUID().toString().replace("-", "")),
			object
		);

		Stream<StatementPattern> statementPatternStream = and.stream()
			.flatMap(c -> c.getStatementPatterns_rsx_targetShape(object, new Var("someVarName"),
				rdfsSubClassOfReasoner, Scope.nodeShape));

		return Stream.concat(statementPatternStream, Stream.of(subjectPattern, objectPattern));

	}
}
