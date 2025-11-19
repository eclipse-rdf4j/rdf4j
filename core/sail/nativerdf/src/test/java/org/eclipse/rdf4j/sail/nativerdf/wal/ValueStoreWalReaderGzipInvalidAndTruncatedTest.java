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
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Tests gzip path for invalid length and truncated CRC conditions.
 */
class ValueStoreWalReaderGzipInvalidAndTruncatedTest {

	@TempDir
	Path tempDir;

	@Test
	void invalidLengthMarksIncomplete() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		Path gz = walDir.resolve("wal-1.v1.gz");
		try (GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
			// Write header frame correctly
			frame(out, headerJson(1, 1));
			// Write an invalid frame length (0) and nothing else
			ByteBuffer lb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0);
			lb.flip();
			out.write(lb.array(), 0, 4);
			out.finish();
		}
		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			ValueStoreWalReader.ScanResult res = reader.scan();
			assertThat(res.complete()).isFalse();
			assertThat(res.records()).isEmpty();
		}
	}

	@Test
	void truncatedCrcMarksIncomplete() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		Path gz = walDir.resolve("wal-2.v1.gz");
		try (GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
			// Header frame
			frame(out, headerJson(2, 1));
			// Minted frame with correct length and payload but omit CRC
			byte[] json = mintedJson(1L, 1);
			ByteBuffer lb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(json.length);
			lb.flip();
			out.write(lb.array(), 0, 4);
			out.write(json);
			// no CRC written -> truncated
			out.finish();
		}
		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			ValueStoreWalReader.ScanResult res = reader.scan();
			assertThat(res.complete()).isFalse();
			assertThat(res.records()).isEmpty();
		}
	}

	private static void frame(GZIPOutputStream out, byte[] json) throws IOException {
		ByteBuffer lb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(json.length);
		lb.flip();
		out.write(lb.array(), 0, 4);
		out.write(json);
		int crc = crc32c(json);
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
