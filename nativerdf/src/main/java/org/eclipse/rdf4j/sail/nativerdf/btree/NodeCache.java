/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

class NodeCache {

	@FunctionalInterface
	interface NodeReader {

		Node apply(Integer nodeId)
			throws IOException;
	}

	/**
	 * The size of the node cache. Note that this is not a hard limit. All nodes that are actively used are
	 * always cached. Also, a minimum of {@link NODE_CACHE_SIZE} nodes of unused nodes is kept in the cache.
	 */
	static final int NODE_CACHE_SIZE = 10;

	/**
	 * The minimum number of most recently released nodes to keep in the cache.
	 */
	static final int MIN_MRU_CACHE_SIZE = 4;

	private final NodeReader reader;

	/**
	 * Map containing cached nodes, indexed by their ID.
	 */
	private final Map<Integer, Node> nodeCache = new HashMap<Integer, Node>(NODE_CACHE_SIZE);

	/**
	 * Map of cached nodes that are no longer "in use", sorted from least recently used to most recently used.
	 * This collection is used to remove nodes from the cache when it is full. Note: needs to be synchronized
	 * through nodeCache (data strucures should prob be merged in a NodeCache class)
	 */
	private final Map<Integer, Node> mruNodes = new LinkedHashMap<Integer, Node>(NODE_CACHE_SIZE);

	public NodeCache(NodeReader reader) {
		this.reader = reader;
	}

	public void flush()
		throws IOException
	{
		synchronized (this) {
			for (Node node : nodeCache.values()) {
				if (node.dataChanged()) {
					node.write();
				}
			}
		}
	}

	public void clear() {
		synchronized (this) {
			try {
				nodeCache.clear();
			}
			finally {
				mruNodes.clear();
			}
		}
	}

	public void put(Node node)
		throws IOException
	{
		synchronized (this) {
			expelNodeFromCacheIfNeeded();
			nodeCache.put(node.getID(), node);
		}
	}

	public Node read(int id)
		throws IOException
	{
		synchronized (this) {
			Node inCache = nodeCache.get(id);
			if (inCache != null) {
				if (inCache.getUsageCount() == 0)
					mruNodes.remove(id);
				return inCache;
			}

			Node haveRead = reader.apply(id);

			expelNodeFromCacheIfNeeded();
			nodeCache.put(id, haveRead);
			return haveRead;
		}
	}

	public void discard(int nodeId) {
		synchronized (this) {
			nodeCache.remove(nodeId);
		}
	}

	public void release(Node node)
		throws IOException
	{
		synchronized (this) {
			mruNodes.put(node.getID(), node);
			expelNodeFromCacheIfNeeded();
		}
	}

	/**
	 * Tries to expel the least recently used node from the cache.
	 */
	private void expelNodeFromCacheIfNeeded()
		throws IOException
	{
		if (nodeCache.size() > NODE_CACHE_SIZE && mruNodes.size() > MIN_MRU_CACHE_SIZE) {
			Iterator<Node> iter = mruNodes.values().iterator();
			Node lruNode = iter.next();

			if (lruNode.dataChanged()) {
				lruNode.write();
			}
			iter.remove();
			nodeCache.remove(lruNode.getID());
		}
	}

}
