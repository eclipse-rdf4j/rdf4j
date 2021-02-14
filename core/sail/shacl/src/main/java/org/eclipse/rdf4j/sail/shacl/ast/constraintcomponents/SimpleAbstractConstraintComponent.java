package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleAbstractConstraintComponent extends AbstractConstraintComponent {

	private static final Logger logger = LoggerFactory.getLogger(SimpleAbstractConstraintComponent.class);

	private Resource id;
	TargetChain targetChain;

	public SimpleAbstractConstraintComponent(Resource id) {
		this.id = id;
	}

	public SimpleAbstractConstraintComponent() {

	}

	public Resource getId() {
		return id;
	}

	@Override
	public TargetChain getTargetChain() {
		return targetChain;
	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		this.targetChain = targetChain;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, PlanNodeProvider overrideTargetNode,
			Scope scope) {

		return generateTransactionalValidationPlan(
				connectionsGroup,
				overrideTargetNode,
				getFilterAttacher(),
				scope
		);

	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, boolean negatePlan, boolean negateChildren, Scope scope) {

		String targetVarPrefix = "target_";
		StatementMatcher.Variable value = new StatementMatcher.Variable("value");

		ComplexQueryFragment complexQueryFragment = getComplexQueryFragment(targetVarPrefix, value, negatePlan,
				connectionsGroup);

		String query = complexQueryFragment.getQuery();
		StatementMatcher.Variable targetVar = complexQueryFragment.getTargetVar();

		return new Select(connectionsGroup.getBaseConnection(), query, null, b -> {

			List<String> collect = b.getBindingNames()
					.stream()
					.filter(s -> s.startsWith(targetVarPrefix))
					.sorted()
					.collect(Collectors.toList());

			ValidationTuple validationTuple = new ValidationTuple(b, collect, scope, true);

//			if (targetChain.getPath().isPresent()) {
//				validationTuple.setPath(targetChain.getPath().get());
//				validationTuple.setValue(b.getValue(value.getName()));
//			} else {
//				validationTuple.setValue(b.getValue(targetVar.getName()));
//			}

			return validationTuple;

		});

	}

	private ComplexQueryFragment getComplexQueryFragment(String targetVarPrefix, StatementMatcher.Variable value,
			boolean negated,
			ConnectionsGroup connectionsGroup) {

		EffectiveTarget effectiveTarget = targetChain.getEffectiveTarget(targetVarPrefix, Scope.propertyShape,
				connectionsGroup.getRdfsSubClassOfReasoner());
		String query = effectiveTarget.getQuery(false);

		StatementMatcher.Variable targetVar = effectiveTarget.getTargetVar();

		Optional<String> pathQuery = targetChain.getPath()
				.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
						connectionsGroup.getRdfsSubClassOfReasoner()));

		if (pathQuery.isPresent()) {
			query += "\n" + pathQuery.get();
			query += "\n FILTER(" + getSparqlFilterExpression(value.getName(), negated) + ")";
		} else {
			query += "\n FILTER(" + getSparqlFilterExpression(targetVar.getName(), negated) + ")";
		}

		return new ComplexQueryFragment(query, targetVarPrefix, targetVar, value);

	}

	abstract String getSparqlFilterExpression(String varName, boolean negated);

	PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, PlanNodeProvider overrideTargetNode,
			Function<PlanNode, FilterPlanNode> filterAttacher,
			Scope scope) {

		boolean negatePlan = false;

		EffectiveTarget effectiveTarget = targetChain.getEffectiveTarget("target_", scope,
				connectionsGroup.getRdfsSubClassOfReasoner());
		Optional<Path> path = targetChain.getPath();

		if (overrideTargetNode != null) {

			PlanNode planNode;

			if (scope == Scope.nodeShape) {
				planNode = overrideTargetNode.getPlanNode();
				planNode = effectiveTarget.extend(planNode, connectionsGroup, scope, EffectiveTarget.Extend.right,
						false);

			} else {
				PlanNode temp = overrideTargetNode.getPlanNode();

				temp = effectiveTarget.extend(temp, connectionsGroup, scope, EffectiveTarget.Extend.right, false);

				planNode = new BulkedExternalInnerJoin(temp,
						connectionsGroup.getBaseConnection(),
						path.get()
								.getTargetQueryFragment(new StatementMatcher.Variable("a"),
										new StatementMatcher.Variable("c"),
										connectionsGroup.getRdfsSubClassOfReasoner()),
						false, null,
						(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true));
			}

			if (negatePlan) {
				return filterAttacher.apply(planNode).getTrueNode(UnBufferedPlanNode.class);
			} else {

				return filterAttacher.apply(planNode).getFalseNode(UnBufferedPlanNode.class);
			}

		}

		if (scope == Scope.nodeShape) {

			PlanNode targets = effectiveTarget.getPlanNode(connectionsGroup, scope, false);

			if (negatePlan) {
				return filterAttacher.apply(targets).getTrueNode(UnBufferedPlanNode.class);
			} else {
				return filterAttacher.apply(targets).getFalseNode(UnBufferedPlanNode.class);
			}

		}

		PlanNode invalidValuesDirectOnPath;

		if (negatePlan) {
			invalidValuesDirectOnPath = path.get()
					.getAdded(connectionsGroup,
							planNode -> filterAttacher.apply(planNode).getTrueNode(UnBufferedPlanNode.class));
		} else {
			invalidValuesDirectOnPath = path.get()
					.getAdded(connectionsGroup,
							planNode -> filterAttacher.apply(planNode).getFalseNode(UnBufferedPlanNode.class));
		}

		InnerJoin innerJoin = new InnerJoin(
				effectiveTarget.getPlanNode(connectionsGroup, scope, false),
				invalidValuesDirectOnPath);

		if (connectionsGroup.getStats().isBaseSailEmpty()) {
			return innerJoin.getJoined(UnBufferedPlanNode.class);

		} else {

			PlanNode top = innerJoin.getJoined(BufferedPlanNode.class);

			PlanNode discardedRight = innerJoin.getDiscardedRight(BufferedPlanNode.class);

			PlanNode typeFilterPlan = effectiveTarget.getTargetFilter(connectionsGroup, discardedRight);

			typeFilterPlan = effectiveTarget.extend(typeFilterPlan, connectionsGroup, scope,
					EffectiveTarget.Extend.left, true);

			top = new UnionNode(top, typeFilterPlan);

			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(
					effectiveTarget.getPlanNode(connectionsGroup, scope, false),
					connectionsGroup.getBaseConnection(),
					path.get()
							.getTargetQueryFragment(new StatementMatcher.Variable("a"),
									new StatementMatcher.Variable("c"),
									connectionsGroup.getRdfsSubClassOfReasoner()),
					true,
					connectionsGroup.getPreviousStateConnection(),
					b -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true));

			top = new UnionNode(top, bulkedExternalInnerJoin);

			if (negatePlan) {
				return filterAttacher.apply(top).getTrueNode(UnBufferedPlanNode.class);
			} else {
				return filterAttacher.apply(top).getFalseNode(UnBufferedPlanNode.class);
			}

		}
	}

	@Override
	public ValidationApproach getPreferedValidationApproach() {
		return ValidationApproach.Transactional;
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		throw new ShaclUnsupportedException(this.getClass().getSimpleName());
	}

	abstract Function<PlanNode, FilterPlanNode> getFilterAttacher();

	String literalToString(Literal literal) {
		IRI datatype = (literal).getDatatype();
		if (datatype == null) {
			return "\"" + literal.stringValue() + "\"";
		}
		if ((literal).getLanguage().isPresent()) {
			return "\"" + literal.stringValue() + "\"@" + (literal).getLanguage().get();
		}
		return "\"" + literal.stringValue() + "\"^^<" + datatype.stringValue() + ">";

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

}
