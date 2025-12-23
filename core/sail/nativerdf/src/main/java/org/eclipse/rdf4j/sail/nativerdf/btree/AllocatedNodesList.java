/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.BitSet;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;

/**
 * List of allocated BTree nodes, persisted to a file on disk.
 *
 * Incremental mmap version: node allocations/frees update the on-disk bitfield in-place, without rewriting the full
 * bitmap on every sync.
 *
 * @author Arjohn Kampman
 */
class AllocatedNodesList implements Closeable {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Magic number "Allocated Nodes File" to detect whether the file is actually an allocated nodes file. The first
	 * three bytes of the file should be equal to this magic number.
	 */
	private static final byte[] MAGIC_NUMBER = new byte[] { 'a', 'n', 'f' };

	/**
	 * The file format version number, stored as the fourth byte in allocated nodes files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	private static final int HEADER_LENGTH = MAGIC_NUMBER.length + 1;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The BTree associated with this allocated nodes list.
	 */
	private final BTree btree;

	/**
	 * The allocated nodes file.
	 */
	private final File allocNodesFile;

	/**
	 * File channel used for reading and writing the allocated nodes file.
	 */
	private final FileChannel channel;

	/**
	 * Memory-mapped buffer for the entire file: header + bitfield.
	 */
	private MappedByteBuffer mapped;

	/**
	 * Number of bits that can currently be represented by the on-disk bitfield. This is (mapped.capacity() -
	 * HEADER_LENGTH) * 8.
	 */
	private int bitCapacity = 0;

	/**
	 * Bit set recording which nodes have been allocated, using node IDs as index.
	 */
	private BitSet allocatedNodes;

	/**
	 * Flag indicating whether the set of allocated nodes has changed and needs to be synced (force()).
	 */
	private boolean needsSync = false;

