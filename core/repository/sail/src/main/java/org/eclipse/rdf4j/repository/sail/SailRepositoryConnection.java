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
package org.eclipse.rdf4j.repository.sail;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.SessionManagerDependent;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryReadOnlyException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.RepositoryResolverClient;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.repository.base.AbstractRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailReadOnlyException;

/**
 * An implementation of the {@link RepositoryConnection} interface that wraps a {@link SailConnection}.
 *
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 */
public class SailRepositoryConnection extends AbstractRepositoryConnection implements FederatedServiceResolverClient,
		RepositoryResolverClient, HttpClientDependent, SessionManagerDependent {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The Sail connection wrapped by this repository connection object.
	 */
	private final SailConnection sailConnection;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new repository connection that will wrap the supplied SailConnection. SailRepositoryConnection objects
	 * are created by {@link SailRepository#getConnection}.
	 */
	protected SailRepositoryConnection(SailRepository repository, SailConnection sailConnection) {
		super(repository);
		this.sailConnection = sailConnection;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Returns the underlying SailConnection.
	 */
	public SailConnection getSailConnection() {
		return sailConnection;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		if (sailConnection instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient) sailConnection).setFederatedServiceResolver(resolver);
		}
	}

	@Override
	public void setRepositoryResolver(RepositoryResolver resolver) {
		if (sailConnection instanceof RepositoryResolverClient) {
			((RepositoryResolverClient) sailConnection).setRepositoryResolver(resolver);
		}
	}

	@Override
	public HttpClientSessionManager getHttpClientSessionManager() {
		if (sailConnection instanceof SessionManagerDependent) {
			return ((SessionManagerDependent) sailConnection).getHttpClientSessionManager();
		} else {
			return null;
		}
	}

	@Override
	public void setHttpClientSessionManager(HttpClientSessionManager client) {
		if (sailConnection instanceof SessionManagerDependent) {
			((SessionManagerDependent) sailConnection).setHttpClientSessionManager(client);
		}
	}

	@Override
	public HttpClient getHttpClient() {
		if (sailConnection instanceof HttpClientDependent) {
			return ((HttpClientDependent) sailConnection).getHttpClient();
		} else {
			return null;
		}
	}

	@Override
	public void setHttpClient(HttpClient client) {
		if (sailConnection instanceof HttpClientDependent) {
			((HttpClientDependent) sailConnection).setHttpClient(client);
		}
	}

	@Override
	public void begin() throws RepositoryException {
		try {
			// always call receiveTransactionSettings(...) before calling begin();
			sailConnection.setTransactionSettings(new TransactionSetting[0]);

			if (getIsolationLevel() != null) {
				sailConnection.begin(getIsolationLevel());
			} else {
				sailConnection.begin();
			}
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void begin(IsolationLevel level) throws RepositoryException {
		try {
			// always call receiveTransactionSettings(...) before calling begin();
			sailConnection.setTransactionSettings(new TransactionSetting[0]);

			if (level != null) {
				sailConnection.begin(level);
			} else {
				sailConnection.begin();
			}
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void begin(TransactionSetting... settings) {
		try {
			// Asserts to catch any of these issues in our tests. These asserts don't run in production since they are
			// slow. Nulls in the transaction settings or multiple isolation levels have undefined behaviour.
			assert Arrays.stream(settings).noneMatch(Objects::isNull) : "No transaction settings should be null!";
			assert Arrays.stream(settings)
					.filter(setting -> setting instanceof IsolationLevel)
					.count() <= 1 : "There should never be more than one isolation level";

			sailConnection.setTransactionSettings(settings);

			for (TransactionSetting setting : settings) {
				if (setting instanceof IsolationLevel) {
					sailConnection.begin((IsolationLevel) setting);
					return;
				}
			}

			// if none of the transaction settings are isolation levels
			sailConnection.begin();

		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void prepare() throws RepositoryException {
		try {
			sailConnection.flush();
			sailConnection.prepare();
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void commit() throws RepositoryException {
		try {
			sailConnection.flush();
			sailConnection.prepare();
			sailConnection.commit();
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void rollback() throws RepositoryException {
		try {
			sailConnection.rollback();
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void close() throws RepositoryException {
		try {
			super.close();
		} catch (SailException e) {
			throw new RepositoryException(e);
		} finally {
			sailConnection.close();
		}
	}

	@Override
	public boolean isOpen() throws RepositoryException {
		try {
			return sailConnection.isOpen();
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public SailQuery prepareQuery(QueryLanguage ql, String queryString, String baseURI) throws MalformedQueryException {
		ParsedQuery parsedQuery = QueryParserUtil.parseQuery(ql, queryString, baseURI);

		if (parsedQuery instanceof ParsedTupleQuery) {
			Optional<TupleExpr> sailTupleExpr = sailConnection.prepareQuery(ql, Query.QueryType.TUPLE, queryString,
					baseURI);
			if (sailTupleExpr.isPresent()) {
				parsedQuery = new ParsedTupleQuery(queryString, sailTupleExpr.get());
			}
			return new SailTupleQuery((ParsedTupleQuery) parsedQuery, this);
		} else if (parsedQuery instanceof ParsedGraphQuery) {
			Optional<TupleExpr> sailTupleExpr = sailConnection.prepareQuery(ql, Query.QueryType.GRAPH, queryString,
					baseURI);
			if (sailTupleExpr.isPresent()) {
				parsedQuery = new ParsedGraphQuery(queryString, sailTupleExpr.get());
			}
			return new SailGraphQuery((ParsedGraphQuery) parsedQuery, this);
		} else if (parsedQuery instanceof ParsedBooleanQuery) {
			Optional<TupleExpr> sailTupleExpr = sailConnection.prepareQuery(ql, Query.QueryType.BOOLEAN, queryString,
					baseURI);
			if (sailTupleExpr.isPresent()) {
				parsedQuery = new ParsedBooleanQuery(queryString, sailTupleExpr.get());
			}
			return new SailBooleanQuery((ParsedBooleanQuery) parsedQuery, this);
		} else {
			throw new RuntimeException("Unexpected query type: " + parsedQuery.getClass());
		}
	}

	@Override
	public SailTupleQuery prepareTupleQuery(QueryLanguage ql, String queryString, String baseURI)
			throws MalformedQueryException {
		Optional<TupleExpr> sailTupleExpr = sailConnection.prepareQuery(ql, Query.QueryType.TUPLE, queryString,
				baseURI);

		ParsedTupleQuery parsedQuery = sailTupleExpr
				.map(expr -> new ParsedTupleQuery(queryString, expr))
				.orElse(QueryParserUtil.parseTupleQuery(ql, queryString, baseURI));
		return new SailTupleQuery(parsedQuery, this);
	}

	@Override
	public SailGraphQuery prepareGraphQuery(QueryLanguage ql, String queryString, String baseURI)
			throws MalformedQueryException {
		Optional<TupleExpr> sailTupleExpr = sailConnection.prepareQuery(ql, Query.QueryType.GRAPH, queryString,
				baseURI);
		ParsedGraphQuery parsedQuery = sailTupleExpr
				.map(expr -> new ParsedGraphQuery(queryString, expr))
				.orElse(QueryParserUtil.parseGraphQuery(ql, queryString, baseURI));
		return new SailGraphQuery(parsedQuery, this);
	}

	@Override
	public SailBooleanQuery prepareBooleanQuery(QueryLanguage ql, String queryString, String baseURI)
			throws MalformedQueryException {
		Optional<TupleExpr> sailTupleExpr = sailConnection.prepareQuery(ql, Query.QueryType.BOOLEAN, queryString,
				baseURI);
		ParsedBooleanQuery parsedQuery = sailTupleExpr
				.map(expr -> new ParsedBooleanQuery(queryString, expr))
				.orElse(QueryParserUtil.parseBooleanQuery(ql, queryString, baseURI));
		return new SailBooleanQuery(parsedQuery, this);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
			throws RepositoryException, MalformedQueryException {
		ParsedUpdate parsedUpdate = QueryParserUtil.parseUpdate(ql, update, baseURI);
		return new SailUpdate(parsedUpdate, this);
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		return sailConnection.hasStatement(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
		try {
			return createRepositoryResult(sailConnection.getContextIDs());
		} catch (SailException e) {
			throw new RepositoryException("Unable to get context IDs from Sail", e);
		}
	}

	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		try {
			return createRepositoryResult(sailConnection.getStatements(subj, pred, obj, includeInferred, contexts));
		} catch (SailException e) {
			throw new RepositoryException("Unable to get statements from Sail", e);
		}
	}

	@Override
	public boolean isEmpty() throws RepositoryException {
		// The following is more efficient than "size() == 0" for Sails
		return !hasStatement(null, null, null, false);
	}

	@Override
	public void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
			Resource... contexts) throws RepositoryException, RDFHandlerException {
		handler.startRDF();

		// Export namespace information
		try (var nsIter = getNamespaces()) {
			while (nsIter.hasNext()) {
				Namespace ns = nsIter.next();
				handler.handleNamespace(ns.getPrefix(), ns.getName());
			}
		}

		// Export statements
		try (var stIter = getStatements(subj, pred, obj, includeInferred, contexts)) {
			while (stIter.hasNext()) {
				handler.handleStatement(stIter.next());
			}
		}

		handler.endRDF();
	}

	@Override
	public long size(Resource... contexts) throws RepositoryException {
		try {
			return sailConnection.size(contexts);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	protected void addWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		try {
			sailConnection.addStatement(subject, predicate, object, contexts);
		} catch (SailReadOnlyException e) {
			throw new RepositoryReadOnlyException(e.getMessage(), e);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	protected void removeWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		try {
			if (subject == null && predicate == null && object == null) {
				sailConnection.clear(contexts);
			} else {
				sailConnection.removeStatements(subject, predicate, object, contexts);
			}
		} catch (SailReadOnlyException e) {
			throw new RepositoryReadOnlyException(e.getMessage(), e);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		try {
			boolean local = startLocalTransaction();
			sailConnection.clear(contexts);
			conditionalCommit(local);
		} catch (SailReadOnlyException e) {
			throw new RepositoryReadOnlyException(e.getMessage(), e);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void setNamespace(String prefix, String name) throws RepositoryException {
		try {
			boolean local = startLocalTransaction();
			sailConnection.setNamespace(prefix, name);
			conditionalCommit(local);
		} catch (SailReadOnlyException e) {
			throw new RepositoryReadOnlyException(e.getMessage(), e);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		try {
			boolean local = startLocalTransaction();
			sailConnection.removeNamespace(prefix);
			conditionalCommit(local);
		} catch (SailReadOnlyException e) {
			throw new RepositoryReadOnlyException(e.getMessage(), e);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		try {
			boolean local = startLocalTransaction();
			sailConnection.clearNamespaces();
			conditionalCommit(local);
		} catch (SailReadOnlyException e) {
			throw new RepositoryReadOnlyException(e.getMessage(), e);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
		try {
			return createRepositoryResult(sailConnection.getNamespaces());
		} catch (SailException e) {
			throw new RepositoryException("Unable to get namespaces from Sail", e);
		}
	}

	@Override
	public String getNamespace(String prefix) throws RepositoryException {
		try {
			return sailConnection.getNamespace(prefix);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Wraps a CloseableIteration coming from a Sail in a RepositoryResult object, applying the required conversions
	 */
	protected <E> RepositoryResult<E> createRepositoryResult(CloseableIteration<? extends E, SailException> sailIter) {
		return new RepositoryResult<>(new SailCloseableIteration<E>(sailIter));
	}

	@Override
	public boolean isActive() throws UnknownTransactionStateException {
		try {
			return sailConnection.isActive();
		} catch (SailException e) {
			throw new UnknownTransactionStateException(e);
		}
	}

	@Override
	public String toString() {
		return getSailConnection().toString();
	}
}
