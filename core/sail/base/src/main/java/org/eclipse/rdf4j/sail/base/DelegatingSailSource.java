/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A wrapper around an {@link SailSource} that can suppress the call to {@link #close()}. This is useful when the a
 * shared branch is sometimes to be used and other times a dedicated branch is to be used.
 *
 * @author James Leigh
 */
class DelegatingSailSource implements SailSource {

	private final SailSource delegate;

	private final boolean releasing;

	/**
	 * Wraps this {@link SailSource}, delegating all calls to it unless <code>closing</code> is false, in which case
	 * {@link #close()} will not be delegated.
	 *
	 * @param delegate
	 * @param closing  if {@link #close()} should be delegated
	 */
	public DelegatingSailSource(SailSource delegate, boolean closing) {
		assert delegate != null;
		this.delegate = delegate;
		this.releasing = closing;
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public void close() throws SailException {
		if (releasing) {
			delegate.close();
		}
	}

	@Override
	public SailSource fork() {
		return delegate.fork();
	}

	@Override
	public void prepare() throws SailException {
		delegate.prepare();
	}

	@Override
	public void flush() throws SailException {
		delegate.flush();
	}

	@Override
	public SailSink sink(IsolationLevel level) throws SailException {
		return delegate.sink(level);
	}

	@Override
	public SailDataset dataset(IsolationLevel level) throws SailException {
		return delegate.dataset(level);
	}
}
