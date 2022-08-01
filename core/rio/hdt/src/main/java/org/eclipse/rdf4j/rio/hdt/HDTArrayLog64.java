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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CheckedInputStream;

import org.eclipse.rdf4j.common.io.UncloseableInputStream;

/**
 * Log64
 *
 * It contains the data part of the {@link HDTArray}, followed by the 32-bit CRC calculated over this data.
 *
 * Data structure:
 *
 * <pre>
 * ...+---------+-------+
 *    | entries | CRC32 |
 * ...+---------+-------+
 * </pre>
 *
 * Entries are stored little-endian, with each entry using <code>nrbits</code> bits
 *
 * @author Bart Hanssens
 */
class HDTArrayLog64 extends HDTArray {
	private byte buffer[];

	@Override
	protected int getType() {
		return HDTArray.Type.LOG64.getValue();
	}

	@Override
	protected int get(int i) {
		// start byte of the value, and start bit in that start byte
		int bytePos = (i * nrbits) / 8;
		int bitPos = (i * nrbits) % 8;

		// value bits may be encoded across boundaries of bytes
		int tmplen = (bitPos + nrbits + 7) / 8;

		long val = 0L;
		// little-endian to big-endian
		for (int j = 0; j < tmplen; j++) {
			val |= (buffer[bytePos + j] & 0xFFL) << (j * 8);
		}

		val >>= bitPos;
		val &= 0xFFFFFFFFFFFFFFFFL >>> (64 - nrbits);

		return (int) val;
	}

	@Override
	protected void parse(InputStream is) throws IOException {
		super.parse(is);

		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, new CRC32())) {
			// read bytes, minimum 1
			long bytes = (nrbits * entries + 7) / 8;
			if (bytes > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException("Maximum number of bytes in array exceeded: " + bytes);
			}

			buffer = new byte[(int) bytes];
			cis.read(buffer);

			checkCRC(cis, is, 4);
		}
	}
}
