/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.wrapper.shape;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailConnection;

public class BackwardChainingShapeSource implements ShapeSource {

	private final SailConnection connection;
	private final Resource[] context;

	public BackwardChainingShapeSource(SailConnection connection) {
		this(connection, null);
	}

	private BackwardChainingShapeSource(SailConnection connection, Resource[] context) {
		this.connection = connection;
		this.context = context;
		assert connection.isActive();
	}

	public BackwardChainingShapeSource withContext(Resource[] context) {
		return new BackwardChainingShapeSource(connection, context);
	}

	@Override
	public Resource[] getActiveContexts() {
		return context;
	}

	public Stream<ShapesGraph> getAllShapeContexts() {
		assert context != null;
		try (Stream<? extends Statement> stream = connection
				.getStatements(null, SHACL.SHAPES_GRAPH, null, false, context)
				.stream()) {

			return stream
					.collect(Collectors.groupingBy(Statement::getSubject))
					.entrySet()
					.stream()
					.map(entry -> new ShapeSource.ShapesGraph(entry.getKey(), entry.getValue()));
		}

	}

	public Stream<Resource> getTargetableShape() {
		assert context != null;

		Stream<Resource> inferred = connection.getStatements(null, RDF.TYPE, RDFS.CLASS, true, context)
				.stream()
				.map(Statement::getSubject)
				.filter(this::isNodeShapeOrPropertyShape);

		return Stream
				.of(getSubjects(Predicates.TARGET_NODE), getSubjects(Predicates.TARGET_CLASS),
						getSubjects(Predicates.TARGET_SUBJECTS_OF), getSubjects(Predicates.TARGET_OBJECTS_OF),
						getSubjects(Predicates.TARGET_PROP), getSubjects(Predicates.RSX_targetShape), inferred)
				.reduce(Stream::concat)
				.get()
				.distinct();
	}

	private boolean isNodeShapeOrPropertyShape(Resource id) {
		return connection.hasStatement(id, RDF.TYPE, SHACL.NODE_SHAPE, true, context)
				|| connection.hasStatement(id, RDF.TYPE, SHACL.PROPERTY_SHAPE, true, context);
	}

	public Stream<Resource> getSubjects(Predicates predicate) {
		assert context != null;

		return connection.getStatements(null, predicate.getIRI(), null, true, context)
				.stream()
				.map(Statement::getSubject)
				.distinct();

	}

	public Stream<Value> getObjects(Resource subject, Predicates predicate) {
		assert context != null;

		return connection.getStatements(subject, predicate.getIRI(), null, true, context)
				.stream()
				.map(Statement::getObject)
				.distinct();
	}

	public Stream<Statement> getAllStatements(Resource id) {
		assert context != null;

		Stream<Statement> backwardsChained = DASH_CONSTANTS.stream();

		if (connection.hasStatement(id, SHACL.PATH, null, true, context)) {
			backwardsChained = Stream.concat(
					backwardsChained,
					Stream.of(Statements.statement(id, RDF.TYPE, SHACL.PROPERTY_SHAPE, null))
			);
		}

		if (connection.hasStatement(id, RDF.TYPE, RDFS.CLASS, true, context) &&
				isNodeShapeOrPropertyShape(id)) {
			backwardsChained = Stream.concat(
					backwardsChained,
					Stream.of(Statements.statement(id, SHACL.TARGET_CLASS, id, null))
			);
		}

		return Stream.concat(
				connection.getStatements(id, null, null, true, context).stream().map(s -> ((Statement) s)),
				backwardsChained
		);
	}

	public Value getRdfFirst(Resource subject) {
		return ShapeSourceHelper.getFirst(connection, subject, context);
	}

	public Resource getRdfRest(Resource subject) {
		return ShapeSourceHelper.getRdfRest(connection, subject, context);
	}

	public boolean isType(Resource subject, IRI type) {
		assert context != null;
		return DASH_CONSTANTS.contains(subject, RDF.TYPE, type)
				|| connection.hasStatement(subject, RDF.TYPE, type, true, context);
	}

	@Override
	public void close() {
		// we don't close the provided connection
	}
}
