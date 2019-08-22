package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.SailException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

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

	DistinctModelReducingUnionIteration(CloseableIteration<? extends Statement, SailException> iterator, Model model,
			Function<Model, Model> filterable) {
		this.iterator = iterator;
		this.model = model;
		this.filterable = filterable;
	}

	private final CloseableIteration<? extends Statement, SailException> iterator;
	private final Model model;
	private final Function<Model, Model> filterable;

	private Iterator<Statement> filteredStatementsIterator;

	@Override
	protected Statement getNextElement() throws SailException {
		Statement next = null;

		if (iterator.hasNext()) {
			next = iterator.next();
			synchronized (model) {
				model.remove(next);
			}
		} else {

			if (filteredStatementsIterator == null) {
				synchronized (model) {
					filteredStatementsIterator = new ArrayList<>(filterable.apply(model)).iterator();
				}
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
