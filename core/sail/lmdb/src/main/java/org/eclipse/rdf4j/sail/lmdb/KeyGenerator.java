/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class KeyGenerator implements AutoCloseable {

	static final int CACHE_THRESHOLD = 5;
	static final int WINDOW_SIZE = 100;

	@FunctionalInterface
	interface BufferSupplier {
		KeyBuffer acquire();
	}

	static final class KeyBuffer {
		private final ByteBuffer buffer;
		private final boolean pooled;
		private final boolean cached;

		KeyBuffer(ByteBuffer buffer, boolean pooled) {
			this(buffer, pooled, false);
		}

		KeyBuffer(ByteBuffer buffer, boolean pooled, boolean cached) {
			this.buffer = Objects.requireNonNull(buffer, "buffer");
			this.pooled = pooled;
			this.cached = cached;
		}

		ByteBuffer buffer() {
			return buffer;
		}

		boolean pooled() {
			return pooled;
		}

		boolean cached() {
			return cached;
		}
	}

	private final IndexKeyWriters.KeyWriter keyWriter;
	private final ConcurrentHashMap<KeySignature, CacheEntry> cache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<KeySignature, AtomicInteger> frequencies = new ConcurrentHashMap<>();
	private final AtomicInteger windowCalls = new AtomicInteger();

	KeyGenerator(IndexKeyWriters.KeyWriter keyWriter) {
		this.keyWriter = Objects.requireNonNull(keyWriter, "keyWriter");
	}

	KeyBuffer keyFor(long subj, long pred, long obj, long context, BufferSupplier supplier) {
		KeySignature signature = new KeySignature(subj, pred, obj, context);
		CacheEntry entry = cache.get(signature);
		if (entry != null) {
			return new KeyBuffer(entry.duplicate(), false, true);
		}

		KeyBuffer supplied = supplier.acquire();
		ByteBuffer buffer = supplied.buffer();
		buffer.clear();
		keyWriter.write(buffer, subj, pred, obj, context);
		buffer.flip();

		maybePromote(signature, supplied, buffer);

		return new KeyBuffer(buffer, supplied.pooled(), false);
	}

	private void maybePromote(KeySignature signature, KeyBuffer supplied, ByteBuffer buffer) {
		int usage = frequencies.computeIfAbsent(signature, key -> new AtomicInteger()).incrementAndGet();
		int callCount = windowCalls.incrementAndGet();
		if (callCount >= WINDOW_SIZE) {
			if (windowCalls.compareAndSet(callCount, 0)) {
				frequencies.clear();
			}
		}
		if (usage > CACHE_THRESHOLD) {
			cache.computeIfAbsent(signature, key -> createEntry(buffer, supplied.pooled()));
		}
	}

	private CacheEntry createEntry(ByteBuffer buffer, boolean pooled) {
		ByteBuffer duplicate = buffer.duplicate();
		ByteBuffer copy = ByteBuffer.allocateDirect(duplicate.remaining());
		copy.put(duplicate);
		copy.flip();
		buffer.rewind();
		return new CacheEntry(copy);
	}

	@Override
	public void close() {
		cache.clear();
		frequencies.clear();
		windowCalls.set(0);
	}

	private static final class CacheEntry {
		private final ByteBuffer stored;

		CacheEntry(ByteBuffer stored) {
			this.stored = stored;
		}

		ByteBuffer duplicate() {
			ByteBuffer duplicate = stored.duplicate();
			duplicate.position(0);
			duplicate.limit(stored.limit());
			return duplicate;
		}

		void close() {
		}
	}

	private static final class KeySignature {
		private final long subj;
		private final long pred;
		private final long obj;
		private final long context;
		private final int hash;

		KeySignature(long subj, long pred, long obj, long context) {
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.context = context;
			this.hash = Objects.hash(subj, pred, obj, context);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof KeySignature)) {
				return false;
			}
			KeySignature other = (KeySignature) obj;
			return subj == other.subj && pred == other.pred && this.obj == other.obj && context == other.context;
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}
}
