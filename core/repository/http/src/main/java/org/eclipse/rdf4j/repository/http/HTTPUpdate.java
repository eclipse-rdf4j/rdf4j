/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.http;

import java.io.IOException;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.http.client.query.AbstractHTTPUpdate;
import org.eclipse.rdf4j.http.protocol.Protocol.Action;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Update specific to the HTTP protocol. Methods in this class may throw the specific RepositoryException subclass
 * UnautorizedException, the semantics of which is defined by the HTTP protocol.
 *
 * @see org.eclipse.rdf4j.http.protocol.UnauthorizedException
 * @author Jeen Broekstra
 */
public class HTTPUpdate extends AbstractHTTPUpdate {

	private final HTTPRepositoryConnection httpCon;

	public HTTPUpdate(HTTPRepositoryConnection con, QueryLanguage ql, String queryString, String baseURI) {
		super(con.getSesameSession(), ql, queryString, baseURI);
		this.httpCon = con;
	}

	@Override
	public void execute() throws UpdateExecutionException {
		try {
			if (httpCon.getRepository().useCompatibleMode()) {
				if (!httpCon.isActive()) {
					// execute update immediately
					SPARQLProtocolSession client = getHttpClient();
					try {
						client.sendUpdate(getQueryLanguage(), getQueryString(), getBaseURI(), dataset, includeInferred,
								getMaxExecutionTime(), getBindingsArray());
					} catch (UnauthorizedException | QueryInterruptedException | MalformedQueryException
							| IOException e) {
						throw new HTTPUpdateExecutionException(e.getMessage(), e);
					}
				} else {
					// defer execution as part of transaction.
					httpCon.scheduleUpdate(this);
				}
				return;
			}

			SPARQLProtocolSession client = getHttpClient();
			try {
				httpCon.flushTransactionState(Action.UPDATE);
				client.sendUpdate(getQueryLanguage(), getQueryString(), getBaseURI(), dataset, includeInferred,
						getMaxExecutionTime(), getBindingsArray());
			} catch (UnauthorizedException | QueryInterruptedException | MalformedQueryException | IOException e) {
				throw new HTTPUpdateExecutionException(e.getMessage(), e);
			}
		} catch (RepositoryException e) {
			throw new HTTPUpdateExecutionException(e.getMessage(), e);
		}

	}
}
