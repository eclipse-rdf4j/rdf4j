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
import org.elasticsearch.client.Client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ReadCommittedWrapper extends DataStructureInterface {

	private DataStructureInterface dataStructure;

	private Set<Statement> internalAdded = new HashSet<>(1000);
	private Set<Statement> internalRemoved = new HashSet<>(100);

	ReadCommittedWrapper(DataStructureInterface dataStructure) {
		this.dataStructure = dataStructure;
	}

	@Override
	public void addStatement(Client client, Statement statement) {
		internalAdded.add(statement);
		internalRemoved.remove(statement);
	}

	@Override
	public void removeStatement(Client client, Statement statement) {
		internalRemoved.add(statement);
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Client client, Resource subject,
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
					return dataStructure.getStatements(client, subject, predicate, object, context);

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

				CloseableIteration<? extends Statement, SailException> right = dataStructure.getStatements(client,
						subject, predicate, object, context);

				Statement next;

				private void setNext() {

					if (next != null)
						return;

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

	@Override
	public void flushThrough(Client client) {

		internalAdded
				.stream()
				.filter(statement -> !internalRemoved.contains(statement))
				.forEach(statement -> dataStructure.addStatement(client, statement));

		internalRemoved.forEach(statement -> dataStructure.removeStatement(client, statement));

		internalAdded = new HashSet<>(internalAdded.size());
		internalRemoved = new HashSet<>(internalRemoved.size());

		dataStructure.flush(client);

	}

	@Override
	public void setElasticsearchScrollTimeout(int timeout) {
		dataStructure.setElasticsearchScrollTimeout(timeout);
	}

	@Override
	public void flush(Client client) {
	}

	@Override
	String getHostname() {
		return dataStructure.getHostname();
	}

	@Override
	int getPort() {
		return dataStructure.getPort();
	}

	@Override
	String getClustername() {
		return dataStructure.getClustername();
	}

	@Override
	void init() {
		dataStructure.init();
	}

	@Override
	public void clear(Client client, Resource[] contexts) {

		try (CloseableIteration<? extends Statement, SailException> statements = getStatements(client, null, null, null,
				contexts)) {
			while (statements.hasNext()) {
				removeStatement(client, statements.next());
			}
		}

	}
}
