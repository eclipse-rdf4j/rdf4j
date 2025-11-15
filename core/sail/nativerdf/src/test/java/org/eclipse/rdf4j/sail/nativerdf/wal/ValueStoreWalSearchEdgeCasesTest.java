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
import java.util.zip.CRC32C;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

class ValueStoreWalSearchEdgeCasesTest {

	@TempDir
	Path tempDir;

	@Test
	void returnsNullWhenIdOutsideRange() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		// Segment with firstId=10 and minted ids 10, 20
		Files.write(walDir.resolve("wal-10.v1"), segmentWithTwoIds(1, 10, 10, 20));

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		ValueStoreWalSearch search = ValueStoreWalSearch.open(cfg);
		Value vLow = search.findValueById(5); // before first
		Value vHigh = search.findValueById(100); // after last
		assertThat(vLow).isNull();
		assertThat(vHigh).isNull();
	}

	@Test
	void refreshesSegmentCacheAfterRotation() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		byte[] segment = segmentWithTwoIds(1, 10, 10, 20);
		Path plainSegment = walDir.resolve("wal-10.v1");
		Files.write(plainSegment, segment);

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		ValueStoreWalSearch search = ValueStoreWalSearch.open(cfg);

		Value initial = search.findValueById(20);
		assertThat(initial).isNotNull();

		Path gzSegment = walDir.resolve("wal-10.v1.gz");
		Files.write(gzSegment, gzip(segment));
		Files.deleteIfExists(plainSegment);

		Value rotated = search.findValueById(20);
		assertThat(rotated).isNotNull();
		assertThat(rotated).isEqualTo(initial);
	}

	private static byte[] segmentWithTwoIds(int seq, int firstId, int id1, int id2) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		frame(out, header(seq, firstId));
		frame(out, minted(id1, "I", "http://ex/i" + id1));
		frame(out, minted(id2, "I", "http://ex/i" + id2));
		return out.toByteArray();
	}

	private static byte[] header(int seq, int firstId) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "V");
			g.writeNumberField("ver", 1);
			g.writeStringField("store", "s");
			g.writeStringField("engine", "valuestore");
			g.writeNumberField("created", 0);
			g.writeNumberField("segment", seq);
			g.writeNumberField("firstId", firstId);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}

	private static byte[] minted(int id, String vk, String lex) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "M");
			g.writeNumberField("lsn", id); // monotonic for simplicity
			g.writeNumberField("id", id);
			g.writeStringField("vk", vk);
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
		ByteBuffer length = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(json.length);
		length.flip();
		out.write(length.array(), 0, 4);
		out.write(json, 0, json.length);
		CRC32C crc32c = new CRC32C();
		crc32c.update(json, 0, json.length);
		ByteBuffer crc = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) crc32c.getValue());
		crc.flip();
		out.write(crc.array(), 0, 4);
	}

	private static byte[] gzip(byte[] data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
			gzip.write(data);
		}
		return baos.toByteArray();
	}
}
