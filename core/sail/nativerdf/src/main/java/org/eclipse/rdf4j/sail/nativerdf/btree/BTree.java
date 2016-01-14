/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.common.io.NioFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of an on-disk B-Tree using the <tt>java.nio</tt> classes that
 * are available in JDK 1.4 and newer. Documentation about B-Trees can be found
 * on-line at the following URLs:
 * <ul>
 * <li>http://cis.stvincent.edu/swd/btree/btree.html</li>,
 * <li>http://bluerwhite.org/btree/</li>, and
 * <li>http://semaphorecorp.com/btp/algo.html</li>.
 * </ul>
 * The first reference was used to implement this class.
 * <p>
 * TODO: clean up code
 * 
 * @author Arjohn Kampman
 * @author Enrico Minack
 */
public class BTree {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Magic number "BTree File" to detect whether the file is actually a BTree
	 * file. The first three bytes of the file should be equal to this magic
	 * number. Note: this header has only been introduced in Sesame 2.3. The old
	 * "header" can be recognized using {@link BTree#OLD_MAGIC_NUMBER}.
	 */
	private static final byte[] MAGIC_NUMBER = new byte[] { 'b', 't', 'f' };

	private static final byte[] OLD_MAGIC_NUMBER = new byte[] { 0, 0, 0 };

	/**
	 * The file format version number, stored as the fourth byte in BTree files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	/**
	 * The length of the header field.
	 */
	private static final int HEADER_LENGTH = 16;

	/**
	 * The size of the node cache. Note that this is not a hard limit. All nodes
	 * that are actively used are always cached. Also, a minimum of
	 * {@link NODE_CACHE_SIZE} nodes of unused nodes is kept in the cache.
	 */
	private static final int NODE_CACHE_SIZE = 10;

	/**
	 * The minimum number of most recently released nodes to keep in the cache.
	 */
	private static final int MIN_MRU_CACHE_SIZE = 4;

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * The BTree file, accessed using java.nio-channels.
	 */
	private final NioFile nioFile;

	/**
	 * Flag indicating whether file writes should be forced to disk using
	 * {@link FileChannel#force(boolean)}.
	 */
	private final boolean forceSync;

	/**
	 * Object used to determine whether one value is lower, equal or greater than
	 * another value. This determines the order of values in the BTree.
	 */
	private final RecordComparator comparator;

	/**
	 * A read/write lock that is used to prevent changes to the BTree while
	 * readers are active in order to prevent concurrency issues.
	 */
	private final ReentrantReadWriteLock btreeLock = new ReentrantReadWriteLock();

	/* 
	 * Node caching 
	 */

	/**
	 * Map containing cached nodes, indexed by their ID.
	 */
	private final Map<Integer, Node> nodeCache = new HashMap<Integer, Node>(NODE_CACHE_SIZE);

	/**
	 * Map of cached nodes that are no longer "in use", sorted from least
	 * recently used to most recently used. This collection is used to remove
	 * nodes from the cache when it is full. Note: needs to be synchronized
	 * through nodeCache (data strucures should prob be merged in a NodeCache
	 * class)
	 */
	private final Map<Integer, Node> mruNodes = new LinkedHashMap<Integer, Node>(NODE_CACHE_SIZE);

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
	 * The block size to use for calculating BTree node size. For optimal
	 * performance, the specified block size should be equal to the file system's
	 * block size.
	 */
	private final int blockSize;

	/**
	 * The size of the values (byte arrays) in this BTree.
	 */
	private final int valueSize;

	/**
	 * The size of a slot storing a node ID and a value. Value derived from
	 * valueSize.
	 */
	private final int slotSize;

	/**
	 * The maximum number of outgoing branches for a node. Value derived from
	 * blockSize and slotSize.
	 */
	private final int branchFactor;

	/**
	 * The minimum number of values for a node (except for the root). Value
	 * derived from branchFactor.
	 */
	private final int minValueCount;

	/**
	 * The size of a node in bytes. Value derived from branchFactor and slotSize.
	 */
	private final int nodeSize;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The ID of the root node, <tt>0</tt> to indicate that there is no root node
	 * (i.e. the BTree is empty).
	 */
	private volatile int rootNodeID;

	/**
	 * The depth of this BTree (the cache variable), < 0 indicating it is
	 * unknown, 0 for an empty BTree, 1 for a BTree with just a root node, and so
	 * on.
	 */
	private volatile int height = -1;

	/**
	 * Flag indicating whether this BTree has been closed.
	 */
	private volatile boolean closed = false;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new BTree that uses an instance of
	 * <tt>DefaultRecordComparator</tt> to compare values.
	 * 
	 * @param dataDir
	 *        The directory for the BTree data.
	 * @param filenamePrefix
	 *        The prefix for all files used by this BTree.
	 * @param blockSize
	 *        The size (in bytes) of a file block for a single node. Ideally, the
	 *        size specified is the size of a block in the used file system.
	 * @param valueSize
	 *        The size (in bytes) of the fixed-length values that are or will be
	 *        stored in the B-Tree.
	 * @throws IOException
	 *         In case the initialization of the B-Tree file failed.
	 * @see DefaultRecordComparator
	 */
	public BTree(File dataDir, String filenamePrefix, int blockSize, int valueSize)
		throws IOException
	{
		this(dataDir, filenamePrefix, blockSize, valueSize, false);
	}

	/**
	 * Creates a new BTree that uses an instance of
	 * <tt>DefaultRecordComparator</tt> to compare values.
	 * 
	 * @param dataDir
	 *        The directory for the BTree data.
	 * @param filenamePrefix
	 *        The prefix for all files used by this BTree.
	 * @param blockSize
	 *        The size (in bytes) of a file block for a single node. Ideally, the
	 *        size specified is the size of a block in the used file system.
	 * @param valueSize
	 *        The size (in bytes) of the fixed-length values that are or will be
	 *        stored in the B-Tree.
	 * @param forceSync
	 *        Flag indicating whether updates should be synced to disk forcefully
	 *        by calling {@link FileChannel#force(boolean)}. This may have a
	 *        severe impact on write performance.
	 * @throws IOException
	 *         In case the initialization of the B-Tree file failed.
	 * @see DefaultRecordComparator
	 */
	public BTree(File dataDir, String filenamePrefix, int blockSize, int valueSize, boolean forceSync)
		throws IOException
	{
		this(dataDir, filenamePrefix, blockSize, valueSize, new DefaultRecordComparator(), forceSync);
	}

	/**
	 * Creates a new BTree that uses the supplied <tt>RecordComparator</tt> to
	 * compare the values that are or will be stored in the B-Tree.
	 * 
	 * @param dataDir
	 *        The directory for the BTree data.
	 * @param filenamePrefix
	 *        The prefix for all files used by this BTree.
	 * @param blockSize
	 *        The size (in bytes) of a file block for a single node. Ideally, the
	 *        size specified is the size of a block in the used file system.
	 * @param valueSize
	 *        The size (in bytes) of the fixed-length values that are or will be
	 *        stored in the B-Tree.
	 * @param comparator
	 *        The <tt>RecordComparator</tt> to use for determining whether one
	 *        value is smaller, larger or equal to another.
	 * @throws IOException
	 *         In case the initialization of the B-Tree file failed.
	 */
	public BTree(File dataDir, String filenamePrefix, int blockSize, int valueSize, RecordComparator comparator)
		throws IOException
	{
		this(dataDir, filenamePrefix, blockSize, valueSize, comparator, false);
	}

	/**
	 * Creates a new BTree that uses the supplied <tt>RecordComparator</tt> to
	 * compare the values that are or will be stored in the B-Tree.
	 * 
	 * @param dataDir
	 *        The directory for the BTree data.
	 * @param filenamePrefix
	 *        The prefix for all files used by this BTree.
	 * @param blockSize
	 *        The size (in bytes) of a file block for a single node. Ideally, the
	 *        size specified is the size of a block in the used file system.
	 * @param valueSize
	 *        The size (in bytes) of the fixed-length values that are or will be
	 *        stored in the B-Tree.
	 * @param comparator
	 *        The <tt>RecordComparator</tt> to use for determining whether one
	 *        value is smaller, larger or equal to another.
	 * @param forceSync
	 *        Flag indicating whether updates should be synced to disk forcefully
	 *        by calling {@link FileChannel#force(boolean)}. This may have a
	 *        severe impact on write performance.
	 * @throws IOException
	 *         In case the initialization of the B-Tree file failed.
	 */
	public BTree(File dataDir, String filenamePrefix, int blockSize, int valueSize,
			RecordComparator comparator, boolean forceSync)
		throws IOException
	{
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
			throw new IllegalArgumentException(
					"block size to small; must at least be able to store three values");
		}
		if (comparator == null) {
			throw new IllegalArgumentException("comparator muts not be null");
		}

		File file = new File(dataDir, filenamePrefix + ".dat");
		this.nioFile = new NioFile(file);
		this.comparator = comparator;
		this.forceSync = forceSync;

		File allocFile = new File(dataDir, filenamePrefix + ".alloc");
		allocatedNodesList = new AllocatedNodesList(allocFile, this);

		if (nioFile.size() == 0L) {
			// Empty file, initialize it with the specified parameters
			this.blockSize = blockSize;
			this.valueSize = valueSize;
			this.rootNodeID = 0;
			this.height = 0;

			writeFileHeader();

			// sync();
		}
		else {
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
				}
				else if (version != FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read BTree file " + file + "; invalid file format version: "
							+ version);
				}
			}
			else if (Arrays.equals(OLD_MAGIC_NUMBER, magicNumber)) {
				if (version != 1) {
					throw new IOException("Unable to read BTree file " + file + "; invalid file format version: "
							+ version);
				}
				// Write new magic number to file
				logger.info("Updating file header for btree file '{}'", file.getAbsolutePath());
				writeFileHeader();
			}
			else {
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
	 * @return <tt>true</tt> if the operation was successful.
	 */
	public boolean delete()
		throws IOException
	{
		close(false);

		boolean success = allocatedNodesList.delete();
		success &= nioFile.delete();
		return success;
	}

	/**
	 * Closes any opened files and release any resources used by this B-Tree. Any
	 * pending changes will be synchronized to disk before closing. Once the
	 * B-Tree has been closed, it can no longer be used.
	 */
	public void close()
		throws IOException
	{
		close(true);
	}

	/**
	 * Closes any opened files and release any resources used by this B-Tree. Any
	 * pending changes are optionally synchronized to disk before closing. Once
	 * the B-Tree has been closed, it can no longer be used.
	 * 
	 * @param syncChanges
	 *        Flag indicating whether pending changes should be synchronized to
	 *        disk.
	 */
	private void close(boolean syncChanges)
		throws IOException
	{
		btreeLock.writeLock().lock();
		try {
			if (closed) {
				return;
			}

			if (syncChanges) {
				sync();
			}

			closed = true;

			synchronized (nodeCache) {
				nodeCache.clear();
				mruNodes.clear();
			}

			try {
				nioFile.close();
			}
			finally {
				allocatedNodesList.close(syncChanges);
			}
		}
		finally {
			btreeLock.writeLock().unlock();
		}
	}

	/**
	 * Writes any changes that are cached in memory to disk.
	 * 
	 * @throws IOException
	 */
	public void sync()
		throws IOException
	{
		btreeLock.readLock().lock();
		try {
			// Write any changed nodes that still reside in the cache to disk
			synchronized (nodeCache) {
				for (Node node : nodeCache.values()) {
					if (node.dataChanged()) {
						node.write();
					}
				}
			}

			if (forceSync) {
				nioFile.force(false);
			}

			allocatedNodesList.sync();
		}
		finally {
			btreeLock.readLock().unlock();
		}
	}

	/**
	 * Gets the value that matches the specified key.
	 * 
	 * @param key
	 *        A value that is equal to the value that should be retrieved, at
	 *        least as far as the RecordComparator of this BTree is concerned.
	 * @return The value matching the key, or <tt>null</tt> if no such value
	 *         could be found.
	 */
	public byte[] get(byte[] key)
		throws IOException
	{
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
				}
				else if (!node.isLeaf()) {
					// Returned index references the first value that is larger than
					// the key, search the child node just left of it (==same index).
					Node childNode = node.getChildNode(-valueIdx - 1);
					node.release();
					node = childNode;
				}
				else {
					// value not found
					node.release();
					return null;
				}
			}
		}
		finally {
			btreeLock.readLock().unlock();
		}
	}

	/**
	 * Returns an iterator that iterates over all values in this B-Tree.
	 */
	public RecordIterator iterateAll() {
		return new RangeIterator(null, null, null, null);
	}

	/**
	 * Returns an iterator that iterates over all values between minValue and
	 * maxValue, inclusive.
	 */
	public RecordIterator iterateRange(byte[] minValue, byte[] maxValue) {
		return new RangeIterator(null, null, minValue, maxValue);
	}

	/**
	 * Returns an iterator that iterates over all values and returns the values
	 * that match the supplied searchKey after searchMask has been applied to the
	 * value.
	 */
	public RecordIterator iterateValues(byte[] searchKey, byte[] searchMask) {
		return new RangeIterator(searchKey, searchMask, null, null);
	}

	/**
	 * Returns an iterator that iterates over all values between minValue and
	 * maxValue (inclusive) and returns the values that match the supplied
	 * searchKey after searchMask has been applied to the value.
	 */
	public RecordIterator iterateRangedValues(byte[] searchKey, byte[] searchMask, byte[] minValue,
			byte[] maxValue)
	{
		return new RangeIterator(searchKey, searchMask, minValue, maxValue);
	}

	/**
	 * Returns an estimate for the number of values stored in this BTree.
	 */
	public long getValueCountEstimate()
		throws IOException
	{
		int allocatedNodesCount = allocatedNodesList.getNodeCount();

		// Assume fill factor of 50%
		return (long)(allocatedNodesCount * (branchFactor - 1) * 0.5);
	}

	/**
	 * Gives an estimate of the number of values between <tt>minValue</tt> and
	 * <tt>maxValue</tt>.
	 * 
	 * @param minValue
	 *        the lower bound of the range.
	 * @param maxValue
	 *        the upper bound of the range,
	 * @return an estimate of the number of values in the specified range.
	 */
	public long getValueCountEstimate(byte[] minValue, byte[] maxValue)
		throws IOException
	{
		assert minValue != null : "minValue must not be null";
		assert maxValue != null : "maxValue must not be null";

		List<PathSegment> minValuePath, maxValuePath;

		btreeLock.readLock().lock();
		try {
			minValuePath = getPath(minValue);
			maxValuePath = getPath(maxValue);
		}
		finally {
			btreeLock.readLock().unlock();
		}

		return getValueCountEstimate(minValuePath, maxValuePath);
	}

	private List<PathSegment> getPath(byte[] key)
		throws IOException
	{
		assert key != null : "key must not be null";

		List<PathSegment> path = new ArrayList<PathSegment>(height());

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
		throws IOException
	{
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
	 * Estimates the number of values contained by a averagely filled node node
	 * at the specified <tt>nodeDepth</tt> (the root is at depth 1).
	 */
	private long getTreeSizeEstimate(int nodeDepth)
		throws IOException
	{
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

	private int height()
		throws IOException
	{
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
	 * Inserts the supplied value into the B-Tree. In case an equal value is
	 * already present in the B-Tree this value is overwritten with the new value
	 * and the old value is returned by this method.
	 * 
	 * @param value
	 *        The value to insert into the B-Tree.
	 * @return The old value that was replaced, if any.
	 * @throws IOException
	 *         If an I/O error occurred.
	 */
	public byte[] insert(byte[] value)
		throws IOException
	{
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
		}
		finally {
			btreeLock.writeLock().unlock();
		}
	}

	private InsertResult insertInTree(byte[] value, int nodeID, Node node)
		throws IOException
	{
		InsertResult insertResult = null;

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
		}
		else {
			// valueIdx references the first value that is larger than the key
			valueIdx = -valueIdx - 1;

			if (node.isLeaf()) {
				// Leaf node, insert value here
				insertResult = insertInNode(value, nodeID, valueIdx, node);
			}
			else {
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

	private InsertResult insertInNode(byte[] value, int nodeID, int valueIdx, Node node)
		throws IOException
	{
		InsertResult insertResult = new InsertResult();

		if (node.isFull()) {
			// Leaf node is full and needs to be split
			Node newNode = createNewNode();
			insertResult.overflowValue = node.splitAndInsert(value, nodeID, valueIdx, newNode);
			insertResult.overflowNodeID = newNode.getID();
			newNode.release();
		}
		else {
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
		 * The nodeID to the right of 'overflowValue' that was removed from a
		 * child node due to overflow.
		 */
		int overflowNodeID = 0;
	}

	/**
	 * Removes the value that matches the specified key from the B-Tree.
	 * 
	 * @param key
	 *        A key that matches the value that should be removed from the
	 *        B-Tree.
	 * @return The value that was removed from the B-Tree, or <tt>null</tt> if no
	 *         matching value was found.
	 * @throws IOException
	 *         If an I/O error occurred.
	 */
	public byte[] remove(byte[] key)
		throws IOException
	{
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
					}
					else {
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
		}
		finally {
			btreeLock.writeLock().unlock();
		}
	}

	/**
	 * Removes the value that matches the specified key from the tree starting at
	 * the specified node and returns the removed value.
	 * 
	 * @param key
	 *        A key that matches the value that should be removed from the
	 *        B-Tree.
	 * @param node
	 *        The root of the (sub) tree.
	 * @return The value that was removed from the B-Tree, or <tt>null</tt> if no
	 *         matching value was found.
	 * @throws IOException
	 *         If an I/O error occurred.
	 */
	private byte[] removeFromTree(byte[] key, Node node)
		throws IOException
	{
		byte[] value = null;

		// Search key
		int valueIdx = node.search(key);

		if (valueIdx >= 0) {
			// Found matching value in this node, remove it
			if (node.isLeaf()) {
				value = node.removeValueRight(valueIdx);
			}
			else {
				// Replace the matching value with the largest value from the left
				// child node
				value = node.getValue(valueIdx);

				Node leftChildNode = node.getChildNode(valueIdx);
				byte[] largestValue = removeLargestValueFromTree(leftChildNode);

				node.setValue(valueIdx, largestValue);

				balanceChildNode(node, leftChildNode, valueIdx);

				leftChildNode.release();
			}
		}
		else if (!node.isLeaf()) {
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
	 * Removes the largest value from the tree starting at the specified node and
	 * returns the removed value.
	 * 
	 * @param node
	 *        The root of the (sub) tree.
	 * @return The value that was removed from the B-Tree.
	 * @throws IOException
	 *         If an I/O error occurred.
	 * @throws IllegalArgumentException
	 *         If the supplied node is an empty leaf node
	 */
	private byte[] removeLargestValueFromTree(Node node)
		throws IOException
	{
		int nodeValueCount = node.getValueCount();

		if (node.isLeaf()) {
			if (node.isEmpty()) {
				throw new IllegalArgumentException("Trying to remove largest value from an empty node in "
						+ getFile());
			}
			return node.removeValueRight(nodeValueCount - 1);
		}
		else {
			// Recurse into right-most child node
			Node childNode = node.getChildNode(nodeValueCount);
			byte[] value = removeLargestValueFromTree(childNode);
			balanceChildNode(node, childNode, nodeValueCount);
			childNode.release();
			return value;
		}
	}

	private void balanceChildNode(Node parentNode, Node childNode, int childIdx)
		throws IOException
	{
		if (childNode.getValueCount() < minValueCount) {
			// Child node contains too few values, try to borrow one from its right
			// sibling
			Node rightSibling = (childIdx < parentNode.getValueCount()) ? parentNode.getChildNode(childIdx + 1)
					: null;

			if (rightSibling != null && rightSibling.getValueCount() > minValueCount) {
				// Right sibling has enough values to give one up
				parentNode.rotateLeft(childIdx, childNode, rightSibling);
			}
			else {
				// Right sibling does not have enough values to give one up, try its
				// left sibling
				Node leftSibling = (childIdx > 0) ? parentNode.getChildNode(childIdx - 1) : null;

				if (leftSibling != null && leftSibling.getValueCount() > minValueCount) {
					// Left sibling has enough values to give one up
					parentNode.rotateRight(childIdx, leftSibling, childNode);
				}
				else {
					// Both siblings contain the minimum amount of values,
					// merge the child node with its left or right sibling
					if (leftSibling != null) {
						leftSibling.mergeWithRightSibling(parentNode.removeValueRight(childIdx - 1), childNode);
					}
					else {
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
	 * @throws IOException
	 *         If an I/O error occurred.
	 */
	public void clear()
		throws IOException
	{
		btreeLock.writeLock().lock();
		try {
			synchronized (nodeCache) {
				nodeCache.clear();
				mruNodes.clear();
			}
			nioFile.truncate(HEADER_LENGTH);

			if (rootNodeID != 0) {
				rootNodeID = 0;
				writeFileHeader();
			}

			allocatedNodesList.clear();
		}
		finally {
			btreeLock.writeLock().unlock();
		}
	}

	private Node createNewNode()
		throws IOException
	{
		int newNodeID = allocatedNodesList.allocateNode();

		Node node = new Node(newNodeID);

		synchronized (nodeCache) {
			if (nodeCache.size() >= NODE_CACHE_SIZE && mruNodes.size() > MIN_MRU_CACHE_SIZE) {
				// Make some room for the new node
				expelNodeFromCache();
			}

			node.use();

			nodeCache.put(node.getID(), node);
		}

		return node;
	}

	Node readRootNode()
		throws IOException
	{
		if (rootNodeID > 0) {
			return readNode(rootNodeID);
		}
		return null;
	}

	private Node readNode(int id)
		throws IOException
	{
		if (id <= 0) {
			throw new IllegalArgumentException("id must be larger than 0, is: " + id + " in " + getFile());
		}

		// Check node cache
		synchronized (nodeCache) {
			Node node = nodeCache.get(id);

			if (node != null) {
				// Found node in cache
				int usageCount = node.use();
				if (usageCount == 1) {
					mruNodes.remove(id);
				}
			}
			else {
				if (nodeCache.size() >= NODE_CACHE_SIZE && mruNodes.size() > MIN_MRU_CACHE_SIZE) {
					// Make some room for the new node
					expelNodeFromCache();
				}

				// Read node from disk and add to cache
				node = new Node(id);

				// FIXME: this blocks the (synchronized) access to the cache for
				// quite some time
				node.read();

				nodeCache.put(id, node);

				node.use();
			}

			return node;
		}
	}

	private void releaseNode(Node node)
		throws IOException
	{
		// Note: this method is called by Node.release(), which already
		// synchronizes on nodeCache. This method should not be called directly to
		// prevent concurrency issues!!!

		// synchronized (nodeCache) {
		if (node.isEmpty() && node.isLeaf()) {
			// Discard node
			node.write();
			nodeCache.remove(node.getID());

			// allow the node ID to be reused
			synchronized (allocatedNodesList) {
				allocatedNodesList.freeNode(node.getID());

				int maxNodeID = allocatedNodesList.getMaxNodeID();
				if (node.getID() > maxNodeID) {
					// Shrink file
					nioFile.truncate(nodeID2offset(maxNodeID) + nodeSize);
				}
			}
		}
		else {
			mruNodes.put(node.getID(), node);

			if (nodeCache.size() > NODE_CACHE_SIZE && mruNodes.size() > MIN_MRU_CACHE_SIZE) {
				expelNodeFromCache();
			}
		}
		// }
	}

	/**
	 * Tries to expel the least recently used node from the cache.
	 */
	private void expelNodeFromCache()
		throws IOException
	{
		if (!mruNodes.isEmpty()) {
			Iterator<Node> iter = mruNodes.values().iterator();
			Node lruNode = iter.next();

			if (lruNode.dataChanged()) {
				lruNode.write();
			}
			iter.remove();
			nodeCache.remove(lruNode.getID());
		}
	}

	private void writeFileHeader()
		throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH);
		buf.put(MAGIC_NUMBER);
		buf.put(FILE_FORMAT_VERSION);
		buf.putInt(blockSize);
		buf.putInt(valueSize);
		buf.putInt(rootNodeID);

		buf.rewind();

		nioFile.write(buf, 0L);
	}

	private long nodeID2offset(int id) {
		return (long)blockSize * id;
	}

	private int offset2nodeID(long offset) {
		return (int)(offset / blockSize);
	}

	/*------------------*
	 * Inner class Node *
	 *------------------*/

	class Node {

		/** This node's ID. */
		private final int id;

		/** This node's data. */
		private final byte[] data;

		/** The number of values containined in this node. */
		private int valueCount;

		/** The number of objects currently 'using' this node. */
		private int usageCount;

		/** Flag indicating whether the contents of data has changed. */
		private boolean dataChanged;

		/** Registered listeners that want to be notified of changes to the node. */
		private final LinkedList<NodeListener> listeners = new LinkedList<NodeListener>();

		/**
		 * Creates a new Node object with the specified ID.
		 * 
		 * @param id
		 *        The node's ID, must be larger than <tt>0</tt>.
		 * @throws IllegalArgumentException
		 *         If the specified <tt>id</tt> is &lt;= <tt>0</tt>.
		 */
		public Node(int id) {
			if (id <= 0) {
				throw new IllegalArgumentException("id must be larger than 0, is: " + id + " in " + getFile());
			}

			this.id = id;
			this.valueCount = 0;
			this.usageCount = 0;

			// Allocate enough room to store one more value and node ID;
			// this greatly simplifies the algorithm for splitting a node.
			this.data = new byte[nodeSize + slotSize];
		}

		public int getID() {
			return id;
		}

		public String toString() {
			return "node " + getID();
		}

		public boolean isLeaf() {
			return getChildNodeID(0) == 0;
		}

		public int use() {
			// synchronize on nodeCache because release() can call
			// releaseNode(Node) and readNode(int) calls this method
			synchronized (nodeCache) {
				return ++usageCount;
			}
		}

		public void release()
			throws IOException
		{
			// synchronize on nodeCache because this method can call
			// releaseNode(Node) and readNode(int) can call use()
			synchronized (nodeCache) {
				assert usageCount > 0 : "Releasing node while usage count is " + usageCount;

				usageCount--;

				if (usageCount == 0) {
					releaseNode(this);
				}
			}
		}

		public int getUsageCount() {
			return usageCount;
		}

		public boolean dataChanged() {
			return dataChanged;
		}

		public int getValueCount() {
			return valueCount;
		}

		public int getNodeCount() {
			if (isLeaf()) {
				return 0;
			}
			else {
				return valueCount + 1;
			}
		}

		/**
		 * Checks if this node has any values.
		 * 
		 * @return <tt>true</tt> if this node has no values, <tt>fals</tt> if it
		 *         has.
		 */
		public boolean isEmpty() {
			return valueCount == 0;
		}

		public boolean isFull() {
			return valueCount == branchFactor - 1;
		}

		public byte[] getValue(int valueIdx) {
			assert valueIdx >= 0 : "valueIdx must be positive, is: " + valueIdx;
			assert valueIdx < valueCount : "valueIdx out of range (" + valueIdx + " >= " + valueCount + ")";

			return ByteArrayUtil.get(data, valueIdx2offset(valueIdx), valueSize);
		}

		public void setValue(int valueIdx, byte[] value) {
			assert value != null : "value must not be null";
			assert valueIdx >= 0 : "valueIdx must be positive, is: " + valueIdx;
			assert valueIdx < valueCount : "valueIdx out of range (" + valueIdx + " >= " + valueCount + ")";

			ByteArrayUtil.put(value, data, valueIdx2offset(valueIdx));
			dataChanged = true;
		}

		/**
		 * Removes the value that can be found at the specified valueIdx and the
		 * node ID directly to the right of it.
		 * 
		 * @param valueIdx
		 *        A legal value index.
		 * @return The value that was removed.
		 * @see #removeValueLeft
		 */
		public byte[] removeValueRight(int valueIdx) {
			assert valueIdx >= 0 : "valueIdx must be positive, is: " + valueIdx;
			assert valueIdx < valueCount : "valueIdx out of range (" + valueIdx + " >= " + valueCount + ")";

			byte[] value = getValue(valueIdx);

			int endOffset = valueIdx2offset(valueCount);

			if (valueIdx < valueCount - 1) {
				// Shift the rest of the data one slot to the left
				shiftData(valueIdx2offset(valueIdx + 1), endOffset, -slotSize);
			}

			// Clear last slot
			clearData(endOffset - slotSize, endOffset);

			setValueCount(--valueCount);

			dataChanged = true;

			notifyValueRemoved(valueIdx);

			return value;
		}

		/**
		 * Removes the value that can be found at the specified valueIdx and the
		 * node ID directly to the left of it.
		 * 
		 * @param valueIdx
		 *        A legal value index.
		 * @return The value that was removed.
		 * @see #removeValueRight
		 */
		public byte[] removeValueLeft(int valueIdx) {
			assert valueIdx >= 0 : "valueIdx must be positive, is: " + valueIdx;
			assert valueIdx < valueCount : "valueIdx out of range (" + valueIdx + " >= " + valueCount + ")";

			byte[] value = getValue(valueIdx);

			int endOffset = valueIdx2offset(valueCount);

			// Move the rest of the data one slot to the left
			shiftData(nodeIdx2offset(valueIdx + 1), endOffset, -slotSize);

			// Clear last slot
			clearData(endOffset - slotSize, endOffset);

			setValueCount(--valueCount);

			dataChanged = true;

			notifyValueRemoved(valueIdx);

			return value;
		}

		public int getChildNodeID(int nodeIdx) {
			assert nodeIdx >= 0 : "nodeIdx must be positive, is: " + nodeIdx;
			assert nodeIdx <= valueCount : "nodeIdx out of range (" + nodeIdx + " > " + valueCount + ")";

			return ByteArrayUtil.getInt(data, nodeIdx2offset(nodeIdx));
		}

		public void setChildNodeID(int nodeIdx, int nodeID) {
			assert nodeIdx >= 0 : "nodeIdx must not be negative, is: " + nodeIdx;
			assert nodeIdx <= valueCount : "nodeIdx out of range (" + nodeIdx + " > " + valueCount + ")";
			assert nodeID >= 0 : "nodeID must not be negative, is: " + nodeID;

			ByteArrayUtil.putInt(nodeID, data, nodeIdx2offset(nodeIdx));
			dataChanged = true;
		}

		public Node getChildNode(int nodeIdx)
			throws IOException
		{
			assert nodeIdx >= 0 : "nodeIdx must be positive, is: " + nodeIdx;
			assert nodeIdx <= valueCount : "nodeIdx out of range (" + nodeIdx + " > " + valueCount + ")";

			int childNodeID = getChildNodeID(nodeIdx);
			return readNode(childNodeID);
		}

		/**
		 * Searches the node for values that match the specified key and returns
		 * its index. If no such value can be found, the index of the first value
		 * that is larger is returned as a negative value by multiplying the index
		 * with -1 and substracting 1 (result = -index - 1). The index can be
		 * calculated from this negative value using the same function, i.e.:
		 * index = -result - 1.
		 */
		public int search(byte[] key) {
			int low = 0;
			int high = valueCount - 1;

			while (low <= high) {
				int mid = (low + high) >> 1;
				int diff = comparator.compareBTreeValues(key, data, valueIdx2offset(mid), valueSize);

				if (diff < 0) {
					// key smaller than middle value
					high = mid - 1;
				}
				else if (diff > 0) {
					// key larger than middle value
					low = mid + 1;
				}
				else {
					// key equal to middle value
					return mid;
				}
			}
			return -low - 1;
		}

		public void insertValueNodeIDPair(int valueIdx, byte[] value, int nodeID) {
			assert valueIdx >= 0 : "valueIdx must be positive, is: " + valueIdx;
			assert valueIdx <= valueCount : "valueIdx out of range (" + valueIdx + " > " + valueCount + ")";
			assert value != null : "value must not be null";
			assert nodeID >= 0 : "nodeID must not be negative, is: " + nodeID;

			int offset = valueIdx2offset(valueIdx);

			if (valueIdx < valueCount) {
				// Shift values right of <offset> to the right
				shiftData(offset, valueIdx2offset(valueCount), slotSize);
			}

			// Insert the new value-nodeID pair
			ByteArrayUtil.put(value, data, offset);
			ByteArrayUtil.putInt(nodeID, data, offset + valueSize);

			// Raise the value count
			setValueCount(++valueCount);

			notifyValueAdded(valueIdx);

			dataChanged = true;
		}

		public void insertNodeIDValuePair(int nodeIdx, int nodeID, byte[] value) {
			assert nodeIdx >= 0 : "nodeIdx must not be negative, is: " + nodeIdx;
			assert nodeIdx <= valueCount : "nodeIdx out of range (" + nodeIdx + " > " + valueCount + ")";
			assert nodeID >= 0 : "nodeID must not be negative, is: " + nodeID;
			assert value != null : "value must not be null";

			int offset = nodeIdx2offset(nodeIdx);

			// Shift values right of <offset> to the right
			shiftData(offset, valueIdx2offset(valueCount), slotSize);

			// Insert the new slot
			ByteArrayUtil.putInt(nodeID, data, offset);
			ByteArrayUtil.put(value, data, offset + 4);

			// Raise the value count
			setValueCount(++valueCount);

			notifyValueAdded(nodeIdx);

			dataChanged = true;
		}

		/**
		 * Splits the node, moving half of its values to the supplied new node,
		 * inserting the supplied value-nodeID pair and returning the median
		 * value. The behaviour of this method when called on a node that isn't
		 * full is not specified and can produce unexpected results!
		 * 
		 * @throws IOException
		 */
		public byte[] splitAndInsert(byte[] newValue, int newNodeID, int newValueIdx, Node newNode)
			throws IOException
		{
			// First store the new value-node pair in data, then split it. This
			// can be done because data got one spare slot when it was allocated.
			insertValueNodeIDPair(newValueIdx, newValue, newNodeID);

			assert valueCount == branchFactor : "Node contains " + valueCount + " values, expected "
					+ branchFactor;

			// Node now contains exactly [branchFactor] values. The median
			// value at index [branchFactor/2] is moved to the parent
			// node, the values left of the median stay in this node, the
			// values right of the median are moved to the new node.
			int medianIdx = branchFactor / 2;
			int medianOffset = valueIdx2offset(medianIdx);
			int splitOffset = medianOffset + valueSize;

			// Move all data (including the spare slot) to the right of
			// <splitOffset> to the new node
			System.arraycopy(data, splitOffset, newNode.data, 4, data.length - splitOffset);

			// Get the median value
			byte[] medianValue = getValue(medianIdx);

			// Clear the right half of the data in this node
			clearData(medianOffset, data.length);

			// Update the value counts
			setValueCount(medianIdx);
			newNode.setValueCount(branchFactor - medianIdx - 1);
			newNode.dataChanged = true;

			notifyNodeSplit(newNode, medianIdx);

			// Return the median value; it should be inserted into the parent node
			return medianValue;
		}

		public void mergeWithRightSibling(byte[] medianValue, Node rightSibling)
			throws IOException
		{
			assert valueCount + rightSibling.getValueCount() + 1 < branchFactor : "Nodes contain too many values to be merged; left: "
					+ valueCount + "; right: " + rightSibling.getValueCount();

			// Append median value from parent node
			insertValueNodeIDPair(valueCount, medianValue, 0);

			int rightIdx = valueCount;

			// Append all values and node references from right sibling
			System.arraycopy(rightSibling.data, 4, data, nodeIdx2offset(rightIdx),
					valueIdx2offset(rightSibling.valueCount) - 4);

			setValueCount(valueCount + rightSibling.valueCount);

			rightSibling.clearData(4, valueIdx2offset(rightSibling.valueCount));
			rightSibling.setValueCount(0);
			rightSibling.dataChanged = true;

			rightSibling.notifyNodeMerged(this, rightIdx);
		}

		public void rotateLeft(int valueIdx, Node leftChildNode, Node rightChildNode)
			throws IOException
		{
			leftChildNode.insertValueNodeIDPair(leftChildNode.getValueCount(), this.getValue(valueIdx),
					rightChildNode.getChildNodeID(0));
			setValue(valueIdx, rightChildNode.removeValueLeft(0));
			notifyRotatedLeft(valueIdx, leftChildNode, rightChildNode);
		}

		public void rotateRight(int valueIdx, Node leftChildNode, Node rightChildNode)
			throws IOException
		{
			rightChildNode.insertNodeIDValuePair(0, leftChildNode.getChildNodeID(leftChildNode.getValueCount()),
					this.getValue(valueIdx - 1));
			setValue(valueIdx - 1, leftChildNode.removeValueRight(leftChildNode.getValueCount() - 1));
			notifyRotatedRight(valueIdx, leftChildNode, rightChildNode);
		}

		public void register(NodeListener listener) {
			synchronized (listeners) {
				assert !listeners.contains(listener);
				listeners.add(listener);
			}
		}

		public void deregister(NodeListener listener) {
			synchronized (listeners) {
	  		   assert listeners.contains(listener);
				listeners.remove(listener);
			}
		}

		private void notifyValueAdded(int index) {
			synchronized (listeners) {
				Iterator<NodeListener> iter = listeners.iterator();

				while (iter.hasNext()) {
					// Deregister if listener return true
					if (iter.next().valueAdded(this, index)) {
						iter.remove();
					}
				}
			}
		}

		private void notifyValueRemoved(int index) {
			synchronized (listeners) {
				Iterator<NodeListener> iter = listeners.iterator();

				while (iter.hasNext()) {
					// Deregister if listener return true
					if (iter.next().valueRemoved(this, index)) {
						iter.remove();
					}
				}
			}
		}

		private void notifyRotatedLeft(int index, Node leftChildNode, Node rightChildNode)
			throws IOException
		{
			synchronized (listeners) {
				Iterator<NodeListener> iter = listeners.iterator();

				while (iter.hasNext()) {
					// Deregister if listener return true
					if (iter.next().rotatedLeft(this, index, leftChildNode, rightChildNode)) {
						iter.remove();
					}
				}
			}
		}

		private void notifyRotatedRight(int index, Node leftChildNode, Node rightChildNode)
			throws IOException
		{
			synchronized (listeners) {
				Iterator<NodeListener> iter = listeners.iterator();

				while (iter.hasNext()) {
					// Deregister if listener return true
					if (iter.next().rotatedRight(this, index, leftChildNode, rightChildNode)) {
						iter.remove();
					}
				}
			}
		}

		private void notifyNodeSplit(Node rightNode, int medianIdx)
			throws IOException
		{
			synchronized (listeners) {
				Iterator<NodeListener> iter = listeners.iterator();

				while (iter.hasNext()) {
					boolean deregister = iter.next().nodeSplit(this, rightNode, medianIdx);

					if (deregister) {
						iter.remove();
					}
				}
			}
		}

		private void notifyNodeMerged(Node targetNode, int mergeIdx)
			throws IOException
		{
			synchronized (listeners) {
				Iterator<NodeListener> iter = listeners.iterator();

				while (iter.hasNext()) {
					boolean deregister = iter.next().nodeMergedWith(this, targetNode, mergeIdx);

					if (deregister) {
						iter.remove();
					}
				}
			}
		}

		public void read()
			throws IOException
		{
			ByteBuffer buf = ByteBuffer.wrap(data);

			// Don't fill the spare slot in data:
			buf.limit(nodeSize);

			int bytesRead = nioFile.read(buf, nodeID2offset(id));
			assert bytesRead == nodeSize : "Read operation didn't read the entire node (" + bytesRead + " of "
					+ nodeSize + " bytes)";

			valueCount = ByteArrayUtil.getInt(data, 0);
		}

		public void write()
			throws IOException
		{
			ByteBuffer buf = ByteBuffer.wrap(data);

			// Don't write the spare slot in data to the file:
			buf.limit(nodeSize);

			int bytesWritten = nioFile.write(buf, nodeID2offset(id));
			assert bytesWritten == nodeSize : "Write operation didn't write the entire node (" + bytesWritten
					+ " of " + nodeSize + " bytes)";

			dataChanged = false;
		}

		/**
		 * Shifts the data between <tt>startOffset</tt> (inclusive) and
		 * <tt>endOffset</tt> (exclusive) <tt>shift</tt> positions to the right.
		 * Negative shift values can be used to shift data to the left.
		 */
		private void shiftData(int startOffset, int endOffset, int shift) {
			System.arraycopy(data, startOffset, data, startOffset + shift, endOffset - startOffset);
		}

		/**
		 * Clears the data between <tt>startOffset</tt> (inclusive) and
		 * <tt>endOffset</tt> (exclusive). All bytes in this range will be set to
		 * 0.
		 */
		private void clearData(int startOffset, int endOffset) {
			Arrays.fill(data, startOffset, endOffset, (byte)0);
		}

		private void setValueCount(int valueCount) {
			this.valueCount = valueCount;
			ByteArrayUtil.putInt(valueCount, data, 0);
		}

		private int valueIdx2offset(int id) {
			return 8 + id * slotSize;
		}

		private int nodeIdx2offset(int id) {
			return 4 + id * slotSize;
		}
	}

	/*--------------------------*
	 * Inner class NodeListener *
	 *--------------------------*/

	private interface NodeListener {

		/**
		 * Signals to registered node listeners that a value has been added to a
		 * node.
		 * 
		 * @param node
		 *        The node which the value has been added to.
		 * @param index
		 *        The index where the value was inserted.
		 * @return Indicates whether the node listener should be deregistered as a
		 *         result of this event.
		 */
		public boolean valueAdded(Node node, int index);

		/**
		 * Signals to registered node listeners that a value has been removed from
		 * a node.
		 * 
		 * @param node
		 *        The node which the value has been removed from.
		 * @param index
		 *        The index where the value was removed.
		 * @return Indicates whether the node listener should be deregistered as a
		 *         result of this event.
		 */
		public boolean valueRemoved(Node node, int index);

		public boolean rotatedLeft(Node node, int index, Node leftChildNode, Node rightChildNode)
			throws IOException;

		public boolean rotatedRight(Node node, int index, Node leftChildNode, Node rightChildNode)
			throws IOException;

		/**
		 * Signals to registered node listeners that a node has been split.
		 * 
		 * @param node
		 *        The node which has been split.
		 * @param newNode
		 *        The newly allocated node containing the "right" half of the
		 *        values.
		 * @param medianIdx
		 *        The index where the node has been split. The value at this index
		 *        has been moved to the node's parent.
		 * @return Indicates whether the node listener should be deregistered as a
		 *         result of this event.
		 */
		public boolean nodeSplit(Node node, Node newNode, int medianIdx)
			throws IOException;

		/**
		 * Signals to registered node listeners that two nodes have been merged.
		 * All values from the source node have been appended to the value of the
		 * target node.
		 * 
		 * @param sourceNode
		 *        The node that donated its values to the target node.
		 * @param targetNode
		 *        The node in which the values have been merged.
		 * @param mergeIdx
		 *        The index of <tt>sourceNode</tt>'s values in <tt>targetNode</tt>
		 *        .
		 * @return Indicates whether the node listener should be deregistered with
		 *         the <em>source node</em> as a result of this event.
		 */
		public boolean nodeMergedWith(Node sourceNode, Node targetNode, int mergeIdx)
			throws IOException;
	}

	/*-----------------------------*
	 * Inner class SeqScanIterator *
	 *-----------------------------*/

	// private class SeqScanIterator implements RecordIterator {
	//
	// private byte[] searchKey;
	//
	// private byte[] searchMask;
	//
	// private int currentNodeID;
	//
	// private Node currentNode;
	//
	// private int currentIdx;
	//
	// public SeqScanIterator(byte[] searchKey, byte[] searchMask) {
	// this.searchKey = searchKey;
	// this.searchMask = searchMask;
	// }
	//
	// public byte[] next()
	// throws IOException
	// {
	// while (currentNodeID <= maxNodeID) {
	// if (currentNode == null) {
	// // Read first node
	// currentNodeID = 1;
	// currentNode = readNode(currentNodeID);
	// currentIdx = 0;
	// }
	//
	// while (currentIdx < currentNode.getValueCount()) {
	// byte[] value = currentNode.getValue(currentIdx++);
	//
	// if (searchKey == null || ByteArrayUtil.matchesPattern(value, searchMask,
	// searchKey)) {
	// // Found a matches value
	// return value;
	// }
	// }
	//
	// currentNode.release();
	//
	// currentNodeID++;
	// currentNode = (currentNodeID <= maxNodeID) ? readNode(currentNodeID) :
	// null;
	// currentIdx = 0;
	// }
	//
	// return null;
	// }
	//
	// public void set(byte[] value) {
	// if (currentNode == null || currentIdx > currentNode.getValueCount()) {
	// throw new IllegalStateException();
	// }
	//
	// currentNode.setValue(currentIdx - 1, value);
	// }
	//
	// public void close()
	// throws IOException
	// {
	// if (currentNode != null) {
	// currentNodeID = maxNodeID + 1;
	//
	// currentNode.release();
	// currentNode = null;
	// }
	// }
	// }
	/*---------------------------*
	 * Inner class RangeIterator *
	 *---------------------------*/

	private class RangeIterator implements RecordIterator, NodeListener {

		private final byte[] searchKey;

		private final byte[] searchMask;

		private final byte[] minValue;

		private final byte[] maxValue;

		private boolean started;

		private Node currentNode;

		private final AtomicBoolean revisitValue = new AtomicBoolean();

		/**
		 * Tracks the parent nodes of {@link #currentNode}.
		 */
		private final LinkedList<Node> parentNodeStack = new LinkedList<Node>();

		/**
		 * Tracks the index of child nodes in parent nodes.
		 */
		private final LinkedList<Integer> parentIndexStack = new LinkedList<Integer>();

		private int currentIdx;

		public RangeIterator(byte[] searchKey, byte[] searchMask, byte[] minValue, byte[] maxValue) {
			this.searchKey = searchKey;
			this.searchMask = searchMask;
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.started = false;
		}

		public byte[] next()
			throws IOException
		{
			btreeLock.readLock().lock();
			try {
				if (!started) {
					started = true;
					findMinimum();
				}

				byte[] value = findNext(revisitValue.getAndSet(false));
				while (value != null) {
					if (maxValue != null && comparator.compareBTreeValues(maxValue, value, 0, value.length) < 0) {
						// Reached maximum value, stop iterating
						close();
						value = null;
						break;
					}
					else if (searchKey != null && !ByteArrayUtil.matchesPattern(value, searchMask, searchKey)) {
						// Value doesn't match search key/mask
						value = findNext(false);
						continue;
					}
					else {
						// Matching value found
						break;
					}
				}

				return value;
			}
			finally {
				btreeLock.readLock().unlock();
			}
		}

		private void findMinimum()
			throws IOException
		{
			currentNode = readRootNode();

			if (currentNode == null) {
				// Empty BTree
				return;
			}

			currentNode.register(this);
			currentIdx = 0;

			// Search first value >= minValue, or the left-most value in case
			// minValue is null
			while (true) {
				if (minValue != null) {
					currentIdx = currentNode.search(minValue);

					if (currentIdx >= 0) {
						// Found exact match with minimum value
						break;
					}
					else {
						// currentIdx indicates the first value larger than the
						// minimum value
						currentIdx = -currentIdx - 1;
					}
				}

				if (currentNode.isLeaf()) {
					break;
				}
				else {
					// [SES-725] must change stacks after node loading has succeeded
					Node childNode = currentNode.getChildNode(currentIdx);
					pushStacks(childNode);
				}
			}
		}

		private byte[] findNext(boolean returnedFromRecursion)
			throws IOException
		{
			if (currentNode == null) {
				return null;
			}

			if (returnedFromRecursion || currentNode.isLeaf()) {
				if (currentIdx >= currentNode.getValueCount()) {
					// No more values in this node, continue with parent node
					popStacks();
					return findNext(true);
				}
				else {
					return currentNode.getValue(currentIdx++);
				}
			}
			else {
				// [SES-725] must change stacks after node loading has succeeded
				Node childNode = currentNode.getChildNode(currentIdx);
				pushStacks(childNode);
				return findNext(false);
			}
		}

		public void set(byte[] value) {
			btreeLock.readLock().lock();
			try {
				if (currentNode == null || currentIdx > currentNode.getValueCount()) {
					throw new IllegalStateException();
				}

				currentNode.setValue(currentIdx - 1, value);
			}
			finally {
				btreeLock.readLock().unlock();
			}
		}

		public void close()
			throws IOException
		{
			btreeLock.readLock().lock();
			try {
				while (popStacks())
					;

				assert parentNodeStack.isEmpty();
				assert parentIndexStack.isEmpty();
			}
			finally {
				btreeLock.readLock().unlock();
			}
		}

		private void pushStacks(Node newChildNode) {
			newChildNode.register(this);
			parentNodeStack.add(currentNode);
			parentIndexStack.add(currentIdx);
			currentNode = newChildNode;
			currentIdx = 0;
		}

		private boolean popStacks()
			throws IOException
		{
			if (currentNode == null) {
				// There's nothing to pop
				return false;
			}

			currentNode.deregister(this);
			currentNode.release();

			if (!parentNodeStack.isEmpty()) {
				currentNode = parentNodeStack.removeLast();
				currentIdx = parentIndexStack.removeLast();
				return true;
			}
			else {
				currentNode = null;
				currentIdx = 0;
				return false;
			}
		}

		public boolean valueAdded(Node node, int addedIndex) {
			assert btreeLock.isWriteLockedByCurrentThread();

			if (node == currentNode) {
				if (addedIndex < currentIdx) {
					currentIdx++;
				}
			}
			else {
				for (int i = 0; i < parentNodeStack.size(); i++) {
					if (node == parentNodeStack.get(i)) {
						int parentIdx = parentIndexStack.get(i);
						if (addedIndex < parentIdx) {
							parentIndexStack.set(i, parentIdx + 1);
						}

						break;
					}
				}
			}

			return false;
		}

		public boolean valueRemoved(Node node, int removedIndex) {
			assert btreeLock.isWriteLockedByCurrentThread();

			if (node == currentNode) {
				if (removedIndex < currentIdx) {
					currentIdx--;
				}
			}
			else {
				for (int i = 0; i < parentNodeStack.size(); i++) {
					if (node == parentNodeStack.get(i)) {
						int parentIdx = parentIndexStack.get(i);
						if (removedIndex < parentIdx) {
							parentIndexStack.set(i, parentIdx - 1);
						}

						break;
					}
				}
			}

			return false;
		}

		public boolean rotatedLeft(Node node, int valueIndex, Node leftChildNode, Node rightChildNode)
			throws IOException
		{
			if (currentNode == node) {
				if (valueIndex == currentIdx - 1) {
					// the value that was removed had just been visited
					currentIdx = valueIndex;
					revisitValue.set(true);

					if (!node.isLeaf()) {
						pushStacks(leftChildNode);
						leftChildNode.use();
					}
				}
			}
			else if (currentNode == rightChildNode) {
				if (currentIdx == 0) {
					// the value that would be visited next has been moved to the
					// parent node
					popStacks();
					currentIdx = valueIndex;
					revisitValue.set(true);
				}
			}
			else {
				for (int i = 0; i < parentNodeStack.size(); i++) {
					Node stackNode = parentNodeStack.get(i);

					if (stackNode == rightChildNode) {
						int stackIdx = parentIndexStack.get(i);

						if (stackIdx == 0) {
							// this node is no longer the parent, replace with left
							// sibling
							rightChildNode.deregister(this);
							rightChildNode.release();

							leftChildNode.use();
							leftChildNode.register(this);

							parentNodeStack.set(i, leftChildNode);
							parentIndexStack.set(i, leftChildNode.getValueCount());
						}

						break;
					}
				}
			}

			return false;
		}

		public boolean rotatedRight(Node node, int valueIndex, Node leftChildNode, Node rightChildNode)
			throws IOException
		{
			for (int i = 0; i < parentNodeStack.size(); i++) {
				Node stackNode = parentNodeStack.get(i);

				if (stackNode == leftChildNode) {
					int stackIdx = parentIndexStack.get(i);

					if (stackIdx == leftChildNode.getValueCount()) {
						// this node is no longer the parent, replace with right
						// sibling
						leftChildNode.deregister(this);
						leftChildNode.release();

						rightChildNode.use();
						rightChildNode.register(this);

						parentNodeStack.set(i, rightChildNode);
						parentIndexStack.set(i, 0);
					}

					break;
				}
			}

			return false;
		}

		public boolean nodeSplit(Node node, Node newNode, int medianIdx)
			throws IOException
		{
			assert btreeLock.isWriteLockedByCurrentThread();

			boolean deregister = false;

			if (node == currentNode) {
				if (currentIdx > medianIdx) {
					currentNode.release();
					deregister = true;

					newNode.use();
					newNode.register(this);

					currentNode = newNode;
					currentIdx -= medianIdx + 1;
				}
			}
			else {
				for (int i = 0; i < parentNodeStack.size(); i++) {
					Node parentNode = parentNodeStack.get(i);

					if (node == parentNode) {
						int parentIdx = parentIndexStack.get(i);

						if (parentIdx > medianIdx) {
							parentNode.release();
							deregister = true;

							newNode.use();
							newNode.register(this);

							parentNodeStack.set(i, newNode);
							parentIndexStack.set(i, parentIdx - medianIdx - 1);
						}

						break;
					}
				}
			}

			return deregister;
		}

		public boolean nodeMergedWith(Node sourceNode, Node targetNode, int mergeIdx)
			throws IOException
		{
			assert btreeLock.isWriteLockedByCurrentThread();

			boolean deregister = false;

			if (sourceNode == currentNode) {
				currentNode.release();
				deregister = true;

				targetNode.use();
				targetNode.register(this);

				currentNode = targetNode;
				currentIdx += mergeIdx;
			}
			else {
				for (int i = 0; i < parentNodeStack.size(); i++) {
					Node parentNode = parentNodeStack.get(i);

					if (sourceNode == parentNode) {
						parentNode.release();
						deregister = true;

						targetNode.use();
						targetNode.register(this);

						parentNodeStack.set(i, targetNode);
						parentIndexStack.set(i, mergeIdx + parentIndexStack.get(i));

						break;
					}
				}
			}

			return deregister;
		}
	}

	/*--------------*
	 * Test methods *
	 *--------------*/

	public static void main(String[] args)
		throws Exception
	{
		System.out.println("Running BTree test...");
		if (args.length > 2) {
			runPerformanceTest(args);
		}
		else {
			runDebugTest(args);
		}
		System.out.println("Done.");
	}

	public static void runPerformanceTest(String[] args)
		throws Exception
	{
		File dataDir = new File(args[0]);
		String filenamePrefix = args[1];
		int valueCount = Integer.parseInt(args[2]);
		RecordComparator comparator = new DefaultRecordComparator();
		BTree btree = new BTree(dataDir, filenamePrefix, 501, 13, comparator);

		java.util.Random random = new java.util.Random(0L);
		byte[] value = new byte[13];

		long startTime = System.currentTimeMillis();
		for (int i = 1; i <= valueCount; i++) {
			random.nextBytes(value);
			btree.insert(value);
			if (i % 50000 == 0) {
				System.out.println("Inserted " + i + " values in " + (System.currentTimeMillis() - startTime)
						+ " ms");
			}
		}

		System.out.println("Iterating over all values in sequential order...");
		startTime = System.currentTimeMillis();
		RecordIterator iter = btree.iterateAll();
		value = iter.next();
		int count = 0;
		while (value != null) {
			count++;
			value = iter.next();
		}
		iter.close();
		System.out.println("Iteration over " + count + " items finished in "
				+ (System.currentTimeMillis() - startTime) + " ms");

		// byte[][] values = new byte[count][13];
		//
		// iter = btree.iterateAll();
		// for (int i = 0; i < values.length; i++) {
		// values[i] = iter.next();
		// }
		// iter.close();
		//
		// startTime = System.currentTimeMillis();
		// for (int i = values.length - 1; i >= 0; i--) {
		// btree.remove(values[i]);
		// }
		// System.out.println("Removed all item in " + (System.currentTimeMillis()
		// - startTime) + " ms");
	}

	public static void runDebugTest(String[] args)
		throws Exception
	{
		File dataDir = new File(args[0]);
		String filenamePrefix = args[1];
		BTree btree = new BTree(dataDir, filenamePrefix, 28, 1);

		btree.print(System.out);

		/*
		 * System.out.println("Adding values..."); btree.startTransaction();
		 * btree.insert("C".getBytes()); btree.insert("N".getBytes());
		 * btree.insert("G".getBytes()); btree.insert("A".getBytes());
		 * btree.insert("H".getBytes()); btree.insert("E".getBytes());
		 * btree.insert("K".getBytes()); btree.insert("Q".getBytes());
		 * btree.insert("M".getBytes()); btree.insert("F".getBytes());
		 * btree.insert("W".getBytes()); btree.insert("L".getBytes());
		 * btree.insert("T".getBytes()); btree.insert("Z".getBytes());
		 * btree.insert("D".getBytes()); btree.insert("P".getBytes());
		 * btree.insert("R".getBytes()); btree.insert("X".getBytes());
		 * btree.insert("Y".getBytes()); btree.insert("S".getBytes());
		 * btree.commitTransaction(); btree.print(System.out);
		 * System.out.println("Removing values..."); System.out.println("Removing
		 * H..."); btree.remove("H".getBytes()); btree.commitTransaction();
		 * btree.print(System.out); System.out.println("Removing T...");
		 * btree.remove("T".getBytes()); btree.commitTransaction();
		 * btree.print(System.out); System.out.println("Removing R...");
		 * btree.remove("R".getBytes()); btree.commitTransaction();
		 * btree.print(System.out); System.out.println("Removing E...");
		 * btree.remove("E".getBytes()); btree.commitTransaction();
		 * btree.print(System.out); System.out.println("Values from I to U:");
		 * RecordIterator iter = btree.iterateRange("I".getBytes(),
		 * "V".getBytes()); byte[] value = iter.next(); while (value != null) {
		 * System.out.print(new String(value) + " "); value = iter.next(); }
		 * System.out.println();
		 */
	}

	public void print(PrintStream out)
		throws IOException
	{
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