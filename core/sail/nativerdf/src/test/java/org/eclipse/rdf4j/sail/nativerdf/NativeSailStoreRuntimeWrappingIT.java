/*
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that NativeSailStore wraps unexpected RuntimeExceptions in SailException so upstream callers can reliably
 * handle failures (e.g., branch flush clearing pending changes).
 */
public class NativeSailStoreRuntimeWrappingIT {

	private File dataDir;
	private NativeSailStore store;

	@BeforeEach
	public void setUp() throws Exception {
		dataDir = Files.createTempDirectory("nativestore-wrap-test").toFile();
		store = new NativeSailStore(dataDir, "spoc");
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (store != null) {
			store.close();
		}
	}

	@Test
	public void testFlushWrapsRuntimeIntoSailException() throws Exception {
		// Replace TripleStore with a stub that throws at commit()
		replaceTripleStore(new ThrowOnCommitTripleStore(dataDir, "spoc"));

		// Add a statement to ensure a triplestore transaction is started
		SailSink sink = store.getExplicitSailSource().sink(null);
		ValueFactory vf = store.getValueFactory();
		IRI s = vf.createIRI("urn:s");
		IRI p = vf.createIRI("urn:p");
		IRI o = vf.createIRI("urn:o");
		sink.approve(s, p, o, (Resource) null);

		// Now flush, expecting a SailException (wrapping the runtime)
		assertThatThrownBy(() -> sink.flush())
				.isInstanceOf(SailException.class);
	}

	@Test
	public void testRemoveStatementsWrapsRuntimeIntoSailException() throws Exception {
		// Replace TripleStore with a stub that throws at removeTriplesByContext()
		replaceTripleStore(new ThrowOnRemoveTripleStore(dataDir, "spoc"));

		SailSink sink = store.getExplicitSailSource().sink(null);
		// Expect SailException when attempting to remove (deprecateByQuery delegates)
		assertThatThrownBy(() -> sink.deprecateByQuery(null, null, null, new Resource[] { null }))
				.isInstanceOf(SailException.class);
	}

	private void replaceTripleStore(TripleStore newTripleStore) throws Exception {
		Field f = NativeSailStore.class.getDeclaredField("tripleStore");
		f.setAccessible(true);
		f.set(store, newTripleStore);
	}

	private static class ThrowOnCommitTripleStore extends TripleStore {
		public ThrowOnCommitTripleStore(File dir, String indexSpecStr) throws Exception {
			super(dir, indexSpecStr, false);
		}

		@Override
		public void commit() {
			throw new RuntimeException("simulated failure during commit");
		}
	}

	private static class ThrowOnRemoveTripleStore extends TripleStore {
		public ThrowOnRemoveTripleStore(File dir, String indexSpecStr) throws Exception {
			super(dir, indexSpecStr, false);
		}

		@Override
		public Map<Integer, Long> removeTriplesByContext(int subjID, int predID, int objID, int contextId,
				boolean explicit) {
			throw new RuntimeException("simulated failure during removeTriplesByContext");
		}

		@Override
		public void startTransaction() {
			// no-op; we're only interested in remove path throwing
		}
	}
}
