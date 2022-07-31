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
package org.eclipse.rdf4j.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

/**
 * Main interface for updating data in and performing queries on an RDF4J {@link Repository}.
 * <p>
 * By default, a RepositoryConnection is in auto-commit mode, meaning that each operation corresponds to a single
 * transaction on the underlying store. Multiple operations can be bundled in a single transaction by using
 * {@link #begin()} and {@link #commit() commit}/ {@link #rollback() rollback}, which may improve performance
 * considerably when dealing with many thousands of statements. Care should be taking to always properly close a
 * RepositoryConnection after one is finished with it, to free up resources and avoid unnecessary locks.
 * <p>
 * RepositoryConnection is not guaranteed to be thread-safe. The recommended access pattern in a multithreaded
 * application is to ensure that each thread creates/uses its own RepositoryConnections (which can be obtained from a
 * shared {@link Repository}).
 * <p>
 * Several methods take a vararg argument that optionally specifies one or more contexts (named graphs) on which the
 * method should operate. A vararg parameter is optional, it can be completely left out of the method call, in which
 * case a method either operates on a provided statements context (if one of the method parameters is a statement or
 * collection of statements), or operates on the repository as a whole, completely ignoring context. A vararg argument
 * may also be 'null' (cast to Resource) meaning that the method operates on those statements which have no associated
 * context only.
 * <p>
 * Examples:
 *
 * <pre>
 * {@code
 * // Ex 1: this method retrieves all statements that appear in either context1 or
 * // context2, or both.
 * RepositoryConnection.getStatements(null, null, null, true, context1, context2);
 *
 * // Ex 2: this method retrieves all statements that appear in the repository
 * // (regardless of context).
 * RepositoryConnection.getStatements(null, null, null, true);
 *
 * // Ex 3: this method retrieves all statements that have no associated context in
 * // the repository.
 * // Observe that this is not equivalent to the previous method call.
 * RepositoryConnection.getStatements(null, null, null, true, (Resource)null);
 *
 * // Ex 4: this method adds a statement to the store. If the statement object
 * // itself has a context (i.e. statement.getContext() != null) the statement is added
 * // to that context. Otherwise, it is added without any associated context.
 * RepositoryConnection.add(statement);
 *
 * // Ex 5: this method adds a statement to context1 in the store. It completely
 * // ignores any context the statement itself has.
 * RepositoryConnection.add(statement, context1);
 * }
 * </pre>
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 * @see Repositories
 */
public interface RepositoryConnection extends AutoCloseable {

	/**
	 * Returns the Repository object to which this connection belongs.
	 */
	Repository getRepository();

	/**
	 * Set the parser configuration this connection should use for RDFParser-based operations.
	 *
	 * @param config a Rio RDF Parser configuration.
	 */
	void setParserConfig(ParserConfig config);

	/**
	 * Returns the parser configuration this connection uses for Rio-based operations.
	 *
	 * @return a Rio RDF parser configuration.
	 */
	ParserConfig getParserConfig();

	/**
	 * Gets a ValueFactory for this RepositoryConnection.
	 *
	 * @return A repository-specific ValueFactory.
	 */
	ValueFactory getValueFactory();

	/**
	 * Checks whether this connection is open. A connection is open from the moment it is created until it is closed.
	 *
	 * @see #close()
	 */
	boolean isOpen() throws RepositoryException;

	/**
	 * Closes the connection, freeing resources. If a {@link #begin() transaction} is {@link #isActive() active} on the
	 * connection, all non-committed operations will be lost by actively calling {@link #rollback()} on any active
	 * transactions.
	 * <p>
	 * Implementation note: All implementations must override this method if they have any resources that they need to
	 * free.
	 *
	 * @throws RepositoryException If the connection could not be closed.
	 */
	@Override
	default void close() throws RepositoryException {
		if (isOpen() && isActive()) {
			rollback();
		}
	}

	/**
	 * Prepares a SPARQL query for evaluation on this repository (optional operation). In case the query contains
	 * relative URIs that need to be resolved against an external base URI, one should use
	 * {@link #prepareQuery(QueryLanguage, String, String)} instead.
	 * <p>
	 * If you already know the type of query, using the more specific {@link #prepareTupleQuery},
	 * {@link #prepareGraphQuery} or {@link #prepareBooleanQuery} is likely to be more efficient.
	 *
	 * @param query The query string, in SPARQL syntax.
	 * @return A query ready to be evaluated on this repository.
	 * @throws MalformedQueryException       If the supplied query is malformed.
	 * @throws UnsupportedOperationException If the <var>prepareQuery</var> method is not supported by this repository.
	 * @see #prepareQuery(QueryLanguage, String)
	 */
	default Query prepareQuery(String query) throws RepositoryException, MalformedQueryException {
		return prepareQuery(QueryLanguage.SPARQL, query);
	}

