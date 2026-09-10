/* SPDX-License-Identifier: BSD-3-Clause */
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.valueoverlay.CompressedValueOverlay;
import org.eclipse.rdf4j.sail.lmdb.valueoverlay.OverlayCapacityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real native/RDF4J integration acceptance; requires the full module dependencies and native library. */
public class ValueStoreCompressedOverlayTest {
	@TempDir
	File directory;

	private CompressedValueOverlay.Options options() {
		return new CompressedValueOverlay.Options(32L << 20, 8192, 16, 64 << 10, 1 << 20, true, true);
	}

	@Test
	void exactPersistentBytesAndLexicalFormsSurviveWarmAndEviction() throws Exception {
		var factory = SimpleValueFactory.getInstance();
		var store = new ValueStore(new File(directory, "values"), new LmdbStoreConfig().setInlineLiterals(false));
		try {
			List<Value> values = new ArrayList<>();
			for (int i = 0; i < 256; i++) {
				values.add(factory.createIRI("https://example.org/items/" + i));
				values.add(factory.createLiteral("+000000" + i, XSD.INTEGER));
				values.add(factory.createLiteral("-00001234567890." + i + "000", XSD.DECIMAL));
				values.add(factory.createLiteral("2026-09-09T12:00:00." + i + "000+02:00", XSD.DATETIME));
				values.add(factory.createLiteral("e\u0301 and é; 漢字; 😀 " + i, "nb"));
				values.add(factory.createBNode("blank-" + i));
			}
			long[] ids = new long[values.size()];
			store.startTransaction(true);
			for (int i = 0; i < ids.length; i++)
				ids[i] = store.storeValue(values.get(i));
			store.commit();
			byte[][] records = new byte[ids.length][];
			for (int i = 0; i < ids.length; i++)
				records[i] = store.getData(ids[i]);
			var stats = store.warmCompressedValueOverlay(options());
			assertTrue(stats.records() >= ids.length);
			store.clearCaches();
			for (int i = 0; i < ids.length; i++) {
				assertArrayEquals(records[i], store.getData(ids[i]));
				Value actual = store.getValue(ids[i]);
				assertEquals(values.get(i), actual);
				if (actual instanceof Literal literal)
					assertEquals(((Literal) values.get(i)).getLabel(), literal.getLabel());
				assertEquals(ids[i], store.getId(values.get(i)));
			}
		} finally {
			store.close();
		}
	}

	@Test
	void rollbackRetainsBaseAndClearRetiresIt() throws Exception {
		var factory = SimpleValueFactory.getInstance();
		var store = new ValueStore(new File(directory, "mutable"), new LmdbStoreConfig());
		try {
			store.startTransaction(true);
			long id = store.storeValue(factory.createBNode("kept"));
			store.commit();
			var base = store.warmCompressedValueOverlay(options());
			assertNotNull(base);
			store.startTransaction(true);
			assertSame(base, store.compressedValueOverlayStats());
			store.storeValue(factory.createBNode("rollback"));
			store.rollback();
			assertSame(base, store.compressedValueOverlayStats());
			assertEquals("kept", store.getValue(id).stringValue());
			store.clear();
			assertNull(store.compressedValueOverlayStats());
			store.warmCompressedValueOverlay(options());
			assertNotNull(store.compressedValueOverlayStats());
		} finally {
			store.close();
		}
	}

	@Test
	void refusalDoesNotReplaceExistingPublishedOverlay() throws Exception {
		var factory = SimpleValueFactory.getInstance();
		var store = new ValueStore(new File(directory, "capacity"), new LmdbStoreConfig());
		try {
			store.startTransaction(true);
			long id = store.storeValue(factory.createBNode("stable"));
			store.commit();
			var installed = store.warmCompressedValueOverlay(options());
			assertThrows(OverlayCapacityException.class, () -> store.warmCompressedValueOverlay(
					new CompressedValueOverlay.Options(100, 0, 2, 1024, 1024, true, true)));
			assertSame(installed, store.compressedValueOverlayStats());
			assertEquals("stable", store.getValue(id).stringValue());
		} finally {
			store.close();
		}
	}

	@Test
	void explicitCommitsReuseTheBaseIncludingBatchedInsertions() throws Exception {
		var vf = SimpleValueFactory.getInstance();
		var store = new ValueStore(new File(directory, "commits"), new LmdbStoreConfig().setInlineLiterals(false));
		try {
			store.startTransaction(true);
			long old = store.storeValue(vf.createLiteral("old"));
			store.commit();
			var base = store.warmCompressedValueOverlay(options());
			long baseTxn = store.compressedValueOverlayViewStats().transactionId();
			for (int i = 0; i < 12; i++) {
				store.startTransaction(true);
				var value = vf.createLiteral("new-" + i);
				long id = store.storeValue(value);
				store.commit();
				store.clearCaches();
				assertSame(base, store.compressedValueOverlayStats());
				assertEquals(baseTxn, store.compressedValueOverlayViewStats().baseTransactionId());
				assertEquals(value, store.getValue(id));
				assertEquals(id, store.getId(value));
				assertEquals("old", store.getValue(old).stringValue());
			}
			long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
			while (store.compressedValueOverlayViewStats().compactions() == 0 && System.nanoTime() < deadline) {
				store.compactCompressedValueOverlay();
				Thread.sleep(1);
			}
			assertTrue(store.compressedValueOverlayViewStats().compactions() > 0);
		} finally {
			store.close();
		}
	}

