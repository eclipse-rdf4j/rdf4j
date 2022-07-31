/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.support;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class UpdateWithModelBuilder {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final RepositoryConnection con;
	private final ModelBuilder modelBuilder;

	public UpdateWithModelBuilder(RepositoryConnection con) {
		this.con = con;
		this.modelBuilder = new ModelBuilder();
	}

	public UpdateWithModelBuilder setNamespace(Namespace ns) {
		modelBuilder.setNamespace(ns);
		return this;
	}

	public UpdateWithModelBuilder setNamespace(String prefix, String namespace) {
		modelBuilder.setNamespace(prefix, namespace);
		return this;
	}

	public UpdateWithModelBuilder subject(Resource subject) {
		modelBuilder.subject(subject);
		return this;
	}

	public UpdateWithModelBuilder subject(String prefixedNameOrIri) {
		modelBuilder.subject(prefixedNameOrIri);
		return this;
	}

	public UpdateWithModelBuilder namedGraph(Resource namedGraph) {
		modelBuilder.namedGraph(namedGraph);
		return this;
	}

	public UpdateWithModelBuilder namedGraph(String prefixedNameOrIRI) {
		modelBuilder.namedGraph(prefixedNameOrIRI);
		return this;
	}

	public UpdateWithModelBuilder defaultGraph() {
		modelBuilder.defaultGraph();
		return this;
	}

	public UpdateWithModelBuilder addMaybe(Resource subject, IRI predicate, Object object) {
		if (ObjectUtils.allNotNull(subject, predicate, object)) {
			return add(subject, predicate, object);
		}
		return this;
	}

	public UpdateWithModelBuilder add(Resource subject, IRI predicate, Object object) {
		modelBuilder.add(subject, predicate, object);
		return this;
	}

	public UpdateWithModelBuilder addMaybe(String subject, IRI predicate, Object object) {
		if (ObjectUtils.allNotNull(subject, predicate, object)) {
			return add(subject, predicate, object);
		}
		return this;
	}

	public UpdateWithModelBuilder add(String subject, IRI predicate, Object object) {
		modelBuilder.add(subject, predicate, object);
		return this;
	}

	public UpdateWithModelBuilder addMaybe(String subject, String predicate, Object object) {
		if (ObjectUtils.allNotNull(subject, predicate, object)) {
			return add(subject, predicate, object);
		}
		return this;
	}

	public UpdateWithModelBuilder add(String subject, String predicate, Object object) {
		modelBuilder.add(subject, predicate, object);
		return this;
	}

	public UpdateWithModelBuilder addMaybe(IRI predicate, Object object) {
		if (ObjectUtils.allNotNull(predicate, object)) {
			return add(predicate, object);
		}
		return this;
	}

	public UpdateWithModelBuilder add(IRI predicate, Object object) {
		modelBuilder.add(predicate, object);
		return this;
	}

	public UpdateWithModelBuilder addMaybe(String predicate, Object object) {
		if (ObjectUtils.allNotNull(predicate, object)) {
			return add(predicate, object);
		}
		return this;
	}

	public UpdateWithModelBuilder add(String predicate, Object object) {
		modelBuilder.add(predicate, object);
		return this;
	}

	public void acceptConnection(Consumer<RepositoryConnection> connectionConsumer) {
		connectionConsumer.accept(this.con);
	}

	public <T> T applyToConnection(Function<RepositoryConnection, T> function) {
		return function.apply(con);
	}

	public BNode createBNode() {
		return con.getValueFactory().createBNode();
	}

	public UpdateWithModelBuilder withSink(Consumer<Collection<Statement>> consumer) {
		List<Statement> sink = new ArrayList<>();
		consumer.accept(sink);
		if (sink.size() > 0) {
			sink.stream()
					.forEach(
							s -> modelBuilder.add(s.getSubject(), s.getPredicate(), s.getObject()));
		}
		return this;
	}

	public void execute() {
		Model model = modelBuilder.build();
		if (logger.isDebugEnabled()) {
			StringWriter sw = new StringWriter();
			Rio.write(model, sw, RDFFormat.TURTLE);
			logger.debug("adding the following triples:\n{}", sw.toString());
		}
		con.add(model);
	}
}
