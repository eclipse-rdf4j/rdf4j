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
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.base.AbstractStatement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An {@link Operation} that holds a {@link Model} internally and exposes a {@link ModelBuilder} for adding to it.
 * Moreover it allows for deleting statements.
 * </p>
 * <p>
 * Thus, the class provides a way of configuring an update to the repository incrementally, and no repository access
 * happens until {@link #execute()} is called. (unless the client uses {@link #applyToConnection(Function)} and accesses
 * the repository that way.)
 * </p>
 * Removing statements via {@link #remove} will remove them from the repository when {@link #execute()} is called;
 * moreover, the statements will also be removed from the model at the time of the {@link #remove} call, such that a
 * subsequent creation of some of the deleted statements to the model will result in those triples being first deleted
 * and then added to the repository when {@link #execute()} is called.
 *
 * @author Florian Kleedorfer
 * @since 4.0.0
 */
public class UpdateWithModelBuilder {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final RepositoryConnection con;

	/** the model builder being exposed to clients */
	private final ModelBuilder modelBuilder;
	/** the model being built by the modelBuilder, and that is going to be added to the repository eventually */
	private final Model addModel;

	/**
	 * Set of Statements to be removed from the repository eventually. The Statement implementation used here is the
	 * {@link WildcardAllowingStatement}, which allows for using wildcards for deletion
	 */
	private final Set<Statement> removeStatements;

	public UpdateWithModelBuilder(RepositoryConnection con) {
		this.con = con;
		this.addModel = new LinkedHashModel();
		this.removeStatements = new HashSet<>();
		this.modelBuilder = new ModelBuilder(addModel);
	}

	public static UpdateWithModelBuilder fromTemplate(RDF4JTemplate template) {
		return template.applyToConnection(con -> new UpdateWithModelBuilder(con));
	}

	/**
	 * Will remove statements upon update execution, before processing any additions. Statements that are removed here
	 * are also removed from the #addModel at the time of this call (not upon update execution)
	 *
	 * <p>
	 * The semantics of {@link RepositoryConnection#remove(Iterable, Resource...)} apply, i.e. the resource(s) specified
	 * here are used there, if any.
	 *
	 * @param subject   the subject, or null to match any resource
	 * @param predicate the predicate, or null to match any IRI
	 * @param object    the object, or null to match any value
	 * @param resources the context(s), if any
	 * @return this builder
	 */
	public UpdateWithModelBuilder remove(
			Resource subject, IRI predicate, Value object, Resource... resources) {
		addModel.remove(subject, predicate, object, resources);
		if (resources.length == 0) {
			removeStatements.add(new WildcardAllowingStatement(subject, predicate, object, null));
		} else {
			for (int i = 0; i < resources.length; i++) {
				removeStatements.add(
						new WildcardAllowingStatement(subject, predicate, object, resources[i]));
			}
		}
		return this;
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
		if (!sink.isEmpty()) {
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
			Rio.write(this.removeStatements, sw, RDFFormat.TURTLE);
			logger.debug("removing the following triples:\n{}", sw.toString());
			sw = new StringWriter();
			Rio.write(model, sw, RDFFormat.TURTLE);
			logger.debug("adding the following triples:\n{}", sw.toString());
		}
		con.remove(this.removeStatements);
		con.add(this.addModel);
	}

	static class WildcardAllowingStatement extends AbstractStatement {
		private static final long serialVersionUID = -4116676621136121342L;
		private final Resource subject;
		private final IRI predicate;
		private final Value object;
		private final Resource context;

		WildcardAllowingStatement(Resource subject, IRI predicate, Value object, Resource context) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
			this.context = context;
		}

		public Resource getSubject() {
			return this.subject;
		}

		public IRI getPredicate() {
			return this.predicate;
		}

		public Value getObject() {
			return this.object;
		}

		public Resource getContext() {
			return this.context;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			WildcardAllowingStatement that = (WildcardAllowingStatement) o;
			return Objects.equals(getSubject(), that.getSubject())
					&& Objects.equals(getPredicate(), that.getPredicate())
					&& Objects.equals(getObject(), that.getObject())
					&& Objects.equals(getContext(), that.getContext());
		}

		@Override
		public int hashCode() {
			return Objects.hash(
					super.hashCode(), getSubject(), getPredicate(), getObject(), getContext());
		}
	}
}
