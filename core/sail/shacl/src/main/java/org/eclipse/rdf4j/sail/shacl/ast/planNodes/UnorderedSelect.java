/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

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
	private final Function<Statement, ValidationTuple> mapper;

	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public UnorderedSelect(SailConnection connection, Resource subject, IRI predicate, Value object,
			Function<Statement, ValidationTuple> mapper) {
		this.connection = connection;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.mapper = mapper;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(subject,
					predicate, object, true);

			@Override
			public void close() throws SailException {
				statements.close();
			}

			@Override
			boolean localHasNext() throws SailException {
				return statements.hasNext();
			}

			@Override
			ValidationTuple loggingNext() throws SailException {

				return mapper.apply(statements.next());
			}

			@Override
			public void remove() throws SailException {

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
		return connection.equals(that.connection) &&
				Objects.equals(subject, that.subject) &&
				Objects.equals(predicate, that.predicate) &&
				Objects.equals(object, that.object) &&
				mapper.equals(that.mapper);
	}

	@Override
	public int hashCode() {
		return Objects.hash(connection, subject, predicate, object, mapper);
	}
}
