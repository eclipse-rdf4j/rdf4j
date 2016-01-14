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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.SesameClient;
import org.eclipse.rdf4j.http.client.SesameClientDependent;
import org.eclipse.rdf4j.http.client.SesameClientImpl;
import org.eclipse.rdf4j.http.client.SesameSession;
import org.eclipse.rdf4j.http.client.SparqlSession;
import org.eclipse.rdf4j.http.protocol.Protocol;
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
 * A repository that serves as a proxy for a remote repository on a Sesame
 * Server. Methods in this class may throw the specific RepositoryException
 * subclass UnautorizedException, the semantics of which is defined by the HTTP
 * protocol.
 * <p>
 * This repository proxy uses a
 * <a href="http://rdf4j.org/doc/system">Sesame-specific extension of the SPARQL
 * 1.1 Protocol</a> to communicate with the server. For communicating with a
 * non-Sesame-based SPARQL endpoint, it is recommend to use
 * {@link org.eclipse.rdf4j.repository.sparql.SPARQLRepository SPARQLRepository}
 * instead.
 *
 * @see org.eclipse.rdf4j.http.protocol.UnauthorizedException
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 * @author Herko ter Horst
 */
public class HTTPRepository extends AbstractRepository implements HttpClientDependent, SesameClientDependent {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The HTTP client that takes care of the client-server communication.
	 */
	private SesameClient client;

	/** dependent life cycle */
	private SesameClientImpl dependentClient;

	private String username;

	private String password;

	private String serverURL;

	private String repositoryURL;

	private RDFFormat rdfFormat;

	private TupleQueryResultFormat tupleFormat;

	private File dataDir;

	private Boolean compatibleMode = null;

