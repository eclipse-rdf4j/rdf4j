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
 * Ensures the reader marks incomplete when a frame is truncated (length OK, payload/CRC missing).
 */
class ValueStoreWalReaderTruncatedRecordTest {

	@TempDir
	Path tempDir;

	@Test
	void truncatedFrameMarksIncomplete() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);

		Path seg = walDir.resolve("wal-1.v1");
		byte[] header = headerFrame();
		// Create a frame header with non-zero length but write no payload/CRC
		ByteBuffer len = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16);
		len.flip();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(header);
		out.write(len.array(), 0, 4);
		Files.write(seg, out.toByteArray());

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			ValueStoreWalReader.ScanResult scan = reader.scan();
			assertThat(scan.complete()).isFalse();
			assertThat(scan.records()).isEmpty();
		}
	}

	private static byte[] headerFrame() throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "V");
			g.writeNumberField("ver", 1);
			g.writeStringField("store", "s");
			g.writeStringField("engine", "valuestore");
			g.writeNumberField("created", 0);
			g.writeNumberField("segment", 1);
			g.writeNumberField("firstId", 1);
			g.writeEndObject();
		}
		baos.write('\n');
		byte[] json = baos.toByteArray();
		ByteBuffer frame = ByteBuffer.allocate(4 + json.length + 4).order(ByteOrder.LITTLE_ENDIAN);
		frame.putInt(json.length);
		frame.put(json);
		frame.putInt(0);
		frame.flip();
		byte[] framed = new byte[frame.remaining()];
		frame.get(framed);
		return framed;
	}
}
