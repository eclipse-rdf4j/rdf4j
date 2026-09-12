/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.NULL_CONTEXT_ID;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.UNKNOWN;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.rdf4j.model.Value;

/**
 * Terminal-only RDF term keys over the evaluation's runtime table. Interning, output payload, ownership, unresolved
 * membership, and the canonical key share one publication record. The common path creates no spelling key, boxed ID,
 * representative object, map entry, or second canonicalization cache. Upstream probes retain the ordinary authority.
 *
 * An unexpected stored/plan key is normalized defensively by reading its Value once (never by value-to-ID lookup).
 * Only such exceptional IDs need a separate memoization map. This path cannot run for proven all-generated keys.
 */
final class NativeGeneratedKeyAuthority implements NativeTermAuthority, AutoCloseable {
	private final NativeTermAuthority delegate;
	private final NativeExecutionContext context;
	private final NativeRuntimeValueTable values;
	private volatile ConcurrentHashMap<Long, Long> exceptionalKeys;
	private volatile boolean closed;

	NativeGeneratedKeyAuthority(NativeTermAuthority delegate, NativeExecutionContext context) {
		this.delegate = delegate;
		this.context = context;
		this.values = context.runtimeValueTable();
	}

	long intern(Value value) {
		checkOpen();
		return values.intern(value, true);
	}

	long internString(String label) {
		checkOpen();
		return values.internString(label);
	}

	@Override
	public Object token() {
		return this;
	}

	@Override
	public NativeIdKind kind(long id) {
		checkOpen();
		return values.contains(id) ? NativeIdKind.RUNTIME : delegate.kind(id);
	}

	@Override
	public long idOfOrIntern(Value value) {
		checkOpen();
		return delegate.idOfOrIntern(value);
	}

	@Override
	public Value valueOf(long id) {
		checkOpen();
		Value value = values.valueOf(id);
		return value != null ? value : delegate.valueOf(id);
	}

	@Override
	public NativeTermRef termRef(long id) {
		Value value = valueOf(id);
		return value == null ? null : NativeTermRef.authoritative(token(), id, value);
	}

	@Override
	public boolean sameRdfTerm(long left, long right) {
		checkOpen();
		return left == right || canonicalTermKey(left) == canonicalTermKey(right);
	}

	@Override
	public long rdfTermHash(long id) {
		Value value = valueOf(id);
		return value == null ? id : value.hashCode();
	}

	@Override
	public boolean supportsCanonicalTermKeys() {
		return true;
	}

	@Override
	public long canonicalTermKey(long id) {
		checkOpen();
		if (id == UNKNOWN || id == NULL_CONTEXT_ID) {
			return id;
		}
		long key = values.key(id);
		return key != 0L ? key : exceptionalKey(id);
	}

	private long exceptionalKey(long id) {
		ConcurrentHashMap<Long, Long> cache = exceptionalKeys;
		Long key = cache == null ? null : cache.get(id);
		return key == null ? normalize(id) : key;
	}

	private synchronized long normalize(long id) {
		checkOpen();
		if (exceptionalKeys == null) {
			exceptionalKeys = new ConcurrentHashMap<>();
		}
		Long known = exceptionalKeys.get(id);
		if (known != null) {
			return known;
		}
		Value value = delegate.valueOf(id);
		if (value == null) {
			throw new IllegalStateException("Unresolvable generated key ID: " + id);
		}
		long key = values.key(values.intern(value, true));
		exceptionalKeys.put(id, key);
		return key;
	}

	@Override
	public long termHashKey(long id) {
		return canonicalTermKey(id);
	}

	@Override
	public TermProbeDisposition probeDisposition(long id) {
		checkOpen();
		return delegate.probeDisposition(id);
	}

	@Override
	public long importId(NativeTermAuthority source, long id) {
		checkOpen();
		return source.token() == token() || source.token() == delegate.token() ? id : delegate.importId(source, id);
	}

	/** Terminal-to-terminal import, never a storage probe. */
	long importKey(NativeTermAuthority source, long id) {
		checkOpen();
		if (source.token() == token() || source.token() == delegate.token() || id == UNKNOWN || id == NULL_CONTEXT_ID) {
			return id;
		}
		return intern(source.valueOf(id));
	}

	private void checkOpen() {
		if (closed || context.isClosed()) {
			throw new IllegalStateException("generated keys belong to a closed evaluation");
		}
	}

	@Override
	public synchronized void close() {
		closed = true;
		exceptionalKeys = null;
		// The evaluation, not an individual terminal, owns the shared payload table.
	}
}
