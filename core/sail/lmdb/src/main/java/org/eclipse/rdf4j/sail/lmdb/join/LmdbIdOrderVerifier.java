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

import java.util.Arrays;

import org.eclipse.rdf4j.sail.lmdb.RecordIterator;

public final class LmdbIdOrderVerifier {

	private LmdbIdOrderVerifier() {
	}

	public static RecordIterator wrap(String label, int mergeIndex, RecordIterator delegate) {
		if (label == null || label.isEmpty()) {
			throw new IllegalArgumentException("Label must be provided");
		}
		if (mergeIndex < 0) {
			throw new IllegalArgumentException("Merge index must be non-negative");
		}
		return new RecordIterator() {
			private long position = 0;
			private long previous = Long.MIN_VALUE;
			private boolean first = true;

			@Override
			public long[] next() {
				long[] record = delegate.next();
				if (record == null) {
					return null;
				}
				if (mergeIndex >= record.length) {
					throw new AssertionError(String.format(
							"%s iterator produced record #%d shorter than merge index %d: %s",
							label, position, mergeIndex, Arrays.toString(record)));
				}
				long current = record[mergeIndex];
				if (!first && current < previous) {
					throw new AssertionError(String.format(
							"%s iterator out of order at position %d: previous=%d current=%d (record=%s)",
							label, position, previous, current, Arrays.toString(record)));
				}
				first = false;
				previous = current;
				long[] copy = Arrays.copyOf(record, record.length);
				position++;
				return copy;
			}

			@Override
			public void close() {
				delegate.close();
			}
		};
	}
}
