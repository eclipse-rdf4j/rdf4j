package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConstraintComponent implements ConstraintComponent {

	private static final Logger logger = LoggerFactory.getLogger(AbstractConstraintComponent.class);

	private Resource id;
	TargetChain targetChain;

	public AbstractConstraintComponent(Resource id) {
		this.id = id;
	}

	public AbstractConstraintComponent() {

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
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, boolean negatePlan, boolean negateChildren, Scope scope) {
		logger.warn("SPARQL based validation for {} has not been implemented", getConstraintComponent());
		return new EmptyNode();
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, PlanNodeProvider overrideTargetNode, boolean negatePlan,
			boolean negateChildren, Scope scope) {
		logger.warn("Transactional validation for {} has not been implemented", getConstraintComponent());
		return new EmptyNode();
	}

	@Override
	public ValidationApproach getPreferedValidationApproach() {
		return ValidationApproach.Transactional;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated) {
		throw new UnsupportedOperationException();
	}
}
