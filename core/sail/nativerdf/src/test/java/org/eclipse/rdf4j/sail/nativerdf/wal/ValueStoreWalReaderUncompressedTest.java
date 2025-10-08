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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32C;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Exercises ValueStoreWalReader's uncompressed path by writing a minimal .v1 segment by hand and verifying iteration.
 */
class ValueStoreWalReaderUncompressedTest {

	@TempDir
	Path tempDir;

	@Test
	void readsMintedRecordsFromUncompressedSegment() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);

		// Build a minimal uncompressed segment with header (V) and two minted (M) records
		Path seg = walDir.resolve("wal-100.v1");
		byte[] segmentBytes = buildUncompressedSegment("store-" + UUID.randomUUID(), 1, 100);
		Files.write(seg, segmentBytes);

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid("store-irrelevant")
				.build();

		List<ValueStoreWalRecord> records = new ArrayList<>();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			var it = reader.iterator();
			while (it.hasNext()) {
				records.add(it.next());
			}
			assertThat(reader.lastValidLsn()).isEqualTo(2L);
			assertThat(reader.isComplete()).isTrue();
		}
		assertThat(records).hasSize(2);
		assertThat(records.get(0).id()).isEqualTo(100);
		assertThat(records.get(1).id()).isEqualTo(101);
		assertThat(records.get(0).valueKind()).isEqualTo(ValueStoreWalValueKind.IRI);
		assertThat(records.get(1).valueKind()).isEqualTo(ValueStoreWalValueKind.LITERAL);
	}

	private static byte[] buildUncompressedSegment(String storeUuid, int segmentSeq, int firstId) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		// header frame
		byte[] hdr = headerJson(storeUuid, segmentSeq, firstId);
		frame(out, hdr);
		// minted 1
		byte[] m1 = mintedJson(1L, firstId, "I", "http://example.com/x", "", "", 123);
		frame(out, m1);
		// minted 2
		byte[] m2 = mintedJson(2L, firstId + 1, "L", "hello", "http://www.w3.org/2001/XMLSchema#string", "", 456);
		frame(out, m2);
		return out.toByteArray();
	}

	private static void frame(ByteArrayOutputStream out, byte[] json) {
		ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(json.length);
		buf.flip();
		out.write(buf.array(), 0, 4);
		out.write(json, 0, json.length);
		CRC32C c = new CRC32C();
		c.update(json, 0, json.length);
		ByteBuffer crc = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) c.getValue());
		crc.flip();
		out.write(crc.array(), 0, 4);
	}

	private static byte[] headerJson(String store, int segment, int firstId) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "V");
			g.writeNumberField("ver", 1);
			g.writeStringField("store", store);
			g.writeStringField("engine", "valuestore");
			g.writeNumberField("created", 0);
			g.writeNumberField("segment", segment);
			g.writeNumberField("firstId", firstId);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}

	private static byte[] mintedJson(long lsn, int id, String vk, String lex, String dt, String lang, int hash)
			throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "M");
			g.writeNumberField("lsn", lsn);
			g.writeNumberField("id", id);
			g.writeStringField("vk", vk);
			g.writeStringField("lex", lex == null ? "" : lex);
			g.writeStringField("dt", dt == null ? "" : dt);
			g.writeStringField("lang", lang == null ? "" : lang);
			g.writeNumberField("hash", hash);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}
}
