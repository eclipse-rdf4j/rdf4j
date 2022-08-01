/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 *
 * @author Bart Hanssens
 */
public class CRC32Test {
	@Test
	public void testHello() {
		CRC32 crc = new CRC32();
		crc.update("Hello world".getBytes(StandardCharsets.US_ASCII), 0, "Hello world".length());
		assertEquals("CRC hello world not correct", 0x72B51F78, crc.getValue());
	}

	@Test
	public void testHelloPerByte() {
		CRC32 crc = new CRC32();
		for (byte b : "Hello world".getBytes(StandardCharsets.US_ASCII)) {
			crc.update(b);
		}
		assertEquals("CRC hello world not correct", 0x72B51F78, crc.getValue());
	}
}
