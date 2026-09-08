/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.UNKNOWN;

import java.util.concurrent.atomic.AtomicReferenceArray;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Shared evaluation-local, store-first value resolution. A four-way bounded cache records dictionary hits AND misses
 * under the source's explicit read-view token. It never records an evaluation error or caches expression evaluation.
 * The compiled carrier has no resolver. Computed BIND, semantic import, source lookup and exceptional term-key
 * normalization use the same instance; none can turn a cached miss into a globally valid absence assertion.
 *
 * Keys preserve exact RDF spelling (language case, datatype, base direction and nested triples), as do the existing
 * catalog and runtime interner. Dictionary and catalog entries are immutable and published atomically; the runtime result is published once.
 * Hits are lock-free; admitted misses for one
 * bucket serialize, preventing a parallel miss storm for the same value. Eviction changes work, never identity.
 * Dictionary/catalog lookup results are kept separate from the interned result, so idOf never starts returning a
 * runtime-only id. The context owns this resolver and releases retained spellings when evaluation ends.
 */
final class NativeValueResolver implements AutoCloseable {
	static final String PROPERTY = "rdf4j.lmdb.computedValueResolutionCache.enabled";
	static final int SETS = 1024;
	static final int WAYS = 4;
	static final int MAX_ENTRIES = SETS * WAYS;

	private final NativeLmdbQuerySource store;
	private final PlanValueCatalog catalog;
	private final NativeExecutionContext context;
	private final boolean enabled;
	private volatile AtomicReferenceArray<Bucket> buckets;
	private volatile boolean closed;

	NativeValueResolver(NativeLmdbQuerySource store, PlanValueCatalog catalog, NativeExecutionContext context) {
		this.store = store;
		this.catalog = catalog;
		this.context = context;
		this.enabled = context.valueResolutionCacheEnabled;
	}

	/** Retains the lookup-only contract: a runtime-interner hit does not satisfy idOf. */
	long lookup(Value value) {
		return resolve(value, 1);
	}

	/** First resolves the store, then the current catalog, and only then mints/reuses an evaluation-local id. */
	long intern(Value value) {
		return resolve(value, 2);
	}

	long storeId(Value value) {
		return resolve(value, 0);
	}

	private long resolve(Value value, int mode) {
		ensureOpen();
		// An existing LMDB value may have an authoritative id without a decoded lexical representation. Let
		// the store validate its owner/revision first; hashing its spelling could otherwise trigger a new read.
		if (value instanceof LmdbValue) {
			return uncached(value, mode);
		}
		Object scope = enabled ? store.valueLookupScope() : null;
		if (scope == null || value == null) {
			return uncached(value, mode);
		}
		AtomicReferenceArray<Bucket> table = buckets;
		if (table == null) {
			table = initialize();
		}
		int hash = NativeValueKey.spellingHash(value);
		int index = (hash ^ (hash >>> 16)) & (SETS - 1);
		Bucket bucket = table.get(index);
		if (bucket == null) {
			Bucket created = new Bucket();
			bucket = table.compareAndExchange(index, null, created);
			if (bucket == null) {
				bucket = created;
			}
		}
		Entry found = bucket.find(value, scope, hash);
		// A hit linearizes at the token read above. A concurrent reset after that instant may affect the next
		// call; it cannot invalidate the answer that was valid in the read view observed by this call.
		if (found == null) {
			if (bucket.skipAdmission(scope)) {
				return uncached(value, mode);
			}
			found = load(bucket, value, hash);
		}
		ensureOpen();
		if (mode == 0) {
			return found.id;
		}
		if (mode == 1 || found.lookupId != UNKNOWN) {
			return found.lookupId;
		}
		long id = found.runtimeId;
		if (id == UNKNOWN) {
			id = context.internValue(value, found.key);
			// The context publishes Value-before-id and serializes equal-key identity. Concurrent first interns
			// therefore publish the same result, while lookup-only readers keep using lookupId (UNKNOWN).
			found.runtimeId = id;
		}
		return id;
	}

	private long uncached(Value value, int mode) {
		long id = store.idOf(value);
		ensureOpen();
		if (mode == 0 || id != UNKNOWN) {
			return id;
		}
		NativeValueKey key = NativeValueKey.of(value);
		id = catalog.idOfKey(key);
		return mode == 1 || id != UNKNOWN ? id : context.internValue(value, key);
	}

	private synchronized AtomicReferenceArray<Bucket> initialize() {
		ensureOpen();
		if (buckets == null) {
			buckets = new AtomicReferenceArray<>(SETS);
		}
		return buckets;
	}

