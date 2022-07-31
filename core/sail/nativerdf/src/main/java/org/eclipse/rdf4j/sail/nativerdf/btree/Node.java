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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;

class Node {

	/** This node's ID. */
	private final int id;

	private final BTree tree;

	/** This node's data. */
	private final byte[] data;

	/** The number of values containined in this node. */
	private int valueCount;

	/** The number of objects currently 'using' this node. */
	private final AtomicInteger usageCount = new AtomicInteger(0);

	/** Flag indicating whether the contents of data has changed. */
	private boolean dataChanged;

	/** Registered listeners that want to be notified of changes to the node. */
	private final ConcurrentLinkedDeque<NodeListener> listeners = new ConcurrentLinkedDeque<>();

	/**
	 * Creates a new Node object with the specified ID.
	 *
	 * @param id The node's ID, must be larger than <var>0</var>.
	 * @throws IllegalArgumentException If the specified <var>id</var> is &lt;= <var>0</var>.
	 */
	public Node(int id, BTree tree) {
		if (id <= 0) {
			throw new IllegalArgumentException("id must be larger than 0, is: " + id + " in " + tree.getFile());
		}

		this.id = id;
		this.tree = tree;
		this.valueCount = 0;

		// Allocate enough room to store one more value and node ID;
		// this greatly simplifies the algorithm for splitting a node.
		this.data = new byte[tree.nodeSize + tree.slotSize];
	}

	public int getID() {
		return id;
	}

	@Override
	public String toString() {
		return "node " + getID();
	}

	public boolean isLeaf() {
		return getChildNodeID(0) == 0;
	}

	public int use() {
		return usageCount.incrementAndGet();
	}

	public void release() throws IOException {
		int newUsage = usageCount.decrementAndGet();
		assert newUsage >= 0 : "Releasing node while usage count is " + (newUsage + 1);

		if (newUsage == 0) {
			tree.releaseNode(this);
		}
	}

