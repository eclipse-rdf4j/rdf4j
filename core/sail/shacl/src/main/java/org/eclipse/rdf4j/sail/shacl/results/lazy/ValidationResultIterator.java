/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.results.lazy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.IteratorCloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

@InternalUseOnly
public class ValidationResultIterator implements Iterator<ValidationResult> {

	private final long limit;
	private long counter = 0;
	private boolean conforms = true;
	private boolean truncated = false;

	private ValidationResult next = null;
	private CloseableIteration<? extends ValidationTuple, SailException> tupleIterator;

	public ValidationResultIterator(CloseableIteration<? extends ValidationTuple, SailException> tupleIterator,
			long limit) {
		this.limit = limit;
		this.tupleIterator = tupleIterator;
		getTuples();

	}

	private void calculateNext() {
		if (tupleIterator.hasNext()) {
			conforms = false;
		}
		if (next == null && tupleIterator.hasNext()) {
			if (limit < 0 || counter < limit) {
				ValidationTuple invalidTuple = tupleIterator.next();

				Deque<ValidationResult> validationResults = invalidTuple.toValidationResult();

				ValidationResult parent = null;

				for (ValidationResult validationResult : validationResults) {
					if (parent == null) {
						parent = validationResult;
						next = parent;
					} else {
						parent.setDetail(validationResult);
						parent = validationResult;
					}
				}

				counter++;
			}

			if (limit >= 0 && counter >= limit && tupleIterator.hasNext()) {
				truncated = true;
			}

		}
	}

	public List<ValidationTuple> getTuples() {
		List<ValidationTuple> actualList = new ArrayList<>();
		long localCounter = 0;
		while (tupleIterator.hasNext() && (limit < 0 || localCounter++ < limit + 1)) {
			actualList.add(tupleIterator.next());
		}

		tupleIterator = new IteratorCloseableIteration<>(actualList.iterator());

		return Collections.unmodifiableList(actualList);
	}

	public boolean conforms() {
		calculateNext();
		return conforms;
	}

	public boolean isTruncated() {
		return truncated;
	}

	@Override
	public boolean hasNext() {
		calculateNext();
		return next != null;
	}

	@Override
	public ValidationResult next() {
		calculateNext();
		if (next == null) {
			throw new IllegalStateException();
		}

		ValidationResult temp = next;
		next = null;
		return temp;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