	/**
	 * Prepares a query for evaluation on this repository (optional operation). In case the query contains relative URIs
	 * that need to be resolved against an external base URI, one should use
	 * {@link #prepareQuery(QueryLanguage, String, String)} instead.
	 * <p>
	 * If you already know the type of query, using the more specific {@link #prepareTupleQuery},
	 * {@link #prepareGraphQuery} or {@link #prepareBooleanQuery} is likely to be more efficient.
	 *
	 * @param ql    The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query The query string.
	 * @return A query ready to be evaluated on this repository.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 * @throws UnsupportedOperationException     If the <var>prepareQuery</var> method is not supported by this
	 *                                           repository.
	 */
	Query prepareQuery(QueryLanguage ql, String query) throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares a query for evaluation on this repository (optional operation).
	 * <p>
	 * If you already know the type of query, using the more specific {@link #prepareTupleQuery},
	 * {@link #prepareGraphQuery} or {@link #prepareBooleanQuery} is likely to be more efficient.
	 *
	 * @param ql      The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query   The query string.
	 * @param baseURI The base URI to resolve any relative URIs that are in the query against, can be <var>null</var> if
	 *                the query does not contain any relative URIs.
	 * @return A query ready to be evaluated on this repository.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 * @throws UnsupportedOperationException     If the <var>prepareQuery</var> method is not supported by this
	 *                                           repository.
	 */
	Query prepareQuery(QueryLanguage ql, String query, String baseURI)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares a SPARQL query that produces sets of value tuples, that is a SPARQL SELECT query. In case the query
	 * contains relative URIs that need to be resolved against an external base URI, one should use
	 * {@link #prepareTupleQuery(QueryLanguage, String, String)} instead.
	 *
	 * @param query The query string, in SPARQL syntax.
	 * @return a {@link TupleQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException If the supplied query is not a tuple query.
	 * @throws MalformedQueryException  If the supplied query is malformed.
	 * @see #prepareTupleQuery(QueryLanguage, String)
	 */
	default TupleQuery prepareTupleQuery(String query) throws RepositoryException, MalformedQueryException {
		return prepareTupleQuery(QueryLanguage.SPARQL, query);
	}

	/**
	 * Prepares a query that produces sets of value tuples. In case the query contains relative URIs that need to be
	 * resolved against an external base URI, one should use {@link #prepareTupleQuery(QueryLanguage, String, String)}
	 * instead.
	 *
	 * @param ql    The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query The query string.
	 * @return a {@link TupleQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException          If the supplied query is not a tuple query.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 */
	TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares a query that produces sets of value tuples.
	 *
	 * @param ql      The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query   The query string.
	 * @param baseURI The base URI to resolve any relative URIs that are in the query against, can be <var>null</var> if
	 *                the query does not contain any relative URIs.
	 * @return a {@link TupleQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException          If the supplied query is not a tuple query.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 */
	TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares SPARQL queries that produce RDF graphs, that is, SPARQL CONSTRUCT or DESCRIBE queries. In case the query
	 * contains relative URIs that need to be resolved against an external base URI, one should use
	 * {@link #prepareGraphQuery(QueryLanguage, String, String)} instead.
	 *
	 * @param query The query string, in SPARQL syntax.
	 * @return a {@link GraphQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException If the supplied query is not a graph query.
	 * @throws MalformedQueryException  If the supplied query is malformed.
	 * @see #prepareGraphQuery(QueryLanguage, String)
	 */
	default GraphQuery prepareGraphQuery(String query) throws RepositoryException, MalformedQueryException {
		return prepareGraphQuery(QueryLanguage.SPARQL, query);
	}

	/**
	 * Prepares queries that produce RDF graphs. In case the query contains relative URIs that need to be resolved
	 * against an external base URI, one should use {@link #prepareGraphQuery(QueryLanguage, String, String)} instead.
	 *
	 * @param ql    The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query The query string.
	 * @return a {@link GraphQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException          If the supplied query is not a graph query.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 */
	GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares queries that produce RDF graphs.
	 *
	 * @param ql      The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query   The query string.
	 * @param baseURI The base URI to resolve any relative URIs that are in the query against, can be <var>null</var> if
	 *                the query does not contain any relative URIs.
	 * @return a {@link GraphQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException          If the supplied query is not a graph query.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 */
	GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares SPARQL queries that return <var>true</var> or <var>false</var>, that is, SPARQL ASK queries. In case the
	 * query contains relative URIs that need to be resolved against an external base URI, one should use
	 * {@link #prepareBooleanQuery(QueryLanguage, String, String)} instead.
	 *
	 * @param query The query string, in SPARQL syntax.
	 * @return a {@link BooleanQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException If the supplied query is not a boolean query.
	 * @throws MalformedQueryException  If the supplied SPARQL query is malformed.
	 * @see #prepareBooleanQuery(QueryLanguage, String)
	 */
	default BooleanQuery prepareBooleanQuery(String query) throws RepositoryException, MalformedQueryException {
		return prepareBooleanQuery(QueryLanguage.SPARQL, query);
	}

	/**
	 * Prepares queries that return <var>true</var> or <var>false</var>. In case the query contains relative URIs that
	 * need to be resolved against an external base URI, one should use
	 * {@link #prepareBooleanQuery(QueryLanguage, String, String)} instead.
	 *
	 * @param ql    The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query The query string.
	 * @return a {@link BooleanQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException          If the supplied query is not a boolean query.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 */
	BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares queries that return <var>true</var> or <var>false</var>.
	 *
	 * @param ql      The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query   The query string.
	 * @param baseURI The base URI to resolve any relative URIs that are in the query against, can be <var>null</var> if
	 *                the query does not contain any relative URIs.
	 * @return a {@link BooleanQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException          If the supplied query is not a boolean query.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 */
	BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares a SPARQL Update operation. In case the update string contains relative URIs that need to be resolved
	 * against an external base URI, one should use {@link #prepareUpdate(QueryLanguage, String, String)} instead.
	 *
	 * @param update The update operation string, in SPARQL syntax.
	 * @return a {@link Update} ready to be executed on this {@link RepositoryConnection}.
	 * @throws MalformedQueryException If the supplied update operation string is malformed.
	 * @see #prepareUpdate(QueryLanguage, String)
	 */
	default Update prepareUpdate(String update) throws RepositoryException, MalformedQueryException {
		return prepareUpdate(QueryLanguage.SPARQL, update);
	}

	/**
	 * Prepares an Update operation. In case the update string contains relative URIs that need to be resolved against
	 * an external base URI, one should use {@link #prepareUpdate(QueryLanguage, String, String)} instead.
	 *
	 * @param ql     The {@link QueryLanguage query language} in which the update operation is formulated.
	 * @param update The update operation string.
	 * @return a {@link Update} ready to be executed on this {@link RepositoryConnection}.
	 * @throws MalformedQueryException If the supplied update operation string is malformed.
	 */
	Update prepareUpdate(QueryLanguage ql, String update) throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares an Update operation.
	 *
	 * @param ql      The {@link QueryLanguage query language} in which the update operation is formulated.
	 * @param update  The update operation string.
	 * @param baseURI The base URI to resolve any relative URIs that are in the update against, can be <var>null</var>
	 *                if the update does not contain any relative URIs.
	 * @return a {@link Update} ready to be executed on this {@link RepositoryConnection}.
	 * @throws MalformedQueryException If the supplied update operation string is malformed.
	 */
	Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Gets all resources that are used as content identifiers. Care should be taken that the returned
	 * {@link RepositoryResult} is closed to free any resources that it keeps hold of.
	 *
	 * @return a RepositoryResult object containing Resources that are used as context identifiers.
	 */
	RepositoryResult<Resource> getContextIDs() throws RepositoryException;

	/**
	 * Gets all statements with a specific subject, predicate and/or object from the repository. The result is
	 * optionally restricted to the specified set of named contexts. If the repository supports inferencing, inferred
	 * statements will be included in the result.
	 *
	 * @param subj     A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred     A URI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj      A Value specifying the object, or <var>null</var> for a wildcard.
	 * @param contexts The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @return The statements matching the specified pattern. The result object is a {@link RepositoryResult} object, a
	 *         lazy Iterator-like object containing {@link Statement}s and optionally throwing a
	 *         {@link RepositoryException} when an error when a problem occurs during retrieval.
	 */
	default RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws RepositoryException {
		return getStatements(subj, pred, obj, true, contexts);
	}

	/**
	 * Gets all statements with a specific subject, predicate and/or object from the repository. The result is
	 * optionally restricted to the specified set of named contexts.
	 *
	 * @param subj            A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred            An IRI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj             A Value specifying the object, or <var>null</var> for a wildcard.
	 * @param contexts        The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                        optional. If no contexts are supplied the method operates on the entire repository.
	 * @param includeInferred if false, no inferred statements are returned; if true, inferred statements are returned
	 *                        if available. The default is true.
	 * @return The statements matching the specified pattern. The result object is a {@link RepositoryResult} object, a
	 *         lazy Iterator-like object containing {@link Statement}s and optionally throwing a
	 *         {@link RepositoryException} when an error when a problem occurs during retrieval.
	 */
	RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException;

	/**
	 * Checks whether the repository contains statements with a specific subject, predicate and/or object, optionally in
	 * the specified contexts.
	 *
	 * @param subj            A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred            An IRI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj             A Value specifying the object, or <var>null</var> for a wildcard.
	 * @param contexts        The context(s) the need to be searched. Note that this parameter is a vararg and as such
	 *                        is optional. If no contexts are supplied the method operates on the entire repository.
	 * @param includeInferred if false, no inferred statements are considered; if true, inferred statements are
	 *                        considered if available
	 * @return true If a matching statement is in the repository in the specified context, false otherwise.
	 */
	boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException;

	/**
	 * Checks whether the repository contains the specified statement, optionally in the specified contexts.
	 *
	 * @param st              The statement to look for. Context information in the statement is ignored.
	 * @param contexts        The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                        optional. If no contexts are supplied the method operates on the entire repository.
	 * @param includeInferred if false, no inferred statements are considered; if true, inferred statements are
	 *                        considered if available
	 * @return true If the repository contains the specified statement, false otherwise.
	 */
	boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts) throws RepositoryException;

	/**
	 * Exports all statements with a specific subject, predicate and/or object from the repository, optionally from the
	 * specified contexts. This method supplies the RDFHandler with all namespace declarations available in the
	 * repository.
	 *
	 * @param subj            The subject, or null if the subject doesn't matter.
	 * @param pred            The predicate, or null if the predicate doesn't matter.
	 * @param obj             The object, or null if the object doesn't matter.
	 * @param contexts        The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                        optional. If no contexts are supplied the method operates on the entire repository.
	 * @param handler         The handler that will handle the RDF data.
	 * @param includeInferred if false, no inferred statements are returned; if true, inferred statements are returned
	 *                        if available
	 * @throws RDFHandlerException If the handler encounters an unrecoverable error.
	 */
	void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
			Resource... contexts) throws RepositoryException, RDFHandlerException;

	/**
	 * Exports all explicit statements in the specified contexts to the supplied RDFHandler. This method supplies the
	 * RDFHandler with all namespace declarations available in the repository.
	 *
	 * @param contexts The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @param handler  The handler that will handle the RDF data.
	 * @throws RDFHandlerException If the handler encounters an unrecoverable error.
	 */
	void export(RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException;

	/**
	 * Returns the number of (explicit) statements that are in the specified contexts in this repository.
	 *
	 * @param contexts The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @return The number of explicit statements from the specified contexts in this repository.
	 */
	long size(Resource... contexts) throws RepositoryException;

	/**
	 * Returns <var>true</var> if this repository does not contain any (explicit) statements.
	 *
	 * @return <var>true</var> if this repository is empty, <var>false</var> otherwise.
	 * @throws RepositoryException If the repository could not be checked to be empty.
	 */
	boolean isEmpty() throws RepositoryException;

	/**
	 * Enables or disables auto-commit mode for the connection. If a connection is in auto-commit mode, then all updates
	 * will be executed and committed as individual transactions. Otherwise, the updates are grouped into transactions
	 * that are terminated by a call to either {@link #commit} or {@link #rollback}. By default, new connections are in
	 * auto-commit mode.
	 * <p>
	 * <b>NOTE:</b> If this connection is switched to auto-commit mode during a transaction, the transaction is
	 * committed.
	 *
	 * @deprecated As of release 2.7.0, use {@link #begin()} instead.
	 * @throws RepositoryException In case the mode switch failed, for example because a currently active transaction
	 *                             failed to commit.
	 * @see #commit()
	 */
	@Deprecated
	void setAutoCommit(boolean autoCommit) throws RepositoryException;

	/**
	 * Indicates if the connection is in auto-commit mode. The connection is in auto-commit mode when no transaction is
	 * currently active, that is, when:
	 * <ol>
	 * <li>{@link #begin()} has not been called or;
	 * <li>{@link #commit()} or {@link #rollback()} have been called to finish the transaction.
	 * </ol>
	 *
	 * @deprecated since 2.0. Use {@link #isActive()} instead.
	 * @throws RepositoryException If a repository access error occurs.
	 */
	@Deprecated
	boolean isAutoCommit() throws RepositoryException;

	/**
	 * Indicates if a transaction is currently active on the connection. A transaction is active if {@link #begin()} has
	 * been called, and becomes inactive after {@link #commit()} or {@link #rollback()} has been called.
	 *
	 * @return <code>true</code> iff a transaction is active, <code>false</code> iff no transaction is active.
	 * @throws UnknownTransactionStateException if the transaction state can not be determined. This can happen for
	 *                                          instance when communication with a repository fails or times out.
	 * @throws RepositoryException
	 */
	boolean isActive() throws UnknownTransactionStateException, RepositoryException;

	/**
	 * Sets the transaction isolation level for the next transaction(s) on this connection. If the level is set to a
	 * value that is not supported by the underlying repository, this method will still succeed but a subsequent call to
	 * {@link #begin()} will result in an exception.
	 *
	 * @param level the transaction isolation level to set.
	 * @throws IllegalStateException if the method is called while a transaction is already active.
	 */
	void setIsolationLevel(IsolationLevel level) throws IllegalStateException;

	/**
	 * Retrieves the current {@link IsolationLevel transaction isolation level} of the connection.
	 *
	 * @return the current transaction isolation level.
	 */
	IsolationLevel getIsolationLevel();

	/**
	 * Begins a new transaction, requiring {@link #commit()} or {@link #rollback()} to be called to end the transaction.
	 * The transaction will use the currently set {@link IsolationLevel isolation level} for this connection.
	 *
	 * @throws RepositoryException If the connection could not start the transaction. One possible reason this may
	 *                             happen is if a transaction is already {@link #isActive() active} on the current
	 *                             connection.
	 * @see #begin(IsolationLevel)
	 * @see #isActive()
	 * @see #commit()
	 * @see #rollback()
	 * @see #setIsolationLevel(IsolationLevel)
	 */
	void begin() throws RepositoryException;

	/**
	 * Begins a new transaction with the supplied {@link IsolationLevel}, requiring {@link #commit()} or
	 * {@link #rollback()} to be called to end the transaction.
	 *
	 * @param level The {@link IsolationLevel} at which this transaction will operate. If set to <code>null</code> the
	 *              default isolation level of the underlying store will be used. If the specified isolation level is
	 *              not supported by the underlying store, it will attempt to use a supported
	 *              {@link IsolationLevel#isCompatibleWith(IsolationLevel) compatible level} instead.
	 * @throws RepositoryException If the connection could not start the transaction. Possible reasons this may happen
	 *                             are:
	 *                             <ul>
	 *                             <li>a transaction is already {@link #isActive() active} on the current connection.
	 *                             <li>the specified {@link IsolationLevel} is not supported by the store, and no
	 *                             compatible level could be found.
	 *                             </ul>
	 * @see #begin()
	 * @see #isActive()
	 * @see #commit()
	 * @see #rollback()
	 * @see #setIsolationLevel(IsolationLevel)
	 */
	void begin(IsolationLevel level) throws RepositoryException;

	/**
	 * Begins a new transaction with the supplied {@link TransactionSetting}, requiring {@link #commit()} or
	 * {@link #rollback()} to be called to end the transaction.
	 *
	 * @param settings The {@link TransactionSetting} (zero or more) for this transaction. If an isolation level is
	 *                 provided in the settings this will be used for the transaction. If none is provided then the
	 *                 default will be used. Behaviour of this method is undefined if more than one isolation level is
	 *                 provided. Behaviour of this method is undefined if one or more settings is null.
	 * @throws RepositoryException If the connection could not start the transaction. Possible reasons this may happen
	 *                             are:
	 *                             <ul>
	 *                             <li>a transaction is already {@link #isActive() active} on the current connection.
	 *                             <li>the specified {@link IsolationLevel} is not supported by the store, and no
	 *                             compatible level could be found.
	 *                             </ul>
	 * @see #begin()
	 * @see #isActive()
	 * @see #commit()
	 * @see #rollback()
	 * @see #setIsolationLevel(IsolationLevel)
	 * @since 3.3.0
	 */
	default void begin(TransactionSetting... settings) {
		for (TransactionSetting setting : settings) {
			if (setting instanceof IsolationLevel) {
				begin(((IsolationLevel) setting));
				return;
			}
		}

		begin();
	}

	/**
	 * Checks for an error state in the active transaction that would force the transaction to be rolled back. This is
	 * an optional call; calling or not calling this method should have no effect on the outcome of {@link #commit()} or
	 * {@link #rollback()}. A call to this method must be followed by (in the same thread) with a call to
	 * {@link #prepare()} , {@link #commit()}, {@link #rollback()}, or {@link #close()} . This method may be called
	 * multiple times within the same transaction by the same thread. If this method returns normally, the caller can
	 * reasonably expect that a subsequent call to {@link #commit()} will also return normally. If this method returns
	 * with an exception the caller should treat the exception as if it came from a call to {@link #commit()}.
	 *
	 * @throws UnknownTransactionStateException If the transaction state can not be determined (this can happen for
	 *                                          instance when communication between client and server fails or
	 *                                          times-out). It does not indicate a problem with the integrity of the
	 *                                          store.
	 * @throws RepositoryException              If there is an active transaction and it cannot be committed.
	 * @throws IllegalStateException            If the connection has been closed or prepare was already called by
	 *                                          another thread.
	 *
	 * @implNote this default method throws an {@link UnsupportedOperationException} and is a temporary measure to
	 *           ensure backward compatibility only. Implementing classes should override.
	 *
	 * @since 3.5.0
	 * @see #commit()
	 * @see #begin()
	 * @see #rollback()
	 */
	default void prepare() throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Commits the active transaction. This operation ends the active transaction.
	 *
	 * @throws UnknownTransactionStateException if the transaction state can not be determined. This can happen for
	 *                                          instance when communication with a repository fails or times out.
	 * @throws RepositoryException              If the connection could not be committed, or if the connection does not
	 *                                          have an active transaction.
	 * @see #isActive()
	 * @see #begin()
	 * @see #rollback()
	 * @see #prepare()
	 */
	void commit() throws RepositoryException;

	/**
	 * Rolls back all updates in the active transaction. This operation ends the active transaction.
	 *
	 * @throws UnknownTransactionStateException if the transaction state can not be determined. This can happen for
	 *                                          instance when communication with a repository fails or times out.
	 * @throws RepositoryException              If the transaction could not be rolled back, or if the connection does
	 *                                          not have an active transaction.
	 * @see #isActive()
	 * @see #begin()
	 * @see #commit()
	 */
	void rollback() throws RepositoryException;

	/**
	 * Adds RDF data from an InputStream to the repository, optionally to one or more named contexts.
	 *
	 * @param in         An InputStream from which RDF data can be read.
	 * @param dataFormat The serialization format of the data.
	 * @param contexts   The contexts to add the data to. If one or more contexts are supplied the method ignores
	 *                   contextual information in the actual data. If no contexts are supplied the contextual
	 *                   information in the input stream is used, if no context information is available the data is
	 *                   added without any context.
	 * @throws IOException                  If an I/O error occurred while reading from the input stream.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 * @since 3.5.0
	 */
	default void add(InputStream in, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		add(in, null, dataFormat, contexts);
	}

	/**
	 * Adds RDF data from an InputStream to the repository, optionally to one or more named contexts.
	 *
	 * @param in         An InputStream from which RDF data can be read.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. May be
	 *                   <code>null</code>.
	 *                   <p>
	 *                   Note that if the data contains an embedded base URI, that embedded base URI will overrule the
	 *                   value supplied here (see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a> section
	 *                   5.1 for details).
	 * @param dataFormat The serialization format of the data.
	 * @param contexts   The contexts to add the data to. If one or more contexts are supplied the method ignores
	 *                   contextual information in the actual data. If no contexts are supplied the contextual
	 *                   information in the input stream is used, if no context information is available the data is
	 *                   added without any context.
	 *
	 * @throws IOException                  If an I/O error occurred while reading from the input stream.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 */
	void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException;

	/**
	 * Adds RDF data from a Reader to the repository, optionally to one or more named contexts. <b>Note: using a Reader
	 * to upload byte-based data means that you have to be careful not to destroy the data's character encoding by
	 * enforcing a default character encoding upon the bytes. If possible, adding such data using an InputStream is to
	 * be preferred.</b>
	 *
	 * @param reader     A Reader from which RDF data can be read.
	 * @param dataFormat The serialization format of the data.
	 * @param contexts   The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                   these contexts, ignoring any context information in the data itself.
	 *
	 * @throws IOException                  If an I/O error occurred while reading from the reader.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 *
	 * @since 3.5.0
	 */
	default void add(Reader reader, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		add(reader, null, dataFormat, contexts);
	}

	/**
	 * Adds RDF data from a Reader to the repository, optionally to one or more named contexts. <b>Note: using a Reader
	 * to upload byte-based data means that you have to be careful not to destroy the data's character encoding by
	 * enforcing a default character encoding upon the bytes. If possible, adding such data using an InputStream is to
	 * be preferred.</b>
	 *
	 * @param reader     A Reader from which RDF data can be read.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. May be
	 *                   <code>null</code>.
	 *                   <p>
	 *                   Note that if the data contains an embedded base URI, that embedded base URI will overrule the
	 *                   value supplied here (see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a> section
	 *                   5.1 for details).
	 * @param dataFormat The serialization format of the data.
	 * @param contexts   The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                   these contexts, ignoring any context information in the data itself.
	 * @throws IOException                  If an I/O error occurred while reading from the reader.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 */
	void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException;

	/**
	 * Adds the RDF data that can be found at the specified URL to the repository, optionally to one or more named
	 * contexts.
	 *
	 * @param url      The URL of the RDF data.
	 * @param contexts The contexts to add the data to. If one or more contexts are specified the data is added to these
	 *                 contexts, ignoring any context information in the data itself.
	 *
	 * @throws IOException                  If an I/O error occurred while reading from the URL.
	 * @throws UnsupportedRDFormatException If the RDF format could not be recognized.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 *
	 * @since 3.5.0
	 */
	default void add(URL url, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		add(url, null, null, contexts);
	}

	/**
	 * Adds the RDF data that can be found at the specified URL to the repository, optionally to one or more named
	 * contexts.
	 *
	 * @param url        The URL of the RDF data.
	 * @param dataFormat The serialization format of the data. If set to <var>null</var>, the format will be
	 *                   automatically determined by examining the content type in the HTTP response header, and failing
	 *                   that, the file name extension of the supplied URL.
	 * @param contexts   The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                   these contexts, ignoring any context information in the data itself.
	 *
	 * @throws IOException                  If an I/O error occurred while reading from the URL.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format, or the RDF format
	 *                                      could not be automatically determined.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 *
	 * @since 3.5.0
	 */
	default void add(URL url, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		add(url, null, dataFormat, contexts);
	}

	/**
	 * Adds the RDF data that can be found at the specified URL to the repository, optionally to one or more named
	 * contexts.
	 *
	 * @param url        The URL of the RDF data.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. This defaults to the
	 *                   value of {@link java.net.URL#toExternalForm() url.toExternalForm()} if the value is set to
	 *                   <var>null</var>.
	 *                   <p>
	 *                   Note that if the data contains an embedded base URI, that embedded base URI will overrule the
	 *                   value supplied here (see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a> section
	 *                   5.1 for details).
	 * @param dataFormat The serialization format of the data. If set to <var>null</var>, the format will be
	 *                   automatically determined by examining the content type in the HTTP response header, and failing
	 *                   that, the file name extension of the supplied URL.
	 * @param contexts   The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                   these contexts, ignoring any context information in the data itself.
	 * @throws IOException                  If an I/O error occurred while reading from the URL.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format, or the RDF format
	 *                                      could not be automatically determined.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 */
	void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException;

	/**
	 * Adds RDF data from the specified file to a specific contexts in the repository.
	 *
	 * @param file     A file containing RDF data.
	 * @param contexts The contexts to add the data to. Note that this parameter is a vararg and as such is optional. If
	 *                 no contexts are specified, the data is added to any context specified in the actual data file, or
	 *                 if the data contains no context, it is added without context. If one or more contexts are
	 *                 specified the data is added to these contexts, ignoring any context information in the data
	 *                 itself.
	 *
	 * @throws IOException                  If an I/O error occurred while reading from the file.
	 * @throws UnsupportedRDFormatException If the RDF format of the supplied file could not be recognized.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 *
	 * @since 3.5.0
	 */
	default void add(File file, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		add(file, null, null, contexts);
	}

	/**
	 * Adds RDF data from the specified file to a specific contexts in the repository.
	 *
	 * @param file       A file containing RDF data.
	 * @param dataFormat The serialization format of the data. If set to <var>null</var>, the format will be
	 *                   automatically determined by examining the file name extension of the supplied File.
	 * @param contexts   The contexts to add the data to. Note that this parameter is a vararg and as such is optional.
	 *                   If no contexts are specified, the data is added to any context specified in the actual data
	 *                   file, or if the data contains no context, it is added without context. If one or more contexts
	 *                   are specified the data is added to these contexts, ignoring any context information in the data
	 *                   itself.
	 *
	 * @throws IOException                  If an I/O error occurred while reading from the file.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 *
	 * @since 3.5.0
	 */
	default void add(File file, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		add(file, null, dataFormat, contexts);
	}

	/**
	 * Adds RDF data from the specified file to a specific contexts in the repository.
	 *
	 * @param file       A file containing RDF data.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. This defaults to the
	 *                   value of {@link java.io.File#toURI() file.toURI()} if the value is set to <var>null</var>.
	 *                   <p>
	 *                   Note that if the data contains an embedded base URI, that embedded base URI will overrule the
	 *                   value supplied here (see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a> section
	 *                   5.1 for details).
	 * @param dataFormat The serialization format of the data. If set to <var>null</var>, the format will be
	 *                   automatically determined by examining the file name extension of the supplied File.
	 * @param contexts   The contexts to add the data to. Note that this parameter is a vararg and as such is optional.
	 *                   If no contexts are specified, the data is added to any context specified in the actual data
	 *                   file, or if the data contains no context, it is added without context. If one or more contexts
	 *                   are specified the data is added to these contexts, ignoring any context information in the data
	 *                   itself.
	 * @throws IOException                  If an I/O error occurred while reading from the file.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 */
	void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException;

	/**
	 * Adds a statement with the specified subject, predicate and object to this repository, optionally to one or more
	 * named contexts.
	 *
	 * @param subject   The statement's subject.
	 * @param predicate The statement's predicate.
	 * @param object    The statement's object.
	 * @param contexts  The contexts to add the data to. Note that this parameter is a vararg and as such is optional.
	 *                  If no contexts are specified, the data is added to any context specified in the actual data
	 *                  file, or if the data contains no context, it is added without context. If one or more contexts
	 *                  are specified the data is added to these contexts, ignoring any context information in the data
	 *                  itself.
	 * @throws RepositoryException If the data could not be added to the repository, for example because the repository
	 *                             is not writable.
	 */
	void add(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException;

	/**
	 * Adds the supplied statement to this repository, optionally to one or more named contexts.
	 *
	 * @param st       The statement to add.
	 * @param contexts The contexts to add the statements to. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are specified, the statement is added to any context specified in each
	 *                 statement, or if the statement contains no context, it is added without context. If one or more
	 *                 contexts are specified the statement is added to these contexts, ignoring any context information
	 *                 in the statement itself.
	 * @throws RepositoryException If the statement could not be added to the repository, for example because the
	 *                             repository is not writable.
	 */
	void add(Statement st, Resource... contexts) throws RepositoryException;

	/**
	 * Adds the supplied statements to this repository, optionally to one or more named contexts.
	 *
	 * @param statements The statements that should be added.
	 * @param contexts   The contexts to add the statements to. Note that this parameter is a vararg and as such is
	 *                   optional. If no contexts are specified, each statement is added to any context specified in the
	 *                   statement, or if the statement contains no context, it is added without context. If one or more
	 *                   contexts are specified each statement is added to these contexts, ignoring any context
	 *                   information in the statement itself. ignored.
	 * @throws RepositoryException If the statements could not be added to the repository, for example because the
	 *                             repository is not writable.
	 */
	void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException;

	/**
	 * Adds the supplied statements to this repository, optionally to one or more named contexts.
	 *
	 * @param statements The statements to add. In case the iteration is a
	 *                   {@link org.eclipse.rdf4j.common.iteration.CloseableIteration}, it will be closed before this
	 *                   method returns.
	 * @param contexts   The contexts to add the statements to. Note that this parameter is a vararg and as such is
	 *                   optional. If no contexts are specified, each statement is added to any context specified in the
	 *                   statement, or if the statement contains no context, it is added without context. If one or more
	 *                   contexts are specified each statement is added to these contexts, ignoring any context
	 *                   information in the statement itself. ignored.
	 * @throws RepositoryException If the statements could not be added to the repository, for example because the
	 *                             repository is not writable.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	<E extends Exception> void add(Iteration<? extends Statement, E> statements, Resource... contexts)
			throws RepositoryException, E;

	/**
	 * Adds the supplied statements to this repository, optionally to one or more named contexts.
	 *
	 * @param statements The statements to add. The iteration will be closed.
	 * @param contexts   The contexts to add the statements to. Note that this parameter is a vararg and as such is
	 *                   optional. If no contexts are specified, each statement is added to any context specified in the
	 *                   statement, or if the statement contains no context, it is added without context. If one or more
	 *                   contexts are specified each statement is added to these contexts, ignoring any context
	 *                   information in the statement itself. ignored.
	 * @throws RepositoryException If the statements could not be added to the repository, for example because the
	 *                             repository is not writable.
	 */
	default <E extends Exception> void add(CloseableIteration<? extends Statement, E> statements, Resource... contexts)
			throws RepositoryException, E {
		add(((Iteration<? extends Statement, E>) statements), contexts);
	}

	/**
	 * Adds the supplied statements to this repository, optionally to one or more named contexts.
	 *
	 * @param statements The statements to add. The @{link RepositoryResult} will be closed before this method returns.
	 * @param contexts   The contexts to add the statements to. Note that this parameter is a vararg and as such is
	 *                   optional. If no contexts are specified, each statement is added to any context specified in the
	 *                   statement, or if the statement contains no context, it is added without context. If one or more
	 *                   contexts are specified each statement is added to these contexts, ignoring any context
	 *                   information in the statement itself. ignored.
	 * @throws RepositoryException If the statements could not be added to the repository, for example because the
	 *                             repository is not writable.
	 */
	default void add(RepositoryResult<Statement> statements, Resource... contexts)
			throws RepositoryException {
		add((CloseableIteration<Statement, RepositoryException>) statements, contexts);
	}

	/**
	 * Removes the statement(s) with the specified subject, predicate and object from the repository, optionally
	 * restricted to the specified contexts.
	 *
	 * @param subject   The statement's subject, or <var>null</var> for a wildcard.
	 * @param predicate The statement's predicate, or <var>null</var> for a wildcard.
	 * @param object    The statement's object, or <var>null</var> for a wildcard.
	 * @param contexts  The context(s) to remove the data from. Note that this parameter is a vararg and as such is
	 *                  optional. If no contexts are supplied the method operates on the entire repository.
	 * @throws RepositoryException If the statement(s) could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 */
	void remove(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException;

	/**
	 * Removes the supplied statement from the specified contexts in the repository.
	 *
	 * @param st       The statement to remove.
	 * @param contexts The context(s) to remove the data from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the contexts associated with the
	 *                 statement itself, and if no context is associated with the statement, on the entire repository.
	 * @throws RepositoryException If the statement could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 */
	void remove(Statement st, Resource... contexts) throws RepositoryException;

	/**
	 * Removes the supplied statements from the specified contexts in this repository.
	 *
	 * @param statements The statements that should be added.
	 * @param contexts   The context(s) to remove the data from. Note that this parameter is a vararg and as such is
	 *                   optional. If no contexts are supplied the method operates on the contexts associated with the
	 *                   statement itself, and if no context is associated with the statement, on the entire repository.
	 * @throws RepositoryException If the statements could not be added to the repository, for example because the
	 *                             repository is not writable.
	 */
	void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException;

	/**
	 * Removes the supplied statements from a specific context in this repository, ignoring any context information
	 * carried by the statements themselves.
	 *
	 * @param statements The statements to remove. In case the iteration is a
	 *                   {@link org.eclipse.rdf4j.common.iteration.CloseableIteration}, it will be closed before this
	 *                   method returns.
	 * @param contexts   The context(s) to remove the data from. Note that this parameter is a vararg and as such is
	 *                   optional. If no contexts are supplied the method operates on the contexts associated with the
	 *                   statement itself, and if no context is associated with the statement, on the entire repository.
	 * @throws RepositoryException If the statements could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	<E extends Exception> void remove(Iteration<? extends Statement, E> statements, Resource... contexts)
			throws RepositoryException, E;

	/**
	 * Removes the supplied statements from a specific context in this repository, ignoring any context information
	 * carried by the statements themselves.
	 *
	 * @param statements The statements to remove. The iteration will be closed.
	 * @param contexts   The context(s) to remove the data from. Note that this parameter is a vararg and as such is
	 *                   optional. If no contexts are supplied the method operates on the contexts associated with the
	 *                   statement itself, and if no context is associated with the statement, on the entire repository.
	 * @throws RepositoryException If the statements could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 */
	default <E extends Exception> void remove(CloseableIteration<? extends Statement, E> statements,
			Resource... contexts)
			throws RepositoryException, E {
		remove((Iteration<Statement, RepositoryException>) statements, contexts);
	}

	/**
	 * Removes the supplied statements from a specific context in this repository, ignoring any context information
	 * carried by the statements themselves.
	 *
	 * @param statements The statements to remove. The {@link RepositoryResult} will be closed before this method
	 *                   returns.
	 * @param contexts   The context(s) to remove the data from. Note that this parameter is a vararg and as such is
	 *                   optional. If no contexts are supplied the method operates on the contexts associated with the
	 *                   statement itself, and if no context is associated with the statement, on the entire repository.
	 * @throws RepositoryException If the statements could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 */
	default void remove(RepositoryResult<Statement> statements, Resource... contexts)
			throws RepositoryException {
		remove((CloseableIteration<Statement, RepositoryException>) statements, contexts);
	}

	/**
	 * Removes all statements from a specific contexts in the repository.
	 *
	 * @param contexts The context(s) to remove the data from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @throws RepositoryException If the statements could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 */
	void clear(Resource... contexts) throws RepositoryException;

	/**
	 * Gets all declared namespaces as a RepositoryResult of {@link Namespace} objects. Each Namespace object consists
	 * of a prefix and a namespace name.
	 *
	 * @return A RepositoryResult containing Namespace objects. Care should be taken to close the RepositoryResult after
	 *         use.
	 * @throws RepositoryException If the namespaces could not be read from the repository.
	 */
	RepositoryResult<Namespace> getNamespaces() throws RepositoryException;

	/**
	 * Gets the namespace that is associated with the specified prefix, if any.
	 *
	 * @param prefix A namespace prefix, or an empty string in case of the default namespace.
	 * @return The namespace name that is associated with the specified prefix, or <var>null</var> if there is no such
	 *         namespace.
	 * @throws RepositoryException  If the namespace could not be read from the repository.
	 * @throws NullPointerException In case <var>prefix</var> is <var>null</var>.
	 */
	String getNamespace(String prefix) throws RepositoryException;

	/**
	 * Sets the prefix for a namespace.
	 *
	 * @param prefix The new prefix, or an empty string in case of the default namespace.
	 * @param name   The namespace name that the prefix maps to.
	 * @throws RepositoryException  If the namespace could not be set in the repository, for example because the
	 *                              repository is not writable.
	 * @throws NullPointerException In case <var>prefix</var> or <var>name</var> is <var>null</var>.
	 */
	void setNamespace(String prefix, String name) throws RepositoryException;

	/**
	 * Removes a namespace declaration by removing the association between a prefix and a namespace name.
	 *
	 * @param prefix The namespace prefix, or an empty string in case of the default namespace.
	 * @throws RepositoryException  If the namespace prefix could not be removed.
	 * @throws NullPointerException In case <var>prefix</var> is <var>null</var>.
	 */
	void removeNamespace(String prefix) throws RepositoryException;

	/**
	 * Removes all namespace declarations from the repository.
	 *
	 * @throws RepositoryException If the namespace declarations could not be removed.
	 */
	void clearNamespaces() throws RepositoryException;

}
