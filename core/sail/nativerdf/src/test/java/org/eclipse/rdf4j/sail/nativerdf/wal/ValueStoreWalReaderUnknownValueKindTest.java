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

import static org.junit.jupiter.api.Assertions.assertThrows;

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
 * Verifies that encountering an unknown value kind code causes parsing to fail with IllegalArgumentException.
 */
class ValueStoreWalReaderUnknownValueKindTest {

	@TempDir
	Path tempDir;

	@Test
	void unknownValueKindThrows() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		Path seg = walDir.resolve("wal-1.v1");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		frame(out, headerJson(1, 1));
		frame(out, invalidMintedJson(2L, 2));
		Files.write(seg, out.toByteArray());

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			assertThrows(IllegalArgumentException.class, () -> {
				// Trigger parsing by iterating
				reader.scan();
			});
		}
	}

	private static void frame(ByteArrayOutputStream out, byte[] json) {
		ByteBuffer len = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(json.length);
		len.flip();
		out.write(len.array(), 0, 4);
		out.write(json, 0, json.length);
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

	private static byte[] invalidMintedJson(long lsn, int id) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "M");
			g.writeNumberField("lsn", lsn);
			g.writeNumberField("id", id);
			g.writeStringField("vk", "?"); // invalid code
			g.writeStringField("lex", "x");
			g.writeStringField("dt", "");
			g.writeStringField("lang", "");
			g.writeNumberField("hash", 0);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}
}
