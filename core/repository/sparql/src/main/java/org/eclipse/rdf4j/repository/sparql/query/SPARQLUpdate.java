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
package org.eclipse.rdf4j.repository.sparql.query;

import java.io.IOException;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.http.client.query.AbstractHTTPUpdate;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

/**
 * Update operation of the {@link SPARQLRepository}
 *
 * @author Jeen Broekstra
 * @author Andreas Schwarte
 */
public class SPARQLUpdate extends AbstractHTTPUpdate {

	public SPARQLUpdate(SPARQLProtocolSession httpClient, String baseURI, String queryString) {
		super(httpClient, QueryLanguage.SPARQL, queryString, baseURI);
	}

	@Override
	public void execute() throws UpdateExecutionException {

		try {
			// execute update immediately
			SPARQLProtocolSession client = getHttpClient();
			try {
				client.sendUpdate(getQueryLanguage(), getQueryString(), getBaseURI(), dataset, includeInferred,
						getMaxExecutionTime(), getBindingsArray());
			} catch (UnauthorizedException | QueryInterruptedException | MalformedQueryException | IOException e) {
				throw new UpdateExecutionException(e.getMessage(), e);
			}
		} catch (RepositoryException e) {
			throw new UpdateExecutionException(e.getMessage(), e);
		}

	}

	@Override
	public String getQueryString() {
		return QueryStringUtil.getUpdateString(queryString, getBindings());
	}
}
