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
package org.eclipse.rdf4j.sail.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.s3.config.S3StoreConfig;
import org.eclipse.rdf4j.sail.s3.storage.FileSystemObjectStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class S3PersistenceTest {

	@TempDir
	Path tempDir;

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@Test
	void writeFlushShutdownRestart_quadsReadable() throws Exception {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3StoreConfig config = new S3StoreConfig();

		IRI s = VF.createIRI("http://example.org/s1");
		IRI p = VF.createIRI("http://example.org/p1");
		IRI o = VF.createIRI("http://example.org/o1");
		IRI ctx = VF.createIRI("http://example.org/g1");

		// Phase 1: Write and flush
		{
			S3SailStore sailStore = new S3SailStore(config, store);
			ValueFactory svf = sailStore.getValueFactory();

			// Add statements using sink
			var source = sailStore.getExplicitSailSource();
			var sink = source.sink(IsolationLevels.NONE);

			// We need to resolve values through the sail's value factory
			S3ValueStore vs = (S3ValueStore) svf;
			long sId = vs.storeValue(s);
			long pId = vs.storeValue(p);
			long oId = vs.storeValue(o);
			long ctxId = vs.storeValue(ctx);

			sink.approve(s, p, o, ctx);
			sink.flush();
			sailStore.close();
		}

		// Phase 2: Restart and verify
		{
			S3SailStore sailStore = new S3SailStore(config, store);
			ValueFactory svf = sailStore.getValueFactory();

			var source = sailStore.getExplicitSailSource();
			var dataset = source.dataset(IsolationLevels.NONE);

			CloseableIteration<? extends Statement> iter = dataset.getStatements(null, null, null);
			assertTrue(iter.hasNext(), "Should have at least one statement after restart");

			Statement stmt = iter.next();
			assertEquals(s.stringValue(), stmt.getSubject().stringValue());
			assertEquals(p.stringValue(), stmt.getPredicate().stringValue());
			assertEquals(o.stringValue(), stmt.getObject().stringValue());
			assertEquals(ctx.stringValue(), stmt.getContext().stringValue());

			assertFalse(iter.hasNext(), "Should have exactly one statement");
			iter.close();
			dataset.close();
			sailStore.close();
		}
	}

	@Test
	void multipleFlushes_allDataReadable() throws Exception {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3StoreConfig config = new S3StoreConfig();

		IRI s1 = VF.createIRI("http://example.org/s1");
		IRI s2 = VF.createIRI("http://example.org/s2");
		IRI p = VF.createIRI("http://example.org/p");
		IRI o = VF.createIRI("http://example.org/o");

		// Write, flush, write more, flush again
		{
			S3SailStore sailStore = new S3SailStore(config, store);

			var source = sailStore.getExplicitSailSource();
			var sink = source.sink(IsolationLevels.NONE);

			sink.approve(s1, p, o, null);
			sink.flush();

			sink.approve(s2, p, o, null);
			sink.flush();

			sailStore.close();
		}

		// Restart and verify both statements exist
		{
			S3SailStore sailStore = new S3SailStore(config, store);

			var source = sailStore.getExplicitSailSource();
			var dataset = source.dataset(IsolationLevels.NONE);

			CloseableIteration<? extends Statement> iter = dataset.getStatements(null, null, null);
			int count = 0;
			while (iter.hasNext()) {
				iter.next();
				count++;
			}
			assertEquals(2, count, "Should have 2 statements after restart");

			iter.close();
			dataset.close();
			sailStore.close();
		}
	}

	@Test
	void deleteAndRestart_deletedQuadsGone() throws Exception {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3StoreConfig config = new S3StoreConfig();

		IRI s1 = VF.createIRI("http://example.org/s1");
		IRI s2 = VF.createIRI("http://example.org/s2");
		IRI p = VF.createIRI("http://example.org/p");
		IRI o = VF.createIRI("http://example.org/o");

		// Write two statements, delete one, flush
		{
			S3SailStore sailStore = new S3SailStore(config, store);

			var source = sailStore.getExplicitSailSource();
			var sink = source.sink(IsolationLevels.NONE);

			sink.approve(s1, p, o, null);
			sink.approve(s2, p, o, null);
			sink.flush();

			// Now delete s1
			Statement toDeprecate = VF.createStatement(s1, p, o);
			sink.deprecate(toDeprecate);
			sink.flush();

			sailStore.close();
		}

		// Restart and verify only s2 remains
		{
			S3SailStore sailStore = new S3SailStore(config, store);

			var source = sailStore.getExplicitSailSource();
			var dataset = source.dataset(IsolationLevels.NONE);

			CloseableIteration<? extends Statement> iter = dataset.getStatements(null, null, null);
			assertTrue(iter.hasNext());
			Statement stmt = iter.next();
			assertEquals(s2.stringValue(), stmt.getSubject().stringValue());
			assertFalse(iter.hasNext());

			iter.close();
			dataset.close();
			sailStore.close();
		}
	}

	@Test
	void multiplePredicates_allQueriesWork() throws Exception {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3StoreConfig config = new S3StoreConfig();

		IRI s1 = VF.createIRI("http://example.org/s1");
		IRI s2 = VF.createIRI("http://example.org/s2");
		IRI p1 = VF.createIRI("http://example.org/name");
		IRI p2 = VF.createIRI("http://example.org/age");
		IRI o1 = VF.createIRI("http://example.org/Alice");
		IRI o2 = VF.createIRI("http://example.org/30");

		// Write data with multiple predicates, flush, restart
		{
			S3SailStore sailStore = new S3SailStore(config, store);
			var source = sailStore.getExplicitSailSource();
			var sink = source.sink(IsolationLevels.NONE);

			sink.approve(s1, p1, o1, null);
			sink.approve(s1, p2, o2, null);
			sink.approve(s2, p1, o2, null);
			sink.flush();
			sailStore.close();
		}

		// Restart and verify queries
		{
			S3SailStore sailStore = new S3SailStore(config, store);
			var source = sailStore.getExplicitSailSource();
			var dataset = source.dataset(IsolationLevels.NONE);

			// All statements
			List<Statement> all = drain(dataset.getStatements(null, null, null));
			assertEquals(3, all.size());

			// By predicate (p1)
			List<Statement> byP1 = drain(dataset.getStatements(null, p1, null));
			assertEquals(2, byP1.size());
			for (Statement st : byP1) {
				assertEquals(p1.stringValue(), st.getPredicate().stringValue());
			}

			// By predicate (p2)
			List<Statement> byP2 = drain(dataset.getStatements(null, p2, null));
			assertEquals(1, byP2.size());
			assertEquals(p2.stringValue(), byP2.get(0).getPredicate().stringValue());

			// By subject
			List<Statement> byS1 = drain(dataset.getStatements(s1, null, null));
			assertEquals(2, byS1.size());

			// By subject + predicate
			List<Statement> byS1P1 = drain(dataset.getStatements(s1, p1, null));
			assertEquals(1, byS1P1.size());

			// By object
			List<Statement> byO2 = drain(dataset.getStatements(null, null, o2));
			assertEquals(2, byO2.size());

			dataset.close();
			sailStore.close();
		}
	}

	@Test
	void fileLayout_flatDataDirectory() throws Exception {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3StoreConfig config = new S3StoreConfig();

		IRI s = VF.createIRI("http://example.org/s1");
		IRI p = VF.createIRI("http://example.org/p1");
		IRI o = VF.createIRI("http://example.org/o1");

		{
			S3SailStore sailStore = new S3SailStore(config, store);
			var source = sailStore.getExplicitSailSource();
			var sink = source.sink(IsolationLevels.NONE);
			sink.approve(s, p, o, null);
			sink.flush();
			sailStore.close();
		}

		// Verify flat file paths (no predicates/ directory)
		List<String> dataFiles = store.list("data/");
		assertFalse(dataFiles.isEmpty(), "Should have data files");
		for (String key : dataFiles) {
			assertFalse(key.contains("predicates/"), "Should not have predicate partitions: " + key);
			assertTrue(key.startsWith("data/L0-"), "Should start with data/L0-: " + key);
			assertTrue(key.endsWith(".parquet"), "Should end with .parquet: " + key);
		}

		// Should have 3 files (one per sort order)
		assertEquals(3, dataFiles.size(), "Should have 3 files (spoc, opsc, cspo)");

		// Check sort orders are present
		List<String> suffixes = dataFiles.stream()
				.map(k -> k.substring(k.lastIndexOf('-') + 1, k.lastIndexOf('.')))
				.collect(Collectors.toList());
		assertTrue(suffixes.contains("spoc"), "Missing spoc file");
		assertTrue(suffixes.contains("opsc"), "Missing opsc file");
		assertTrue(suffixes.contains("cspo"), "Missing cspo file");
	}

	@Test
	void contextQuery_afterRestart() throws Exception {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3StoreConfig config = new S3StoreConfig();

		IRI s = VF.createIRI("http://example.org/s1");
		IRI p = VF.createIRI("http://example.org/p1");
		IRI o = VF.createIRI("http://example.org/o1");
		IRI g1 = VF.createIRI("http://example.org/graph1");
		IRI g2 = VF.createIRI("http://example.org/graph2");

		{
			S3SailStore sailStore = new S3SailStore(config, store);
			var source = sailStore.getExplicitSailSource();
			var sink = source.sink(IsolationLevels.NONE);
			sink.approve(s, p, o, g1);
			sink.approve(s, p, o, g2);
			sink.flush();
			sailStore.close();
		}

		{
			S3SailStore sailStore = new S3SailStore(config, store);
			var source = sailStore.getExplicitSailSource();
			var dataset = source.dataset(IsolationLevels.NONE);

			// Query by context g1
			List<Statement> byG1 = drain(
					dataset.getStatements(null, null, null, new org.eclipse.rdf4j.model.Resource[] { g1 }));
			assertEquals(1, byG1.size());
			assertEquals(g1.stringValue(), byG1.get(0).getContext().stringValue());

			// Query all
			List<Statement> all = drain(dataset.getStatements(null, null, null));
			assertEquals(2, all.size());

			dataset.close();
			sailStore.close();
		}
	}

	private List<Statement> drain(CloseableIteration<? extends Statement> iter) {
		List<Statement> result = new ArrayList<>();
		while (iter.hasNext()) {
			result.add(iter.next());
		}
		iter.close();
		return result;
	}

	@Test
	void namespacePersistence() throws Exception {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3StoreConfig config = new S3StoreConfig();

		// Set a namespace
		{
			S3SailStore sailStore = new S3SailStore(config, store);

			var source = sailStore.getExplicitSailSource();
			var sink = source.sink(IsolationLevels.NONE);
			sink.setNamespace("ex", "http://example.org/");
			sink.flush();
			sailStore.close();
		}

		// Restart and verify namespace persists
		{
			S3SailStore sailStore = new S3SailStore(config, store);

			var source = sailStore.getExplicitSailSource();
			var dataset = source.dataset(IsolationLevels.NONE);

			assertEquals("http://example.org/", dataset.getNamespace("ex"));

			dataset.close();
			sailStore.close();
		}
	}
}
