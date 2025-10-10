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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that corrupt or missing ValueStore files can be reconstructed from the ValueStore WAL, restoring consistent IDs
 * so existing triple indexes remain valid.
 */
class ValueStoreWalRecoveryCorruptionTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	@Test
	@Timeout(10)
	void rebuildsAfterDeletingAllValueFiles() throws Exception {
		File dataDir = tempDir.resolve("store").toFile();
		dataDir.mkdirs();

		// Pre-create an empty context index to avoid ContextStore reconstruction during init
		ensureEmptyContextIndex(dataDir.toPath());
		Repository repo = new SailRepository(new NativeStore(dataDir, "spoc,posc"));
		repo.init();

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			IRI exA = VF.createIRI("http://example.org/a0");
			IRI exB = VF.createIRI("http://example.org/b1");
			IRI exC = VF.createIRI("http://example.org/c2");
			Literal lit0 = VF.createLiteral("zero");
			Literal lit1 = VF.createLiteral("one");
			Literal lit2 = VF.createLiteral("two");
			Literal lit2en = VF.createLiteral("two", "en");
			Literal litTyped = VF.createLiteral(1.2);

			conn.add(exA, RDFS.LABEL, lit0);
			conn.add(exB, RDFS.LABEL, lit1, VF.createIRI("urn:one"));
			conn.add(exC, RDFS.LABEL, lit2, VF.createIRI("urn:two"));
			conn.add(exC, RDFS.LABEL, lit2, VF.createIRI("urn:two"));
			conn.add(Values.bnode(), RDF.TYPE, Values.bnode(), VF.createIRI("urn:two"));
			conn.add(exC, RDFS.LABEL, lit2en, VF.createIRI("urn:two"));
			conn.add(exC, RDFS.LABEL, litTyped, VF.createIRI("urn:two"));
			conn.commit();
		}

		repo.shutDown();

		// Simulate corruption: delete all ValueStore files
		deleteIfExists(dataDir.toPath().resolve("values.dat"));
		deleteIfExists(dataDir.toPath().resolve("values.id"));
		deleteIfExists(dataDir.toPath().resolve("values.hash"));

		recoverValueStoreFromWal(dataDir.toPath());
		validateDictionaryMatchesWal(dataDir.toPath());
	}

	@Test
	@Timeout(10)
	void rebuildsAfterCorruptingValuesDat() throws Exception {
		File dataDir = tempDir.resolve("store2").toFile();
		dataDir.mkdirs();

		ensureEmptyContextIndex(dataDir.toPath());
		Repository repo = new SailRepository(new NativeStore(dataDir, "spoc,posc"));
		repo.init();
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.add(VF.createIRI("http://ex.com/s"), RDFS.LABEL, VF.createLiteral("hello"));
			conn.add(VF.createIRI("http://ex.com/t"), RDFS.LABEL, VF.createLiteral("world", "en"));
			conn.add(VF.createIRI("http://ex.com/u"), RDFS.LABEL, VF.createLiteral(42));
			conn.commit();
		}
		repo.shutDown();

		Path valuesDat = dataDir.toPath().resolve("values.dat");
		if (Files.exists(valuesDat)) {
			Files.newByteChannel(valuesDat, Set.of(StandardOpenOption.WRITE))
					.truncate(0)
					.close();
		}

		recoverValueStoreFromWal(dataDir.toPath());
		validateDictionaryMatchesWal(dataDir.toPath());
	}

	private void deleteIfExists(Path path) throws IOException {
		if (Files.exists(path)) {
			Files.delete(path);
		}
	}

	private void recoverValueStoreFromWal(Path dataDir) throws Exception {
		Path walDir = dataDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Path uuidFile = walDir.resolve("store.uuid");
		String storeUuid = Files.exists(uuidFile) ? Files.readString(uuidFile, StandardCharsets.UTF_8).trim()
				: UUID.randomUUID().toString();

		ValueStoreWalConfig config = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid(storeUuid).build();

		Map<Integer, ValueStoreWalRecord> dictionary;
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
			dictionary = new LinkedHashMap<>(recovery.replay(reader));
		}

		try (DataStore ds = new DataStore(dataDir.toFile(), "values", false)) {
			for (ValueStoreWalRecord record : dictionary.values()) {
				switch (record.valueKind()) {
				case NAMESPACE: {
					byte[] nsBytes = record.lexical().getBytes(StandardCharsets.UTF_8);
					ds.storeData(nsBytes);
					break;
				}
				case IRI: {
					byte[] iriBytes = encodeIri(record.lexical(), ds);
					ds.storeData(iriBytes);
					break;
				}
				case BNODE: {
					byte[] idData = record.lexical().getBytes(StandardCharsets.UTF_8);
					byte[] bnode = new byte[1 + idData.length];
					bnode[0] = 0x2;
					ByteArrayUtil.put(idData, bnode, 1);
					ds.storeData(bnode);
					break;
				}
				case LITERAL: {
					byte[] litBytes = encodeLiteral(record.lexical(), record.datatype(), record.language(), ds);
					ds.storeData(litBytes);
					break;
				}
				default:
					break;
				}
			}
			ds.sync();
		}
	}

	private byte[] encodeIri(String lexical, DataStore ds) throws IOException {
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

	private byte[] encodeLiteral(String label, String datatype, String language, DataStore ds) throws IOException {
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

	private void validateDictionaryMatchesWal(Path dataDir) throws Exception {
		Path walDir = dataDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		String storeUuid = Files.readString(walDir.resolve("store.uuid"), StandardCharsets.UTF_8).trim();
		ValueStoreWalConfig config = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid(storeUuid).build();

		Map<Integer, ValueStoreWalRecord> dictionary;
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
			dictionary = new LinkedHashMap<>(recovery.replay(reader));
		}

		try (ValueStore vs = new ValueStore(dataDir.toFile(), false, ValueStore.VALUE_CACHE_SIZE,
				ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE,
				null)) {
			for (ValueStoreWalRecord record : dictionary.values()) {
				switch (record.valueKind()) {
				case IRI: {
					IRI iri = VF.createIRI(record.lexical());
					int id = vs.getID(iri);
					assertThat(id).isNotEqualTo(-1);
					assertThat(vs.getValue(id).stringValue()).isEqualTo(record.lexical());
					break;
				}
				case BNODE: {
					int id = vs.getID(VF.createBNode(record.lexical()));
					assertThat(id).isNotEqualTo(-1);
					assertThat(vs.getValue(id).stringValue()).isEqualTo(record.lexical());
					break;
				}
				case LITERAL: {
					Literal lit;
					if (record.language() != null && !record.language().isEmpty()) {
						lit = VF.createLiteral(record.lexical(), record.language());
					} else if (record.datatype() != null && !record.datatype().isEmpty()) {
						lit = VF.createLiteral(record.lexical(), VF.createIRI(record.datatype()));
					} else {
						lit = VF.createLiteral(record.lexical());
					}
					int id = vs.getID(lit);
					assertThat(id).isNotEqualTo(-1);
					assertThat(vs.getValue(id).stringValue()).isEqualTo(lit.stringValue());
					break;
				}
				case NAMESPACE:
					// Namespaces indirectly validated via IRIs
					break;
				default:
					break;
				}
			}
		}
	}

	private void ensureEmptyContextIndex(Path dataDir) throws IOException {
		Path file = dataDir.resolve("contexts.dat");
		if (Files.exists(file)) {
			return;
		}
		Files.createDirectories(dataDir);
		try (var out = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(file.toFile())))) {
			out.write(new byte[] { 'n', 'c', 'f' });
			out.writeByte(1);
			out.writeInt(0);
			out.flush();
		}
	}
}
