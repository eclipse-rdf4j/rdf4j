/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.resultcache;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T>
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class LRUResultCache<T> implements ResultCache<Integer, T> {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final Map<Integer, Entry<T>> cache;
	private final AtomicBoolean dirty = new AtomicBoolean(false);
	private final Map<Thread, Boolean> bypassInThread = Collections.synchronizedMap(new WeakHashMap<>());
	private final Duration entryLifetime;

	public LRUResultCache(ResultCacheProperties properties) {
		this.entryLifetime = properties.getEntryLifetime();
		this.cache = Collections.synchronizedMap(
				new LRUMap<>(properties.getMaxSize(), properties.getInitialSize()));
	}

	@Override
	public T get(Integer key) {
		debug("obtaining cached result for key {} from cache {}", key, hashCode());
		Objects.requireNonNull(key);
		if (dirty.get()) {
			debug("cache is dirty");
			clearCachedResults();
			debug("returning null");
			return null;
		}
		if (isBypass()) {
			debug("bypassing cache, returning null");
			return null;
		}
		Entry<T> entry = cache.get(key);
		if (entry == null) {
			debug("nothing found in cache, returning null");
			return null;
		}
		if (entry.isExpired()) {
			cache.remove(key);
			debug("cached object is expired, returning null");
			return null;
		}
		debug("returning cached object");
		return entry.getCachedObject();
	}

	private void debug(String message, Object... args) {
		if (logger.isDebugEnabled()) {
			logger.debug(message, args);
		}
	}

	private boolean isBypass() {
		return bypassInThread.containsKey(Thread.currentThread());
	}

	@Override
	public void put(Integer key, T cachedObject) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(cachedObject);
		debug("about to put object {} into cache {}", key, hashCode());
		if (isBypass()) {
			debug("bypassing cache, not caching object");
			return;
		}
		if (dirty.get()) {
			debug("cache is dirty");
			clearCachedResults();
		}
		debug("putting object into cache");
		cache.put(key, new Entry<>(cachedObject));
	}

	@Override
	public void markDirty() {
		debug("marking dirty: cache {}", hashCode());
		this.dirty.set(true);
	}

	@Override
	public synchronized void clearCachedResults() {
		debug("clearing cache {}", hashCode());
		if (dirty.get()) {
			cache.clear();
			bypassInThread.clear();
			dirty.set(false);
		}
	}

	@Override
	public void bypassForCurrentThread() {
		bypassInThread.put(Thread.currentThread(), true);
	}

	private class Entry<E> {
		E cachedObject;
		Instant createdAtTimestamp = Instant.now();

		public Entry(E cachedObject) {
			this.cachedObject = cachedObject;
		}

		public E getCachedObject() {
			return cachedObject;
		}

		public boolean isExpired() {
			return createdAtTimestamp.plus(entryLifetime).isBefore(Instant.now());
		}
	}
}
