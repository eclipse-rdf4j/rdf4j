/** *****************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ****************************************************************************** */
package org.eclipse.rdf4j.rio.hdt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Bart Hanssens
 */
public class HDTLogArray64Test {
	private ByteArrayOutputStream bos;
	private HDTArray arrayOut;

	@Before
	public void setUp() throws Exception {
		bos = new ByteArrayOutputStream();
		arrayOut = HDTArrayFactory.write(bos, HDTArray.Type.LOG64);
	}

	@Test
	public void testNrBits6() {
		arrayOut.setMaxValue(37);
		assertEquals("Number of bits per entry does not match", 6, arrayOut.getNrBits());
	}

	@Test
	public void testNrBits11() {
		arrayOut.setMaxValue(1734);
		assertEquals("Number of bits per entry does not match", 11, arrayOut.getNrBits());
	}

	@Test
	public void testEncode133() {
		arrayOut.setMaxValue(133);
		arrayOut.size(2);
		arrayOut.set(0, 0);
		arrayOut.set(1, 133);

		byte[] expected = new byte[] {
				(byte) 0x01, (byte) 0x08, (byte) 0x82, (byte) 0x44, // header
				(byte) 0x00, (byte) 0x85, // entries
				(byte) 0xb6, (byte) 0x58, (byte) 0x66, (byte) 0x46 }; // crc

		try {
			arrayOut.write(bos);
		} catch (Exception ioe) {
			fail(ioe.getMessage());
		}

		assertArrayEquals(expected, bos.toByteArray());
	}

	@Test
	public void testEncode1734() {
		arrayOut.setMaxValue(1734);
		arrayOut.size(3);
		arrayOut.set(0, 0);
		arrayOut.set(1, 1364);
		arrayOut.set(2, 1734);

		byte[] expected = new byte[] {
				(byte) 0x01, (byte) 0x0b, (byte) 0x83, (byte) 0x7c, // header
				(byte) 0x00, (byte) 0xa0, (byte) 0xaa, (byte) 0xb1, (byte) 0x1, // entries
				(byte) 0x10, (byte) 0xb8, (byte) 0xee, (byte) 0x87 }; // crc

		try {
			arrayOut.write(bos);
		} catch (Exception ioe) {
			fail(ioe.getMessage());
		}

		assertArrayEquals(expected, bos.toByteArray());
	}

	@Test
	public void testDecode1734() {
		// HDT-IT does not clears the last (padding) bits in the last byte of entries
		// this means that same entries (for e.g. 3 entries x 11 bits = 33 bits) may have different encodings
		byte[] expected1 = new byte[] {
				(byte) 0x01, (byte) 0x0b, (byte) 0x83, (byte) 0x7c, // header
				(byte) 0x00, (byte) 0xa0, (byte) 0xaa, (byte) 0xb1, (byte) 0x51, // entries
				(byte) 0xc3, (byte) 0x62, (byte) 0xcb, (byte) 0xd6 }; // crc

		ByteArrayInputStream bis1 = new ByteArrayInputStream(expected1);
		try {
			HDTArray array = HDTArrayFactory.parse(bis1);
		} catch (Exception ioe) {
			fail(ioe.getMessage());
		}

		byte[] expected2 = new byte[] {
				(byte) 0x01, (byte) 0x0b, (byte) 0x83, (byte) 0x7c, // header
				(byte) 0x00, (byte) 0xa0, (byte) 0xaa, (byte) 0xb1, (byte) 0x1, // entries
				(byte) 0x10, (byte) 0xb8, (byte) 0xee, (byte) 0x87 }; // crc

		ByteArrayInputStream bis2 = new ByteArrayInputStream(expected2);
		try {
			HDTArray array = HDTArrayFactory.parse(bis2);
		} catch (Exception ioe) {
			fail(ioe.getMessage());
		}
	}
}
