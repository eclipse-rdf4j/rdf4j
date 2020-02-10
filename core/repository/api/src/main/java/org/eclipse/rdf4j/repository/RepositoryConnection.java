/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
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
 * Main interface for updating data in and performing queries on an RDF4J {@link Repository}. By default, a
 * RepositoryConnection is in auto-commit mode, meaning that each operation corresponds to a single transaction on the
 * underlying store. Multiple operations can be bundled in a single transaction by using {@link #begin()} and
 * {@link #commit() commit}/ {@link #rollback() rollback}. Care should be taking to always properly close a
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
	public Repository getRepository();

	/**
	 * Set the parser configuration this connection should use for RDFParser-based operations.
	 * 
	 * @param config a Rio RDF Parser configuration.
	 */
	public void setParserConfig(ParserConfig config);

	/**
	 * Returns the parser configuration this connection uses for Rio-based operations.
	 * 
	 * @return a Rio RDF parser configuration.
	 */
	public ParserConfig getParserConfig();

	/**
	 * Gets a ValueFactory for this RepositoryConnection.
	 * 
	 * @return A repository-specific ValueFactory.
	 */
	public ValueFactory getValueFactory();

	/**
	 * Checks whether this connection is open. A connection is open from the moment it is created until it is closed.
	 * 
	 * @see #close()
	 */
	public boolean isOpen() throws RepositoryException;

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
	public default void close() throws RepositoryException {
		if (isOpen() && isActive()) {
			rollback();
		}
	}

	/**
	 * Prepares a SPARQL query for evaluation on this repository (optional operation). In case the query contains
	 * relative URIs that need to be resolved against an external base URI, one should use
	 * {@link #prepareQuery(QueryLanguage, String, String)} instead.
	 * 
	 * @param query The query string, in SPARQL syntax.
	 * @return A query ready to be evaluated on this repository.
	 * @throws MalformedQueryException       If the supplied query is malformed.
	 * @throws UnsupportedOperationException If the <tt>prepareQuery</tt> method is not supported by this repository.
	 * @see #prepareQuery(QueryLanguage, String)
	 */
	public default Query prepareQuery(String query) throws RepositoryException, MalformedQueryException {
		return prepareQuery(QueryLanguage.SPARQL, query);
	}

	/**
	 * Prepares a query for evaluation on this repository (optional operation). In case the query contains relative URIs
	 * that need to be resolved against an external base URI, one should use
	 * {@link #prepareQuery(QueryLanguage, String, String)} instead.
	 * 
	 * @param ql    The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query The query string.
	 * @return A query ready to be evaluated on this repository.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 * @throws UnsupportedOperationException     If the <tt>prepareQuery</tt> method is not supported by this
	 *                                           repository.
	 */
	public Query prepareQuery(QueryLanguage ql, String query) throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares a query for evaluation on this repository (optional operation).
	 * 
	 * @param ql      The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query   The query string.
	 * @param baseURI The base URI to resolve any relative URIs that are in the query against, can be <tt>null</tt> if
	 *                the query does not contain any relative URIs.
	 * @return A query ready to be evaluated on this repository.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 * @throws UnsupportedOperationException     If the <tt>prepareQuery</tt> method is not supported by this
	 *                                           repository.
	 */
	public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
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
	public default TupleQuery prepareTupleQuery(String query) throws RepositoryException, MalformedQueryException {
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
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares a query that produces sets of value tuples.
	 * 
	 * @param ql      The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query   The query string.
	 * @param baseURI The base URI to resolve any relative URIs that are in the query against, can be <tt>null</tt> if
	 *                the query does not contain any relative URIs.
	 * @return a {@link TupleQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException          If the supplied query is not a tuple query.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 */
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI)
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
	public default GraphQuery prepareGraphQuery(String query) throws RepositoryException, MalformedQueryException {
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
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares queries that produce RDF graphs.
	 * 
	 * @param ql      The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query   The query string.
	 * @param baseURI The base URI to resolve any relative URIs that are in the query against, can be <tt>null</tt> if
	 *                the query does not contain any relative URIs.
	 * @return a {@link GraphQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException          If the supplied query is not a graph query.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 */
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares SPARQL queries that return <tt>true</tt> or <tt>false</tt>, that is, SPARQL ASK queries. In case the
	 * query contains relative URIs that need to be resolved against an external base URI, one should use
	 * {@link #prepareBooleanQuery(QueryLanguage, String, String)} instead.
	 * 
	 * @param query The query string, in SPARQL syntax.
	 * @return a {@link BooleanQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException If the supplied query is not a boolean query.
	 * @throws MalformedQueryException  If the supplied SPARQL query is malformed.
	 * @see #prepareBooleanQuery(QueryLanguage, String)
	 */
	public default BooleanQuery prepareBooleanQuery(String query) throws RepositoryException, MalformedQueryException {
		return prepareBooleanQuery(QueryLanguage.SPARQL, query);
	}

	/**
	 * Prepares queries that return <tt>true</tt> or <tt>false</tt>. In case the query contains relative URIs that need
	 * to be resolved against an external base URI, one should use
	 * {@link #prepareBooleanQuery(QueryLanguage, String, String)} instead.
	 * 
	 * @param ql    The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query The query string.
	 * @return a {@link BooleanQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException          If the supplied query is not a boolean query.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 */
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares queries that return <tt>true</tt> or <tt>false</tt>.
	 * 
	 * @param ql      The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query   The query string.
	 * @param baseURI The base URI to resolve any relative URIs that are in the query against, can be <tt>null</tt> if
	 *                the query does not contain any relative URIs.
	 * @return a {@link BooleanQuery} ready to be evaluated on this {@link RepositoryConnection}.
	 * @throws IllegalArgumentException          If the supplied query is not a boolean query.
	 * @throws MalformedQueryException           If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException If the supplied query language is not supported.
	 */
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI)
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
	public default Update prepareUpdate(String update) throws RepositoryException, MalformedQueryException {
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
	public Update prepareUpdate(QueryLanguage ql, String update) throws RepositoryException, MalformedQueryException;

	/**
	 * Prepares an Update operation.
	 * 
	 * @param ql      The {@link QueryLanguage query language} in which the update operation is formulated.
	 * @param update  The update operation string.
	 * @param baseURI The base URI to resolve any relative URIs that are in the update against, can be <tt>null</tt> if
	 *                the update does not contain any relative URIs.
	 * @return a {@link Update} ready to be executed on this {@link RepositoryConnection}.
	 * @throws MalformedQueryException If the supplied update operation string is malformed.
	 */
	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
			throws RepositoryException, MalformedQueryException;

	/**
	 * Gets all resources that are used as content identifiers. Care should be taken that the returned
	 * {@link RepositoryResult} is closed to free any resources that it keeps hold of.
	 * 
	 * @return a RepositoryResult object containing Resources that are used as context identifiers.
	 */
	public RepositoryResult<Resource> getContextIDs() throws RepositoryException;

	/**
	 * Gets all statements with a specific subject, predicate and/or object from the repository. The result is
	 * optionally restricted to the specified set of named contexts. If the repository supports inferencing, inferred
	 * statements will be included in the result.
	 * 
	 * @param subj     A Resource specifying the subject, or <tt>null</tt> for a wildcard.
	 * @param pred     A URI specifying the predicate, or <tt>null</tt> for a wildcard.
	 * @param obj      A Value specifying the object, or <tt>null</tt> for a wildcard.
	 * @param contexts The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @return The statements matching the specified pattern. The result object is a {@link RepositoryResult} object, a
	 *         lazy Iterator-like object containing {@link Statement}s and optionally throwing a
	 *         {@link RepositoryException} when an error when a problem occurs during retrieval.
	 */
	public default RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws RepositoryException {
		return getStatements(subj, pred, obj, true, contexts);
	}

	/**
	 * Gets all statements with a specific subject, predicate and/or object from the repository. The result is
	 * optionally restricted to the specified set of named contexts.
	 * 
	 * @param subj            A Resource specifying the subject, or <tt>null</tt> for a wildcard.
	 * @param pred            A URI specifying the predicate, or <tt>null</tt> for a wildcard.
	 * @param obj             A Value specifying the object, or <tt>null</tt> for a wildcard.
	 * @param contexts        The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                        optional. If no contexts are supplied the method operates on the entire repository.
	 * @param includeInferred if false, no inferred statements are returned; if true, inferred statements are returned
	 *                        if available. The default is true.
	 * @return The statements matching the specified pattern. The result object is a {@link RepositoryResult} object, a
	 *         lazy Iterator-like object containing {@link Statement}s and optionally throwing a
	 *         {@link RepositoryException} when an error when a problem occurs during retrieval.
	 * @deprecated since 2.0. Use {@link #getStatements(Resource, IRI, Value, boolean, Resource...)} instead.
	 */
	@Deprecated
	public default RepositoryResult<Statement> getStatements(Resource subj, URI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws RepositoryException {
		return getStatements(subj, (IRI) pred, obj, includeInferred, contexts);
	}

	/**
	 * Gets all statements with a specific subject, predicate and/or object from the repository. The result is
	 * optionally restricted to the specified set of named contexts.
	 * 
	 * @param subj            A Resource specifying the subject, or <tt>null</tt> for a wildcard.
	 * @param pred            An IRI specifying the predicate, or <tt>null</tt> for a wildcard.
	 * @param obj             A Value specifying the object, or <tt>null</tt> for a wildcard.
	 * @param contexts        The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                        optional. If no contexts are supplied the method operates on the entire repository.
	 * @param includeInferred if false, no inferred statements are returned; if true, inferred statements are returned
	 *                        if available. The default is true.
	 * @return The statements matching the specified pattern. The result object is a {@link RepositoryResult} object, a
	 *         lazy Iterator-like object containing {@link Statement}s and optionally throwing a
	 *         {@link RepositoryException} when an error when a problem occurs during retrieval.
	 */
	public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException;

	/**
	 * Checks whether the repository contains statements with a specific subject, predicate and/or object, optionally in
	 * the specified contexts.
	 * 
	 * @param subj            A Resource specifying the subject, or <tt>null</tt> for a wildcard.
	 * @param pred            An IRI specifying the predicate, or <tt>null</tt> for a wildcard.
	 * @param obj             A Value specifying the object, or <tt>null</tt> for a wildcard.
	 * @param contexts        The context(s) the need to be searched. Note that this parameter is a vararg and as such
	 *                        is optional. If no contexts are supplied the method operates on the entire repository.
	 * @param includeInferred if false, no inferred statements are considered; if true, inferred statements are
	 *                        considered if available
	 * @return true If a matching statement is in the repository in the specified context, false otherwise.
	 */
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException;

	/**
	 * Checks whether the repository contains statements with a specific subject, predicate and/or object, optionally in
	 * the specified contexts.
	 * 
	 * @param subj            A Resource specifying the subject, or <tt>null</tt> for a wildcard.
	 * @param pred            A URI specifying the predicate, or <tt>null</tt> for a wildcard.
	 * @param obj             A Value specifying the object, or <tt>null</tt> for a wildcard.
	 * @param contexts        The context(s) the need to be searched. Note that this parameter is a vararg and as such
	 *                        is optional. If no contexts are supplied the method operates on the entire repository.
	 * @param includeInferred if false, no inferred statements are considered; if true, inferred statements are
	 *                        considered if available
	 * @return true If a matching statement is in the repository in the specified context, false otherwise.
	 * @deprecated since 2.0. Use {@link #hasStatement(Resource, IRI, Value, boolean, Resource...)} instead.
	 */
	@Deprecated
	public default boolean hasStatement(Resource subj, URI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		return hasStatement(subj, (IRI) pred, obj, includeInferred, contexts);
	}

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
	public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts) throws RepositoryException;

	/**
	 * Exports all statements with a specific subject, predicate and/or object from the repository, optionally from the
	 * specified contexts.
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
	public void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
			Resource... contexts) throws RepositoryException, RDFHandlerException;

	/**
	 * Exports all explicit statements in the specified contexts to the supplied RDFHandler.
	 * 
	 * @param contexts The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @param handler  The handler that will handle the RDF data.
	 * @throws RDFHandlerException If the handler encounters an unrecoverable error.
	 */
	public void export(RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException;

	/**
	 * Returns the number of (explicit) statements that are in the specified contexts in this repository.
	 * 
	 * @param contexts The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @return The number of explicit statements from the specified contexts in this repository.
	 */
	public long size(Resource... contexts) throws RepositoryException;

	/**
	 * Returns <tt>true</tt> if this repository does not contain any (explicit) statements.
	 * 
	 * @return <tt>true</tt> if this repository is empty, <tt>false</tt> otherwise.
	 * @throws RepositoryException If the repository could not be checked to be empty.
	 */
	public boolean isEmpty() throws RepositoryException;

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
	public void setAutoCommit(boolean autoCommit) throws RepositoryException;

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
	public boolean isAutoCommit() throws RepositoryException;

	/**
	 * Indicates if a transaction is currently active on the connection. A transaction is active if {@link #begin()} has
	 * been called, and becomes inactive after {@link #commit()} or {@link #rollback()} has been called.
	 * 
	 * @return <code>true</code> iff a transaction is active, <code>false</code> iff no transaction is active.
	 * @throws UnknownTransactionStateException if the transaction state can not be determined. This can happen for
	 *                                          instance when communication with a repository fails or times out.
	 * @throws RepositoryException
	 */
	public boolean isActive() throws UnknownTransactionStateException, RepositoryException;

	/**
	 * Sets the transaction isolation level for the next transaction(s) on this connection. If the level is set to a
	 * value that is not supported by the underlying repository, this method will still succeed but a subsequent call to
	 * {@link #begin()} will result in an exception.
	 * 
	 * @param level the transaction isolation level to set.
	 * @throws IllegalStateException if the method is called while a transaction is already active.
	 */
	public void setIsolationLevel(IsolationLevel level) throws IllegalStateException;

	/**
	 * Retrieves the current {@link IsolationLevel transaction isolation level} of the connection.
	 * 
	 * @return the current transaction isolation level.
	 */
	public IsolationLevel getIsolationLevel();

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
	public void begin() throws RepositoryException;

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
	public void begin(IsolationLevel level) throws RepositoryException;

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
	 */
	public void commit() throws RepositoryException;

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
	public void rollback() throws RepositoryException;

	/**
	 * Adds RDF data from an InputStream to the repository, optionally to one or more named contexts.
	 * 
	 * @param in         An InputStream from which RDF data can be read.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against.
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
	 */
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException;

	/**
	 * Adds RDF data from a Reader to the repository, optionally to one or more named contexts. <b>Note: using a Reader
	 * to upload byte-based data means that you have to be careful not to destroy the data's character encoding by
	 * enforcing a default character encoding upon the bytes. If possible, adding such data using an InputStream is to
	 * be preferred.</b>
	 * 
	 * @param reader     A Reader from which RDF data can be read.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against.
	 * @param dataFormat The serialization format of the data.
	 * @param contexts   The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                   these contexts, ignoring any context information in the data itself.
	 * @throws IOException                  If an I/O error occurred while reading from the reader.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 */
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException;

	/**
	 * Adds the RDF data that can be found at the specified URL to the repository, optionally to one or more named
	 * contexts.
	 * 
	 * @param url        The URL of the RDF data.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. This defaults to the
	 *                   value of {@link java.net.URL#toExternalForm() url.toExternalForm()} if the value is set to
	 *                   <tt>null</tt>.
	 * @param dataFormat The serialization format of the data. If set to <tt>null</tt>, the format will be automatically
	 *                   determined by examining the content type in the HTTP response header, and failing that, the
	 *                   file name extension of the supplied URL.
	 * @param contexts   The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                   these contexts, ignoring any context information in the data itself.
	 * @throws IOException                  If an I/O error occurred while reading from the URL.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format, or the RDF format
	 *                                      could not be automatically determined.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RepositoryException          If the data could not be added to the repository, for example because the
	 *                                      repository is not writable.
	 */
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException;

	/**
	 * Adds RDF data from the specified file to a specific contexts in the repository.
	 * 
	 * @param file       A file containing RDF data.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. This defaults to the
	 *                   value of {@link java.io.File#toURI() file.toURI()} if the value is set to <tt>null</tt>.
	 * @param dataFormat The serialization format of the data.
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
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
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
	public void add(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException;

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
	 * @deprecated since 2.0. Use {@link #add(Resource, IRI, Value, Resource...)} instead.
	 */
	@Deprecated
	public default void add(Resource subject, URI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		this.add(subject, (IRI) predicate, object, contexts);
	}

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
	public void add(Statement st, Resource... contexts) throws RepositoryException;

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
	public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException;

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
	public <E extends Exception> void add(Iteration<? extends Statement, E> statements, Resource... contexts)
			throws RepositoryException, E;

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
		add((Iteration<Statement, RepositoryException>) statements, contexts);
	}

	/**
	 * Removes the statement(s) with the specified subject, predicate and object from the repository, optionally
	 * restricted to the specified contexts.
	 * 
	 * @param subject   The statement's subject, or <tt>null</tt> for a wildcard.
	 * @param predicate The statement's predicate, or <tt>null</tt> for a wildcard.
	 * @param object    The statement's object, or <tt>null</tt> for a wildcard.
	 * @param contexts  The context(s) to remove the data from. Note that this parameter is a vararg and as such is
	 *                  optional. If no contexts are supplied the method operates on the entire repository.
	 * @throws RepositoryException If the statement(s) could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 */
	public void remove(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException;

	/**
	 * Removes the statement(s) with the specified subject, predicate and object from the repository, optionally
	 * restricted to the specified contexts.
	 * 
	 * @param subject   The statement's subject, or <tt>null</tt> for a wildcard.
	 * @param predicate The statement's predicate, or <tt>null</tt> for a wildcard.
	 * @param object    The statement's object, or <tt>null</tt> for a wildcard.
	 * @param contexts  The context(s) to remove the data from. Note that this parameter is a vararg and as such is
	 *                  optional. If no contexts are supplied the method operates on the entire repository.
	 * @throws RepositoryException If the statement(s) could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 * @deprecated since 2.0. Use {@link #remove(Resource, IRI, Value, Resource...)} instead.
	 */
	@Deprecated
	public default void remove(Resource subject, URI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		this.remove(subject, (IRI) predicate, object, contexts);
	}

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
	public void remove(Statement st, Resource... contexts) throws RepositoryException;

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
	public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException;

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
	public <E extends Exception> void remove(Iteration<? extends Statement, E> statements, Resource... contexts)
			throws RepositoryException, E;

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
		remove((Iteration<Statement, RepositoryException>) statements, contexts);
	}

	/**
	 * Removes all statements from a specific contexts in the repository.
	 * 
	 * @param contexts The context(s) to remove the data from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @throws RepositoryException If the statements could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 */
	public void clear(Resource... contexts) throws RepositoryException;

	/**
	 * Gets all declared namespaces as a RepositoryResult of {@link Namespace} objects. Each Namespace object consists
	 * of a prefix and a namespace name.
	 * 
	 * @return A RepositoryResult containing Namespace objects. Care should be taken to close the RepositoryResult after
	 *         use.
	 * @throws RepositoryException If the namespaces could not be read from the repository.
	 */
	public RepositoryResult<Namespace> getNamespaces() throws RepositoryException;

	/**
	 * Gets the namespace that is associated with the specified prefix, if any.
	 * 
	 * @param prefix A namespace prefix, or an empty string in case of the default namespace.
	 * @return The namespace name that is associated with the specified prefix, or <tt>null</tt> if there is no such
	 *         namespace.
	 * @throws RepositoryException  If the namespace could not be read from the repository.
	 * @throws NullPointerException In case <tt>prefix</tt> is <tt>null</tt>.
	 */
	public String getNamespace(String prefix) throws RepositoryException;

	/**
	 * Sets the prefix for a namespace.
	 * 
	 * @param prefix The new prefix, or an empty string in case of the default namespace.
	 * @param name   The namespace name that the prefix maps to.
	 * @throws RepositoryException  If the namespace could not be set in the repository, for example because the
	 *                              repository is not writable.
	 * @throws NullPointerException In case <tt>prefix</tt> or <tt>name</tt> is <tt>null</tt>.
	 */
	public void setNamespace(String prefix, String name) throws RepositoryException;

	/**
	 * Removes a namespace declaration by removing the association between a prefix and a namespace name.
	 * 
	 * @param prefix The namespace prefix, or an empty string in case of the default namespace.
	 * @throws RepositoryException  If the namespace prefix could not be removed.
	 * @throws NullPointerException In case <tt>prefix</tt> is <tt>null</tt>.
	 */
	public void removeNamespace(String prefix) throws RepositoryException;

	/**
	 * Removes all namespace declarations from the repository.
	 * 
	 * @throws RepositoryException If the namespace declarations could not be removed.
	 */
	public void clearNamespaces() throws RepositoryException;

}
