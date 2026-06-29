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
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.BitSet;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.common.io.NioFile;

/**
 * List of allocated BTree nodes, persisted to a file on disk.
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
	private final NioFile nioFile;

	/**
	 * Bit set recording which nodes have been allocated, using node IDs as index.
	 */
	private BitSet allocatedNodes;

	/**
	 * Flag indicating whether the set of allocated nodes has changed and needs to be written to file.
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

		this.nioFile = new NioFile(allocNodesFile);
		this.btree = btree;
		this.forceSync = forceSync;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the allocated nodes file.
	 */
	public File getFile() {
		return nioFile.getFile();
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
		return nioFile.delete();
	}

	public synchronized void close(boolean syncChanges) throws IOException {
		if (syncChanges) {
			sync();
		}
		allocatedNodes = null;
		needsSync = false;
		nioFile.close();
	}

	/**
	 * Writes any changes that are cached in memory to disk.
	 *
	 * @throws IOException
	 */
	public synchronized void sync() throws IOException {
		if (needsSync) {
			// Trim bit set
			BitSet bitSet = allocatedNodes;
			int bitSetLength = allocatedNodes.length();
			if (bitSetLength < allocatedNodes.size()) {
				bitSet = allocatedNodes.get(0, bitSetLength);
			}

			byte[] data = ByteArrayUtil.toByteArray(bitSet);

			// Write bit set to file
			nioFile.truncate(HEADER_LENGTH + data.length);
			nioFile.writeBytes(MAGIC_NUMBER, 0);
			nioFile.writeByte(FILE_FORMAT_VERSION, MAGIC_NUMBER.length);
			nioFile.writeBytes(data, HEADER_LENGTH);

			if (forceSync) {
				nioFile.force(false);
			}

			needsSync = false;
		}
	}

	private void scheduleSync() throws IOException {
		if (needsSync == false) {
			nioFile.truncate(0);
			needsSync = true;
		}
	}

	/**
	 * Clears the allocated nodes list.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public synchronized void clear() throws IOException {
		if (allocatedNodes != null) {
			allocatedNodes.clear();
		} else {
			// bit set has not yet been initialized
			allocatedNodes = new BitSet();
		}

		scheduleSync();
	}

	public synchronized int allocateNode() throws IOException {
		initAllocatedNodes();

		int newNodeID = allocatedNodes.nextClearBit(1);
		allocatedNodes.set(newNodeID);

		scheduleSync();

		return newNodeID;
	}

	public synchronized void freeNode(int nodeID) throws IOException {
		initAllocatedNodes();
		allocatedNodes.clear(nodeID);
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

	private void initAllocatedNodes() throws IOException {
		if (allocatedNodes == null) {
			if (nioFile.size() > 0L) {
				loadAllocatedNodesInfo();
			} else {
				crawlAllocatedNodes();
			}
		}
	}

	private void loadAllocatedNodesInfo() throws IOException {
		byte[] data;

		if (nioFile.size() >= HEADER_LENGTH && Arrays.equals(MAGIC_NUMBER, nioFile.readBytes(0, MAGIC_NUMBER.length))) {
			byte version = nioFile.readByte(MAGIC_NUMBER.length);
			if (version > FILE_FORMAT_VERSION) {
				throw new IOException("Unable to read allocated nodes file; it uses a newer file format");
			} else if (version != FILE_FORMAT_VERSION) {
				throw new IOException("Unable to read allocated nodes file; invalid file format version: " + version);
			}

			data = nioFile.readBytes(HEADER_LENGTH, (int) (nioFile.size() - HEADER_LENGTH));
		} else {
			// assume header is missing (old file format)
			data = nioFile.readBytes(0, (int) nioFile.size());
			scheduleSync();
		}

		allocatedNodes = ByteArrayUtil.toBitSet(data);
	}

	private void crawlAllocatedNodes() throws IOException {
		allocatedNodes = new BitSet();

		Node rootNode = btree.readRootNode();
		if (rootNode != null) {
			crawlAllocatedNodes(rootNode);
		}

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
}
