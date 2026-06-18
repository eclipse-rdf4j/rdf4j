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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.nativerdf.ConcurrentCache;

class ConcurrentNodeCache extends ConcurrentCache<Integer, Node> {

	private final Function<Integer, Node> reader;

	private static final Consumer<Node> writeNode = node -> {
		if (node.dataChanged()) {
			try {
				node.write();
			} catch (IOException exc) {
				throw new SailException("Error writing B-tree node", exc);
			}
		}
	};

	public ConcurrentNodeCache(Function<Integer, Node> reader) {
		super(0); // cleanUp, when actually run, will try to completely purge the cache (but retain currently used
		// nodes)
		this.reader = reader;
	}

	public void flush() {
		cache.values().forEach(writeNode);
	}

	public void put(Node node) {
		cache.put(node.getID(), node);
	}

	public Node readAndUse(int id) {
		return cache.compute(id, (k, v) -> {
			Node node = v == null ? reader.apply(k) : v;
			node.use();
			return node;
		});
	}

	public boolean discardEmptyUnused(Node node) {
		AtomicBoolean discarded = new AtomicBoolean(false);

		cache.computeIfPresent(node.getID(), (k, v) -> {
			if (v == node && v.getUsageCount() == 0 && v.isEmpty() && v.isLeaf()) {
				writeNode.accept(v);
				discarded.set(true);
				return null;
			}
			return v;
		});

		return discarded.get();
	}

	public void release(Node node, boolean forceSync) {
		if (forceSync) {
			cache.computeIfPresent(node.getID(), (k, v) -> {
				if (v == node) {
					writeNode.accept(v);
				}
				return v;
			});
		}
		cleanUp();
	}

	@Override
	protected boolean onEntryRemoval(Integer key) {
		Node node = cache.get(key);

		if (node == null) {
			return true;
		}

		if (node.getUsageCount() > 0) {
			return false;
		}

		if (node.isEmpty() && node.isLeaf()) {
			// Empty leaf nodes must be removed through discardEmptyUnused(Node), so the node ID can be freed
			// atomically with the cache removal by BTree.releaseNode(Node).
			return false;
		}

		writeNode.accept(node);

		return true;
	}

}
