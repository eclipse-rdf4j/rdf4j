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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Test utility helpers for inspecting ValueStore WAL segments.
 */
public final class ValueStoreWalTestUtils {

	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	private ValueStoreWalTestUtils() {
	}

	public static int readSegmentSequence(Path segmentPath) throws IOException {
		boolean compressed = segmentPath.getFileName().toString().endsWith(".gz");
		try (InputStream raw = Files.newInputStream(segmentPath);
				InputStream in = compressed ? new GZIPInputStream(raw) : raw) {
			return readSegmentSequence(in);
		}
	}

	public static int readSegmentSequence(byte[] segmentContent) throws IOException {
		try (ByteArrayInputStream in = new ByteArrayInputStream(segmentContent)) {
			return readSegmentSequence(in);
		}
	}

	private static int readSegmentSequence(InputStream in) throws IOException {
		byte[] lenBytes = in.readNBytes(Integer.BYTES);
		if (lenBytes.length < Integer.BYTES) {
			return 0;
		}
		ByteBuffer lenBuf = ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN);
		int frameLen = lenBuf.getInt();
		if (frameLen <= 0) {
			return 0;
		}
		byte[] jsonBytes = in.readNBytes(frameLen);
		if (jsonBytes.length < frameLen) {
			return 0;
		}
		// Skip header CRC
		in.readNBytes(Integer.BYTES);
		try (JsonParser parser = JSON_FACTORY.createParser(jsonBytes)) {
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				if (parser.currentToken() == JsonToken.FIELD_NAME) {
					String field = parser.getCurrentName();
					parser.nextToken();
					if ("segment".equals(field)) {
						return parser.getIntValue();
					}
				}
			}
		}
		return 0;
	}
}
