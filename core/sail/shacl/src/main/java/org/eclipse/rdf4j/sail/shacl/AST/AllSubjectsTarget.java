/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
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
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalFilterIsSubject;
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
 * sh:targetObjectsOf
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class AllSubjectsTarget extends NodeShape {

	NodeShape filterShape;
	List<StatementPattern> statementPatternList;
	String query;

	AllSubjectsTarget(Resource id, ShaclSail shaclSail, SailRepositoryConnection connection, boolean deactivated,
			Resource filterShape) {
		super(id, shaclSail, connection, deactivated);
		if (shaclSail.isExperimentalFilterShapeSupport() && filterShape != null) {
			this.filterShape = new NodeShape(filterShape, shaclSail, connection, false);
			statementPatternList = this.filterShape.getStatementPatterns().collect(Collectors.toList());
			query = this.filterShape.buildSparqlValidNodes("?a");

		}

	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;

		if (filterShape != null) {

			PlanNode select = new Select(connectionsGroup.getBaseConnection(), getQuery("?a", null, null), "?a");

			return connectionsGroup.getCachedNodeFor(select);
		} else {
			PlanNode select = new Unique(
					new Sort(
							new TrimTuple(
									new UnorderedSelect(
											connectionsGroup.getBaseConnection(),
											null,
											null,
											null,
											UnorderedSelect.OutputPattern.SubjectPredicateObject),
									0,
									1)));

			return connectionsGroup.getCachedNodeFor(select);
		}
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {

		if (filterShape != null) {

			/*
			 * General approach here is to: 2. Get all subjects that match any of the statements patterns of the filter
			 * shape 3. Feed this in as a bind into the SPARQL query generated from the filter shape
			 */

			PlanNode planNode = new EmptyNode();
			for (StatementPattern statementPattern : statementPatternList) {
				planNode = new UnionNode(planNode, new Sort(new TrimTuple(new UnorderedSelect(
						connectionsGroup.getAddedStatements(),
						null,
						((IRI) statementPattern.getPredicateVar().getValue()),
						(statementPattern.getObjectVar().getValue()),
						UnorderedSelect.OutputPattern.SubjectPredicateObject

				), 0, 1)));
			}

			planNode = new Unique(new Sort(planNode));

			planNode = new BulkedExternalInnerJoin(planNode, connectionsGroup.getBaseConnection(), query, false, null,
					"?a");

			planNode = new Unique(new TrimTuple(planNode, 0, 1));

			return planNode;

		} else {
			PlanNode select = new Unique(
					new Sort(
							new TrimTuple(
									new UnorderedSelect(
											connectionsGroup.getAddedStatements(),
											null,
											null,
											null,
											UnorderedSelect.OutputPattern.SubjectPredicateObject),
									0,
									1)));

			return connectionsGroup.getCachedNodeFor(select);
		}

	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		if (filterShape != null) {

			PlanNode planNode = new EmptyNode();
			for (StatementPattern statementPattern : statementPatternList) {
				planNode = new UnionNode(planNode, new Sort(new TrimTuple(new UnorderedSelect(
						connectionsGroup.getRemovedStatements(),
						null,
						((IRI) statementPattern.getPredicateVar().getValue()),
						(statementPattern.getObjectVar().getValue()),
						UnorderedSelect.OutputPattern.SubjectPredicateObject

				), 0, 1)));
			}

			planNode = new Unique(new Sort(planNode));

			planNode = new BulkedExternalInnerJoin(planNode, connectionsGroup.getBaseConnection(), query, false, null,
					"?a");

			planNode = new Unique(new TrimTuple(planNode, 0, 1));

			return planNode;

		} else {
			PlanNode select = new Unique(
					new Sort(
							new TrimTuple(
									new UnorderedSelect(
											connectionsGroup.getRemovedStatements(),
											null,
											null,
											null,
											UnorderedSelect.OutputPattern.SubjectPredicateObject),
									0,
									1)));

			return connectionsGroup.getCachedNodeFor(select);
		}
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements, Stats stats) {
		if (stats.isEmpty()) {
			return false;
		}

		return true;
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		if (filterShape != null) {
			return filterShape.buildSparqlValidNodes(subjectVariable);
		} else {
			return subjectVariable + " ?allSubjectsTarget" + UUID.randomUUID().toString().replace("-", "") + " "
					+ objectVariable + " .";
		}
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		if (filterShape != null) {
			return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), parent, 0,
					getQuery("?a", null, null), "?a")
							.getTrueNode(UnBufferedPlanNode.class);
		} else {
			return new ExternalFilterIsSubject(connectionsGroup.getBaseConnection(), parent, 0)
					.getTrueNode(UnBufferedPlanNode.class);
		}

	}

	@Override
	public String toString() {
		return "AllSubjectsTarget{" +
				"id=" + id +
				'}';
	}
}
