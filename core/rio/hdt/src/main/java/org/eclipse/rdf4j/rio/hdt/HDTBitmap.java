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
 *
 * @author Bart Hanssens
 */
class HDTBitmap extends HDTPart {
	protected final static int BITMAP1 = 1;

	private int bits;
	private byte[] buffer;

	/**
	 * Get bit
	 *
	 * @param i
	 * @return 0 or 1
	 */
	protected int get(int i) {
		int bytePos = i / 8;
		int bitPos = i % 8;

		byte b = buffer[bytePos];
		return ((b & 0xFF) >> bitPos) & 1;
	}

	/**
	 * Get number of entries in this bitmap
	 *
	 * @return positive integer value
	 */
	protected int size() {
		return bits;
	}

	@Override
	protected void parse(InputStream is) throws IOException {
		long bytes;

		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, new CRC8())) {

			int dtype = cis.read();
			if (dtype != BITMAP1) {
				throw new UnsupportedOperationException("Bitmap encoding " + Long.toHexString(dtype) +
						", but only bitmap v1 is supported");
			}

			long b = (int) VByte.decode(cis);
			if (b > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException("Maximum number of entries in bitmap exceeded: " + b);
			}
			bits = (int) b;
			bytes = (bits + 7) / 8;

			checkCRC(cis, is, 1);
		}

		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, new CRC32())) {

			buffer = new byte[(int) bytes];
			cis.read(buffer);

			checkCRC(cis, is, 4);
		}
	}
}
