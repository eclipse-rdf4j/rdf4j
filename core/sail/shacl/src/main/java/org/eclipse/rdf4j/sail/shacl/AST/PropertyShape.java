/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.Stats;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;

/**
 * The AST (Abstract Syntax Tree) node that represents a property nodeShape without any restrictions. This node should
 * be extended by other nodes.
 *
 * @author Heshan Jayasinghe
 * @author Håvard M. Ottestad
 */
public abstract class PropertyShape implements PlanGenerator, RequiresEvalutation {

	boolean deactivated;
	final Resource id;

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
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements, Stats stats) {
		if (deactivated) {
			return false;
		}

		return nodeShape.requiresEvaluation(addedStatements, removedStatements, stats);
	}

	public String getPlanAsGraphvizDot(PlanNode planNode, ConnectionsGroup connectionsGroup) {

		StringBuilder stringBuilder = new StringBuilder("Graphviz DOT output:\n\n");

		stringBuilder.append("digraph  {").append("\n");
		stringBuilder.append("labelloc=t;\nfontsize=30;\nlabel=\"" + this.getClass().getSimpleName() + "\";")
				.append("\n");

		stringBuilder.append(System.identityHashCode(connectionsGroup.getBaseConnection())
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

	static Set<Value> toSet(SailRepositoryConnection connection, Resource orList) {
		Set<Value> ret = new HashSet<>();
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

	public String buildSparqlValidNodes(String targetVar) {
		return "";
	}

	public Stream<StatementPattern> getStatementPatterns() {
		return Stream.empty();
	}

	public static class Factory {

		public static List<PathPropertyShape> getPropertyShapes(Resource ShapeId, SailRepositoryConnection connection,
				NodeShape nodeShape, ShaclSail shaclSail) {

			try (Stream<Statement> stream = connection.getStatements(ShapeId, SHACL.PROPERTY, null).stream()) {
				return stream.map(Statement::getObject).map(v -> (Resource) v).flatMap(propertyShapeId -> {
					List<PathPropertyShape> propertyShapes = getPropertyShapesInner(connection, nodeShape,
							propertyShapeId,
							null, shaclSail);
					return propertyShapes.stream();
				}).collect(Collectors.toList());
			}

		}

		public static List<PathPropertyShape> getPropertyShapesInner(SailRepositoryConnection connection,
				NodeShape nodeShape,
				Resource propertyShapeId, PathPropertyShape parent, ShaclSail shaclSail) {
			List<PathPropertyShape> propertyShapes = new ArrayList<>(2);

			ShaclProperties shaclProperties = new ShaclProperties(propertyShapeId, connection);

			if (shaclProperties.getMinCount() != null && shaclProperties.getMinCount() > 0) {
				propertyShapes.add(new MinCountPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getMinCount()));
			}
			if (shaclProperties.getMaxCount() != null) {
				propertyShapes.add(new MaxCountPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getMaxCount()));
			}
			if (shaclProperties.getDatatype() != null) {
				propertyShapes.add(new DatatypePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getDatatype()));
			}
			if (!shaclProperties.getOr().isEmpty()) {
				shaclProperties.getOr().forEach(or -> {
					propertyShapes.add(new OrPropertyShape(propertyShapeId, connection, nodeShape,
							shaclProperties.isDeactivated(), parent, shaclProperties.getPath(), or, shaclSail));
				});
			}
			if (shaclProperties.getMinLength() != null) {
				propertyShapes.add(new MinLengthPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getMinLength()));
			}
			if (shaclProperties.getMaxLength() != null) {
				propertyShapes.add(new MaxLengthPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getMaxLength()));
			}
			if (!shaclProperties.getPattern().isEmpty()) {
				shaclProperties.getPattern().forEach(pattern -> {
					propertyShapes.add(new PatternPropertyShape(propertyShapeId, connection, nodeShape,
							shaclProperties.isDeactivated(), parent, shaclProperties.getPath(), pattern,
							shaclProperties.getFlags()));
				});
			}
			if (shaclProperties.getLanguageIn() != null) {
				propertyShapes.add(new LanguageInPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getLanguageIn()));
			}
			if (shaclProperties.getNodeKind() != null) {
				propertyShapes.add(new NodeKindPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getNodeKind()));
			}
			if (shaclProperties.getMinExclusive() != null) {
				propertyShapes.add(new MinExclusivePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getMinExclusive()));
			}
			if (shaclProperties.getMaxExclusive() != null) {
				propertyShapes.add(new MaxExclusivePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getMaxExclusive()));
			}
			if (shaclProperties.getMaxInclusive() != null) {
				propertyShapes.add(new MaxInclusivePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getMaxInclusive()));
			}
			if (shaclProperties.getMinInclusive() != null) {
				propertyShapes.add(new MinInclusivePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getMinInclusive()));
			}
			if (!shaclProperties.getClazz().isEmpty()) {
				shaclProperties.getClazz().forEach(clazz -> {
					propertyShapes.add(new ClassPropertyShape(propertyShapeId, connection, nodeShape,
							shaclProperties.isDeactivated(), parent, shaclProperties.getPath(), clazz));
				});
			}
			if (!shaclProperties.getAnd().isEmpty()) {
				shaclProperties.getAnd().forEach(and -> {
					propertyShapes.add(new AndPropertyShape(propertyShapeId, connection, nodeShape,
							shaclProperties.isDeactivated(), parent, shaclProperties.getPath(), and, shaclSail));
				});
			}
			if (!shaclProperties.getNot().isEmpty()) {
				shaclProperties.getNot().forEach(not -> {
					propertyShapes.add(new NotPropertyShape(propertyShapeId, connection, nodeShape,
							shaclProperties.isDeactivated(), parent, shaclProperties.getPath(), not, shaclSail));
				});
			}
			if (shaclProperties.getIn() != null) {
				propertyShapes.add(new InPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(), shaclProperties.getIn()));
			}
			if (shaclProperties.isUniqueLang()) {
				propertyShapes.add(new UniqueLangPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.isUniqueLang()));
			}
			if (shaclProperties.getHasValue() != null) {
				propertyShapes.add(new HasValuePropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getHasValue()));
			}
			if (shaclProperties.getHasValueIn() != null && shaclSail.isDashDataShapes()) {
				propertyShapes.add(new HasValueInPropertyShape(propertyShapeId, connection, nodeShape,
						shaclProperties.isDeactivated(), parent, shaclProperties.getPath(),
						shaclProperties.getHasValueIn()));
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
