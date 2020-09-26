package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.DebugPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.GroupByCountFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.EffectiveTarget;

public class MinCountConstraintComponent extends AbstractConstraintComponent {

	long minCount;

	public MinCountConstraintComponent(long minCount) {
		this.minCount = minCount;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MIN_COUNT,
				SimpleValueFactory.getInstance().createLiteral(minCount + "", XMLSchema.INTEGER));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MinCountConstraintComponent;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, boolean negatePlan, boolean negateChildren, Scope scope) {

		PlanNode target = getTargetChain().getEffectiveTarget("_target", scope)
				.getPlanNode(connectionsGroup, scope, true);

		System.out.println(getTargetChain().getChain().stream().findFirst().get());

		target = new DebugPlanNode(target, "", p -> {
			System.out.println();
		});

		if (overrideTargetNode != null) {
			target = getTargetChain().getEffectiveTarget("_target", scope)
					.extend(overrideTargetNode.getPlanNode(), connectionsGroup, scope, EffectiveTarget.Extend.right,
							false);
		}

		target = new Unique(new TrimToTarget(target));

		PlanNode relevantTargetsWithPath = new BulkedExternalLeftOuterJoin(
				target,
				connectionsGroup.getBaseConnection(),
				getTargetChain().getPath().get().getTargetQueryFragment(new Var("a"), new Var("c")),
				false,
				null,
				(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
		);

		PlanNode groupByCount = new GroupByCountFilter(relevantTargetsWithPath, count -> count < minCount);

		return new Unique(new TrimToTarget(groupByCount));

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated, Scope scope) {
		return new EmptyNode();
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MinCountConstraintComponent(minCount);
	}
}
