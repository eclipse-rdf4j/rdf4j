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

import static org.eclipse.rdf4j.model.util.Values.iri;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencerConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class ForwardChainingShapeSource implements ShapeSource {

	// SHACL Vocabulary from W3C - https://www.w3.org/ns/shacl.ttl
	private final static IRI shaclVocabularyGraph = iri(RDF4J.NAMESPACE, "shaclVocabularyGraph");
	private final static SchemaCachingRDFSInferencer shaclVocabulary = createShaclVocabulary();

	private final RepositoryConnection connection;
	private final Resource[] context;
	private final Repository repository;

	public ForwardChainingShapeSource(RepositoryConnection connection) {
		this.context = null;

		assert connection.isActive();

		repository = forwardChain(connection);
		this.connection = repository.getConnection();
		this.connection.begin(IsolationLevels.NONE);

	}

	public ForwardChainingShapeSource(SailConnection connection) {
		this.context = null;

		assert connection.isActive();

		repository = forwardChain(connection);
		this.connection = repository.getConnection();
		this.connection.begin(IsolationLevels.NONE);

	}

	private ForwardChainingShapeSource(Repository repository, RepositoryConnection connection,
			Resource[] context) {
		this.connection = connection;
		this.context = context;
		this.repository = repository;

	}

	private SailRepository forwardChain(RepositoryConnection shapesRepoConnection) {
		try (var statements = shapesRepoConnection.getStatements(null, null, null, false)) {
			if (!statements.hasNext()) {
				return new SailRepository(new MemoryStore());
			}

			SailRepository shapesRepoWithReasoning = new SailRepository(
					SchemaCachingRDFSInferencer.fastInstantiateFrom(shaclVocabulary, new MemoryStore(), false));

			try (var shapesRepoWithReasoningConnection = shapesRepoWithReasoning.getConnection()) {
				shapesRepoWithReasoningConnection.begin(IsolationLevels.NONE);

				shapesRepoWithReasoningConnection.add(statements);
				enrichShapes(shapesRepoWithReasoningConnection);

				shapesRepoWithReasoningConnection.commit();
			}

			return shapesRepoWithReasoning;

		}
	}

	private SailRepository forwardChain(SailConnection shapesRepoConnection) {
		try (var statements = shapesRepoConnection.getStatements(null, null, null, false)) {
			if (!statements.hasNext()) {
				return new SailRepository(new MemoryStore());
			}

			SailRepository shapesRepoWithReasoning = new SailRepository(
					SchemaCachingRDFSInferencer.fastInstantiateFrom(shaclVocabulary, new MemoryStore(), false));

			try (var shapesRepoWithReasoningConnection = shapesRepoWithReasoning.getConnection()) {
				shapesRepoWithReasoningConnection.begin(IsolationLevels.NONE);

				shapesRepoWithReasoningConnection.add(statements);
				enrichShapes(shapesRepoWithReasoningConnection);

				shapesRepoWithReasoningConnection.commit();
			}

			return shapesRepoWithReasoning;

		}
	}

	private static SchemaCachingRDFSInferencer createShaclVocabulary() {
		try (InputStream in = getResourceAsStream("shacl-sparql-inference/shaclVocabulary.ttl")) {
			SchemaCachingRDFSInferencer schemaCachingRDFSInferencer = new SchemaCachingRDFSInferencer(
					new MemoryStore());
			try (var connection = schemaCachingRDFSInferencer.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				Model model = Rio.parse(in, "", RDFFormat.TURTLE);
				model.forEach(s -> connection.addStatement(s.getSubject(), s.getPredicate(), s.getObject(),
						shaclVocabularyGraph));
				connection.commit();
			}

			// warm up the fast instantiation
			SchemaCachingRDFSInferencer fastInstantiated = SchemaCachingRDFSInferencer
					.fastInstantiateFrom(schemaCachingRDFSInferencer, new MemoryStore());
			try (SchemaCachingRDFSInferencerConnection connection = fastInstantiated.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.commit();
			} finally {
				fastInstantiated.shutDown();
			}

			return schemaCachingRDFSInferencer;
		} catch (IOException e) {
			throw new IllegalStateException("Resource could not be read: shacl-sparql-inference/shaclVocabulary.ttl",
					e);
		}
	}

	private static InputStream getResourceAsStream(String filename) {
		InputStream resourceAsStream = ForwardChainingShapeSource.class.getClassLoader().getResourceAsStream(filename);
		if (resourceAsStream == null) {
			throw new IllegalStateException("Resource could not be found: " + filename);
		}
		return new BufferedInputStream(resourceAsStream);
	}

	private void enrichShapes(RepositoryConnection shaclSailConnection) {

		// performance optimisation, running the queries below is time-consuming, even if the repo is empty
		if (shaclSailConnection.isEmpty()) {
			return;
		}

		shaclSailConnection.add(DASH_CONSTANTS, RDF4J.SHACL_SHAPE_GRAPH);
		shaclSailConnection.add(DASH_CONSTANTS);
		shaclSailConnection.add(DASH_CONSTANTS, new Resource[] { null });

		try (Stream<Statement> stream = shaclSailConnection
				.getStatements(null, SHACL.SHAPES_GRAPH, null, false)
				.stream()) {

			stream.forEach(s -> {
				shaclSailConnection.add(DASH_CONSTANTS, ((IRI) s.getObject()));
			});

		}

		implicitTargetClass(shaclSailConnection);

	}

	private void implicitTargetClass(RepositoryConnection shaclSailConnection) {
		try (var stream = shaclSailConnection.getStatements(null, RDF.TYPE, RDFS.CLASS, false).stream()) {
			stream
					.map(Statement::getSubject)
					.filter(s ->

					shaclSailConnection.hasStatement(s, RDF.TYPE, SHACL.NODE_SHAPE, true)
							|| shaclSailConnection.hasStatement(s, RDF.TYPE, SHACL.PROPERTY_SHAPE, true)
					)
					.forEach(s -> {
						// TODO: This only works for the MemoryStore where we store the shape and not for other graphs
						shaclSailConnection.add(s, SHACL.TARGET_CLASS, s, RDF4J.SHACL_SHAPE_GRAPH);
					});
		}
	}

	public ForwardChainingShapeSource withContext(Resource[] context) {
		return new ForwardChainingShapeSource(repository, connection, context);
	}

	@Override
	public Resource[] getActiveContexts() {
		return context;
	}

	public Stream<ShapesGraph> getAllShapeContexts() {
		assert context != null;

		if (!connection.hasStatement(null, SHACL.SHAPES_GRAPH, null, false)) {
			return Stream.of(new ShapesGraph(RDF4J.SHACL_SHAPE_GRAPH));
		}

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
		return Stream
				.of(getSubjects(Predicates.TARGET_NODE),
						getSubjects(Predicates.TARGET_CLASS),
						getSubjects(Predicates.TARGET_SUBJECTS_OF),
						getSubjects(Predicates.TARGET_OBJECTS_OF),
						getSubjects(Predicates.TARGET_PROP),
						getSubjects(Predicates.RSX_targetShape)
				)
				.reduce(Stream::concat)
				.get()
				.distinct();
	}

	public boolean isType(Resource subject, IRI type) {
		assert context != null;
		if (connection.hasStatement(subject, RDF.TYPE, type, true, context)) {
			return true;
		}
		if (type.equals(SHACL.NODE_SHAPE)) {
			if (connection.hasStatement(subject, RDF.TYPE, SHACL.SHAPE, true, context)) {
				return true;
			}
			try (Stream<Statement> stream = connection.getStatements(subject, null, null, true, context).stream()) {
				return stream
						.map(Statement::getPredicate)
						.map(Value::stringValue)
						.anyMatch(predicate -> predicate.startsWith(SHACL.NAMESPACE)
								|| predicate.startsWith(DASH.NAMESPACE));
			}
		}
		return false;
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
		return connection.getStatements(id, null, null, true, context).stream();
	}

	public Value getRdfFirst(Resource subject) {
		return ShapeSourceHelper.getFirst(connection, subject, context);
	}

	public Resource getRdfRest(Resource subject) {
		return ShapeSourceHelper.getRdfRest(connection, subject, context);
	}

	@Override
	public void close() {
		connection.commit();
		connection.close();
		repository.shutDown();

	}
}
