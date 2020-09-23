package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.DebugPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetChain;
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
			boolean logValidationPlans, PlanNodeProvider overrideTargetNode, boolean negatePlan,
			boolean negateChildren, Scope scope) {

		return generateTransactionalValidationPlan(
				connectionsGroup,
				overrideTargetNode,
				negatePlan,
				negateChildren,
				getFilterAttacher(),
				scope
		);

	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, boolean negatePlan, boolean negateChildren, Scope scope) {

		String targetVarPrefix = "target_";
		Var value = new Var("value");

		ComplexQueryFragment complexQueryFragment = getComplexQueryFragment(targetVarPrefix, value, negatePlan);

		String query = complexQueryFragment.getQuery();
		Var targetVar = complexQueryFragment.getTargetVar();

		return new Select(connectionsGroup.getBaseConnection(), query, b -> {

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

		}, null);

	}

	private ComplexQueryFragment getComplexQueryFragment(String targetVarPrefix, Var value, boolean negated) {

		EffectiveTarget effectiveTarget = targetChain.getEffectiveTarget(targetVarPrefix, Scope.propertyShape);
		String query = effectiveTarget.getQuery();

		Var targetVar = effectiveTarget.getTargetVar();

		Optional<String> pathQuery = targetChain.getPath()
				.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value));

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
			boolean negatePlan, boolean negateChildren, Function<PlanNode, FilterPlanNode> filterAttacher,
			Scope scope) {
		assert !negateChildren : "Node does not have children";

		EffectiveTarget effectiveTarget = targetChain.getEffectiveTarget("target_", scope);
		Optional<Path> path = targetChain.getPath();

		if (overrideTargetNode != null) {

			PlanNode planNode;

			if (scope == Scope.nodeShape) {
				planNode = overrideTargetNode.getPlanNode();
				planNode = effectiveTarget.extend(planNode, connectionsGroup, scope, EffectiveTarget.Extend.right);

			} else {
				PlanNode temp = new DebugPlanNode(overrideTargetNode.getPlanNode(),
						"SimpleAbstractConstraintComponent");

				temp = effectiveTarget.extend(temp, connectionsGroup, scope, EffectiveTarget.Extend.right);

				planNode = new BulkedExternalInnerJoin(temp,
						connectionsGroup.getBaseConnection(),
						path.get().getTargetQueryFragment(new Var("a"), new Var("c")), false, null,
						(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true));
			}

			planNode = new DebugPlanNode(planNode, "", p -> {
				System.out.println(p);
			});

			if (negatePlan) {
				return filterAttacher.apply(planNode).getTrueNode(UnBufferedPlanNode.class);
			} else {

				PlanNode falseNode = filterAttacher.apply(planNode).getFalseNode(UnBufferedPlanNode.class);
				falseNode = new DebugPlanNode(falseNode, "", p -> {
					System.out.println();
				});
				return falseNode;
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

			top = new UnionNode(top, typeFilterPlan);

			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(
					effectiveTarget.getPlanNode(connectionsGroup, scope, false),
					connectionsGroup.getBaseConnection(), path.get().getTargetQueryFragment(new Var("a"), new Var("c")),
					true,
					connectionsGroup.getPreviousStateConnection(),
					b -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true));

			bulkedExternalInnerJoin = new DebugPlanNode(bulkedExternalInnerJoin, "", p -> {
//				System.out.println(p);
			});

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
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated, Scope scope) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain().getEffectiveTarget("target_", Scope.nodeShape)
					.getPlanNode(connectionsGroup, Scope.nodeShape, true);

			return new Unique(new Sort(new ShiftToPropertyShape(allTargetsPlan)));
		}
		return new EmptyNode();
	}

}
