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
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Paths;

import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class EmbeddedServer {

	public static final String HOST = "localhost";
	private static final int MIN_TEST_PORT = 32768;
	private static final int PORT_ALLOCATION_ATTEMPTS = 100;
	public static final String CONTEXT_PATH = "/";
	public static final String WAR_PATH = Paths.get("build", "test", "rdf4j-server")
			.toAbsolutePath()
			.toString();

	private final Server jetty;
	private final String host;
	private final int port;
	private final String contextPath;

	public EmbeddedServer() throws IOException {
		this(HOST, allocatePortAbove32768(), CONTEXT_PATH, WAR_PATH);
	}

	public EmbeddedServer(String host, int port, String contextPath, String warPath) {
		this.host = host;
		this.port = port;
		this.contextPath = contextPath;

		jetty = new Server();

		ServerConnector conn = new ServerConnector(jetty);
		conn.setHost(host);
		conn.setPort(port);
		jetty.addConnector(conn);

		WebAppContext webapp = new WebAppContext();
		WebAppContext.addServerClasses(jetty, "org.slf4j.", "ch.qos.logback.");
		webapp.setContextPath(contextPath);
		webapp.setTempDirectory(new File("temp/webapp/"));
		webapp.setWar(warPath);
		jetty.setHandler(webapp);
	}

	public String getServerUrl() {
		return "http://" + host + ":" + port + contextPath;
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

	private static int allocatePortAbove32768() throws IOException {
		for (int attempt = 0; attempt < PORT_ALLOCATION_ATTEMPTS; attempt++) {
			try (ServerSocket socket = new ServerSocket(0)) {
				int candidate = socket.getLocalPort();
				if (candidate > MIN_TEST_PORT) {
					return candidate;
				}
			}
		}
		throw new IOException("Unable to allocate random test port above " + MIN_TEST_PORT);
	}
}
