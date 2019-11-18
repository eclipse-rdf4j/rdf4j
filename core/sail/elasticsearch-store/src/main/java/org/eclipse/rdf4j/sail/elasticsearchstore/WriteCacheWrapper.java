/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class WriteCacheWrapper extends DataStructureInterface {

	private DataStructureInterface dataStructure;

	private Set<Statement> internalAdded = new HashSet<>();
	private Set<Statement> internalRemoved = new HashSet<>();

	WriteCacheWrapper(DataStructureInterface dataStructure) {
		this.dataStructure = dataStructure;
	}

	@Override
	synchronized public void addStatement(Statement statement) {
		if (internalAdded.size() >= ElasticsearchDataStructure.BUFFER_THRESHOLD)
			flushThrough();
		internalAdded.add(statement);
		internalRemoved.remove(statement);
	}

	@Override
	synchronized public void removeStatement(Statement statement) {
		if (internalRemoved.size() >= ElasticsearchDataStructure.BUFFER_THRESHOLD)
			flushThrough();
		internalRemoved.add(statement);
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subject,
			IRI predicate, Value object, Resource... context) {

		if (subject != null && predicate != null && object != null && context != null && context.length == 1) {
			Statement statement = SimpleValueFactory.getInstance()
					.createStatement(subject, predicate, object, context[0]);

			if (internalAdded.contains(statement)) {
				return new CloseableIteration<Statement, SailException>() {
					Statement statement = SimpleValueFactory.getInstance()
							.createStatement(subject, predicate, object, context[0]);

					@Override
					public void close() throws SailException {

					}

					@Override
					public boolean hasNext() throws SailException {
						return statement != null;
					}

					@Override
					public Statement next() throws SailException {
						Statement temp = statement;
						statement = null;

						return temp;
					}

					@Override
					public void remove() throws SailException {

					}
				};

			} else {
				if (internalRemoved.contains(statement)) {
					return new EmptyIteration<>();
				} else {
					return dataStructure.getStatements(subject, predicate, object, context);

				}

			}

		} else {

			return new CloseableIteration<Statement, SailException>() {

				Set<Statement> internalAddedLocal = new HashSet<>(internalAdded);
				Set<Statement> internalRemovedLocal = new HashSet<>(internalRemoved);

				Iterator<Statement> left = internalAddedLocal.stream()
						.filter(statement -> {

							if (subject != null && !statement.getSubject().equals(subject)) {
								return false;
							}
							if (predicate != null && !statement.getPredicate().equals(predicate)) {
								return false;
							}
							if (object != null && !statement.getObject().equals(object)) {
								return false;
							}
							if (context != null && context.length > 0
									&& !containsContext(context, statement.getContext())) {
								return false;
							}

							return true;
						})
						.iterator();

				CloseableIteration<? extends Statement, SailException> right = dataStructure.getStatements(
						subject, predicate, object, context);

				Statement next;

				private void setNext() {

					if (next != null) {
						return;
					}

					do {
						Statement tempNext = null;
						if (left.hasNext()) {
							tempNext = left.next();
						} else if (right.hasNext()) {
							tempNext = right.next();

							if (internalAddedLocal.contains(tempNext)) {
								tempNext = null;
							}
						}

						if (tempNext != null) {
							if (!internalRemovedLocal.contains(tempNext)) {
								next = tempNext;
							}
						}

					} while (next == null && (left.hasNext() || right.hasNext()));

				}

				@Override
				public void close() throws SailException {
					right.close();
				}

				@Override
				public boolean hasNext() throws SailException {
					if (next == null) {
						setNext();
					}
					return next != null;
				}

				@Override
				public Statement next() throws SailException {
					if (next == null) {
						setNext();
					}
					Statement temp = next;
					next = null;

					return temp;
				}

				@Override
				public void remove() throws SailException {

				}
			};

		}

	}

	private static boolean containsContext(Resource[] haystack, Resource needle) {
		for (Resource resource : haystack) {
			if (resource == null && needle == null) {
				return true;
			}
			if (resource != null && resource.equals(needle)) {
				return true;
			}
		}
		return false;
	}

	@Override
	synchronized public void flushThrough() {

		internalAdded
				.stream()
				.filter(statement -> !internalRemoved.contains(statement))
				.forEach(statement -> dataStructure.addStatement(statement));

		internalRemoved.forEach(statement -> dataStructure.removeStatement(statement));

		internalAdded = new HashSet<>(internalAdded.size());
		internalRemoved = new HashSet<>(internalRemoved.size());

		dataStructure.flush();

	}

	@Override
	synchronized boolean removeStatementsByQuery(Resource subject, IRI predicate, Value object, Resource[] contexts) {
		final boolean[] removed = { false };

		internalAdded.removeIf(statement -> {
			if (subject != null && !statement.getSubject().equals(subject)) {
				return false;
			}
			if (predicate != null && !statement.getPredicate().equals(predicate)) {
				return false;
			}
			if (object != null && !statement.getObject().equals(object)) {
				return false;
			}
			if (contexts != null && contexts.length > 0
					&& !containsContext(contexts, statement.getContext())) {
				return false;
			}
			removed[0] = true;

			return true;
		});

		return dataStructure.removeStatementsByQuery(subject, predicate, object, contexts) || removed[0];

	}

	@Override
	public void flush() {
	}

	@Override
	void init() {
		dataStructure.init();
	}

	@Override
	public void clear(Resource[] contexts) {

		try (CloseableIteration<? extends Statement, SailException> statements = getStatements(null, null, null,
				contexts)) {
			while (statements.hasNext()) {
				removeStatement(statements.next());
			}
		}

	}
}
