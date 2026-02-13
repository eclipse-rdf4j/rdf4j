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
package org.eclipse.rdf4j.http.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.sail.shacl.config.ShaclSailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Herko ter Horst
 */
public class TestServer {

	private static final Logger logger = LoggerFactory.getLogger(TestServer.class);

	private static final String HOST = "localhost";
	private static final int MIN_TEST_PORT = 32768;
	private static final int PORT_ALLOCATION_ATTEMPTS = 100;

	public static final String TEST_REPO_ID = "Test";

	public static final String TEST_INFERENCE_REPO_ID = "Test-RDFS";
	public static final String TEST_SHACL_REPO_ID = "Test-SHACL";

	private static final String RDF4J_CONTEXT = "/rdf4j";

	public static String SERVER_URL;
	public static String REPOSITORY_URL;

	private final RemoteRepositoryManager manager;

	private final Server jetty;

	private final WebAppContext webapp;
	private final int port;
	private final String serverUrl;
	private static final String DISPATCHER_CONTEXT_ATTRIBUTE = "org.springframework.web.servlet.FrameworkServlet.CONTEXT.rdf4j-http-server";

	public TestServer() throws IOException {
		System.clearProperty("DEBUG");
		port = allocatePortAbove32768();
		serverUrl = "http://" + HOST + ":" + port + RDF4J_CONTEXT;
		SERVER_URL = serverUrl;
		REPOSITORY_URL = Protocol.getRepositoryLocation(serverUrl, TEST_REPO_ID);

		PropertiesReader reader = new PropertiesReader("maven-config.properties");
		String webappDir = reader.getProperty("testserver.webapp.dir");
		logger.debug("build path: {}", webappDir);

		jetty = new Server();

		ServerConnector conn = new ServerConnector(jetty);
		conn.setHost(HOST);
		conn.setPort(port);
		jetty.addConnector(conn);

		webapp = new WebAppContext();
		WebAppContext.addServerClasses(jetty, "org.slf4j.", "ch.qos.logback.");
		webapp.setContextPath(RDF4J_CONTEXT);
		// warPath configured in pom.xml maven-war-plugin configuration
		webapp.setWar("./target/rdf4j-server");
		if (Boolean.getBoolean("rdf4j.testserver.logRequests")) {
			webapp.addFilter(RequestDebugFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		}
		jetty.setHandler(webapp);

		manager = RemoteRepositoryManager.getInstance(serverUrl);
	}

	public void start() throws Exception {
		File dataDir = new File(System.getProperty("user.dir") + "/target/datadir");
		dataDir.mkdirs();
		System.setProperty("org.eclipse.rdf4j.appdata.basedir", dataDir.getAbsolutePath());

		jetty.start();

		if (!webapp.isAvailable()) {
			throw new IllegalStateException("Webapp failed to start", webapp.getUnavailableException());
		}
		try {
			Object dispatcherContext = webapp.getServletContext().getAttribute(DISPATCHER_CONTEXT_ATTRIBUTE);
			if (dispatcherContext != null) {
				ClassLoader webappClassLoader = dispatcherContext.getClass().getClassLoader();
				Class<?> handlerMappingClass = Class.forName("org.springframework.web.servlet.HandlerMapping", false,
						webappClassLoader);
				@SuppressWarnings("unchecked")
				Map<String, ?> handlerMappings = (Map<String, ?>) dispatcherContext.getClass()
						.getMethod("getBeansOfType", Class.class)
						.invoke(dispatcherContext, handlerMappingClass);
				logger.warn("Handler mappings detected: {}", handlerMappings.keySet());
				for (Map.Entry<String, ?> entry : handlerMappings.entrySet()) {
					Class<?> mappingClass = entry.getValue().getClass();
					java.lang.reflect.Field handlerMapField = null;
					while (mappingClass != null && handlerMapField == null) {
						try {
							handlerMapField = mappingClass.getDeclaredField("handlerMap");
						} catch (NoSuchFieldException ignore) {
							mappingClass = mappingClass.getSuperclass();
						}
					}
					if (handlerMapField != null) {
						handlerMapField.setAccessible(true);
						Object handlerMap = handlerMapField.get(entry.getValue());
						logger.warn("Mapping {} handler map: {}", entry.getKey(), handlerMap);
						System.out.println("Handler mapping " + entry.getKey() + " -> " + handlerMap);
					} else {
						logger.warn("Mapping {} has no handlerMap field", entry.getKey());
					}
				}
			} else {
				logger.warn("No dispatcher context found at attribute {}", DISPATCHER_CONTEXT_ATTRIBUTE);
			}
		} catch (Throwable t) {
			logger.warn("Unable to inspect handler mappings", t);
		}
		createTestRepositories();
	}

	public void stop() throws Exception {
		try {
			manager.getAllRepositoryInfos().forEach(ri -> manager.removeRepository(ri.getId()));
			manager.shutDown();
		} finally {
			jetty.stop();
			System.clearProperty("org.mortbay.log.class");
		}
	}

	public static class RequestDebugFilter implements Filter {
		private static final Logger filterLogger = LoggerFactory.getLogger(RequestDebugFilter.class);

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			try {
				if (request instanceof HttpServletRequest) {
					HttpServletRequest http = (HttpServletRequest) request;
					String lookup = http.getRequestURI();
					if (http.getContextPath() != null && lookup.startsWith(http.getContextPath())) {
						lookup = lookup.substring(http.getContextPath().length());
					}

					String msg = String.format("Request uri=%s contextPath=%s servletPath=%s pathInfo=%s lookup=%s",
							http.getRequestURI(), http.getContextPath(), http.getServletPath(), http.getPathInfo(),
							lookup);
					filterLogger.warn(msg);
					System.out.println(msg);
					try {
						ClassLoader cl = http.getClass().getClassLoader();
						Class<?> urlPathHelperClass = Class.forName("org.springframework.web.util.UrlPathHelper", false,
								cl);
						Object urlPathHelper = urlPathHelperClass.getConstructor().newInstance();
						String pathWithinApp = (String) urlPathHelperClass
								.getMethod("getPathWithinApplication", HttpServletRequest.class)
								.invoke(urlPathHelper, http);
						filterLogger.warn("UrlPathHelper pathWithinApplication={}", pathWithinApp);
						System.out.println("UrlPathHelper pathWithinApplication=" + pathWithinApp);
					} catch (Throwable e) {
						filterLogger.warn("Unable to compute pathWithinApplication for {}", lookup, e);
					}
					try {
						Object dispatcherContext = http.getServletContext().getAttribute(DISPATCHER_CONTEXT_ATTRIBUTE);
						if (dispatcherContext != null) {
							Object mapping = dispatcherContext.getClass()
									.getMethod("getBean", String.class)
									.invoke(dispatcherContext, "rdf4jProtocolUrlMapping");
							Object handlerMap = mapping.getClass().getMethod("getHandlerMap").invoke(mapping);
							filterLogger.warn("Handler map keys: {}", handlerMap);
							Object handler = mapping.getClass()
									.getMethod("getHandler", HttpServletRequest.class)
									.invoke(mapping, http);
							filterLogger.warn("Handler lookup for {} -> {}", lookup, handler);
							System.out.println("Handler lookup " + lookup + " -> " + handler);
						} else {
							filterLogger.warn("No dispatcher context at {}", DISPATCHER_CONTEXT_ATTRIBUTE);
						}
					} catch (Throwable e) {
						filterLogger.warn("Unable to resolve handler for {}", lookup, e);
					}
				}
			} catch (Throwable e) {
				// ignore
			}
			chain.doFilter(request, response);
		}
	}

