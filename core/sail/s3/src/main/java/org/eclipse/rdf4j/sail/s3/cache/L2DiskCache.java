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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * L2 disk-based LRU cache that mirrors S3 path structure on local filesystem. Entries are evicted in LRU order when the
 * total cache size exceeds the configured maximum. A JSON index file is persisted on close so that cache state survives
 * restarts.
 */
public class L2DiskCache implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(L2DiskCache.class);
	private static final String INDEX_FILE = "_cache_index.json";

	private final Path cacheDir;
	private final long maxSizeBytes;
	private final AtomicLong currentSizeBytes = new AtomicLong(0);
	private final ConcurrentHashMap<String, CacheEntry> index = new ConcurrentHashMap<>();
	private final ObjectMapper mapper = new ObjectMapper();

	public L2DiskCache(Path cacheDir, long maxSizeBytes) {
		this.cacheDir = cacheDir;
		this.maxSizeBytes = maxSizeBytes;
		try {
			Files.createDirectories(cacheDir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		loadIndex();
	}

	public byte[] get(String s3Key) {
		CacheEntry entry = index.get(s3Key);
		if (entry == null) {
			return null;
		}
		Path filePath = cacheDir.resolve(s3Key);
		if (!Files.exists(filePath)) {
			index.remove(s3Key);
			currentSizeBytes.addAndGet(-entry.sizeBytes);
			return null;
		}
		entry.lastAccessNanos = System.nanoTime();
		try {
			return Files.readAllBytes(filePath);
		} catch (IOException e) {
			logger.warn("Failed to read cache file: {}", filePath, e);
			return null;
		}
	}

	public void put(String s3Key, byte[] data) {
		evictIfNeeded(data.length);
		Path filePath = cacheDir.resolve(s3Key);
		try {
			Files.createDirectories(filePath.getParent());
			// Atomic write via temp file + rename
			Path tmpFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
			Files.write(tmpFile, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			Files.move(tmpFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger.warn("Failed to write cache file: {}", filePath, e);
			return;
		}

		CacheEntry prev = index.put(s3Key, new CacheEntry(data.length, System.nanoTime()));
		if (prev != null) {
			currentSizeBytes.addAndGet(data.length - prev.sizeBytes);
		} else {
			currentSizeBytes.addAndGet(data.length);
		}
	}

	public void remove(String s3Key) {
		CacheEntry entry = index.remove(s3Key);
		if (entry != null) {
			currentSizeBytes.addAndGet(-entry.sizeBytes);
			try {
				Files.deleteIfExists(cacheDir.resolve(s3Key));
			} catch (IOException e) {
				logger.warn("Failed to delete cache file: {}", s3Key, e);
			}
		}
	}

	private void evictIfNeeded(long incomingSize) {
		while (currentSizeBytes.get() + incomingSize > maxSizeBytes && !index.isEmpty()) {
			// Find LRU entry
			String lruKey = null;
			long oldestAccess = Long.MAX_VALUE;
			for (var e : index.entrySet()) {
				if (e.getValue().lastAccessNanos < oldestAccess) {
					oldestAccess = e.getValue().lastAccessNanos;
					lruKey = e.getKey();
				}
			}
			if (lruKey != null) {
				remove(lruKey);
			} else {
				break;
			}
		}
	}

	private void loadIndex() {
		Path indexPath = cacheDir.resolve(INDEX_FILE);
		if (Files.exists(indexPath)) {
			try {
				CacheIndex saved = mapper.readValue(indexPath.toFile(), CacheIndex.class);
				if (saved.entries != null) {
					long totalSize = 0;
					for (var e : saved.entries.entrySet()) {
						if (Files.exists(cacheDir.resolve(e.getKey()))) {
							index.put(e.getKey(), e.getValue());
							totalSize += e.getValue().sizeBytes;
						}
					}
					currentSizeBytes.set(totalSize);
				}
				return;
			} catch (IOException e) {
				logger.warn("Failed to load cache index, rebuilding", e);
			}
		}
		rebuildIndex();
	}

	private void rebuildIndex() {
		index.clear();
		long totalSize = 0;
		try (Stream<Path> walk = Files.walk(cacheDir)) {
			var iter = walk.filter(Files::isRegularFile)
					.filter(p -> !p.getFileName().toString().equals(INDEX_FILE))
					.filter(p -> !p.getFileName().toString().endsWith(".tmp"))
					.iterator();
			while (iter.hasNext()) {
				Path p = iter.next();
				try {
					long size = Files.size(p);
					String key = cacheDir.relativize(p).toString();
					index.put(key, new CacheEntry(size, System.nanoTime()));
					totalSize += size;
				} catch (IOException e) {
					// skip unreadable files
				}
			}
		} catch (IOException e) {
			logger.warn("Failed to walk cache directory", e);
		}
		currentSizeBytes.set(totalSize);
	}

	public void persistIndex() {
		try {
			CacheIndex ci = new CacheIndex();
			ci.entries = new ConcurrentHashMap<>(index);
			Path indexPath = cacheDir.resolve(INDEX_FILE);
			mapper.writeValue(indexPath.toFile(), ci);
		} catch (IOException e) {
			logger.warn("Failed to persist cache index", e);
		}
	}

	@Override
	public void close() {
		persistIndex();
	}

	static class CacheEntry {
		@JsonProperty("sizeBytes")
		public long sizeBytes;

		@JsonProperty("lastAccessNanos")
		public long lastAccessNanos;

		public CacheEntry() {
			// for Jackson deserialization
		}

		CacheEntry(long sizeBytes, long lastAccessNanos) {
			this.sizeBytes = sizeBytes;
			this.lastAccessNanos = lastAccessNanos;
		}
	}

	static class CacheIndex {
		@JsonProperty("entries")
		public ConcurrentHashMap<String, CacheEntry> entries;
	}
}
