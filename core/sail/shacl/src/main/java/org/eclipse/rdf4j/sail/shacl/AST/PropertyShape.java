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
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents a property nodeShape without any restrictions. This node should
 * be extended by other nodes.
 *
 * @author Heshan Jayasinghe, Håvard Mikkelsen Ottestad
 */
public abstract class PropertyShape implements PlanGenerator, RequiresEvalutation {

	final boolean deactivated;
	private Resource id;

	NodeShape nodeShape;
	PathPropertyShape parent;

	PropertyShape(Resource id, NodeShape nodeShape, boolean deactivated, PathPropertyShape parent) {
		this.id = id;
		this.nodeShape = nodeShape;
		this.deactivated = deactivated;
		this.parent = parent;
	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		throw new IllegalStateException("Should never get here!!!");
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		throw new IllegalStateException("Should never get here!!!");
	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		throw new IllegalStateException("Should never get here!!!");
	}

	@Override
	public List<Path> getPaths() {
		throw new IllegalStateException();
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		if (deactivated) {
			return false;
		}

		return nodeShape.requiresEvaluation(addedStatements, removedStatements);
	}

	public String getPlanAsGraphvizDot(PlanNode planNode, ConnectionsGroup connectionsGroup) {

		StringBuilder stringBuilder = new StringBuilder("Graphviz DOT output:\n\n");

		stringBuilder.append("digraph  {").append("\n");
		stringBuilder.append("labelloc=t;\nfontsize=30;\nlabel=\"" + this.getClass().getSimpleName() + "\";")
				.append("\n");

		stringBuilder.append(System.identityHashCode(connectionsGroup)
				+ " [label=\"Base sail\" nodeShape=pentagon fillcolor=lightblue style=filled];").append("\n");
		stringBuilder
				.append(System.identityHashCode(connectionsGroup.getPreviousStateConnection())
						+ " [label=\"Previous state connection\" nodeShape=pentagon fillcolor=lightblue style=filled];")
				.append("\n");

		MemoryStore addedStatements = ((MemoryStoreConnection) connectionsGroup.getAddedStatements()).getSail();
		MemoryStore removedStatements = ((MemoryStoreConnection) connectionsGroup.getRemovedStatements()).getSail();

		stringBuilder
				.append(System.identityHashCode(addedStatements)
						+ " [label=\"Added statements\" nodeShape=pentagon fillcolor=lightblue style=filled];")
				.append("\n");
		stringBuilder
				.append(System.identityHashCode(removedStatements)
						+ " [label=\"Removed statements\" nodeShape=pentagon fillcolor=lightblue style=filled];")
				.append("\n");

		planNode.getPlanAsGraphvizDot(stringBuilder);

		stringBuilder.append("}").append("\n");

		return stringBuilder.append("\n\n").toString();

	}

