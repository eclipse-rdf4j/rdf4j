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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CheckedInputStream;

import org.eclipse.rdf4j.common.io.UncloseableInputStream;

/**
 * HDT DictionarySection Plain Front Coding.
 * 
 * This part starts with a byte indicating the type of the dictionary section, followed by the VByte-encoded number of
 * strings, the VByte-encoded buffer size and the VByte-encoded buffer length.
 *
 * Then the 8-bit CRC.
 * 
 * Followed by an array and one or more buffers, and the 32-bit CRC calculated over the index and the buffers.
 * 
 * Structure:
 * 
 * <pre>
 * +------+-----------+---------+-------+------+-------+--------+...+-------+
 * | type | nrstrings | bufsize | array | CRC8 | index | buffer |...| CRC32 | 
 * +------+-----------+---------+-------+------+-------+--------+...+-------+
 * </pre>
 * 
 * Each buffer starts with a full string, followed by a maximum of <code>bufsize</code> - 1 pair of a VByte-encoded
 * number of characters this string has in common with the _previous_ string, and the (different) suffix.
 * 
 * E.g. <code>abcdef 2 gh 3 ij</code> will result in <code>abcde, abgh, abgij</code>.
 * 
 * Buffer structure:
 * 
 * <pre>
 * +--------+--------+--------+...+--------+--------+
 * | string | common | suffix |...| common | suffix |
 * +--------+--------+--------+...+--------+--------+
 * </pre>
 * 
 * @author Bart Hanssens
 */
class HDTDictionarySectionPFC extends HDTDictionarySection {
	private ArrayList<byte[]> arr;
	private int nrstrings;
	private long buflen;
	private int bufsize;

	@Override
	protected int size() {
		return nrstrings;
	}

	@Override
	protected byte[] get(int i) {
		return arr.get(i - 1);
	}

	@Override
	protected void parse(InputStream is) throws IOException {
		CRC8 crc8 = new CRC8();
		crc8.update((byte) HDTDictionarySection.Type.FRONT.getValue());

		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, crc8)) {

			long val = VByte.decode(cis);
			if (nrstrings > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException("Maximum number of strings in dictionary exceeded: " + val);
			}
			nrstrings = (int) val;

			buflen = VByte.decode(cis);

			val = VByte.decode(cis);
			if (val > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException("Maximum number of bufsize in dictionary exceeded: " + val);
			}
			bufsize = (int) val;

			checkCRC(cis, is, 1);
		}

		HDTArray ha = HDTArrayFactory.parse(is);
		ha.parse(is);

		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, new CRC32())) {

			arr = new ArrayList<>(nrstrings);
			parseBlocks(cis, nrstrings, bufsize);

			checkCRC(cis, is, 4);
		}
	}

	/**
	 * Parse a buffer (though a better name would be a "block") of strings
	 * 
	 * @param is        input stream
	 * @param nrstrings total number of strings of all buffers
	 * @param bufsize   max number of strings in 1 buffer
	 * @throws IOException
	 */
	private void parseBlocks(InputStream is, int nrstrings, int bufsize) throws IOException {
		// minimum one block
		int blocks = (nrstrings + bufsize - 1) / bufsize;

		for (int i = 0; i < blocks; i++) {
			byte[] str = HDTPart.readToNull(is);
			arr.add(str);

			// read the remaining strings, with a maximum of bufsize at a time
			int remaining = nrstrings - (i * bufsize);
			for (int j = 1; j < bufsize && j < remaining; j++) {
				int common = (int) VByte.decode(is);
				byte[] suffix = HDTPart.readToNull(is);
				// copy the common part and add the suffix
				str = Arrays.copyOf(str, common + suffix.length);
				System.arraycopy(suffix, 0, str, common, suffix.length);
				arr.add(str);
			}
		}
	}
}
