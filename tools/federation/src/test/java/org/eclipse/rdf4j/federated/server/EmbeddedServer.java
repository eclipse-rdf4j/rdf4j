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
package org.eclipse.rdf4j.federated.server;

import org.eclipse.rdf4j.federated.SPARQLServerBaseTest;

import com.github.mjeanroy.junit.servers.jetty.EmbeddedJettyConfiguration;
import com.github.mjeanroy.junit.servers.jetty9.EmbeddedJetty;
import com.github.mjeanroy.junit.servers.jetty9.EmbeddedJettyFactory;

public class EmbeddedServer {

	protected final com.github.mjeanroy.junit.servers.servers.EmbeddedServer<?> server;

	public EmbeddedServer() {
		this(18080, "/", "./build/test/rdf4j-server/");
	}

	public EmbeddedServer(int port, String contextPath, String warPath) {
		EmbeddedJettyConfiguration configuration = EmbeddedJettyConfiguration.builder()
				.withPort(port)
				.withPath(contextPath)
				.withWebapp(warPath)
				.build();
		// TODO: Supplying the base class (SPARQLServerBaseTest) for all these server tests for now
		server = EmbeddedJettyFactory.createFrom(SPARQLServerBaseTest.class, configuration);
	}

	public void start() throws Exception {
		server.start();
	}

	public void stop() throws Exception {
		server.stop();
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args)
			throws Exception {
		EmbeddedServer server = new EmbeddedServer();
		server.start();
	}
}
