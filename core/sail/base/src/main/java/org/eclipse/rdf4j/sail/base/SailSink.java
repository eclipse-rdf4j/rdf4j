/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import java.util.Set;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A mutable source of RDF graphs. The life cycle follows that of a write operation.
 *
 * @author James Leigh
 */
public interface SailSink extends SailClosable {

	/**
	 * Checks if this {@link SailSink} is consistent with the isolation level it was created with. If this Sink was
	 * created with a {@link IsolationLevels#SERIALIZABLE} and another conflicting {@link SailSink} has already been
	 * {@link #flush()}ed, this method will throw a {@link SailConflictException}.
	 */
	void prepare() throws SailException;

	/**
	 * Once this method returns successfully, changes that were made to this {@link SailSink} will be visible to
	 * subsequent {@link SailSource#dataset(IsolationLevel)}.
	 *
	 * @throws SailException
	 */
	void flush() throws SailException;

	/**
	 * Sets the prefix for a namespace.
	 *
	 * @param prefix The new prefix, or an empty string in case of the default namespace.
	 * @param name   The namespace name that the prefix maps to.
	 * @throws SailException        If the Sail object encountered an error or unexpected situation internally.
	 * @throws NullPointerException In case <var>prefix</var> or <var>name</var> is <var>null</var>.
	 */
	void setNamespace(String prefix, String name) throws SailException;

	/**
	 * Removes a namespace declaration by removing the association between a prefix and a namespace name.
	 *
	 * @param prefix The namespace prefix, or an empty string in case of the default namespace.
	 * @throws SailException
	 * @throws NullPointerException In case <var>prefix</var> is <var>null</var>.
	 */
	void removeNamespace(String prefix) throws SailException;

	/**
	 * Removes all namespace declarations from this {@link SailSource}.
	 *
	 * @throws SailException
	 */
	void clearNamespaces() throws SailException;

	/**
	 * Removes all statements from the specified/all contexts. If no contexts are specified the method operates on the
	 * entire repository.
	 *
	 * @param contexts The context(s) from which to remove the statements. Note that this parameter is a vararg and as
	 *                 such is optional. If no contexts are specified the method operates on the entire repository. A
	 *                 <var>null</var> value can be used to match context-less statements.
	 * @throws SailException If the statements could not be removed.
	 */
	void clear(Resource... contexts) throws SailException;

	/**
	 * Called to indicate matching statements have been observed and must not change their state until after this
	 * {@link SailSink} is committed, iff this was opened in an isolation level compatible with
	 * {@link IsolationLevels#SERIALIZABLE}.
	 *
	 * @param subj     A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred     A IRI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj      A Value specifying the object, or <var>null</var> for a wildcard.
	 * @param contexts The context(s) of the observed statements. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on all contexts.
	 * @throws SailException If the triple source failed to observe these statements.
	 */
	void observe(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException;

	/**
	 * Called to indicate matching statements have been observed and must not change their state until after this
	 * {@link SailSink} is committed, iff this was opened in an isolation level compatible with
	 * {@link IsolationLevels#SERIALIZABLE}.
	 *
	 * @param subj    A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred    A IRI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj     A Value specifying the object, or <var>null</var> for a wildcard.
	 * @param context The context of the observed statements.
	 * @throws SailException If the triple source failed to observe these statements.
	 */
	default void observe(Resource subj, IRI pred, Value obj, Resource context) throws SailException {
		observe(subj, pred, obj, new Resource[] { context });
	}

	/**
	 * Adds a statement to the store.
	 *
	 * @param subj The subject of the statement to add.
	 * @param pred The predicate of the statement to add.
	 * @param obj  The object of the statement to add.
	 * @param ctx  The context to add the statement to.
	 * @throws SailException If the statement could not be added, for example because no transaction is active.
	 */
	void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException;

	/**
	 * Adds a statement to the store.
	 *
	 * @param statement The statement to add.
	 * @throws SailException If the statement could not be added, for example because no transaction is active.
	 */
	default void approve(Statement statement) throws SailException {
		approve(statement.getSubject(), statement.getPredicate(), statement.getObject(), statement.getContext());
	}

	/**
	 * Removes a statement with the specified subject, predicate, object, and context. All four parameters may be
	 * non-null.
	 * <p>
	 * Deprecated since 3.1.0 2019.
	 *
	 * @param subj The subject of the statement that should be removed
	 * @param pred The predicate of the statement that should be removed
	 * @param obj  The object of the statement that should be removed
	 * @param ctx  The context from which to remove the statement
	 * @throws SailException If the statement could not be removed, for example because no transaction is active.
	 */
	@Deprecated
	default void deprecate(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {
		deprecate(SimpleValueFactory.getInstance().createStatement(subj, pred, obj, ctx));
	}

	/**
	 * Removes a statement.
	 *
	 * @param statement The statement that should be removed
	 * @throws SailException If the statement could not be removed, for example because no transaction is active.
	 */
	void deprecate(Statement statement) throws SailException;

	/**
	 * Removes all statements with the specified subject, predicate, object, and context. All four parameters may be
	 * null.
	 *
	 * @throws SailException If statements could not be removed, for example because no transaction is active.
	 */
	default boolean deprecateByQuery(Resource subj, IRI pred, Value obj, Resource[] contexts) {
		throw new UnsupportedOperationException();
	}

	default boolean supportsDeprecateByQuery() {
		return false;
	}

	default void approveAll(Set<Statement> approved, Set<Resource> approvedContexts) {
		for (Statement statement : approved) {
			approve(statement);
		}
	}

	default void deprecateAll(Set<Statement> deprecated) {
		for (Statement statement : deprecated) {
			deprecate(statement);
		}
	}

	default void observeAll(Set<Changeset.SimpleStatementPattern> observed) {
		for (Changeset.SimpleStatementPattern p : observed) {
			Resource subj = p.getSubject();
			IRI pred = p.getPredicate();
			Value obj = p.getObject();
			Resource context = p.getContext();
			if (p.isAllContexts()) {
				observe(subj, pred, obj);
			} else {
				observe(subj, pred, obj, context);
			}
		}

	}
}
