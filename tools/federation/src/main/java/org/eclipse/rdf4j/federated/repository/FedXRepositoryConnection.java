/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.repository;

import java.util.Collections;
import java.util.Set;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.structures.FedXBooleanQuery;
import org.eclipse.rdf4j.federated.structures.FedXGraphQuery;
import org.eclipse.rdf4j.federated.structures.FedXTupleQuery;
import org.eclipse.rdf4j.federated.structures.QueryType;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.parser.ParsedDescribeQuery;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailBooleanQuery;
import org.eclipse.rdf4j.repository.sail.SailGraphQuery;
import org.eclipse.rdf4j.repository.sail.SailQuery;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailTupleQuery;
import org.eclipse.rdf4j.sail.SailConnection;

import com.google.common.collect.Sets;

/**
 * A special {@link SailRepositoryConnection} which adds the original query string as binding to the returned query. The
 * binding name is defined by {@link #BINDING_ORIGINAL_QUERY} and is added to all query instances returned by the
 * available prepare methods.
 *
 * @author Andreas Schwarte
 *
 */
public class FedXRepositoryConnection extends SailRepositoryConnection {

	/**
	 * We add a binding to each parsed query mapping the original query in order to send the original query to the
	 * endpoint if there is only a single federation member is relevant for this query.
	 */
	public static final String BINDING_ORIGINAL_QUERY = "__originalQuery";
	public static final String BINDING_ORIGINAL_BASE_URI = "__originalBaseURI";
	public static final String BINDING_ORIGINAL_QUERY_TYPE = "__originalQueryType";
	public static final String BINDING_ORIGINAL_MAX_EXECUTION_TIME = "__originalQueryMaxExecutionTime";

	/**
	 * The number of bindings in the external binding set that are added by FedX.
	 *
	 * @see #BINDING_ORIGINAL_QUERY
	 * @see #BINDING_ORIGINAL_QUERY_TYPE
	 * @see #BINDING_ORIGINAL_MAX_EXECUTION_TIME
	 */
	public static final Set<String> FEDX_BINDINGS = Collections.unmodifiableSet(
			Sets.newHashSet(BINDING_ORIGINAL_QUERY, BINDING_ORIGINAL_BASE_URI, BINDING_ORIGINAL_QUERY_TYPE,
					BINDING_ORIGINAL_MAX_EXECUTION_TIME));

	private final FederationContext federationContext;

	protected FedXRepositoryConnection(FedXRepository repository,
			SailConnection sailConnection) {
		super(repository, sailConnection);
		this.federationContext = repository.getFederationContext();
	}

	@Override
	public SailQuery prepareQuery(QueryLanguage ql, String queryString,
			String baseURI) throws MalformedQueryException {
		SailQuery q = super.prepareQuery(ql, queryString, baseURI);
		if (q instanceof SailTupleQuery) {
			insertOriginalQueryString(q, queryString, baseURI, QueryType.SELECT);
			q = new FedXTupleQuery((SailTupleQuery) q);
		} else if (q instanceof SailGraphQuery) {
			insertOriginalQueryString(q, queryString, baseURI, determineGraphQueryType((SailGraphQuery) q));
			q = new FedXGraphQuery((SailGraphQuery) q);
		} else if (q instanceof SailBooleanQuery) {
			insertOriginalQueryString(q, queryString, baseURI, QueryType.ASK);
			q = new FedXBooleanQuery((SailBooleanQuery) q);
		}
		setIncludeInferredDefault(q);
		return q;
	}

	@Override
	public FedXTupleQuery prepareTupleQuery(QueryLanguage ql,
			String queryString, String baseURI) throws MalformedQueryException {
		SailTupleQuery q = super.prepareTupleQuery(ql, queryString, baseURI);
		insertOriginalQueryString(q, queryString, baseURI, QueryType.SELECT);
		setIncludeInferredDefault(q);
		return new FedXTupleQuery(q);
	}

	@Override
	public FedXGraphQuery prepareGraphQuery(QueryLanguage ql,
			String queryString, String baseURI) throws MalformedQueryException {
		SailGraphQuery q = super.prepareGraphQuery(ql, queryString, baseURI);
		insertOriginalQueryString(q, queryString, baseURI, determineGraphQueryType(q));
		setIncludeInferredDefault(q);
		return new FedXGraphQuery(q);
	}

	@Override
	public SailBooleanQuery prepareBooleanQuery(QueryLanguage ql,
			String queryString, String baseURI) throws MalformedQueryException {
		SailBooleanQuery q = super.prepareBooleanQuery(ql, queryString, baseURI);
		insertOriginalQueryString(q, queryString, baseURI, QueryType.ASK);
		setIncludeInferredDefault(q);
		return new FedXBooleanQuery(q);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String updateString, String baseURI)
			throws RepositoryException, MalformedQueryException {
		Update update = super.prepareUpdate(ql, updateString, baseURI);
		insertOriginalQueryString(update, updateString, baseURI, QueryType.UPDATE);
		return update;
	}

	private void setIncludeInferredDefault(SailQuery query) {
		query.setIncludeInferred(federationContext.getConfig().getIncludeInferredDefault());
	}

	private void insertOriginalQueryString(Operation query, String queryString, String baseURI, QueryType qt) {
		if (baseURI != null) {
			query.setBinding(BINDING_ORIGINAL_BASE_URI, FedXUtil.literal(baseURI));
		}
		query.setBinding(BINDING_ORIGINAL_QUERY, FedXUtil.literal(queryString));
		query.setBinding(BINDING_ORIGINAL_QUERY_TYPE, FedXUtil.literal(qt.name()));
	}

	private QueryType determineGraphQueryType(SailGraphQuery q) {
		if (q.getParsedQuery() instanceof ParsedDescribeQuery) {
			return QueryType.DESCRIBE;
		}
		return QueryType.CONSTRUCT;
	}
}
