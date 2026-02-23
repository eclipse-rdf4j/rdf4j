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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Reads and queries an immutable SSTable from its binary representation. The entire SSTable byte[] is held in memory
 * (Phase 1c). The block index enables binary search to find the starting block for range scans.
 */
public class SSTable {

	private final byte[] raw;
	private final QuadIndex quadIndex;

	// Parsed from footer
	private final long blockIndexOffset;
	private final int blockIndexLength;
	private final long statsOffset;
	private final int statsLength;

	// Parsed from block index
	private final int blockCount;
	private final byte[][] blockFirstKeys;
	private final long[] blockOffsets;
	private final int[] blockLengths;

	// Parsed from stats
	private final byte[] minKey;
	private final byte[] maxKey;
	private final long entryCount;

	public SSTable(byte[] raw, QuadIndex quadIndex) {
		this.raw = raw;
		this.quadIndex = quadIndex;

		// Parse footer (last 32 bytes)
		ByteBuffer footer = ByteBuffer.wrap(raw, raw.length - SSTableWriter.FOOTER_SIZE, SSTableWriter.FOOTER_SIZE);
		int magic = footer.getInt();
		if (magic != SSTableWriter.MAGIC) {
			throw new IllegalArgumentException("Invalid SSTable magic: 0x" + Integer.toHexString(magic));
		}
		int version = footer.getInt();
		if (version != SSTableWriter.VERSION) {
			throw new IllegalArgumentException("Unsupported SSTable version: " + version);
		}
		this.blockIndexOffset = footer.getLong();
		this.blockIndexLength = footer.getInt();
		this.statsOffset = footer.getLong();
		this.statsLength = footer.getInt();

		// Parse block index
		ByteBuffer biBuffer = ByteBuffer.wrap(raw, (int) blockIndexOffset, blockIndexLength);
		this.blockCount = biBuffer.getInt();
		this.blockFirstKeys = new byte[blockCount][];
		this.blockOffsets = new long[blockCount];
		this.blockLengths = new int[blockCount];
		for (int i = 0; i < blockCount; i++) {
			int keyLen = (int) Varint.readUnsigned(biBuffer);
			blockFirstKeys[i] = new byte[keyLen];
			biBuffer.get(blockFirstKeys[i]);
			blockOffsets[i] = biBuffer.getLong();
			blockLengths[i] = biBuffer.getInt();
		}

		// Parse stats
		ByteBuffer statsBuffer = ByteBuffer.wrap(raw, (int) statsOffset, statsLength);
		int minKeyLen = (int) Varint.readUnsigned(statsBuffer);
		this.minKey = new byte[minKeyLen];
		statsBuffer.get(this.minKey);
		int maxKeyLen = (int) Varint.readUnsigned(statsBuffer);
		this.maxKey = new byte[maxKeyLen];
		statsBuffer.get(this.maxKey);
		this.entryCount = statsBuffer.getLong();
	}

	public byte[] getMinKey() {
		return minKey;
	}

	public byte[] getMaxKey() {
		return maxKey;
	}

	public long getEntryCount() {
		return entryCount;
	}

	/**
	 * Scans for matching quads, filtering by flag (explicit/inferred) and pattern. Same contract as
	 * {@link MemTable#scan(long, long, long, long, boolean)}.
	 */
	public Iterator<long[]> scan(long s, long p, long o, long c, boolean explicit) {
		byte expectedFlag = explicit ? MemTable.FLAG_EXPLICIT : MemTable.FLAG_INFERRED;
		byte[] scanMinKey = quadIndex.getMinKeyBytes(s, p, o, c);
		byte[] scanMaxKey = quadIndex.getMaxKeyBytes(s, p, o, c);
		int startBlock = findStartBlock(scanMinKey);

		return new ScanIterator(startBlock, scanMinKey, scanMaxKey, expectedFlag, s, p, o, c);
	}

	/**
	 * Returns a {@link RawEntrySource} over the given key range. Includes tombstones (no flag filtering). Used by
	 * {@link MergeIterator}.
	 */
	public RawEntrySource asRawSource(long s, long p, long o, long c) {
		byte[] scanMinKey = quadIndex.getMinKeyBytes(s, p, o, c);
		byte[] scanMaxKey = quadIndex.getMaxKeyBytes(s, p, o, c);
		int startBlock = findStartBlock(scanMinKey);

		return new RawSourceImpl(startBlock, scanMinKey, scanMaxKey);
	}

	/**
	 * Binary search to find the block that could contain the given key.
	 */
	private int findStartBlock(byte[] targetKey) {
		int lo = 0, hi = blockCount - 1;
		int result = 0;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int cmp = Arrays.compareUnsigned(blockFirstKeys[mid], targetKey);
			if (cmp <= 0) {
				result = mid;
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}
		return result;
	}

