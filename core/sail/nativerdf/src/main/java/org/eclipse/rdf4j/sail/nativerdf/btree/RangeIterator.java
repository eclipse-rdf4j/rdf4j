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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;

class RangeIterator implements RecordIterator, NodeListener {

	private final BTree tree;

	private final byte[] searchKey;

	private final byte[] searchMask;

	private final byte[] minValue;

	private final byte[] maxValue;

	private volatile boolean started;

	private volatile Node currentNode;

	private final AtomicBoolean revisitValue = new AtomicBoolean();

	/**
	 * Tracks parent nodes, child indices and handles for {@link #currentNode}.
	 */
	private final Deque<StackFrame> parentStack = new ArrayDeque<>();

	private NodeListenerHandle currentNodeHandle;

	private volatile int currentIdx;

	private volatile boolean closed = false;

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

	private void findMinimum() {
		Node nextCurrentNode = currentNode = tree.readRootNode();

		if (nextCurrentNode == null) {
			// Empty BTree
			return;
		}

		currentNodeHandle = nextCurrentNode.register(this);
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
	public void close() throws IOException {
		if (!closed) {
			synchronized (this) {
				if (!closed) {
					closed = true;
					tree.btreeLock.readLock().lock();
					try {
						clearTraversalState();
						assert parentStack.isEmpty();
					} finally {
						tree.btreeLock.readLock().unlock();
					}
				}
			}
		}
	}

	private void pushStacks(Node newChildNode) {
		NodeListenerHandle childHandle = newChildNode.register(this);
		parentStack.addLast(new StackFrame(currentNode, currentIdx, currentNodeHandle));
		currentNode = newChildNode;
		currentNodeHandle = childHandle;
		currentIdx = 0;
	}

	private synchronized boolean popStacks() throws IOException {
		if (currentNode == null && parentStack.isEmpty()) {
			return false;
		}

		releaseCurrentFrame();
		StackFrame previous = parentStack.pollLast();
		if (previous != null) {
			currentNode = previous.node;
			currentIdx = previous.childIndex;
			currentNodeHandle = previous.handle;
			return true;
		}

		currentNode = null;
		currentIdx = 0;
		currentNodeHandle = null;
		return false;
	}

	private void clearTraversalState() throws IOException {
		while (currentNode != null || !parentStack.isEmpty()) {
			releaseCurrentFrame();
			StackFrame previous = parentStack.pollLast();
			if (previous == null) {
				currentNode = null;
				currentIdx = 0;
				currentNodeHandle = null;
				break;
			}
			currentNode = previous.node;
			currentIdx = previous.childIndex;
			currentNodeHandle = previous.handle;
		}
	}

	private void releaseCurrentFrame() throws IOException {
		Node nextCurrentNode = currentNode;
		if (nextCurrentNode != null) {
			if (currentNodeHandle != null) {
				currentNodeHandle.remove();
				currentNodeHandle = null;
			}
			nextCurrentNode.release();
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
			for (StackFrame frame : parentStack) {
				if (node == frame.node) {
					if (addedIndex < frame.childIndex) {
						frame.childIndex++;
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
			for (StackFrame frame : parentStack) {
				if (node == frame.node) {
					if (removedIndex < frame.childIndex) {
						frame.childIndex--;
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
			for (StackFrame frame : parentStack) {
				if (frame.node == rightChildNode) {
					int stackIdx = frame.childIndex;

					if (stackIdx == 0) {
						// this node is no longer the parent, replace with left sibling
						NodeListenerHandle replacedHandle = frame.handle;
						if (replacedHandle != null) {
							replacedHandle.remove();
						}
						rightChildNode.release();

						leftChildNode.use();
						NodeListenerHandle leftHandle = leftChildNode.register(this);

						frame.node = leftChildNode;
						frame.handle = leftHandle;
						frame.childIndex = leftChildNode.getValueCount();
					}

					break;
				}
			}
		}

		return false;
	}

	@Override
	public boolean rotatedRight(Node node, int valueIndex, Node leftChildNode, Node rightChildNode) throws IOException {
		for (StackFrame frame : parentStack) {
			if (frame.node == leftChildNode) {
				int stackIdx = frame.childIndex;

				if (stackIdx == leftChildNode.getValueCount()) {
					// this node is no longer the parent, replace with right sibling
					NodeListenerHandle replacedHandle = frame.handle;
					if (replacedHandle != null) {
						replacedHandle.remove();
					}
					leftChildNode.release();

					rightChildNode.use();
					NodeListenerHandle rightHandle = rightChildNode.register(this);

					frame.node = rightChildNode;
					frame.handle = rightHandle;
					frame.childIndex = 0;
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
				if (currentNodeHandle != null) {
					currentNodeHandle.remove();
					currentNodeHandle = null;
				}
				nextCurrentNode.release();
				deregister = true;

				newNode.use();
				NodeListenerHandle newHandle = newNode.register(this);

				currentNode = newNode;
				currentNodeHandle = newHandle;
				currentIdx -= medianIdx + 1;
			}
		} else {
			for (StackFrame frame : parentStack) {
				if (node == frame.node) {
					int parentIdx = frame.childIndex;

					if (parentIdx > medianIdx) {
						NodeListenerHandle replacedHandle = frame.handle;
						if (replacedHandle != null) {
							replacedHandle.remove();
						}
						Node parentNode = frame.node;
						parentNode.release();
						deregister = true;

						newNode.use();
						NodeListenerHandle newHandle = newNode.register(this);

						frame.node = newNode;
						frame.handle = newHandle;
						frame.childIndex = parentIdx - medianIdx - 1;
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
			if (currentNodeHandle != null) {
				currentNodeHandle.remove();
				currentNodeHandle = null;
			}
			nextCurrentNode.release();
			deregister = true;

			targetNode.use();
			NodeListenerHandle newHandle = targetNode.register(this);

			currentNode = targetNode;
			currentNodeHandle = newHandle;
			currentIdx += mergeIdx;
		} else {
			for (StackFrame frame : parentStack) {
				if (sourceNode == frame.node) {
					NodeListenerHandle replacedHandle = frame.handle;
					if (replacedHandle != null) {
						replacedHandle.remove();
					}
					Node parentNode = frame.node;
					parentNode.release();
					deregister = true;

					targetNode.use();
					NodeListenerHandle newHandle = targetNode.register(this);

					frame.node = targetNode;
					frame.handle = newHandle;
					frame.childIndex = mergeIdx + frame.childIndex;

					break;
				}
			}
		}

		return deregister;
	}

	@Override
	public String toString() {
		return "RangeIterator{" +
				"tree=" + tree +
				'}';
	}

	private static final class StackFrame {
		Node node;
		int childIndex;
		NodeListenerHandle handle;

		StackFrame(Node node, int childIndex, NodeListenerHandle handle) {
			this.node = node;
			this.childIndex = childIndex;
			this.handle = handle;
		}
	}
}
