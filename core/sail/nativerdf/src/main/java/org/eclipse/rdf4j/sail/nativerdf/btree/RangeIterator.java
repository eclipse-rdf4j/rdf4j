/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;

class RangeIterator implements RecordIterator, NodeListener {

	/**
	 *
	 */
	private final BTree tree;

	private final byte[] searchKey;

	private final byte[] searchMask;

	private final byte[] minValue;

	private final byte[] maxValue;

	private volatile boolean started;

	private volatile Node currentNode;

	private final AtomicBoolean revisitValue = new AtomicBoolean();

	/**
	 * Tracks the parent nodes of {@link #currentNode}.
	 */
	private final LinkedList<Node> parentNodeStack = new LinkedList<>();

	/**
	 * Tracks the index of child nodes in parent nodes.
	 */
	private final LinkedList<Integer> parentIndexStack = new LinkedList<>();

	private volatile int currentIdx;

	public RangeIterator(BTree tree, byte[] searchKey, byte[] searchMask, byte[] minValue, byte[] maxValue) {
		this.tree = tree;
		this.searchKey = searchKey;
		this.searchMask = searchMask;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.started = false;
	}

	@Override
	public byte[] next() throws IOException {
		tree.btreeLock.readLock().lock();
		try {
			if (!started) {
				started = true;
				findMinimum();
			}

			byte[] value = findNext(revisitValue.getAndSet(false));
			while (value != null) {
				if (maxValue != null && tree.comparator.compareBTreeValues(maxValue, value, 0, value.length) < 0) {
					// Reached maximum value, stop iterating
					close();
					value = null;
					break;
				} else if (searchKey != null && !ByteArrayUtil.matchesPattern(value, searchMask, searchKey)) {
					// Value doesn't match search key/mask
					value = findNext(false);
					continue;
				} else {
					// Matching value found
					break;
				}
			}

			return value;
		} finally {
			tree.btreeLock.readLock().unlock();
		}
	}

	private void findMinimum() throws IOException {
		Node nextCurrentNode = currentNode = tree.readRootNode();

		if (nextCurrentNode == null) {
			// Empty BTree
			return;
		}

		nextCurrentNode.register(this);
		currentIdx = 0;

		// Search first value >= minValue, or the left-most value in case
		// minValue is null
		while (true) {
			if (minValue != null) {
				currentIdx = nextCurrentNode.search(minValue);

				if (currentIdx >= 0) {
					// Found exact match with minimum value
					break;
				} else {
					// currentIdx indicates the first value larger than the
					// minimum value
					currentIdx = -currentIdx - 1;
				}
			}

			if (nextCurrentNode.isLeaf()) {
				break;
			} else {
				// [SES-725] must change stacks after node loading has succeeded
				Node childNode = nextCurrentNode.getChildNode(currentIdx);
				pushStacks(childNode);
				// pushStacks updates the current node
				nextCurrentNode = currentNode;
			}
		}
	}

	private byte[] findNext(boolean returnedFromRecursion) throws IOException {
		Node nextCurrentNode = currentNode;
		if (nextCurrentNode == null) {
			return null;
		}

		if (returnedFromRecursion || nextCurrentNode.isLeaf()) {
			if (currentIdx >= nextCurrentNode.getValueCount()) {
				// No more values in this node, continue with parent node
				popStacks();
				return findNext(true);
			} else {
				return nextCurrentNode.getValue(currentIdx++);
			}
		} else {
			// [SES-725] must change stacks after node loading has succeeded
			Node childNode = nextCurrentNode.getChildNode(currentIdx);
			pushStacks(childNode);
			return findNext(false);
		}
	}

	@Override
	public void set(byte[] value) {
		tree.btreeLock.readLock().lock();
		try {
			Node nextCurrentNode = currentNode;
			if (nextCurrentNode == null || currentIdx > nextCurrentNode.getValueCount()) {
				throw new IllegalStateException();
			}

			nextCurrentNode.setValue(currentIdx - 1, value);
		} finally {
			tree.btreeLock.readLock().unlock();
		}
	}

	@Override
	public synchronized void close() throws IOException {
		tree.btreeLock.readLock().lock();
		try {
			while (popStacks()) {
			}

			assert parentNodeStack.isEmpty();
			assert parentIndexStack.isEmpty();
		} finally {
			tree.btreeLock.readLock().unlock();
		}
	}

	private void pushStacks(Node newChildNode) {
		newChildNode.register(this);
		parentNodeStack.add(currentNode);
		parentIndexStack.add(currentIdx);
		currentNode = newChildNode;
		currentIdx = 0;
	}

	private boolean popStacks() throws IOException {
		Node nextCurrentNode = currentNode;
		if (nextCurrentNode == null) {
			// There's nothing to pop
			return false;
		}

		nextCurrentNode.deregister(this);
		nextCurrentNode.release();

		if (!parentNodeStack.isEmpty()) {
			currentNode = parentNodeStack.removeLast();
			currentIdx = parentIndexStack.removeLast();
			return true;
		} else {
			currentNode = null;
			currentIdx = 0;
			return false;
		}
	}

	@Override
	public boolean valueAdded(Node node, int addedIndex) {
		assert tree.btreeLock.isWriteLockedByCurrentThread();

		if (node == currentNode) {
			if (addedIndex < currentIdx) {
				currentIdx++;
			}
		} else {
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

	@Override
	public boolean valueRemoved(Node node, int removedIndex) {
		assert tree.btreeLock.isWriteLockedByCurrentThread();

		if (node == currentNode) {
			if (removedIndex < currentIdx) {
				currentIdx--;
			}
		} else {
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

	@Override
	public boolean rotatedLeft(Node node, int valueIndex, Node leftChildNode, Node rightChildNode) throws IOException {
		Node nextCurrentNode = currentNode;
		if (nextCurrentNode == node) {
			if (valueIndex == currentIdx - 1) {
				// the value that was removed had just been visited
				currentIdx = valueIndex;
				revisitValue.set(true);

				if (!node.isLeaf()) {
					pushStacks(leftChildNode);
					leftChildNode.use();
				}
			}
		} else if (nextCurrentNode == rightChildNode) {
			if (currentIdx == 0) {
				// the value that would be visited next has been moved to the
				// parent node
				popStacks();
				currentIdx = valueIndex;
				revisitValue.set(true);
			}
		} else {
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

	@Override
	public boolean rotatedRight(Node node, int valueIndex, Node leftChildNode, Node rightChildNode) throws IOException {
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

	@Override
	public boolean nodeSplit(Node node, Node newNode, int medianIdx) throws IOException {
		assert tree.btreeLock.isWriteLockedByCurrentThread();

		boolean deregister = false;

		Node nextCurrentNode = currentNode;
		if (node == nextCurrentNode) {
			if (currentIdx > medianIdx) {
				nextCurrentNode.release();
				deregister = true;

				newNode.use();
				newNode.register(this);

				currentNode = newNode;
				currentIdx -= medianIdx + 1;
			}
		} else {
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

	@Override
	public boolean nodeMergedWith(Node sourceNode, Node targetNode, int mergeIdx) throws IOException {
		assert tree.btreeLock.isWriteLockedByCurrentThread();

		boolean deregister = false;

		Node nextCurrentNode = currentNode;
		if (sourceNode == nextCurrentNode) {
			nextCurrentNode.release();
			deregister = true;

			targetNode.use();
			targetNode.register(this);

			currentNode = targetNode;
			currentIdx += mergeIdx;
		} else {
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
