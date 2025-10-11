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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalRecoveryRebuildTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	@Test
	void rebuildAssignsExactIds() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		IRI iri = VF.createIRI("http://example.com/res");
		Literal lit = VF.createLiteral("value", "en");

		// Mint values and persist WAL
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			File valueDir = tempDir.resolve("values").toFile();
			Files.createDirectories(valueDir.toPath());
			try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
					ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				store.storeValue(iri);
				store.storeValue(lit);
				var lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		Map<Integer, ValueStoreWalRecord> dictionary;
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
			dictionary = new LinkedHashMap<>(recovery.replay(reader));
		}
		assertThat(dictionary).isNotEmpty();

		// Rebuild DataStore directly from WAL dictionary
		File dataDir = tempDir.resolve("rebuilt").toFile();
		Files.createDirectories(dataDir.toPath());
		try (DataStore ds = new DataStore(dataDir, "values", false)) {
			for (ValueStoreWalRecord rec : dictionary.values()) {
				if (rec.valueKind() == ValueStoreWalValueKind.NAMESPACE) {
					ds.storeData(rec.lexical().getBytes(StandardCharsets.UTF_8));
				} else if (rec.valueKind() == ValueStoreWalValueKind.IRI) {
					ds.storeData(encodeIri(rec.lexical(), ds));
				} else if (rec.valueKind() == ValueStoreWalValueKind.BNODE) {
					byte[] idData = rec.lexical().getBytes(StandardCharsets.UTF_8);
					byte[] bnode = new byte[1 + idData.length];
					bnode[0] = 0x2; // BNODE tag
					ByteArrayUtil.put(idData, bnode, 1);
					ds.storeData(bnode);
				} else if (rec.valueKind() == ValueStoreWalValueKind.LITERAL) {
					ds.storeData(encodeLiteral(rec.lexical(), rec.datatype(), rec.language(), ds));
				}
			}
			ds.sync();
		}

		// Verify exact id equality using ValueStore on rebuilt data
		try (ValueStore vs = new ValueStore(dataDir, false, ValueStore.VALUE_CACHE_SIZE,
				ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
				ValueStore.NAMESPACE_ID_CACHE_SIZE, null)) {
			for (ValueStoreWalRecord rec : dictionary.values()) {
				switch (rec.valueKind()) {
				case IRI:
					assertThat(vs.getID(VF.createIRI(rec.lexical()))).isEqualTo(rec.id());
					break;
				case BNODE:
					assertThat(vs.getID(VF.createBNode(rec.lexical()))).isEqualTo(rec.id());
					break;
				case LITERAL:
					Literal l = (rec.language() != null && !rec.language().isEmpty())
							? VF.createLiteral(rec.lexical(), rec.language())
							: (rec.datatype() != null && !rec.datatype().isEmpty())
									? VF.createLiteral(rec.lexical(), VF.createIRI(rec.datatype()))
									: VF.createLiteral(rec.lexical());
					assertThat(vs.getID(l)).isEqualTo(rec.id());
					break;
				default:
					// skip NAMESPACE here
				}
			}
		}
	}

	@Test
	void missingSegmentMarksIncomplete() throws Exception {
		Path walDir = tempDir.resolve("wal-missing");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.maxSegmentBytes(1 << 12)
				.build();

		Path valueDir = tempDir.resolve("values-missing");
		Files.createDirectories(valueDir);
		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valueDir.toFile(), false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
			for (int i = 0; i < 200; i++) {
				store.storeValue(VF.createIRI("http://example.com/value/" + i));
			}
			OptionalLong lsn = store.drainPendingWalHighWaterMark();
			if (lsn.isPresent()) {
				store.awaitWalDurable(lsn.getAsLong());
			}
		}

		List<Path> segments;
		try (var stream = Files.list(walDir)) {
			segments = stream.filter(p -> p.getFileName().toString().startsWith("wal-"))
					.sorted()
					.collect(Collectors.toList());
		}
		assertThat(segments).hasSizeGreaterThan(1);
		Files.deleteIfExists(segments.get(0));

		ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
		ValueStoreWalRecovery.ReplayReport report;
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			report = recovery.replayWithReport(reader);
		}
		assertThat(report.complete()).isFalse();
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
		data[0] = 0x1; // URI tag
		ByteArrayUtil.putInt(nsId, data, 1);
		ByteArrayUtil.put(localBytes, data, 5);
		return data;
	}

	private byte[] encodeLiteral(String label, String datatype, String language, DataStore ds) throws IOException {
		int dtId = -1; // UNKNOWN_ID
		if (datatype != null && !datatype.isEmpty()) {
			byte[] dtBytes = encodeIri(datatype, ds);
			int id = ds.getID(dtBytes);
			dtId = id == -1 ? ds.storeData(dtBytes) : id;
		}
		byte[] langBytes = language == null ? new byte[0] : language.getBytes(StandardCharsets.UTF_8);
		byte[] labelBytes = label.getBytes(StandardCharsets.UTF_8);
		byte[] data = new byte[1 + 4 + 1 + langBytes.length + labelBytes.length];
		data[0] = 0x3; // LITERAL tag
		ByteArrayUtil.putInt(dtId, data, 1);
		data[5] = (byte) (langBytes.length & 0xFF);
		if (langBytes.length > 0) {
			ByteArrayUtil.put(langBytes, data, 6);
		}
		ByteArrayUtil.put(labelBytes, data, 6 + langBytes.length);
		return data;
	}
}
