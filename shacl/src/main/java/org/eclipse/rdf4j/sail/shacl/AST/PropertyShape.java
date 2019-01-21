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
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents a property nodeShape without any restrictions. This node should be extended by other nodes.
 *
 * @author Heshan Jayasinghe
 */
public class PropertyShape implements PlanGenerator, RequiresEvalutation {

	private Resource id;

	NodeShape nodeShape;


	PropertyShape(Resource id, NodeShape nodeShape) {
		this.id = id;
		this.nodeShape = nodeShape;
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, boolean assumeBaseSailValid) {
		throw new IllegalStateException("Should never get here!!!");
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		throw new IllegalStateException("Should never get here!!!");
	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		throw new IllegalStateException("Should never get here!!!");
	}

	@Override
	public List<Path> getPaths() {
		throw new IllegalStateException();
	}

	@Override
	public boolean requiresEvaluation(Repository addedStatements, Repository removedStatements) {
		return false;
	}


	public String getPlanAsGraphvizDot(PlanNode planNode, ShaclSailConnection shaclSailConnection) {

		StringBuilder stringBuilder = new StringBuilder("Graphviz DOT output:\n\n");

		stringBuilder.append("digraph  {").append("\n");
		stringBuilder.append("labelloc=t;\nfontsize=30;\nlabel=\"" + this.getClass().getSimpleName() + "\";").append("\n");

		stringBuilder.append(System.identityHashCode(shaclSailConnection) + " [label=\"Base sail\" nodeShape=pentagon fillcolor=lightblue style=filled];").append("\n");
		stringBuilder.append(System.identityHashCode(shaclSailConnection.getAddedStatements()) + " [label=\"Added statements\" nodeShape=pentagon fillcolor=lightblue style=filled];").append("\n");
		stringBuilder.append(System.identityHashCode(shaclSailConnection.getRemovedStatements()) + " [label=\"Removed statements\" nodeShape=pentagon fillcolor=lightblue style=filled];").append("\n");
		stringBuilder.append(System.identityHashCode(shaclSailConnection.getPreviousStateConnection()) + " [label=\"Previous state connection\" nodeShape=pentagon fillcolor=lightblue style=filled];").append("\n");


		planNode.getPlanAsGraphvizDot(stringBuilder);

		stringBuilder.append("}").append("\n");


		return stringBuilder.append("\n\n").toString();

	}

	static List<Value> toList(SailRepositoryConnection connection, Resource orList) {
		List<Value> ret = new ArrayList<>();
		while (!orList.equals(RDF.NIL)) {
			try (Stream<Statement> stream = Iterations.stream(connection.getStatements(orList, RDF.FIRST, null))) {
				Value value = stream.map(Statement::getObject).findAny().get();
				ret.add(value);
			}

			try (Stream<Statement> stream = Iterations.stream(connection.getStatements(orList, RDF.REST, null))) {
				orList = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().get();
			}

		}


		return ret;


	}

	public Resource getId() {
		return id;
	}

	public NodeShape getNodeShape() {
		return nodeShape;
	}

	public SourceConstraintComponent getSourceConstraintComponent() {
		throw new IllegalStateException("Missing implementetion in extending class!");
	}

	static class Factory {

		static List<PropertyShape> getPropertyShapes(Resource ShapeId, SailRepositoryConnection connection, NodeShape nodeShape) {

			try (Stream<Statement> stream = Iterations.stream(connection.getStatements(ShapeId, SHACL.PROPERTY, null))) {
				return stream
					.map(Statement::getObject)
					.map(v -> (Resource) v)
					.flatMap(propertyShapeId -> {
						List<PropertyShape> propertyShapes = getPropertyShapesInner(connection, nodeShape, propertyShapeId);

						return propertyShapes.stream();

					})
					.collect(Collectors.toList());
			}

		}

		static List<PropertyShape> getPropertyShapesInner(SailRepositoryConnection connection, NodeShape nodeShape, Resource propertyShapeId) {
			List<PropertyShape> propertyShapes = new ArrayList<>(2);

			if (hasMinCount(propertyShapeId, connection)) {
				propertyShapes.add(new MinCountPropertyShape(propertyShapeId, connection, nodeShape));
			}

			if (hasMaxCount(propertyShapeId, connection)) {
				propertyShapes.add(new MaxCountPropertyShape(propertyShapeId, connection, nodeShape));
			}

			if (hasDatatype(propertyShapeId, connection)) {
				propertyShapes.add(new DatatypePropertyShape(propertyShapeId, connection, nodeShape));
			}

			if (hasOr(propertyShapeId, connection)) {
				propertyShapes.add(new OrPropertyShape(propertyShapeId, connection, nodeShape));
			}

			if (hasMinLength(propertyShapeId, connection)) {
				propertyShapes.add(new MinLengthPropertyShape(propertyShapeId, connection, nodeShape));
			}

			if (hasMaxLength(propertyShapeId, connection)) {
				propertyShapes.add(new MaxLengthPropertyShape(propertyShapeId, connection, nodeShape));
			}

			if (hasPattern(propertyShapeId, connection)) {
				propertyShapes.add(new PatternPropertyShape(propertyShapeId, connection, nodeShape));
			}

			if (hasLanguageIn(propertyShapeId, connection)) {
				propertyShapes.add(new LanguageInPropertyShape(propertyShapeId, connection, nodeShape));
			}

			if (hasNodeKind(propertyShapeId, connection)) {
				propertyShapes.add(new NodeKindPropertyShape(propertyShapeId, connection, nodeShape));
			}

			if (hasMinExclusive(propertyShapeId, connection)) {
				propertyShapes.add(new MinExclusivePropertyShape(propertyShapeId, connection, nodeShape));
			}
			return propertyShapes;
		}

		private static boolean hasOr(Resource id, SailRepositoryConnection connection) {
			return connection.hasStatement(id, SHACL.OR, null, true);
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

		private static boolean hasMinLength(Resource id, SailRepositoryConnection connection) {
			return connection.hasStatement(id, SHACL.MIN_LENGTH, null, true);
		}

		private static boolean hasMaxLength(Resource id, SailRepositoryConnection connection) {
			return connection.hasStatement(id, SHACL.MAX_LENGTH, null, true);
		}

		private static boolean hasPattern(Resource id, SailRepositoryConnection connection) {
			return connection.hasStatement(id, SHACL.PATTERN, null, true);
		}

		private static boolean hasLanguageIn(Resource id, SailRepositoryConnection connection) {
			return connection.hasStatement(id, SHACL.LANGUAGE_IN, null, true);
		}

		private static boolean hasNodeKind(Resource id, SailRepositoryConnection connection) {
			return connection.hasStatement(id, SHACL.NODE_KIND_PROP, null, true);
		}

		private static boolean hasMinExclusive(Resource id, SailRepositoryConnection connection) {
			return connection.hasStatement(id, SHACL.MIN_EXCLUSIVE, null, true);
		}

	}
}



