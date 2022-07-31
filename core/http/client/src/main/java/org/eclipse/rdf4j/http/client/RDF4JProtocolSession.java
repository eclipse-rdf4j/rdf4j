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
package org.eclipse.rdf4j.http.client;

import static org.eclipse.rdf4j.http.protocol.Protocol.TRANSACTION_SETTINGS_PREFIX;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.Action;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.http.protocol.transaction.TransactionWriter;
import org.eclipse.rdf4j.http.protocol.transaction.operations.TransactionOperation;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SPARQLProtocolSession} subclass which extends the standard SPARQL 1.1 Protocol with additional
 * functionality, as documented in the <a href="http://docs.rdf4j.org/rest-api">RDF4J REST API</a>.
 *
 * @author Andreas Schwarte
 * @author Jeen Broekstra
 * @see <a href="http://docs.rdf4j.org/rest-api">RDF4J REST API</a>
 */
public class RDF4JProtocolSession extends SPARQLProtocolSession {

	/**
	 * How long the client should wait before sending another PING to the server
	 */
	private static final long PINGDELAY = TimeUnit.MILLISECONDS.convert(Protocol.DEFAULT_TIMEOUT, TimeUnit.SECONDS) / 2;

	private final Logger logger = LoggerFactory.getLogger(RDF4JProtocolSession.class);

	private String serverURL;

	private String transactionURL;

	private final ScheduledExecutorService pingScheduler;

	private ScheduledFuture<?> ping;

	private long pingDelay = PINGDELAY;

	/**
	 *
	 * @deprecated since 3.6.2 - use {@link #RDF4JProtocolSession(HttpClient, ExecutorService)} instead
	 */
	@Deprecated
	public RDF4JProtocolSession(HttpClient client, ScheduledExecutorService executor) {
		this(client, (ExecutorService) executor);
	}

	public RDF4JProtocolSession(HttpClient client, ExecutorService executor) {
		super(client, executor);

		// we want to preserve bnode ids to allow RDF4J API methods to match
		// blank nodes.
		getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

		// RDF4J Protocol has a preference for binary response formats, as these are
		// most performant
		setPreferredTupleQueryResultFormat(TupleQueryResultFormat.BINARY);
		setPreferredRDFFormat(RDFFormat.BINARY);
		try {
			final String configuredValue = System.getProperty(Protocol.CACHE_TIMEOUT_PROPERTY);
			if (configuredValue != null) {
				int timeout = Integer.parseInt(configuredValue);
				pingDelay = TimeUnit.MILLISECONDS.convert(Math.max(timeout, 1), TimeUnit.SECONDS) / 2;
			}
		} catch (Exception e) {
			logger.warn("Could not read integer value of system property {}", Protocol.CACHE_TIMEOUT_PROPERTY);
		}

		// use a single-threaded scheduled executor to handle keepalive pings for transactions
		pingScheduler = Executors.newSingleThreadScheduledExecutor((Runnable runnable) -> {
			Thread thread = Executors.defaultThreadFactory().newThread(runnable);
			thread.setName("rdf4j-pingScheduler");
			thread.setDaemon(true);
			return thread;
		});
	}

	public void setServerURL(String serverURL) {
		if (serverURL == null) {
			throw new IllegalArgumentException("serverURL must not be null");
		}

		this.serverURL = serverURL;
	}

	public String getServerURL() {
		return serverURL;
	}

	public String getRepositoryURL() {
		return this.getQueryURL();
	}

	public void setRepository(String repositoryURL) {
		// Try to parse the server URL from the repository URL
		Pattern urlPattern = Pattern.compile("(.*)/" + Protocol.REPOSITORIES + "/[^/]*/?");
		Matcher matcher = urlPattern.matcher(repositoryURL);

		if (matcher.matches() && matcher.groupCount() == 1) {
			setServerURL(matcher.group(1));
		}

		setQueryURL(repositoryURL);
	}

	protected void checkRepositoryURL() {
		if (getRepositoryURL() == null) {
			throw new IllegalStateException("Repository URL has not been set");
		}
	}

	protected void checkServerURL() {
		if (serverURL == null) {
			throw new IllegalStateException("Server URL has not been set");
		}
	}

	@Override
	public String getUpdateURL() {
		return Protocol.getStatementsLocation(getQueryURL());
	}

	@Override
	public void close() {
		try {
			super.close();
		} finally {
			transactionURL = null;
			if (ping != null) {
				ping.cancel(false);
				ping = null;
			}
			pingScheduler.shutdownNow();
		}
	}

	private synchronized String getTransactionURL() {
		return transactionURL;
	}

