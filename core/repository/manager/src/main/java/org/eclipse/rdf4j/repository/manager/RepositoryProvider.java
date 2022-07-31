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
package org.eclipse.rdf4j.repository.manager;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

/**
 * A static access point to manage {@link RepositoryManager}s and {@link Repository Repositories}. RepositoryProvider
 * ensures that all managers and repositories obtained through it are automatically shutdown when the JVM exits.
 *
 * @author James Leigh
 */
public class RepositoryProvider {

	private static final String REPOSITORIES = "repositories/";

	private static class SynchronizedManager {

		private final String url;

		private RepositoryManager manager;

		public SynchronizedManager(String url) {
			this.url = url;
		}

		public synchronized RepositoryManager get() throws RepositoryConfigException, RepositoryException {
			if (manager == null || !manager.isInitialized()) {
				shutDown();
				RepositoryManager m = createRepositoryManager(url);
				m.init();
				manager = m;
			}
			return manager;
		}

		public synchronized boolean isInitialized() {
			return manager != null && manager.isInitialized();
		}

		public synchronized void shutDown() {
			if (manager != null) {
				manager.shutDown();
			}
		}
	}

	static final Map<String, SynchronizedManager> managers = new HashMap<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread("RepositoryProvider-shutdownHook") {

			@Override
			public void run() {
				synchronized (managers) {
					for (SynchronizedManager manager : managers.values()) {
						manager.shutDown();
					}
				}
			}
		});
	}

	/**
	 * Creates a {@link RepositoryManager}, if not already created, that will be shutdown when the JVM exits cleanly.
	 *
	 * @param url location of the data directory for the RepositoryManager. This should be a URL of the form
	 *            http://host:port/path/ (for a RemoteRepositoryManager) or file:///path/ (for a
	 *            LocalRepositoryManager).
	 * @return a (new or existing) {@link RepositoryManager} using the supplied url as its data dir.
	 */
	public static RepositoryManager getRepositoryManager(String url)
			throws RepositoryConfigException, RepositoryException {
		String uri = normalizeDirectory(url);
		SynchronizedManager sync;
		synchronized (managers) {
			Iterator<SynchronizedManager> iter = managers.values().iterator();
			while (iter.hasNext()) {
				SynchronizedManager sm = iter.next();
				if (!sm.isInitialized()) {
					sm.shutDown();
					iter.remove();
				}
			}
			if (managers.containsKey(uri)) {
				sync = managers.get(uri);
			} else {
				managers.put(uri, sync = new SynchronizedManager(url));
			}
		}
		return sync.get();
	}

	/**
	 * Creates a {@link LocalRepositoryManager}, if not already created, that will be shutdown when the JVM exits
	 * cleanly.
	 *
	 * @param dir the data directory for the repository manager.
	 * @return a (new or existing) {@link LocalRepositoryManager}.
	 * @throws RepositoryConfigException
	 * @throws RepositoryException
	 */
	public static LocalRepositoryManager getRepositoryManager(File dir)
			throws RepositoryConfigException, RepositoryException {
		String url = dir.toURI().toASCIIString();
		return (LocalRepositoryManager) getRepositoryManager(url);
	}

	/**
	 * Retrieves the {@link RepositoryManager} that will be used for the given repository URL. Creates a
	 * {@link RepositoryManager}, if not already created, that will be shutdown when the JVM exits cleanly.
	 *
	 * @param url the location of the repository for which to retrieve the corresponding RepositoryManager. The
	 *            parameter must be a URL of the form http://host:port/path/repositories/id or
	 *            file:///path/repositories/id.
	 * @return the {@link RepositoryManager} that manages the repository identified by the URL.
	 * @throws IllegalArgumentException  if the supplied URL is not a repository URL.
	 * @throws RepositoryConfigException
	 * @throws RepositoryException
	 */
	public static RepositoryManager getRepositoryManagerOfRepository(String url)
			throws RepositoryConfigException, RepositoryException {
		if (!url.contains(REPOSITORIES)) {
			throw new IllegalArgumentException("URL is not repository URL: " + url);
		}
		int idx = url.lastIndexOf(REPOSITORIES);
		String server = url.substring(0, idx);
		if (server.length() == 0) {
			server = ".";
		}
		return getRepositoryManager(server);
	}

	/**
	 * Retrieves the Repository ID that will be passed to a RepositoryManager for the given repository URL.
	 *
	 * @param url the location URL for the repository. The parameter must be a URL of the form
	 *            http://host:port/path/repositories/id or file:///path/repositories/id.
	 * @return the repository identifier string for the given repository URL.
	 * @throws IllegalArgumentException if the supplied URL is not a repository URL.
	 */
	public static String getRepositoryIdOfRepository(String url) {
		if (!url.contains(REPOSITORIES)) {
			throw new IllegalArgumentException("URL is not repository URL: " + url);
		}
		int idx = url.lastIndexOf(REPOSITORIES);
		String id = url.substring(idx + REPOSITORIES.length());
		if (id.endsWith("/")) {
			id = id.substring(0, id.length() - 1);
		}
		return id;
	}

	/**
	 * Retrieves a (new or existing) Repository object for the supplied repository URL. The Repository will be shutdown
	 * when the JVM exits cleanly.
	 *
	 * @param url the repository URL. The parameter must be a URL of the form http://host:port/path/repositories/id or
	 *            file:///path/repositories/id.
	 * @return Repository from a RepositoryManager or null if repository is not defined
	 */
	public static Repository getRepository(String url) throws RepositoryException, RepositoryConfigException {
		RepositoryManager manager = getRepositoryManagerOfRepository(url);
		String id = getRepositoryIdOfRepository(url);
		return manager.getRepository(id);
	}

	static RepositoryManager createRepositoryManager(String url) throws RepositoryConfigException {
		if (url.startsWith("http")) {
			return new RemoteRepositoryManager(url);
		} else {
			return new LocalRepositoryManager(asLocalFile(url));
		}
	}

	private static File asLocalFile(String url) throws RepositoryConfigException {
		URI uri = new File(".").toURI().resolve(url);
		return new File(uri);
	}

	private static String normalizeDirectory(String url) throws IllegalArgumentException {
		try {
			if (!url.endsWith("/")) {
				return normalizeDirectory(url + '/');
			}
			URI norm = URI.create(url);
			if (!norm.isAbsolute()) {
				norm = new File(".").toURI().resolve(url);
			}
			norm = norm.normalize();
			if (norm.isOpaque()) {
				throw new IllegalArgumentException("Repository Manager URL must not be opaque: " + url);
			}
			String sch = norm.getScheme();
			String host = norm.getAuthority();
			String path = norm.getPath();
			if (sch != null) {
				sch = sch.toLowerCase();
			}
			if (host != null) {
				host = host.toLowerCase();
			}
			return new URI(sch, host, path, null, null).toASCIIString();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
