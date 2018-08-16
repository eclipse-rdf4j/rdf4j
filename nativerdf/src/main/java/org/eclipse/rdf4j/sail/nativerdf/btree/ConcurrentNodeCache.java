/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.rdf4j.sail.nativerdf.ConcurrentCache;

class ConcurrentNodeCache extends ConcurrentCache<Integer, Node> {

	private final Function<Integer, Node> reader;

	private static final Consumer<Node> writeNode = node -> {
		if (node.dataChanged()) {
			try {
				node.write();
			}
			catch (IOException exc) {
				throw new RuntimeException(exc);
			}
		}
	};

	public ConcurrentNodeCache(Function<Integer, Node> reader) {
		super(0); //cleanUp, when actually run, will try to completely purge the cache (but retain currently used nodes)
		this.reader = reader;
	}

	public void flush() {
		cache.forEachValue(Long.MAX_VALUE, writeNode);
	}

	public void put(Node node)
		throws IOException
	{
		cache.put(node.getID(), node);
	}

	public Node read(int id) {
		return cache.computeIfAbsent(id, reader);
	}

	public void discard(int nodeId) {
		cache.computeIfPresent(nodeId, (k, v) -> v.getUsageCount() == 0 ? null : v);
	}

	public void release(Node node) {
		cleanUp();
	}

	@Override
	protected boolean onEntryRemoval(Integer key) {
		Node node = cache.get(key);

		if (node.getUsageCount() > 0)
			return false;

		if (node != null)
			writeNode.accept(node);

		return true;
	}

}
