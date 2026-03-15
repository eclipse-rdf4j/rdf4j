/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.cache;

import java.io.Closeable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * L1 in-memory cache backed by Caffeine. Caches full file bytes keyed by S3 object key, weighted by byte array length.
 */
public class L1HeapCache implements Closeable {

	private final Cache<String, byte[]> fileCache;

	public L1HeapCache(long maxWeightBytes) {
		this.fileCache = Caffeine.newBuilder()
				.maximumWeight(maxWeightBytes)
				.weigher((String key, byte[] value) -> value.length)
				.recordStats()
				.build();
	}

	public byte[] get(String s3Key) {
		return fileCache.getIfPresent(s3Key);
	}

	public void put(String s3Key, byte[] data) {
		fileCache.put(s3Key, data);
	}

	public void invalidate(String s3Key) {
		fileCache.invalidate(s3Key);
	}

	public void invalidateAll() {
		fileCache.invalidateAll();
	}

	@Override
	public void close() {
		fileCache.invalidateAll();
		fileCache.cleanUp();
	}
}