	/*-----------------*
	 * Repository list *
	 *-----------------*/

	public TupleQueryResult getRepositoryList()
			throws IOException, RepositoryException, UnauthorizedException, QueryInterruptedException {
		try {
			TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
			getRepositoryList(builder);
			return builder.getQueryResult();
		} catch (TupleQueryResultHandlerException e) {
			// Found a bug in TupleQueryResultBuilder?
			throw new RuntimeException(e);
		}
	}

	public void getRepositoryList(TupleQueryResultHandler handler) throws IOException, TupleQueryResultHandlerException,
			RepositoryException, UnauthorizedException, QueryInterruptedException {
		checkServerURL();

		HttpGet method = applyAdditionalHeaders(new HttpGet(Protocol.getRepositoriesLocation(serverURL)));

		try {
			getTupleQueryResult(method, handler);
		} catch (MalformedQueryException e) {
			// This shouldn't happen as no queries are involved
			logger.warn("Server reported unexpected malfored query error", e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			method.reset();
		}
	}

	/*------------------*
	 * Protocol version *
	 *------------------*/

	public String getServerProtocol() throws IOException, RepositoryException, UnauthorizedException {
		checkServerURL();

		HttpGet method = applyAdditionalHeaders(new HttpGet(Protocol.getProtocolLocation(serverURL)));

		try {
			return EntityUtils.toString(executeOK(method).getEntity());
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		} finally {
			method.reset();
		}
	}

	/*-------------------------*
	 * Repository/context size *
	 *-------------------------*/

	public long size(Resource... contexts) throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		try {
			String transactionURL = getTransactionURL();
			final boolean useTransaction = transactionURL != null;

			String baseLocation = useTransaction ? appendAction(transactionURL, Action.SIZE)
					: Protocol.getSizeLocation(getQueryURL());
			URIBuilder url = new URIBuilder(baseLocation);

			String[] encodedContexts = Protocol.encodeContexts(contexts);
			for (int i = 0; i < encodedContexts.length; i++) {
				url.addParameter(Protocol.CONTEXT_PARAM_NAME, encodedContexts[i]);
			}

			HttpRequestBase method = useTransaction ? new HttpPut(url.build()) : new HttpGet(url.build());
			applyAdditionalHeaders(method);

			try {
				String response = EntityUtils.toString(executeOK(method).getEntity());
				pingTransaction();

				try {
					return Long.parseLong(response);
				} catch (NumberFormatException e) {
					throw new RepositoryException("Server responded with invalid size value: " + response);
				}
			} finally {
				method.reset();
			}
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Create a new repository.
	 *
	 * @param config the repository configuration
	 * @throws IOException
	 * @throws RepositoryException
	 */
	public void createRepository(RepositoryConfig config) throws IOException, RepositoryException {
		String baseURI = Protocol.getRepositoryLocation(serverURL, config.getID());
		setRepository(baseURI);
		Resource ctx = SimpleValueFactory.getInstance().createIRI(baseURI + "#" + config.getID());
		Model model = new LinkedHashModel();
		config.export(model, ctx);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Rio.write(model, baos, getPreferredRDFFormat());

		HttpEntityEnclosingRequestBase method = null;
		try (InputStream contents = new ByteArrayInputStream(baos.toByteArray())) {
			HttpEntity entity = new InputStreamEntity(contents, -1,
					ContentType.parse(getPreferredRDFFormat().getDefaultMIMEType()));
			method = applyAdditionalHeaders(new HttpPut(baseURI));
			method.setEntity(entity);
			executeNoContent((HttpUriRequest) method);
		} catch (RepositoryException | RDFParseException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		} finally {
			if (method != null) {
				method.reset();
			}
		}
	}

	/**
	 * Update the config of an existing repository.
	 *
	 * @param config the repository configuration
	 * @throws IOException
	 * @throws RepositoryException
	 */
	public void updateRepository(RepositoryConfig config) throws IOException, RepositoryException {
		String baseURI = Protocol.getRepositoryLocation(serverURL, config.getID());
		setRepository(baseURI);
		Resource ctx = SimpleValueFactory.getInstance().createIRI(baseURI + "#" + config.getID());
		Model model = new LinkedHashModel();
		config.export(model, ctx);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Rio.write(model, baos, getPreferredRDFFormat());

		HttpEntityEnclosingRequestBase method = null;
		try (InputStream contents = new ByteArrayInputStream(baos.toByteArray())) {
			HttpEntity entity = new InputStreamEntity(contents, -1,
					ContentType.parse(getPreferredRDFFormat().getDefaultMIMEType()));
			method = applyAdditionalHeaders(new HttpPost(Protocol.getRepositoryConfigLocation(baseURI)));
			method.setEntity(entity);
			executeNoContent((HttpUriRequest) method);
		} catch (RepositoryException | RDFParseException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		} finally {
			if (method != null) {
				method.reset();
			}
		}
	}

	public void deleteRepository(String repositoryID) throws IOException, RepositoryException {

		HttpDelete method = applyAdditionalHeaders(
				new HttpDelete(Protocol.getRepositoryLocation(serverURL, repositoryID)));

		try {
			executeNoContent(method);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		} finally {
			method.reset();
		}
	}

	/**
	 * Retrieve configuration of the current repository and send it to the supplied {@link StatementCollector}
	 *
	 * @param statementCollector receiver of the repository config information
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws RDFHandlerException
	 * @throws QueryInterruptedException
	 * @throws UnauthorizedException
	 *
	 * @since 3.1.0
	 */
	public void getRepositoryConfig(StatementCollector statementCollector) throws UnauthorizedException,
			QueryInterruptedException, RDFHandlerException, RepositoryException, IOException {
		checkRepositoryURL();

		try {
			String baseLocation = Protocol.getRepositoryConfigLocation(getRepositoryURL());
			URIBuilder url = new URIBuilder(baseLocation);

			HttpRequestBase method = new HttpGet(url.build());
			method = applyAdditionalHeaders(method);

			try {
				getRDF(method, statementCollector, true);
			} catch (MalformedQueryException e) {
				logger.warn("Server reported unexpected malformed query error", e);
				throw new RepositoryException(e.getMessage(), e);
			} finally {
				method.reset();
			}
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
	}

	/*---------------------------*
	 * Get/add/remove namespaces *
	 *---------------------------*/

	public TupleQueryResult getNamespaces()
			throws IOException, RepositoryException, UnauthorizedException, QueryInterruptedException {
		try {
			TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
			getNamespaces(builder);
			return builder.getQueryResult();
		} catch (TupleQueryResultHandlerException e) {
			// Found a bug in TupleQueryResultBuilder?
			throw new RuntimeException(e);
		}
	}

	public void getNamespaces(TupleQueryResultHandler handler) throws IOException, TupleQueryResultHandlerException,
			RepositoryException, UnauthorizedException, QueryInterruptedException {
		checkRepositoryURL();

		HttpGet method = applyAdditionalHeaders(new HttpGet(Protocol.getNamespacesLocation(getQueryURL())));

		try {
			getTupleQueryResult(method, handler);
		} catch (MalformedQueryException e) {
			logger.warn("Server reported unexpected malfored query error", e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			method.reset();
		}
	}

	public String getNamespace(String prefix) throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		HttpGet method = applyAdditionalHeaders(
				new HttpGet(Protocol.getNamespacePrefixLocation(getQueryURL(), prefix)));

		try {
			HttpResponse response = execute(method);
			int code = response.getStatusLine().getStatusCode();
			if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_NOT_AUTHORITATIVE) {
				return EntityUtils.toString(response.getEntity());
			} else {
				EntityUtils.consume(response.getEntity());
				return null;
			}
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		} finally {
			method.reset();
		}
	}

	public void setNamespacePrefix(String prefix, String name)
			throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		HttpPut method = applyAdditionalHeaders(
				new HttpPut(Protocol.getNamespacePrefixLocation(getQueryURL(), prefix)));

		try {
			method.setEntity(new StringEntity(name, ContentType.create("text/plain", StandardCharsets.UTF_8)));
			executeNoContent(method);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		} finally {
			method.reset();
		}
	}

	public void removeNamespacePrefix(String prefix) throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		HttpDelete method = applyAdditionalHeaders(
				new HttpDelete(Protocol.getNamespacePrefixLocation(getQueryURL(), prefix)));

		try {
			executeNoContent(method);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		} finally {
			method.reset();
		}
	}

