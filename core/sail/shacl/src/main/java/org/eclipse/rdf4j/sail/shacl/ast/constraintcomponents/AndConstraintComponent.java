package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.eclipse.rdf4j.sail.shacl.ast.NodeShape;
import org.eclipse.rdf4j.sail.shacl.ast.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;

public class AndConstraintComponent extends LogicalOperatorConstraintComponent {
	List<Shape> and;

	public AndConstraintComponent(Resource id, RepositoryConnection connection,
			Cache cache, ShaclSail shaclSail) {
		super(id);
		and = ShaclAstLists.toList(connection, id, Resource.class)
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
			ShaclAstLists.listToRdf(and.stream().map(Shape::getId).collect(Collectors.toList()), getId(), model);
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

		return buildSparqlValidNodes_rsx_targetShape_inner(subject, object, rdfsSubClassOfReasoner, scope, and,
				getTargetChain(),
				SparqlFragment::join, SparqlFragment::and);

	}

}
