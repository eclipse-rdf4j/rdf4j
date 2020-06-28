package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.DatatypeFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.TargetChainPopper;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.EffectiveTarget;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class DatatypeConstraintComponent extends AbstractConstraintComponent {

	Resource datatype;

	public DatatypeConstraintComponent(Resource datatype) {
		this.datatype = datatype;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.DATATYPE, datatype);
	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans) {
		return super.generateSparqlValidationPlan(connectionsGroup, logValidationPlans);
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, PlanNodeProvider overrideTargetNode, boolean negatePlan,
			boolean negateChildren) {

		EffectiveTarget effectiveTarget = targetChain.getEffectiveTarget();
		Optional<Path> path = targetChain.getPath();

		Function<PlanNode, FilterPlanNode> filterAttacher = (parent) -> new DatatypeFilter(parent, datatype);

		if (overrideTargetNode != null) {

			PlanNode planNode;

			if (!path.isPresent()) {
				planNode = overrideTargetNode.getPlanNode();
				planNode = new TargetChainPopper(planNode);

			} else {
				planNode = new BulkedExternalInnerJoin(overrideTargetNode.getPlanNode(),
						connectionsGroup.getBaseConnection(),
						path.get().getQueryFragment(new Var("a"), new Var("c")), false, null,
						(b) -> new ValidationTuple(b.getValue("a"), path.get(), b.getValue("c")));
			}

			if (negatePlan) {
				return filterAttacher.apply(planNode).getTrueNode(UnBufferedPlanNode.class);
			} else {
				return filterAttacher.apply(planNode).getFalseNode(UnBufferedPlanNode.class);
			}

		}

		if (!path.isPresent()) {

			PlanNode targets = effectiveTarget.getAdded(connectionsGroup);
			targets = new TargetChainPopper(targets);

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
				effectiveTarget.getAdded(connectionsGroup),
				invalidValuesDirectOnPath);

		if (connectionsGroup.getStats().isBaseSailEmpty()) {
			return innerJoin.getJoined(UnBufferedPlanNode.class);

		} else {

			PlanNode top = innerJoin.getJoined(BufferedPlanNode.class);

			PlanNode discardedRight = innerJoin.getDiscardedRight(BufferedPlanNode.class);

			PlanNode typeFilterPlan = effectiveTarget.getTargetFilter(connectionsGroup, discardedRight);

			top = new UnionNode(top, typeFilterPlan);

			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(
					effectiveTarget.getAdded(connectionsGroup),
					connectionsGroup.getBaseConnection(), path.get().getQueryFragment(new Var("a"), new Var("c")), true,
					connectionsGroup.getPreviousStateConnection(),
					b -> new ValidationTuple(b.getValue("a"), path.get(), b.getValue("c")));

			top = new UnionNode(top, bulkedExternalInnerJoin);

			if (negatePlan) {
				return filterAttacher.apply(top).getTrueNode(UnBufferedPlanNode.class);
			} else {
				return filterAttacher.apply(top).getFalseNode(UnBufferedPlanNode.class);
			}

		}

	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.DatatypeConstraintComponent;
	}
}
