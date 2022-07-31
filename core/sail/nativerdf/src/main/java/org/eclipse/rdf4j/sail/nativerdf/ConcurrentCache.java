/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limited-size concurrent cache. The actual cleanup to keep the size limited is done once per
 * <code>CLEANUP_INTERVAL</code> invocations of the protected method <code>cleanUp</code>. <code>cleanUp</code> method
 * is called every time by <code>put</code> The maximum size is maintained approximately. Cleanup is not done if size is
 * less than <code>capacity + CLEANUP_INTERVAL / 2</code>.
 *
 * @author Oleg Mirzov
 */
public class ConcurrentCache<K, V> {

	private static final int CLEANUP_INTERVAL = 1024;

	private static final float LOAD_FACTOR = 0.75f;

	private final int capacity;

	private volatile int cleanupTick = 0;

	protected final ConcurrentHashMap<K, V> cache;

	public ConcurrentCache(int capacity) {
		this.capacity = capacity;
		this.cache = new ConcurrentHashMap<>((int) (capacity / LOAD_FACTOR), LOAD_FACTOR);
	}

	public V get(Object key) {
		return cache.get(key);
	}

	public V put(K key, V value) {
		cleanUp();
		return cache.put(key, value);
	}

	public void clear() {
		cache.clear();
	}

	/**
	 * @param key the key of the node to test for removal and do finalization on
	 * @return true if removal is approved
	 */
	protected boolean onEntryRemoval(K key) {
		// Hook method, doing nothing by default
		return true;
	}

	protected void cleanUp() {
		// This is not thread-safe, but the worst that can happen is that we may (rarely) get slightly longer
		// cleanup intervals or run cleanUp twice
		cleanupTick++;
		if (cleanupTick <= CLEANUP_INTERVAL) {
			return;
		}

		cleanupTick %= CLEANUP_INTERVAL;

		synchronized (cache) {

			final int size = cache.size();
			if (size < capacity + CLEANUP_INTERVAL / 2) {
				return;
			}

			Iterator<K> iter = cache.keySet().iterator();

			float removeEachTh = (float) size / (size - capacity);

			for (int i = 0; iter.hasNext(); i++) {

				K key = iter.next();

				if (i % removeEachTh < 1) {
					cache.computeIfPresent(key, (k, v) -> onEntryRemoval(k) ? null : v);
				}
			}
		}
	}
}
