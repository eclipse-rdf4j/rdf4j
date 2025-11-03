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
package org.eclipse.rdf4j.sail.lmdb.join;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.lmdb.IdBindingInfo;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
class LmdbIdMergeJoinIteratorTest {

	@Test
	void mergeJoinProducesIdRecordsWithoutMaterialization() throws Exception {
		StatementPattern leftPattern = new StatementPattern(
				new Var("x"),
				new Var("pl", SimpleValueFactory.getInstance().createIRI("urn:p:left")),
				new Var("ol", SimpleValueFactory.getInstance().createIRI("urn:o:left")));
		StatementPattern rightPattern = new StatementPattern(
				new Var("x"),
				new Var("pr", SimpleValueFactory.getInstance().createIRI("urn:p:right")),
				new Var("or", SimpleValueFactory.getInstance().createIRI("urn:o:right")));

		LmdbIdJoinIterator.PatternInfo leftInfo = LmdbIdJoinIterator.PatternInfo.create(leftPattern);
		LmdbIdJoinIterator.PatternInfo rightInfo = LmdbIdJoinIterator.PatternInfo.create(rightPattern);
		IdBindingInfo bindingInfo = IdBindingInfo.combine(IdBindingInfo.fromFirstPattern(leftInfo), rightInfo, null);

		long[] leftRecord = new long[] { 7L, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, 0L };
		long[] rightRecord = new long[] { 7L, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, 0L };

		RecordIterator leftIterator = new ArrayRecordIterator(leftRecord);
		RecordIterator rightIterator = new ArrayRecordIterator(rightRecord);

		LmdbIdMergeJoinIterator iterator = new LmdbIdMergeJoinIterator(leftIterator, rightIterator, leftInfo, rightInfo,
				"x", bindingInfo);
		try {
			Object record = iterator.next();
			assertThat(record).isInstanceOf(long[].class);
			assertThat((long[]) record).containsExactly(7L, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, 0L);
		} finally {
			iterator.close();
		}
	}

	private static final class ArrayRecordIterator implements RecordIterator {
		private final long[][] data;
		private int index;

		private ArrayRecordIterator(long[]... records) {
			this.data = Arrays.stream(records)
					.map(arr -> Arrays.copyOf(arr, arr.length))
					.toArray(long[][]::new);
		}

		@Override
		public long[] next() {
			if (index >= data.length) {
				return null;
			}
			long[] source = data[index++];
			return Arrays.copyOf(source, source.length);
		}

		@Override
		public void close() {
			// nothing to do
		}
	}
}
