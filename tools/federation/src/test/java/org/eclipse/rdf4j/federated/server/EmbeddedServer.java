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

import java.io.File;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;

public class EmbeddedServer {

	public static final String HOST = "localhost";
	public static final int PORT = 18080;
	public static final String CONTEXT_PATH = "/";
	public static final String WAR_PATH = "./build/test/rdf4j-server/";

	private final Server jetty;

	public EmbeddedServer() {
		this(HOST, PORT, CONTEXT_PATH, WAR_PATH);
	}

	public EmbeddedServer(String host, int port, String contextPath, String warPath) {

		jetty = new Server();

		ServerConnector conn = new ServerConnector(jetty);
		conn.setHost(host);
		conn.setPort(port);
		jetty.addConnector(conn);

		WebAppContext webapp = new WebAppContext();
		webapp.setContextPath(contextPath);
		webapp.setTempDirectory(new File("temp/webapp/"));
		webapp.setWar(warPath);
		jetty.setHandler(webapp);
	}

	public void start() throws Exception {
		jetty.start();
	}

	public void stop() throws Exception {
		jetty.stop();
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
