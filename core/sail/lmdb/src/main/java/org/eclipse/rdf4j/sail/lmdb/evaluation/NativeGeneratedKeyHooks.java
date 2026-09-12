/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelHooks;

/** Terminal hash-table semantics; the enclosing kernel retains its original hooks for expressions and probes. */
final class NativeGeneratedKeyHooks implements KernelHooks {
	private final KernelHooks delegate;
	private final NativeGeneratedKeyAuthority authority;

	NativeGeneratedKeyHooks(KernelHooks delegate, NativeGeneratedKeyAuthority authority) {
		this.delegate = delegate;
		this.authority = authority;
	}

	@Override
	public boolean sameRdfTerm(long left, long right) {
		return authority.sameRdfTerm(left, right);
	}

	@Override
	public long rdfTermHash(long id) {
		return authority.rdfTermHash(id);
	}

	@Override
	public long termHashKey(long id) {
		return authority.canonicalTermKey(id);
	}

	@Override
	public boolean supportsCanonicalTermKeys() {
		return true;
	}

	@Override
	public long canonicalTermKey(long id) {
		return authority.canonicalTermKey(id);
	}

	@Override
	public long importRdfTerm(KernelHooks source, long id) {
		if (source instanceof NativeGeneratedKeyHooks other) {
			return authority.importKey(other.authority, id);
		}
		return delegate.importRdfTerm(source, id);
	}

	@Override
	public boolean testFilter(int filter, long a, long b, long c) {
		return delegate.testFilter(filter, a, b, c);
	}

	@Override
	public long computeBind(int bind, long a, long b) {
		return delegate.computeBind(bind, a, b);
	}

	@Override
	public int compareValues(long left, long right) {
		return delegate.compareValues(left, right);
	}

	@Override
	public boolean isNumeric(long id) {
		return delegate.isNumeric(id);
	}

	@Override
	public double doubleValue(long id) {
		return delegate.doubleValue(id);
	}

	@Override
	public void accumulateNumeric(int aggregate, int group, long value) {
		delegate.accumulateNumeric(aggregate, group, value);
	}
}
