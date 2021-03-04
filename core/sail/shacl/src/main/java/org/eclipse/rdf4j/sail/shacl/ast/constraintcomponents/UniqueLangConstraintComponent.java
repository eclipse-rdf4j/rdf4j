package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.NonUniqueTargetLang;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;

public class UniqueLangConstraintComponent extends AbstractConstraintComponent {

	public UniqueLangConstraintComponent() {
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.UNIQUE_LANG, BooleanLiteral.TRUE);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.UniqueLangConstraintComponent;
	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			boolean negatePlan, boolean negateChildren, Scope scope) {
		assert !negateChildren : "There are no subplans!";
		assert !negatePlan;

		if (!getTargetChain().getPath().isPresent()) {
			throw new IllegalStateException("UniqueLang only operates on paths");
		}

		String targetVarPrefix = "target_";

		ComplexQueryFragment complexQueryFragment = getComplexQueryFragment(targetVarPrefix, connectionsGroup);

		String query = complexQueryFragment.getQuery();

		return new Select(connectionsGroup.getBaseConnection(), query, null, b -> {

			List<String> targetVars = b.getBindingNames()
					.stream()
					.filter(s -> s.startsWith(targetVarPrefix))
					.sorted()
					.collect(Collectors.toList());

			ValidationTuple validationTuple = new ValidationTuple(b, targetVars, scope, false);

			return validationTuple;

		});

	}

	private ComplexQueryFragment getComplexQueryFragment(String targetVarPrefix, ConnectionsGroup connectionsGroup) {

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(targetVarPrefix, Scope.propertyShape,
				connectionsGroup.getRdfsSubClassOfReasoner());
		String query = effectiveTarget.getQuery(false);

		StatementMatcher.Variable targetVar = effectiveTarget.getTargetVar();

		String pathQuery1 = getTargetChain().getPath()
				.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(),
						new StatementMatcher.Variable("value1"),
						connectionsGroup.getRdfsSubClassOfReasoner()))
				.get();

		String pathQuery2 = getTargetChain().getPath()
				.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(),
						new StatementMatcher.Variable("value2"),
						connectionsGroup.getRdfsSubClassOfReasoner()))
				.get();

		query += "\n FILTER(EXISTS{" +
				"\n" + query +
				"\n" + pathQuery1 +
				"\n" + pathQuery2 +
				"FILTER(?value1 != ?value2 && lang(?value1) = lang(?value2) && lang(?value1) != \"\")" +
				"} )";

		return new ComplexQueryFragment(query, targetVarPrefix, targetVar, null);

	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, Scope scope) {
//		assert !negateChildren : "There are no subplans!";
//		assert !negatePlan;

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget("target_", Scope.propertyShape,
				connectionsGroup.getRdfsSubClassOfReasoner());
		Optional<Path> path = getTargetChain().getPath();

		if (!path.isPresent() || scope != Scope.propertyShape) {
			throw new IllegalStateException("UniqueLang only operates on paths");
		}

		if (overrideTargetNode != null) {

			PlanNode targets = effectiveTarget.extend(overrideTargetNode.getPlanNode(), connectionsGroup, scope,
					EffectiveTarget.Extend.right, false);

			PlanNode relevantTargetsWithPath = new BulkedExternalInnerJoin(
					targets,
					connectionsGroup.getBaseConnection(),
					path.get()
							.getTargetQueryFragment(new StatementMatcher.Variable("a"),
									new StatementMatcher.Variable("c"),
									connectionsGroup.getRdfsSubClassOfReasoner()),
					false,
					null,
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
			);

			PlanNode nonUniqueTargetLang = new NonUniqueTargetLang(relevantTargetsWithPath);
			return new Unique(new TrimToTarget(nonUniqueTargetLang), false);
		}

		if (connectionsGroup.getStats().isBaseSailEmpty()) {
			PlanNode addedTargets = effectiveTarget.getPlanNode(connectionsGroup, scope, false);

			PlanNode addedByPath = path.get().getAdded(connectionsGroup, null);

			PlanNode innerJoin = new InnerJoin(addedTargets, addedByPath).getJoined(UnBufferedPlanNode.class);

			PlanNode nonUniqueTargetLang = new NonUniqueTargetLang(innerJoin);
			return new Unique(new TrimToTarget(nonUniqueTargetLang), false);
		}

		PlanNode addedTargets = effectiveTarget.getPlanNode(connectionsGroup, scope, false);

		PlanNode addedByPath = path.get().getAdded(connectionsGroup, null);

		addedByPath = effectiveTarget.getTargetFilter(connectionsGroup,
				new Unique(new TrimToTarget(addedByPath), false));

		addedByPath = effectiveTarget.extend(addedByPath, connectionsGroup, scope, EffectiveTarget.Extend.left, false);

		PlanNode mergeNode = new UnionNode(addedTargets, addedByPath);

		mergeNode = new TrimToTarget(mergeNode);

		PlanNode allRelevantTargets = new Unique(mergeNode, false);

		PlanNode relevantTargetsWithPath = new BulkedExternalInnerJoin(
				allRelevantTargets,
				connectionsGroup.getBaseConnection(),
				path.get()
						.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
								connectionsGroup.getRdfsSubClassOfReasoner()),
				false,
				null,
				(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
		);

		PlanNode nonUniqueTargetLang = new NonUniqueTargetLang(relevantTargetsWithPath);

		return new Unique(new TrimToTarget(nonUniqueTargetLang), false);

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
		return new UniqueLangConstraintComponent();
	}
}
