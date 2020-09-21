package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Cache;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.HelperTool;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.NodeShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Shape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.DebugPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EqualsJoinValue;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetChain;

public class OrConstraintComponent extends AbstractConstraintComponent {
	List<Shape> or;

	public OrConstraintComponent(Resource id, RepositoryConnection connection,
			Cache cache) {
		super(id);
		or = HelperTool.toList(connection, id, Resource.class)
				.stream()
				.map(r -> new ShaclProperties(r, connection))
				.map(p -> {
					if (p.getType() == SHACL.NODE_SHAPE) {
						return NodeShape.getInstance(p, connection, cache);
					} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
						return PropertyShape.getInstance(p, connection, cache);
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
			PlanNodeProvider overrideTargetNode, boolean negatePlan, boolean negateChildren, Scope scope) {
		// if (scope == Scope.nodeShape) {

		PlanNodeProvider planNodeProvider;

		if (overrideTargetNode != null) {
			planNodeProvider = overrideTargetNode;
		} else {
			planNodeProvider = () -> getAllTargetsPlan(connectionsGroup, negatePlan, scope);
		}

		PlanNode orPlanNodes = or.stream()
				.map(or -> or.generateTransactionalValidationPlan(
						connectionsGroup,
						logValidationPlans,
						planNodeProvider,
						negateChildren,
						false,
						scope
				)
				)
				.reduce((a, b) -> new EqualsJoinValue(a, b, true))
				.orElse(new EmptyNode());

		PlanNode invalid = new Unique(orPlanNodes);

		invalid = new DebugPlanNode(invalid, "", p -> {
			System.out.println();
		});

		return invalid;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated, Scope scope) {
		PlanNode allTargets;

		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain().getEffectiveTarget("target_", Scope.nodeShape)
					.getPlanNode(connectionsGroup, Scope.nodeShape, true);

			allTargets = new Unique(new Sort(new ShiftToPropertyShape(allTargetsPlan)));
		} else {
			allTargets = getTargetChain().getEffectiveTarget("target_", scope)
					.getPlanNode(connectionsGroup, scope, true);

		}

		PlanNode planNode = or.stream()
				.map(or -> or.getAllTargetsPlan(connectionsGroup, negated, scope))
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
}
