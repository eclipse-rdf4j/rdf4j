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
 * Entries are stored little-endian, with each entry using <code>nrbits</code> bits, which is the log2(max_value). E.g.
 * to store a maximum value of 1024 only 10 bits are needed, instead of storing a 16-bit <code>short</code>.
 * 
 * @author Bart Hanssens
 */
class HDTArrayLog64 extends HDTArray {
	private byte buffer[];

	@Override
	protected void setMaxValue(int maxval) {
		// ceil(log2(maxval))
		int i = 0;
		while (++i < 32 && (maxval >> i) > 0)
			;
		nrbits = i;
	}

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

	@Override
	protected void setSize(int entries) {
		buffer = new byte[(entries * nrbits + 7) / 8];
	}

	@Override
	protected void set(int i, int entry) {
		// start byte of the value, and start bit in that start byte
		int bytePos = (i * nrbits) / 8;
		int bitPos = (i * nrbits) % 8;

		// value bits may be encoded across boundaries of bytes
		int tmplen = (bitPos + nrbits + 7) / 8;

		long val = entry << bitPos;
		// big-endian to little-endian

		for (int j = 0; j < tmplen; j++) {
			int curval = buffer[bytePos + j] & 0xFF;
			buffer[bytePos + j] = (byte) ((val >> (j * 8)) & 0xFF | curval);
		}
	}

	@Override
	protected void write(OutputStream os) throws IOException {
		super.write(os);

		// don't close CheckedOutputStream, as it will close the underlying outputstream
		try (UncloseableOutputStream uos = new UncloseableOutputStream(os);
				CheckedOutputStream cos = new CheckedOutputStream(uos, new CRC32())) {

			cos.write(buffer);

			writeCRC(cos, os, 4);
		}
	}
}