	/**
	 * Reads the next entry from the data region at the given position.
	 *
	 * @return the position after the entry, or -1 if we've exceeded the data region
	 */
	private int readEntry(int pos, byte[][] keyOut, byte[] flagOut) {
		if (pos >= blockIndexOffset) {
			return -1;
		}
		ByteBuffer bb = ByteBuffer.wrap(raw, pos, (int) blockIndexOffset - pos);
		int keyLen = (int) Varint.readUnsigned(bb);
		int newPos = pos + (bb.position() - 0) + keyLen + 1;
		// Recalculate from actual buffer position
		int absKeyStart = pos + (bb.position() - (int) 0);

		// Re-read properly
		bb = ByteBuffer.wrap(raw, pos, (int) blockIndexOffset - pos);
		keyLen = (int) Varint.readUnsigned(bb);
		byte[] key = new byte[keyLen];
		bb.get(key);
		byte flag = bb.get();

		keyOut[0] = key;
		flagOut[0] = flag;
		return pos + Varint.calcLengthUnsigned(keyLen) + keyLen + 1;
	}

	private class ScanIterator implements Iterator<long[]> {
		private int pos;
		private final byte[] scanMaxKey;
		private final byte expectedFlag;
		private final long patternS, patternP, patternO, patternC;
		private long[] next;
		private final byte[][] keyBuf = new byte[1][];
		private final byte[] flagBuf = new byte[1];

		ScanIterator(int startBlock, byte[] scanMinKey, byte[] scanMaxKey, byte expectedFlag,
				long s, long p, long o, long c) {
			this.pos = (int) blockOffsets[startBlock];
			this.scanMaxKey = scanMaxKey;
			this.expectedFlag = expectedFlag;
			this.patternS = s;
			this.patternP = p;
			this.patternO = o;
			this.patternC = c;

			// Skip entries before scanMinKey
			skipToMinKey(scanMinKey);
			advance();
		}

		private void skipToMinKey(byte[] scanMinKey) {
			while (pos < blockIndexOffset) {
				int savedPos = pos;
				int nextPos = readEntry(pos, keyBuf, flagBuf);
				if (nextPos < 0) {
					break;
				}
				if (Arrays.compareUnsigned(keyBuf[0], scanMinKey) >= 0) {
					pos = savedPos; // revert - this entry is in range
					return;
				}
				pos = nextPos;
			}
		}

		private void advance() {
			next = null;
			while (pos < blockIndexOffset) {
				int nextPos = readEntry(pos, keyBuf, flagBuf);
				if (nextPos < 0) {
					break;
				}
				pos = nextPos;

				byte[] key = keyBuf[0];
				byte flag = flagBuf[0];

				// Past max key?
				if (Arrays.compareUnsigned(key, scanMaxKey) > 0) {
					pos = (int) blockIndexOffset; // done
					return;
				}

				if (flag != expectedFlag) {
					continue;
				}

				long[] quad = new long[4];
				quadIndex.keyToQuad(key, quad);

				if ((patternS >= 0 && quad[QuadIndex.SUBJ_IDX] != patternS)
						|| (patternP >= 0 && quad[QuadIndex.PRED_IDX] != patternP)
						|| (patternO >= 0 && quad[QuadIndex.OBJ_IDX] != patternO)
						|| (patternC >= 0 && quad[QuadIndex.CONTEXT_IDX] != patternC)) {
					continue;
				}

				next = quad;
				return;
			}
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public long[] next() {
			if (next == null) {
				throw new NoSuchElementException();
			}
			long[] result = next;
			advance();
			return result;
		}
	}

	private class RawSourceImpl implements RawEntrySource {
		private int pos;
		private final byte[] scanMaxKey;
		private byte[] currentKey;
		private byte currentFlag;
		private boolean valid;
		private final byte[][] keyBuf = new byte[1][];
		private final byte[] flagBuf = new byte[1];

		RawSourceImpl(int startBlock, byte[] scanMinKey, byte[] scanMaxKey) {
			this.pos = (int) blockOffsets[startBlock];
			this.scanMaxKey = scanMaxKey;
			skipToMinKey(scanMinKey);
		}

		private void skipToMinKey(byte[] scanMinKey) {
			while (pos < blockIndexOffset) {
				int savedPos = pos;
				int nextPos = readEntry(pos, keyBuf, flagBuf);
				if (nextPos < 0) {
					valid = false;
					return;
				}
				if (Arrays.compareUnsigned(keyBuf[0], scanMinKey) >= 0) {
					currentKey = keyBuf[0];
					currentFlag = flagBuf[0];
					pos = nextPos;
					valid = Arrays.compareUnsigned(currentKey, scanMaxKey) <= 0;
					return;
				}
				pos = nextPos;
			}
			valid = false;
		}

		@Override
		public boolean hasNext() {
			return valid;
		}

		@Override
		public byte[] peekKey() {
			return currentKey;
		}

		@Override
		public byte peekFlag() {
			return currentFlag;
		}

		@Override
		public void advance() {
			if (pos >= blockIndexOffset) {
				valid = false;
				return;
			}
			int nextPos = readEntry(pos, keyBuf, flagBuf);
			if (nextPos < 0) {
				valid = false;
				return;
			}
			pos = nextPos;
			currentKey = keyBuf[0];
			currentFlag = flagBuf[0];
			if (Arrays.compareUnsigned(currentKey, scanMaxKey) > 0) {
				valid = false;
			}
		}
	}
}
