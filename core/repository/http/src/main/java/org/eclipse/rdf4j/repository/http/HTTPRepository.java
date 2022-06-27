/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.http;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.http.client.SessionManagerDependent;
import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.AbstractRepository;
import org.eclipse.rdf4j.rio.RDFFormat;

/**
 * A repository that serves as a client for a remote repository on an RDF4J Server. Methods in this class may throw the
 * specific {@link RepositoryException} subclass {@link UnauthorizedException}, the semantics of which is defined by the
 * HTTP protocol.
 * <p>
 * This repository client uses a <a href="https://rdf4j.org/documentation/reference/rest-api/">RDF4J-specific extension
 * of the SPARQL 1.1 Protocol</a> to communicate with the server. For communicating with a SPARQL endpoint that is not
 * based on RDF4J, it is recommended to use {@link org.eclipse.rdf4j.repository.sparql.SPARQLRepository
 * SPARQLRepository} instead.
 *
 * @see org.eclipse.rdf4j.http.protocol.UnauthorizedException
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 * @author Herko ter Horst
 */
public class HTTPRepository extends AbstractRepository implements HttpClientDependent, SessionManagerDependent {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The HTTP client that takes care of the client-server communication.
	 */
	private volatile HttpClientSessionManager sessionManager;

	/** dependent life cycle */
	private volatile SharedHttpClientSessionManager dependentSessionManager;

	private String username;

	private String password;

	private String serverURL;

	private String repositoryURL;

	private RDFFormat rdfFormat;

	private TupleQueryResultFormat tupleFormat;

	private File dataDir;

	private volatile Boolean compatibleMode = null;

	private volatile Map<String, String> additionalHttpHeaders = Collections.emptyMap();

	private HTTPRepository() {
		super();
	}

	public HTTPRepository(final String serverURL, final String repositoryID) {
		this();
		this.serverURL = serverURL;
		this.repositoryURL = Protocol.getRepositoryLocation(serverURL, repositoryID);
	}

	public HTTPRepository(final String repositoryURL) {
		this();
		// Try to parse the server URL from the repository URL
		Pattern urlPattern = Pattern.compile("(.*)/" + Protocol.REPOSITORIES + "/[^/]*/?");
		Matcher matcher = urlPattern.matcher(repositoryURL);

		if (matcher.matches() && matcher.groupCount() == 1) {
			this.serverURL = matcher.group(1);
		} else {
			throw new IllegalArgumentException("URL must be to a RDF4J Repository (not just the server)");
		}
		this.repositoryURL = repositoryURL;
	}

	@Override
	public void setDataDir(final File dataDir) {
		this.dataDir = dataDir;
	}

	@Override
	public File getDataDir() {
		return dataDir;
	}

	@Override
	public HttpClientSessionManager getHttpClientSessionManager() {
		HttpClientSessionManager result = sessionManager;
		if (result == null) {
			synchronized (this) {
				result = sessionManager;
				if (result == null) {
					result = sessionManager = dependentSessionManager = new SharedHttpClientSessionManager();
				}
			}
		}
		return result;
	}

	@Override
	public void setHttpClientSessionManager(HttpClientSessionManager client) {
		synchronized (this) {
			this.sessionManager = client;
			// If they set a client, we need to check whether we need to
			// shutdown any existing dependentClient
			SharedHttpClientSessionManager toCloseDependentClient = dependentSessionManager;
			dependentSessionManager = null;
			if (toCloseDependentClient != null) {
				toCloseDependentClient.shutDown();
			}
		}
	}

	/**
	 * Get the additional HTTP headers which will be used
	 *
	 * @return a read-only view of the additional HTTP headers which will be included in every request to the server.
	 */
	public Map<String, String> getAdditionalHttpHeaders() {
		return Collections.unmodifiableMap(additionalHttpHeaders);
	}

	/**
	 * Set additional HTTP headers to be included in every request to the server, which may be required for certain
	 * unusual server configurations. This will only take effect on connections subsequently returned by
	 * {@link #getConnection()}.
	 *
	 * @param additionalHttpHeaders a map containing pairs of header names and values. May be null
	 */
	public void setAdditionalHttpHeaders(Map<String, String> additionalHttpHeaders) {
		if (additionalHttpHeaders == null) {
			this.additionalHttpHeaders = Collections.emptyMap();
		} else {
			this.additionalHttpHeaders = additionalHttpHeaders;
		}
	}

	@Override
	public final HttpClient getHttpClient() {
		return getHttpClientSessionManager().getHttpClient();
	}