	@Test
	void implicitLargeRecordUsesItsWriteTransactionAndPublishesDelta() throws Exception {
		var vf = SimpleValueFactory.getInstance();
		var store = new ValueStore(new File(directory, "implicit"), new LmdbStoreConfig().setInlineLiterals(false));
		try {
			store.storeValue(vf.createLiteral("seed"));
			var base = store.warmCompressedValueOverlay(options());
			var value = vf.createLiteral("large lexical payload/".repeat(800));
			long id = store.storeValue(value);
			store.clearCaches();
			assertSame(base, store.compressedValueOverlayStats());
			assertEquals(value, store.getValue(id));
			assertEquals(id, store.getId(value));
			var view = store.getRecordView(id);
			assertNotNull(view);
			assertTrue(view.lexicalEquals(value.getLabel().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		} finally {
			store.close();
		}
	}

	@Test
	void reverseRetirementDoesNotResurrectAnIdFromTheCompressedBase() throws Exception {
		var vf = SimpleValueFactory.getInstance();
		var store = new ValueStore(new File(directory, "retire"), new LmdbStoreConfig().setInlineLiterals(false));
		try {
			store.startTransaction(true);
			var value = vf.createBNode("retired");
			long id = store.storeValue(value);
			store.commit();
			var base = store.warmCompressedValueOverlay(options());
			store.startTransaction(true);
			store.gcIds(new java.util.HashSet<>(java.util.Set.of(id)), new java.util.HashSet<>());
			store.commit();
			store.clearCaches();
			assertSame(base, store.compressedValueOverlayStats());
			assertEquals(org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID, store.getId(value));
			assertEquals(value, store.getValue(id));
		} finally {
			store.close();
		}
	}

	@Test
	void selectiveMetadataAndLexicalVisitorMatchTheNativeRecord() throws Exception {
		var vf = SimpleValueFactory.getInstance();
		var store = new ValueStore(new File(directory, "selective"), new LmdbStoreConfig().setInlineLiterals(false));
		try {
			var value = vf.createLiteral("é/e\u0301/漢字/" + "compressed label ".repeat(400), "nB");
			store.startTransaction(true);
			long id = store.storeValue(value);
			store.commit();
			long datatype = store.literalDatatypeId(id);
			store.warmCompressedValueOverlay(options());
			store.clearCaches();
			assertEquals(datatype, store.literalDatatypeId(id));
			long[] ids = { id, id, id }, actual = new long[3];
			assertEquals(3, store.literalDatatypeIds(ids, 0, 3, actual, 0));
			assertArrayEquals(new long[] { datatype, datatype, datatype }, actual);
			var escaped = new org.eclipse.rdf4j.sail.lmdb.valueoverlay.ValueStoreRecordVisitor.Record[1];
			assertTrue(store.visitRecord(id, r -> {
				escaped[0] = r;
				assertEquals(datatype, r.datatypeId());
				assertTrue(r.lexicalEquals(value.getLabel().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
				assertFalse(r.lexicalStartsWith(new byte[] { 'z' }));
			}));
			assertThrows(IllegalStateException.class, () -> escaped[0].kind());
			store.clearCompressedValueOverlay();
			assertTrue(store.visitRecord(id, r -> assertEquals(datatype, r.datatypeId())));
		} finally {
			store.close();
		}
	}

	@Test
	void maintenanceCannotBeRequestedFromTheNativeWriter() throws Exception {
		var vf = SimpleValueFactory.getInstance();
		var store = new ValueStore(new File(directory, "worker"), new LmdbStoreConfig().setInlineLiterals(false));
		try {
			store.storeValue(vf.createLiteral("base"));
			store.warmCompressedValueOverlay(options());
			store.startTransaction(true);
			assertThrows(IllegalStateException.class, store::compactCompressedValueOverlay);
			store.storeValue(vf.createLiteral("private value"));
			store.rollback();
			assertNotNull(store.compressedValueOverlayStats());
			assertEquals(0, store.compactCompressedValueOverlay());
			assertTrue(store.compressedValueOverlayRetainedMemoryStats().nativeBytes() > 0);
			store.clear();
			store.storeValue(vf.createLiteral("after reset"));
			store.warmCompressedValueOverlay(options());
			assertNotNull(store.compressedValueOverlayStats());
		} finally {
			store.close();
		}
	}

}
