package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
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
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans) {
		logger.warn("SPARQL based calidation for {} has not been implemented", getConstraintComponent());
		return new EmptyNode();
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans) {
		logger.warn("Transactional validation for {} has not been implemented", getConstraintComponent());
		return new EmptyNode();
	}

	protected IRI getConstraintComponent() {
		return SimpleValueFactory.getInstance().createIRI("http://TODO/" + this.getClass().getSimpleName());
	}

	@Override
	public ValidationApproach getPreferedValidationApproach() {
		return ValidationApproach.Transactional;
	}

}
