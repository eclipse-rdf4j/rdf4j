package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.planNodes.DatatypeFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;

import java.util.Optional;
import java.util.Set;

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
														boolean logValidationPlans, PlanNodeProvider overrideTargetNode, boolean negatePlan, boolean negateChildren) {

		EffectiveTarget effectiveTarget = targetChain.getEffectiveTarget();

		Optional<Path> path = targetChain.getPath();
		if (path.isPresent()) {

			PlanNode addedTargets = effectiveTarget.getAdded(connectionsGroup);

			PlanNode invalidValuesDirectOnPath = path.get()
				.getAdded(connectionsGroup,
					planNode -> new DatatypeFilter(planNode, datatype)
						.getFalseNode(UnBufferedPlanNode.class));

			return new InnerJoin(addedTargets, invalidValuesDirectOnPath);
		} else {
			throw new UnsupportedOperationException();
		}


	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.DatatypeConstraintComponent;
	}
}
