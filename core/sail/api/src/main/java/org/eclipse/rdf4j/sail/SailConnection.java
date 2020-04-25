/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail;

import java.util.Optional;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;

/**
 * A connection to an RDF Sail object. A SailConnection is active from the moment it is created until it is closed. Care
 * should be taken to properly close SailConnections as they might block concurrent queries and/or updates on the Sail
 * while active, depending on the Sail-implementation that is being used.
 *
 * @author jeen
 * @author Arjohn Kampman
 */
public interface SailConnection extends AutoCloseable {

	/**
	 * Checks whether this SailConnection is open. A SailConnection is open from the moment it is created until it is
	 * closed.
	 *
	 * @see SailConnection#close
	 */
	public boolean isOpen() throws SailException;

	/**
	 * Closes the connection. Any updates that haven't been committed yet will be rolled back. The connection can no
	 * longer be used once it is closed.
	 */
	@Override
	public void close() throws SailException;

	/**
	 * Allows the SailConnection to bypass the standard query parser and provide its own internal {@link TupleExpr}
	 * implementation. By default this method returns an empty result, signaling that it will rely on the RDF4J query
	 * parser.
	 *
	 * @param ql      the query language.
	 * @param type    indicates if the supplied query is a graph, tuple, or boolean query
	 * @param query   the unparsed query string
	 * @param baseURI the provided base URI. May be null or empty.
	 * @return an optional TupleExpr that represents a sail-specific version of the query, which {@link #evaluate} can
	 *         process. Returns {@link Optional#empty()} if the Sail does not provide its own query processing.
	 * @since 3.2.0
	 */
	public default Optional<TupleExpr> prepareQuery(QueryLanguage ql, Query.QueryType type, String query,
			String baseURI) {
		return Optional.empty();
	}

	/**
	 * Evaluates the supplied TupleExpr on the data contained in this Sail object, using the (optional) dataset and
	 * supplied bindings as input parameters.
	 *
	 * @param tupleExpr       The tuple expression to evaluate.
	 * @param dataset         The dataset to use for evaluating the query, <tt>null</tt> to use the Sail's default
	 *                        dataset.
	 * @param bindings        A set of input parameters for the query evaluation. The keys reference variable names that
	 *                        should be bound to the value they map to.
	 * @param includeInferred Indicates whether inferred triples are to be considered in the query result. If false, no
	 *                        inferred statements are returned; if true, inferred statements are returned if available
	 * @return The TupleQueryResult.
	 * @throws SailException         If the Sail object encountered an error or unexpected situation internally.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException;

	/**
	 * Returns the set of all unique context identifiers that are used to store statements.
	 *
	 * @return An iterator over the context identifiers, should not contain any duplicates.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException;

	/**
	 * Gets all statements from the specified contexts that have a specific subject, predicate and/or object. All three
	 * parameters may be null to indicate wildcards. The <tt>includeInferred</tt> parameter can be used to control which
	 * statements are fetched: all statements or only the statements that have been added explicitly.
	 *
	 * @param subj            A Resource specifying the subject, or <tt>null</tt> for a wildcard.
	 * @param pred            A URI specifying the predicate, or <tt>null</tt> for a wildcard.
	 * @param obj             A Value specifying the object, or <tt>null</tt> for a wildcard.
	 * @param includeInferred if false, no inferred statements are returned; if true, inferred statements are returned
	 *                        if available
	 * @param contexts        The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                        optional. If no contexts are specified the method operates on the entire repository. A
	 *                        <tt>null</tt> value can be used to match context-less statements.
	 * @return The statements matching the specified pattern.
	 * @throws SailException         If the Sail object encountered an error or unexpected situation internally.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws SailException;

	/**
	 * @deprecated since 4.0. Use {@link #getStatements(Resource, IRI, Value, boolean, Resource...)} instead.
	 */
	@Deprecated
	default CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, URI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {
		return getStatements(subj, (IRI) pred, obj, includeInferred, contexts);
	}

