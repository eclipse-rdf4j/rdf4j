package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.Cache;
import org.eclipse.rdf4j.sail.shacl.ast.NodeShape;
import org.eclipse.rdf4j.sail.shacl.ast.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.NotValuesIn;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;

public class NotConstraintComponent extends AbstractConstraintComponent {
	Shape not;

	public NotConstraintComponent(Resource id, RepositoryConnection connection,
			Cache cache, ShaclSail shaclSail) {
		super(id);

		ShaclProperties p = new ShaclProperties(id, connection);

		if (p.getType() == SHACL.NODE_SHAPE) {
			not = NodeShape.getInstance(p, connection, cache, false, shaclSail);
		} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
			not = PropertyShape.getInstance(p, connection, cache, shaclSail);
		} else {
			throw new IllegalStateException("Unknown shape type for " + p.getId());
		}

	}

	public NotConstraintComponent(NotConstraintComponent notConstraintComponent) {
		super(notConstraintComponent.getId());
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.NOT, getId());

		not.toModel(null, null, model, cycleDetection);

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
		return not.generateSparqlValidationPlan(connectionsGroup, logValidationPlans, !negatePlan, false, scope);
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

		PlanNode planNode = not.generateTransactionalValidationPlan(
				connectionsGroup,
				logValidationPlans,
				planNodeProvider,
				scope
		);

		PlanNode invalid = new Unique(planNode, false);

		PlanNode allTargetsPlan;
		if (overrideTargetNode != null) {
			if (scope == Scope.propertyShape) {
				allTargetsPlan = getTargetChain()
						.getEffectiveTarget("_target", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.extend(planNodeProvider.getPlanNode(), connectionsGroup, Scope.nodeShape,
								EffectiveTarget.Extend.right, false);
				allTargetsPlan = new Unique(new ShiftToPropertyShape(allTargetsPlan), true);
			} else {
				allTargetsPlan = getTargetChain()
						.getEffectiveTarget("_target", scope, connectionsGroup.getRdfsSubClassOfReasoner())
						.extend(planNodeProvider.getPlanNode(), connectionsGroup, scope, EffectiveTarget.Extend.right,
								false);
			}

		} else {
			allTargetsPlan = planNodeProvider.getPlanNode();
		}

		invalid = new NotValuesIn(allTargetsPlan, invalid);

		return invalid;

	}

	/*
	 * PlanNodeProvider targetProvider = overrideTargetNode; if (targetProvider == null) { targetProvider = () ->
	 * getAllTargetsPlan(connectionsGroup, negatePlan, scope); }else{ System.out.println(); }
	 *
	 * PlanNode allTargetsPlan = targetProvider.getPlanNode();
	 *
	 * allTargetsPlan = new DebugPlanNode(allTargetsPlan, p -> { System.out.println("HERE!" + p); });
	 *
	 * PlanNode planNode = not.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans, targetProvider,
	 * negateChildren, false, scope);
	 *
	 * PlanNode invalid = new Unique(planNode);
	 *
	 * PlanNode discardedLeft = new NotValuesIn(allTargetsPlan, invalid);
	 *
	 * discardedLeft = new DebugPlanNode(discardedLeft, p -> { System.out.println(); });
	 *
	 * return discardedLeft;
	 *
	 */

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

		PlanNode notTargets = not.getAllTargetsPlan(connectionsGroup, scope);

		return new Unique(new UnionNode(allTargets, notTargets), false);
	}

	@Override
	public ConstraintComponent deepClone() {
		NotConstraintComponent notConstraintComponent = new NotConstraintComponent(this);
		notConstraintComponent.not = (Shape) not.deepClone();
		return notConstraintComponent;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope) {
		return not.requiresEvaluation(connectionsGroup, scope);
	}
}
