package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Collections;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.DebugPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ExternalPredicateObjectFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.EffectiveTarget;

public class ClassConstraintComponent extends AbstractConstraintComponent {

	Resource clazz;

	public ClassConstraintComponent(Resource clazz) {
		this.clazz = clazz;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.CLASS, clazz);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.ClassConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new ClassConstraintComponent(clazz);
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, boolean negatePlan, boolean negateChildren, Scope scope) {

		EffectiveTarget target = getTargetChain().getEffectiveTarget("_target", scope);

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
				addedTargets = target.extend(addedTargets, connectionsGroup, scope, EffectiveTarget.Extend.right);

			} else {
				addedTargets = target.getPlanNode(connectionsGroup, scope, false);
				PlanNode addedByPath = path.getAdded(connectionsGroup, null);

				addedByPath = target.getTargetFilter(connectionsGroup, new Unique(new TrimToTarget(addedByPath)));

				addedTargets = new UnionNode(addedByPath, addedTargets);
				addedTargets = new Unique(addedTargets);
			}

			PlanNode joined = new BulkedExternalInnerJoin(
					addedTargets,
					connectionsGroup.getBaseConnection(),
					path.getTargetQueryFragment(new Var("a"), new Var("c")),
					false,
					null,
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
			);

			// filter by type against the base sail
			PlanNode falseNode = new ExternalPredicateObjectFilter(
					connectionsGroup.getBaseConnection(),
					RDF.TYPE, Collections.singleton(clazz),
					joined, false, ExternalPredicateObjectFilter.FilterOn.value);

			return falseNode;

		} else if (scope == Scope.nodeShape) {

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
				addedTargets = target.extend(addedTargets, connectionsGroup, scope, EffectiveTarget.Extend.right);
			} else {
				addedTargets = target.getPlanNode(connectionsGroup, scope, false);
			}

			// filter by type against the base sail
			PlanNode falseNode = new ExternalPredicateObjectFilter(
					connectionsGroup.getBaseConnection(),
					RDF.TYPE, Collections.singleton(clazz),
					addedTargets, false, ExternalPredicateObjectFilter.FilterOn.value);

			falseNode = new DebugPlanNode(falseNode, "", p -> {
				System.out.println(p);
			});

			return falseNode;

		} else {
			throw new UnsupportedOperationException("Unknown scope: " + scope);
		}

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated, Scope scope) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain().getEffectiveTarget("target_", Scope.nodeShape)
					.getPlanNode(connectionsGroup, Scope.nodeShape, true);

			if (connectionsGroup.getStats().hasRemoved()) {
				PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
						clazz, s -> new ValidationTuple(s.getSubject(), Scope.nodeShape, false));
				deletedTypes = getTargetChain().getEffectiveTarget("target_", Scope.nodeShape)
						.getTargetFilter(connectionsGroup, deletedTypes);
				deletedTypes = getTargetChain().getEffectiveTarget("target_", Scope.nodeShape)
						.extend(deletedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left);
				allTargetsPlan = new UnionNode(allTargetsPlan, deletedTypes);
			}

			return new Unique(new Sort(new ShiftToPropertyShape(allTargetsPlan)));
		}

		if (connectionsGroup.getStats().hasRemoved()) {
			PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE, clazz,
					s -> new ValidationTuple(s.getSubject(), Scope.nodeShape, false));
			deletedTypes = getTargetChain().getEffectiveTarget("target_", Scope.nodeShape)
					.getTargetFilter(connectionsGroup, deletedTypes);
			return getTargetChain().getEffectiveTarget("target_", Scope.nodeShape)
					.extend(deletedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left);
		}

		return new EmptyNode();
	}
}
