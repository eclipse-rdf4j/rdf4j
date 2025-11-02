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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.ArrayBindingSet;
import org.eclipse.rdf4j.sail.lmdb.IdBindingInfo;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
class LmdbIdJoinLazyMaterializationTest {

	private static final String PROPERTY = "rdf4j.lmdb.idJoin.lazyMaterialization";

	@Test
	void eagerMaterializationWhenDisabled() throws Exception {
		System.setProperty(PROPERTY, "false");
		try {
			ValueStore valueStore = mock(ValueStore.class);
			LmdbValue materialized = mock(LmdbValue.class);
			when(valueStore.getLazyValue(1L)).thenReturn(materialized);
			when(valueStore.getLazyValue(2L)).thenReturn(materialized);

			StatementPattern pattern = new StatementPattern(
					new Var("person"),
					new Var("predicate", SimpleValueFactory.getInstance().createIRI("http://example.com/p")),
					new Var("item"));
			LmdbIdJoinIterator.PatternInfo patternInfo = LmdbIdJoinIterator.PatternInfo.create(pattern);
			IdBindingInfo info = IdBindingInfo.fromFirstPattern(patternInfo);

			long[] record = new long[] { 1L, 2L };
			MutableBindingSet target = new ArrayBindingSet(new String[] { "person", "item" });
			boolean result = info.applyRecord(record, target, valueStore);

			assertThat(result).isTrue();
			verify(valueStore).getLazyValue(1L);
			verify(valueStore).getLazyValue(2L);
			verify(materialized, times(2)).init();
			verify(valueStore, never()).getValue(anyLong());
			assertThat(target.getValue("person")).isSameAs(materialized);
			assertThat(target.getValue("item")).isSameAs(materialized);
		} finally {
			System.clearProperty(PROPERTY);
		}
	}
}
