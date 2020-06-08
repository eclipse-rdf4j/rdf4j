package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets.Target;
import org.eclipse.rdf4j.sail.shacl.planNodes.DatatypeFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
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
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans) {
		return super.generateSparqlValidationPlan(connectionsGroup, logValidationPlans);
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans) {

		if (targetChain.getChain().size() == 2) {

			List<Object> chain = targetChain.getChain();
			Target target = (Target) chain.get(0);
			Path path = (Path) chain.get(1);

			PlanNode addedTargets = target.getAdded(connectionsGroup);

			PlanNode invalidValuesDirectOnPath = path.getAdded(connectionsGroup,
					planNode -> new DatatypeFilter(planNode, datatype).getFalseNode(UnBufferedPlanNode.class));

			return new InnerJoin(addedTargets, invalidValuesDirectOnPath).getJoined(UnBufferedPlanNode.class);
		}

		throw new UnsupportedOperationException();

	}
}
