package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.Exportable;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.TargetChainInterface;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;

public interface ConstraintComponent extends Exportable, TargetChainInterface {

	PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans);

	PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, PlanNodeProvider overrideTargetNode, boolean negatePlan,
			boolean negateChildren);

	ValidationApproach getPreferedValidationApproach();

	SourceConstraintComponent getConstraintComponent();
}
