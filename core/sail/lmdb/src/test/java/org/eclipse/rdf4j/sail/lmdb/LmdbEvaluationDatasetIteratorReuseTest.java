/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.junit.jupiter.api.Test;

class LmdbEvaluationDatasetIteratorReuseTest {

	@Test
	void defaultImplementationDoesNotCloseReusableIterator() throws Exception {
		RecordingDataset dataset = new RecordingDataset();
		long[] binding = new long[] { 0L };
		long[] patternIds = new long[] { 0L, 0L, 0L, 0L };
		RecordingIterator reusable = new RecordingIterator();

		dataset.getRecordIterator(binding, -1, -1, -1, -1, patternIds, null, null, null, reusable);

		assertFalse(reusable.closed, "reusable iterator should remain open");
	}

	private static final class RecordingDataset implements LmdbEvaluationDataset {

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds) throws QueryEvaluationException {
			return new RecordIterator() {
				@Override
				public long[] next() {
					return null;
				}

				@Override
				public void close() {
					// no-op
				}
			};
		}

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds, KeyRangeBuffers keyBuffers, long[] bindingReuse, long[] quadReuse)
				throws QueryEvaluationException {
			return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds);
		}

		@Override
		public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings)
				throws QueryEvaluationException {
			return new RecordIterator() {
				@Override
				public long[] next() {
					return null;
				}

				@Override
				public void close() {
					// no-op
				}
			};
		}

		@Override
		public ValueStore getValueStore() {
			return null;
		}
	}

	private static final class RecordingIterator implements RecordIterator {
		private boolean closed;

		@Override
		public long[] next() {
			return null;
		}

		@Override
		public void close() {
			closed = true;
		}
	}
}
