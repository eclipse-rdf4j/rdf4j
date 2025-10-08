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
package org.eclipse.rdf4j.sail.nativerdf.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CorruptUnknownValue#getLabel()} recovery behavior.
 */
public class CorruptUnknownValueTest {

	private static CorruptUnknownValue valueWithData(byte[] data) {
		return new CorruptUnknownValue(null, 456, data);
	}

	@Test
	public void recoversLongestValidUtf8Substring() {
		byte[] invalid1 = new byte[] { (byte) 0xC3, (byte) 0x28 }; // invalid UTF-8
		byte[] validLong = "validlong".getBytes(StandardCharsets.UTF_8);
		byte[] invalid2 = new byte[] { (byte) 0xC0, (byte) 0xAF }; // invalid UTF-8
		byte[] validShort = "abc".getBytes(StandardCharsets.UTF_8);

		byte[] data = new byte[invalid1.length + validLong.length + invalid2.length + validShort.length];
		int pos = 0;
		System.arraycopy(invalid1, 0, data, pos, invalid1.length);
		pos += invalid1.length;
		System.arraycopy(validLong, 0, data, pos, validLong.length);
		pos += validLong.length;
		System.arraycopy(invalid2, 0, data, pos, invalid2.length);
		pos += invalid2.length;
		System.arraycopy(validShort, 0, data, pos, validShort.length);

		CorruptUnknownValue v = valueWithData(data);
		String label = v.getLabel();

		assertTrue(label.startsWith("CorruptUnknownValue with ID 456 with possible data: "));
		assertTrue(label.contains("validlong"), "Should recover the core decodable segment");
	}

	@Test
	public void fallsBackToHexWhenNoDecodableSubstring() {
		byte[] data = new byte[] { (byte) 0x80, (byte) 0x81, (byte) 0xFE, (byte) 0xFF };
		CorruptUnknownValue v = valueWithData(data);

		String label = v.getLabel();
		String expectedHex = Hex.encodeHexString(data);

		assertTrue(label.startsWith("CorruptUnknownValue with ID 456 with possible data: "));
		assertTrue(label.contains(expectedHex), "Should fall back to hex encoding when undecodable");
	}

	@Test
	public void stopsParsingAtTripleZeroSentinel() {
		byte[] valid = "xyz".getBytes(StandardCharsets.UTF_8);
		byte[] sentinel = new byte[] { 0, 0, 0 };
		byte[] tail = "end".getBytes(StandardCharsets.UTF_8);

		byte[] data = new byte[valid.length + sentinel.length + tail.length];
		int pos = 0;
		System.arraycopy(valid, 0, data, pos, valid.length);
		pos += valid.length;
		System.arraycopy(sentinel, 0, data, pos, sentinel.length);
		pos += sentinel.length;
		System.arraycopy(tail, 0, data, pos, tail.length);

		CorruptUnknownValue v = valueWithData(data);
		String label = v.getLabel();

		assertTrue(label.startsWith("CorruptUnknownValue with ID 456 with possible data: "));
		assertTrue(label.contains("xyz"), "Should use data before sentinel");
		assertTrue(!label.contains("end"), "Should not parse past sentinel");
	}
}
