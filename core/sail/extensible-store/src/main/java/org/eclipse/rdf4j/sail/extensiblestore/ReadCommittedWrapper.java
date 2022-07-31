/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatementHelper;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ReadCommittedWrapper implements DataStructureInterface {

	private final DataStructureInterface dataStructure;

	private Map<ExtensibleStatement, ExtensibleStatement> internalAdded = new HashMap<>();
	private Map<ExtensibleStatement, ExtensibleStatement> internalRemoved = new HashMap<>();

	ReadCommittedWrapper(DataStructureInterface dataStructure) {
		this.dataStructure = dataStructure;
	}

	@Override
	public void addStatement(ExtensibleStatement statement) {
		internalAdded.put(statement, statement);
		internalRemoved.remove(statement);

	}

	@Override
	public void removeStatement(ExtensibleStatement statement) {
		internalRemoved.put(statement, statement);

	}

	@Override
	public CloseableIteration<? extends ExtensibleStatement, SailException> getStatements(Resource subject,
			IRI predicate, Value object, boolean inferred, Resource... context) {

		// must match single statement
		if (subject != null && predicate != null && object != null && context != null && context.length == 1) {
			Statement statement = SimpleValueFactory.getInstance()
					.createStatement(subject, predicate, object, context[0]);

			statement = ExtensibleStatementHelper.getDefaultImpl().fromStatement(statement, inferred);

			ExtensibleStatement extensibleStatement = internalAdded.get(statement);

			if (extensibleStatement != null) {
				return new SingletonIteration<>(extensibleStatement);
			} else {
				if (internalRemoved.containsKey(statement)) {
					return new EmptyIteration<>();
				} else {
					synchronized (dataStructure) {
						return dataStructure.getStatements(subject, predicate, object, inferred, context);
					}
				}
			}

		} else {
			synchronized (dataStructure) {

				return new LookAheadIteration<>() {

					final Set<ExtensibleStatement> internalAddedLocal = new HashSet<>(internalAdded.values());
					final Set<ExtensibleStatement> internalRemovedLocal = new HashSet<>(internalRemoved.values());

					final Iterator<ExtensibleStatement> left = internalAddedLocal.stream()
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

								if (!inferred && inferred != statement.isInferred()) {
									return false;
								}

								return true;
							})
							.iterator();

					final CloseableIteration<? extends ExtensibleStatement, SailException> right = dataStructure
							.getStatements(
									subject, predicate, object, inferred, context);

					@Override
					protected void handleClose() throws SailException {
						super.handleClose();
						right.close();
					}

					@Override
					protected ExtensibleStatement getNextElement() throws SailException {

						ExtensibleStatement next = null;

						do {
							ExtensibleStatement tempNext = null;
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

		if (internalAdded.isEmpty() && internalRemoved.isEmpty()) {
			return;
		}

		List<ExtensibleStatement> internalAddedEffective = internalAdded
				.keySet()
				.stream()
				.filter(statement -> !internalRemoved.containsKey(statement))
				.collect(Collectors.toList());

		synchronized (dataStructure) {
			internalAddedEffective.forEach(dataStructure::addStatement);
			internalRemoved.values().forEach(dataStructure::removeStatement);
			dataStructure.flushForReading();
		}

		internalAdded = new HashMap<>();
		internalRemoved = new HashMap<>();

	}

	@Override
	public void flushForReading() {
	}

	@Override
	public void init() {
		dataStructure.init();
	}

}
