/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility subclass of {@link LinkedHashMap} the makes it a fixed-size LRU
 * cache.
 * 
 * @author Arjohn Kampman
 */
class LRUCache<K, V> extends LinkedHashMap<K, V> {

	private static final long serialVersionUID = -8180282377977820910L;

	private final int capacity;

	public LRUCache(int capacity) {
		this(capacity, 0.75f);
	}

	public LRUCache(int capacity, float loadFactor) {
		super((int)(capacity / loadFactor), loadFactor, true);
		this.capacity = capacity;
	}

	public int getCapacity() {
		return capacity;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > capacity;
	}

	@Override
	public synchronized V get(Object key) {
		return super.get(key);
	}

	@Override
	public synchronized V put(K key, V value) {
		return super.put(key, value);
	}

	@Override
	public synchronized void clear() {
		super.clear();
	}
}
