/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Stats;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;

/**
 * rsx:targetShape
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class TargetShape extends NodeShape {

	NodeShape targetShape;
	List<StatementPattern> statementPatternList;
	String query;

	TargetShape(Resource id, ShaclSail shaclSail, SailRepositoryConnection connection, boolean deactivated,
			Resource targetShape) {
		super(id, shaclSail, connection, deactivated);

		if (connection.hasStatement(targetShape, SHACL.PATH, null, false)) {
			throw new UnsupportedOperationException(
					"Experimental rsx:targetShape support only supports sh:NodeShape and not sh:PropertyShape");
		}

		this.targetShape = new NodeShape(targetShape, shaclSail, connection, false);
		statementPatternList = this.targetShape.getStatementPatterns().collect(Collectors.toList());
		query = this.targetShape.buildSparqlValidNodes("?a");

		assert !statementPatternList.isEmpty();
	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;

		PlanNode select = new Select(connectionsGroup.getBaseConnection(), getQuery("?a", null, null), "?a");

		return connectionsGroup.getCachedNodeFor(select);

	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		assert planeNodeWrapper == null;

		return getInnerPlanRemovedOrAdded(connectionsGroup, connectionsGroup.getAddedStatements());

	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		assert planeNodeWrapper == null;

		return getInnerPlanRemovedOrAdded(connectionsGroup, connectionsGroup.getRemovedStatements());

	}

	private PlanNode getInnerPlanRemovedOrAdded(ConnectionsGroup connectionsGroup, SailConnection removedStatements) {

		// @formatter:off
		/*
		 * General approach here is to:
		 *
		 * 1. Get all subjects that match any of the statement patterns of the filter shape
		 * 2. Feed this in as a bind into the SPARQL query generated from the filter shape
		 */
		// @formatter:on

		PlanNode allPotentialTargetsFromTransaction = new EmptyNode();
		for (StatementPattern statementPattern : statementPatternList) {

			PlanNode statementsThatMatchPattern = new TrimTuple(
					new UnorderedSelect(
							removedStatements,
							null,
							((IRI) statementPattern.getPredicateVar().getValue()),
							(statementPattern.getObjectVar().getValue()),
							UnorderedSelect.OutputPattern.SubjectPredicateObject),
					0,
					1);

			allPotentialTargetsFromTransaction = new UnionNode(allPotentialTargetsFromTransaction,
					statementsThatMatchPattern);

		}

		allPotentialTargetsFromTransaction = new Unique(new Sort(allPotentialTargetsFromTransaction));

		// TODO: The bulked external inner join could actually just be a bulked external sparql filter
		PlanNode allTargetsThatReallyAreTargets = new BulkedExternalInnerJoin(allPotentialTargetsFromTransaction,
				connectionsGroup.getBaseConnection(), query, false, null,
				"?a");

		allTargetsThatReallyAreTargets = new Unique(new TrimTuple(allTargetsThatReallyAreTargets, 0, 1));

		return allTargetsThatReallyAreTargets;
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements, Stats stats) {
		return !stats.isEmpty();
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		return targetShape.buildSparqlValidNodes(subjectVariable);

	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), parent, 0,
				getQuery("?a", null, null), "?a")
						.getTrueNode(UnBufferedPlanNode.class);
	}

	@Override
	public String toString() {
		return "TargetShape{" +
				"targetShape=" + targetShape +
				", id=" + id +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		TargetShape that = (TargetShape) o;
		return Objects.equals(targetShape, that.targetShape);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), targetShape);
	}
}
