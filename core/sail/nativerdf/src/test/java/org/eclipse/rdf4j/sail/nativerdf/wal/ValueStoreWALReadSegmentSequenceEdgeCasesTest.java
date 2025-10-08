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

class ValueStoreWALReadSegmentSequenceEdgeCasesTest {

	@TempDir
	Path tempDir;

	@Test
	void returnsZeroForEmptyOrShortFiles() throws Exception {
		Path empty = tempDir.resolve("wal-empty.v1");
		Files.write(empty, new byte[0]);
		assertThat(ValueStoreWAL.readSegmentSequence(empty)).isEqualTo(0);

		Path shortHdr = tempDir.resolve("wal-short.v1");
		Files.write(shortHdr, new byte[] { 1, 2 });
		assertThat(ValueStoreWAL.readSegmentSequence(shortHdr)).isEqualTo(0);
	}

	@Test
	void returnsZeroForNonPositiveLengthAndTruncatedJson() throws Exception {
		Path lenZero = tempDir.resolve("wal-lenzero.v1");
		ByteBuffer b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0);
		b.flip();
		Files.write(lenZero, b.array());
		assertThat(ValueStoreWAL.readSegmentSequence(lenZero)).isEqualTo(0);

		Path trunc = tempDir.resolve("wal-trunc.v1");
		ByteBuffer hdr = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16);
		hdr.flip();
		Files.write(trunc, hdr.array());
		assertThat(ValueStoreWAL.readSegmentSequence(trunc)).isEqualTo(0);
	}

	@Test
	void returnsZeroWhenHeaderHasNoSegmentField() throws Exception {
		Path noseg = tempDir.resolve("wal-noseg.v1");
		byte[] json = headerWithoutSegment();
		ByteBuffer out = ByteBuffer.allocate(4 + json.length + 4).order(ByteOrder.LITTLE_ENDIAN);
		out.putInt(json.length);
		out.put(json);
		out.putInt(0);
		out.flip();
		byte[] data = new byte[out.remaining()];
		out.get(data);
		Files.write(noseg, data);
		assertThat(ValueStoreWAL.readSegmentSequence(noseg)).isEqualTo(0);
	}

	private static byte[] headerWithoutSegment() throws IOException {
		JsonFactory f = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator g = f.createGenerator(baos)) {
			g.writeStartObject();
			g.writeStringField("t", "V");
			g.writeNumberField("ver", 1);
			g.writeStringField("store", "s");
			g.writeStringField("engine", "valuestore");
			g.writeNumberField("created", 0);
			g.writeNumberField("firstId", 1);
			g.writeEndObject();
		}
		baos.write('\n');
		return baos.toByteArray();
	}
}