	private void createTestRepositories() throws RepositoryException, RepositoryConfigException {
		// create a (non-inferencing) memory store
		MemoryStoreConfig memStoreConfig = new MemoryStoreConfig();
		SailRepositoryConfig sailRepConfig = new SailRepositoryConfig(memStoreConfig);
		RepositoryConfig repConfig = new RepositoryConfig(TEST_REPO_ID, sailRepConfig);
		manager.addRepositoryConfig(repConfig);

		// create an inferencing memory store
		SchemaCachingRDFSInferencerConfig inferMemStoreConfig = new SchemaCachingRDFSInferencerConfig(
				new MemoryStoreConfig());
		sailRepConfig = new SailRepositoryConfig(inferMemStoreConfig);
		repConfig = new RepositoryConfig(TEST_INFERENCE_REPO_ID, sailRepConfig);
		manager.addRepositoryConfig(repConfig);

		// create memory store with shacl support
		ShaclSailConfig shaclConfig = new ShaclSailConfig(new MemoryStoreConfig());
		sailRepConfig = new SailRepositoryConfig(shaclConfig);
		repConfig = new RepositoryConfig(TEST_SHACL_REPO_ID, sailRepConfig);
		manager.addRepositoryConfig(repConfig);
	}

	static class PropertiesReader {
		private final Properties properties;

		public PropertiesReader(String propertyFileName) throws IOException {
			InputStream is = getClass().getClassLoader()
					.getResourceAsStream(propertyFileName);
			this.properties = new Properties();
			this.properties.load(is);
		}

		public String getProperty(String propertyName) {
			return this.properties.getProperty(propertyName);
		}
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
