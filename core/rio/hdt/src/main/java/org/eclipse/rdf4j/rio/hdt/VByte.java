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

/**
 * Variable byte encoding for numbers.
 *
 * A variable number of bytes is used to encode (unsigned) numeric values, the first bit (MSB) of each byte indicates if
 * there are more bytes to read, the other 7 bits are used to encode the value.
 *
 * In this implementation, the MSB is set to <code>1</code> if this byte is the last one.
 *
 * E.g: <code>10000001</code> is value 1, <code>00000001 10000001</code> is 128 (decimal). Note that the value is stored
 * little-endian, so in this example <code>10000001 00000001</code>.
 *
 * @author Bart.Hanssens
 *
 * @see <a href="https://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html">Variable byte codes</a>
 */
public class VByte {
	/**
	 * Checks if the most significant bit is set. If this bit is zero, then the next byte must also be read to decode
	 * the number.
	 *
	 * @param b
	 * @return true if there is a next byte
	 */
	public static boolean hasNext(byte b) {
		return (b & 0xff) < 0x80;
	}

	/**
	 * Decode a series of encoded bytes, with a maximum of 8 bytes
	 *
	 * @param bytes byte array
	 * @param len   number of bytes to decode
	 * @return long value
	 */
	public static long decode(byte[] bytes, int len) {
		if (len > 8 || bytes.length < len) {
			throw new IllegalArgumentException("Buffer too long, or incorrect length");
		}
		long val = 0L;
		// little-endian to big-endian, e.g. HDT-It stores vbyte 0x81 0x00 as 0x00 0x81 (at least on x86)
		for (int i = len - 1; i >= 0; i--) {
			val <<= 7;
			val |= bytes[i] & 0x7F;
		}
		return val;
	}

	/**
	 * Decode a maximum of 8 bytes from the input stream.
	 *
	 * @param is input stream
	 * @return decode value
	 * @throws IOException
	 */
	public static long decode(InputStream is) throws IOException {
		byte[] buffer = new byte[8];

		int i = 0;
		do {
			buffer[i] = (byte) is.read();
		} while (i < buffer.length && hasNext(buffer[i++]));
		return decode(buffer, i);
	}

	/**
	 * Decode a maximum of 8 bytes from a byte array.
	 *
	 * @param b     byte array
	 * @param start starting position
	 * @return decode value
	 * @throws IOException
	 */
	public static long decodeFrom(byte[] b, int start) throws IOException {
		byte[] buffer = new byte[8];

		int i = 0;
		do {
			buffer[i] = b[start + i];
		} while (i < buffer.length && hasNext(buffer[i++]));
		return decode(buffer, i);
	}

	/**
	 * Calculate the number of bytes needed for encoding a value
	 *
	 * @param value numeric value
	 * @return number of bytes
	 */
	public static int encodedLength(long value) {
		if (value < 127) {
			return 1;
		}
		if (value < 16_384) {
			return 2;
		}
		if (value < 2_097_152) {
			return 3;
		}
		if (value < 268_435_456) {
			return 4;
		}
		return 5;
	}
}
