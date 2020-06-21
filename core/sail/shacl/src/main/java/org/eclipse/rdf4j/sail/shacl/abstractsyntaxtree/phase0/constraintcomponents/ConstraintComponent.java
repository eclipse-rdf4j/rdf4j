package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.Exportable;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.TargetChainInterface;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.TupleValidationPlanNode;

public interface ConstraintComponent extends Exportable, TargetChainInterface {

	TupleValidationPlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans);

	TupleValidationPlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans);

	ValidationApproach getPreferedValidationApproach();

	SourceConstraintComponent getConstraintComponent();
}
