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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;

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
		try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(null,
				SHACL.SHAPES_GRAPH, null, false, context)) {
			if (statements == null) {
				return Stream.empty();
			}

			return statements.stream()
					.collect(Collectors.groupingBy(Statement::getSubject))
					.entrySet()
					.stream()
					.map(entry -> new ShapesGraph(entry.getKey(), entry.getValue()));
		}
	}

	public Stream<Resource> getTargetableShape() {
		assert context != null;
		Stream<Resource> inferred;

		CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(null, RDF.TYPE,
				RDFS.CLASS, true, context);
		if (statements != null) {
			inferred = statements
					.stream()
					.map(Statement::getSubject)
					.filter(this::isNodeShapeOrPropertyShape);
		} else {
			inferred = Stream.empty();
		}

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

		CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(null,
				predicate.getIRI(), null, true, context);
		if (statements == null) {
			return Stream.empty();
		}

		return statements
				.stream()
				.map(Statement::getSubject)
				.distinct();

	}

	public Stream<Value> getObjects(Resource subject, Predicates predicate) {
		assert context != null;

		CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(subject,
				predicate.getIRI(), null, true, context);
		if (statements == null) {
			return Stream.empty();
		}
		return statements
				.stream()
				.map(Statement::getObject)
				.distinct();
	}

	public Stream<Statement> getAllStatements(Resource id) {
		assert context != null;

		Stream<Statement> backwardsChained = DASH_CONSTANTS.stream();

		if (connection.hasStatement(id, SHACL.PATH, null, true, context)) {
			backwardsChained = Stream.concat(backwardsChained,
					Stream.of(Statements.statement(id, RDF.TYPE, SHACL.PROPERTY_SHAPE, null)));
		}

		if (connection.hasStatement(id, RDF.TYPE, RDFS.CLASS, true, context) && isNodeShapeOrPropertyShape(id)) {
			backwardsChained = Stream.concat(backwardsChained,
					Stream.of(Statements.statement(id, SHACL.TARGET_CLASS, id, null)));
		}

		CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(id, null, null,
				true, context);
		if (statements == null) {
			return backwardsChained;
		}

		return Stream.concat(statements.stream().map(s -> ((Statement) s)),
				backwardsChained);
	}

	public Value getRdfFirst(Resource subject) {
		assert context != null;

		try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(subject,
				RDF.FIRST, null, true, context)) {
			if (statements == null) {
				return null;
			}

			return statements.stream().map(Statement::getObject).findAny().orElse(null);

		}
		// .orElseThrow(() -> new IllegalStateException("Corrupt rdf:list at rdf:first: " + subject));
	}

	public Resource getRdfRest(Resource subject) {
		assert context != null;
		try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(subject,
				RDF.REST, null, true, context)) {
			if (statements == null) {
				return null;
			}
			return (Resource) statements.stream().map(Statement::getObject).findAny().orElse(null);
		}
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
