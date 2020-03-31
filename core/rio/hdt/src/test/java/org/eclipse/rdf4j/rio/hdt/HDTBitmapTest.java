/** *****************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ****************************************************************************** */
package org.eclipse.rdf4j.rio.hdt;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Bart Hanssens
 */
public class HDTBitmapTest {
	private ByteArrayOutputStream bos;
	private HDTBitmap bitmapOut;

	@Before
	public void setUp() throws Exception {
		bos = new ByteArrayOutputStream();
		bitmapOut = new HDTBitmap();
	}

	@Test
	public void test12BitsSet() {
		byte[] expected = new byte[] {
				(byte) 0x01, (byte) 0x8c, (byte) 0xb8, // header
				(byte) 0xff, (byte) 0x0f, // entries
				(byte) 0x75, (byte) 0x6f, (byte) 0x91, (byte) 0x0c }; // crc

		bitmapOut.setSize(12);
		for (int i = 0; i < 12; i++) {
			bitmapOut.set(i, 1);
		}
		try {
			bitmapOut.write(bos);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertArrayEquals(expected, bos.toByteArray());
	}
}
