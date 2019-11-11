/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.client.Client;

import java.util.Comparator;
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
				return dataStructure.getStatements(client, subject, predicate, object, context);

			}

		} else {

			return new CloseableIteration<Statement, SailException>() {

				Iterator<Statement> left = internalAdded.stream()
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

							if (internalRemoved.contains(statement)) {
								return false;
							}

							return true;

						})
						.sorted(Comparator.comparing(Object::toString))
						.iterator();
				CloseableIteration<? extends Statement, SailException> right = dataStructure.getStatements(client,
						subject, predicate, object, context);

				Statement nextLeft;
				Statement nextRight;

				Statement next;

				private void setNext() {

					if (nextLeft == null && left.hasNext()) {
						nextLeft = left.next();
					}

					if (nextRight == null && right.hasNext()) {
						nextRight = right.next();
					}

					if (nextRight != null && nextRight.equals(nextLeft)) {
						next = nextRight;
						nextRight = null;
						nextLeft = null;
					} else if (nextLeft == null) {
						next = nextRight;
						nextRight = null;
					} else if (nextRight == null) {
						next = nextLeft;
						nextLeft = null;
					} else if (nextLeft.toString().compareTo(nextRight.toString()) > 0) {
						next = nextLeft;
						nextLeft = null;
					} else {
						next = nextRight;
						nextRight = null;
					}

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
	public void flush(Client client) {

		internalAdded
				.stream()
				.filter(statement -> !internalRemoved.contains(statement))
				.forEach(statement -> dataStructure.addStatement(client, statement));

		internalRemoved.forEach(statement -> dataStructure.removeStatement(client, statement));

		dataStructure.flush(client);

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

		// TODO

	}
}
