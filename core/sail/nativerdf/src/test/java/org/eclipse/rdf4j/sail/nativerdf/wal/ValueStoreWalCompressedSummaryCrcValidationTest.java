/**
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.rdf4j.sail.nativerdf.wal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.CRC32C;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Validates that ValueStoreWalReader verifies the CRC32 summary embedded in compressed segments and marks the scan as
 * incomplete when the summary does not match the decompressed content.
 */
class ValueStoreWalCompressedSummaryCrcValidationTest {

	private static final Pattern SEGMENT_GZ = Pattern.compile("wal-(\\d+)\\.v1\\.gz");

	@TempDir
	Path tempDir;

	@Test
	void mismatchSummaryCrcMarksScanIncomplete() throws Exception {
		// Arrange: create a WAL with at least one compressed segment
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.maxSegmentBytes(4096) // small to ensure rotation + gzip
				.build();

		// Write enough values to rotate segments and compress the first
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			Path valuesDir = tempDir.resolve("values");
			Files.createDirectories(valuesDir);
			try (ValueStore store = new ValueStore(valuesDir.toFile(), false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
					ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				var vf = SimpleValueFactory.getInstance();
				for (int i = 0; i < 1000; i++) {
					store.storeValue(vf.createLiteral("val-" + i));
				}
			}
		}

		// Pick one compressed segment
		Path compressed = locateFirstCompressed(walDir);
		assertThat(compressed).as("compressed WAL segment").isNotNull();

		// Corrupt the summary CRC inside the compressed segment while keeping per-frame CRCs valid
		corruptSummaryCrc32(compressed);

		// Act: scan with reader
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			ValueStoreWalReader.ScanResult scan = reader.scan();
			// Assert: the scan is marked incomplete due to summary CRC mismatch
			assertThat(scan.complete()).as("scan completeness should be false if summary CRC mismatches").isFalse();
		}
	}

	private static Path locateFirstCompressed(Path walDir) throws IOException {
		try (var stream = Files.list(walDir)) {
			return stream.filter(p -> SEGMENT_GZ.matcher(p.getFileName().toString()).matches())
					.findFirst()
					.orElse(null);
		}
	}

	private static void corruptSummaryCrc32(Path gz) throws IOException {
		// Decompress entire segment
		byte[] decompressed;
		try (GZIPInputStream gin = new GZIPInputStream(Files.newInputStream(gz))) {
			decompressed = gin.readAllBytes();
		}

		// Walk frames to find the summary frame and its start offset
		int pos = 0;
		int summaryOffset = -1;
		int lastId = 0;
		while (pos + 12 <= decompressed.length) { // need at least len + crc around data
			int length = getIntLE(decompressed, pos);
			pos += 4;
			if (pos + length + 4 > decompressed.length) {
				break; // truncated safeguard
			}
			byte[] json = new byte[length];
			System.arraycopy(decompressed, pos, json, 0, length);
			pos += length;
			// skip frame CRC32C
			pos += 4;

			// Parse JSON and detect summary frame
			try (JsonParser jp = new JsonFactory().createParser(json)) {
				if (jp.nextToken() != JsonToken.START_OBJECT) {
					continue;
				}
				String type = null;
				Integer lid = null;
				while (jp.nextToken() != JsonToken.END_OBJECT) {
					String field = jp.getCurrentName();
					jp.nextToken();
					if ("t".equals(field)) {
						type = jp.getValueAsString("");
					} else if ("lastId".equals(field)) {
						lid = jp.getValueAsInt(0);
					} else {
						jp.skipChildren();
					}
				}
				if ("S".equals(type)) {
					summaryOffset = pos - (length + 4 /* len */ + 4 /* crc */);
					lastId = lid == null ? 0 : lid.intValue();
					break;
				}
			}
		}

		if (summaryOffset < 0) {
			throw new IOException("No summary frame found in compressed WAL segment: " + gz);
		}

		// Original content without the summary frame
		byte[] originalWithoutSummary = new byte[summaryOffset];
		System.arraycopy(decompressed, 0, originalWithoutSummary, 0, summaryOffset);

		// Build replacement summary frame with deliberately wrong crc32 value
		byte[] newSummary = buildSummaryFrameWithCrc(lastId, 0L); // mismatch on purpose

		// Rebuild gz with intact content and corrupted summary
		try (GZIPOutputStream gout = new GZIPOutputStream(Files.newOutputStream(gz))) {
			gout.write(originalWithoutSummary);
			gout.write(newSummary);
			gout.finish();
		}
	}

	private static byte[] buildSummaryFrameWithCrc(int lastMintedId, long wrongCrc32) throws IOException {
		JsonFactory factory = new JsonFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
		try (JsonGenerator gen = factory.createGenerator(baos)) {
			gen.writeStartObject();
			gen.writeStringField("t", "S");
			gen.writeNumberField("lastId", lastMintedId);
			gen.writeNumberField("crc32", wrongCrc32 & 0xFFFFFFFFL);
			gen.writeEndObject();
		}
		baos.write('\n');
		byte[] json = baos.toByteArray();

		// Frame = lenLE + json + crc32cLE(json)
		ByteBuffer lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(json.length);
		CRC32C crc32c = new CRC32C();
		crc32c.update(json, 0, json.length);
		int crc = (int) crc32c.getValue();
		ByteBuffer crcBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(crc);
		lenBuf.flip();
		crcBuf.flip();
		byte[] framed = new byte[4 + json.length + 4];
		lenBuf.get(framed, 0, 4);
		System.arraycopy(json, 0, framed, 4, json.length);
		crcBuf.get(framed, 4 + json.length, 4);
		return framed;
	}

	private static int getIntLE(byte[] arr, int off) {
		return (arr[off] & 0xFF) | ((arr[off + 1] & 0xFF) << 8) | ((arr[off + 2] & 0xFF) << 16)
				| ((arr[off + 3] & 0xFF) << 24);
	}
}
