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
 * HDT Array
 * <p>
 * This part starts with a byte indicating the type of the array, followed by a byte containing the number of bits used
 * to encode an entry in the array, and the VByte-encoded number of entries.
 * <p>
 * Then the 8-bit CRC, followed by the array data itself.
 * <p>
 * Structure:
 *
 * <pre>
 * +------+--------+---------+------+------...
 * | type | nrbits | entries | CRC8 | data
 * +------+--------+---------+------+------...
 * </pre>
 *
 * @author Bart Hanssens
 */
abstract class HDTArray extends HDTPart {
	enum Type {
		LOG64(1),
		UINT32(2),
		UINT64(3);

		private final int value;

		/**
		 * Get value associated with this type
		 *
		 * @return value 1,2 or 3
		 */
		int getValue() {
			return value;
		}

		Type(int value) {
			this.value = value;
		}
	}

	int nrbits;
	int entries;

	/**
	 * Get the type of the array
	 *
	 * @return byte
	 */
	abstract int getType();

	/**
	 * Get number of bits used to encode an entry
	 *
	 * @return positive integer value
	 */
	int getNrBits() {
		return nrbits;
	}

	/**
	 * Get number of entries in this array
	 *
	 * @return positive integer value
	 */
	int size() {
		return entries;
	}

	/**
	 * Get entry from this array
	 *
	 * @param i zero-based index
	 * @return entry
	 */
	abstract int get(int i);

	@Override
	void parse(InputStream is) throws IOException {
		CRC8 crc8 = new CRC8();
		crc8.update(getType());

		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, crc8)) {

			nrbits = cis.read();
			long l = VByte.decode(cis);
			if (l > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException("Maximum number of bytes in array exceeded: " + l);
			}
			entries = (int) l;

			checkCRC(cis, is, 1);
		}
	}
}
