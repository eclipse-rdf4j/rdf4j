package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

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
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.Cache;
import org.eclipse.rdf4j.sail.shacl.ast.HelperTool;
import org.eclipse.rdf4j.sail.shacl.ast.NodeShape;
import org.eclipse.rdf4j.sail.shacl.ast.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EqualsJoinValue;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;

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
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection,
			Set<Resource> rdfListDedupe) {
		model.add(subject, SHACL.OR, getId());
		if (!cycleDetection.contains(getId())) {
			cycleDetection.add(getId());
			or.forEach(o -> o.toModel(null, null, model, cycleDetection, rdfListDedupe));
		}

		if (!rdfListDedupe.contains(getId())) {
			rdfListDedupe.add(getId());
			HelperTool.listToRdf(or.stream().map(Shape::getId).collect(Collectors.toList()), getId(), model);
		}

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
			planNodeProvider = () -> getAllTargetsPlan(connectionsGroup, scope);
		}

		PlanNode orPlanNodes = or.stream()
				.map(or -> or.generateTransactionalValidationPlan(
						connectionsGroup,
						logValidationPlans,
						planNodeProvider,
						scope
				)
				)
				.reduce((a, b) -> new EqualsJoinValue(a, b, false))
				.orElse(new EmptyNode());

		PlanNode invalid = new Unique(orPlanNodes, false);

		return invalid;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		PlanNode allTargets;

		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.getPlanNode(connectionsGroup, Scope.nodeShape, true);

			allTargets = new Unique(new ShiftToPropertyShape(allTargetsPlan), true);
		} else {
			allTargets = getTargetChain()
					.getEffectiveTarget("target_", scope, connectionsGroup.getRdfsSubClassOfReasoner())
					.getPlanNode(connectionsGroup, scope, true);

		}

		PlanNode planNode = or.stream()
				.map(or -> or.getAllTargetsPlan(connectionsGroup, scope))
				.reduce(UnionNode::new)
				.orElse(new EmptyNode());

		return new Unique(new UnionNode(allTargets, planNode), false);
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
//			query = "{\n" + VALUES_INJECTION_POINT + "\n " + query.replaceAll("(?m)^", "\t")
//				+ " \n} UNION {\n\t" + VALUES_INJECTION_POINT + "\n\t" + targetVar + " "
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
//					Collectors.joining("\n} UNION {\n" + VALUES_INJECTION_POINT + "\n",
//						"{\n" + VALUES_INJECTION_POINT + "\n",
//						"\n}"));
//		}
//	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {

		boolean isFilterCondition = or.stream()
				.map(o -> o.buildSparqlValidNodes_rsx_targetShape(subject, object, rdfsSubClassOfReasoner, scope))
				.map(SparqlFragment::isFilterCondition)
				.findFirst()
				.orElse(false);

		if (scope == Scope.nodeShape) {

			if (!isFilterCondition) {
				String collect = or
						.stream()
						.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object,
								rdfsSubClassOfReasoner, scope))
						.map(SparqlFragment::getFragment)
						.map(s -> s.replaceAll("(?m)^", "\t"))
						.collect(
								Collectors.joining(
										"\n} UNION {\n" + VALUES_INJECTION_POINT + "\n",
										"{\n" + VALUES_INJECTION_POINT + "\n",
										"\n}"));
				return SparqlFragment.bgp(collect);

			} else {
				String collect = or
						.stream()
						.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object,
								rdfsSubClassOfReasoner, scope))
						.map(SparqlFragment::getFragment)
						.collect(Collectors.joining(" ) || ( ", "( ", " )"));
				return SparqlFragment.filterCondition(collect);

			}
		} else if (scope == Scope.propertyShape) {

			if (!isFilterCondition) {
				throw new UnsupportedOperationException();
			} else {

				Path path = getTargetChain().getPath().get();

				String collect = or
						.stream()
						.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object,
								rdfsSubClassOfReasoner, scope))
						.map(SparqlFragment::getFragment)
						.collect(Collectors.joining(" ) || ( ", "( ",
								" )"));

				String pathQuery1 = path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner);

				String query = pathQuery1 + "\n FILTER (! EXISTS {\n" + pathQuery1.replaceAll("(?m)^", "\t")
						+ "\n\tFILTER(!(" + collect + "))\n})";

				String pathQuery2 = path.getTargetQueryFragment(subject,
						new StatementMatcher.Variable(UUID.randomUUID().toString().replace("-", "")),
						rdfsSubClassOfReasoner);

				query = "{\n" +
						VALUES_INJECTION_POINT + "\n " +
						"" + query.replaceAll("(?m)^", "\t") + " \n" +
						"} UNION {\n" +
						"\t " + VALUES_INJECTION_POINT + "\n" +
						"\t ?" + subject.getName() + " " + randomVariable() + " " + randomVariable() + ".\n" +
						"\t FILTER(NOT EXISTS {\n " +
						"" + pathQuery2.replaceAll("(?m)^", "\t")
						+ " \n" +
						"})\n" +
						"}";

				return SparqlFragment.bgp(query);
			}
		} else {
			throw new UnsupportedOperationException("Unknown scope: " + scope);
		}

	}

	private String randomVariable() {
		return "?" + UUID.randomUUID().toString().replace("-", "");
	}

	@Override
	public Stream<StatementMatcher> getStatementMatchers_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {

		StatementMatcher subjectPattern = new StatementMatcher(
				object,
				null,
				null
		);

		StatementMatcher objectPattern = new StatementMatcher(
				null,
				null,
				object
		);

		Stream<StatementMatcher> statementPatternStream = or.stream()
				.flatMap(c -> c.getStatementMatchers_rsx_targetShape(object,
						new StatementMatcher.Variable("someVarName"),
						rdfsSubClassOfReasoner, Scope.nodeShape));

		return Stream.concat(statementPatternStream, Stream.of(subjectPattern, objectPattern));

	}

}
