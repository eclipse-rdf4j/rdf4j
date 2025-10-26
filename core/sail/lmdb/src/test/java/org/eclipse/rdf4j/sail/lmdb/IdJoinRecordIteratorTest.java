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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.rdf4j.sail.lmdb.join.IdJoinRecordIterator;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdJoinIterator;
import org.junit.jupiter.api.Test;

class IdJoinRecordIteratorTest {

	@Test
	void rightIteratorAlreadyIncludesLeftBindings() {
		IdBindingInfo leftInfo = bindingInfo(Map.of("a", 0, "b", 2));

		RecordIterator leftIterator = recordIterator(
				new long[] { 1L, 0L, 2L },
				new long[] { 7L, 0L, 8L });

		IdJoinRecordIterator.RightFactory rightFactory = leftRecord -> {
			long a = leftInfo.getId(leftRecord, "a");
			long b = leftInfo.getId(leftRecord, "b");
			if (a == 1L) {
				return LmdbIdJoinIterator.emptyRecordIterator();
			}
			return recordIterator(
					new long[] { a, b, 50L },
					new long[] { a, b, 60L });
		};

		IdJoinRecordIterator iterator = new IdJoinRecordIterator(leftIterator, rightFactory);

		assertThat(iterator.next()).containsExactly(7L, 8L, 50L);
		assertThat(iterator.next()).containsExactly(7L, 8L, 60L);
		assertThat(iterator.next()).isNull();
	}

	private static RecordIterator recordIterator(long[]... records) {
		return new RecordIterator() {
			int index = 0;

			@Override
			public long[] next() {
				if (index >= records.length) {
					return null;
				}
				return Arrays.copyOf(records[index], records[index++].length);
			}

			@Override
			public void close() {
				// no-op
			}
		};
	}

	private static IdBindingInfo bindingInfo(Map<String, Integer> indexByVar) {
		LinkedHashMap<String, Integer> indexMap = new LinkedHashMap<>(indexByVar);
		Map<String, Integer> maskMap = new java.util.HashMap<>();
		indexByVar.forEach((name, position) -> maskMap.put(name, 1 << position));
		return new IdBindingInfo(indexMap, maskMap);
	}
}
