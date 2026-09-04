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
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

@Tag("slow")
class ValueStoreWalReadSegmentSequenceTest {

	@TempDir
	Path tempDir;

	@Test
	void readsSequenceFromUncompressed() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		Path seg = walDir.resolve("wal-1.v1");
		Files.write(seg, buildHeaderFrame("store-" + UUID.randomUUID(), 42, 1));
		int seq = ValueStoreWAL.readSegmentSequence(seg);
		assertThat(seq).isEqualTo(42);
	}

	@Test
	void readsSequenceFromCompressed() throws Exception {
		Path walDir = tempDir.resolve("wal-gz");
		Files.createDirectories(walDir);
		Path gz = walDir.resolve("wal-10.v1.gz");
		byte[] header = buildHeaderFrame("store-" + UUID.randomUUID(), 7, 10);
		try (GZIPOutputStream gout = new GZIPOutputStream(Files.newOutputStream(gz))) {
			gout.write(header);
			gout.finish();
		}
		int seq = ValueStoreWAL.readSegmentSequence(gz);
		assertThat(seq).isEqualTo(7);
	}

	private static byte[] buildHeaderFrame(String store, int segment, int firstId) throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), baos)) {
			g.writeStartObject();
			g.writeStringProperty("t", "V");
			g.writeNumberProperty("ver", 1);
			g.writeStringProperty("store", store);
			g.writeStringProperty("engine", "valuestore");
			g.writeNumberProperty("created", 0);
			g.writeNumberProperty("segment", segment);
			g.writeNumberProperty("firstId", firstId);
			g.writeEndObject();
		}
		baos.write('\n');
		byte[] json = baos.toByteArray();
		ByteBuffer buf = ByteBuffer.allocate(4 + json.length + 4).order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(json.length);
		buf.put(json);
		buf.putInt(0); // CRC is ignored by readSegmentSequence
		buf.flip();
		byte[] framed = new byte[buf.remaining()];
		buf.get(framed);
		return framed;
	}
}
