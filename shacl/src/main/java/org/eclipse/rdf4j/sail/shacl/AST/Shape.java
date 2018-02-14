/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents the Shape node. Shape nodes can have multiple property shapes, which are the restrictions for everything that matches the Shape.
 *
 * @author Heshan Jayasinghe
 */
public class Shape implements PlanGenerator, RequiresEvalutation, QueryGenerator {

	private Resource id;

	private List<PropertyShape> propertyShapes;

	public Shape(Resource id, SailRepositoryConnection connection) {
		this.id = id;
		propertyShapes = PropertyShape.Factory.getProprtyShapes(id, connection, this);
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, Shape shape) {
		return new Select(shaclSailConnection.addedStatements, getQuery());
	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, Shape shape) {
		return new Select(shaclSailConnection.removedStatements, getQuery());
	}

	public List<PlanNode> generatePlans(ShaclSailConnection shaclSailConnection, Shape shape) {
		return propertyShapes.stream()
			.filter(propertyShape -> propertyShape.requiresEvalutation(shaclSailConnection.addedStatements, shaclSailConnection.removedStatements))
			.map(propertyShape -> propertyShape.getPlan(shaclSailConnection, shape))
			.collect(Collectors.toList());
	}

	@Override
	public boolean requiresEvalutation(Repository addedStatements, Repository removedStatements) {
		return propertyShapes
			.stream()
			.anyMatch(propertyShape -> propertyShape.requiresEvalutation(addedStatements, removedStatements));
	}

	@Override
	public String getQuery() {
		return "?a ?b ?c";
	}


	public static class Factory {

		public static List<Shape> getShapes(SailRepositoryConnection connection) {
			try (Stream<Statement> stream = Iterations.stream(connection.getStatements(null, RDF.TYPE, SHACL.SHAPE))) {
				return stream.map(Statement::getSubject).map(shapeId -> {
					if (hasTargetClass(shapeId, connection)) {
						return new TargetClass(shapeId, connection);
					} else {
						return new Shape(shapeId, connection); // target class shapes are the only supported shapes
					}
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
