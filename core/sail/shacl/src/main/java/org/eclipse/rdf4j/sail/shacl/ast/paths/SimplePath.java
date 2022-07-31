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

package org.eclipse.rdf4j.sail.shacl.ast.paths;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class SimplePath extends Path {

	IRI predicate;

	public SimplePath(IRI predicate) {
		super(predicate);
		this.predicate = predicate;
	}

	@Override
	public Resource getId() {
		return predicate;
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {
		PlanNode unorderedSelect = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, predicate, null,
				dataGraph, UnorderedSelect.Mapper.SubjectObjectPropertyShapeMapper.getFunction());

		if (planNodeWrapper != null) {
			unorderedSelect = planNodeWrapper.apply(unorderedSelect);
		}

		return connectionsGroup.getCachedNodeFor(unorderedSelect);
	}

	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public String toString() {
		return "SimplePath{ <" + predicate + "> }";
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
	}

	@Override
	public Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		return Stream.of(new StatementMatcher(subject, new StatementMatcher.Variable(predicate), object));
	}

	@Override
	public String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		return "?" + subject.getName() + " <" + predicate + "> ?" + object.getName() + " .";
	}
}
