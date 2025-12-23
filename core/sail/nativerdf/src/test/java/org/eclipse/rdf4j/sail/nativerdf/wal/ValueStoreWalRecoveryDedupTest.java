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
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Ensures ValueStoreWalRecovery keeps the first occurrence of a duplicated id encountered across segments.
 */
class ValueStoreWalRecoveryDedupTest {

	@TempDir
	Path tempDir;

	@Test
	void keepsFirstOccurrenceOfId() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);

		// Segment seq=1 with id=100 lex="first"
		Files.write(walDir.resolve("wal-100.v1"), segmentBytes(1, 100, "first"));
		// Segment seq=2 with id=100 lex="second"
		Files.write(walDir.resolve("wal-200.v1"), segmentBytes(2, 100, "second"));

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid("s")
				.build();
		Map<Integer, ValueStoreWalRecord> dict;
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			dict = new ValueStoreWalRecovery().replay(reader);
		}
		assertThat(dict).containsKey(100);
		assertThat(dict.get(100).lexical()).isEqualTo("first"); // first occurrence retained
	}

	private static byte[] segmentBytes(int segment, int id, String lex) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		frame(out, header(segment, id));
		frame(out, minted(1L, id, lex));
		return out.toByteArray();
	}

	private static byte[] header(int segment, int firstId) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "V");
			g.writeNumberField("ver", 1);
			g.writeStringField("store", "s");
			g.writeStringField("engine", "valuestore");
			g.writeNumberField("created", 0);
			g.writeNumberField("segment", segment);
			g.writeNumberField("firstId", firstId);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}

	private static byte[] minted(long lsn, int id, String lex) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "M");
			g.writeNumberField("lsn", lsn);
			g.writeNumberField("id", id);
			g.writeStringField("vk", "I");
			g.writeStringField("lex", lex);
			g.writeStringField("dt", "");
			g.writeStringField("lang", "");
			g.writeNumberField("hash", 0);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}

	private static void frame(ByteArrayOutputStream out, byte[] json) {
		ByteBuffer len = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(json.length);
		len.flip();
		out.write(len.array(), 0, 4);
		out.write(json, 0, json.length);
		int crc = java.util.zip.CRC32C.class.desiredAssertionStatus() ? 0 : 0; // keep import minimal
		java.util.zip.CRC32C c = new java.util.zip.CRC32C();
		c.update(json, 0, json.length);
		crc = (int) c.getValue();
		ByteBuffer crcBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(crc);
		crcBuf.flip();
		out.write(crcBuf.array(), 0, 4);
	}
}
