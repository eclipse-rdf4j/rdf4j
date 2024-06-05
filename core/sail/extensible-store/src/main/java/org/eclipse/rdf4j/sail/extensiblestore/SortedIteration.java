/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.extensiblestore;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;

/**
 * A wrapper for an Iteration that filters the statements against a pattern similar to getStatements(Resource subject,
 * IRI predicate, Value object, Resource... context).
 */
@Experimental
public class SortedIteration<E extends ExtensibleStatement, X extends Exception> extends LookAheadIteration<E> {

	private final CloseableIteration<E> wrappedIteration;
	private final StatementOrder statementOrder;
	private final Comparator<E> comparator;
	private boolean initialized;
	private Iterator<E> orderedIterator;

	public SortedIteration(CloseableIteration<E> wrappedIteration, StatementOrder statementOrder) {
		this.wrappedIteration = wrappedIteration;
		this.statementOrder = statementOrder;

		if (statementOrder.equals(StatementOrder.S)) {
			comparator = Comparator.comparing(o -> o.getSubject().toString());
		} else if (statementOrder.equals(StatementOrder.P)) {
			comparator = Comparator.comparing(o -> o.getPredicate().toString());
		} else if (statementOrder.equals(StatementOrder.O)) {
			comparator = Comparator.comparing(o -> o.getObject().toString());
		} else if (statementOrder.equals(StatementOrder.C)) {
			comparator = Comparator.comparing(o -> o.getContext().toString());
		} else {
			throw new IllegalArgumentException("Unknown StatementOrder: " + statementOrder);
		}
	}

	private void lazyInit() {
		if (initialized) {
			return;
		}

		initialized = true;

		try (wrappedIteration) {
			List<E> list = Iterations.asList(wrappedIteration);
			list.sort(comparator);
			orderedIterator = list.iterator();
		}

	}

	@Override
	protected E getNextElement() {
		lazyInit();
		if (orderedIterator == null) {
			throw new NoSuchElementException("Iteration has been closed");
		}
		if (orderedIterator.hasNext()) {
			return orderedIterator.next();
		}
		return null;

	}

	@Override
	protected void handleClose() {
		wrappedIteration.close();
		orderedIterator = null;
	}

}
