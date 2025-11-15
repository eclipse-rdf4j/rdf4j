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
 * Ensures listSegments tolerates unreadable/mis-typed entries that match the filename pattern by creating a directory
 * named like a segment. This exercises the catch(IOException) branch in the segment header read.
 */
class ValueStoreWalReaderListSegmentsUnreadableTest {

	@TempDir
	Path tempDir;

	@Test
	void unreadableSegmentHeaderIsTolerated() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		// Create a directory that matches the segment filename pattern -> readSegmentSequence will fail to open
		Files.createDirectory(walDir.resolve("wal-100.v1"));
		// Also create a valid uncompressed segment with sequence 1 so the reader has something to process
		Path seg = walDir.resolve("wal-1.v1");
		Files.write(seg, headerFrame(1, 1));

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			ValueStoreWalReader.ScanResult res = reader.scan();
			// Completeness may be false due to a sequence gap introduced by the unreadable item, but no exception
			// occurs
			assertThat(res.records()).isEmpty();
		}
	}

	private static byte[] headerFrame(int seq, int firstId) throws IOException {
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
		byte[] json = baos.toByteArray();
		ByteBuffer buf = ByteBuffer.allocate(4 + json.length + 4).order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(json.length);
		buf.put(json);
		buf.putInt(0); // CRC ignored in readSegmentSequence
		buf.flip();
		byte[] framed = new byte[buf.remaining()];
		buf.get(framed);
		return framed;
	}
}