	private Entry load(Bucket bucket, Value value, int hash) {
		synchronized (bucket) {
			ensureOpen();
			Object scope = store.valueLookupScope();
			Entry found = scope != null ? bucket.find(value, scope, hash) : null;
			if (found != null) {
				return found;
			}
			NativeValueKey key = NativeValueKey.of(value);
			long id = store.idOf(value); // exceptions must never be memoized as UNKNOWN
			ensureOpen();
			long lookupId = id != UNKNOWN ? id : catalog.idOfKey(key);
			Entry entry = new Entry(key, scope, id, lookupId, hash);
			if (scope != null && store.valueLookupScope() == scope) {
				bucket.put(entry);
			}
			return entry;
		}
	}

	private void ensureOpen() {
		if (closed || context.isClosed()) {
			throw new IllegalStateException("value resolver belongs to a closed evaluation");
		}
	}

	static int setIndex(NativeValueKey key) {
		int hash = key.spellingHash();
		return (hash ^ (hash >>> 16)) & (SETS - 1);
	}

	/** Metadata-only diagnostic for contract tests; never consulted by the hot path. */
	int retainedEntries() {
		AtomicReferenceArray<Bucket> table = buckets;
		if (table == null) {
			return 0;
		}
		int n = 0;
		for (int i = 0; i < table.length(); i++) {
			Bucket b = table.get(i);
			if (b != null) {
				n += (b.a == null ? 0 : 1) + (b.b == null ? 0 : 1)
						+ (b.c == null ? 0 : 1) + (b.d == null ? 0 : 1);
			}
		}
		return n;
	}

	@Override
	public void close() {
		AtomicReferenceArray<Bucket> table;
		synchronized (this) {
			if (closed) {
				return;
			}
			closed = true;
			table = buckets;
			buckets = null;
		}
		if (table != null) {
			for (int i = 0; i < table.length(); i++) {
				Bucket b = table.getAndSet(i, null);
				if (b != null) {
					synchronized (b) {
						b.a = b.b = b.c = b.d = null;
					}
				}
			}
		}
	}

	/** Identity, not arbitrary source equals/hashCode: equal-looking wrappers may expose different read views. */
	static final class Scope {
		private final NativeLmdbQuerySource source;
		private final PlanValueCatalog catalog;

		Scope(NativeLmdbQuerySource source, PlanValueCatalog catalog) {
			this.source = source;
			this.catalog = catalog;
		}

		@Override
		public int hashCode() {
			return 31 * System.identityHashCode(source) + System.identityHashCode(catalog);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof Scope s && source == s.source && catalog == s.catalog;
		}
	}

	private static final class Entry {
		final NativeValueKey key;
		final Object scope;
		final long id;
		final long lookupId;
		final int hash;
		volatile long runtimeId = UNKNOWN;

		Entry(NativeValueKey key, Object scope, long id, long lookupId, int hash) {
			this.key = key;
			this.scope = scope;
			this.id = id;
			this.lookupId = lookupId;
			this.hash = hash;
		}
	}

	private static final class Bucket {
		private volatile Entry a, b, c, d;
		/** Round-robin only on misses; no write, timestamp, or global LRU maintenance on a hit. Guarded by this. */
		private int victim;
		private int evictions;
		private volatile Object bypassScope;
		private volatile int bypassRemaining;

		/**
		 * Saturated, repeatedly evicting sets admit one new miss in 33. Existing hits always bypass this test.
		 * The approximate countdown is a work heuristic only: racing decrements can change admission, never
		 * the returned identity. Periodic admission allows a later repeating value to become hot again.
		 */
		boolean skipAdmission(Object scope) {
			int remaining = bypassRemaining;
			if (remaining > 0 && bypassScope == scope) {
				bypassRemaining = remaining - 1;
				return true;
			}
			return false;
		}

		Entry find(Value value, Object scope, int hash) {
			Entry e = a;
			if (matches(e, value, scope, hash)) {
				return e;
			}
			e = b;
			if (matches(e, value, scope, hash)) {
				return e;
			}
			e = c;
			if (matches(e, value, scope, hash)) {
				return e;
			}
			e = d;
			return matches(e, value, scope, hash) ? e : null;
		}

		private static boolean matches(Entry entry, Value value, Object scope, int hash) {
			return entry != null && entry.hash == hash && entry.scope == scope && entry.key.matches(value);
		}

		void put(Entry entry) {
			// Prefer an empty or invalidated way; a revision transition must not retain old keys unnecessarily.
			if (a == null || a.scope != entry.scope) {
				evictions = 0;
				bypassScope = null;
				bypassRemaining = 0;
				a = entry;
			} else if (b == null || b.scope != entry.scope) {
				b = entry;
			} else if (c == null || c.scope != entry.scope) {
				c = entry;
			} else if (d == null || d.scope != entry.scope) {
				d = entry;
			} else {
				switch (victim++ & (WAYS - 1)) {
				case 0: a = entry; break;
				case 1: b = entry; break;
				case 2: c = entry; break;
				default: d = entry; break;
				}
				if (evictions < 8) {
					evictions++;
				}
				if (evictions == 8) {
					bypassScope = entry.scope;
					bypassRemaining = 32;
				}
			}
		}
	}
}
