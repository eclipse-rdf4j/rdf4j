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
package org.eclipse.rdf4j.common.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies the EOF contract for NioFile#read(ByteBuffer,long): when the underlying channel is at EOF and no bytes were
 * read, the method must return -1 (EOF sentinel), not 0.
 */
public class NioFileEOFContractTest {

	@TempDir
	File tmp;

	@Test
	public void readReturnsMinusOneAtEofWhenNoBytesRead() throws Exception {
		Path p = tmp.toPath().resolve("empty.dat");
		Files.write(p, new byte[0]); // empty file

		try (NioFile nf = new NioFile(p.toFile())) {
			ByteBuffer buf = ByteBuffer.allocate(16);
			int n = nf.read(buf, 0);
			assertEquals(-1, n, "EOF sentinel -1 expected when no bytes were read");
		}
	}
}
