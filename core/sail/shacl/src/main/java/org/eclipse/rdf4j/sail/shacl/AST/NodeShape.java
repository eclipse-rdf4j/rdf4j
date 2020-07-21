/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Stats;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AST (Abstract Syntax Tree) node that represents the NodeShape node. NodeShape nodes can have multiple property
 * nodeShapes, which are the restrictions for everything that matches the NodeShape.
 *
 * @author Håvard Mikkelsen Ottestad
 * @author Heshan Jayasinghe
 */
public class NodeShape implements PlanGenerator, RequiresEvalutation, QueryGenerator {

	private static final Logger logger = LoggerFactory.getLogger(NodeShape.class);

	final Resource id;

	private List<PathPropertyShape> propertyShapes = Collections.emptyList();
	private List<PathPropertyShape> nodeShapes = Collections.emptyList();

	public NodeShape(Resource id, ShaclSail shaclSail, SailRepositoryConnection connection, boolean deactivated) {
		this.id = id;
		if (!deactivated) {
			propertyShapes = PropertyShape.Factory.getPropertyShapes(id, connection, this, shaclSail);
			nodeShapes = PropertyShape.Factory.getPropertyShapesInner(connection, this, id, null, shaclSail);
		}
	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		PlanNode node = new Select(connectionsGroup.getBaseConnection(), getQuery("?a", "?c", null), "?a", "?c");

		return new Unique(new TrimTuple(node, 0, 1));
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		assert planeNodeWrapper == null;

		PlanNode node = connectionsGroup.getCachedNodeFor(
				new Select(connectionsGroup.getAddedStatements(), getQuery("?a", "?c", null), "?a", "?c"));

		return new Unique(new TrimTuple(node, 0, 1));
	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		assert planeNodeWrapper == null;

		PlanNode node = connectionsGroup.getCachedNodeFor(
				new Select(connectionsGroup.getRemovedStatements(), getQuery("?a", "?c", null), "?a", "?c"));

		return new Unique(new TrimTuple(node, 0, 1));
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Path> getPaths() {
		throw new IllegalStateException();
	}

	public Stream<PlanNode> generatePlans(ConnectionsGroup connectionsGroup, NodeShape nodeShape, boolean printPlans,
			boolean validateEntireBaseSail) {

		PlanNodeProvider overrideTargetNodeBufferedSplitter;
		SailConnection addedStatements;
		SailConnection removedStatements;

		if (validateEntireBaseSail) {
			if (connectionsGroup.getTransactionSettings().isCacheSelectNodes()) {
				PlanNode overrideTargetNode = getPlan(connectionsGroup, printPlans, null, false, false);
				overrideTargetNodeBufferedSplitter = new BufferedSplitter(overrideTargetNode);
			} else {
				overrideTargetNodeBufferedSplitter = () -> getPlan(connectionsGroup, printPlans, null,
						false, false);
			}
			addedStatements = connectionsGroup.getBaseConnection();
			removedStatements = connectionsGroup.getBaseConnection();
		} else {
			overrideTargetNodeBufferedSplitter = null;
			addedStatements = connectionsGroup.getAddedStatements();
			removedStatements = connectionsGroup.getRemovedStatements();
		}

		Stream<PlanNode> propertyShapesPlans = convertToPlan(propertyShapes, connectionsGroup, nodeShape, printPlans,
				overrideTargetNodeBufferedSplitter, addedStatements, removedStatements);

		Stream<PlanNode> nodeShapesPlans = convertToPlan(this.nodeShapes, connectionsGroup, nodeShape, printPlans,
				overrideTargetNodeBufferedSplitter, addedStatements, removedStatements);

		return Stream.concat(propertyShapesPlans, nodeShapesPlans);
	}

	private Stream<PlanNode> convertToPlan(List<PathPropertyShape> propertyShapes, ConnectionsGroup connectionsGroup,
			NodeShape nodeShape, boolean printPlans, PlanNodeProvider overrideTargetNodeBufferedSplitter,
			SailConnection addedStatements, SailConnection removedStatements) {

		Stats stats = connectionsGroup.getStats();
		return propertyShapes
				.stream()
				.filter(propertyShape -> propertyShape.requiresEvaluation(addedStatements, removedStatements, stats))
				.map(propertyShape -> propertyShape.getPlan(connectionsGroup, printPlans,
						overrideTargetNodeBufferedSplitter, false, false));
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements, Stats stats) {
		return true;
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		return subjectVariable + " ?fj42798yfhf2j4 " + objectVariable + " .";
	}

	public Resource getId() {
		return id;
	}

	/**
	 * Returns a query that can be run against the base sail to retrieve all targets that would be valid according to
	 * this shape.
	 *
	 * Eg. A datatype restriction on foaf:age datatype == integer would look like:
	 *
	 * ?a foaf:age ?age_UUID. FILTER(isLiteral(?age_UUID) && datatype(?age_UUID) = xsd:integer)
	 *
	 * Where targetVar == ?a
	 *
	 * @param targetVar the SPARQL variable name used to bind the target nodes
	 * @return sparql query
	 */
	protected String buildSparqlValidNodes(String targetVar) {

		if (!propertyShapes.isEmpty() && !nodeShapes.isEmpty()) {
			throw new UnsupportedOperationException(
					"sh:targetShape don't support both nodeshapes and property shapes!");
		}

		if (!propertyShapes.isEmpty()) {
			return propertyShapes
					.stream()
					.map(propertyShapes -> propertyShapes.buildSparqlValidNodes(targetVar))
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");
		}

		if (!nodeShapes.isEmpty()) {
			return nodeShapes
					.stream()
					.map(propertyShapes -> propertyShapes.buildSparqlValidNodes(targetVar))
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");
		}

		return "";

	}

	/**
	 * Get all statement patterns that would affect the validity of this shape.
	 *
	 * Eg. If the shape validates that all foaf:Person should have one foaf:name, then the statement pattern that would
	 * affect the validity would be "?a foaf:name ?b". We don't consider the patterns that would affect the targets (eg.
	 * foaf:Person).
	 *
	 * @return
	 */
	protected Stream<StatementPattern> getStatementPatterns() {

		return Stream.concat(
				propertyShapes
						.stream()
						.flatMap(PropertyShape::getStatementPatterns),
				nodeShapes
						.stream()
						.flatMap(PropertyShape::getStatementPatterns));
	}

	public static class Factory {

		public static List<NodeShape> getShapes(SailRepositoryConnection connection, ShaclSail shaclSail) {
			try (Stream<Statement> stream = connection.getStatements(null, RDF.TYPE, SHACL.NODE_SHAPE).stream()) {
				return stream.map(Statement::getSubject).flatMap(shapeId -> {

					List<NodeShape> propertyShapes = new ArrayList<>();

					ShaclProperties shaclProperties = new ShaclProperties(shapeId, connection);

					if (!shaclProperties.getTargetClass().isEmpty()) {
						propertyShapes
								.add(new TargetClass(shapeId, shaclSail, connection, shaclProperties.isDeactivated(),
										shaclProperties.getTargetClass()));
					}
					if (!shaclProperties.getTargetNode().isEmpty()) {
						propertyShapes
								.add(new TargetNode(shapeId, shaclSail, connection, shaclProperties.isDeactivated(),
										shaclProperties.getTargetNode()));
					}
					if (!shaclProperties.getTargetSubjectsOf().isEmpty()) {
						propertyShapes.add(
								new TargetSubjectsOf(shapeId, shaclSail, connection, shaclProperties.isDeactivated(),
										shaclProperties.getTargetSubjectsOf()));
					}
					if (!shaclProperties.getTargetObjectsOf().isEmpty()) {
						propertyShapes.add(
								new TargetObjectsOf(shapeId, shaclSail, connection, shaclProperties.isDeactivated(),
										shaclProperties.getTargetObjectsOf()));
					}

					if (shaclSail.isEclipseRdf4jShaclExtensions()) {
						shaclProperties.getTargetShape()
								.stream()
								.map(targetShape -> new TargetShape(shapeId, shaclSail, connection,
										shaclProperties.isDeactivated(), targetShape))
								.forEach(propertyShapes::add);

					}

					if (!shaclProperties.getTarget().isEmpty()) {
						shaclProperties.getTarget()
								.forEach(sparqlTarget -> {
//									if (connection.hasStatement(sparqlTarget, RDF.TYPE, SHACL.SPARQL_TARGET, true)) {
//										propertyShapes.add(new SparqlTarget(shapeId, shaclSail, connection,
//												shaclProperties.isDeactivated(), sparqlTarget));
//									}
									if (shaclSail.isDashDataShapes() && connection.hasStatement(sparqlTarget,
											RDF.TYPE, DASH.AllObjectsTarget, true)) {
										propertyShapes.add(
												new AllObjectsTarget(shapeId, shaclSail, connection,
														shaclProperties.isDeactivated()));
									}
									if (shaclSail.isDashDataShapes() && connection.hasStatement(sparqlTarget,
											RDF.TYPE, DASH.AllSubjectsTarget, true)) {
										propertyShapes.add(new AllSubjectsTarget(shapeId, shaclSail, connection,
												shaclProperties.isDeactivated()));
									}

								});

					}

					if (shaclSail.isUndefinedTargetValidatesAllSubjects() && propertyShapes.isEmpty()) {
						logger.info(
								"isUndefinedTargetValidatesAllSubjects() is deprecated, please use .setDashDataShapes(true) and use the custom targets from http://datashapes.org/dash#AllSubjectsTarget");

						propertyShapes
								.add(new NodeShape(shapeId, shaclSail, connection, shaclProperties.isDeactivated()));
						// target class nodeShapes are the only supported nodeShapes
					}

					return propertyShapes.stream();
				}).filter(Objects::nonNull).collect(Collectors.toList());
			}
		}

	}

	@Override
	public String toString() {
		return id.toString();
	}

	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		return parent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NodeShape nodeShape = (NodeShape) o;
		return id.equals(nodeShape.id) &&
				propertyShapes.equals(nodeShape.propertyShapes) &&
				nodeShapes.equals(nodeShape.nodeShapes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, propertyShapes, nodeShapes);
	}

}
