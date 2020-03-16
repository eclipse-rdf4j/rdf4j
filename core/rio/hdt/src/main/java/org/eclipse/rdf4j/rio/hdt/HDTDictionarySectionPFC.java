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
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import org.eclipse.rdf4j.common.io.UncloseableInputStream;
import org.eclipse.rdf4j.common.io.UncloseableOutputStream;

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
 * +------+--------------+--------------+-------+------+-------+--------+...+-------+
 * | type | totalStrings | stringsBlock | array | CRC8 | index | buffer |...| CRC32 | 
 * +------+--------------+--------------+-------+------+-------+--------+...+-------+
 * </pre>
 * 
 * Each buffer starts with a full string, followed by a maximum of <code>stringsBlock</code> - 1 pair of a VByte-encoded
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
	private byte[] readBuffer;
	private byte[][] writeBuffers;
	private int wbpos;

	private int totalStrings;
	private int stringsBlock;
	private HDTArray blockStarts;

	// keep most recently used blocks in memory as decoded values
	private final LinkedHashMap<Integer, ArrayList<byte[]>> cache = new LinkedHashMap<Integer, ArrayList<byte[]>>(100,
			1, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return size() > 99;
		}
	};

	/**
	 * Constructor
	 * 
	 * @param name
	 * @param pos
	 */
	protected HDTDictionarySectionPFC(String name, long pos) {
		super(name, pos);
	}

	@Override
	protected int size() {
		return totalStrings;
	}

	@Override
	protected void setSize(int size) {
		this.totalStrings = size;
		writeBuffers = new byte[(size - 1 + stringsBlock) / stringsBlock][];
	}

	protected void setBlockSize(int stringBlocks) {
		this.stringsBlock = stringBlocks;
	}

	@Override
	protected byte[] get(int i) throws IOException {
		// HDT index start counting from 1
		int idx = i - 1;

		// get the block this string belongs to, and maintain the cache of recently used blocks
		int block = idx / stringsBlock;

		ArrayList<byte[]> strings = cache.get(block);
		if (strings == null) {
			int blockStart = blockStarts.get(block);
			strings = decodeBlock(readBuffer, block, blockStart);
			cache.put(block, strings);
		}
		return strings.get(idx - (block * stringsBlock));
	}

	@Override
	protected void parse(InputStream is) throws IOException {
		CRC8 crc8 = new CRC8();
		crc8.update((byte) HDTDictionarySection.Type.FRONT.getValue());

		int buflen;

		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, crc8)) {

			long val = VByte.decode(cis);
			if (totalStrings > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException(getDebugPartStr() + " max number of strings exceeded: " + val);
			}
			totalStrings = (int) val;

			val = VByte.decode(cis);
			if (val > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException(getDebugPartStr() + " max buffer length exceeded: " + val);
			}
			buflen = (int) val;

			val = VByte.decode(cis);
			if (val > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException(
						getDebugPartStr() + " max number of strings per block exceeded: " + val);
			}
			stringsBlock = (int) val;

			checkCRC(cis, is, 1);
		}

		// keep track of starting positions of the blocks
		blockStarts = HDTArrayFactory.parse(is);
		blockStarts.parse(is);

		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, new CRC32())) {

			readBuffer = new byte[buflen];
			cis.read(readBuffer);
			checkCRC(cis, is, 4);
		}
	}

	@Override
	protected void set(Iterator<String> iter) {
		while (iter.hasNext()) {
			writeBuffers[wbpos++] = encodeBlock(iter);
		}
	}

	@Override
	protected void write(OutputStream os) throws IOException {
		CRC8 crc8 = new CRC8();
		crc8.update((byte) HDTDictionarySection.Type.FRONT.getValue());

		// calculate total buffer length
		int buflen = 0;
		for (int i = 0; i < writeBuffers.length; i++) {
			buflen += writeBuffers[i].length;
		}

		// don't close CheckedOutputStream, as it will close the underlying outputstream
		try (UncloseableOutputStream uos = new UncloseableOutputStream(os);
				CheckedOutputStream cos = new CheckedOutputStream(uos, crc8)) {
			VByte.encode(cos, totalStrings);
			VByte.encode(cos, buflen);
			VByte.encode(cos, stringsBlock);

			writeCRC(cos, os, 1);
		}

		// keep track of starting positions of the blocks
		blockStarts = HDTArrayFactory.write(os, HDTArray.Type.LOG64);
		blockStarts.setMaxValue(writeBuffers[writeBuffers.length - 1].length);
		blockStarts.setSize(writeBuffers.length + 1);
		blockStarts.set(0, 0);
		for (int i = 1; i <= writeBuffers.length; i++) {
			blockStarts.set(i, writeBuffers[i - 1].length);
		}
		blockStarts.write(os);

		// don't close CheckedOutputStream, as it will close the underlying outputstream
		try (UncloseableOutputStream uos = new UncloseableOutputStream(os);
				CheckedOutputStream cos = new CheckedOutputStream(uos, new CRC32())) {
			for (int i = 0; i < writeBuffers.length; i++) {
				cos.write(writeBuffers[i]);
			}
			writeCRC(cos, os, 4);
		}
	}

	/**
	 * Parse a single block
	 * 
	 * @param block block number
	 * @param start starting position
	 * @return list of decoded byte strings
	 * @throws IOException
	 */
	private ArrayList<byte[]> decodeBlock(byte[] buffer, int block, int start) throws IOException {
		ArrayList<byte[]> arr = new ArrayList<>(stringsBlock);

		// initial string
		int idx = start;
		int end = HDTPart.countToNull(buffer, idx);
		byte[] str = Arrays.copyOfRange(buffer, idx, end);
		arr.add(str);
		idx = end + 1;

		// read the remaining strings, with a maximum of stringsBlock
		int remaining = totalStrings - (block * stringsBlock);
		for (int j = 1; j < stringsBlock && j < remaining; j++) {
			int common = (int) VByte.decodeFrom(buffer, idx);
			idx += VByte.encodedLength(common);
			end = HDTPart.countToNull(buffer, idx);
			byte[] suffix = Arrays.copyOfRange(buffer, idx, end);

			// copy the common part and add the suffix
			str = Arrays.copyOf(str, common + suffix.length);
			System.arraycopy(suffix, 0, str, common, suffix.length);
			arr.add(str);

			idx = end + 1;
		}
		return arr;
	}

	/**
	 * Encode a single block, removing the input strings to conserve memory
	 * 
	 * @param iter
	 * @return encoded block
	 */
	private byte[] encodeBlock(Iterator<String> iter) {
		byte[][] tmp = new byte[stringsBlock][];
		int i = 0;

		byte[] base = iter.next().getBytes(StandardCharsets.UTF_8);
		iter.remove();
		// copy base string into first entry, with a trailing NULL
		tmp[i] = new byte[base.length + 1];
		System.arraycopy(base, 0, tmp[i], 0, base.length);

		byte[] prev = base;

		// encode a block, with a maximum of strings per block (minus the first base string)
		for (i = 1; iter.hasNext() && i < stringsBlock; i++) {
			byte[] str = iter.next().getBytes(StandardCharsets.UTF_8);
			iter.remove();

			// remove the common part, and only store the offset and the suffix and trailing NULL
			int common = Arrays.mismatch(prev, str);
			byte[] c = VByte.encode(common);
			tmp[i] = new byte[c.length + str.length - common + 1];
			System.arraycopy(c, 0, tmp[i], 0, c.length);
			System.arraycopy(str, common, tmp[i], c.length, str.length - common);

			prev = str;
		}

		if (i == 1) {
			return tmp[0];
		}

		// flatten 2-dimensional byte array to a single byte array
		int len = 0;
		for (int j = 0; j < i; j++) {
			len += tmp[j].length;
		}

		byte[] ret = new byte[len];
		for (int j = 0, idx = 0; j < i; j++) {
			System.arraycopy(tmp[j], 0, ret, idx, tmp[j].length);
			idx += tmp[j].length;
		}
		return ret;
	}
}
