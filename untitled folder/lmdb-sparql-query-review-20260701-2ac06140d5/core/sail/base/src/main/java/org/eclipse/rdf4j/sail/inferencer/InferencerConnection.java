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
package org.eclipse.rdf4j.sail.inferencer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;

/**
 * An extension of the {@link SailConnection} interface offering methods that can be used by inferencers to store and
 * remove inferred statements.
 */
public interface InferencerConnection extends NotifyingSailConnection {

	/**
	 * Adds an inferred statement to a specific context.
	 *
	 * @param subj     The subject of the statement to add.
	 * @param pred     The predicate of the statement to add.
	 * @param obj      The object of the statement to add.
	 * @param contexts The context(s) to add the statement to. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @throws SailException         If the statement could not be added.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	// FIXME: remove boolean result value to enable batch-wise processing
	boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException;

	/**
	 * Removes an inferred statement from a specific context.
	 *
	 * @param subj     The subject of the statement that should be removed.
	 * @param pred     The predicate of the statement that should be removed.
	 * @param obj      The object of the statement that should be removed.
	 * @param contexts The context(s) from which to remove the statements. Note that this parameter is a vararg and as
	 *                 such is optional. If no contexts are supplied the method operates on the entire repository.
	 * @throws SailException         If the statement could not be removed.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	// FIXME: remove boolean result value to enable batch-wise processing
	boolean removeInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException;

	/**
	 * Removes all inferred statements from the specified/all contexts. If no contexts are specified the method operates
	 * on the entire repository.
	 *
	 * @param contexts The context(s) from which to remove the statements. Note that this parameter is a vararg and as
	 *                 such is optional. If no contexts are supplied the method operates on the entire repository.
	 * @throws SailException         If the statements could not be removed.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	void clearInferred(Resource... contexts) throws SailException;

	/**
	 * Flushes any pending updates to be processed and the resulting changes to be reported to registered
	 * {@link SailConnectionListener}s.
	 *
	 * @throws SailException         If the updates could not be processed.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	void flushUpdates() throws SailException;
}
