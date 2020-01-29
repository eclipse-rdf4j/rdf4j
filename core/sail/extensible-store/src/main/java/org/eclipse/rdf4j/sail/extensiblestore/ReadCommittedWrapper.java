/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ReadCommittedWrapper implements DataStructureInterface {

	private final DataStructureInterface dataStructure;

	private Set<Statement> internalAdded = new HashSet<>(1000);
	private Set<Statement> internalRemoved = new HashSet<>(100);

	ReadCommittedWrapper(DataStructureInterface dataStructure) {
		this.dataStructure = dataStructure;
	}

	@Override
	public void addStatement(Statement statement) {
		internalAdded.add(statement);
		internalRemoved.remove(statement);

	}

	@Override
	public void removeStatement(Statement statement) {
		internalRemoved.add(statement);

	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subject,
			IRI predicate, Value object, Resource... context) {

		// must match single statement
		if (subject != null && predicate != null && object != null && context != null && context.length == 1) {
			Statement statement = SimpleValueFactory.getInstance()
					.createStatement(subject, predicate, object, context[0]);

			if (internalAdded.contains(statement)) {
				return new SingletonIteration<>(statement);
			} else {
				if (internalRemoved.contains(statement)) {
					return new EmptyIteration<>();
				} else {
					synchronized (dataStructure) {
						return dataStructure.getStatements(subject, predicate, object, context);
					}
				}
			}

		} else {
			synchronized (dataStructure) {

				return new LookAheadIteration<Statement, SailException>() {

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

					@Override
					protected void handleClose() throws SailException {
						super.handleClose();
						right.close();
					}

					@Override
					protected Statement getNextElement() throws SailException {

						Statement next = null;

						do {
							Statement tempNext = null;
							if (left.hasNext()) {
								tempNext = left.next();
							} else if (right.hasNext()) {
								tempNext = right.next();

								if (!internalAddedLocal.isEmpty() && internalAddedLocal.contains(tempNext)) {
									tempNext = null;
								}
							}

							if (tempNext != null) {
								if (internalRemovedLocal.isEmpty() || !internalRemovedLocal.contains(tempNext)) {
									next = tempNext;
								}
							}

						} while (next == null && (left.hasNext() || right.hasNext()));

						return next;

					}

				};

			}
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
	public void flushForCommit() {

		List<Statement> internalAddedEffective = internalAdded
				.stream()
				.filter(statement -> !internalRemoved.contains(statement))
				.collect(Collectors.toList());

		synchronized (dataStructure) {
			internalAddedEffective.forEach(dataStructure::addStatement);
			internalRemoved.forEach(dataStructure::removeStatement);
			dataStructure.flushForReading();
		}

		internalAdded = new HashSet<>(internalAdded.size());
		internalRemoved = new HashSet<>(internalRemoved.size());

	}

	@Override
	public void flushForReading() {
	}

	@Override
	public void init() {
		dataStructure.init();
	}

}
