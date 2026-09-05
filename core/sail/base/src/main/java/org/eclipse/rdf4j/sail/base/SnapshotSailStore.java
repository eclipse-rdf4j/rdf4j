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

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A {@link SailStore} wrapper that branches the backing {@link SailSource}s to provide concurrent
 * {@link IsolationLevels#SNAPSHOT_READ} isolation and higher.
 *
 * @author James Leigh
 */
public class SnapshotSailStore implements SailStore {

	/**
	 * The underlying {@link SailStore}.
	 */
	private final SailStore backingStore;

	/**
	 * {@link SailSource} of {@link SailStore#getExplicitSailSource()} .
	 */
	private final SailSourceBranch explicitAutoFlush;

	/**
	 * {@link SailSource} of {@link SailStore#getInferredSailSource()} .
	 */
	private final SailSourceBranch inferredAutoFlush;

	/**
	 * Wraps an {@link SailStore}, tracking changes in {@link ModelFactory} instances.
	 *
	 * @param backingStore
	 * @param modelFactory
	 */
	public SnapshotSailStore(SailStore backingStore, ModelFactory modelFactory) {
		this.backingStore = backingStore;
		explicitAutoFlush = new SailSourceBranch(backingStore.getExplicitSailSource(), modelFactory, true);
		inferredAutoFlush = new SailSourceBranch(backingStore.getInferredSailSource(), modelFactory, true);
	}

	@Override
	public void close() throws SailException {
		try {
			try {
				explicitAutoFlush.flush();
				inferredAutoFlush.flush();
			} finally {
				explicitAutoFlush.close();
				inferredAutoFlush.close();
			}
		} finally {
			backingStore.close();
		}
	}

	@Override
	public ValueFactory getValueFactory() {
		return backingStore.getValueFactory();
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return backingStore.getEvaluationStatistics();
	}

	@Override
	public SailSource getExplicitSailSource() {
		return explicitAutoFlush;
	}

	/**
	 * Whether committed changes are still buffered in the auto-flush branch(es) — because a dataset derived from a
	 * branch is still open — and have not yet reached the backing store. A read that bypasses the branches and goes to
	 * the backing store directly is stale while this is {@code true}.
	 *
	 * @param includeInferred whether to also consider the inferred branch
	 */
	@InternalUseOnly
	public boolean hasUnflushedChanges(boolean includeInferred) {
		return explicitAutoFlush.isChanged() || (includeInferred && inferredAutoFlush.isChanged());
	}

	@Override
	public SailSource getInferredSailSource() {
		return inferredAutoFlush;
	}

}
