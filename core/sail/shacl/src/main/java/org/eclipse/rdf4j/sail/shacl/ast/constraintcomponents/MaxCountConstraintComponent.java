package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import static org.eclipse.rdf4j.model.util.Values.literal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.GroupByCountFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;

public class MaxCountConstraintComponent extends AbstractConstraintComponent {

	long maxCount;

	public MaxCountConstraintComponent(long maxCount) {
		this.maxCount = maxCount;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.MAX_COUNT, literal(BigInteger.valueOf(maxCount)));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MaxCountConstraintComponent;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget("_target", scope,
				connectionsGroup.getRdfsSubClassOfReasoner());
		Optional<Path> path = getTargetChain().getPath();

		PlanNode addedTargets = effectiveTarget.getPlanNode(connectionsGroup, scope, false);

		PlanNode addedByPath = path.get().getAdded(connectionsGroup, null);

		addedByPath = effectiveTarget.getTargetFilter(connectionsGroup,
				new Unique(new TrimToTarget(addedByPath), false));

		addedByPath = effectiveTarget.extend(addedByPath, connectionsGroup, scope, EffectiveTarget.Extend.left, false);

		PlanNode mergeNode = new UnionNode(addedTargets, addedByPath);

		if (overrideTargetNode != null) {
			mergeNode = effectiveTarget.extend(overrideTargetNode.getPlanNode(), connectionsGroup, scope,
					EffectiveTarget.Extend.right, false);
		}

		mergeNode = new Unique(new TrimToTarget(mergeNode), false);

		PlanNode relevantTargetsWithPath = new BulkedExternalInnerJoin(
				mergeNode,
				connectionsGroup.getBaseConnection(),
				getTargetChain().getPath()
						.get()
						.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
								connectionsGroup.getRdfsSubClassOfReasoner()),
				false,
				null,
				(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
		);

		PlanNode groupByCount = new GroupByCountFilter(relevantTargetsWithPath, count -> count > maxCount);

		return new Unique(new TrimToTarget(groupByCount), false);

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
	public ConstraintComponent deepClone() {
		return new MaxCountConstraintComponent(maxCount);
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, boolean negatePlan, boolean negateChildren, Scope scope) {

		String targetVarPrefix = "target_";

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(targetVarPrefix, scope,
				connectionsGroup.getRdfsSubClassOfReasoner());
		String query = effectiveTarget.getQuery(false);

		if (maxCount == 0) {
			StatementMatcher.Variable value = new StatementMatcher.Variable("value");

			String pathQuery = getTargetChain().getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner()))
					.orElseThrow(IllegalStateException::new);

			query += pathQuery;

		} else {

			StringBuilder paths = new StringBuilder();
			String valuePrefix = "value_";

			for (int i = 0; i < maxCount + 1; i++) {
				StatementMatcher.Variable value = new StatementMatcher.Variable(valuePrefix + i);

				String pathQuery = getTargetChain().getPath()
						.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
								connectionsGroup.getRdfsSubClassOfReasoner()))
						.orElseThrow(IllegalStateException::new);

				paths.append(pathQuery).append("\n");
			}

			List<String> collect = LongStream.range(0, maxCount + 1)
					.mapToObj(i -> "?" + valuePrefix + i)
					.collect(Collectors.toList());

			Set<String> notEquals = new HashSet<>();

			for (String left : collect) {
				for (String right : collect) {
					if (left == right) {
						continue;
					}
					if (left.compareTo(right) < 0) {
						notEquals.add(left + " != " + right);
					} else {
						notEquals.add(right + " != " + left);

					}
				}
			}

			String innerCondition = String.join(" && ", notEquals);

			query += paths + "FILTER(" + innerCondition + ")\n";
		}

		List<StatementMatcher.Variable> allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(query, allTargetVariables, null, scope, getConstraintComponent(), null, null);

	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		// performance of large maxCount is terrible
		if (maxCount > 5) {
			return ValidationApproach.Transactional;
		}
		return ValidationApproach.SPARQL;
	}
}
