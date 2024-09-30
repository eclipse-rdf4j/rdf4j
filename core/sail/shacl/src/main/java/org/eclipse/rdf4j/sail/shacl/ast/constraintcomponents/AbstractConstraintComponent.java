/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ReduceTargets;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
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
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider,
			ValidationSettings validationSettings) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(Variable<Value> subject,
			Variable<Value> object, RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		throw new UnsupportedOperationException(this.getClass().getSimpleName());
	}

	static CharSequence[] trim(String... s) {
		for (int i = 0; i < s.length; i++) {
			s[i] = s[i].trim();
		}
		return s;
	}

	public String stringRepresentationOfValue(Value value) {
		if (value.isIRI()) {
			return "<" + value + ">";
		}
		if (value.isLiteral()) {
			IRI datatype = ((Literal) value).getDatatype();
			if (datatype == null) {
				return "\"" + value.stringValue()
						.replace("\\", "\\\\")
						.replace("\"", "\\\"")
						.replace("\n", "\\n")
						+ "\"";
			}
			if (((Literal) value).getLanguage().isPresent()) {
				return "\"" + value.stringValue()
						.replace("\\", "\\\\")
						.replace("\"", "\\\"")
						.replace("\n", "\\n")
						+ "\"@" + ((Literal) value).getLanguage().get();
			}
			return "\"" + value.stringValue()
					.replace("\\", "\\\\")
					.replace("\"", "\\\"")
					.replace("\n", "\\n")
					+ "\"^^<" + datatype.stringValue() + ">";
		}

		throw new IllegalStateException(value.getClass().getSimpleName());
	}

	static PlanNode getAllTargetsIncludingThoseAddedByPath(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, Scope scope, EffectiveTarget effectiveTarget, Path path,
			boolean includeTargetsAffectedByRemoval) {
		PlanNode allTargets;
		BufferedSplitter addedTargets = BufferedSplitter.getInstance(
				effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(),
						scope, includeTargetsAffectedByRemoval, null));

		PlanNode addedByPath = path.getAllAdded(connectionsGroup, validationSettings.getDataGraph(), null);

		addedByPath = Unique.getInstance(new TrimToTarget(addedByPath, connectionsGroup), false, connectionsGroup);

		addedByPath = new ReduceTargets(addedByPath, addedTargets.getPlanNode(), connectionsGroup);

		addedByPath = effectiveTarget.extend(addedByPath, connectionsGroup, validationSettings.getDataGraph(),
				scope, EffectiveTarget.Extend.left,
				false,
				null);

		allTargets = UnionNode.getInstance(connectionsGroup, addedTargets.getPlanNode(), addedByPath);

		allTargets = Unique.getInstance(new TrimToTarget(allTargets, connectionsGroup), false, connectionsGroup);

		return allTargets;
	}

}
