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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * @author Bart.Hanssens
 */
public class VByteTest {
	@Test
	public void test127() {
		assertEquals(127, VByte.decode(new byte[] { (byte) 0xff }, 1), "127 not correctly decoded");
	}

	@Test
	public void test128() {
		assertEquals(128, VByte.decode(new byte[] { (byte) 0x00, (byte) 0x81 }, 2), "128 not correctly decoded");
	}

	@Test
	public void test128Input() {
		byte b[] = new byte[] { (byte) 0x00, (byte) 0x81 };

		try (ByteArrayInputStream bis = new ByteArrayInputStream(b)) {
			assertEquals(128, VByte.decode(bis), "128 not correctly decoded");
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}
}
