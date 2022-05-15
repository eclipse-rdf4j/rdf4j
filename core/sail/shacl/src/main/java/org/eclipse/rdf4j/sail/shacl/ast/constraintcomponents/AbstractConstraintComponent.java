/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
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
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {
		logger.error("SPARQL based validation for {} has not been implemented", getConstraintComponent());
		throw new ShaclUnsupportedException();
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, PlanNodeProvider overrideTargetNode, Scope scope) {
		logger.error("Transactional validation for {} has not been implemented", getConstraintComponent());
		return EmptyNode.getInstance();
	}

	@Override
	public ValidationApproach getPreferredValidationApproach(ConnectionsGroup connectionsGroup) {
		return ValidationApproach.Transactional;
	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.Transactional;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		return getTargetChain()
				.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
				.couldMatch(connectionsGroup, dataGraph);
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object, RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		throw new UnsupportedOperationException(this.getClass().getSimpleName());
	}

}
