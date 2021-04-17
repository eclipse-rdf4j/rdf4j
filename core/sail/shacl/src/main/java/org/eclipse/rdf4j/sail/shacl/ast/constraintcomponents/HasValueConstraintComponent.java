package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.GroupByFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValueInFilter;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;

public class HasValueConstraintComponent extends AbstractConstraintComponent {

	Value hasValue;

	public HasValueConstraintComponent(Value hasValue) {
		this.hasValue = hasValue;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.HAS_VALUE, hasValue);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.HasValueConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new HasValueConstraintComponent(hasValue);
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
				addedTargets = target.getPlanNode(connectionsGroup, scope, true);
				PlanNode addedByPath = path.getAdded(connectionsGroup, null);

				addedByPath = target.getTargetFilter(connectionsGroup,
						new Unique(new TrimToTarget(addedByPath), false));
				addedByPath = target.extend(addedByPath, connectionsGroup, scope, EffectiveTarget.Extend.left, false);

				addedTargets = new UnionNode(addedByPath, addedTargets);
				addedTargets = new Unique(addedTargets, false);
			}

			PlanNode joined = new BulkedExternalLeftOuterJoin(
					addedTargets,
					connectionsGroup.getBaseConnection(),
					path.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
							connectionsGroup.getRdfsSubClassOfReasoner()),
					false,
					null,
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
			);

			PlanNode invalidTargets = new GroupByFilter(joined, group -> {
				return group.stream().map(ValidationTuple::getValue).noneMatch(v -> hasValue.equals(v));
			});

			return new Unique(new TrimToTarget(invalidTargets), false);

		} else if (scope == Scope.nodeShape) {

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
				addedTargets = target.extend(addedTargets, connectionsGroup, scope, EffectiveTarget.Extend.right,
						false);
			} else {
				addedTargets = target.getPlanNode(connectionsGroup, scope, false);
			}

			PlanNode falseNode = new ValueInFilter(addedTargets, new HashSet<>(Collections.singletonList(hasValue)))
					.getFalseNode(UnBufferedPlanNode.class);

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

			return new Unique(new ShiftToPropertyShape(allTargetsPlan), true);
		}
		return new EmptyNode();
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {

		List<StatementMatcher> statementMatchers = Collections.emptyList();

		if (getTargetChain().getPath().isPresent()) {
			Path path = getTargetChain().getPath().get();

			statementMatchers = path
					.getStatementMatcher(subject, new StatementMatcher.Variable(hasValue), rdfsSubClassOfReasoner)
					.collect(Collectors.toList());
		}

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			if (hasValue.isIRI()) {
				return SparqlFragment.bgp("BIND(<" + hasValue + "> as ?" + object.getName() + ")\n"
						+ path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner), statementMatchers);
			}
			if (hasValue.isLiteral()) {
				return SparqlFragment.bgp("BIND(" + hasValue.toString() + " as ?" + object.getName() + ")\n"
						+ path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner), statementMatchers);
			}

			throw new UnsupportedOperationException(
					"value was unsupported type: " + hasValue.getClass().getSimpleName());

		} else {
			if (hasValue.isIRI()) {
				return SparqlFragment.filterCondition("?" + object.getName() + " = <" + hasValue + ">",
						statementMatchers);
			} else if (hasValue.isLiteral()) {
				return SparqlFragment.filterCondition("?" + object.getName() + " = " + hasValue, statementMatchers);
			}
			throw new UnsupportedOperationException(
					"value was unsupported type: " + hasValue.getClass().getSimpleName());

		}
	}

}
