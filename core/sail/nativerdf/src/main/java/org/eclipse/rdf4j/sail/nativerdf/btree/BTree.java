/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
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
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.common.io.NioFile;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of an on-disk B-Tree using the <var>java.nio</var> classes that are available in JDK 1.4 and newer.
 * Documentation about B-Trees can be found on-line at the following URLs:
 * <ul>
 * <li>http://cis.stvincent.edu/swd/btree/btree.html</li>,
 * <li>http://bluerwhite.org/btree/</li>, and
 * <li>http://semaphorecorp.com/btp/algo.html</li>.
 * </ul>
 * The first reference was used to implement this class.
 * <p>
 *
 * @author Arjohn Kampman
 * @author Enrico Minack
 */
public class BTree implements Closeable {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Magic number "BTree File" to detect whether the file is actually a BTree file. The first three bytes of the file
	 * should be equal to this magic number. Note: this header has only been introduced in Sesame 2.3. The old "header"
	 * can be recognized using {@link BTree#OLD_MAGIC_NUMBER}.
	 */
	static final byte[] MAGIC_NUMBER = new byte[] { 'b', 't', 'f' };

	static final byte[] OLD_MAGIC_NUMBER = new byte[] { 0, 0, 0 };

	/**
	 * The file format version number, stored as the fourth byte in BTree files.
	 */
	static final byte FILE_FORMAT_VERSION = 1;

	/**
	 * The length of the header field.
	 */
	static final int HEADER_LENGTH = 16;

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final Logger logger = LoggerFactory.getLogger(BTree.class);

	/**
	 * The BTree file, accessed using java.nio-channels.
	 */
	final NioFile nioFile;

	/**
	 * Flag indicating whether file writes should be forced to disk using {@link FileChannel#force(boolean)}.
	 */
	private final boolean forceSync;

	/**
	 * Object used to determine whether one value is lower, equal or greater than another value. This determines the
	 * order of values in the BTree.
	 */
	final RecordComparator comparator;

	/**
	 * A read/write lock that is used to prevent changes to the BTree while readers are active in order to prevent
	 * concurrency issues.
	 */
	final ReentrantReadWriteLock btreeLock = new ReentrantReadWriteLock();

	private final ConcurrentNodeCache nodeCache = new ConcurrentNodeCache(id -> {
		Node node = new Node(id, this);
		try {
			node.read();
		} catch (IOException exc) {
			throw new SailException("Error reading B-tree node", exc);
		}
		return node;
	});

	/*
	 * Info about allocated and unused nodes in the file
	 */

	/**
	 * List of allocated nodes.
	 */
	private final AllocatedNodesList allocatedNodesList;

	/*
	 * BTree parameters
	 */

	/**
	 * The block size to use for calculating BTree node size. For optimal performance, the specified block size should
	 * be equal to the file system's block size.
	 */
	final int blockSize;

	/**
	 * The size of the values (byte arrays) in this BTree.
	 */
	final int valueSize;

	/**
	 * The size of a slot storing a node ID and a value. Value derived from valueSize.
	 */
	final int slotSize;

	/**
	 * The maximum number of outgoing branches for a node. Value derived from blockSize and slotSize.
	 */
	final int branchFactor;

	/**
	 * The minimum number of values for a node (except for the root). Value derived from branchFactor.
	 */
	final int minValueCount;

	/**
	 * The size of a node in bytes. Value derived from branchFactor and slotSize.
	 */
	final int nodeSize;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The ID of the root node, <var>0</var> to indicate that there is no root node (i.e. the BTree is empty).
	 */
	private volatile int rootNodeID;

	/**
	 * The depth of this BTree (the cache variable), < 0 indicating it is unknown, 0 for an empty BTree, 1 for a BTree
	 * with just a root node, and so on.
	 */
	private volatile int height = -1;

	/**
	 * Flag indicating whether this BTree has been closed.
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new BTree that uses an instance of <var>DefaultRecordComparator</var> to compare values.
	 *
	 * @param dataDir        The directory for the BTree data.
	 * @param filenamePrefix The prefix for all files used by this BTree.
	 * @param blockSize      The size (in bytes) of a file block for a single node. Ideally, the size specified is the
	 *                       size of a block in the used file system.
	 * @param valueSize      The size (in bytes) of the fixed-length values that are or will be stored in the B-Tree.
	 * @throws IOException In case the initialization of the B-Tree file failed.
	 * @see DefaultRecordComparator
	 */
	public BTree(File dataDir, String filenamePrefix, int blockSize, int valueSize) throws IOException {
		this(dataDir, filenamePrefix, blockSize, valueSize, false);
	}

	/**
	 * Creates a new BTree that uses an instance of <var>DefaultRecordComparator</var> to compare values.
	 *
	 * @param dataDir        The directory for the BTree data.
	 * @param filenamePrefix The prefix for all files used by this BTree.
	 * @param blockSize      The size (in bytes) of a file block for a single node. Ideally, the size specified is the
	 *                       size of a block in the used file system.
	 * @param valueSize      The size (in bytes) of the fixed-length values that are or will be stored in the B-Tree.
	 * @param forceSync      Flag indicating whether updates should be synced to disk forcefully by calling
	 *                       {@link FileChannel#force(boolean)}. This may have a severe impact on write performance.
	 * @throws IOException In case the initialization of the B-Tree file failed.
	 * @see DefaultRecordComparator
	 */
	public BTree(File dataDir, String filenamePrefix, int blockSize, int valueSize, boolean forceSync)
			throws IOException {
		this(dataDir, filenamePrefix, blockSize, valueSize, new DefaultRecordComparator(), forceSync);
	}

	/**
	 * Creates a new BTree that uses the supplied <var>RecordComparator</var> to compare the values that are or will be
	 * stored in the B-Tree.
	 *
	 * @param dataDir        The directory for the BTree data.
	 * @param filenamePrefix The prefix for all files used by this BTree.
	 * @param blockSize      The size (in bytes) of a file block for a single node. Ideally, the size specified is the
	 *                       size of a block in the used file system.
	 * @param valueSize      The size (in bytes) of the fixed-length values that are or will be stored in the B-Tree.
	 * @param comparator     The <var>RecordComparator</var> to use for determining whether one value is smaller, larger
	 *                       or equal to another.
	 * @throws IOException In case the initialization of the B-Tree file failed.
	 */
	public BTree(File dataDir, String filenamePrefix, int blockSize, int valueSize, RecordComparator comparator)
			throws IOException {
		this(dataDir, filenamePrefix, blockSize, valueSize, comparator, false);
	}

	/**
	 * Creates a new BTree that uses the supplied <var>RecordComparator</var> to compare the values that are or will be
	 * stored in the B-Tree.
	 *
	 * @param dataDir        The directory for the BTree data.
	 * @param filenamePrefix The prefix for all files used by this BTree.
	 * @param blockSize      The size (in bytes) of a file block for a single node. Ideally, the size specified is the
	 *                       size of a block in the used file system.
	 * @param valueSize      The size (in bytes) of the fixed-length values that are or will be stored in the B-Tree.
	 * @param comparator     The <var>RecordComparator</var> to use for determining whether one value is smaller, larger
	 *                       or equal to another.
	 * @param forceSync      Flag indicating whether updates should be synced to disk forcefully by calling
	 *                       {@link FileChannel#force(boolean)}. This may have a severe impact on write performance.
	 * @throws IOException In case the initialization of the B-Tree file failed.
	 */
	public BTree(File dataDir, String filenamePrefix, int blockSize, int valueSize, RecordComparator comparator,
			boolean forceSync) throws IOException {
		if (dataDir == null) {
			throw new IllegalArgumentException("dataDir must not be null");
		}
		if (filenamePrefix == null) {
			throw new IllegalArgumentException("filenamePrefix must not be null");
		}
		if (blockSize < HEADER_LENGTH) {
			throw new IllegalArgumentException("block size must be at least " + HEADER_LENGTH + " bytes");
		}
		if (valueSize <= 0) {
			throw new IllegalArgumentException("value size must be larger than 0");
		}
		if (blockSize < 3 * valueSize + 20) {
			throw new IllegalArgumentException("block size to small; must at least be able to store three values");
		}
		if (comparator == null) {
			throw new IllegalArgumentException("comparator muts not be null");
		}

		File file = new File(dataDir, filenamePrefix + ".dat");
		this.nioFile = new NioFile(file);
		this.comparator = comparator;
		this.forceSync = forceSync;

		File allocFile = new File(dataDir, filenamePrefix + ".alloc");
		allocatedNodesList = new AllocatedNodesList(allocFile, this, forceSync);

		if (nioFile.size() == 0L) {
			// Empty file, initialize it with the specified parameters
			this.blockSize = blockSize;
			this.valueSize = valueSize;
			this.rootNodeID = 0;
			this.height = 0;

			writeFileHeader();

			// sync();
		} else {
			// Read parameters from file
			ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH);
			nioFile.read(buf, 0L);

			buf.rewind();

			byte[] magicNumber = new byte[MAGIC_NUMBER.length];
			buf.get(magicNumber);
			byte version = buf.get();
			this.blockSize = buf.getInt();
			this.valueSize = buf.getInt();
			this.rootNodeID = buf.getInt();

			if (Arrays.equals(MAGIC_NUMBER, magicNumber)) {
				if (version > FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read BTree file " + file + "; it uses a newer file format");
				} else if (version != FILE_FORMAT_VERSION) {
					throw new IOException(
							"Unable to read BTree file " + file + "; invalid file format version: " + version);
				}
			} else if (Arrays.equals(OLD_MAGIC_NUMBER, magicNumber)) {
				if (version != 1) {
					throw new IOException(
							"Unable to read BTree file " + file + "; invalid file format version: " + version);
				}
				// Write new magic number to file
				logger.info("Updating file header for btree file '{}'", file.getAbsolutePath());
				writeFileHeader();
			} else {
				throw new IOException("File doesn't contain (compatible) BTree data: " + file);
			}

			// Verify that the value sizes match
			if (this.valueSize != valueSize) {
				throw new IOException("Specified value size (" + valueSize
						+ ") is different from what is stored on disk (" + this.valueSize + ") in " + file);
			}
		}

		// Calculate derived properties
		slotSize = 4 + this.valueSize;
		branchFactor = 1 + (this.blockSize - 8) / slotSize;
		// bf=30 --> mvc=14; bf=29 --> mvc=14
		minValueCount = (branchFactor - 1) / 2;
		nodeSize = 8 + (branchFactor - 1) * slotSize;

		// System.out.println("blockSize=" + this.blockSize);
		// System.out.println("valueSize=" + this.valueSize);
		// System.out.println("slotSize=" + this.slotSize);
		// System.out.println("branchFactor=" + this.branchFactor);
		// System.out.println("minimum value count=" + this.minValueCount);
		// System.out.println("nodeSize=" + this.nodeSize);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the file that this BTree operates on.
	 */
	public File getFile() {
		return nioFile.getFile();
	}

	/**
	 * Closes the BTree and then deletes its data files.
	 *
	 * @return <var>true</var> if the operation was successful.
	 */
	public boolean delete() throws IOException {
		if (closed.compareAndSet(false, true)) {
			close(false);

			boolean success = allocatedNodesList.delete();
			success &= nioFile.delete();
			return success;
		}
		return false;
	}

	/**
	 * Closes any opened files and release any resources used by this B-Tree. Any pending changes will be synchronized
	 * to disk before closing. Once the B-Tree has been closed, it can no longer be used.
	 */
	@Override
	public void close() throws IOException {
		if (closed.compareAndSet(false, true)) {
			close(true);
		}
	}

	/**
	 * Closes any opened files and release any resources used by this B-Tree. Any pending changes are optionally
	 * synchronized to disk before closing. Once the B-Tree has been closed, it can no longer be used.
	 *
	 * @param syncChanges Flag indicating whether pending changes should be synchronized to disk.
	 */
	private void close(boolean syncChanges) throws IOException {
		btreeLock.writeLock().lock();
		try {
			try {
				if (syncChanges) {
					sync();
				}
			} finally {
				try {
					nodeCache.clear();
				} finally {
					try {
						nioFile.close();
					} finally {
						allocatedNodesList.close(syncChanges);
					}
				}
			}
		} finally {
			btreeLock.writeLock().unlock();
		}
	}

	/**
	 * Writes any changes that are cached in memory to disk.
	 *
	 * @throws IOException
	 */
	public void sync() throws IOException {
		btreeLock.readLock().lock();
		try {
			// Write any changed nodes that still reside in the cache to disk
			nodeCache.flush();

			if (forceSync) {
				nioFile.force(false);
			}

			allocatedNodesList.sync();
		} finally {
			btreeLock.readLock().unlock();
		}
	}

	/**
	 * Gets the value that matches the specified key.
	 *
	 * @param key A value that is equal to the value that should be retrieved, at least as far as the RecordComparator
	 *            of this BTree is concerned.
	 * @return The value matching the key, or <var>null</var> if no such value could be found.
	 */
	public byte[] get(byte[] key) throws IOException {
		btreeLock.readLock().lock();
		try {
			Node node = readRootNode();

			if (node == null) {
				// Empty BTree
				return null;
			}

			while (true) {
				int valueIdx = node.search(key);

				if (valueIdx >= 0) {
					// Return matching value
					byte[] result = node.getValue(valueIdx);
					node.release();
					return result;
				} else if (!node.isLeaf()) {
					// Returned index references the first value that is larger than
					// the key, search the child node just left of it (==same index).
					Node childNode = node.getChildNode(-valueIdx - 1);
					node.release();
					node = childNode;
				} else {
					// value not found
					node.release();
					return null;
				}
			}
		} finally {
			btreeLock.readLock().unlock();
		}
	}

	/**
	 * Returns an iterator that iterates over all values in this B-Tree.
	 */
	public RecordIterator iterateAll() {
		return new RangeIterator(this, null, null, null, null);
	}

	/**
	 * Returns an iterator that iterates over all values between minValue and maxValue, inclusive.
	 */
	public RecordIterator iterateRange(byte[] minValue, byte[] maxValue) {
		return new RangeIterator(this, null, null, minValue, maxValue);
	}

	/**
	 * Returns an iterator that iterates over all values and returns the values that match the supplied searchKey after
	 * searchMask has been applied to the value.
	 */
	public RecordIterator iterateValues(byte[] searchKey, byte[] searchMask) {
		return new RangeIterator(this, searchKey, searchMask, null, null);
	}

	/**
	 * Returns an iterator that iterates over all values between minValue and maxValue (inclusive) and returns the
	 * values that match the supplied searchKey after searchMask has been applied to the value.
	 */
	public RecordIterator iterateRangedValues(byte[] searchKey, byte[] searchMask, byte[] minValue, byte[] maxValue) {
		return new RangeIterator(this, searchKey, searchMask, minValue, maxValue);
	}

	/**
	 * Returns an estimate for the number of values stored in this BTree.
	 */
	public long getValueCountEstimate() throws IOException {
		int allocatedNodesCount = allocatedNodesList.getNodeCount();

		// Assume fill factor of 50%
		return (long) (allocatedNodesCount * (branchFactor - 1) * 0.5);
	}

	/**
	 * Gives an estimate of the number of values between <var>minValue</var> and <var>maxValue</var>.
	 *
	 * @param minValue the lower bound of the range.
	 * @param maxValue the upper bound of the range,
	 * @return an estimate of the number of values in the specified range.
	 */
	public long getValueCountEstimate(byte[] minValue, byte[] maxValue) throws IOException {
		assert minValue != null : "minValue must not be null";
		assert maxValue != null : "maxValue must not be null";

		List<PathSegment> minValuePath, maxValuePath;

		btreeLock.readLock().lock();
		try {
			minValuePath = getPath(minValue);
			maxValuePath = getPath(maxValue);
		} finally {
			btreeLock.readLock().unlock();
		}

		return getValueCountEstimate(minValuePath, maxValuePath);
	}

	private List<PathSegment> getPath(byte[] key) throws IOException {
		assert key != null : "key must not be null";

		List<PathSegment> path = new ArrayList<>(height());

		Node currentNode = readRootNode();

		if (currentNode != null) {
			while (true) {
				int keyIndex = currentNode.search(key);

				path.add(new PathSegment(keyIndex, currentNode.getValueCount()));

				if (keyIndex >= 0 || currentNode.isLeaf()) {
					break;
				}

				Node childNode = currentNode.getChildNode(-keyIndex - 1);
				currentNode.release();
				currentNode = childNode;
			}

			currentNode.release();
		}

		return path;
	}

	private static class PathSegment {

		public final int valueIndex;

		public final int valueCount;

		public PathSegment(int valueIndex, int valueCount) {
			this.valueIndex = valueIndex;
			this.valueCount = valueCount;
		}

		public int getMinValueIndex() {
			if (valueIndex < 0) {
				return -valueIndex - 1;
			}
			return valueIndex;
		}

		public int getMaxValueIndex() {
			if (valueIndex < 0) {
				return -valueIndex - 2;
			}
			return valueIndex;
		}

		@Override
		public String toString() {
			return valueIndex + ":" + valueCount;
		}
	}

	private long getValueCountEstimate(List<PathSegment> minValuePath, List<PathSegment> maxValuePath)
			throws IOException {
		int commonListSize = Math.min(minValuePath.size(), maxValuePath.size());

		if (commonListSize == 0) {
			return 0;
		}

		PathSegment minNode = null, maxNode = null;

		// Find node depth where the paths start to diverge
		int splitIdx = 0;
		for (; splitIdx < commonListSize; splitIdx++) {
			minNode = minValuePath.get(splitIdx);
			maxNode = maxValuePath.get(splitIdx);

			if (minNode.valueIndex != maxNode.valueIndex) {
				break;
			}
		}

		if (splitIdx >= commonListSize) {
			// range does not span multiple values/child nodes
			return minNode.valueIndex >= 0 ? 1 : 0;
		}

		int minValueIndex = minNode.getMinValueIndex();
		int maxValueIndex = maxNode.getMaxValueIndex();

		// Estimate number of values in child nodes that fall entirely in the
		// range
		long valueCount = (maxValueIndex - minValueIndex) * getTreeSizeEstimate(splitIdx + 2);

		// Add number of values that are in the split node
		valueCount += (maxValueIndex - minValueIndex + 1);

		// Add values from left-most child node
		for (int i = splitIdx + 1; i < minValuePath.size(); i++) {
			PathSegment ps = minValuePath.get(i);
			minValueIndex = ps.getMinValueIndex();

			valueCount += (ps.valueCount - minValueIndex);
			valueCount += (ps.valueCount - minValueIndex) * getTreeSizeEstimate(i + 2);
		}

		// Add values from right-most child node
		for (int i = splitIdx + 1; i < maxValuePath.size(); i++) {
			PathSegment ps = maxValuePath.get(i);
			maxValueIndex = ps.getMaxValueIndex();

			valueCount += maxValueIndex + 1;
			valueCount += (maxValueIndex + 1) * getTreeSizeEstimate(i + 2);
		}

		return valueCount;
	}

	/**
	 * Estimates the number of values contained by a averagely filled node node at the specified <var>nodeDepth</var>
	 * (the root is at depth 1).
	 */
	private long getTreeSizeEstimate(int nodeDepth) throws IOException {
		// Assume fill factor of 50%
		int fanOut = this.branchFactor / 2;

		long valueCount = 0;

		for (int i = height() - nodeDepth; i >= 0; i--) {
			// valueCount += (long)Math.pow(fanOut, i);

			// equivalent but faster:
			valueCount += 1;
			valueCount *= fanOut;
		}

		return valueCount;
	}

	private int height() throws IOException {
		// if the depth is cached, return that value
		if (height >= 0) {
			return height;
		}

		int nodeDepth = 0;

		Node currentNode = readRootNode();

		if (currentNode != null) {
			nodeDepth = 1;

			while (!currentNode.isLeaf()) {
				Node childNode = currentNode.getChildNode(0);
				currentNode.release();
				currentNode = childNode;

				nodeDepth++;
			}

			currentNode.release();
		}

		height = nodeDepth;

		return height;
	}

	/**
	 * Inserts the supplied value into the B-Tree. In case an equal value is already present in the B-Tree this value is
	 * overwritten with the new value and the old value is returned by this method.
	 *
	 * @param value The value to insert into the B-Tree.
	 * @return The old value that was replaced, if any.
	 * @throws IOException If an I/O error occurred.
	 */
	public byte[] insert(byte[] value) throws IOException {
		btreeLock.writeLock().lock();
		try {
			Node rootNode = readRootNode();

			if (rootNode == null) {
				// Empty B-Tree, create a root node
				rootNode = createNewNode();
				rootNodeID = rootNode.getID();
				writeFileHeader();
				height = 1;
			}

			InsertResult insertResult = insertInTree(value, 0, rootNode);

			if (insertResult.overflowValue != null) {
				// Root node overflowed, create a new root node and insert overflow
				// value-nodeID pair in it
				Node newRootNode = createNewNode();
				newRootNode.setChildNodeID(0, rootNode.getID());
				newRootNode.insertValueNodeIDPair(0, insertResult.overflowValue, insertResult.overflowNodeID);

				rootNodeID = newRootNode.getID();
				writeFileHeader();
				newRootNode.release();

				// update the cached depth of this BTree
				if (height >= 0) {
					height++;
				}
			}

			rootNode.release();

			return insertResult.oldValue;
		} finally {
			btreeLock.writeLock().unlock();
		}
	}

	private InsertResult insertInTree(byte[] value, int nodeID, Node node) throws IOException {
		InsertResult insertResult;

		// Search value in node
		int valueIdx = node.search(value);

		if (valueIdx >= 0) {
			// Found an equal value, replace it
			insertResult = new InsertResult();
			insertResult.oldValue = node.getValue(valueIdx);

			// Do not replace the value if it's identical to the old
			// value to prevent possibly unnecessary disk writes
			if (!Arrays.equals(value, insertResult.oldValue)) {
				node.setValue(valueIdx, value);
			}
		} else {
			// valueIdx references the first value that is larger than the key
			valueIdx = -valueIdx - 1;

			if (node.isLeaf()) {
				// Leaf node, insert value here
				insertResult = insertInNode(value, nodeID, valueIdx, node);
			} else {
				// Not a leaf node, insert value in the child node just left of
				// the found value (==same index)
				Node childNode = node.getChildNode(valueIdx);
				insertResult = insertInTree(value, nodeID, childNode);
				childNode.release();

				if (insertResult.overflowValue != null) {
					// Child node overflowed, insert overflow in this node
					byte[] oldValue = insertResult.oldValue;
					insertResult = insertInNode(insertResult.overflowValue, insertResult.overflowNodeID, valueIdx,
							node);
					insertResult.oldValue = oldValue;
				}
			}
		}

		return insertResult;
	}

	private InsertResult insertInNode(byte[] value, int nodeID, int valueIdx, Node node) throws IOException {
		InsertResult insertResult = new InsertResult();

		if (node.isFull()) {
			// Leaf node is full and needs to be split
			Node newNode = createNewNode();
			insertResult.overflowValue = node.splitAndInsert(value, nodeID, valueIdx, newNode);
			insertResult.overflowNodeID = newNode.getID();
			newNode.release();
		} else {
			// Leaf node is not full, simply add the value to it
			node.insertValueNodeIDPair(valueIdx, value, nodeID);
		}

		return insertResult;
	}

	/**
	 * struct-like class used to represent the result of an insert operation.
	 */
	private static class InsertResult {

		/**
		 * The old value that has been replaced by the insertion of a new value.
		 */
		byte[] oldValue = null;

		/**
		 * The value that was removed from a child node due to overflow.
		 */
		byte[] overflowValue = null;

		/**
		 * The nodeID to the right of 'overflowValue' that was removed from a child node due to overflow.
		 */
		int overflowNodeID = 0;
	}

	/**
	 * Removes the value that matches the specified key from the B-Tree.
	 *
	 * @param key A key that matches the value that should be removed from the B-Tree.
	 * @return The value that was removed from the B-Tree, or <var>null</var> if no matching value was found.
	 * @throws IOException If an I/O error occurred.
	 */
	public byte[] remove(byte[] key) throws IOException {
		btreeLock.writeLock().lock();
		try {
			byte[] result = null;

			Node rootNode = readRootNode();

			if (rootNode != null) {
				result = removeFromTree(key, rootNode);

				if (rootNode.isEmpty()) {
					// Root node has become empty as a result of the removal
					if (rootNode.isLeaf()) {
						// Nothing's left
						rootNodeID = 0;
					} else {
						// Collapse B-Tree one level
						rootNodeID = rootNode.getChildNodeID(0);
						rootNode.setChildNodeID(0, 0);
					}

					// Write new root node ID to file header
					writeFileHeader();

					if (height >= 0) {
						height--;
					}
				}

				rootNode.release();
			}

			return result;
		} finally {
			btreeLock.writeLock().unlock();
		}
	}

	/**
	 * Removes the value that matches the specified key from the tree starting at the specified node and returns the
	 * removed value.
	 *
	 * @param key  A key that matches the value that should be removed from the B-Tree.
	 * @param node The root of the (sub) tree.
	 * @return The value that was removed from the B-Tree, or <var>null</var> if no matching value was found.
	 * @throws IOException If an I/O error occurred.
	 */
	private byte[] removeFromTree(byte[] key, Node node) throws IOException {
		byte[] value = null;

		// Search key
		int valueIdx = node.search(key);

		if (valueIdx >= 0) {
			// Found matching value in this node, remove it
			if (node.isLeaf()) {
				value = node.removeValueRight(valueIdx);
			} else {
				// Replace the matching value with the largest value from the left
				// child node
				value = node.getValue(valueIdx);

				Node leftChildNode = node.getChildNode(valueIdx);
				byte[] largestValue = removeLargestValueFromTree(leftChildNode);

				node.setValue(valueIdx, largestValue);

				balanceChildNode(node, leftChildNode, valueIdx);

				leftChildNode.release();
			}
		} else if (!node.isLeaf()) {
			// Recurse into left child node
			valueIdx = -valueIdx - 1;
			Node childNode = node.getChildNode(valueIdx);
			value = removeFromTree(key, childNode);

			balanceChildNode(node, childNode, valueIdx);

			childNode.release();
		}

		return value;
	}

	/**
	 * Removes the largest value from the tree starting at the specified node and returns the removed value.
	 *
	 * @param node The root of the (sub) tree.
	 * @return The value that was removed from the B-Tree.
	 * @throws IOException              If an I/O error occurred.
	 * @throws IllegalArgumentException If the supplied node is an empty leaf node
	 */
	private byte[] removeLargestValueFromTree(Node node) throws IOException {
		int nodeValueCount = node.getValueCount();

		if (node.isLeaf()) {
			if (node.isEmpty()) {
				throw new IllegalArgumentException("Trying to remove largest value from an empty node in " + getFile());
			}
			return node.removeValueRight(nodeValueCount - 1);
		} else {
			// Recurse into right-most child node
			Node childNode = node.getChildNode(nodeValueCount);
			byte[] value = removeLargestValueFromTree(childNode);
			balanceChildNode(node, childNode, nodeValueCount);
			childNode.release();
			return value;
		}
	}

	private void balanceChildNode(Node parentNode, Node childNode, int childIdx) throws IOException {
		if (childNode.getValueCount() < minValueCount) {
			// Child node contains too few values, try to borrow one from its right
			// sibling
			Node rightSibling = (childIdx < parentNode.getValueCount()) ? parentNode.getChildNode(childIdx + 1) : null;

			if (rightSibling != null && rightSibling.getValueCount() > minValueCount) {
				// Right sibling has enough values to give one up
				parentNode.rotateLeft(childIdx, childNode, rightSibling);
			} else {
				// Right sibling does not have enough values to give one up, try its
				// left sibling
				Node leftSibling = (childIdx > 0) ? parentNode.getChildNode(childIdx - 1) : null;

				if (leftSibling != null && leftSibling.getValueCount() > minValueCount) {
					// Left sibling has enough values to give one up
					parentNode.rotateRight(childIdx, leftSibling, childNode);
				} else {
					// Both siblings contain the minimum amount of values,
					// merge the child node with its left or right sibling
					if (leftSibling != null) {
						leftSibling.mergeWithRightSibling(parentNode.removeValueRight(childIdx - 1), childNode);
					} else {
						childNode.mergeWithRightSibling(parentNode.removeValueRight(childIdx), rightSibling);
					}
				}

				if (leftSibling != null) {
					leftSibling.release();
				}
			}

			if (rightSibling != null) {
				rightSibling.release();
			}
		}
	}

	/**
	 * Removes all values from the B-Tree.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public void clear() throws IOException {
		btreeLock.writeLock().lock();
		try {
			nodeCache.clear();
			nioFile.truncate(HEADER_LENGTH);

			if (rootNodeID != 0) {
				rootNodeID = 0;
				writeFileHeader();
			}

			allocatedNodesList.clear();
		} finally {
			btreeLock.writeLock().unlock();
		}
	}

	private Node createNewNode() throws IOException {
		int newNodeID = allocatedNodesList.allocateNode();

		Node node = new Node(newNodeID, this);
		node.use();

		nodeCache.put(node);

		return node;
	}

	Node readRootNode() throws IOException {
		if (rootNodeID > 0) {
			return readNode(rootNodeID);
		}
		return null;
	}

	Node readNode(int id) throws IOException {
		if (id <= 0) {
			throw new IllegalArgumentException("id must be larger than 0, is: " + id + " in " + getFile());
		}

		return nodeCache.readAndUse(id);
	}

	void releaseNode(Node node) throws IOException {
		// Note: this method is called by Node.release()
		// This method should not be called directly (to prevent concurrency issues)!!!

		if (node.isEmpty() && node.isLeaf() && nodeCache.discardEmptyUnused(node.getID())) {
			// allow the discarded node ID to be reused
			synchronized (allocatedNodesList) {
				allocatedNodesList.freeNode(node.getID());

				int maxNodeID = allocatedNodesList.getMaxNodeID();
				if (node.getID() > maxNodeID) {
					// Shrink file
					nioFile.truncate(nodeID2offset(maxNodeID) + nodeSize);
				}
			}
		} else {
			nodeCache.release(node, forceSync);
		}
	}

	private void writeFileHeader() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH);
		buf.put(MAGIC_NUMBER);
		buf.put(FILE_FORMAT_VERSION);
		buf.putInt(blockSize);
		buf.putInt(valueSize);
		buf.putInt(rootNodeID);

		buf.rewind();

		nioFile.write(buf, 0L);
	}

	long nodeID2offset(int id) {
		return (long) blockSize * id;
	}

	private int offset2nodeID(long offset) {
		return (int) (offset / blockSize);
	}

	public void print(PrintStream out) throws IOException {
		out.println("---contents of BTree file---");
		out.println("Stored parameters:");
		out.println("block size   = " + blockSize);
		out.println("value size   = " + valueSize);
		out.println("root node ID = " + rootNodeID);
		out.println();
		out.println("Derived parameters:");
		out.println("slot size       = " + slotSize);
		out.println("branch factor   = " + branchFactor);
		out.println("min value count = " + minValueCount);
		out.println("node size       = " + nodeSize);
		out.println();

		int nodeCount = 0;
		int valueCount = 0;

		ByteBuffer buf = ByteBuffer.allocate(nodeSize);
		for (long offset = blockSize; offset < nioFile.size(); offset += blockSize) {
			nioFile.read(buf, offset);
			buf.rewind();

			int nodeID = offset2nodeID(offset);
			int count = buf.getInt();
			nodeCount++;
			valueCount += count;
			out.print("node " + nodeID + ": ");
			out.print("count=" + count + " ");

			byte[] value = new byte[valueSize];

			for (int i = 0; i < count; i++) {
				// node ID
				out.print(buf.getInt());

				// value
				buf.get(value);
				out.print("[" + ByteArrayUtil.toHexString(value) + "]");
				// out.print("["+new String(value)+"]");
			}

			// last node ID
			out.println(buf.getInt());

			buf.clear();
		}
		out.println("#nodes          = " + nodeCount);
		out.println("#values         = " + valueCount);
		out.println("---end of BTree file---");
	}
}
