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
package org.eclipse.rdf4j.sail.nativerdf.wal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.common.io.NioFile;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.testutil.FailureInjectingFileChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves that a NativeStore with forceSync disabled can be fully recovered from a WAL that runs with SyncPolicy.COMMIT
 * and synchronous bootstrap on open ensuring durability before commit returns.
 */
class ValueStoreWalDurabilityRecoveryTest {

	@TempDir
	Path tempDir;

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@Test
	void recoversFromLostValueStoreUsingWALCommitDurability() throws Exception {
		// Install a delegating FileChannel factory (no failures by default), proving injection works
		NioFile.setChannelFactoryForTesting(
				(path, options) -> new FailureInjectingFileChannel(java.nio.channels.FileChannel.open(path, options)));

		File dataDir = tempDir.resolve("store").toFile();
		dataDir.mkdirs();

		NativeStore store = new NativeStore(dataDir, "spoc,posc");
		store.setForceSync(false); // ValueStore won't fsync
		store.setWalSyncPolicy(ValueStoreWalConfig.SyncPolicy.COMMIT); // WAL fsyncs on commit
		store.setWalSyncBootstrapOnOpen(false);
		Repository repo = new SailRepository(store);
		repo.init();

		IRI p = VF.createIRI("http://ex/p");
		IRI s = VF.createIRI("http://ex/s");
		IRI o = VF.createIRI("http://ex/o");
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.add(s, p, o, Values.iri("urn:g"));
			conn.commit(); // WAL should force+persist before this returns
		}
		repo.shutDown();

		// Simulate crash that loses the ValueStore by deleting the value files, WAL remains
		Files.deleteIfExists(dataDir.toPath().resolve("values.dat"));
		Files.deleteIfExists(dataDir.toPath().resolve("values.id"));
		Files.deleteIfExists(dataDir.toPath().resolve("values.hash"));

		// Manually recover the ValueStore from WAL to simulate crash recovery
		Path walDir = dataDir.toPath().resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		String storeUuid = Files.readString(walDir.resolve("store.uuid"), StandardCharsets.UTF_8).trim();
		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid(storeUuid).build();
		java.util.Map<Integer, ValueStoreWalRecord> dictionary;
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
			dictionary = new java.util.LinkedHashMap<>(recovery.replay(reader));
		}
		try (org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore ds = new org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore(
				dataDir, "values", false)) {
			for (ValueStoreWalRecord record : dictionary.values()) {
				switch (record.valueKind()) {
				case NAMESPACE:
					ds.storeData(record.lexical().getBytes(StandardCharsets.UTF_8));
					break;
				case IRI:
					ds.storeData(encodeIri(record.lexical(), ds));
					break;
				case BNODE:
					byte[] idData = record.lexical().getBytes(StandardCharsets.UTF_8);
					byte[] bnode = new byte[1 + idData.length];
					bnode[0] = 0x2;
					ByteArrayUtil.put(idData, bnode, 1);
					ds.storeData(bnode);
					break;
				default:
					ds.storeData(encodeLiteral(record.lexical(), record.datatype(), record.language(), ds));
					break;
				}
			}
			ds.sync();
		}

		// Restart store and verify statement is readable (dictionary present)
		NativeStore store2 = new NativeStore(dataDir, "spoc,posc");
		store2.setForceSync(false);
		store2.setWalSyncPolicy(ValueStoreWalConfig.SyncPolicy.COMMIT);
		store2.setWalSyncBootstrapOnOpen(true);
		Repository repo2 = new SailRepository(store2);
		repo2.init();
		try (RepositoryConnection conn = repo2.getConnection()) {
			long count = conn.getStatements(s, p, o, false, Values.iri("urn:g")).stream().count();
			assertThat(count).isEqualTo(1L);
		}
		repo2.shutDown();

		// Remove factory to avoid impacting other tests
		NioFile.setChannelFactoryForTesting(null);
	}

	private byte[] encodeIri(String lexical, org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore ds) throws Exception {
		IRI iri = VF.createIRI(lexical);
		String ns = iri.getNamespace();
		String local = iri.getLocalName();
		int nsId = ds.getID(ns.getBytes(StandardCharsets.UTF_8));
		if (nsId == -1) {
			nsId = ds.storeData(ns.getBytes(StandardCharsets.UTF_8));
		}
		byte[] localBytes = local.getBytes(StandardCharsets.UTF_8);
		byte[] data = new byte[1 + 4 + localBytes.length];
		data[0] = 0x1;
		ByteArrayUtil.putInt(nsId, data, 1);
		ByteArrayUtil.put(localBytes, data, 5);
		return data;
	}

	private byte[] encodeLiteral(String label, String datatype, String language,
			org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore ds) throws Exception {
		int dtId = -1; // -1 denotes UNKNOWN_ID
		if (datatype != null && !datatype.isEmpty()) {
			byte[] dtBytes = encodeIri(datatype, ds);
			int id = ds.getID(dtBytes);
			dtId = id == -1 ? ds.storeData(dtBytes) : id;
		}
		byte[] langBytes = language == null ? new byte[0] : language.getBytes(StandardCharsets.UTF_8);
		byte[] labelBytes = label.getBytes(StandardCharsets.UTF_8);
		byte[] data = new byte[1 + 4 + 1 + langBytes.length + labelBytes.length];
		data[0] = 0x3;
		ByteArrayUtil.putInt(dtId, data, 1);
		data[5] = (byte) (langBytes.length & 0xFF);
		if (langBytes.length > 0) {
			ByteArrayUtil.put(langBytes, data, 6);
		}
		ByteArrayUtil.put(labelBytes, data, 6 + langBytes.length);
		return data;
	}
}
