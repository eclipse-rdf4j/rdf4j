package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import static org.eclipse.rdf4j.model.util.Values.literal;

import java.math.BigInteger;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.GroupByCountFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.LeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;

public class MinCountConstraintComponent extends AbstractConstraintComponent {

	long minCount;

	public MinCountConstraintComponent(long minCount) {
		this.minCount = minCount;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.MIN_COUNT,
				literal(BigInteger.valueOf(minCount)));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MinCountConstraintComponent;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		PlanNode target = getTargetChain()
				.getEffectiveTarget("_target", scope, connectionsGroup.getRdfsSubClassOfReasoner())
				.getPlanNode(connectionsGroup, scope, true);

		if (overrideTargetNode != null) {
			target = getTargetChain().getEffectiveTarget("_target", scope, connectionsGroup.getRdfsSubClassOfReasoner())
					.extend(overrideTargetNode.getPlanNode(), connectionsGroup, scope, EffectiveTarget.Extend.right,
							false);
		} else {
			// we can assume that we are not doing bulk validation, so it is worth checking our added statements before
			// we go to the base sail

			PlanNode addedByPath = getTargetChain().getPath().get().getAdded(connectionsGroup, null);
			LeftOuterJoin leftOuterJoin = new LeftOuterJoin(target, addedByPath);
			target = new GroupByCountFilter(leftOuterJoin, count -> count < minCount);
		}

		PlanNode relevantTargetsWithPath = new BulkedExternalLeftOuterJoin(
				new Unique(new TrimToTarget(target), false),
				connectionsGroup.getBaseConnection(),
				getTargetChain().getPath()
						.get()
						.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
								connectionsGroup.getRdfsSubClassOfReasoner()),
				false,
				null,
				(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
		);

		PlanNode groupByCount = new GroupByCountFilter(relevantTargetsWithPath, count -> count < minCount);

		return new Unique(new TrimToTarget(groupByCount), false);

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		return new EmptyNode();
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MinCountConstraintComponent(minCount);
	}
}
