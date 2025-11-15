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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Ensures uncompressed reader stops and marks incomplete when CRC32C mismatches.
 */
class ValueStoreWalReaderUncompressedCrcMismatchTest {

	@TempDir
	Path tempDir;

	@Test
	void crcMismatchStopsScan() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		Path seg = walDir.resolve("wal-1.v1");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Valid header
		frame(out, headerJson(1, 1), true);
		// Minted record with wrong CRC
		frame(out, mintedJson(1L, 1), false);
		Files.write(seg, out.toByteArray());

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			ValueStoreWalReader.ScanResult res = reader.scan();
			assertThat(res.complete()).isFalse();
			assertThat(res.records()).isEmpty();
		}
	}

	private static void frame(ByteArrayOutputStream out, byte[] json, boolean correctCrc) {
		ByteBuffer len = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(json.length);
		len.flip();
		out.write(len.array(), 0, 4);
		out.write(json, 0, json.length);
		int crc = correctCrc ? crc32c(json) : 0xDEADBEEF; // wrong CRC
		ByteBuffer cb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(crc);
		cb.flip();
		out.write(cb.array(), 0, 4);
	}

	private static int crc32c(byte[] data) {
		java.util.zip.CRC32C c = new java.util.zip.CRC32C();
		c.update(data, 0, data.length);
		return (int) c.getValue();
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
}