	/*--------------*
	 * Constructors *
	 *--------------*/

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
		}
		else {
			throw new IllegalArgumentException("URL must be to a Sesame Repository (not just the server)");
		}
		this.repositoryURL = repositoryURL;
	}

	/* ---------------*
	 * public methods *
	 * ---------------*/

	public void setDataDir(final File dataDir) {
		this.dataDir = dataDir;
	}

	public File getDataDir() {
		return dataDir;
	}

	public synchronized SesameClient getSesameClient() {
		if (client == null) {
			client = dependentClient = new SesameClientImpl();
		}
		return client;
	}

	public synchronized void setSesameClient(SesameClient client) {
		this.client = client;
	}

	public final HttpClient getHttpClient() {
		return getSesameClient().getHttpClient();
	}

	public void setHttpClient(HttpClient httpClient) {
		if (dependentClient == null) {
			client = dependentClient = new SesameClientImpl();
		}
		dependentClient.setHttpClient(httpClient);
	}

	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	public RepositoryConnection getConnection()
		throws RepositoryException
	{
		return new HTTPRepositoryConnection(this, createHTTPClient());
	}

	public boolean isWritable()
		throws RepositoryException
	{
		if (!isInitialized()) {
			throw new IllegalStateException("HTTPRepository not initialized.");
		}

		boolean isWritable = false;
		final String repositoryURL = createHTTPClient().getRepositoryURL();

		try {
			final TupleQueryResult repositoryList = createHTTPClient().getRepositoryList();
			try {
				while (repositoryList.hasNext()) {
					final BindingSet bindingSet = repositoryList.next();
					final Value uri = bindingSet.getValue("uri");

					if (uri != null && uri.stringValue().equals(repositoryURL)) {
						isWritable = Literals.getBooleanValue(bindingSet.getValue("writable"), false);
						break;
					}
				}
			}
			finally {
				repositoryList.close();
			}
		}
		catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}

		return isWritable;
	}

	/**
	 * Sets the preferred serialization format for tuple query results to the
	 * supplied {@link TupleQueryResultFormat}, overriding the
	 * {@link SparqlSession} 's default preference. Setting this parameter is not
	 * necessary in most cases as the {@link SparqlSession} by default indicates
	 * a preference for the most compact and efficient format available.
	 * 
	 * @param format
	 *        the preferred {@link TupleQueryResultFormat}. If set to 'null' no
	 *        explicit preference will be stated.
	 */
	public void setPreferredTupleQueryResultFormat(final TupleQueryResultFormat format) {
		this.tupleFormat = format;
	}

	/**
	 * Indicates the current preferred {@link TupleQueryResultFormat}.
	 * 
	 * @return The preferred format, of 'null' if no explicit preference is
	 *         defined.
	 */
	public TupleQueryResultFormat getPreferredTupleQueryResultFormat() {
		return tupleFormat;
	}

	/**
	 * Sets the preferred serialization format for RDF to the supplied
	 * {@link RDFFormat}, overriding the {@link SparqlSession}'s default
	 * preference. Setting this parameter is not necessary in most cases as the
	 * {@link SparqlSession} by default indicates a preference for the most
	 * compact and efficient format available.
	 * <p>
	 * Use with caution: if set to a format that does not support context
	 * serialization any context info contained in the query result will be lost.
	 * 
	 * @param format
	 *        the preferred {@link RDFFormat}. If set to 'null' no explicit
	 *        preference will be stated.
	 */
	public void setPreferredRDFFormat(final RDFFormat format) {
		this.rdfFormat = format;
	}

	/**
	 * Indicates the current preferred {@link RDFFormat}.
	 * 
	 * @return The preferred format, of 'null' if no explicit preference is
	 *         defined.
	 */
	public RDFFormat getPreferredRDFFormat() {
		return rdfFormat;
	}

	/**
	 * Set the username and password to use for authenticating with the remote
	 * repository.
	 * 
	 * @param username
	 *        the username. Setting this to null will disable authentication.
	 * @param password
	 *        the password. Setting this to null will disable authentication.
	 */
	public void setUsernameAndPassword(final String username, final String password) {
		this.username = username;
		this.password = password;
	}

	public String getRepositoryURL() {
		return repositoryURL;
	}

	/* -------------------*
	 * non-public methods *
	 * -------------------*/

	@Override
	protected void initializeInternal()
		throws RepositoryException
	{
		// no-op
	}

	protected void shutDownInternal()
		throws RepositoryException
	{
		if (dependentClient != null) {
			dependentClient.shutDown();
			dependentClient = null;
		}
		// remove reference but do not shut down, client may be shared by other
		// repos.
		client = null;
	}

	/**
	 * Creates a new HTTPClient object. Subclasses may override to return a more
	 * specific HTTPClient subtype.
	 * 
	 * @return a HTTPClient object.
	 */
	protected SesameSession createHTTPClient() {
		// initialize HTTP client
		SesameSession httpClient = getSesameClient().createSesameSession(serverURL);
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
		return httpClient;
	}

	/**
	 * Verify if transaction handling should be done in backward-compatible mode
	 * (this is the case when communicating with an older Sesame Server).
	 * 
	 * @return <code>true</code> if the Server does not support the extended
	 *         transaction protocol, <code>false</code> otherwise.
	 * @throws RepositoryException
	 *         if something went wrong while querying the server for the protocol
	 *         version.
	 */
	synchronized boolean useCompatibleMode()
		throws RepositoryException
	{
		if (compatibleMode == null) {
			try {
				final String serverProtocolVersion = createHTTPClient().getServerProtocol();

				// protocol version 7 supports the new transaction handling. If
				// the server is older, we need to run in backward-compatible mode.
				this.compatibleMode = (Integer.parseInt(serverProtocolVersion) < 7);
			}
			catch (NumberFormatException e) {
				throw new RepositoryException("could not read protocol version from server: ", e);
			}
			catch (IOException e) {
				throw new RepositoryException("could not read protocol version from server: ", e);
			}
		}
		return compatibleMode;
	}
}
