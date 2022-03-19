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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

@InternalUseOnly
public class ValidationResultIterator implements Iterator<ValidationResult> {

	private final long limit;
	private long counter = 0;
	private boolean conforms = true;
	private boolean truncated = false;

	private Iterator<ValidationResult> next = Collections.emptyIterator();
	private Iterator<ValidationTuple> tupleIterator;

	public ValidationResultIterator(CloseableIteration<? extends ValidationTuple, SailException> tupleIterator,
			long limit) throws InterruptedException {
		this.limit = limit;
		this.tupleIterator = toList(tupleIterator).iterator();
	}

	private void calculateNext() {
		if (tupleIterator.hasNext()) {
			conforms = false;
		}
		if (next.hasNext()) {
			return;
		}

		if (tupleIterator.hasNext()) {
			if (limit < 0 || counter < limit) {
				ValidationTuple invalidTuple = tupleIterator.next();

				Set<ValidationTuple> invalidTuples;

				if (!invalidTuple.getCompressedTuples().isEmpty()) {
					invalidTuples = invalidTuple.getCompressedTuples();
				} else {
					invalidTuples = Collections.singleton(invalidTuple);
				}

				Set<ValidationResult> validationResultsRet = new HashSet<>();

				for (ValidationTuple tuple : invalidTuples) {
					List<ValidationResult> validationResults = tuple.getValidationResult();

					ValidationResult validationResult1 = validationResults.get(validationResults.size() - 1);
					validationResultsRet.add(validationResult1);

//					ValidationResult parent = null;
//
//					// we iterate in reverse order to get the most recent validation result first
//					for (int i = validationResults.size() - 1; i >= 0; i--) {
//						ValidationResult validationResult = validationResults.get(i);
//						if (parent == null) {
//							parent = validationResult;
//							validationResultsRet.add(parent);
//						} else {
//							parent.setDetail(validationResult);
//							parent = validationResult;
//						}
//					}

					counter++;
				}

				next = validationResultsRet.iterator();

			}

			if (limit >= 0 && counter >= limit && tupleIterator.hasNext()) {
				truncated = true;
			}

		}
	}

	private List<ValidationTuple> toList(CloseableIteration<? extends ValidationTuple, SailException> tupleIterator)
			throws InterruptedException {
		try (tupleIterator) {
			List<ValidationTuple> actualList = new ArrayList<>();
			long localCounter = 0;
			while (tupleIterator.hasNext() && (limit < 0 || localCounter++ < limit + 1)) {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
				actualList.add(tupleIterator.next());
			}

			return Collections.unmodifiableList(actualList);
		}
	}

	public List<ValidationTuple> getTuples() {
		List<ValidationTuple> actualList = new ArrayList<>();
		long localCounter = 0;
		while (tupleIterator.hasNext() && (limit < 0 || localCounter++ < limit + 1)) {
			actualList.add(tupleIterator.next());
		}

		this.tupleIterator = actualList.iterator();

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
		return next.hasNext();
	}

	@Override
	public ValidationResult next() {
		calculateNext();
		if (!next.hasNext()) {
			throw new IllegalStateException();
		}

		return next.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
