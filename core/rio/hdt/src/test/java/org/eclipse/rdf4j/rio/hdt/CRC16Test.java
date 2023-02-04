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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * @author Bart Hanssens
 */
public class CRC16Test {
	@Test
	public void testHello() {
		CRC16 crc = new CRC16();
		crc.update("Hello world".getBytes(StandardCharsets.US_ASCII), 0, "Hello world".length());
		assertEquals(0xF96A, crc.getValue(), "CRC hello world not correct");
	}

	@Test
	public void testHelloPerByte() {
		CRC16 crc = new CRC16();
		for (byte b : "Hello world".getBytes(StandardCharsets.US_ASCII)) {
			crc.update(b);
		}
		assertEquals(0xF96A, crc.getValue(), "CRC hello world not correct");
	}
}