	static List<Value> toList(SailRepositoryConnection connection, Resource orList) {
		List<Value> ret = new ArrayList<>();
		while (!orList.equals(RDF.NIL)) {
			try (Stream<Statement> stream = connection.getStatements(orList, RDF.FIRST, null).stream()) {
				Value value = stream.map(Statement::getObject).findAny().get();
				ret.add(value);
			}

			try (Stream<Statement> stream = connection.getStatements(orList, RDF.REST, null).stream()) {
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

		static List<PathPropertyShape> getPropertyShapes(Resource ShapeId, SailRepositoryConnection connection,
				NodeShape nodeShape) {

			try (Stream<Statement> stream = connection.getStatements(ShapeId, SHACL.PROPERTY, null).stream()) {
				return stream.map(Statement::getObject).map(v -> (Resource) v).flatMap(propertyShapeId -> {
					List<PathPropertyShape> propertyShapes = getPropertyShapesInner(connection, nodeShape,
							propertyShapeId,
							null);
					return propertyShapes.stream();
				}).collect(Collectors.toList());
			}

		}

		static List<PathPropertyShape> getPropertyShapesInner(SailRepositoryConnection connection, NodeShape nodeShape,
				Resource propertyShapeId, PathPropertyShape parent) {
			List<PathPropertyShape> propertyShapes = new ArrayList<>(2);

			ShaclProperties shaclProperties = new ShaclProperties(propertyShapeId, connection);

			if (shaclProperties.minCount != null) {
				propertyShapes.add(new MinCountPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.minCount));
			}
			if (shaclProperties.maxCount != null) {
				propertyShapes.add(new MaxCountPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.maxCount));
			}
			if (shaclProperties.datatype != null) {
				propertyShapes.add(new DatatypePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.datatype));
			}
			if (!shaclProperties.or.isEmpty()) {
				shaclProperties.or.forEach(or -> {
					propertyShapes.add(new OrPropertyShape(propertyShapeId, connection, nodeShape,
							shaclProperties.deactivated, parent, shaclProperties.path, or));
				});
			}
			if (shaclProperties.minLength != null) {
				propertyShapes.add(new MinLengthPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.minLength));
			}
			if (shaclProperties.maxLength != null) {
				propertyShapes.add(new MaxLengthPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.maxLength));
			}
			if (!shaclProperties.pattern.isEmpty()) {
				shaclProperties.pattern.forEach(pattern -> {
					propertyShapes.add(new PatternPropertyShape(propertyShapeId, connection, nodeShape,
							shaclProperties.deactivated, parent, shaclProperties.path, pattern, shaclProperties.flags));
				});
			}
			if (shaclProperties.languageIn != null) {
				propertyShapes.add(new LanguageInPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.languageIn));
			}
			if (shaclProperties.nodeKind != null) {
				propertyShapes.add(new NodeKindPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.nodeKind));
			}
			if (shaclProperties.minExclusive != null) {
				propertyShapes.add(new MinExclusivePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.minExclusive));
			}
			if (shaclProperties.maxExclusive != null) {
				propertyShapes.add(new MaxExclusivePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.maxExclusive));
			}
			if (shaclProperties.maxInclusive != null) {
				propertyShapes.add(new MaxInclusivePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.maxInclusive));
			}
			if (shaclProperties.minInclusive != null) {
				propertyShapes.add(new MinInclusivePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.minInclusive));
			}
			if (!shaclProperties.clazz.isEmpty()) {
				shaclProperties.clazz.forEach(clazz -> {
					propertyShapes.add(new ClassPropertyShape(propertyShapeId, connection, nodeShape,
							shaclProperties.deactivated, parent, shaclProperties.path, clazz));
				});
			}
			if (!shaclProperties.and.isEmpty()) {
				shaclProperties.and.forEach(and -> {
					propertyShapes.add(new AndPropertyShape(propertyShapeId, connection, nodeShape,
							shaclProperties.deactivated, parent, shaclProperties.path, and));
				});
			}
			if (!shaclProperties.not.isEmpty()) {
				shaclProperties.not.forEach(not -> {
					propertyShapes.add(new NotPropertyShape(propertyShapeId, connection, nodeShape,
							shaclProperties.deactivated, parent, shaclProperties.path, not));
				});
			}
			if (shaclProperties.in != null) {
				propertyShapes.add(new InPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.in));
			}
			if (shaclProperties.uniqueLang) {
				propertyShapes.add(new UniqueLangPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.deactivated, parent, shaclProperties.path, shaclProperties.uniqueLang));
			}

			return propertyShapes;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PropertyShape that = (PropertyShape) o;
		return deactivated == that.deactivated &&
				id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(deactivated, id);
	}

	protected static String toString(List<List<PathPropertyShape>> propertyShapes) {

		List<String> collect = propertyShapes.stream()
				.map(l -> Arrays.toString(l.toArray()))
				.collect(Collectors.toList());

		return Arrays.toString(collect.toArray());

	}

	String describe(SailRepositoryConnection connection, Resource resource) {
		GraphQuery graphQuery = connection.prepareGraphQuery("describe ?a where {BIND(?resource as ?a)}");
		graphQuery.setBinding("resource", resource);

		try (Stream<Statement> stream = graphQuery.evaluate().stream()) {

			LinkedHashModel statements = stream.collect(Collectors.toCollection(LinkedHashModel::new));
			statements.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);

			WriterConfig config = new WriterConfig();
			config.set(BasicWriterSettings.PRETTY_PRINT, true);
			config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

			StringWriter stringWriter = new StringWriter();

			Rio.write(statements, stringWriter, RDFFormat.TURTLE, config);

			return stringWriter.toString();
		}
	}

}