	/**
	 * Flag indicating whether file writes should be forced to disk using {@link FileChannel#force(boolean)}.
	 */
	private final boolean forceSync;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new AllocatedNodelist for the specified BTree.
	 */
	public AllocatedNodesList(File allocNodesFile, BTree btree, boolean forceSync) throws IOException {
		if (allocNodesFile == null) {
			throw new IllegalArgumentException("allocNodesFile must not be null");
		}
		if (btree == null) {
			throw new IllegalArgumentException("btree muts not be null");
		}

		this.allocNodesFile = allocNodesFile;
		this.btree = btree;
		this.forceSync = forceSync;

		this.channel = FileChannel.open(
				allocNodesFile.toPath(),
				StandardOpenOption.READ,
				StandardOpenOption.WRITE,
				StandardOpenOption.CREATE);

		// We delay actual mapping until we know the desired bitset size
		// (after initAllocatedNodes / loadAllocatedNodesInfo / crawlAllocatedNodes).
		this.mapped = null;
		this.bitCapacity = 64;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the allocated nodes file.
	 */
	public File getFile() {
		return allocNodesFile;
	}

	@Override
	public synchronized void close() throws IOException {
		close(true);
	}

	/**
	 * Deletes the allocated nodes file.
	 *
	 * @return <var>true</var> if the file was deleted.
	 */
	public synchronized boolean delete() throws IOException {
		close(false);
		return allocNodesFile.delete();
	}

	public synchronized void close(boolean syncChanges) throws IOException {
		if (syncChanges) {
			sync();
		}
		allocatedNodes = null;
		needsSync = false;
		mapped = null; // let GC clean up mapping
		channel.close();
	}

	/**
	 * Writes any changes that are cached in memory to disk.
	 *
	 * For mmap, changes to individual bits are already reflected in the mapped region; sync() is mainly responsible for
	 * calling force() when requested.
	 */
	public synchronized void sync() throws IOException {
		if (!needsSync) {
			return;
		}

		if (mapped != null && forceSync) {
			mapped.force();
		}

		needsSync = false;
	}

	private void scheduleSync() {
		if (!needsSync) {
			needsSync = true;
		}
	}

	/**
	 * Clears the allocated nodes list.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public synchronized void clear() throws IOException {
		initAllocatedNodes();

		allocatedNodes.clear();

		// Clear on-disk bits as well (if mapped and any capacity).
		if (mapped != null && bitCapacity > 0) {
			int byteCount = (bitCapacity + 7) >>> 3;
			int start = HEADER_LENGTH;
			int end = start + byteCount;
			for (int pos = start; pos < end; pos++) {
				mapped.put(pos, (byte) 0);
			}
		}

		scheduleSync();
	}

	public synchronized int allocateNode() throws IOException {
		initAllocatedNodes();

		int newNodeID = allocatedNodes.nextClearBit(1);
		allocatedNodes.set(newNodeID);

		ensureCapacityForBit(newNodeID);
		setOnDiskBit(newNodeID, true);

		scheduleSync();

		return newNodeID;
	}

	public synchronized void freeNode(int nodeID) throws IOException {
		initAllocatedNodes();

		allocatedNodes.clear(nodeID);

		// It's possible we free a node above current bitCapacity if the file
		// was truncated, but in normal operation ensureCapacityForBit() will
		// have made sure we have space for this bit already.
		if (bitCapacity > 0 && nodeID < bitCapacity && mapped != null) {
			setOnDiskBit(nodeID, false);
		}

		scheduleSync();
	}

	/**
	 * Returns the highest allocated node ID.
	 */
	public synchronized int getMaxNodeID() throws IOException {
		initAllocatedNodes();
		return Math.max(0, allocatedNodes.length() - 1);
	}

	/**
	 * Returns the number of allocated nodes.
	 */
	public synchronized int getNodeCount() throws IOException {
		initAllocatedNodes();
		return allocatedNodes.cardinality();
	}

	/*--------------*
	 * Initialization *
	 *--------------*/

	private void initAllocatedNodes() throws IOException {
		if (allocatedNodes != null) {
			return;
		}

		long size = channel.size();
		if (size > 0L) {
			loadAllocatedNodesInfo();
		} else {
			crawlAllocatedNodes();
		}

		// At this point allocatedNodes is initialized; we can build an mmap
		// representing the current state so that future alloc/free calls
		// can update bits incrementally.
		remapFromAllocatedNodes();
	}

	/**
	 * Load allocated node info from disk (old or new format), into the in-memory BitSet.
	 */
	private void loadAllocatedNodesInfo() throws IOException {
		long size = channel.size();
		if (size <= 0L) {
			allocatedNodes = new BitSet();
			return;
		}

		// We read using standard I/O so we can interpret both headered and
		// headerless (old) formats.
		ByteBuffer buf = ByteBuffer.allocate((int) size);
		channel.position(0L);
		while (buf.hasRemaining()) {
			if (channel.read(buf) < 0) {
				break;
			}
		}
		byte[] fileBytes = buf.array();

		byte[] data;

		if (size >= HEADER_LENGTH && hasMagicHeader(fileBytes)) {
			byte version = fileBytes[MAGIC_NUMBER.length];
			if (version > FILE_FORMAT_VERSION) {
				throw new IOException("Unable to read allocated nodes file; it uses a newer file format");
			} else if (version != FILE_FORMAT_VERSION) {
				throw new IOException("Unable to read allocated nodes file; invalid file format version: " + version);
			}

			int dataLength = (int) (size - HEADER_LENGTH);
			data = new byte[dataLength];
			System.arraycopy(fileBytes, HEADER_LENGTH, data, 0, dataLength);
		} else {
			// assume header is missing (old file format)
			data = fileBytes;
			// triggers rewrite to new headered format on next sync
			scheduleSync();
		}

		allocatedNodes = ByteArrayUtil.toBitSet(data);
	}

	private boolean hasMagicHeader(byte[] fileBytes) {
		if (fileBytes.length < MAGIC_NUMBER.length) {
			return false;
		}
		for (int i = 0; i < MAGIC_NUMBER.length; i++) {
			if (fileBytes[i] != MAGIC_NUMBER[i]) {
				return false;
			}
		}
		return true;
	}

	private void crawlAllocatedNodes() throws IOException {
		allocatedNodes = new BitSet();

		Node rootNode = btree.readRootNode();
		if (rootNode != null) {
			crawlAllocatedNodes(rootNode);
		}

		// after crawling, we will write a fresh header+bitmap
		scheduleSync();
	}

	private void crawlAllocatedNodes(Node node) throws IOException {
		try {
			allocatedNodes.set(node.getID());

			if (!node.isLeaf()) {
				for (int i = 0; i < node.getValueCount() + 1; i++) {
					crawlAllocatedNodes(node.getChildNode(i));
				}
			}
		} finally {
			node.release();
		}
	}

	/*--------------*
	 * mmap helpers *
	 *--------------*/

	/**
	 * Ensure that the mapped file has enough room to represent the given bit index. If not, grow the file and rebuild
	 * the mapping from the current BitSet.
	 */
	private void ensureCapacityForBit(int bitIndex) throws IOException {
		// bits start at index 0; we need space for [0..bitIndex]
		int neededBits = bitIndex + 1;
		if (neededBits <= bitCapacity && mapped != null) {
			return;
		}

		// Expand capacity to at least neededBits, rounded up to a multiple of 64 bits
		int newBitCapacity = Math.max(neededBits, bitCapacity);
		newBitCapacity = (newBitCapacity + (4 * 8 * 1024) - 1) & ~((4 * 8 * 1024) - 1); // round up to 4KB boundary
		newBitCapacity -= HEADER_LENGTH * 8;

		assert newBitCapacity > 0;
		if (newBitCapacity < 0) {
			newBitCapacity = neededBits + 8; // at least 8 bits
		}

		// Serialize current BitSet into bytes according to the existing format
		byte[] data = ByteArrayUtil.toByteArray(allocatedNodes);
		int neededBytes = (newBitCapacity + 7) >>> 3;
		if (data.length < neededBytes) {
			data = Arrays.copyOf(data, neededBytes);
		}

		long newFileSize = HEADER_LENGTH + (long) data.length;

		// Resize file on disk
		long currentSize = channel.size();
		if (currentSize < newFileSize) {
			channel.position(newFileSize - 1);
			channel.write(ByteBuffer.wrap(new byte[] { 0 }));
		} else if (currentSize > newFileSize) {
			channel.truncate(newFileSize);
		}

		// Remap and write header + data
		mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, newFileSize);
		mapped.position(0);
		mapped.put(MAGIC_NUMBER);
		mapped.put(FILE_FORMAT_VERSION);
		mapped.put(data);

		bitCapacity = newBitCapacity;
	}

