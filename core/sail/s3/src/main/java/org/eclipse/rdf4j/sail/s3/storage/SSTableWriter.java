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
package org.eclipse.rdf4j.sail.s3.storage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serializes a frozen {@link MemTable} into SSTable binary format.
 *
 * <h3>Format</h3>
 *
 * <pre>
 * [DATA BLOCKS]
 *   Per entry: [key_length varint][key bytes][flag 1 byte]
 *   Block boundary when cumulative size exceeds blockSize
 *
 * [BLOCK INDEX]
 *   [block_count: 4-byte int BE]
 *   Per block: [first_key_length varint][first_key bytes][offset: 8-byte long BE][length: 4-byte int BE]
 *
 * [STATS]
 *   [min_key_length varint][min_key bytes]
 *   [max_key_length varint][max_key bytes]
 *   [entry_count: 8-byte long BE]
 *
 * [FOOTER: 32 bytes]
 *   [magic: 4 bytes = 0x53535431 "SST1"]
 *   [version: 4 bytes = 1]
 *   [block_index_offset: 8-byte long BE]
 *   [block_index_length: 4-byte int BE]
 *   [stats_offset: 8-byte long BE]
 *   [stats_length: 4-byte int BE]
 * </pre>
 */
public class SSTableWriter {

	static final int MAGIC = 0x53535431; // "SST1"
	static final int VERSION = 1;
	static final int FOOTER_SIZE = 32;
	static final int DEFAULT_BLOCK_SIZE = 4 * 1024 * 1024; // 4 MiB

	public static byte[] write(MemTable memTable) {
		return write(memTable, DEFAULT_BLOCK_SIZE);
	}

	public static byte[] write(MemTable memTable, int blockSize) {
		try {
			Map<byte[], byte[]> data = memTable.getData();
			if (data.isEmpty()) {
				throw new IllegalArgumentException("Cannot write empty MemTable to SSTable");
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(baos);

			// Track block boundaries
			List<BlockInfo> blocks = new ArrayList<>();
			byte[] firstKeyInBlock = null;
			long blockStartOffset = 0;
			long currentBlockSize = 0;

			byte[] minKey = null;
			byte[] maxKey = null;
			long entryCount = 0;

			for (Map.Entry<byte[], byte[]> entry : data.entrySet()) {
				byte[] key = entry.getKey();
				byte flag = entry.getValue()[0];

				if (minKey == null) {
					minKey = key;
				}
				maxKey = key;
				entryCount++;

				// Start a new block if needed
				if (firstKeyInBlock == null) {
					firstKeyInBlock = key;
					blockStartOffset = baos.size();
					currentBlockSize = 0;
				}

				// Write entry: [key_length varint][key bytes][flag 1 byte]
				writeVarint(out, key.length);
				out.write(key);
				out.write(flag);
				currentBlockSize += varintLength(key.length) + key.length + 1;

				// Check block boundary
				if (currentBlockSize >= blockSize) {
					long blockEnd = baos.size();
					blocks.add(new BlockInfo(firstKeyInBlock, blockStartOffset, (int) (blockEnd - blockStartOffset)));
					firstKeyInBlock = null;
					currentBlockSize = 0;
				}
			}

			// Finalize last block
			if (firstKeyInBlock != null) {
				long blockEnd = baos.size();
				blocks.add(new BlockInfo(firstKeyInBlock, blockStartOffset, (int) (blockEnd - blockStartOffset)));
			}

			// Write block index
			long blockIndexOffset = baos.size();
			out.writeInt(blocks.size());
			for (BlockInfo block : blocks) {
				writeVarint(out, block.firstKey.length);
				out.write(block.firstKey);
				out.writeLong(block.offset);
				out.writeInt(block.length);
			}
			int blockIndexLength = (int) (baos.size() - blockIndexOffset);

			// Write stats
			long statsOffset = baos.size();
			writeVarint(out, minKey.length);
			out.write(minKey);
			writeVarint(out, maxKey.length);
			out.write(maxKey);
			out.writeLong(entryCount);
			int statsLength = (int) (baos.size() - statsOffset);

			// Write footer (32 bytes)
			out.writeInt(MAGIC);
			out.writeInt(VERSION);
			out.writeLong(blockIndexOffset);
			out.writeInt(blockIndexLength);
			out.writeLong(statsOffset);
			out.writeInt(statsLength);

			out.flush();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void writeVarint(DataOutputStream out, int value) throws IOException {
		// Simple varint for lengths (always non-negative int)
		ByteBuffer bb = ByteBuffer.allocate(5);
		Varint.writeUnsigned(bb, value);
		out.write(bb.array(), 0, bb.position());
	}

	private static int varintLength(int value) {
		return Varint.calcLengthUnsigned(value);
	}

	private static class BlockInfo {
		final byte[] firstKey;
		final long offset;
		final int length;

		BlockInfo(byte[] firstKey, long offset, int length) {
			this.firstKey = firstKey;
			this.offset = offset;
			this.length = length;
		}
	}
}
