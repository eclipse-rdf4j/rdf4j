/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.plan.PlanNode;
import org.eclipse.rdf4j.plan.Select;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.validation.ShaclSailConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Heshan Jayasinghe
 */
public class Shape implements PlanGenerator {

	Resource id;

	List<PropertyShape> propertyShapes;

	TargetClass targetClass;

	public Shape(Resource id, SailRepositoryConnection connection) {
		this.id = id;
		propertyShapes = PropertyShape.Factory.getProprtyShapes(id, connection);

	}

	@Override
	public Select getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {
		return null;
	}

	public List<PlanNode> generatePlans(ShaclSailConnection shaclSailConnection, Shape shape) {
		return propertyShapes.stream().map(
				pathpropertyShape -> pathpropertyShape.getPlan(shaclSailConnection, shape)).collect(
				Collectors.toList());
	}

	public static class Factory {

		public static List<Shape> getShapes(SailRepositoryConnection connection) {
			List<Shape> shapes = new ArrayList<>();
			RepositoryResult<Statement> statements = connection.getStatements(null, RDF.TYPE, SHACL.SHAPE);
			while (statements.hasNext()) {
				Resource shapeId = statements.next().getSubject();
				if (hasTargetClass(shapeId, connection)) {
					shapes.add(new TargetClass(shapeId, connection));
				}
				else {
					shapes.add(new Shape(shapeId, connection));
				}
			}
			return shapes;
		}

		private static boolean hasTargetClass(Resource shapeId, SailRepositoryConnection connection) {
			return connection.hasStatement(shapeId, SHACL.TARGET_CLASS, null, true);
		}
	}
}
