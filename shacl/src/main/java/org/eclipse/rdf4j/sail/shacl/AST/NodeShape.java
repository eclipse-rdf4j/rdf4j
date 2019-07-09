/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents the NodeShape node. NodeShape nodes can have multiple property
 * nodeShapes, which are the restrictions for everything that matches the NodeShape.
 *
 * @author Heshan Jayasinghe
 */
public class NodeShape implements PlanGenerator, RequiresEvalutation, QueryGenerator {

	private Resource id;

	private List<PathPropertyShape> propertyShapes = Collections.emptyList();
	private List<PathPropertyShape> nodeShapes = Collections.emptyList();

	public NodeShape(Resource id, SailRepositoryConnection connection, boolean deactivated) {
		this.id = id;
		if (!deactivated) {
			propertyShapes = PropertyShape.Factory.getPropertyShapes(id, connection, this);
			nodeShapes = PropertyShape.Factory.getPropertyShapesInner(connection, this, id, null);
		}
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode node = shaclSailConnection.getCachedNodeFor(
				new Select(shaclSailConnection.getAddedStatements(), getQuery("?a", "?c", null), "?a", "?c"));
		return new Unique(new TrimTuple(node, 0, 1));
	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode node = shaclSailConnection.getCachedNodeFor(
				new Select(shaclSailConnection.getRemovedStatements(), getQuery("?a", "?c", null), "?a", "?c"));
		return new Unique(new TrimTuple(node, 0, 1));
	}

	@Override
	public PlanNode getAllTargetsPlan(ShaclSailConnection shaclSailConnection, boolean negated) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Path> getPaths() {
		throw new IllegalStateException();
	}

	public Stream<PlanNode> generatePlans(ShaclSailConnection shaclSailConnection, NodeShape nodeShape,
			boolean printPlans, boolean validateEntireBaseSail) {

		PlanNodeProvider overrideTargetNodeBufferedSplitter;
		SailConnection addedStatements;
		SailConnection removedStatements;

		if (validateEntireBaseSail) {
			if (shaclSailConnection.sail.isCacheSelectNodes()) {
				PlanNode overrideTargetNode = getPlan(shaclSailConnection, printPlans, null, false, false);
				overrideTargetNodeBufferedSplitter = new BufferedSplitter(overrideTargetNode);
			} else {
				overrideTargetNodeBufferedSplitter = () -> getPlan(shaclSailConnection, printPlans, null,
						false, false);
			}
			addedStatements = shaclSailConnection;
			removedStatements = shaclSailConnection;
		} else {
			overrideTargetNodeBufferedSplitter = null;
			addedStatements = shaclSailConnection.getAddedStatements();
			removedStatements = shaclSailConnection.getRemovedStatements();
		}

		Stream<PlanNode> propertyShapesPlans = convertToPlan(propertyShapes, shaclSailConnection, nodeShape, printPlans,
				overrideTargetNodeBufferedSplitter, addedStatements, removedStatements);

		Stream<PlanNode> nodeShapesPlans = convertToPlan(this.nodeShapes, shaclSailConnection, nodeShape, printPlans,
				overrideTargetNodeBufferedSplitter, addedStatements, removedStatements);

		return Stream.concat(propertyShapesPlans, nodeShapesPlans);
	}

	private Stream<PlanNode> convertToPlan(List<PathPropertyShape> propertyShapes,
			ShaclSailConnection shaclSailConnection,
			NodeShape nodeShape, boolean printPlans, PlanNodeProvider overrideTargetNodeBufferedSplitter,
			SailConnection addedStatements,
			SailConnection removedStatements) {

		return propertyShapes
				.stream()
				.filter(propertyShape -> propertyShape.requiresEvaluation(addedStatements, removedStatements))
				.map(propertyShape -> propertyShape.getPlan(shaclSailConnection, printPlans,
						overrideTargetNodeBufferedSplitter, false, false));
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return true;
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		return subjectVariable + " ?b " + objectVariable;
	}

	public Resource getId() {
		return id;
	}

	public static class Factory {

		public static List<NodeShape> getShapes(SailRepositoryConnection connection, ShaclSail sail) {
			try (Stream<Statement> stream = Iterations
					.stream(connection.getStatements(null, RDF.TYPE, SHACL.NODE_SHAPE))) {
				return stream.map(Statement::getSubject).flatMap(shapeId -> {

					List<NodeShape> propertyShapes = new ArrayList<>(2);

					ShaclProperties shaclProperties = new ShaclProperties(shapeId, connection);

					if (!shaclProperties.targetClass.isEmpty()) {
						propertyShapes.add(new TargetClass(shapeId, connection, shaclProperties.deactivated,
								shaclProperties.targetClass));
					}
					if (!shaclProperties.targetNode.isEmpty()) {
						propertyShapes.add(new TargetNode(shapeId, connection, shaclProperties.deactivated,
								shaclProperties.targetNode));
					}
					if (!shaclProperties.targetSubjectsOf.isEmpty()) {
						propertyShapes.add(new TargetSubjectsOf(shapeId, connection, shaclProperties.deactivated,
								shaclProperties.targetSubjectsOf));
					}
					if (!shaclProperties.targetObjectsOf.isEmpty()) {
						propertyShapes.add(new TargetObjectsOf(shapeId, connection, shaclProperties.deactivated,
								shaclProperties.targetObjectsOf));
					}

					if (sail.isUndefinedTargetValidatesAllSubjects() && propertyShapes.isEmpty()) {
						propertyShapes.add(new NodeShape(shapeId, connection, shaclProperties.deactivated)); // target
						// class
						// nodeShapes
						// are
						// the
						// only
						// supported
						// nodeShapes
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

	public PlanNode getTargetFilter(NotifyingSailConnection shaclSailConnection, PlanNode parent) {
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
