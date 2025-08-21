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
package org.eclipse.rdf4j.sail.lmdb.inlined;

public class Bytes {

	/**
	 * Packs a byte array into a long value.
	 *
	 * @param bytes the byte array to be packed into a long.
	 * @return the long value representing the packed bytes.
	 *
	 *         Note: Assumes the length of the byte array is within a reasonable range for packing into a long
	 *         (typically 8 bytes or less, depending on use case).
	 */
	static long packBytes(byte[] bytes) {
		long value = 0;
		for (int i = 0; i < bytes.length; i++) {
			value = value << 8;
			value = value | (bytes[i] & 0xFF);
		}
		return value;
	}

	/**
	 * Unpacks a long value into a byte array.
	 *
	 * @param value  the long value to be unpacked.
	 * @param length the number of bytes to unpack from the long value.
	 * @return the byte array representing the unpacked bytes.
	 */
	static byte[] unpackBytes(long value, int length) {
		byte[] bytes = new byte[length];
		for (int i = bytes.length - 1; i >= 0; i--) {
			bytes[i] = (byte) (value & 0xFF);
			value = value >>> 8;
		}
		return bytes;
	}
}