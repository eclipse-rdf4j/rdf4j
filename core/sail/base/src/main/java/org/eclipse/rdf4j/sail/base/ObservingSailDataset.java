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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A {@link IsolationLevels#SERIALIZABLE} {@link SailDataset} that tracks the observed statement patterns to an
 * {@link SailSink#observe(Resource, IRI, Value, Resource...)} to check consistency.
 *
 * @author James Leigh
 */
class ObservingSailDataset extends DelegatingSailDataset {

	/**
	 * The {@link SailSink} that is tracking the statement patterns.
	 */
	private final SailSink observer;

	/**
	 * Creates a {@link IsolationLevels#SERIALIZABLE} {@link SailDataset} that tracks consistency.
	 *
	 * @param delegate to be {@link SailDataset#close()} when this {@link SailDataset} is closed.
	 * @param observer to be {@link SailSink#flush()} and {@link SailSink#close()} when this {@link SailDataset} is
	 *                 closed.
	 */
	public ObservingSailDataset(SailDataset delegate, SailSink observer) {
		super(delegate);
		this.observer = observer;
	}

	@Override
	public void close() throws SailException {
		try {
			super.close();
		} finally {
			try {
				// flush observer regardless of consistency
				observer.flush();
			} finally {
				observer.close();
			}
		}
	}

	@Override
	public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
		observer.observe(null, null, null);
		return super.getContextIDs();
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException {
		observer.observe(subj, pred, obj, contexts);
		return super.getStatements(subj, pred, obj, contexts);
	}

}