	/**
	 * Determines if the store contains any statements from the specified contexts that have a specific subject,
	 * predicate and/or object. All three parameters may be null to indicate wildcards. The <tt>includeInferred</tt>
	 * parameter can be used to control which statements are checked: all statements or only the statements that have
	 * been added explicitly.
	 *
	 * @param subj            A Resource specifying the subject, or <tt>null</tt> for a wildcard.
	 * @param pred            An IRI specifying the predicate, or <tt>null</tt> for a wildcard.
	 * @param obj             A Value specifying the object, or <tt>null</tt> for a wildcard.
	 * @param includeInferred if false, no inferred statements are returned; if true, inferred statements are returned
	 *                        if available
	 * @param contexts        The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                        optional. If no contexts are specified the method operates on the entire repository. A
	 *                        <tt>null</tt> value can be used to match context-less statements.
	 * @return <code>true</code> iff the store contains any statements matching the supplied criteria,
	 *         <code>false</code> otherwise.
	 * @throws SailException         If the Sail object encountered an error or unexpected situation internally.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	default boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws SailException {

		try (CloseableIteration<? extends Statement, SailException> stIter = getStatements(subj, pred, obj,
				includeInferred, contexts)) {
			return stIter.hasNext();
		}

	}

	/**
	 * Returns the number of (explicit) statements in the store, or in specific contexts.
	 *
	 * @param contexts The context(s) to determine the size of. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are specified the method operates on the entire repository. A
	 *                 <tt>null</tt> value can be used to match context-less statements.
	 * @return The number of explicit statements in this store, or in the specified context(s).
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public long size(Resource... contexts) throws SailException;

	/**
	 * Begins a transaction requiring {@link #commit()} or {@link #rollback()} to be called to close the transaction.
	 * The transaction will use the default {@link IsolationLevel} level for the SAIL, as returned by
	 * {@link Sail#getDefaultIsolationLevel()}.
	 *
	 * @throws SailException If the connection could not start a transaction or if a transaction is already active on
	 *                       this connection.
	 */
	public void begin() throws SailException;

	/**
	 * Begins a transaction with the specified {@link IsolationLevel} level, requiring {@link #commit()} or
	 * {@link #rollback()} to be called to close the transaction.
	 *
	 * @param level the transaction isolation level on which this transaction operates.
	 * @throws UnknownSailTransactionStateException If the IsolationLevel is not supported by this implementation
	 * @throws SailException                        If the connection could not start a transaction, if the supplied
	 *                                              transaction isolation level is not supported, or if a transaction is
	 *                                              already active on this connection.
	 */
	public void begin(IsolationLevel level) throws UnknownSailTransactionStateException, SailException;

	/**
	 * Flushes any pending updates and notify changes to listeners as appropriate. This is an optional call; calling or
	 * not calling this method should have no effect on the outcome of other calls. This method exists to give the
	 * caller more control over the efficiency when calling {@link #prepare()}. This method may be called multiple times
	 * within the same transaction.
	 *
	 * @throws SailException         If the updates could not be processed, for example because no transaction is
	 *                               active.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public void flush() throws SailException;

	/**
	 * Checks for an error state in the active transaction that would force the transaction to be rolled back. This is
	 * an optional call; calling or not calling this method should have no effect on the outcome of {@link #commit()} or
	 * {@link #rollback()}. A call to this method must be followed by (in the same thread) with a call to
	 * {@link #prepare()} , {@link #commit()}, {@link #rollback()}, or {@link #close()} . This method may be called
	 * multiple times within the same transaction by the same thread. If this method returns normally, the caller can
	 * reasonably expect that a subsequent call to {@link #commit()} will also return normally. If this method returns
	 * with an exception the caller should treat the exception as if it came from a call to {@link #commit()}.
	 *
	 * @throws UnknownSailTransactionStateException If the transaction state can not be determined (this can happen for
	 *                                              instance when communication between client and server fails or
	 *                                              times-out). It does not indicate a problem with the integrity of the
	 *                                              store.
	 * @throws SailException                        If there is an active transaction and it cannot be committed.
	 * @throws IllegalStateException                If the connection has been closed or prepare was already called by
	 *                                              another thread.
	 */
	public void prepare() throws SailException;

	/**
	 * Commits any updates that have been performed since the last time {@link #commit()} or {@link #rollback()} was
	 * called.
	 *
	 * @throws UnknownSailTransactionStateException If the transaction state can not be determined (this can happen for
	 *                                              instance when communication between client and server fails or
	 *                                              times-out). It does not indicate a problem with the integrity of the
	 *                                              store.
	 * @throws SailException                        If the SailConnection could not be committed.
	 * @throws IllegalStateException                If the connection has been closed.
	 */
	public void commit() throws SailException;

