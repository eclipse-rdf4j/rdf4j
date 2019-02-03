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
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConfig;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents the NodeShape node. NodeShape nodes can have multiple property nodeShapes, which are the restrictions for everything that matches the NodeShape.
 *
 * @author Heshan Jayasinghe
 */
public class NodeShape implements PlanGenerator, RequiresEvalutation, QueryGenerator {

	private Resource id;

	private List<PropertyShape> propertyShapes;

	public NodeShape(Resource id, SailRepositoryConnection connection) {
		this.id = id;
		propertyShapes = PropertyShape.Factory.getPropertyShapes(id, connection, this);
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, boolean assumeBaseSailValid) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		PlanNode node = shaclSailConnection.getCachedNodeFor(new Select(shaclSailConnection.getAddedStatements(), getQuery()));
		return new TrimTuple(new LoggingNode(node), 1);
	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		PlanNode node = shaclSailConnection.getCachedNodeFor(new Select(shaclSailConnection.getRemovedStatements(), getQuery()));
		return new TrimTuple(new LoggingNode(node), 1);
	}

	@Override
	public List<Path> getPaths() {
		throw new IllegalStateException();
	}

	public List<PlanNode> generatePlans(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans) {
		return propertyShapes.stream()
			.filter(propertyShape -> propertyShape.requiresEvaluation(shaclSailConnection.getAddedStatements(), shaclSailConnection.getRemovedStatements()))
			.map(propertyShape -> propertyShape.getPlan(shaclSailConnection, nodeShape, printPlans, true))
			.collect(Collectors.toList());
	}

	@Override
	public boolean requiresEvaluation(Repository addedStatements, Repository removedStatements) {
		return propertyShapes
			.stream()
			.anyMatch(propertyShape -> propertyShape.requiresEvaluation(addedStatements, removedStatements));
	}

	@Override
	public String getQuery() {


		return "?a ?b ?c";
	}

	public Resource getId() {
		return id;
	}


	public static class Factory {

		public static List<NodeShape> getShapes(SailRepositoryConnection connection, ShaclSailConfig config) {
			try (Stream<Statement> stream = Iterations.stream(connection.getStatements(null, RDF.TYPE, SHACL.NODE_SHAPE))) {
				return stream.map(Statement::getSubject).map(shapeId -> {
					if (hasTargetClass(shapeId, connection)) {
						return new TargetClass(shapeId, connection);
					} else {
						if(config.isUndefinedTargetValidatesAllSubjects()) {
							return new NodeShape(shapeId, connection); // target class nodeShapes are the only supported nodeShapes
						}
					}
					return null;
				})
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			}
		}

		private static boolean hasTargetClass(Resource shapeId, SailRepositoryConnection connection) {
			return connection.hasStatement(shapeId, SHACL.TARGET_CLASS, null, true);
		}
	}

	@Override
	public String toString() {
		return id.toString();
	}
}
