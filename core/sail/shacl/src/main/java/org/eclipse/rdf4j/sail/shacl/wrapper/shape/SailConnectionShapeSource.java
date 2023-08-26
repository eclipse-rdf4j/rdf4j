/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.wrapper.shape;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailConnection;

public class SailConnectionShapeSource implements ShapeSource {

	private final SailConnection connection;
	private final Resource[] context;

	public SailConnectionShapeSource(SailConnection connection) {
		this(connection, null);
	}

	private SailConnectionShapeSource(SailConnection connection, Resource[] context) {
		this.connection = connection;
		this.context = context;
		assert connection.isActive();
	}

	public SailConnectionShapeSource withContext(Resource[] context) {
		return new SailConnectionShapeSource(connection, context);
	}

	@Override
	public Resource[] getActiveContexts() {
		return context;
	}

	public Stream<ShapesGraph> getAllShapeContexts() {
		assert context == null;

		Stream<ShapesGraph> rsxDataAndShapesGraphLink = ShapeSource.getRsxDataAndShapesGraphLink(connection, context);

		Stream<ShapesGraph> shapesGraphStream;
		try (Stream<? extends Statement> stream = connection.getStatements(null, SHACL.SHAPES_GRAPH, null, false)
				.stream()) {

			shapesGraphStream = stream
					.collect(Collectors.groupingBy(Statement::getSubject))
					.entrySet()
					.stream()
					.map(entry -> new ShapesGraph(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList())
					.stream();
		}

		return Stream.concat(rsxDataAndShapesGraphLink, shapesGraphStream);

	}

	private Stream<Resource> getContext(Predicates predicate) {
		assert context == null;

		return connection.getStatements(null, predicate.getIRI(), null, true)
				.stream()
				.map(Statement::getContext)
				.distinct();
	}

	public Stream<Resource> getTargetableShape() {
		assert context != null;
		return Stream
				.of(getSubjects(Predicates.TARGET_NODE), getSubjects(Predicates.TARGET_CLASS),
						getSubjects(Predicates.TARGET_SUBJECTS_OF), getSubjects(Predicates.TARGET_OBJECTS_OF),
						getSubjects(Predicates.TARGET_PROP), getSubjects(Predicates.RSX_targetShape))
				.reduce(Stream::concat)
				.get()
				.distinct();
	}

	public boolean isType(Resource subject, IRI type) {
		if (DASH_CONSTANTS.contains(subject, RDF.TYPE, type)
				|| connection.hasStatement(subject, RDF.TYPE, type, true, context)) {
			return true;
		}
		if (!(type == SHACL.NODE_SHAPE || type == SHACL.PROPERTY_SHAPE)) {
			if (type.equals(SHACL.NODE_SHAPE)) {
				type = SHACL.NODE_SHAPE;
			} else if (type.equals(SHACL.PROPERTY_SHAPE)) {
				type = SHACL.PROPERTY_SHAPE;
			}
		}

		if (type == SHACL.PROPERTY_SHAPE) {
			return connection.hasStatement(subject, SHACL.PATH, null, true, context);
		} else if (type == SHACL.NODE_SHAPE) {
			if (connection.hasStatement(subject, SHACL.PATH, null, true, context)) {
				return false;
			}
			if (connection.hasStatement(null, SHACL.NODE, subject, true, context)) {
				return true;
			}
			try (Stream<? extends Statement> stream = connection.getStatements(subject, null, null, true, context)
					.stream()) {
				return stream
						.map(Statement::getPredicate)
						.map(Value::stringValue)
						.anyMatch(predicate -> predicate.startsWith(SHACL.NAMESPACE)
								|| predicate.startsWith(DASH.NAMESPACE));
			}
		} else {
			return false;
		}
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
		return connection.getStatements(id, null, null, true, context)
				.stream()
				.map(s -> ((Statement) s))
				.map(Statements::stripContext)
				.distinct();
	}

	public Value getRdfFirst(Resource subject) {
		return ShapeSourceHelper.getFirst(connection, subject, context);
	}

	public Resource getRdfRest(Resource subject) {
		return ShapeSourceHelper.getRdfRest(connection, subject, context);
	}

	@Override
	public void close() {
		// we don't close the provided connection
	}
}
