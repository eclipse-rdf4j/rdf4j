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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CorruptIRIOrBNode#getLocalName()} recovery behavior.
 */
public class CorruptIRIOrBNodeTest {

	private static CorruptIRIOrBNode nodeWithData(byte[] data) {
		return new CorruptIRIOrBNode(null, 123, data);
	}

	@Test
	public void recoversLongestValidUtf8Substring() {
		// Prepare a byte array with 5-byte header followed by: invalid, valid ASCII/UTF-8, invalid, short valid
		byte[] header = new byte[] { 0, 0, 0, 0, 0 };
		byte[] invalid1 = new byte[] { (byte) 0xC3, (byte) 0x28 }; // invalid UTF-8 sequence
		byte[] validLong = "validlong".getBytes(StandardCharsets.UTF_8);
		byte[] invalid2 = new byte[] { (byte) 0xC0, (byte) 0xAF }; // invalid UTF-8 sequence
		byte[] validShort = "abc".getBytes(StandardCharsets.UTF_8);

		byte[] data = new byte[header.length + invalid1.length + validLong.length + invalid2.length
				+ validShort.length];
		int pos = 0;
		System.arraycopy(header, 0, data, pos, header.length);
		pos += header.length;
		System.arraycopy(invalid1, 0, data, pos, invalid1.length);
		pos += invalid1.length;
		System.arraycopy(validLong, 0, data, pos, validLong.length);
		pos += validLong.length;
		System.arraycopy(invalid2, 0, data, pos, invalid2.length);
		pos += invalid2.length;
		System.arraycopy(validShort, 0, data, pos, validShort.length);

		CorruptIRIOrBNode node = nodeWithData(data);
		String localName = node.getLocalName();

		// Expect a valid decodable segment to be chosen containing the core text
		assertTrue(localName.startsWith("CORRUPT_"), "Should be prefixed with CORRUPT_");
		assertTrue(localName.contains("validlong"), "Should recover the core decodable segment");
	}

	@Test
	public void fallsBackToHexWhenNoDecodableSubstring() {
		// Prepare a byte array with 5-byte header followed by bytes with no ASCII/UTF-8 decodable sequences
		byte[] header = new byte[] { 0, 0, 0, 0, 0 };
		byte[] body = new byte[] { (byte) 0x80, (byte) 0x81, (byte) 0xFE, (byte) 0xFF };

		byte[] data = new byte[header.length + body.length];
		System.arraycopy(header, 0, data, 0, header.length);
		System.arraycopy(body, 0, data, header.length, body.length);

		CorruptIRIOrBNode node = nodeWithData(data);
		String expectedHex = Hex.encodeHexString(stripLeavingZeros(data));

		String localName = node.getLocalName();
		assertTrue(localName.startsWith("CORRUPT_"), "Should be prefixed with CORRUPT_");
		assertEquals("CORRUPT_ID_" + node.getInternalID() + "_HEX_" + expectedHex, localName);
	}

	private byte[] stripLeavingZeros(byte[] data) {
		int firstNonZero = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i] != 0) {
				firstNonZero = i;
				break;
			}
		}
		byte[] stripped = new byte[data.length - firstNonZero];
		System.arraycopy(data, firstNonZero, stripped, 0, stripped.length);
		return stripped;
	}

	@Test
	public void stopsParsingAtTripleZeroSentinel() {
		byte[] header = new byte[] { 0, 0, 0, 0, 0 };
		byte[] valid = "abc".getBytes(StandardCharsets.UTF_8);
		byte[] sentinel = new byte[] { 0, 0, 0 };
		byte[] tail = "tail".getBytes(StandardCharsets.UTF_8);

		byte[] data = new byte[header.length + valid.length + sentinel.length + tail.length];
		int pos = 0;
		System.arraycopy(header, 0, data, pos, header.length);
		pos += header.length;
		System.arraycopy(valid, 0, data, pos, valid.length);
		pos += valid.length;
		System.arraycopy(sentinel, 0, data, pos, sentinel.length);
		pos += sentinel.length;
		System.arraycopy(tail, 0, data, pos, tail.length);

		CorruptIRIOrBNode node = nodeWithData(data);
		String localName = node.getLocalName();

		assertTrue(localName.startsWith("CORRUPT_"));
		assertTrue(localName.contains("abc"), "Should recover text before sentinel");
		assertTrue(!localName.contains("tail"), "Should not parse past sentinel");
	}

	@Test
	public void ignoresLeadingZerosBeforeSentinel() {
		byte[] header = new byte[] { 0, 0, 0, 0, 0 };
		byte[] leadingZeros = new byte[] { 0, 0, 0, 0, 0, 0 };
		byte[] valid = "abc".getBytes(StandardCharsets.UTF_8);
		byte[] sentinel = new byte[] { 0, 0, 0 };
		byte[] tail = "tail".getBytes(StandardCharsets.UTF_8);

		byte[] data = new byte[header.length + leadingZeros.length + valid.length + sentinel.length + tail.length];
		int pos = 0;
		System.arraycopy(header, 0, data, pos, header.length);
		pos += header.length;
		System.arraycopy(leadingZeros, 0, data, pos, leadingZeros.length);
		pos += leadingZeros.length;
		System.arraycopy(valid, 0, data, pos, valid.length);
		pos += valid.length;
		System.arraycopy(sentinel, 0, data, pos, sentinel.length);
		pos += sentinel.length;
		System.arraycopy(tail, 0, data, pos, tail.length);

		CorruptIRIOrBNode node = nodeWithData(data);
		String localName = node.getLocalName();

		assertTrue(localName.startsWith("CORRUPT_"));
		assertTrue(localName.contains("abc"), "Should recover data after leading zeros");
		assertTrue(!localName.contains("tail"), "Should stop at sentinel after non-zero encountered");
	}
}
