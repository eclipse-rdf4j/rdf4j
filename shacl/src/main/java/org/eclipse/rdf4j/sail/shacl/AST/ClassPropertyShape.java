/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalTypeFilterNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.ModifyTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.TupleLengthFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Håvard Ottestad
 */
public class ClassPropertyShape extends PathPropertyShape {

	private final Resource classResource;
	private static final Logger logger = LoggerFactory.getLogger(ClassPropertyShape.class);

	ClassPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path,
			Resource classResource) {
		super(id, connection, nodeShape, deactivated, parent, path);
		this.classResource = classResource;
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		if (deactivated) {
			return null;
		}
		assert !negateSubPlans : "There are no subplans!";

		if (negateThisPlan) {
			PlanNode negatedPlan = getNegatedPlan(shaclSailConnection, overrideTargetNode);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(negatedPlan,
						shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(negatedPlan, this);
		}

		SailConnection addedStatements = shaclSailConnection.getAddedStatements();

		if (overrideTargetNode != null) {
			PlanNode planNode;

			if (getPath() == null) {
				planNode = new ModifyTuple(overrideTargetNode.getPlanNode(), t -> {
					t.line.add(t.line.get(0));
					return t;
				});

			} else {
				planNode = new BulkedExternalInnerJoin(overrideTargetNode.getPlanNode(), shaclSailConnection,
						getPath().getQuery("?a", "?c", null), false, "?a", "?c");
			}

			// filter by type against addedStatements, this is an optimization for when you add the type statement in
			// the same transaction
			PlanNode addedStatementsTypeFilter = new ExternalTypeFilterNode(addedStatements,
					Collections.singleton(classResource), planNode, 1, false);

			// filter by type against the base sail
			PlanNode invalidTuplesDueToDataAddedThatMatchesTargetOrPath = new ExternalTypeFilterNode(
					shaclSailConnection, Collections.singleton(classResource),
					addedStatementsTypeFilter, 1, false);
			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(invalidTuplesDueToDataAddedThatMatchesTargetOrPath,
						shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}
			return new EnrichWithShape(invalidTuplesDueToDataAddedThatMatchesTargetOrPath, this);
		}

		if (getPath() != null && shaclSailConnection.stats.isBaseSailEmpty()) {

			String query = nodeShape.getQuery("?a", "?b", null);
			String query1 = getPath().getQuery("?a", "?d", null);

			String negationQuery = query + "\n" + query1 + "\n FILTER(NOT EXISTS{?d a <" + classResource + ">})\n";

			PlanNode select = new Select(shaclSailConnection.getAddedStatements(), negationQuery, "?a", "?d");
			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(select, shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}
			return new EnrichWithShape(select, this);

		}

		if (getPath() == null) {
			PlanNode targets = new ModifyTuple(
					nodeShape.getPlanAddedStatements(shaclSailConnection, null),
					t -> {
						t.line.add(t.line.get(0));
						return t;
					});

			// filter by type against addedStatements, this is an optimization for when you add the type statement
			// in
			// the same transaction
			PlanNode filteredAgainstAdded = new ExternalTypeFilterNode(addedStatements,
					Collections.singleton(classResource), targets, 1,
					false);

			// filter by type against the base sail
			PlanNode filteredAgainsteBaseSail = new ExternalTypeFilterNode(shaclSailConnection,
					Collections.singleton(classResource),
					filteredAgainstAdded, 1, false);

			if (shaclSailConnection.stats.hasRemoved()) {

				// Handle when a type statement has been removed, first get all removed type statements that match
				// the
				// classResource for this shape
				PlanNode removedTypeStatements = new Select(shaclSailConnection.getRemovedStatements(),
						"?a a <" + classResource + ">", "?a");

				// Build a query to run against the base sail. eg:
				// ?c foaf:knows ?a.
				// ?c a foaf:Person.
				String query = nodeShape.getQuery("?a", "?q", shaclSailConnection.getRdfsSubClassOfReasoner());

				// do bulked external join for the removed class statements again the query above.
				// Essentially gets data that is now invalid because of the removed type statement
				PlanNode invalidDataDueToRemovedTypeStatement = new Unique(new TrimTuple(
						new BulkedExternalInnerJoin(removedTypeStatements, shaclSailConnection, query, false, "?a",
								"?c"),
						0, 1));

				filteredAgainsteBaseSail = new UnionNode(filteredAgainsteBaseSail,
						invalidDataDueToRemovedTypeStatement);
			}

			return new EnrichWithShape(filteredAgainsteBaseSail, this);
		}

		PlanNode addedByPath = getPlanAddedStatements(shaclSailConnection, null);

		// join all added by type and path
		InnerJoin innerJoinHolder = new InnerJoin(
				nodeShape.getPlanAddedStatements(shaclSailConnection, null),
				addedByPath);
		PlanNode innerJoin = innerJoinHolder.getJoined(BufferedPlanNode.class);
		PlanNode discardedRight = innerJoinHolder.getDiscardedRight(BufferedPlanNode.class);

		PlanNode typeFilterPlan = nodeShape.getTargetFilter(shaclSailConnection, discardedRight);

		innerJoin = new Unique(new UnionNode(innerJoin, typeFilterPlan));

		// also add anything that matches the path from the previousConnection, eg. if you add ":peter a
		// foaf:Person", and ":peter foaf:knows :steve" is already added
		PlanNode bulkedExternalLeftOuter = new BulkedExternalLeftOuterJoin(
				nodeShape.getPlanAddedStatements(shaclSailConnection, null),
				shaclSailConnection, getPath().getQuery("?a", "?c", null), true, "?a", "?c");

		// only get tuples that came from the first or the innerJoin or bulkedExternalLeftOuter,
		// we don't care if you added ":peter a foaf:Person" and nothing else and there is nothing else in the
		// underlying sail
		PlanNode joined = new TupleLengthFilter(new UnionNode(innerJoin, bulkedExternalLeftOuter), 2, false)
				.getTrueNode(UnBufferedPlanNode.class);

		// filter by type against addedStatements, this is an optimization for when you add the type statement in
		// the same transaction
		PlanNode addedStatementsTypeFilter = new ExternalTypeFilterNode(addedStatements,
				Collections.singleton(classResource), joined, 1, false);

		// filter by type against the base sail
		PlanNode invalidTuplesDueToDataAddedThatMatchesTargetOrPath = new ExternalTypeFilterNode(shaclSailConnection,
				Collections.singleton(classResource),
				addedStatementsTypeFilter, 1, false);

		if (shaclSailConnection.stats.hasRemoved()) {

			// Handle when a type statement has been removed, first get all removed type statements that match the
			// classResource for this shape
			PlanNode removedTypeStatements = new Select(shaclSailConnection.getRemovedStatements(),
					"?a a <" + classResource + ">", "?a");

			// Build a query to run against the base sail. eg:
			// ?c foaf:knows ?a.
			// ?c a foaf:Person.
			String query = getPath().getQuery("?c", "?a", null)
					+ nodeShape.getQuery("?c", "?q", shaclSailConnection.getRdfsSubClassOfReasoner());

			// do bulked external join for the removed class statements again the query above.
			// Essentially gets data that is now invalid because of the removed type statement
			PlanNode invalidDataDueToRemovedTypeStatement = new Sort(new ModifyTuple(
					new BulkedExternalInnerJoin(removedTypeStatements, shaclSailConnection, query, false, "?a", "?c"),
					t -> {
						List<Value> line = t.line;
						t.line = new ArrayList<>(2);
						t.line.add(line.get(1));
						t.line.add(line.get(0));

						return t;
					}));

			invalidTuplesDueToDataAddedThatMatchesTargetOrPath = new UnionNode(
					invalidTuplesDueToDataAddedThatMatchesTargetOrPath, invalidDataDueToRemovedTypeStatement);
		}

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidTuplesDueToDataAddedThatMatchesTargetOrPath,
					shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(invalidTuplesDueToDataAddedThatMatchesTargetOrPath, this);

	}

	private PlanNode getNegatedPlan(ShaclSailConnection shaclSailConnection, PlanNodeProvider overrideTargetNode) {

		if (overrideTargetNode != null) {
			PlanNode planNode;

			if (getPath() == null) {
				planNode = new ModifyTuple(overrideTargetNode.getPlanNode(), t -> {
					t.line.add(t.line.get(0));
					return t;
				});

			} else {
				planNode = new BulkedExternalInnerJoin(overrideTargetNode.getPlanNode(),
						shaclSailConnection,
						getPath().getQuery("?a", "?c", null), false, "?a", "?c");
			}

			// filter by type against the base sail
			planNode = new ExternalTypeFilterNode(shaclSailConnection, Collections.singleton(classResource),
					planNode, 1, true);

			return planNode;
		}

		if (getPath() != null) {

			if (shaclSailConnection.stats.isBaseSailEmpty()) {
				StringBuilder query = new StringBuilder();
				query.append(nodeShape.getQuery("?target", "?type", null));
				query.append("\n");
				query.append(getPath().getQuery("?target", "?object", null));
				query.append("\n");
				query.append("?object a <" + classResource + ">");

				return new Select(shaclSailConnection.getAddedStatements(), query.toString(), "?target", "?object");
			}

			if (!shaclSailConnection.hasStatement(null, RDF.TYPE, classResource, true)) {
				return new EmptyNode();
			}

			PlanNode addedByPath = getPlanAddedStatements(shaclSailConnection, null);

			// join all added by type and path
			InnerJoin innerJoinHolder = new InnerJoin(
					nodeShape.getPlanAddedStatements(shaclSailConnection, null),
					addedByPath);
			PlanNode innerJoin = innerJoinHolder.getJoined(BufferedPlanNode.class);
			PlanNode discardedRight = innerJoinHolder.getDiscardedRight(BufferedPlanNode.class);

			PlanNode typeFilterPlan = nodeShape.getTargetFilter(shaclSailConnection, discardedRight);

			innerJoin = new Unique(new UnionNode(innerJoin, typeFilterPlan));

			// also add anything that matches the path from the previousConnection, eg. if you add ":peter a
			// foaf:Person", and ":peter foaf:knows :steve" is already added
			PlanNode bulkedExternalLeftOuter = new BulkedExternalLeftOuterJoin(
					nodeShape.getPlanAddedStatements(shaclSailConnection, null),
					shaclSailConnection, getPath().getQuery("?a", "?c", null), true, "?a", "?c");

			// only get tuples that came from the first or the innerJoin or bulkedExternalLeftOuter,
			// we don't care if you added ":peter a foaf:Person" and nothing else and there is nothing else in the
			// underlying sail
			innerJoin = new TupleLengthFilter(new UnionNode(innerJoin, bulkedExternalLeftOuter), 2, false)
					.getTrueNode(UnBufferedPlanNode.class);

			{ // handle if ?a a <classResource> is added, which might affect data in the base sail
				PlanNode newAddedByClassResource = new Select(shaclSailConnection.getAddedStatements(),
						"?a a <" + classResource + ">", "?a");

				newAddedByClassResource = newAddedByClassResource;

				// Build a query to run against the base sail. eg:
				// ?c foaf:knows ?a.
				// ?c a foaf:Person.
				String query = getPath().getQuery("?c", "?a", null)
						+ nodeShape.getQuery("?c", "?q", shaclSailConnection.getRdfsSubClassOfReasoner());

				// do bulked external join for the removed class statements again the query above.
				// Essentially gets data that is now invalid because of the removed type statement
				PlanNode invalidDataDueToRemovedTypeStatement = new Sort(new ModifyTuple(
						new BulkedExternalInnerJoin(newAddedByClassResource, shaclSailConnection, query, false, "?a",
								"?c"),
						t -> {
							List<Value> line = t.line;
							t.line = new ArrayList<>(2);
							t.line.add(line.get(1));
							t.line.add(line.get(0));

							return t;
						}));

				innerJoin = new UnionNode(innerJoin, invalidDataDueToRemovedTypeStatement);
			}

			innerJoin = new Unique(innerJoin);

			return new ExternalTypeFilterNode(shaclSailConnection, Collections.singleton(classResource), innerJoin, 1,
					true);

		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		if (deactivated) {
			return false;
		}

		return removedStatements.hasStatement(null, RDF.TYPE, classResource, true)
				|| super.requiresEvaluation(addedStatements, removedStatements);
	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.ClassConstraintComponent;
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
		ClassPropertyShape that = (ClassPropertyShape) o;
		return classResource.equals(that.classResource);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), classResource);
	}

	@Override
	public String toString() {
		return "ClassPropertyShape{" +
				"classResource=" + classResource +
				", path=" + getPath() +
				'}';
	}

	@Override
	public PlanNode getAllTargetsPlan(ShaclSailConnection shaclSailConnection, boolean negated) {
		PlanNode plan = nodeShape.getPlanAddedStatements(shaclSailConnection, null);
		plan = new UnionNode(plan, nodeShape.getPlanRemovedStatements(shaclSailConnection, null));

		Path path = getPath();
		if (path != null) {
			plan = new UnionNode(plan, getPlanAddedStatements(shaclSailConnection, null));
			plan = new UnionNode(plan, getPlanRemovedStatements(shaclSailConnection, null));
		}

		if (!negated && shaclSailConnection.stats.hasRemoved() && getPath() != null) {
			// Handle when a type statement has been removed, first get all removed type statements that match the
			// classResource for this shape
			PlanNode removedTypeStatements = new Select(shaclSailConnection.getRemovedStatements(),
					"?a a <" + classResource + ">", "?a");
			// Build a query to run against the base sail. eg:
			// ?c foaf:knows ?a.
			// ?c a foaf:Person.
			String query = getPath().getQuery("?c", "?a", null)
					+ nodeShape.getQuery("?c", "?q", shaclSailConnection.getRdfsSubClassOfReasoner());

			// do bulked external join for the removed class statements again the query above.
			// Essentially gets data that is now invalid because of the removed type statement
			PlanNode invalidDataDueToRemovedTypeStatement = new Sort(new ModifyTuple(
					new BulkedExternalInnerJoin(removedTypeStatements, shaclSailConnection, query, false, "?a", "?c"),
					t -> {
						List<Value> line = t.line;
						t.line = new ArrayList<>(2);
						t.line.add(line.get(1));
						t.line.add(line.get(0));

						return t;
					}));

			plan = new UnionNode(plan, invalidDataDueToRemovedTypeStatement);

		}

		if (negated && shaclSailConnection.stats.hasAdded() && getPath() != null) {
			// Handle when a type statement has been removed, first get all removed type statements that match the
			// classResource for this shape
			PlanNode removedTypeStatements = new Select(shaclSailConnection.getAddedStatements(),
					"?a a <" + classResource + ">", "?a");
			// Build a query to run against the base sail. eg:
			// ?c foaf:knows ?a.
			// ?c a foaf:Person.
			String query = getPath().getQuery("?c", "?a", null)
					+ nodeShape.getQuery("?c", "?q", shaclSailConnection.getRdfsSubClassOfReasoner());

			// do bulked external join for the removed class statements again the query above.
			// Essentially gets data that is now invalid because of the removed type statement
			PlanNode invalidDataDueToRemovedTypeStatement = new Sort(new ModifyTuple(
					new BulkedExternalInnerJoin(removedTypeStatements, shaclSailConnection, query, false, "?a", "?c"),
					t -> {
						List<Value> line = t.line;
						t.line = new ArrayList<>(2);
						t.line.add(line.get(1));
						t.line.add(line.get(0));

						return t;
					}));

			plan = new UnionNode(plan, invalidDataDueToRemovedTypeStatement);

		}

		plan = new Unique(new TrimTuple(plan, 0, 1));

		return nodeShape.getTargetFilter(shaclSailConnection, plan);
	}
}