	public int getUsageCount() {
		return usageCount.get();
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
		} else {
			return valueCount + 1;
		}
	}

	/**
	 * Checks if this node has any values.
	 *
	 * @return <var>true</var> if this node has no values, <var>fals</var> if it has.
	 */
	public boolean isEmpty() {
		return valueCount == 0;
	}

	public boolean isFull() {
		return valueCount == tree.branchFactor - 1;
	}

	public byte[] getValue(int valueIdx) {
		assert valueIdx >= 0 : "valueIdx must be positive, is: " + valueIdx;
		assert valueIdx < valueCount : "valueIdx out of range (" + valueIdx + " >= " + valueCount + ")";

		return ByteArrayUtil.get(data, valueIdx2offset(valueIdx), tree.valueSize);
	}

	public void setValue(int valueIdx, byte[] value) {
		assert value != null : "value must not be null";
		assert valueIdx >= 0 : "valueIdx must be positive, is: " + valueIdx;
		assert valueIdx < valueCount : "valueIdx out of range (" + valueIdx + " >= " + valueCount + ")";

		ByteArrayUtil.put(value, data, valueIdx2offset(valueIdx));
		dataChanged = true;
	}

	/**
	 * Removes the value that can be found at the specified valueIdx and the node ID directly to the right of it.
	 *
	 * @param valueIdx A legal value index.
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
			shiftData(valueIdx2offset(valueIdx + 1), endOffset, -tree.slotSize);
		}

		// Clear last slot
		clearData(endOffset - tree.slotSize, endOffset);

		setValueCount(--valueCount);

		dataChanged = true;

		notifyValueRemoved(valueIdx);

		return value;
	}

	/**
	 * Removes the value that can be found at the specified valueIdx and the node ID directly to the left of it.
	 *
	 * @param valueIdx A legal value index.
	 * @return The value that was removed.
	 * @see #removeValueRight
	 */
	public byte[] removeValueLeft(int valueIdx) {
		assert valueIdx >= 0 : "valueIdx must be positive, is: " + valueIdx;
		assert valueIdx < valueCount : "valueIdx out of range (" + valueIdx + " >= " + valueCount + ")";

		byte[] value = getValue(valueIdx);

		int endOffset = valueIdx2offset(valueCount);

		// Move the rest of the data one slot to the left
		shiftData(nodeIdx2offset(valueIdx + 1), endOffset, -tree.slotSize);

		// Clear last slot
		clearData(endOffset - tree.slotSize, endOffset);

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

	public Node getChildNode(int nodeIdx) throws IOException {
		assert nodeIdx >= 0 : "nodeIdx must be positive, is: " + nodeIdx;
		assert nodeIdx <= valueCount : "nodeIdx out of range (" + nodeIdx + " > " + valueCount + ")";

		int childNodeID = getChildNodeID(nodeIdx);
		return tree.readNode(childNodeID);
	}

	/**
	 * Searches the node for values that match the specified key and returns its index. If no such value can be found,
	 * the index of the first value that is larger is returned as a negative value by multiplying the index with -1 and
	 * substracting 1 (result = -index - 1). The index can be calculated from this negative value using the same
	 * function, i.e.: index = -result - 1.
	 */
	public int search(byte[] key) {
		int low = 0;
		int high = valueCount - 1;

		while (low <= high) {
			int mid = (low + high) >> 1;
			int diff = tree.comparator.compareBTreeValues(key, data, valueIdx2offset(mid), tree.valueSize);

			if (diff < 0) {
				// key smaller than middle value
				high = mid - 1;
			} else if (diff > 0) {
				// key larger than middle value
				low = mid + 1;
			} else {
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
			shiftData(offset, valueIdx2offset(valueCount), tree.slotSize);
		}

		// Insert the new value-nodeID pair
		ByteArrayUtil.put(value, data, offset);
		ByteArrayUtil.putInt(nodeID, data, offset + tree.valueSize);

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
		shiftData(offset, valueIdx2offset(valueCount), tree.slotSize);

		// Insert the new slot
		ByteArrayUtil.putInt(nodeID, data, offset);
		ByteArrayUtil.put(value, data, offset + 4);

		// Raise the value count
		setValueCount(++valueCount);

		notifyValueAdded(nodeIdx);

		dataChanged = true;
	}

	/**
	 * Splits the node, moving half of its values to the supplied new node, inserting the supplied value-nodeID pair and
	 * returning the median value. The behaviour of this method when called on a node that isn't full is not specified
	 * and can produce unexpected results!
	 *
	 * @throws IOException
	 */
	public byte[] splitAndInsert(byte[] newValue, int newNodeID, int newValueIdx, Node newNode) throws IOException {
		// First store the new value-node pair in data, then split it. This
		// can be done because data got one spare slot when it was allocated.
		insertValueNodeIDPair(newValueIdx, newValue, newNodeID);

		assert valueCount == tree.branchFactor : "Node contains " + valueCount + " values, expected "
				+ tree.branchFactor;

		// Node now contains exactly [branchFactor] values. The median
		// value at index [branchFactor/2] is moved to the parent
		// node, the values left of the median stay in this node, the
		// values right of the median are moved to the new node.
		int medianIdx = tree.branchFactor / 2;
		int medianOffset = valueIdx2offset(medianIdx);
		int splitOffset = medianOffset + tree.valueSize;

		// Move all data (including the spare slot) to the right of
		// <splitOffset> to the new node
		System.arraycopy(data, splitOffset, newNode.data, 4, data.length - splitOffset);

		// Get the median value
		byte[] medianValue = getValue(medianIdx);

		// Clear the right half of the data in this node
		clearData(medianOffset, data.length);

		// Update the value counts
		setValueCount(medianIdx);
		newNode.setValueCount(tree.branchFactor - medianIdx - 1);
		newNode.dataChanged = true;

		notifyNodeSplit(newNode, medianIdx);

		// Return the median value; it should be inserted into the parent node
		return medianValue;
	}

	public void mergeWithRightSibling(byte[] medianValue, Node rightSibling) throws IOException {
		assert valueCount + rightSibling.getValueCount()
				+ 1 < tree.branchFactor : "Nodes contain too many values to be merged; left: " + valueCount
						+ "; right: " + rightSibling.getValueCount();

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

	public void rotateLeft(int valueIdx, Node leftChildNode, Node rightChildNode) throws IOException {
		leftChildNode.insertValueNodeIDPair(leftChildNode.getValueCount(), this.getValue(valueIdx),
				rightChildNode.getChildNodeID(0));
		setValue(valueIdx, rightChildNode.removeValueLeft(0));
		notifyRotatedLeft(valueIdx, leftChildNode, rightChildNode);
	}

	public void rotateRight(int valueIdx, Node leftChildNode, Node rightChildNode) throws IOException {
		rightChildNode.insertNodeIDValuePair(0, leftChildNode.getChildNodeID(leftChildNode.getValueCount()),
				this.getValue(valueIdx - 1));
		setValue(valueIdx - 1, leftChildNode.removeValueRight(leftChildNode.getValueCount() - 1));
		notifyRotatedRight(valueIdx, leftChildNode, rightChildNode);
	}

	public void register(NodeListener listener) {
		// assert !listeners.contains(listener);
		listeners.add(listener);
	}

	public void deregister(NodeListener listener) {
		// assert listeners.contains(listener);
		listeners.removeFirstOccurrence(listener);
	}

	private void notifyValueAdded(int index) {
		notifySafeListeners(nl -> nl.valueAdded(this, index));
	}

	private void notifyValueRemoved(int index) {
		notifySafeListeners(nl -> nl.valueRemoved(this, index));
	}

	private void notifyRotatedLeft(int index, Node leftChildNode, Node rightChildNode) throws IOException {
		notifyListeners(nl -> nl.rotatedLeft(this, index, leftChildNode, rightChildNode));
	}

	private void notifyRotatedRight(int index, Node leftChildNode, Node rightChildNode) throws IOException {
		notifyListeners(nl -> nl.rotatedRight(this, index, leftChildNode, rightChildNode));
	}

	private void notifyNodeSplit(Node rightNode, int medianIdx) throws IOException {
		notifyListeners(nl -> nl.nodeSplit(this, rightNode, medianIdx));
	}

	private void notifyNodeMerged(Node targetNode, int mergeIdx) throws IOException {
		notifyListeners(nl -> nl.nodeMergedWith(this, targetNode, mergeIdx));
	}

	@FunctionalInterface
	private interface NodeListenerNotifier {

		/**
		 * @return true if the notifier should be deregistered
		 */
		boolean apply(NodeListener listener) throws IOException;
	}

	private void notifyListeners(NodeListenerNotifier notifier) throws IOException {
		Iterator<NodeListener> iter = listeners.iterator();

		while (iter.hasNext()) {
			boolean deregister = notifier.apply(iter.next());

			if (deregister) {
				iter.remove();
			}
		}
	}

	private void notifySafeListeners(Function<NodeListener, Boolean> notifier) {
		Iterator<NodeListener> iter = listeners.iterator();

		while (iter.hasNext()) {
			boolean deregister = notifier.apply(iter.next());

			if (deregister) {
				iter.remove();
			}
		}
	}

	public void read() throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(data);

		// Don't fill the spare slot in data:
		buf.limit(tree.nodeSize);

		int bytesRead = tree.nioFile.read(buf, tree.nodeID2offset(id));
		assert bytesRead == tree.nodeSize : "Read operation didn't read the entire node (" + bytesRead + " of "
				+ tree.nodeSize + " bytes)";

		valueCount = ByteArrayUtil.getInt(data, 0);
	}

	public void write() throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(data);

		// Don't write the spare slot in data to the file:
		buf.limit(tree.nodeSize);

		int bytesWritten = tree.nioFile.write(buf, tree.nodeID2offset(id));
		assert bytesWritten == tree.nodeSize : "Write operation didn't write the entire node (" + bytesWritten + " of "
				+ tree.nodeSize + " bytes)";

		dataChanged = false;
	}

	/**
	 * Shifts the data between <var>startOffset</var> (inclusive) and <var>endOffset</var> (exclusive) <var>shift</var>
	 * positions to the right. Negative shift values can be used to shift data to the left.
	 */
	private void shiftData(int startOffset, int endOffset, int shift) {
		System.arraycopy(data, startOffset, data, startOffset + shift, endOffset - startOffset);
	}

	/**
	 * Clears the data between <var>startOffset</var> (inclusive) and <var>endOffset</var> (exclusive). All bytes in
	 * this range will be set to 0.
	 */
	private void clearData(int startOffset, int endOffset) {
		Arrays.fill(data, startOffset, endOffset, (byte) 0);
	}

	private void setValueCount(int valueCount) {
		this.valueCount = valueCount;
		ByteArrayUtil.putInt(valueCount, data, 0);
	}

	private int valueIdx2offset(int id) {
		return 8 + id * tree.slotSize;
	}

	private int nodeIdx2offset(int id) {
		return 4 + id * tree.slotSize;
	}
}
