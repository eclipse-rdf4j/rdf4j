package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.HelperTool;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.DebugPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.GroupByFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValueInFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.EffectiveTarget;

public class HasValueInConstraintComponent extends AbstractConstraintComponent {

	final Set<Value> hasValueIn;

	public HasValueInConstraintComponent(Resource hasValueIn, RepositoryConnection connection) {
		super(hasValueIn);
		this.hasValueIn = Collections
				.unmodifiableSet(new LinkedHashSet<>(HelperTool.toList(connection, hasValueIn, Value.class)));
	}

	public HasValueInConstraintComponent(HasValueInConstraintComponent hasValueInConstraintComponent) {
		super(hasValueInConstraintComponent.getId());
		hasValueIn = hasValueInConstraintComponent.hasValueIn;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, DASH.hasValueIn, getId());
		HelperTool.listToRdf(hasValueIn, getId(), model);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.HasValueConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new HasValueInConstraintComponent(this);
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, boolean negatePlan, boolean negateChildren, Scope scope) {

		EffectiveTarget target = getTargetChain().getEffectiveTarget("_target", scope,
				connectionsGroup.getRdfsSubClassOfReasoner());

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
				addedTargets = target.extend(addedTargets, connectionsGroup, scope, EffectiveTarget.Extend.right,
						false);

			} else {
				addedTargets = target.getPlanNode(connectionsGroup, scope, true);
				PlanNode addedByPath = path.getAdded(connectionsGroup, null);

				addedByPath = target.getTargetFilter(connectionsGroup, new Unique(new TrimToTarget(addedByPath)));
				addedByPath = target.extend(addedByPath, connectionsGroup, scope, EffectiveTarget.Extend.left, false);

				addedTargets = new UnionNode(addedByPath, addedTargets);
				addedTargets = new Unique(addedTargets);
			}

			PlanNode joined = new BulkedExternalLeftOuterJoin(
					addedTargets,
					connectionsGroup.getBaseConnection(),
					path.getTargetQueryFragment(new Var("a"), new Var("c"),
							connectionsGroup.getRdfsSubClassOfReasoner()),
					false,
					null,
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
			);

			PlanNode invalidTargets = new GroupByFilter(joined, group -> {
				return group.stream().map(ValidationTuple::getValue).noneMatch(hasValueIn::contains);
			});

			return new Unique(new TrimToTarget(invalidTargets));

		} else if (scope == Scope.nodeShape) {

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
				addedTargets = target.extend(addedTargets, connectionsGroup, scope, EffectiveTarget.Extend.right,
						false);
			} else {
				addedTargets = target.getPlanNode(connectionsGroup, scope, false);
			}

			PlanNode falseNode = new ValueInFilter(addedTargets, hasValueIn)
					.getFalseNode(UnBufferedPlanNode.class);

			falseNode = new DebugPlanNode(falseNode, p -> {
				assert p != null;
			});

			return falseNode;

		} else {
			throw new UnsupportedOperationException("Unknown scope: " + scope);
		}

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated, Scope scope) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.getPlanNode(connectionsGroup, Scope.nodeShape, true);

			return new Unique(new ShiftToPropertyShape(allTargetsPlan));
		}
		return new EmptyNode();
	}
}
