package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Collections;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalPredicateObjectFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;

public class ClassConstraintComponent extends AbstractConstraintComponent {

	Resource clazz;

	public ClassConstraintComponent(Resource clazz) {
		this.clazz = clazz;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
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
			PlanNodeProvider overrideTargetNode, Scope scope) {

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
				addedTargets = target.getPlanNode(connectionsGroup, scope, false);
				PlanNode addedByPath = path.getAdded(connectionsGroup, null);

				addedByPath = target.getTargetFilter(connectionsGroup,
						new Unique(new TrimToTarget(addedByPath), false));

				addedByPath = target.extend(addedByPath, connectionsGroup, scope, EffectiveTarget.Extend.left, false);

				if (connectionsGroup.getStats().hasRemoved()) {
					PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
							clazz, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));

					deletedTypes = getTargetChain()
							.getEffectiveTarget("target_", Scope.nodeShape,
									connectionsGroup.getRdfsSubClassOfReasoner())
							.extend(deletedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left,
									false);

					deletedTypes = getTargetChain()
							.getEffectiveTarget("target_", Scope.nodeShape,
									connectionsGroup.getRdfsSubClassOfReasoner())
							.getTargetFilter(connectionsGroup, deletedTypes);

					addedTargets = new UnionNode(addedTargets,
							new TrimToTarget(new ShiftToPropertyShape(deletedTypes)));
				}

				addedTargets = new UnionNode(addedByPath, addedTargets);
				addedTargets = new Unique(addedTargets, false);
			}

			PlanNode joined = new BulkedExternalInnerJoin(
					addedTargets,
					connectionsGroup.getBaseConnection(),
					path.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
							connectionsGroup.getRdfsSubClassOfReasoner()),
					false,
					null,
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
			);

			RdfsSubClassOfReasoner rdfsSubClassOfReasoner = connectionsGroup.getRdfsSubClassOfReasoner();
			Set<Resource> clazzForwardChained = rdfsSubClassOfReasoner.backwardsChain(clazz);

			// filter by type against the base sail
			PlanNode falseNode = new ExternalPredicateObjectFilter(
					connectionsGroup.getBaseConnection(),
					RDF.TYPE, clazzForwardChained,
					joined, false, ExternalPredicateObjectFilter.FilterOn.value);

			return falseNode;

		} else if (scope == Scope.nodeShape) {

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
				addedTargets = target.extend(addedTargets, connectionsGroup, scope, EffectiveTarget.Extend.right,
						false);
			} else {
				addedTargets = target.getPlanNode(connectionsGroup, scope, false);

				if (connectionsGroup.getStats().hasRemoved()) {
					PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
							clazz, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope));
					deletedTypes = getTargetChain()
							.getEffectiveTarget("target_", scope, connectionsGroup.getRdfsSubClassOfReasoner())
							.getTargetFilter(connectionsGroup, deletedTypes);
					deletedTypes = getTargetChain()
							.getEffectiveTarget("target_", scope, connectionsGroup.getRdfsSubClassOfReasoner())
							.extend(deletedTypes, connectionsGroup, scope, EffectiveTarget.Extend.left, false);
					addedTargets = new UnionNode(addedTargets, new TrimToTarget(deletedTypes));
				}
			}

			// filter by type against the base sail
			PlanNode falseNode = new ExternalPredicateObjectFilter(
					connectionsGroup.getBaseConnection(),
					RDF.TYPE, Collections.singleton(clazz),
					addedTargets, false, ExternalPredicateObjectFilter.FilterOn.value);

			return falseNode;

		} else {
			throw new UnsupportedOperationException("Unknown scope: " + scope);
		}

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.getPlanNode(connectionsGroup, Scope.nodeShape, true);

			// removed type statements that match clazz could affect sh:or
			if (connectionsGroup.getStats().hasRemoved()) {
				PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
						clazz, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
				deletedTypes = getTargetChain()
						.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.getTargetFilter(connectionsGroup, deletedTypes);
				deletedTypes = getTargetChain()
						.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.extend(deletedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left, false);
				allTargetsPlan = new UnionNode(allTargetsPlan, deletedTypes);
			}

			// added type statements that match clazz could affect sh:not
			if (connectionsGroup.getStats().hasAdded()) {
				PlanNode addedTypes = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, RDF.TYPE,
						clazz, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
				addedTypes = getTargetChain()
						.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.getTargetFilter(connectionsGroup, addedTypes);
				addedTypes = getTargetChain()
						.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.extend(addedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left, false);
				allTargetsPlan = new UnionNode(allTargetsPlan, addedTypes);
			}

			return new Unique(new TrimToTarget(new ShiftToPropertyShape(allTargetsPlan)), false);
		}
		PlanNode allTargetsPlan = new EmptyNode();

		// removed type statements that match clazz could affect sh:or
		if (connectionsGroup.getStats().hasRemoved()) {
			PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE, clazz,
					UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
			deletedTypes = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.getTargetFilter(connectionsGroup, deletedTypes);
			deletedTypes = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.extend(deletedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left, false);
			allTargetsPlan = new UnionNode(allTargetsPlan, deletedTypes);

		}

		// added type statements that match clazz could affect sh:not
		if (connectionsGroup.getStats().hasAdded()) {
			PlanNode addedTypes = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, RDF.TYPE, clazz,
					UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
			addedTypes = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.getTargetFilter(connectionsGroup, addedTypes);
			addedTypes = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.extend(addedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left, false);
			allTargetsPlan = new UnionNode(allTargetsPlan, addedTypes);

		}

		return new Unique(allTargetsPlan, false);
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope) {
		return super.requiresEvaluation(connectionsGroup, scope)
				|| connectionsGroup.getRemovedStatements().hasStatement(null, RDF.TYPE, clazz, true)
				|| connectionsGroup.getAddedStatements().hasStatement(null, RDF.TYPE, clazz, true);
	}
}
