/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import org.eclipse.rdf4j.common.io.UncloseableInputStream;
import org.eclipse.rdf4j.common.io.UncloseableOutputStream;

/**
 * Bitmap
 * 
 * This is used in combination with a {@link org.eclipse.rdf4j.rio.hdt.HDTArray HDTArray} to encode triple parts.
 *
 * This part starts with a byte indicating the type of the bitmap, followed by the VByte-encoded number of entries.
 *
 * Then the 8-bit CRC, followed by the bitmap (a series of 0 and 1's) and the 32-bit CRC.
 * 
 * Structure:
 * 
 * <pre>
 * +------+---------+------+---------+-------+
 * | type | entries | CRC8 | bits ...| CRC32 |
 * +------+---------+------+---------+-------+
 * </pre>
 * 
 * @author Bart Hanssens
 */
class HDTBitmap extends HDTPart {
	protected final static int BITMAP1 = 1;

	private int bits;
	private byte[] buffer = null;

	/**
	 * Set number of bits.
	 * 
	 * When first called, it will initialize an internal buffer. The size can still be decreased (but not increased)
	 * afterwards, though this will not free up memory.
	 * 
	 * @param number of bits
	 */
	protected void setSize(int bits) {
		this.bits = bits;
		if (buffer == null) {
			buffer = new byte[(bits + 7) / 8];
		}
	}

	/**
	 * Get number of entries in this bitmap
	 * 
	 * @return positive integer value
	 */
	protected int size() {
		return bits;
	}

	/**
	 * Get bit
	 * 
	 * @param i position
	 * @return 0 or 1
	 */
	protected int get(int i) {
		int bytePos = i / 8;
		int bitPos = i % 8;

		byte b = buffer[bytePos];
		return ((b & 0xFF) >> bitPos) & 1;
	}

	/**
	 * Set bit
	 * 
	 * @param i   position
	 * @param val 0 or 1
	 */
	protected void set(int i, int val) {
		int bytePos = i / 8;
		int bitPos = i % 8;

		buffer[bytePos] |= (val << bitPos) & 0xFF;
	}

	@Override
	protected void parse(InputStream is) throws IOException {
		long bytes = 0L;

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

	@Override
	protected void write(OutputStream os) throws IOException {
		long bytes = 0L;

		// don't close CheckedOutputStream, as it will close the underlying outputstream
		try (UncloseableOutputStream uos = new UncloseableOutputStream(os);
				CheckedOutputStream cos = new CheckedOutputStream(uos, new CRC8())) {

			cos.write(BITMAP1);
			VByte.encode(cos, bits);

			writeCRC(cos, os, 1);
		}

		// don't close CheckedOutputStream, as it will close the underlying outputstream
		try (UncloseableOutputStream uos = new UncloseableOutputStream(os);
				CheckedOutputStream cos = new CheckedOutputStream(uos, new CRC32())) {

			// setSize of the buffer might have been decreased
			cos.write(buffer, 0, (bits + 7) / 8);

			writeCRC(cos, os, 4);
		}
	}
}
