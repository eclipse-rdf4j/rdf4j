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
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents a property shape without any restrictions. This node should be extended by other nodes.
 *
 * @author Heshan Jayasinghe
 */
public class PropertyShape implements PlanGenerator, RequiresEvalutation {

	private Resource id;

	Shape shape;


	PropertyShape(Resource id, Shape shape) {
		this.id = id;
		this.shape = shape;
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {
		throw new IllegalStateException("Should never get here!!!");
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, Shape shape) {
		throw new IllegalStateException("Should never get here!!!");
	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, Shape shape) {
		throw new IllegalStateException("Should never get here!!!");
	}

	@Override
	public boolean requiresEvalutation(Repository addedStatements, Repository removedStatements) {
		return false;
	}

	static class Factory {

		static List<PropertyShape> getProprtyShapes(Resource ShapeId, SailRepositoryConnection connection, Shape shape) {

			try (Stream<Statement> stream = Iterations.stream(connection.getStatements(ShapeId, SHACL.PROPERTY, null))) {
				return stream
					.map(Statement::getObject)
					.map(v -> (Resource) v)
					.flatMap(propertyShapeId -> {
						List<PropertyShape> propertyShapes = new ArrayList<>(2);

						if (hasMinCount(propertyShapeId, connection)) {
							propertyShapes.add(new MinCountPropertyShape(propertyShapeId, connection, shape));
						}

						if (hasMaxCount(propertyShapeId, connection)) {
							propertyShapes.add(new MaxCountPropertyShape(propertyShapeId, connection, shape));
						}

						if (hasDatatype(propertyShapeId, connection)) {
							propertyShapes.add(new DatatypePropertyShape(propertyShapeId, connection, shape));
						}

						return propertyShapes.stream();

					})
					.collect(Collectors.toList());
			}

		}


		private static boolean hasMinCount(Resource id, SailRepositoryConnection connection) {
			return connection.hasStatement(id, SHACL.MIN_COUNT, null, true);
		}

		private static boolean hasMaxCount(Resource id, SailRepositoryConnection connection) {
			return connection.hasStatement(id, SHACL.MAX_COUNT, null, true);
		}

		private static boolean hasDatatype(Resource id, SailRepositoryConnection connection) {
			return connection.hasStatement(id, SHACL.DATATYPE, null, true);
		}

	}
}