	public void clearNamespaces() throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		HttpDelete method = applyAdditionalHeaders(new HttpDelete(Protocol.getNamespacesLocation(getQueryURL())));

		try {
			executeNoContent(method);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		} finally {
			method.reset();
		}
	}

	/*-------------*
	 * Context IDs *
	 *-------------*/

	public TupleQueryResult getContextIDs()
			throws IOException, RepositoryException, UnauthorizedException, QueryInterruptedException {
		try {
			TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
			getContextIDs(builder);
			return builder.getQueryResult();
		} catch (TupleQueryResultHandlerException e) {
			// Found a bug in TupleQueryResultBuilder?
			throw new RuntimeException(e);
		}
	}

	public void getContextIDs(TupleQueryResultHandler handler) throws IOException, TupleQueryResultHandlerException,
			RepositoryException, UnauthorizedException, QueryInterruptedException {
		checkRepositoryURL();

		HttpGet method = applyAdditionalHeaders(new HttpGet(Protocol.getContextsLocation(getQueryURL())));

		try {
			getTupleQueryResult(method, handler);
		} catch (MalformedQueryException e) {
			logger.warn("Server reported unexpected malfored query error", e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			method.reset();
		}
	}

	/*---------------------------*
	 * Get/add/remove statements *
	 *---------------------------*/

	public void getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
			Resource... contexts) throws IOException, RDFHandlerException, RepositoryException, UnauthorizedException,
			QueryInterruptedException {
		checkRepositoryURL();

		try {
			String transactionURL = getTransactionURL();
			final boolean useTransaction = transactionURL != null;

			String baseLocation = useTransaction ? transactionURL : Protocol.getStatementsLocation(getQueryURL());
			URIBuilder url = new URIBuilder(baseLocation);

			if (subj != null) {
				url.setParameter(Protocol.SUBJECT_PARAM_NAME, Protocol.encodeValue(subj));
			}
			if (pred != null) {
				url.setParameter(Protocol.PREDICATE_PARAM_NAME, Protocol.encodeValue(pred));
			}
			if (obj != null) {
				url.setParameter(Protocol.OBJECT_PARAM_NAME, Protocol.encodeValue(obj));
			}
			for (String encodedContext : Protocol.encodeContexts(contexts)) {
				url.addParameter(Protocol.CONTEXT_PARAM_NAME, encodedContext);
			}
			url.setParameter(Protocol.INCLUDE_INFERRED_PARAM_NAME, Boolean.toString(includeInferred));
			if (useTransaction) {
				url.setParameter(Protocol.ACTION_PARAM_NAME, Action.GET.toString());
			}

			HttpRequestBase method = useTransaction ? new HttpPut(url.build()) : new HttpGet(url.build());
			method = applyAdditionalHeaders(method);

			try {
				getRDF(method, handler, true);
			} catch (MalformedQueryException e) {
				logger.warn("Server reported unexpected malfored query error", e);
				throw new RepositoryException(e.getMessage(), e);
			} finally {
				method.reset();
			}
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
		pingTransaction();
	}

	public synchronized void beginTransaction(IsolationLevel isolationLevel)
			throws RDF4JException, IOException, UnauthorizedException {
		beginTransaction((TransactionSetting) isolationLevel);
	}

	public synchronized void beginTransaction(TransactionSetting... transactionSettings)
			throws RDF4JException, IOException, UnauthorizedException {
		checkRepositoryURL();

		if (transactionURL != null) {
			throw new IllegalStateException("Transaction URL is already set");
		}

		HttpPost method = applyAdditionalHeaders(new HttpPost(Protocol.getTransactionsLocation(getRepositoryURL())));

		try {
			method.setHeader("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8");

			List<NameValuePair> params = new ArrayList<>();

			for (TransactionSetting transactionSetting : transactionSettings) {
				if (transactionSetting == null) {
					continue;
				}
				params.add(
						new BasicNameValuePair(
								TRANSACTION_SETTINGS_PREFIX + transactionSetting.getName(),
								transactionSetting.getValue()
						)
				);
			}

			method.setEntity(new UrlEncodedFormEntity(params, UTF8));
			HttpResponse response = execute(method);
			try {
				int code = response.getStatusLine().getStatusCode();

				if (code == HttpURLConnection.HTTP_CREATED) {
					transactionURL = response.getFirstHeader("Location").getValue();
					if (transactionURL == null) {
						throw new RepositoryException("no valid transaction ID received in server response.");
					} else {
						pingTransaction();
					}
				} else {
					throw new RepositoryException("unable to start transaction. HTTP error code " + code);
				}
			} finally {
				EntityUtils.consume(response.getEntity());
			}
		} finally {
			method.reset();
		}
	}

	public synchronized void prepareTransaction() throws RDF4JException, IOException, UnauthorizedException {
		checkRepositoryURL();

		if (transactionURL == null) {
			throw new IllegalStateException("Transaction URL has not been set");
		}

		HttpPut method = null;
		try {
			URIBuilder url = new URIBuilder(transactionURL);
			url.addParameter(Protocol.ACTION_PARAM_NAME, Action.PREPARE.toString());
			method = applyAdditionalHeaders(new HttpPut(url.build()));

			final HttpResponse response = execute(method);
			try {
				int code = response.getStatusLine().getStatusCode();
				if (code == HttpURLConnection.HTTP_OK) {
				} else {
					throw new RepositoryException("unable to prepare transaction. HTTP error code " + code);
				}
			} finally {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		} catch (URISyntaxException e) {
			logger.error("could not create URL for transaction prepare", e);
			throw new RuntimeException(e);
		} finally {
			if (method != null) {
				method.reset();
			}
		}
	}

	public synchronized void commitTransaction() throws RDF4JException, IOException, UnauthorizedException {
		checkRepositoryURL();

		if (transactionURL == null) {
			throw new IllegalStateException("Transaction URL has not been set");
		}

		HttpPut method = null;
		try {
			URIBuilder url = new URIBuilder(transactionURL);
			url.addParameter(Protocol.ACTION_PARAM_NAME, Action.COMMIT.toString());
			method = applyAdditionalHeaders(new HttpPut(url.build()));

			final HttpResponse response = execute(method);
			try {
				int code = response.getStatusLine().getStatusCode();
				if (code == HttpURLConnection.HTTP_OK) {
					// we're done.
					transactionURL = null;
					if (ping != null) {
						ping.cancel(false);
					}
				} else {
					throw new RepositoryException("unable to commit transaction. HTTP error code " + code);
				}
			} finally {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		} catch (URISyntaxException e) {
			logger.error("could not create URL for transaction commit", e);
			throw new RuntimeException(e);
		} finally {
			if (method != null) {
				method.reset();
			}
		}
	}

	public synchronized void rollbackTransaction() throws RDF4JException, IOException, UnauthorizedException {
		checkRepositoryURL();

		if (transactionURL == null) {
			throw new IllegalStateException("Transaction URL has not been set");
		}

		String requestURL = transactionURL;
		HttpDelete method = applyAdditionalHeaders(new HttpDelete(requestURL));

		try {
			final HttpResponse response = execute(method);
			try {
				int code = response.getStatusLine().getStatusCode();
				if (code == HttpURLConnection.HTTP_NO_CONTENT) {
					// we're done.
					transactionURL = null;
					if (ping != null) {
						ping.cancel(false);
					}
				} else {
					throw new RepositoryException("unable to rollback transaction. HTTP error code " + code);
				}
			} finally {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		} finally {
			method.reset();
		}
	}

	private synchronized void pingTransaction() {
		if (transactionURL == null) {
			return;
		}
		if (ping != null) {
			ping.cancel(false);
		}
		if (pingDelay > 0) {
			ping = pingScheduler.schedule(() -> {
				executeTransactionPing();
			}, pingDelay, TimeUnit.MILLISECONDS);
		}
	}

	void executeTransactionPing() {
		String transactionURL = getTransactionURL();
		if (transactionURL == null) {
			return; // transaction has already been closed
		}
		HttpPost method;
		try {
			URIBuilder url = new URIBuilder(transactionURL);
			url.addParameter(Protocol.ACTION_PARAM_NAME, Action.PING.toString());
			method = applyAdditionalHeaders(new HttpPost(url.build()));
			String text = EntityUtils.toString(executeOK(method).getEntity());
			long timeout = Long.parseLong(text);
			// clients should ping before server timeouts transaction
			long nextPingDelay = timeout / 2;
			synchronized (this) {
				if (pingDelay != nextPingDelay) {
					pingDelay = nextPingDelay;
				}
			}
		} catch (Exception e) {
			logger.warn("Failed to ping transaction", e.toString());
		}
		pingTransaction(); // reschedule
	}

	/**
	 * Appends the action as a parameter to the supplied url
	 *
	 * @param url    a url on which to append the parameter. it is assumed the url has no parameters.
	 * @param action the action to add as a parameter
	 * @return the url parametrized with the supplied action
	 */
	private String appendAction(String url, Action action) {
		return url + "?" + Protocol.ACTION_PARAM_NAME + "=" + action.toString();
	}

	/**
	 * Sends a transaction list as serialized XML to the server.
	 *
	 * @deprecated since 2.8.0
	 * @param txn
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws UnauthorizedException
	 */
	@Deprecated
	public void sendTransaction(final Iterable<? extends TransactionOperation> txn)
			throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		HttpPost method = applyAdditionalHeaders(new HttpPost(Protocol.getStatementsLocation(getQueryURL())));

		try {
			// Create a RequestEntity for the transaction data
			method.setEntity(new AbstractHttpEntity() {

				@Override
				public long getContentLength() {
					return -1; // don't know
				}

				@Override
				public Header getContentType() {
					return new BasicHeader("Content-Type", Protocol.TXN_MIME_TYPE);
				}

				@Override
				public boolean isRepeatable() {
					return true;
				}

				@Override
				public boolean isStreaming() {
					return true;
				}

				@Override
				public InputStream getContent() throws IOException, IllegalStateException {
					ByteArrayOutputStream buf = new ByteArrayOutputStream();
					writeTo(buf);
					return new ByteArrayInputStream(buf.toByteArray());
				}

				@Override
				public void writeTo(OutputStream out) throws IOException {
					TransactionWriter txnWriter = new TransactionWriter();
					txnWriter.serialize(txn, out);
				}
			});

			try {
				executeNoContent(method);
			} catch (RepositoryException e) {
				throw e;
			} catch (RDF4JException e) {
				throw new RepositoryException(e);
			}
		} finally {
			method.reset();
		}
	}

	public void addData(InputStream contents, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws UnauthorizedException, RDFParseException, RepositoryException, IOException {
		upload(contents, baseURI, dataFormat, false, true, Action.ADD, contexts);
	}

	public void removeData(InputStream contents, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws UnauthorizedException, RDFParseException, RepositoryException, IOException {
		upload(contents, baseURI, dataFormat, false, true, Action.DELETE, contexts);
	}

	public void upload(InputStream contents, String baseURI, RDFFormat dataFormat, boolean overwrite,
			boolean preserveNodeIds, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException, UnauthorizedException {
		upload(contents, baseURI, dataFormat, overwrite, preserveNodeIds, Action.ADD, contexts);
	}

	protected void upload(InputStream contents, String baseURI, RDFFormat dataFormat, boolean overwrite,
			boolean preserveNodeIds, Action action, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException, UnauthorizedException {
		// Set Content-Length to -1 as we don't know it and we also don't want to
		// cache
		HttpEntity entity = new InputStreamEntity(contents, -1, ContentType.parse(dataFormat.getDefaultMIMEType()));
		upload(entity, baseURI, overwrite, preserveNodeIds, action, contexts);
	}

	public void upload(final Reader contents, String baseURI, final RDFFormat dataFormat, boolean overwrite,
			boolean preserveNodeIds, Resource... contexts)
			throws UnauthorizedException, RDFParseException, RepositoryException, IOException {
		upload(contents, baseURI, dataFormat, overwrite, preserveNodeIds, Action.ADD, contexts);
	}

	@Override
	protected HttpUriRequest getQueryMethod(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings) {
		RequestBuilder builder;
		String transactionURL = getTransactionURL();
		if (transactionURL != null) {
			builder = RequestBuilder.put(transactionURL);
			builder.setHeader("Content-Type", Protocol.SPARQL_QUERY_MIME_TYPE + "; charset=utf-8");
			builder.addParameter(Protocol.ACTION_PARAM_NAME, Action.QUERY.toString());
			for (NameValuePair nvp : getQueryMethodParameters(ql, null, baseURI, dataset, includeInferred, maxQueryTime,
					bindings)) {
				builder.addParameter(nvp);
			}
			// in a PUT request, we carry the actual query string as the entity
			// body rather than a parameter.
			builder.setEntity(new StringEntity(query, UTF8));
			pingTransaction();
		} else {
			builder = RequestBuilder.post(getQueryURL());
			builder.setHeader("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8");

			builder.setEntity(new UrlEncodedFormEntity(
					getQueryMethodParameters(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings),
					UTF8));
		}
		// functionality to provide custom http headers as required by the
		// applications
		for (Map.Entry<String, String> additionalHeader : getAdditionalHttpHeaders().entrySet()) {
			builder.addHeader(additionalHeader.getKey(), additionalHeader.getValue());
		}
		return builder.build();
	}

	@Override
	protected HttpUriRequest getUpdateMethod(QueryLanguage ql, String update, String baseURI, Dataset dataset,
			boolean includeInferred, int maxExecutionTime, Binding... bindings) {
		RequestBuilder builder;
		String transactionURL = getTransactionURL();
		if (transactionURL != null) {
			builder = RequestBuilder.put(transactionURL);
			builder.addHeader("Content-Type", Protocol.SPARQL_UPDATE_MIME_TYPE + "; charset=utf-8");
			builder.addParameter(Protocol.ACTION_PARAM_NAME, Action.UPDATE.toString());
			for (NameValuePair nvp : getUpdateMethodParameters(ql, null, baseURI, dataset, includeInferred,
					maxExecutionTime, bindings)) {
				builder.addParameter(nvp);
			}
			// in a PUT request, we carry the only actual update string as the
			// request body - the rest is sent as request parameters
			builder.setEntity(new StringEntity(update, UTF8));
			pingTransaction();
		} else {
			builder = RequestBuilder.post(getUpdateURL());
			builder.addHeader("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8");

			builder.setEntity(new UrlEncodedFormEntity(getUpdateMethodParameters(ql, update, baseURI, dataset,
					includeInferred, maxExecutionTime, bindings), UTF8));
		}
		// functionality to provide custom http headers as required by the
		// applications
		for (Map.Entry<String, String> additionalHeader : getAdditionalHttpHeaders().entrySet()) {
			builder.addHeader(additionalHeader.getKey(), additionalHeader.getValue());
		}
		return builder.build();
	}

	protected void upload(final Reader contents, String baseURI, final RDFFormat dataFormat, boolean overwrite,
			boolean preserveNodeIds, Action action, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException, UnauthorizedException {
		final Charset charset = dataFormat.hasCharset() ? dataFormat.getCharset() : StandardCharsets.UTF_8;

		HttpEntity entity = new AbstractHttpEntity() {

			private InputStream content;

			@Override
			public long getContentLength() {
				return -1; // don't know
			}

			@Override
			public Header getContentType() {
				return new BasicHeader("Content-Type", dataFormat.getDefaultMIMEType() + "; charset=" + charset.name());
			}

			@Override
			public boolean isRepeatable() {
				return false;
			}

			@Override
			public boolean isStreaming() {
				return true;
			}

			@Override
			public synchronized InputStream getContent() throws IOException, IllegalStateException {
				if (content == null) {
					ByteArrayOutputStream buf = new ByteArrayOutputStream();
					writeTo(buf);
					content = new ByteArrayInputStream(buf.toByteArray());
				}
				return content;
			}

			@Override
			public void writeTo(OutputStream out) throws IOException {
				try (contents) {
					OutputStreamWriter writer = new OutputStreamWriter(out, charset);
					IOUtil.transfer(contents, writer);
					writer.flush();
				}
			}
		};

		upload(entity, baseURI, overwrite, preserveNodeIds, action, contexts);
	}

	protected void upload(HttpEntity reqEntity, String baseURI, boolean overwrite, boolean preserveNodeIds,
			Action action, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException, UnauthorizedException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		checkRepositoryURL();

		String transactionURL = getTransactionURL();
		boolean useTransaction = transactionURL != null;

		try {

			String baseLocation = useTransaction ? transactionURL : Protocol.getStatementsLocation(getQueryURL());
			URIBuilder url = new URIBuilder(baseLocation);

			// Set relevant query parameters
			for (String encodedContext : Protocol.encodeContexts(contexts)) {
				url.addParameter(Protocol.CONTEXT_PARAM_NAME, encodedContext);
			}
			if (baseURI != null && baseURI.trim().length() != 0) {
				String encodedBaseURI = Protocol.encodeValue(SimpleValueFactory.getInstance().createIRI(baseURI));
				url.setParameter(Protocol.BASEURI_PARAM_NAME, encodedBaseURI);
			}
			if (preserveNodeIds) {
				url.setParameter(Protocol.PRESERVE_BNODE_ID_PARAM_NAME, "true");
			}

			if (useTransaction) {
				if (action == null) {
					throw new IllegalArgumentException("action can not be null on transaction operation");
				}
				url.setParameter(Protocol.ACTION_PARAM_NAME, action.toString());
			}

			// Select appropriate HTTP method
			HttpEntityEnclosingRequestBase method = null;
			try {
				if (overwrite || useTransaction) {
					method = applyAdditionalHeaders(new HttpPut(url.build()));
				} else {
					method = applyAdditionalHeaders(new HttpPost(url.build()));
				}

				// Set payload
				method.setEntity(reqEntity);

				// Send request
				try {
					executeNoContent((HttpUriRequest) method);
				} catch (RepositoryException | RDFParseException e) {
					throw e;
				} catch (RDF4JException e) {
					throw new RepositoryException(e);
				}
			} finally {
				if (method != null) {
					method.reset();
				}
			}
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
		pingTransaction();
	}

	@Override
	public void setUsernameAndPassword(String username, String password) {
		checkServerURL();
		setUsernameAndPasswordForUrl(username, password, getServerURL());
	}

	@Override
	protected List<NameValuePair> getQueryMethodParameters(QueryLanguage ql, String query, String baseURI,
			Dataset dataset, boolean includeInferred, int maxQueryTime, Binding... bindings) {
		Objects.requireNonNull(ql, "QueryLanguage may not be null");

		List<NameValuePair> queryParams = new ArrayList<>();
		queryParams.add(new BasicNameValuePair(Protocol.QUERY_LANGUAGE_PARAM_NAME, ql.getName()));
		queryParams.add(new BasicNameValuePair(Protocol.QUERY_PARAM_NAME, query));

		if (baseURI != null) {
			queryParams.add(new BasicNameValuePair(Protocol.BASEURI_PARAM_NAME, baseURI));
		}

		queryParams
				.add(new BasicNameValuePair(Protocol.INCLUDE_INFERRED_PARAM_NAME, Boolean.toString(includeInferred)));

		if (maxQueryTime > 0) {
			queryParams.add(new BasicNameValuePair(Protocol.TIMEOUT_PARAM_NAME, Integer.toString(maxQueryTime)));
		}

		if (dataset != null) {
			for (IRI defaultGraphURI : dataset.getDefaultGraphs()) {
				queryParams.add(
						new BasicNameValuePair(Protocol.DEFAULT_GRAPH_PARAM_NAME, String.valueOf(defaultGraphURI)));
			}
			for (IRI namedGraphURI : dataset.getNamedGraphs()) {
				queryParams.add(new BasicNameValuePair(Protocol.NAMED_GRAPH_PARAM_NAME, String.valueOf(namedGraphURI)));
			}
		}

		for (int i = 0; i < bindings.length; i++) {
			String paramName = Protocol.BINDING_PREFIX + bindings[i].getName();
			String paramValue = Protocol.encodeValue(bindings[i].getValue());
			queryParams.add(new BasicNameValuePair(paramName, paramValue));
		}

		return queryParams;
	}

	@Override
	protected List<NameValuePair> getUpdateMethodParameters(QueryLanguage ql, String update, String baseURI,
			Dataset dataset, boolean includeInferred, int maxQueryTime, Binding... bindings) {
		Objects.requireNonNull(ql, "QueryLanguage may not be null");

		List<NameValuePair> queryParams = new ArrayList<>();

		queryParams.add(new BasicNameValuePair(Protocol.QUERY_LANGUAGE_PARAM_NAME, ql.getName()));

		if (update != null) {
			queryParams.add(new BasicNameValuePair(Protocol.UPDATE_PARAM_NAME, update));
		}

		if (baseURI != null) {
			queryParams.add(new BasicNameValuePair(Protocol.BASEURI_PARAM_NAME, baseURI));
		}

		queryParams
				.add(new BasicNameValuePair(Protocol.INCLUDE_INFERRED_PARAM_NAME, Boolean.toString(includeInferred)));

		if (dataset != null) {
			for (IRI graphURI : dataset.getDefaultRemoveGraphs()) {
				queryParams.add(new BasicNameValuePair(Protocol.REMOVE_GRAPH_PARAM_NAME, String.valueOf(graphURI)));
			}
			if (dataset.getDefaultInsertGraph() != null) {
				queryParams.add(new BasicNameValuePair(Protocol.INSERT_GRAPH_PARAM_NAME,
						String.valueOf(dataset.getDefaultInsertGraph())));
			}
			for (IRI defaultGraphURI : dataset.getDefaultGraphs()) {
				queryParams
						.add(new BasicNameValuePair(Protocol.USING_GRAPH_PARAM_NAME, String.valueOf(defaultGraphURI)));
			}
			for (IRI namedGraphURI : dataset.getNamedGraphs()) {
				queryParams.add(
						new BasicNameValuePair(Protocol.USING_NAMED_GRAPH_PARAM_NAME, String.valueOf(namedGraphURI)));
			}
		}

		if (maxQueryTime > 0) {
			queryParams.add(new BasicNameValuePair(Protocol.TIMEOUT_PARAM_NAME, Integer.toString(maxQueryTime)));
		}

		for (int i = 0; i < bindings.length; i++) {
			String paramName = Protocol.BINDING_PREFIX + bindings[i].getName();
			String paramValue = Protocol.encodeValue(bindings[i].getValue());
			queryParams.add(new BasicNameValuePair(paramName, paramValue));
		}

		return queryParams;
	}

	private <T extends HttpUriRequest> T applyAdditionalHeaders(T method) {
		for (Map.Entry<String, String> additionalHeader : getAdditionalHttpHeaders().entrySet()) {
			method.addHeader(additionalHeader.getKey(), additionalHeader.getValue());
		}
		return method;
	}
}
