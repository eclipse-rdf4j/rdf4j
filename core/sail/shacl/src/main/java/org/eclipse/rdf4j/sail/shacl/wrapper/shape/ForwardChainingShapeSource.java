/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.wrapper.shape;

import static org.eclipse.rdf4j.model.util.Values.iri;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class ForwardChainingShapeSource implements ShapeSource {

	// SHACL Vocabulary from W3C - https://www.w3.org/ns/shacl.ttl
	private final static IRI shaclVocabularyGraph = iri(RDF4J.NAMESPACE, "shaclVocabularyGraph");
	private final static SchemaCachingRDFSInferencer shaclVocabulary = createShaclVocabulary();
	private static final Resource[] defaultContext = { RDF4J.SHACL_SHAPE_GRAPH };

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

	private ForwardChainingShapeSource(Repository repository, RepositoryConnection connection,
			Resource[] context) {
		this.connection = connection;
		this.context = context;
		this.repository = repository;

	}

	private SailRepository forwardChain(RepositoryConnection shapesRepoConnection) {
		SailRepository shapesRepoWithReasoning = new SailRepository(
				SchemaCachingRDFSInferencer.fastInstantiateFrom(shaclVocabulary, new MemoryStore(), false));
		shapesRepoWithReasoning.init();

		try (var shapesRepoWithReasoningConnection = shapesRepoWithReasoning.getConnection()) {

			shapesRepoWithReasoningConnection.begin(IsolationLevels.NONE);

			try (var statements = shapesRepoConnection.getStatements(null, null, null, false)) {
				shapesRepoWithReasoningConnection.add(statements);
			}

			enrichShapes(shapesRepoWithReasoningConnection);
			shapesRepoWithReasoningConnection.commit();
		}

		return shapesRepoWithReasoning;
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

		// TODO: We need to handle DASH_CONSTANTS for the other shape graphs too!

		shaclSailConnection.add(DASH_CONSTANTS, RDF4J.SHACL_SHAPE_GRAPH);
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
		if (connection.hasStatement(null, null, null, false, RDF4J.SHACL_SHAPE_GRAPH)) {
			return Stream.of(new ShapesGraph(RDF4J.SHACL_SHAPE_GRAPH));
		}
		return Stream.empty();

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
		return connection.hasStatement(subject, RDF.TYPE, type, true, context);
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
