package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Collections;
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
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EqualsJoinValue;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;

public class OrConstraintComponent extends LogicalOperatorConstraintComponent {
	List<Shape> or;

	public OrConstraintComponent(Resource id, RepositoryConnection connection,
			Cache cache, ShaclSail shaclSail) {
		super(id);
		or = ShaclAstLists.toList(connection, id, Resource.class)
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
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.OR, getId());
		if (!cycleDetection.contains(getId())) {
			cycleDetection.add(getId());
			or.forEach(o -> o.toModel(null, null, model, cycleDetection));
		}

		if (!model.contains(getId(), null, null)) {
			ShaclAstLists.listToRdf(or.stream().map(Shape::getId).collect(Collectors.toList()), getId(), model);
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
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			boolean negatePlan, boolean negateChildren, Scope scope) {
		throw new ShaclUnsupportedException();
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		PlanNodeProvider planNodeProvider;

		if (overrideTargetNode != null) {
			planNodeProvider = overrideTargetNode;
		} else {
			planNodeProvider = new BufferedSplitter(getAllTargetsPlan(connectionsGroup, scope));
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
				.orElse(EmptyNode.getInstance());

		PlanNode invalid = Unique.getInstance(orPlanNodes, false);

		return invalid;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		PlanNode allTargets;

		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.getPlanNode(connectionsGroup, Scope.nodeShape, true, null);

			allTargets = Unique.getInstance(new ShiftToPropertyShape(allTargetsPlan), true);
		} else {
			allTargets = getTargetChain()
					.getEffectiveTarget("target_", scope, connectionsGroup.getRdfsSubClassOfReasoner())
					.getPlanNode(connectionsGroup, scope, true, null);

		}

		PlanNode planNode = or.stream()
				.map(or -> or.getAllTargetsPlan(connectionsGroup, scope))
				.distinct()
				.reduce(UnionNode::getInstanceDedupe)
				.orElse(EmptyNode.getInstance());

		return Unique.getInstance(UnionNode.getInstanceDedupe(allTargets, planNode), false);
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

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		return buildSparqlValidNodes_rsx_targetShape_inner(subject, object, rdfsSubClassOfReasoner, scope,
				stableRandomVariableProvider, or,
				getTargetChain(),
				SparqlFragment::union, SparqlFragment::or);

	}

}
