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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the parseJson branch where the first token is not START_OBJECT, by writing a frame with a single newline as
 * JSON payload. The reader should ignore the frame and proceed without errors.
 */
class ValueStoreWalReaderParseJsonNoStartObjectTest {

	@TempDir
	Path tempDir;

	@Test
	void frameNotStartingWithStartObjectIsIgnored() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		Path seg = walDir.resolve("wal-1.v1");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Valid header frame (minimal '{}') with correct CRC
		byte[] hdr = new byte[] { '{', '}' };
		out.write(lenLE(hdr.length));
		out.write(hdr);
		out.write(intLE(crc32c(hdr)));
		// Non-object JSON: just a newline (0x0A)
		out.write(lenLE(1));
		out.write(new byte[] { '\n' });
		out.write(intLE(crc32c(new byte[] { '\n' })));
		Files.write(seg, out.toByteArray());

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid("s").build();
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
			ValueStoreWalReader.ScanResult res = reader.scan();
			assertThat(res.records()).isEmpty();
			assertThat(res.complete()).isTrue();
		}
	}

	private static byte[] lenLE(int v) {
		ByteBuffer b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v);
		b.flip();
		byte[] a = new byte[4];
		b.get(a);
		return a;
	}

	private static int crc32c(byte[] data) {
		java.util.zip.CRC32C c = new java.util.zip.CRC32C();
		c.update(data, 0, data.length);
		return (int) c.getValue();
	}

	private static byte[] intLE(int v) {
		return lenLE(v);
	}
}
