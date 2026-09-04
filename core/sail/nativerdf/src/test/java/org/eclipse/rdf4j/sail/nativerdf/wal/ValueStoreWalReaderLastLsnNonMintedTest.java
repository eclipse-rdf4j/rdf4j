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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

/**
 * Verifies that encountering non-minted frames (header 'V' and summary 'S') does not alter lastValidLsn; it should
 * reflect the last minted record's LSN only.
 */
@Tag("slow")
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
		try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), baos)) {
			g.writeStartObject();
			g.writeStringProperty("t", "V");
			g.writeNumberProperty("ver", 1);
			g.writeStringProperty("store", "s");
			g.writeStringProperty("engine", "valuestore");
			g.writeNumberProperty("created", 0);
			g.writeNumberProperty("segment", segment);
			g.writeNumberProperty("firstId", firstId);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}

	private static byte[] mintedJson(long lsn, int id) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), baos)) {
			g.writeStartObject();
			g.writeStringProperty("t", "M");
			g.writeNumberProperty("lsn", lsn);
			g.writeNumberProperty("id", id);
			g.writeStringProperty("vk", "I");
			g.writeStringProperty("lex", "http://ex/id" + id);
			g.writeStringProperty("dt", "");
			g.writeStringProperty("lang", "");
			g.writeNumberProperty("hash", 0);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}

	private static byte[] summaryJson(int lastId) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), baos)) {
			g.writeStartObject();
			g.writeStringProperty("t", "S");
			g.writeNumberProperty("lastId", lastId);
			g.writeNumberProperty("crc32", 0L);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}
}
