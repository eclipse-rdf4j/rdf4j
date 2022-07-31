/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class UnorderedSelect implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(UnorderedSelect.class);

	private final SailConnection connection;

	private final Resource subject;
	private final IRI predicate;
	private final Value object;
	private final Resource[] dataGraph;
	private final Function<Statement, ValidationTuple> mapper;

	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public UnorderedSelect(SailConnection connection, Resource subject, IRI predicate, Value object,
			Resource[] dataGraph, Function<Statement, ValidationTuple> mapper) {
		this.connection = connection;
		assert this.connection != null;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.dataGraph = dataGraph;
		this.mapper = mapper;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(subject,
					predicate, object, true, dataGraph);

			@Override
			public void localClose() throws SailException {
				statements.close();
			}

			@Override
			protected boolean localHasNext() throws SailException {
				return statements.hasNext();
			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {

				return mapper.apply(statements.next());
			}

		};
	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");

		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection) {
			stringBuilder
					.append(System.identityHashCode(((MemoryStoreConnection) connection).getSail()) + " -> " + getId())
					.append("\n");
		} else {
			stringBuilder.append(System.identityHashCode(connection) + " -> " + getId()).append("\n");
		}

	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "UnorderedSelect{" +
				"subject=" + Formatter.prefix(subject) +
				", predicate=" + Formatter.prefix(predicate) +
				", object=" + Formatter.prefix(object) +
				'}';
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
	}

	@Override
	public boolean producesSorted() {
		return false;
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UnorderedSelect that = (UnorderedSelect) o;

		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection && that.connection instanceof MemoryStoreConnection) {
			return ((MemoryStoreConnection) connection).getSail()
					.equals(((MemoryStoreConnection) that.connection).getSail()) &&
					Objects.equals(subject, that.subject) &&
					Objects.equals(predicate, that.predicate) &&
					Objects.equals(object, that.object) &&
					Arrays.equals(dataGraph, that.dataGraph) &&
					mapper.equals(that.mapper);
		} else {
			return Objects.equals(connection, that.connection) &&
					Objects.equals(subject, that.subject) &&
					Objects.equals(predicate, that.predicate) &&
					Objects.equals(object, that.object) &&
					Arrays.equals(dataGraph, that.dataGraph) &&
					mapper.equals(that.mapper);
		}

	}

	@Override
	public int hashCode() {
		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(((MemoryStoreConnection) connection).getSail(), subject, predicate, object, mapper,
					Arrays.hashCode(dataGraph));
		}

		return Objects.hash(connection, subject, predicate, object, mapper, Arrays.hashCode(dataGraph));
	}

	public static class Mapper {
		public static class SubjectScopedMapper implements Function<Statement, ValidationTuple> {

			private final ConstraintComponent.Scope scope;

			private SubjectScopedMapper(ConstraintComponent.Scope scope) {
				this.scope = scope;
			}

			static SubjectScopedMapper nodeShapeInstance = new SubjectScopedMapper(ConstraintComponent.Scope.nodeShape);
			static SubjectScopedMapper propertyShapeInstance = new SubjectScopedMapper(
					ConstraintComponent.Scope.propertyShape);
			static SubjectScopedMapper noneInstance = new SubjectScopedMapper(ConstraintComponent.Scope.none);

			@Override
			public ValidationTuple apply(Statement s) {
				return new ValidationTuple(s.getSubject(), scope, false, s.getContext());
			}

			@Override
			public int hashCode() {
				return 72706357 + scope.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				return obj == this;
			}

			public static SubjectScopedMapper getFunction(ConstraintComponent.Scope scope) {
				switch (scope) {

				case none:
					return noneInstance;
				case nodeShape:
					return nodeShapeInstance;
				case propertyShape:
					return propertyShapeInstance;
				}

				throw new IllegalStateException("Unknown scope: " + scope);
			}

		}

		public static class ObjectScopedMapper implements Function<Statement, ValidationTuple> {

			private final ConstraintComponent.Scope scope;

			private ObjectScopedMapper(ConstraintComponent.Scope scope) {
				this.scope = scope;
			}

			static ObjectScopedMapper nodeShapeInstance = new ObjectScopedMapper(ConstraintComponent.Scope.nodeShape);
			static ObjectScopedMapper propertyShapeInstance = new ObjectScopedMapper(
					ConstraintComponent.Scope.propertyShape);
			static ObjectScopedMapper noneInstance = new ObjectScopedMapper(ConstraintComponent.Scope.none);

			@Override
			public ValidationTuple apply(Statement s) {
				return new ValidationTuple(s.getObject(), scope, false, s.getContext());
			}

			@Override
			public int hashCode() {
				return 25482634 + scope.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				return obj == this;
			}

			public static ObjectScopedMapper getFunction(ConstraintComponent.Scope scope) {
				switch (scope) {

				case none:
					return noneInstance;
				case nodeShape:
					return nodeShapeInstance;
				case propertyShape:
					return propertyShapeInstance;
				}

				throw new IllegalStateException("Unknown scope: " + scope);
			}

		}

		public static class SubjectObjectPropertyShapeMapper implements Function<Statement, ValidationTuple> {

			private SubjectObjectPropertyShapeMapper() {
			}

			static SubjectObjectPropertyShapeMapper instance = new SubjectObjectPropertyShapeMapper();

			@Override
			public ValidationTuple apply(Statement s) {
				return new ValidationTuple(s.getSubject(), s.getObject(), ConstraintComponent.Scope.propertyShape,
						true, s.getContext());
			}

			@Override
			public int hashCode() {
				return 35972357;
			}

			@Override
			public boolean equals(Object obj) {
				return obj == this;
			}

			public static SubjectObjectPropertyShapeMapper getFunction() {
				return instance;
			}

		}
	}
}