	/**
	 * Rebuild the mmap and on-disk representation from the current in-memory BitSet. Used at initialization / migration
	 * time.
	 */
	private void remapFromAllocatedNodes() throws IOException {
		// Determine minimal bit capacity needed for current BitSet
		int neededBits = Math.max(allocatedNodes.length(), 1); // at least 1 bit
		int newBitCapacity = (neededBits + (4 * 8 * 1024) - 1) & ~((4 * 8 * 1024) - 1); // round up to 4KB boundary
		newBitCapacity -= HEADER_LENGTH * 8;

		assert newBitCapacity > 0;
		if (newBitCapacity < 0) {
			newBitCapacity = neededBits + 8; // at least 8 bits
		}

		byte[] data = ByteArrayUtil.toByteArray(allocatedNodes);
		int neededBytes = (newBitCapacity + 7) >>> 3;
		if (data.length < neededBytes) {
			data = Arrays.copyOf(data, neededBytes);
		}

		long newFileSize = HEADER_LENGTH + (long) data.length;

		// Resize file
		channel.truncate(newFileSize);
		channel.position(newFileSize - 1);
		channel.write(ByteBuffer.wrap(new byte[] { 0 }));

		// Map and write header + data
		mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, newFileSize);
		mapped.position(0);
		mapped.put(MAGIC_NUMBER);
		mapped.put(FILE_FORMAT_VERSION);
		mapped.put(data);

		bitCapacity = newBitCapacity;
	}

	/**
	 * Set/clear a single bit in the mapped bitfield.
	 *
	 * Layout is identical to ByteArrayUtil.toByteArray(BitSet): bits are packed 8 per byte, with bit index i at byte (i
	 * >>> 3), bit (i & 7).
	 */
	private void setOnDiskBit(int bitIndex, boolean value) {
		if (mapped == null || bitIndex < 0) {
			return;
		}

		int byteIndex = bitIndex >>> 3;
		int bitInByte = bitIndex & 7;

		int fileOffset = HEADER_LENGTH + byteIndex;
		if (fileOffset >= mapped.capacity()) {
			// Should not happen if ensureCapacityForBit() is used correctly
			return;
		}

		byte b = mapped.get(fileOffset);
		int mask = 1 << bitInByte;

		if (value) {
			b = (byte) (b | mask);
		} else {
			b = (byte) (b & ~mask);
		}

		mapped.put(fileOffset, b);
	}
}