	@Override
	public void setHttpClient(HttpClient httpClient) {
		SharedHttpClientSessionManager toSetDependentSessionManager = dependentSessionManager;
		if (toSetDependentSessionManager == null) {
			getHttpClientSessionManager();
			toSetDependentSessionManager = dependentSessionManager;
		}
		// The strange lifecycle results in the possibility that the
		// dependentSessionManger will be null due to a call to setHttpClient, so add
		// a null guard here for that possibility
		if (toSetDependentSessionManager != null) {
			toSetDependentSessionManager.setHttpClient(httpClient);
		}
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	public RepositoryConnection getConnection() throws RepositoryException {
		if (!isInitialized()) {
			init();
		}
		return new HTTPRepositoryConnection(this, createHTTPClient());
	}

	@Override
	public boolean isWritable() throws RepositoryException {
		if (!isInitialized()) {
			init();
		}

		boolean isWritable = false;

		try (RDF4JProtocolSession client = createHTTPClient()) {
			final String repositoryURL = client.getRepositoryURL();
			try (TupleQueryResult repositoryList = client.getRepositoryList()) {
				while (repositoryList.hasNext()) {
					final BindingSet bindingSet = repositoryList.next();
					final Value uri = bindingSet.getValue("uri");

					if (uri != null && uri.stringValue().equals(repositoryURL)) {
						isWritable = Literals.getBooleanValue(bindingSet.getValue("writable"), false);
						break;
					}
				}
			}
		} catch (QueryEvaluationException | IOException e) {
			throw new RepositoryException(e);
		}

		return isWritable;
	}

	/**
	 * Sets the preferred serialization format for tuple query results to the supplied {@link TupleQueryResultFormat},
	 * overriding the {@link SPARQLProtocolSession} 's default preference. Setting this parameter is not necessary in
	 * most cases as the {@link SPARQLProtocolSession} by default indicates a preference for the most compact and
	 * efficient format available.
	 *
	 * @param format the preferred {@link TupleQueryResultFormat}. If set to 'null' no explicit preference will be
	 *               stated.
	 */
	public void setPreferredTupleQueryResultFormat(final TupleQueryResultFormat format) {
		this.tupleFormat = format;
	}

	/**
	 * Indicates the current preferred {@link TupleQueryResultFormat}.
	 *
	 * @return The preferred format, of 'null' if no explicit preference is defined.
	 */
	public TupleQueryResultFormat getPreferredTupleQueryResultFormat() {
		return tupleFormat;
	}

	/**
	 * Sets the preferred serialization format for RDF to the supplied {@link RDFFormat}, overriding the
	 * {@link SPARQLProtocolSession}'s default preference. Setting this parameter is not necessary in most cases as the
	 * {@link SPARQLProtocolSession} by default indicates a preference for the most compact and efficient format
	 * available.
	 * <p>
	 * Use with caution: if set to a format that does not support context serialization any context info contained in
	 * the query result will be lost.
	 *
	 * @param format the preferred {@link RDFFormat}. If set to 'null' no explicit preference will be stated.
	 */
	public void setPreferredRDFFormat(final RDFFormat format) {
		this.rdfFormat = format;
	}

	/**
	 * Indicates the current preferred {@link RDFFormat}.
	 *
	 * @return The preferred format, of 'null' if no explicit preference is defined.
	 */
	public RDFFormat getPreferredRDFFormat() {
		return rdfFormat;
	}

	/**
	 * Set the username and password to use for authenticating with the remote repository.
	 *
	 * @param username the username. Setting this to null will disable authentication.
	 * @param password the password. Setting this to null will disable authentication.
	 */
	public void setUsernameAndPassword(final String username, final String password) {
		this.username = username;
		this.password = password;
	}

	public String getRepositoryURL() {
		return repositoryURL;
	}

	/*
	 * -------------------* non-public methods * -------------------
	 */

	@Override
	protected void initializeInternal() throws RepositoryException {
		// no-op
	}

	@Override
	protected void shutDownInternal() throws RepositoryException {
		try {
			SharedHttpClientSessionManager toCloseDependentClient = dependentSessionManager;
			dependentSessionManager = null;
			if (toCloseDependentClient != null) {
				toCloseDependentClient.shutDown();
			}
		} finally {
			// remove reference but do not shut down, client may be shared by
			// other repos.
			sessionManager = null;
		}
	}

	/**
	 * Creates a new {@link RDF4JProtocolSession} object.
	 *
	 * @return a {@link RDF4JProtocolSession} object.
	 */
	protected RDF4JProtocolSession createHTTPClient() {
		// initialize HTTP client
		RDF4JProtocolSession httpClient = getHttpClientSessionManager().createRDF4JProtocolSession(serverURL);
		httpClient.setValueFactory(SimpleValueFactory.getInstance());
		if (repositoryURL != null) {
			httpClient.setRepository(repositoryURL);
		}
		if (tupleFormat != null) {
			httpClient.setPreferredTupleQueryResultFormat(tupleFormat);
		}
		if (rdfFormat != null) {
			httpClient.setPreferredRDFFormat(rdfFormat);
		}
		if (username != null) {
			httpClient.setUsernameAndPassword(username, password);
		}
		httpClient.setAdditionalHttpHeaders(additionalHttpHeaders);
		return httpClient;
	}

	/**
	 * Verify if transaction handling should be done in backward-compatible mode (this is the case when communicating
	 * with an older RDF4J Server).
	 *
	 * @return <code>true</code> if the Server does not support the extended transaction protocol, <code>false</code>
	 *         otherwise.
	 * @throws RepositoryException if something went wrong while querying the server for the protocol version.
	 */
	boolean useCompatibleMode() throws RepositoryException {
		Boolean result = compatibleMode;
		if (result == null) {
			synchronized (this) {
				result = compatibleMode;
				if (result == null) {
					// protocol version 7 supports the new transaction
					// handling. If the server is older, we need to run in
					// backward-compatible mode.
					result = compatibleMode = (getServerProtocolVersion() < 7);
				}
			}
		}
		return result;
	}

	/**
	 * Get the RDF4J Server's protocol version, as an integer
	 *
	 * @return the protocol version implemented by the RDF4J, as an integer number.
	 */
	int getServerProtocolVersion() {
		try (RDF4JProtocolSession client = createHTTPClient()) {
			final String serverProtocolVersion = client.getServerProtocol();
			return Integer.parseInt(serverProtocolVersion);
		} catch (NumberFormatException | IOException e) {
			throw new RepositoryException("could not read protocol version from server: ", e);
		}

	}
}
