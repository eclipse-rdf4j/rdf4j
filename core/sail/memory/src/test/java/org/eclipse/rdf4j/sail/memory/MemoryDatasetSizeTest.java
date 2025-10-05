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

package org.eclipse.rdf4j.sail.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies dataset.size() semantics for MemoryStore across explicit, inferred and mixed data.
 */
public class MemoryDatasetSizeTest {

	private MemoryStore store;

	@BeforeEach
	public void setUp() {
		store = new MemoryStore();
		store.init();
	}

	@AfterEach
	public void tearDown() {
		if (store != null) {
			store.shutDown();
		}
	}

	@Test
	public void explicitDatasetSize_countsOnlyExplicit() throws Exception {
		ValueFactory vf = store.getValueFactory();
		IRI s = vf.createIRI("urn:explicit:s");
		IRI p = vf.createIRI("urn:p");
		IRI o = vf.createIRI("urn:o");

		SailSource explicit = store.getSailStore().getExplicitSailSource();
		SailSource inferred = store.getSailStore().getInferredSailSource();

		try (SailSink sink = explicit.sink(IsolationLevels.NONE)) {
			sink.approve(s, p, o, null);
			sink.flush();
		}

		try (SailDataset ds = explicit.dataset(IsolationLevels.NONE)) {
			assertEquals(1L, ds.size(null, null, null), "explicit dataset should count explicit statements");
		}
		try (SailDataset ds = inferred.dataset(IsolationLevels.NONE)) {
			assertEquals(0L, ds.size(null, null, null), "inferred dataset should not include explicit statements");
		}
	}

	@Test
	public void inferredDatasetSize_countsOnlyInferred() throws Exception {
		ValueFactory vf = store.getValueFactory();
		IRI s = vf.createIRI("urn:inferred:s");
		IRI p = vf.createIRI("urn:p");
		IRI o = vf.createIRI("urn:o");

		SailSource explicit = store.getSailStore().getExplicitSailSource();
		SailSource inferred = store.getSailStore().getInferredSailSource();

		try (SailSink sink = inferred.sink(IsolationLevels.NONE)) {
			sink.approve(s, p, o, null);
			sink.flush();
		}

		try (SailDataset ds = inferred.dataset(IsolationLevels.NONE)) {
			assertEquals(1L, ds.size(null, null, null), "inferred dataset should count inferred statements");
		}
		try (SailDataset ds = explicit.dataset(IsolationLevels.NONE)) {
			assertEquals(0L, ds.size(null, null, null), "explicit dataset should not include inferred statements");
		}
	}

	@Test
	public void mixedDatasets_eachCountsOwn_andCombinedSums() throws Exception {
		ValueFactory vf = store.getValueFactory();
		IRI s1 = vf.createIRI("urn:explicit:s");
		IRI s2 = vf.createIRI("urn:inferred:s");
		IRI p = vf.createIRI("urn:p");
		IRI o = vf.createIRI("urn:o");

		SailSource explicit = store.getSailStore().getExplicitSailSource();
		SailSource inferred = store.getSailStore().getInferredSailSource();

		try (SailSink sink = explicit.sink(IsolationLevels.NONE)) {
			sink.approve(s1, p, o, null);
			sink.flush();
		}
		try (SailSink sink = inferred.sink(IsolationLevels.NONE)) {
			sink.approve(s2, p, o, null);
			sink.flush();
		}

		long explicitSize;
		long inferredSize;
		try (SailDataset ds = explicit.dataset(IsolationLevels.NONE)) {
			explicitSize = ds.size(null, null, null);
		}
		try (SailDataset ds = inferred.dataset(IsolationLevels.NONE)) {
			inferredSize = ds.size(null, null, null);
		}

		assertEquals(1L, explicitSize, "explicit dataset should count explicit statements only");
		assertEquals(1L, inferredSize, "inferred dataset should count inferred statements only");
		assertEquals(2L, explicitSize + inferredSize, "combined explicit+inferred should sum to total statements");
	}
}
