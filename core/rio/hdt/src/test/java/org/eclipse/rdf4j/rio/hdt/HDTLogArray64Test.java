/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.ByteArrayOutputStream;
import org.junit.Assert;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Bart.Hanssens
 */
public class HDTLogArray64Test {
	private ByteArrayOutputStream bos;
	private HDTArray array;

	@Before
	public void setUp() throws Exception {
		bos = new ByteArrayOutputStream();
		array = HDTArrayFactory.write(bos, HDTArray.Type.LOG64);
	}

	@Test
	public void testNrBits6() {
		array.setMaxValue(37);
		assertEquals("Number of bits per entry does not match", 6, array.getNrBits());
	}

	@Test
	public void testNrBits11() {
		array.setMaxValue(1734);
		assertEquals("Number of bits per entry does not match", 11, array.getNrBits());
	}

	@Test
	public void testEncode133() {
		array.setMaxValue(133);
		array.setSize(2);
		array.set(0, 0);
		array.set(1, 133);

		byte[] expected = new byte[] {
				(byte) 0x01, (byte) 0x08, (byte) 0x80, (byte) 0x4a, // header
				(byte) 0x00, (byte) 0x85, // entries
				(byte) 0xb6, (byte) 0x58, (byte) 0x66, (byte) 0x46 }; // crc

		try {
			array.write(bos);
		} catch (Exception ioe) {
			fail(ioe.getMessage());
		}

		assertArrayEquals(expected, bos.toByteArray());
	}

	@Test
	public void testEncode1734() {
		array.setMaxValue(1734);
		array.setSize(3);
		array.set(0, 0);
		array.set(1, 1364);
		array.set(2, 1734);

		byte[] expected = new byte[] {
				(byte) 0x01, (byte) 0x0b, (byte) 0x83, (byte) 0x7c, // header
				(byte) 0x20, (byte) 0xa0, (byte) 0xaa, (byte) 0xb1, (byte) 0x51, (byte) 0xc3, // entries
				(byte) 0x62, (byte) 0xcb, (byte) 0xd6, (byte) 0x22 }; // crc

		try {
			array.write(bos);
		} catch (Exception ioe) {
			fail(ioe.getMessage());
		}

		for (byte b : bos.toByteArray()) {
			System.err.print(Integer.toHexString(b & 0xFF));
			System.err.print(" ");
		}
		
		assertArrayEquals(expected, bos.toByteArray());
	}
}
