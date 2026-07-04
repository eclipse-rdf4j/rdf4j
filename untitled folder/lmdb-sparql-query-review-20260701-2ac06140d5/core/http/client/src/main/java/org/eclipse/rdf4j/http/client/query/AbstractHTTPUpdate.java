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

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.AbstractUpdate;

/**
 * Base class for any {@link Update} operation over HTTP.
 *
 * @author Andreas Schwarte
 */
public abstract class AbstractHTTPUpdate extends AbstractUpdate {

	private final SPARQLProtocolSession httpClient;

	protected final QueryLanguage queryLanguage;

	protected final String queryString;

	protected final String baseURI;

	protected AbstractHTTPUpdate(SPARQLProtocolSession httpClient, QueryLanguage queryLanguage, String queryString,
			String baseURI) {
		super();
		this.httpClient = httpClient;
		this.queryLanguage = queryLanguage;
		this.queryString = queryString;
		this.baseURI = baseURI;
	}

	/**
	 * @return Returns the {@link SPARQLProtocolSession} to be used for all HTTP based interaction
	 */
	protected SPARQLProtocolSession getHttpClient() {
		return httpClient;
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

	/**
	 * @return Returns the baseURI.
	 */
	public String getBaseURI() {
		return baseURI;
	}

	/**
	 * @return Returns the queryLanguage.
	 */
	public QueryLanguage getQueryLanguage() {
		return queryLanguage;
	}

	/**
	 * @return Returns the queryString.
	 */
	public String getQueryString() {
		return queryString;
	}

	@Override
	public String toString() {
		return queryString;
	}
}
