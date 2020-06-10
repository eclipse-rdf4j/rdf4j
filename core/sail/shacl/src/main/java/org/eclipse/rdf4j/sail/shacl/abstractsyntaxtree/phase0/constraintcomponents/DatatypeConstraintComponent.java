package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.TupleValidationPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.ValidationInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.DatatypeFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;

public class DatatypeConstraintComponent extends AbstractConstraintComponent {

	Resource datatype;

	public DatatypeConstraintComponent(Resource datatype) {
		this.datatype = datatype;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.DATATYPE, datatype);
	}

	@Override
	public TupleValidationPlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans) {
		return super.generateSparqlValidationPlan(connectionsGroup, logValidationPlans);
	}

	@Override
	public TupleValidationPlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans) {

		EffectiveTarget effectiveTarget = targetChain.getEffectiveTarget();

		Optional<Path> path = targetChain.getPath();
		if (path.isPresent()) {

			TupleValidationPlanNode addedTargets = effectiveTarget.getAdded(connectionsGroup);

			TupleValidationPlanNode invalidValuesDirectOnPath = path.get()
					.getAdded(connectionsGroup,
							planNode -> new DatatypeFilter(planNode, datatype)
									.getFalseNode(UnBufferedPlanNode.class));

			return new ValidationInnerJoin(addedTargets, invalidValuesDirectOnPath);
		} else {
			throw new UnsupportedOperationException();
		}

	}
}
