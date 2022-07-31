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
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A state of an {@link SailSource} at a point in time that will remain consistent until {@link #close()} is called. The
 * life cycle follows that of a read operation.
 *
 * @author James Leigh
 */
public interface SailDataset extends SailClosable {

	/**
	 * Called when this {@link SailDataset} is no longer is used, such as when a read operation is complete. An
	 * isolation level compatible with {@link IsolationLevels#SNAPSHOT} will ensure the state of this
	 * {@link SailDataset} dose not change between the first call to this object until {@link SailClosable#release()} is
	 * called.
	 */
	@Override
	void close() throws SailException;

	/**
	 * Gets the namespaces relevant to the data contained in this object.
	 *
	 * @return An iterator over the relevant namespaces, should not contain any duplicates.
	 * @throws SailException If this object encountered an error or unexpected situation internally.
	 */
	CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException;

	/**
	 * Gets the namespace that is associated with the specified prefix, if any.
	 *
	 * @param prefix A namespace prefix, or an empty string in case of the default namespace.
	 * @return The namespace name that is associated with the specified prefix, or <var>null</var> if there is no such
	 *         namespace.
	 * @throws SailException        If this object encountered an error or unexpected situation internally.
	 * @throws NullPointerException In case <var>prefix</var> is <var>null</var>.
	 */
	String getNamespace(String prefix) throws SailException;

	/**
	 * Returns the set of all unique context identifiers that are used to store statements.
	 *
	 * @return An iterator over the context identifiers, should not contain any duplicates.
	 */
	CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException;

	/**
	 * Gets all statements that have a specific subject, predicate and/or object. All three parameters may be null to
	 * indicate wildcards. Optionally a (set of) context(s) may be specified in which case the result will be restricted
	 * to statements matching one or more of the specified contexts.
	 *
	 * @param subj     A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred     A IRI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj      A Value specifying the object, or <var>null</var> for a wildcard.
	 * @param contexts The context(s) to get the statements from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on all contexts.
	 * @return An iterator over the relevant statements.
	 * @throws SailException If the triple source failed to get the statements.
	 */
	CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException;

	/**
	 * Gets all RDF-star triples that have a specific subject, predicate and/or object. All three parameters may be null
	 * to indicate wildcards.
	 *
	 * @param subj A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred A IRI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj  A Value specifying the object, or <var>null</var> for a wildcard.
	 * @return An iterator over the relevant triples.
	 * @throws SailException If the triple source failed to get the RDF-star triples.
	 */
	default CloseableIteration<? extends Triple, SailException> getTriples(Resource subj, IRI pred, Value obj)
			throws SailException {
		throw new SailException("RDF-star triple retrieval not supported by this store");
	}

}