	/**
	 * Rolls back the transaction, discarding any uncommitted changes that have been made in this SailConnection.
	 *
	 * @throws UnknownSailTransactionStateException If the transaction state can not be determined (this can happen for
	 *                                              instance when communication between client and server fails or
	 *                                              times-out). It does not indicate a problem with the integrity of the
	 *                                              store.
	 * @throws SailException                        If the SailConnection could not be rolled back.
	 * @throws IllegalStateException                If the connection has been closed.
	 */
	public void rollback() throws SailException;

	/**
	 * Indicates if a transaction is currently active on the connection. A transaction is active if {@link #begin()} has
	 * been called, and becomes inactive after {@link #commit()} or {@link #rollback()} has been called.
	 *
	 * @return <code>true</code> iff a transaction is active, <code>false</code> iff no transaction is active.
	 * @throws UnknownSailTransactionStateException if the transaction state can not be determined (this can happen for
	 *                                              instance when communication between client and server fails or times
	 *                                              out).
	 */
	public boolean isActive() throws UnknownSailTransactionStateException;

	/**
	 * Adds a statement to the store.
	 *
	 * @param subj     The subject of the statement to add.
	 * @param pred     The predicate of the statement to add.
	 * @param obj      The object of the statement to add.
	 * @param contexts The context(s) to add the statement to. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are specified, a context-less statement will be added.
	 * @throws SailException         If the statement could not be added, for example because no transaction is active.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException;

	/**
	 * @deprecated since 4.0. Use {@link #addStatement(Resource, IRI, Value, Resource...)} instead.
	 */
	@Deprecated
	default void addStatement(Resource subj, URI pred, Value obj, Resource... contexts) throws SailException {
		addStatement(subj, (IRI) pred, obj, contexts);
	}

	/**
	 * Removes all statements matching the specified subject, predicate and object from the repository. All three
	 * parameters may be null to indicate wildcards.
	 *
	 * @param subj     The subject of the statement that should be removed, or <tt>null</tt> to indicate a wildcard.
	 * @param pred     The predicate of the statement that should be removed, or <tt>null</tt> to indicate a wildcard.
	 * @param obj      The object of the statement that should be removed , or <tt>null</tt> to indicate a wildcard. *
	 * @param contexts The context(s) from which to remove the statement. Note that this parameter is a vararg and as
	 *                 such is optional. If no contexts are specified the method operates on the entire repository. A
	 *                 <tt>null</tt> value can be used to match context-less statements.
	 * @throws SailException         If the statement could not be removed, for example because no transaction is
	 *                               active.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException;

	/**
	 * @deprecated since 4.0. Use {@link #removeStatements(Resource, IRI, Value, Resource...)} instead.
	 */
	@Deprecated
	default void removeStatements(Resource subj, URI pred, Value obj, Resource... contexts) throws SailException {
		removeStatements(subj, (IRI) pred, obj, contexts);
	}

	/**
	 * Signals the start of an update operation. The given <code>op</code> maybe passed to subsequent
	 * {@link #addStatement(UpdateContext, Resource, IRI, Value, Resource...)} or
	 * {@link #removeStatement(UpdateContext, Resource, IRI, Value, Resource...)} calls before
	 * {@link #endUpdate(UpdateContext)} is called.
	 *
	 * @throws SailException
	 */
	public void startUpdate(UpdateContext op) throws SailException;

	/**
	 * Adds a statement to the store. Called when adding statements through a {@link UpdateExpr} operation.
	 *
	 * @param op       operation properties of the {@link UpdateExpr} operation producing these statements.
	 * @param subj     The subject of the statement to add.
	 * @param pred     The predicate of the statement to add.
	 * @param obj      The object of the statement to add.
	 * @param contexts The context(s) to add the statement to. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are specified, a context-less statement will be added.
	 * @throws SailException         If the statement could not be added, for example because no transaction is active.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public void addStatement(UpdateContext op, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException;

	/**
	 * @deprecated since 4.0. Use {@link #addStatement(UpdateContext, Resource, IRI, Value, Resource...)} instead.
	 */
	@Deprecated
	default void addStatement(UpdateContext op, Resource subj, URI pred, Value obj, Resource... contexts)
			throws SailException {
		addStatement(op, subj, (IRI) pred, obj, contexts);
	}

