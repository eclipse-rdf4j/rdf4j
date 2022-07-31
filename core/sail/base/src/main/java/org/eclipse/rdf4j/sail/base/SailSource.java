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
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A persistent yet mutable source or container of RDF graphs. In which its state can change over time. The life cycle
 * follows that of a store and/or transactions. The {@link SailClosable#close()} is only applicable to results from
 * {@link #fork()}, not to the backing {@link SailSource} itself.
 *
 * @author James Leigh
 */
public interface SailSource extends SailClosable {

	/**
	 * Creates a new branch of this source. When it's {@link #flush()} is called the changes are applied to this backing
	 * source.
	 *
	 * @return a branched {@link SailSource}.
	 */
	SailSource fork();

	/**
	 * Create a {@link SailSink} that when when its {@link #flush()} is called, the changes are applied to this source.
	 *
	 * @param level If this level is compatible with {@link IsolationLevels#SERIALIZABLE} then a
	 *              {@link SailSink#prepare()} can throw a {@link SailConflictException}.
	 * @return Newly created {@link SailSink}
	 * @throws SailException
	 */
	SailSink sink(IsolationLevel level) throws SailException;

	/**
	 * Create an observable {@link SailDataset} of the current state of this {@link SailSource}. Repeatedly calling with
	 * methods with {@link IsolationLevels#SNAPSHOT} (or higher) isolation levels will result in {@link SailDataset}s
	 * that are all derived from the same state of the backing {@link SailSource} (if applicable), that is the only
	 * difference between the states of the {@link SailDataset} will be from changes using this
	 * {@link #sink(IsolationLevel)}.
	 *
	 * @param level If this is compatible with {@link IsolationLevels#SNAPSHOT_READ} the resulting {@link SailDataset}
	 *              will observe a single state of this {@link SailSource}.
	 * @return an {@link SailDataset} of the current state
	 * @throws SailException
	 */
	SailDataset dataset(IsolationLevel level) throws SailException;

	/**
	 * Check the consistency of this branch and throws a {@link SailConflictException} if {@link #flush()}ing this
	 * branch would cause the backing {@link SailSource} to be inconsistent, if applicable. If this is the final backing
	 * {@link SailSource} calling this method has no effect.
	 *
	 * @throws SailException
	 */
	void prepare() throws SailException;

	/**
	 * Apply all the changes to this branch to the backing {@link SailSource}, if applicable. If this is the final
	 * backing {@link SailSource} calling this method has no effect.
	 *
	 * @throws SailException
	 */
	void flush() throws SailException;

}
