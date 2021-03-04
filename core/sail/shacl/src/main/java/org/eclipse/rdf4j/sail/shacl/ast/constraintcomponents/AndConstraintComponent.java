package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

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
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;

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
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.AND, getId());

		if (!cycleDetection.contains(getId())) {
			cycleDetection.add(getId());
			and.forEach(o -> o.toModel(null, null, model, cycleDetection));
		}

		if (!model.contains(getId(), null, null)) {
			HelperTool.listToRdf(and.stream().map(Shape::getId).collect(Collectors.toList()), getId(), model);
		}

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

		return new Unique(planNode, false);

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		PlanNode planNode = and.stream()
				.map(c -> c.getAllTargetsPlan(connectionsGroup, scope))
				.reduce(UnionNode::new)
				.orElse(new EmptyNode());

		planNode = new Unique(planNode, false);

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
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {

		boolean isFilterCondition = and.stream()
				.map(o -> o.buildSparqlValidNodes_rsx_targetShape(subject, object, rdfsSubClassOfReasoner, scope))
				.map(SparqlFragment::isFilterCondition)
				.findFirst()
				.orElse(false);

		if (scope == Scope.nodeShape) {

			if (!isFilterCondition) {
				String collect = and
						.stream()
						.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object,
								rdfsSubClassOfReasoner, scope))
						.map(SparqlFragment::getFragment)
						.map(s -> s.replaceAll("(?m)^", "\t"))
						.reduce((a, b) -> a + " \n " + b)
						.orElse("");
				return SparqlFragment.bgp(collect);

			} else {
				String collect = and
						.stream()
						.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object,
								rdfsSubClassOfReasoner, scope))
						.map(SparqlFragment::getFragment)
						.collect(Collectors.joining(" ) && ( ", "( ",
								" )"));
				return SparqlFragment.filterCondition(collect);

			}
		} else if (scope == Scope.propertyShape) {

			if (!isFilterCondition) {
				throw new UnsupportedOperationException();
			} else {

				Path path = getTargetChain().getPath().get();

				String collect = and
						.stream()
						.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object,
								rdfsSubClassOfReasoner, scope))
						.map(SparqlFragment::getFragment)
						.collect(Collectors.joining(" ) && ( ", "( ",
								" )"));

				String pathQuery1 = path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner);

				String query = pathQuery1 + "\n FILTER (! EXISTS {\n" + pathQuery1.replaceAll("(?m)^", "\t")
						+ "\n\tFILTER(!(" + collect + "))\n})";

				String pathQuery2 = path.getTargetQueryFragment(subject,
						StatementMatcher.Variable.getRandomInstance(), rdfsSubClassOfReasoner);

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

		Stream<StatementMatcher> statementPatternStream = and.stream()
				.flatMap(c -> c.getStatementMatchers_rsx_targetShape(object,
						StatementMatcher.Variable.getRandomInstance(),
						rdfsSubClassOfReasoner, Scope.nodeShape));

		return Stream.concat(statementPatternStream, Stream.of(subjectPattern, objectPattern));

	}
}
