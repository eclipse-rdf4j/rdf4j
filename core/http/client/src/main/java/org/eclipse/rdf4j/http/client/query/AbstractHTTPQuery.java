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
package org.eclipse.rdf4j.http.client.query;

import java.util.Iterator;

import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.AbstractQuery;

/**
 * Base class for any {@link Query} operation over HTTP.
 *
 * @author Andreas Schwarte
 */
public abstract class AbstractHTTPQuery extends AbstractQuery {

	private final SPARQLProtocolSession httpClient;

	protected final QueryLanguage queryLanguage;

	protected final String queryString;

	protected final String baseURI;

	private String forcedLmdbExecutionStrategy;

	protected AbstractHTTPQuery(SPARQLProtocolSession httpClient, QueryLanguage queryLanguage, String queryString,
			String baseURI) {
		super();
		this.httpClient = httpClient;
		this.queryLanguage = queryLanguage;
		this.queryString = queryString;
		// TODO think about the following
		// for legacy reasons we should support the empty string for baseURI
		// this is used in the SPARQL repository in several places, e.g. in
		// getStatements
		this.baseURI = baseURI != null && !baseURI.isEmpty() ? baseURI : null;
	}

	/**
	 * @return Returns the {@link SPARQLProtocolSession} to be used for all HTTP based interaction
	 */
	protected SPARQLProtocolSession getHttpClient() {
		return httpClient;
	}

	/**
	 * Requests that an LMDB backed server run this query with one specific, named execution strategy instead of its own
	 * adaptive selection. Every spelling of "no strategy" accepted by
	 * {@link Protocol#isLmdbForcedExecutionStrategyUnset(String)} — {@code null}, blank, and
	 * {@link Protocol#LMDB_FORCED_EXECUTION_STRATEGY_NOT_ACTIVATED} — means "do not force anything". A server whose
	 * repository is not LMDB backed ignores the request.
	 */
	public void setForcedLmdbExecutionStrategy(String strategyOrNull) {
		this.forcedLmdbExecutionStrategy = strategyOrNull;
	}

	/** The strategy set by {@link #setForcedLmdbExecutionStrategy(String)}, or {@code null} when none was set. */
	public String getForcedLmdbExecutionStrategy() {
		return forcedLmdbExecutionStrategy;
	}

	/**
	 * Pushes {@link #getForcedLmdbExecutionStrategy()} onto the session that is about to carry this query. The value
	 * lives on the session because it travels as an ordinary request parameter assembled there, next to {@code infer} —
	 * see {@code RDF4JProtocolSession.getQueryMethodParameters}. Concrete subclasses call this immediately before each
	 * {@code send*Query} call. Sessions that do not speak the RDF4J protocol (a plain SPARQL endpoint) have no such
	 * parameter, so nothing is sent for them.
	 */
	protected void applyForcedLmdbExecutionStrategy() {
		if (getHttpClient()instanceof RDF4JProtocolSession session) {
			session.setForcedLmdbExecutionStrategy(forcedLmdbExecutionStrategy);
		}
	}

	public Binding[] getBindingsArray() {
		BindingSet bindings = this.getBindings();

		Binding[] bindingsArray = new Binding[bindings.size()];

		Iterator<Binding> iter = bindings.iterator();
		for (int i = 0; i < bindings.size(); i++) {
			bindingsArray[i] = iter.next();
		}

		return bindingsArray;
	}

	@Override
	public void setMaxExecutionTime(int maxExecutionTimeSeconds) {
		super.setMaxExecutionTime(maxExecutionTimeSeconds);
		// maxExecutionTimeSeconds is propagated as a per-request response timeout on the HTTP connection:
		// concrete subclasses pass getMaxExecutionTime() to SPARQLProtocolSession, which sets it on the
		// HttpRequest via HttpRequest.Builder#responseTimeout(Duration). The ApacheHC5RDF4JHttpClient
		// implementation applies it as a per-request RequestConfig#setResponseTimeout override.
	}

	@Override
	public String toString() {
		return queryString;
	}
}
