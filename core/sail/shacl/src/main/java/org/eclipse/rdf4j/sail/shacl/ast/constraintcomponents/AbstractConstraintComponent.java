package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConstraintComponent implements ConstraintComponent {

	private static final Logger logger = LoggerFactory.getLogger(AbstractConstraintComponent.class);

	public static final String VALUES_INJECTION_POINT = "#VALUES_INJECTION_POINT#";

	private Resource id;
	private TargetChain targetChain;

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
			boolean logValidationPlans, PlanNodeProvider overrideTargetNode,
			Scope scope) {
		logger.warn("Transactional validation for {} has not been implemented", getConstraintComponent());
		return new EmptyNode();
	}

	@Override
	public ValidationApproach getPreferedValidationApproach() {
		return ValidationApproach.Transactional;
	}

	@Override
	public Set<ValidationApproach> getSupportedValidationApproaches() {
		return new HashSet<>(Collections.singletonList(ValidationApproach.Transactional));
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope) {
		return getTargetChain().getEffectiveTarget("_target", scope, connectionsGroup.getRdfsSubClassOfReasoner())
				.couldMatch(connectionsGroup);
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {
		throw new UnsupportedOperationException(this.getClass().getSimpleName());
	}

	static String randomSparqlVariable() {
		return "?" + UUID.randomUUID().toString().replace("-", "");
	}

}
