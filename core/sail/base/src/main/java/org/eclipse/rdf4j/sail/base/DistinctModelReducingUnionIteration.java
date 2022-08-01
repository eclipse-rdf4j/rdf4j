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
package org.eclipse.rdf4j.sail.base;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.SailException;

/**
 * <p>
 * An Iteration that takes two source. An iterator and a model.
 * </p>
 * <p>
 * For every statement returned by the iterator, that statement is removed from the model. Once the iterator is
 * exhausted, a new filteredStatementsIterator is created by applying the filterable function to the model.
 * </p>
 * <p>
 * The point of this iteration is to create a distinct iterator that produces only distinct results in a lazy and
 * mutable manner. This is useful when iterating in a transaction, since the user may have added duplicate statements.
 * On a potential second iteration there will be no need for further deduplication, since the initial deduplication was
 * mutable.
 * </p>
 * <p>
 * Model will throw a ConcurrentModificationException if two threads call .remove(...) at the same time or one thread
 * calls .next() on an iterator while another calls .remove(...). This is resolved by synchronizing access to the model
 * and by consuming the entire iterator into an ArrayList, effectively caching the filtered part of the model in memory.
 * There is no overflow to disk for this cache.
 * </p>
 **/
public class DistinctModelReducingUnionIteration extends LookAheadIteration<Statement, SailException> {

	private final CloseableIteration<? extends Statement, SailException> iterator;
	private final Consumer<Statement> approvedRemover;
	private final Supplier<Iterable<Statement>> approvedSupplier;

	DistinctModelReducingUnionIteration(CloseableIteration<? extends Statement, SailException> iterator,
			Consumer<Statement> approvedRemover,
			Supplier<Iterable<Statement>> approvedSupplier) {
		this.iterator = iterator;
		this.approvedRemover = approvedRemover;
		this.approvedSupplier = approvedSupplier;
	}

	private Iterator<? extends Statement> filteredStatementsIterator;

	@Override
	protected Statement getNextElement() throws SailException {
		Statement next = null;

		// first run through the statements from the base store
		if (iterator.hasNext()) {
			next = iterator.next();

			// remove the statement from the approved model in the Changeset, in case the approved model has a duplicate
			approvedRemover.accept(next);
		} else {
			// we have now exhausted the base store and will start returning data added in this transaction but not yet
			// committed, eg. approved model in the Changeset

			if (filteredStatementsIterator == null) {
				filteredStatementsIterator = approvedSupplier.get().iterator();
			}

			if (filteredStatementsIterator.hasNext()) {
				next = filteredStatementsIterator.next();
			}
		}

		return next;
	}

	@Override
	protected void handleClose() throws SailException {
		try {
			iterator.close();
		} finally {
			super.handleClose();
		}
	}

}
