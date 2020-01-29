/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

/**
 * A manager for {@link Repository}s that reside on a remote server. This repository manager allows one to access
 * repositories over HTTP similar to how local repositories are accessed using the {@link LocalRepositoryManager}.
 *
 * @author Arjohn Kampman
 */
public class RemoteRepositoryManager extends RepositoryManager {

	/*------------------------*
	 * Static utility methods *
	 *------------------------*/

	/**
	 * Creates an initialized {@link RemoteRepositoryManager} with the specified server URL.
	 */
	public static RemoteRepositoryManager getInstance(String serverURL) throws RepositoryException {
		RemoteRepositoryManager manager = new RemoteRepositoryManager(serverURL);
		manager.init();
		return manager;
	}

	/**
	 * Creates an initialized {@link RemoteRepositoryManager} with the specified server URL and credentials.
	 */
	public static RemoteRepositoryManager getInstance(String serverURL, String username, String password)
			throws RepositoryException {
		RemoteRepositoryManager manager = new RemoteRepositoryManager(serverURL);
		manager.setUsernameAndPassword(username, password);
		manager.init();
		return manager;
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	/** dependent life cycle */
	private volatile SharedHttpClientSessionManager client;

	/**
	 * The URL of the remote server, e.g. http://localhost:8080/rdf4j-server/
	 */
	private final String serverURL;

	private String username;

	private String password;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RepositoryManager that operates on the specified base directory.
	 *
	 * @param serverURL The URL of the server.
	 */
	public RemoteRepositoryManager(String serverURL) {
		super();
		this.serverURL = serverURL;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * @return Returns the {@link SharedHttpClientSessionManager}
	 */
	protected SharedHttpClientSessionManager getSharedHttpClientSessionManager() {
		SharedHttpClientSessionManager result = client;
		if (result == null) {
			synchronized (this) {
				result = client;
				if (result == null) {
					result = client = new SharedHttpClientSessionManager();
				}
			}
		}
		return result;
	}

	@Override
	public HttpClient getHttpClient() {
		SharedHttpClientSessionManager nextClient = client;
		if (nextClient == null) {
			return null;
		} else {
			return nextClient.getHttpClient();
		}
	}

	@Override
	public void setHttpClient(HttpClient protocolSession) {
		getSharedHttpClientSessionManager().setHttpClient(protocolSession);
	}

	@Override
	public void shutDown() {
		try {
			super.shutDown();
		} finally {
			SharedHttpClientSessionManager toCloseClient = client;
			client = null;
			if (toCloseClient != null) {
				toCloseClient.shutDown();
			}
		}
	}

	/**
	 * Set the username and password for authenication with the remote server.
	 *
	 * @param username the username
	 * @param password the password
	 */
	public void setUsernameAndPassword(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	@Deprecated
	protected Repository createSystemRepository() throws RepositoryException {
		HTTPRepository systemRepository = new HTTPRepository(serverURL, SystemRepository.ID);
		systemRepository.setHttpClientSessionManager(getSharedHttpClientSessionManager());
		systemRepository.setUsernameAndPassword(username, password);
		systemRepository.initialize();
		return systemRepository;
	}

	/**
	 * Gets the URL of the remote server, e.g. "http://localhost:8080/rdf4j-server/".
	 *
	 * @throws MalformedURLException If serverURL cannot be parsed
	 */
	@Override
	public URL getLocation() throws MalformedURLException {
		return new URL(serverURL);
	}

	/**
	 * Gets the URL of the remote server, e.g. "http://localhost:8080/rdf4j-server/".
	 */
	public String getServerURL() {
		return serverURL;
	}

	/**
	 * Creates and initializes the repository with the specified ID.
	 *
	 * @param id A repository ID.
	 * @return The created repository, or <tt>null</tt> if no such repository exists.
	 * @throws RepositoryConfigException If no repository could be created due to invalid or incomplete configuration
	 *                                   data.
	 */
	@Override
	protected Repository createRepository(String id) throws RepositoryConfigException, RepositoryException {
		HTTPRepository result = null;

		if (hasRepositoryConfig(id)) {
			result = new HTTPRepository(serverURL, id);
			result.setHttpClientSessionManager(getSharedHttpClientSessionManager());
			result.setUsernameAndPassword(username, password);
			result.init();
		}

		return result;
	}

	@Override
	public RepositoryConfig getRepositoryConfig(String id) throws RepositoryException {
		Model model = getModelFactory().createEmptyModel();
		try (RDF4JProtocolSession protocolSession = getSharedHttpClientSessionManager()
				.createRDF4JProtocolSession(serverURL)) {
			protocolSession.setUsernameAndPassword(username, password);

			int serverProtocolVersion = Integer.parseInt(protocolSession.getServerProtocol());
			if (serverProtocolVersion < 10) { // explicit per-repo config endpoint was introduced in Protocol version 10
				protocolSession.setRepository(Protocol.getRepositoryLocation(serverURL, SystemRepository.ID));
				protocolSession.getStatements(null, null, null, true, new StatementCollector(model));
			} else {
				protocolSession.setRepository(Protocol.getRepositoryLocation(serverURL, id));
				protocolSession.getRepositoryConfig(new StatementCollector(model));
			}

		} catch (IOException | QueryEvaluationException | UnauthorizedException ue) {
			throw new RepositoryException(ue);
		}
		return RepositoryConfigUtil.getRepositoryConfig(model, id);
	}

	@Override
	public Collection<RepositoryInfo> getAllRepositoryInfos(boolean skipSystemRepo) throws RepositoryException {
		List<RepositoryInfo> result = new ArrayList<>();

		try (RDF4JProtocolSession protocolSession = getSharedHttpClientSessionManager()
				.createRDF4JProtocolSession(serverURL)) {
			protocolSession.setUsernameAndPassword(username, password);
			try (TupleQueryResult responseFromServer = protocolSession.getRepositoryList()) {
				while (responseFromServer.hasNext()) {
					BindingSet bindingSet = responseFromServer.next();
					RepositoryInfo repInfo = new RepositoryInfo();

					String id = Literals.getLabel(bindingSet.getValue("id"), null);

					if (skipSystemRepo && id.equals(SystemRepository.ID)) {
						continue;
					}

					Value uri = bindingSet.getValue("uri");
					String description = Literals.getLabel(bindingSet.getValue("title"), null);
					boolean readable = Literals.getBooleanValue(bindingSet.getValue("readable"), false);
					boolean writable = Literals.getBooleanValue(bindingSet.getValue("writable"), false);

					if (uri instanceof IRI) {
						try {
							repInfo.setLocation(new URL(uri.toString()));
						} catch (MalformedURLException e) {
							logger.warn("Server reported malformed repository URL: {}", uri);
						}
					}

					repInfo.setId(id);
					repInfo.setDescription(description);
					repInfo.setReadable(readable);
					repInfo.setWritable(writable);

					result.add(repInfo);
				}
			}
		} catch (IOException | QueryEvaluationException ioe) {
			logger.warn("Unable to retrieve list of repositories", ioe);
			throw new RepositoryException(ioe);
		} catch (UnauthorizedException ue) {
			logger.warn("Not authorized to retrieve list of repositories", ue);
			throw new RepositoryException(ue);
		} catch (RepositoryException re) {
			logger.warn("Unable to retrieve list of repositories", re);
			throw re;
		}

		return result;
	}

	@Override
	public void addRepositoryConfig(RepositoryConfig config) throws RepositoryException, RepositoryConfigException {
		try (RDF4JProtocolSession protocolSession = getSharedHttpClientSessionManager()
				.createRDF4JProtocolSession(serverURL)) {
			protocolSession.setUsernameAndPassword(username, password);

			int serverProtocolVersion = Integer.parseInt(protocolSession.getServerProtocol());
			if (serverProtocolVersion < 9) { // explicit PUT create operation was introduced in Protocol version 9
				String baseURI = Protocol.getRepositoryLocation(serverURL, config.getID());
				Resource ctx = SimpleValueFactory.getInstance().createIRI(baseURI + "#" + config.getID());
				protocolSession.setRepository(Protocol.getRepositoryLocation(serverURL, SystemRepository.ID));
				Model model = getModelFactory().createEmptyModel();
				config.export(model, ctx);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				Rio.write(model, baos, protocolSession.getPreferredRDFFormat());
				removeRepository(config.getID());
				try (InputStream contents = new ByteArrayInputStream(baos.toByteArray())) {
					protocolSession.upload(contents, baseURI, protocolSession.getPreferredRDFFormat(), false, true,
							ctx);
				}
			} else {
				if (hasRepositoryConfig(config.getID())) {
					protocolSession.updateRepository(config);
				} else {
					protocolSession.createRepository(config);
				}
			}
		} catch (IOException | QueryEvaluationException | UnauthorizedException | NumberFormatException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public boolean removeRepository(String repositoryID) throws RepositoryException, RepositoryConfigException {

		boolean existingRepo = hasRepositoryConfig(repositoryID);

		if (existingRepo) {
			try (RDF4JProtocolSession protocolSession = getSharedHttpClientSessionManager()
					.createRDF4JProtocolSession(serverURL)) {
				protocolSession.setUsernameAndPassword(username, password);
				protocolSession.deleteRepository(repositoryID);
			} catch (IOException e) {
				logger.warn("error while deleting remote repository", e);
				throw new RepositoryConfigException(e);
			}
		}

		return existingRepo;
	}
}
