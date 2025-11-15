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
 * Ensures reader reports incomplete when segment sequences are non-contiguous (e.g., segments 1 and 3 present).
 */
class ValueStoreWalReaderHasSequenceGapsTest {

	@TempDir
	Path tempDir;

	@Test
	void sequenceGapsMarkIncomplete() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		Files.write(walDir.resolve("wal-10.v1"), headerOnly(1, 10));
		Files.write(walDir.resolve("wal-20.v1"), headerOnly(3, 20));

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			ValueStoreWalReader.ScanResult res = reader.scan();
			assertThat(res.records()).isEmpty();
			assertThat(res.complete()).isFalse();
		}
	}

	private static byte[] headerOnly(int segment, int firstId) throws IOException {
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
		byte[] json = baos.toByteArray();
		ByteBuffer buf = ByteBuffer.allocate(4 + json.length + 4).order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(json.length);
		buf.put(json);
		buf.putInt(0);
		buf.flip();
		byte[] framed = new byte[buf.remaining()];
		buf.get(framed);
		return framed;
	}
}
