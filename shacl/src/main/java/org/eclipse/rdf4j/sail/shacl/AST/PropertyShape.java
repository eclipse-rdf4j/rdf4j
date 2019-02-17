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
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
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
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, PlanNode overrideTargetNode) {
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
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return nodeShape.requiresEvaluation(addedStatements, removedStatements);
	}


	public String getPlanAsGraphvizDot(PlanNode planNode, ShaclSailConnection shaclSailConnection) {

		StringBuilder stringBuilder = new StringBuilder("Graphviz DOT output:\n\n");

		stringBuilder.append("digraph  {").append("\n");
		stringBuilder.append("labelloc=t;\nfontsize=30;\nlabel=\"" + this.getClass().getSimpleName() + "\";").append("\n");

		stringBuilder.append(System.identityHashCode(shaclSailConnection) + " [label=\"Base sail\" nodeShape=pentagon fillcolor=lightblue style=filled];").append("\n");
		stringBuilder.append(System.identityHashCode(shaclSailConnection.getPreviousStateConnection()) + " [label=\"Previous state connection\" nodeShape=pentagon fillcolor=lightblue style=filled];").append("\n");


		MemoryStore addedStatements = ((MemoryStoreConnection) shaclSailConnection.getAddedStatements()).getSail();
		MemoryStore removedStatements = ((MemoryStoreConnection) shaclSailConnection.getRemovedStatements()).getSail();

		stringBuilder.append(System.identityHashCode(addedStatements) + " [label=\"Added statements\" nodeShape=pentagon fillcolor=lightblue style=filled];").append("\n");
		stringBuilder.append(System.identityHashCode(removedStatements) + " [label=\"Removed statements\" nodeShape=pentagon fillcolor=lightblue style=filled];").append("\n");


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

			ShaclProperties shaclProperties = new ShaclProperties(propertyShapeId, connection);

			if (shaclProperties.minCount != null) {
				propertyShapes.add(new MinCountPropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.minCount));
			}
			if (shaclProperties.maxCount != null) {
				propertyShapes.add(new MaxCountPropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.maxCount));
			}
			if (shaclProperties.datatype != null) {
				propertyShapes.add(new DatatypePropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.datatype));
			}
			if(shaclProperties.or != null){
				propertyShapes.add(new OrPropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.or));
			}
			if(shaclProperties.minLength != null){
				propertyShapes.add(new MinLengthPropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.minLength));
			}
			if(shaclProperties.maxLength != null){
				propertyShapes.add(new MaxLengthPropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.maxLength));
			}
			if(shaclProperties.pattern != null){
				propertyShapes.add(new PatternPropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.pattern));
			}
			if(shaclProperties.languageIn != null){
				propertyShapes.add(new LanguageInPropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.languageIn));
			}
			if(shaclProperties.nodeKind != null){
				propertyShapes.add(new NodeKindPropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.nodeKind));
			}
			if(shaclProperties.minExclusive != null){
				propertyShapes.add(new MinExclusivePropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.minExclusive));
			}
			if(shaclProperties.maxExclusive != null){
				propertyShapes.add(new MaxExclusivePropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.maxExclusive));
			}
			if(shaclProperties.maxInclusive != null){
				propertyShapes.add(new MaxInclusivePropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.maxInclusive));
			}
			if(shaclProperties.minInclusive != null){
				propertyShapes.add(new MinInclusivePropertyShape(propertyShapeId, connection, nodeShape, shaclProperties.minInclusive));
			}


			if (shaclProperties.clazz != null) {
				propertyShapes.add(new ClassPropertyShape(propertyShapeId, connection, nodeShape));
			}
			return propertyShapes;
		}
	}
}



