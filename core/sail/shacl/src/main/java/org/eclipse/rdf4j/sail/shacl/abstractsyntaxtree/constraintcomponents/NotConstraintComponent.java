package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Cache;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.NodeShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Shape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.DebugPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.NotValuesIn;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ShiftToNodeShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.TargetChainPopper;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetChain;

public class NotConstraintComponent extends AbstractConstraintComponent {
	final Shape not;

	public NotConstraintComponent(Resource id, RepositoryConnection connection,
			Cache cache) {
		super(id);

		ShaclProperties p = new ShaclProperties(id, connection);

		if (p.getType() == SHACL.NODE_SHAPE) {
			not = NodeShape.getInstance(p, connection, cache);
		} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
			not = PropertyShape.getInstance(p, connection, cache);
		} else {
			throw new IllegalStateException("Unknown shape type for " + p.getId());
		}

	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.NOT, getId());

		not.toModel(null, null, model, exported);

	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		super.setTargetChain(targetChain);
		not.setTargetChain(targetChain.setOptimizable(false));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.NotConstraintComponent;
	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			boolean negatePlan, boolean negateChildren, Scope scope) {
		return not.generateSparqlValidationPlan(connectionsGroup, logValidationPlans, !negatePlan, false, Scope.not);
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, boolean negatePlan, boolean negateChildren, Scope scope) {

		// if (scope == Scope.nodeShape) {

		PlanNode allTargetsPlan = getTargets(connectionsGroup, scope);

		PlanNode planNode = not.generateTransactionalValidationPlan(connectionsGroup,
				logValidationPlans,
				() -> getTargets(connectionsGroup, scope),
				negateChildren,
				false, scope);

		allTargetsPlan = new DebugPlanNode(allTargetsPlan, "", p -> {
			System.out.println("HERE!" + p);
		});

		PlanNode invalid = new Unique(planNode);

		PlanNode discardedLeft = new NotValuesIn(allTargetsPlan, invalid);

		discardedLeft = new DebugPlanNode(discardedLeft, "", p -> {
			System.out.println();
		});

		return discardedLeft;
//		}
//
//		throw new UnsupportedOperationException();
	}

	private PlanNode getTargets(ConnectionsGroup connectionsGroup, Scope scope) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain().getEffectiveTarget("target_", Scope.nodeShape)
					.getAdded(connectionsGroup, Scope.nodeShape);
			return new ShiftToPropertyShape(allTargetsPlan);
		}

		return getTargetChain().getEffectiveTarget("target_", scope).getAdded(connectionsGroup, scope);
	}
}