	/**
	 * Removes all statements matching the specified subject, predicate and object from the repository. All three
	 * parameters may be null to indicate wildcards. Called when removing statements through a {@link UpdateExpr}
	 * operation.
	 *
	 * @param op       operation properties of the {@link UpdateExpr} operation removing these statements.
	 * @param subj     The subject of the statement that should be removed.
	 * @param pred     The predicate of the statement that should be removed.
	 * @param obj      The object of the statement that should be removed.
	 * @param contexts The context(s) from which to remove the statement. Note that this parameter is a vararg and as
	 *                 such is optional. If no contexts are specified the method operates on the entire repository. A
	 *                 <tt>null</tt> value can be used to match context-less statements.
	 * @throws SailException         If the statement could not be removed, for example because no transaction is
	 *                               active.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public void removeStatement(UpdateContext op, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException;

	/**
	 * @deprecated since 4.0. USe {@link #removeStatement(UpdateContext, Resource, IRI, Value, Resource...)} instead.
	 */
	@Deprecated
	default void removeStatement(UpdateContext op, Resource subj, URI pred, Value obj, Resource... contexts)
			throws SailException {
		removeStatement(op, subj, (IRI) pred, obj, contexts);
	}

	/**
	 * Indicates that the given <code>op</code> will not be used in any call again. Implementations should use this to
	 * flush of any temporary operation states that may have occurred.
	 *
	 * @param op
	 * @throws SailException
	 */
	public void endUpdate(UpdateContext op) throws SailException;

	/**
	 * Removes all statements from the specified/all contexts. If no contexts are specified the method operates on the
	 * entire repository.
	 *
	 * @param contexts The context(s) from which to remove the statements. Note that this parameter is a vararg and as
	 *                 such is optional. If no contexts are specified the method operates on the entire repository. A
	 *                 <tt>null</tt> value can be used to match context-less statements.
	 * @throws SailException         If the statements could not be removed.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public void clear(Resource... contexts) throws SailException;

	/**
	 * Gets the namespaces relevant to the data contained in this Sail object.
	 *
	 * @return An iterator over the relevant namespaces, should not contain any duplicates.
	 * @throws SailException         If the Sail object encountered an error or unexpected situation internally.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException;

	/**
	 * Gets the namespace that is associated with the specified prefix, if any.
	 *
	 * @param prefix A namespace prefix, or an empty string in case of the default namespace.
	 * @return The namespace name that is associated with the specified prefix, or <tt>null</tt> if there is no such
	 *         namespace.
	 * @throws SailException         If the Sail object encountered an error or unexpected situation internally.
	 * @throws NullPointerException  In case <tt>prefix</tt> is <tt>null</tt>.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public String getNamespace(String prefix) throws SailException;

	/**
	 * Sets the prefix for a namespace.
	 *
	 * @param prefix The new prefix, or an empty string in case of the default namespace.
	 * @param name   The namespace name that the prefix maps to.
	 * @throws SailException         If the Sail object encountered an error or unexpected situation internally.
	 * @throws NullPointerException  In case <tt>prefix</tt> or <tt>name</tt> is <tt>null</tt>.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public void setNamespace(String prefix, String name) throws SailException;

	/**
	 * Removes a namespace declaration by removing the association between a prefix and a namespace name.
	 *
	 * @param prefix The namespace prefix, or an empty string in case of the default namespace.
	 * @throws SailException         If the Sail object encountered an error or unexpected situation internally.
	 * @throws NullPointerException  In case <tt>prefix</tt> is <tt>null</tt>.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public void removeNamespace(String prefix) throws SailException;

	/**
	 * Removes all namespace declarations from the repository.
	 *
	 * @throws SailException         If the Sail object encountered an error or unexpected situation internally.
	 * @throws IllegalStateException If the connection has been closed.
	 */
	public void clearNamespaces() throws SailException;

	/**
	 * Indicates if the Sail has any statement removal operations pending (not yet {@link #flush() flushed}) for the
	 * current transaction.
	 *
	 * @return true if any statement removal operations have not yet been flushed, false otherwise.
	 * @see #flush()
	 * @deprecated
	 */
	@Deprecated
	boolean pendingRemovals();

	// TODO - make this a default no-op for backwards compatibility
	TupleExpr explain(Query.QueryExplainLevel queryExplainLevel, TupleExpr tupleExpr, Dataset activeDataset,
			BindingSet bindings, boolean includeInferred);
}
