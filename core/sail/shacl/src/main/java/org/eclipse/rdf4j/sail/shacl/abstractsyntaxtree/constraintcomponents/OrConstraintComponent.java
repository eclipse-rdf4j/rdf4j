package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Collections;
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
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EqualsJoinValue;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.planNodes.AbstractBulkJoinPlanNode;

public class OrConstraintComponent extends AbstractConstraintComponent {
	List<Shape> or;

	public OrConstraintComponent(Resource id, RepositoryConnection connection,
			Cache cache, ShaclSail shaclSail) {
		super(id);
		or = HelperTool.toList(connection, id, Resource.class)
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

	public OrConstraintComponent(OrConstraintComponent orConstraintComponent) {
		super(orConstraintComponent.getId());
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.OR, getId());
		HelperTool.listToRdf(or.stream().map(Shape::getId).collect(Collectors.toList()), getId(), model);

		if (exported.contains(getId())) {
			return;
		}
		exported.add(getId());
		or.forEach(o -> o.toModel(null, null, model, exported));

	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		super.setTargetChain(targetChain);
		for (Shape shape : or) {
			shape.setTargetChain(targetChain.setOptimizable(false));
		}
	}

	public List<Shape> getOr() {
		return Collections.unmodifiableList(or);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.OrConstraintComponent;
	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			boolean negatePlan, boolean negateChildren, Scope scope) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, Scope scope) {
		// if (scope == Scope.nodeShape) {

		PlanNodeProvider planNodeProvider;

		if (overrideTargetNode != null) {
			planNodeProvider = overrideTargetNode;
		} else {
			planNodeProvider = () -> new DebugPlanNode(getAllTargetsPlan(connectionsGroup, scope), "",
					p -> {
						assert p != null;
					});
		}

		PlanNode orPlanNodes = or.stream()
				.map(or -> or.generateTransactionalValidationPlan(
						connectionsGroup,
						logValidationPlans,
						planNodeProvider,
						scope
				)
				)
				.map(p -> {
					return (PlanNode) new DebugPlanNode(p, "", p1 -> {
						assert p1 != null;
					});
				})
				.reduce((a, b) -> new EqualsJoinValue(a, b, true))
				.orElse(new EmptyNode());

		PlanNode invalid = new Unique(orPlanNodes);

		invalid = new DebugPlanNode(invalid, p -> {
			assert p != null;
		});

		return invalid;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		PlanNode allTargets;

		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.getPlanNode(connectionsGroup, Scope.nodeShape, true);

			allTargets = new Unique(new ShiftToPropertyShape(allTargetsPlan));
		} else {
			allTargets = getTargetChain()
					.getEffectiveTarget("target_", scope, connectionsGroup.getRdfsSubClassOfReasoner())
					.getPlanNode(connectionsGroup, scope, true);

		}

		PlanNode planNode = or.stream()
				.map(or -> or.getAllTargetsPlan(connectionsGroup, scope))
				.reduce(UnionNode::new)
				.orElse(new EmptyNode());

		return new Unique(new UnionNode(allTargets, planNode));
	}

	@Override
	public ConstraintComponent deepClone() {

		OrConstraintComponent constraintComponent = new OrConstraintComponent(this);
		constraintComponent.or = or.stream()
				.map(ConstraintComponent::deepClone)
				.map(a -> ((Shape) a))
				.collect(Collectors.toList());
		return constraintComponent;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope) {
		return or.stream().anyMatch(c -> c.requiresEvaluation(connectionsGroup, scope));
	}

//	@Override
//	public String buildSparqlValidNodes_rsx_targetShape(Var subject, Var object, RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {
//		if (scope == Scope.propertyShape) {
//			// within property shape
//			String objectVariable = randomVariable();
//			String pathQuery1 = getTargetChain().getPath().get().getTargetQueryFragment(subject, object, null);
//
//			String collect = or.stream()
//				.map(l -> l.stream()
//					.map(p -> p.buildSparqlValidNodes(objectVariable))
//					.reduce((a, b) -> a + " && " + b))
//				.filter(Optional::isPresent)
//				.map(Optional::get)
//				.collect(Collectors.joining(" ) || ( ", "( ",
//					" )"));
//
//			String query = pathQuery1 + "\n FILTER (! EXISTS {\n" + pathQuery1.replaceAll("(?m)^", "\t")
//				+ "\n\tFILTER(!(" + collect + "))\n})";
//
//			String pathQuery2 = getPath().getQuery(targetVar, randomVariable(), null);
//
//			query = "{\n" + AbstractBulkJoinPlanNode.VALUES_INJECTION_POINT + "\n " + query.replaceAll("(?m)^", "\t")
//				+ " \n} UNION {\n\t" + AbstractBulkJoinPlanNode.VALUES_INJECTION_POINT + "\n\t" + targetVar + " "
//				+ randomVariable() + " "
//				+ randomVariable() + ".\n\tFILTER(NOT EXISTS {\n " + pathQuery2.replaceAll("(?m)^", "\t")
//				+ " \n})\n}";
//
//			return query;
//		} else if (!childrenHasOwnPathRecursive()) {
//
//			return or.stream()
//				.map(l -> l.stream()
//					.map(p -> p.buildSparqlValidNodes(targetVar))
//					.reduce((a, b) -> a + " && " + b))
//				.filter(Optional::isPresent)
//				.map(Optional::get)
//				.collect(Collectors.joining(" ) || ( ", "( ",
//					" )"));
//		} else {
//			// within node shape
//			return or.stream()
//				.map(l -> l.stream().map(p -> p.buildSparqlValidNodes(targetVar)).reduce((a, b) -> a + "\n" + b))
//				.filter(Optional::isPresent)
//				.map(Optional::get)
//				.map(s -> s.replaceAll("(?m)^", "\t"))
//				.collect(
//					Collectors.joining("\n} UNION {\n" + AbstractBulkJoinPlanNode.VALUES_INJECTION_POINT + "\n",
//						"{\n" + AbstractBulkJoinPlanNode.VALUES_INJECTION_POINT + "\n",
//						"\n}"));
//		}
//	}

	@Override
	public String buildSparqlValidNodes_rsx_targetShape(Var subject, Var object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {

		List<? extends Class<? extends Shape>> type = or
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

				String collect = or
						.stream()
						.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object,
								rdfsSubClassOfReasoner, scope))
						.reduce((a, b) -> a + " || " + b)
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

		Stream<StatementPattern> statementPatternStream = or.stream()
				.flatMap(c -> c.getStatementPatterns_rsx_targetShape(object, new Var("someVarName"),
						rdfsSubClassOfReasoner, Scope.nodeShape));

		return Stream.concat(statementPatternStream, Stream.of(subjectPattern, objectPattern));

	}

}
