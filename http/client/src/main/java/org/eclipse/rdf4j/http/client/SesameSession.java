/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import java.util.concurrent.ExecutorService;

import org.apache.http.client.HttpClient;

/**
 * @author Jeen Broekstra
 * @deprecated use {@link RDF4JProtocolSession} instead.
 */
@Deprecated
public class SesameSession extends RDF4JProtocolSession {

	/**
	 * @deprecated use {@link RDF4JProtocolSession} instead.
	 */
	@Deprecated
	public SesameSession(HttpClient client, ExecutorService executor) {
		super(client, executor);
	}

}
