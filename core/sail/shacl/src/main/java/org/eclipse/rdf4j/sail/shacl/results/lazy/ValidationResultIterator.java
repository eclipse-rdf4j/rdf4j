/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
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
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
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

	public List<ValidationTuple> getTuples() {
		List<ValidationTuple> actualList = new ArrayList<>();
		long localCounter = 0;
		while (tupleIterator.hasNext() && (limit < 0 || localCounter++ < limit + 1)) {
			actualList.add(tupleIterator.next());
		}

		tupleIterator = new CloseableIteratorIteration<>(actualList.iterator());
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
