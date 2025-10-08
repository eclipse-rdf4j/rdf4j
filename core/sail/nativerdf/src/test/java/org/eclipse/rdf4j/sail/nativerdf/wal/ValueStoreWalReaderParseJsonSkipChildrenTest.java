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
 * Crafts a minted frame with an extra nested object field to exercise parseJson's skipChildren branch.
 */
class ValueStoreWalReaderParseJsonSkipChildrenTest {

	@TempDir
	Path tempDir;

	@Test
	void mintedWithExtraNestedObjectIsParsedAndIgnored() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		Path seg = walDir.resolve("wal-1.v1");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Header
		byte[] hdr = headerJson(1, 1);
		out.write(lenLE(hdr.length));
		out.write(hdr);
		out.write(intLE(crc32c(hdr)));
		// Minted with extra nested object field "x": {"a":1}
		byte[] minted = mintedJsonWithExtra(123L, 1);
		out.write(lenLE(minted.length));
		out.write(minted);
		out.write(intLE(crc32c(minted)));
		Files.write(seg, out.toByteArray());

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			ValueStoreWalReader.ScanResult res = reader.scan();
			assertThat(res.complete()).isTrue();
			assertThat(res.records()).hasSize(1);
			assertThat(res.records().get(0).id()).isEqualTo(1);
			assertThat(res.lastValidLsn()).isEqualTo(123L);
		}
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

	private static byte[] mintedJsonWithExtra(long lsn, int id) throws IOException {
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
			// Extra nested object to trigger skipChildren
			g.writeObjectFieldStart("x");
			g.writeNumberField("a", 1);
			g.writeEndObject();
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}

	private static byte[] lenLE(int v) {
		ByteBuffer b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v);
		b.flip();
		byte[] a = new byte[4];
		b.get(a);
		return a;
	}

	private static byte[] intLE(int v) {
		return lenLE(v);
	}

	private static int crc32c(byte[] data) {
		java.util.zip.CRC32C c = new java.util.zip.CRC32C();
		c.update(data, 0, data.length);
		return (int) c.getValue();
	}
}
