/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.join;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;

/**
 * Record iterator that interleaves left and right record iterators in the same way that
 * {@link org.eclipse.rdf4j.query.algebra.evaluation.iterator.JoinIterator} does for binding sets, but operating purely
 * on ID-based record arrays.
 */
public final class IdJoinRecordIterator implements RecordIterator {

	@FunctionalInterface
	public interface RightFactory {
		RecordIterator apply(long[] leftRecord) throws QueryEvaluationException;
	}

	private final RecordIterator left;
	private final RightFactory rightFactory;
	private RecordIterator currentRight;

	public IdJoinRecordIterator(RecordIterator left, RightFactory rightFactory) {
		this.left = left;
		this.rightFactory = rightFactory;
	}

	@Override
	public long[] next() {
		try {
			while (true) {
				if (currentRight != null) {
					long[] rightRec = currentRight.next();
					if (rightRec != null) {
						return rightRec;
					}
					currentRight.close();
					currentRight = null;
				}

				long[] leftRec = left.next();
				if (leftRec == null) {
					return null;
				}

				currentRight = rightFactory.apply(leftRec);
				if (currentRight == null) {
					currentRight = LmdbIdJoinIterator.emptyRecordIterator();
				}
			}
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		left.close();
		if (currentRight != null) {
			currentRight.close();
			currentRight = null;
		}
	}
}
