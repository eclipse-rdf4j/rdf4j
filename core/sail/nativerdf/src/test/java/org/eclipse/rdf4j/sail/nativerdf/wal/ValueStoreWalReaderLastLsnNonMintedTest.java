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
import java.util.zip.CRC32C;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Verifies that encountering non-minted frames (header 'V' and summary 'S') does not alter lastValidLsn; it should
 * reflect the last minted record's LSN only.
 */
class ValueStoreWalReaderLastLsnNonMintedTest {

	@TempDir
	Path tempDir;

	@Test
	void lastValidLsnIgnoresNonMintedFrames() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		Path seg = walDir.resolve("wal-1.v1");
		int mintedId = 10;
		long mintedLsn = 42L;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		frame(out, headerJson(1, 1));
		frame(out, mintedJson(mintedLsn, mintedId));
		frame(out, summaryJson(mintedId));
		Files.write(seg, out.toByteArray());

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		List<ValueStoreWalRecord> recs = new ArrayList<>();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			var it = reader.iterator();
			while (it.hasNext()) {
				recs.add(it.next());
			}
			assertThat(reader.lastValidLsn()).isEqualTo(mintedLsn);
		}
		assertThat(recs).hasSize(1);
		assertThat(recs.get(0).id()).isEqualTo(mintedId);
	}

	private static void frame(ByteArrayOutputStream out, byte[] json) {
		ByteBuffer len = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(json.length);
		len.flip();
		out.write(len.array(), 0, 4);
		out.write(json, 0, json.length);
		CRC32C c = new CRC32C();
		c.update(json, 0, json.length);
		ByteBuffer crc = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) c.getValue());
		crc.flip();
		out.write(crc.array(), 0, 4);
	}

	private static byte[] headerJson(int segment, int firstId) throws IOException {
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

	private static byte[] mintedJson(long lsn, int id) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "M");
			g.writeNumberField("lsn", lsn);
			g.writeNumberField("id", id);
			g.writeStringField("vk", "I");
			g.writeStringField("lex", "http://ex/id" + id);
			g.writeStringField("dt", "");
			g.writeStringField("lang", "");
			g.writeNumberField("hash", 0);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}

	private static byte[] summaryJson(int lastId) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "S");
			g.writeNumberField("lastId", lastId);
			g.writeNumberField("crc32", 0L);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}
}
