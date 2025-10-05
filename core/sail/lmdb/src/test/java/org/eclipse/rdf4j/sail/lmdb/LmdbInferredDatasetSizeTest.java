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

package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reproduces regression: inferred dataset size() must count inferred statements. The current implementation ignores the
 * dataset's explicit/inferred flag and always counts explicit-only, causing inferred datasets to report 0.
 */
public class LmdbInferredDatasetSizeTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private LmdbStore store;

	@BeforeEach
	public void setUp(@TempDir File dataDir) {
		store = new LmdbStore(dataDir);
		store.init();
	}

	@AfterEach
	public void tearDown() {
		if (store != null) {
			store.shutDown();
		}
	}

	@Test
	public void inferredDatasetSize_countsInferredStatements() throws Exception {
		// Arrange: add one inferred statement via the inferred sink
		LmdbSailStore backing = store.getBackingStore();
		SailSource inferred = backing.getInferredSailSource();
		SailSource explicit = backing.getExplicitSailSource();

		IRI s = vf.createIRI("urn:s");
		IRI p = vf.createIRI("urn:p");
		IRI o = vf.createIRI("urn:o");

		try (SailSink sink = inferred.sink(IsolationLevels.NONE)) {
			sink.approve(s, p, o, null);
			sink.flush();
		}

		// Act/Assert: inferred dataset sees the inferred statement, explicit does not
		try (SailDataset ds = inferred.dataset(IsolationLevels.NONE)) {
			assertEquals(1L, ds.size(null, null, null), "inferred dataset should count inferred statements");
		}

		try (SailDataset ds = explicit.dataset(IsolationLevels.NONE)) {
			assertEquals(0L, ds.size(null, null, null), "explicit dataset should not include inferred statements");
		}
	}
}
