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
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.rdf4j.sail.s3.storage.ObjectStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified three-tier cache facade: L1 (heap) -> L2 (disk) -> L3 (S3 / ObjectStore). On a cache miss at a given tier,
 * data is fetched from the next tier down and promoted into all higher tiers.
 */
public class TieredCache implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(TieredCache.class);

	private final L1HeapCache l1;
	private final L2DiskCache l2; // nullable when no disk cache path is configured
	private final ObjectStore objectStore;

	public TieredCache(long heapCacheSize, Path diskCachePath, long diskCacheSize, ObjectStore objectStore) {
		this.l1 = new L1HeapCache(heapCacheSize);
		this.l2 = diskCachePath != null ? new L2DiskCache(diskCachePath, diskCacheSize) : null;
		this.objectStore = objectStore;
	}

	/**
	 * Get file bytes for the given S3 key. Checks L1 (heap) first, then L2 (disk), then L3 (S3), promoting data into
	 * higher tiers on a miss.
	 */
	public byte[] get(String s3Key) {
		// L1
		byte[] data = l1.get(s3Key);
		if (data != null) {
			return data;
		}

		// L2
		if (l2 != null) {
			data = l2.get(s3Key);
			if (data != null) {
				l1.put(s3Key, data); // promote to L1
				return data;
			}
		}

		// L3 (S3)
		data = objectStore.get(s3Key);
		if (data != null) {
			l1.put(s3Key, data); // populate L1
			if (l2 != null) {
				l2.put(s3Key, data); // populate L2
			}
		}
		return data;
	}

	/**
	 * Write-through: populate L1 and L2 immediately (e.g., on flush). Does NOT write to S3; the caller handles that
	 * separately.
	 */
	public void writeThrough(String s3Key, byte[] data) {
		l1.put(s3Key, data);
		if (l2 != null) {
			l2.put(s3Key, data);
		}
	}

	/**
	 * Invalidate a key from all cache tiers (e.g., after compaction deletes a file).
	 */
	public void invalidate(String s3Key) {
		l1.invalidate(s3Key);
		if (l2 != null) {
			l2.remove(s3Key);
		}
	}

	@Override
	public void close() throws IOException {
		l1.close();
		if (l2 != null) {
			l2.close();
		}
	}
}
