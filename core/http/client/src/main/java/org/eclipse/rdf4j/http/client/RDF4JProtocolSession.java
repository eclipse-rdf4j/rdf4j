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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
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

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.http.client.spi.HttpClientRequest;
import org.eclipse.rdf4j.http.client.spi.HttpClientRequestBody;
import org.eclipse.rdf4j.http.client.spi.HttpClientRequests;
import org.eclipse.rdf4j.http.client.spi.HttpClientResponse;
import org.eclipse.rdf4j.http.client.spi.HttpClientResponses;
import org.eclipse.rdf4j.http.client.spi.NameValuePair;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.http.client.spi.UriBuilder;
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

	public RDF4JProtocolSession(RDF4JHttpClient client, ExecutorService executor) {
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

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.get(Protocol.getRepositoriesLocation(serverURL)))
				.build();

		try {
			getTupleQueryResult(method, handler);
		} catch (MalformedQueryException e) {
			// This shouldn't happen as no queries are involved
			logger.warn("Server reported unexpected malfored query error", e);
			throw new RepositoryException(e.getMessage(), e);
		}
	}

	/*------------------*
	 * Protocol version *
	 *------------------*/

	public String getServerProtocol() throws IOException, RepositoryException, UnauthorizedException {
		checkServerURL();

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.get(Protocol.getProtocolLocation(serverURL)))
				.build();

		try (HttpClientResponse response = executeOK(method)) {
			return HttpClientResponses.toString(response);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
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
			UriBuilder url = UriBuilder.from(baseLocation);

			String[] encodedContexts = Protocol.encodeContexts(contexts);
			for (int i = 0; i < encodedContexts.length; i++) {
				url.addParameter(Protocol.CONTEXT_PARAM_NAME, encodedContexts[i]);
			}

			HttpClientRequest method;
			if (useTransaction) {
				method = applyAdditionalHeaders(HttpClientRequests.put(url.build())).build();
			} else {
				method = applyAdditionalHeaders(HttpClientRequests.get(url.build())).build();
			}

			try (HttpClientResponse response = executeOK(method)) {
				String responseText = HttpClientResponses.toString(response);
				pingTransaction();

				try {
					return Long.parseLong(responseText);
				} catch (NumberFormatException e) {
					throw new RepositoryException("Server responded with invalid size value: " + responseText);
				}
			}
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
		byte[] bytes = baos.toByteArray();

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.put(baseURI)
						.body(HttpClientRequestBody.ofBytes(bytes, getPreferredRDFFormat().getDefaultMIMEType())))
				.build();

		try {
			executeNoContent(method);
		} catch (RepositoryException | RDFParseException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
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
		byte[] bytes = baos.toByteArray();

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.post(Protocol.getRepositoryConfigLocation(baseURI))
						.body(HttpClientRequestBody.ofBytes(bytes, getPreferredRDFFormat().getDefaultMIMEType())))
				.build();

		try {
			executeNoContent(method);
		} catch (RepositoryException | RDFParseException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		}
	}

	public void deleteRepository(String repositoryID) throws IOException, RepositoryException {

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.delete(Protocol.getRepositoryLocation(serverURL, repositoryID)))
				.build();

		try {
			executeNoContent(method);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
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
	 * @since 3.1.0
	 */
	public void getRepositoryConfig(StatementCollector statementCollector) throws UnauthorizedException,
			QueryInterruptedException, RDFHandlerException, RepositoryException, IOException {
		checkRepositoryURL();

		String baseLocation = Protocol.getRepositoryConfigLocation(getRepositoryURL());
		UriBuilder url = UriBuilder.from(baseLocation);

		HttpClientRequest method = applyAdditionalHeaders(HttpClientRequests.get(url.build())).build();

		try {
			getRDF(method, statementCollector, true);
		} catch (MalformedQueryException e) {
			logger.warn("Server reported unexpected malformed query error", e);
			throw new RepositoryException(e.getMessage(), e);
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

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.get(Protocol.getNamespacesLocation(getQueryURL())))
				.build();

		try {
			getTupleQueryResult(method, handler);
		} catch (MalformedQueryException e) {
			logger.warn("Server reported unexpected malfored query error", e);
			throw new RepositoryException(e.getMessage(), e);
		}
	}

	public String getNamespace(String prefix) throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.get(Protocol.getNamespacePrefixLocation(getQueryURL(), prefix)))
				.build();

		try (HttpClientResponse response = execute(method)) {
			int code = response.getStatusCode();
			if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_NOT_AUTHORITATIVE) {
				return HttpClientResponses.toString(response);
			} else {
				return null;
			}
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		}
	}

	public void setNamespacePrefix(String prefix, String name)
			throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.put(Protocol.getNamespacePrefixLocation(getQueryURL(), prefix))
						.body(HttpClientRequestBody.ofString(name, "text/plain", StandardCharsets.UTF_8)))
				.build();

		try {
			executeNoContent(method);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		}
	}

	public void removeNamespacePrefix(String prefix) throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.delete(Protocol.getNamespacePrefixLocation(getQueryURL(), prefix)))
				.build();

		try {
			executeNoContent(method);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		}
	}

	public void clearNamespaces() throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.delete(Protocol.getNamespacesLocation(getQueryURL())))
				.build();

		try {
			executeNoContent(method);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
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

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.get(Protocol.getContextsLocation(getQueryURL())))
				.build();

		try {
			getTupleQueryResult(method, handler);
		} catch (MalformedQueryException e) {
			logger.warn("Server reported unexpected malfored query error", e);
			throw new RepositoryException(e.getMessage(), e);
		}
	}

	/*---------------------------*
	 * Get/add/remove statements *
	 *---------------------------*/

	public void getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
			Resource... contexts) throws IOException, RDFHandlerException, RepositoryException, UnauthorizedException,
			QueryInterruptedException {
		checkRepositoryURL();

		String transactionURL = getTransactionURL();
		final boolean useTransaction = transactionURL != null;

		String baseLocation = useTransaction ? transactionURL : Protocol.getStatementsLocation(getQueryURL());
		UriBuilder url = UriBuilder.from(baseLocation);

		if (subj != null) {
			url.addParameter(Protocol.SUBJECT_PARAM_NAME, Protocol.encodeValue(subj));
		}
		if (pred != null) {
			url.addParameter(Protocol.PREDICATE_PARAM_NAME, Protocol.encodeValue(pred));
		}
		if (obj != null) {
			url.addParameter(Protocol.OBJECT_PARAM_NAME, Protocol.encodeValue(obj));
		}
		for (String encodedContext : Protocol.encodeContexts(contexts)) {
			url.addParameter(Protocol.CONTEXT_PARAM_NAME, encodedContext);
		}
		url.addParameter(Protocol.INCLUDE_INFERRED_PARAM_NAME, Boolean.toString(includeInferred));
		if (useTransaction) {
			url.addParameter(Protocol.ACTION_PARAM_NAME, Action.GET.toString());
		}

		HttpClientRequest method;
		if (useTransaction) {
			method = applyAdditionalHeaders(HttpClientRequests.put(url.build())).build();
		} else {
			method = applyAdditionalHeaders(HttpClientRequests.get(url.build())).build();
		}

		try {
			getRDF(method, handler, true);
		} catch (MalformedQueryException e) {
			logger.warn("Server reported unexpected malfored query error", e);
			throw new RepositoryException(e.getMessage(), e);
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

		List<NameValuePair> params = new ArrayList<>();

		for (TransactionSetting transactionSetting : transactionSettings) {
			if (transactionSetting == null) {
				continue;
			}
			params.add(
					NameValuePair.of(
							TRANSACTION_SETTINGS_PREFIX + transactionSetting.getName(),
							transactionSetting.getValue()
					)
			);
		}

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.post(Protocol.getTransactionsLocation(getRepositoryURL()))
						.header("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8")
						.body(HttpClientRequestBody.ofFormData(params)))
				.build();

		try (HttpClientResponse response = execute(method)) {
			int code = response.getStatusCode();

			if (code == HttpURLConnection.HTTP_CREATED) {
				transactionURL = response.getHeader("Location")
						.orElseThrow(() -> new RepositoryException(
								"no valid transaction ID received in server response."));
				pingTransaction();
			} else {
				throw new RepositoryException("unable to start transaction. HTTP error code " + code);
			}
		}
	}

	public synchronized void prepareTransaction() throws RDF4JException, IOException, UnauthorizedException {
		checkRepositoryURL();

		if (transactionURL == null) {
			throw new IllegalStateException("Transaction URL has not been set");
		}

		UriBuilder url = UriBuilder.from(transactionURL);
		url.addParameter(Protocol.ACTION_PARAM_NAME, Action.PREPARE.toString());
		HttpClientRequest method = applyAdditionalHeaders(HttpClientRequests.put(url.build())).build();

		try (HttpClientResponse response = execute(method)) {
			int code = response.getStatusCode();
			if (code == HttpURLConnection.HTTP_OK) {
				// prepared
			} else {
				throw new RepositoryException("unable to prepare transaction. HTTP error code " + code);
			}
		}
	}

	public synchronized void commitTransaction() throws RDF4JException, IOException, UnauthorizedException {
		checkRepositoryURL();

		if (transactionURL == null) {
			throw new IllegalStateException("Transaction URL has not been set");
		}

		UriBuilder url = UriBuilder.from(transactionURL);
		url.addParameter(Protocol.ACTION_PARAM_NAME, Action.COMMIT.toString());
		HttpClientRequest method = applyAdditionalHeaders(HttpClientRequests.put(url.build())).build();

		try (HttpClientResponse response = execute(method)) {
			int code = response.getStatusCode();
			if (code == HttpURLConnection.HTTP_OK) {
				// we're done.
				transactionURL = null;
				if (ping != null) {
					ping.cancel(false);
				}
			} else {
				throw new RepositoryException("unable to commit transaction. HTTP error code " + code);
			}
		}
	}

	public synchronized void rollbackTransaction() throws RDF4JException, IOException, UnauthorizedException {
		checkRepositoryURL();

		if (transactionURL == null) {
			throw new IllegalStateException("Transaction URL has not been set");
		}

		String requestURL = transactionURL;
		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.delete(requestURL))
				.build();

		try (HttpClientResponse response = execute(method)) {
			int code = response.getStatusCode();
			if (code == HttpURLConnection.HTTP_NO_CONTENT) {
				// we're done.
				transactionURL = null;
				if (ping != null) {
					ping.cancel(false);
				}
			} else {
				throw new RepositoryException("unable to rollback transaction. HTTP error code " + code);
			}
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
		try {
			UriBuilder url = UriBuilder.from(transactionURL);
			url.addParameter(Protocol.ACTION_PARAM_NAME, Action.PING.toString());
			HttpClientRequest method = applyAdditionalHeaders(HttpClientRequests.post(url.build())).build();
			try (HttpClientResponse response = executeOK(method)) {
				String text = HttpClientResponses.toString(response);
				long timeout = Long.parseLong(text);
				// clients should ping before server timeouts transaction
				long nextPingDelay = timeout / 2;
				synchronized (this) {
					if (pingDelay != nextPingDelay) {
						pingDelay = nextPingDelay;
					}
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
	 * @param txn
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws UnauthorizedException
	 * @deprecated since 2.8.0
	 */
	@Deprecated(since = "2.8.0")
	public void sendTransaction(final Iterable<? extends TransactionOperation> txn)
			throws IOException, RepositoryException, UnauthorizedException {
		checkRepositoryURL();

		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		TransactionWriter txnWriter = new TransactionWriter();
		txnWriter.serialize(txn, buf);
		byte[] bytes = buf.toByteArray();

		HttpClientRequest method = applyAdditionalHeaders(
				HttpClientRequests.post(Protocol.getStatementsLocation(getQueryURL()))
						.body(HttpClientRequestBody.ofBytes(bytes, Protocol.TXN_MIME_TYPE)))
				.build();

		try {
			executeNoContent(method);
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
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
		// buffer
		HttpClientRequestBody body = HttpClientRequestBody.ofStream(contents, dataFormat.getDefaultMIMEType(), -1);
		upload(body, baseURI, overwrite, preserveNodeIds, action, contexts);
	}

	public void upload(final Reader contents, String baseURI, final RDFFormat dataFormat, boolean overwrite,
			boolean preserveNodeIds, Resource... contexts)
			throws UnauthorizedException, RDFParseException, RepositoryException, IOException {
		upload(contents, baseURI, dataFormat, overwrite, preserveNodeIds, Action.ADD, contexts);
	}

	@Override
	protected HttpClientRequest getQueryMethod(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings) {
		HttpClientRequest.Builder builder;
		String transactionURL = getTransactionURL();
		if (transactionURL != null) {
			UriBuilder urib = UriBuilder.from(transactionURL);
			urib.addParameter(Protocol.ACTION_PARAM_NAME, Action.QUERY.toString());
			for (NameValuePair nvp : getQueryMethodParameters(ql, null, baseURI, dataset, includeInferred, maxQueryTime,
					bindings)) {
				urib.addParameter(nvp);
			}
			// in a PUT request, we carry the actual query string as the entity
			// body rather than a parameter.
			builder = HttpClientRequests.put(urib.build())
					.header("Content-Type", Protocol.SPARQL_QUERY_MIME_TYPE + "; charset=utf-8")
					.body(HttpClientRequestBody.ofString(query, Protocol.SPARQL_QUERY_MIME_TYPE + "; charset=utf-8",
							UTF8));
			pingTransaction();
		} else {
			List<NameValuePair> queryParams = getQueryMethodParameters(ql, query, baseURI, dataset, includeInferred,
					maxQueryTime, bindings);
			builder = HttpClientRequests.post(getQueryURL())
					.header("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8")
					.body(HttpClientRequestBody.ofFormData(queryParams));
		}
		// functionality to provide custom http headers as required by the applications
		for (Map.Entry<String, String> additionalHeader : getAdditionalHttpHeaders().entrySet()) {
			builder.header(additionalHeader.getKey(), additionalHeader.getValue());
		}
		String authHeader = getAuthorizationHeader();
		if (authHeader != null) {
			builder.header("Authorization", authHeader);
		}
		return builder.build();
	}

	@Override
	protected HttpClientRequest getUpdateMethod(QueryLanguage ql, String update, String baseURI, Dataset dataset,
			boolean includeInferred, int maxExecutionTime, Binding... bindings) {
		HttpClientRequest.Builder builder;
		String transactionURL = getTransactionURL();
		if (transactionURL != null) {
			UriBuilder urib = UriBuilder.from(transactionURL);
			urib.addParameter(Protocol.ACTION_PARAM_NAME, Action.UPDATE.toString());
			for (NameValuePair nvp : getUpdateMethodParameters(ql, null, baseURI, dataset, includeInferred,
					maxExecutionTime, bindings)) {
				urib.addParameter(nvp);
			}
			// in a PUT request, we carry the only actual update string as the
			// request body - the rest is sent as request parameters
			builder = HttpClientRequests.put(urib.build())
					.header("Content-Type", Protocol.SPARQL_UPDATE_MIME_TYPE + "; charset=utf-8")
					.body(HttpClientRequestBody.ofString(update, Protocol.SPARQL_UPDATE_MIME_TYPE + "; charset=utf-8",
							UTF8));
			pingTransaction();
		} else {
			List<NameValuePair> updateParams = getUpdateMethodParameters(ql, update, baseURI, dataset, includeInferred,
					maxExecutionTime, bindings);
			builder = HttpClientRequests.post(getUpdateURL())
					.header("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8")
					.body(HttpClientRequestBody.ofFormData(updateParams));
		}
		// functionality to provide custom http headers as required by the applications
		for (Map.Entry<String, String> additionalHeader : getAdditionalHttpHeaders().entrySet()) {
			builder.header(additionalHeader.getKey(), additionalHeader.getValue());
		}
		String authHeader = getAuthorizationHeader();
		if (authHeader != null) {
			builder.header("Authorization", authHeader);
		}
		return builder.build();
	}

	protected void upload(final Reader contents, String baseURI, final RDFFormat dataFormat, boolean overwrite,
			boolean preserveNodeIds, Action action, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException, UnauthorizedException {
		final Charset charset = dataFormat.hasCharset() ? dataFormat.getCharset() : StandardCharsets.UTF_8;

		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		try (OutputStreamWriter writer = new OutputStreamWriter(buf, charset)) {
			IOUtil.transfer(contents, writer);
			writer.flush();
		}
		byte[] bytes = buf.toByteArray();
		String contentType = dataFormat.getDefaultMIMEType() + "; charset=" + charset.name();
		HttpClientRequestBody body = HttpClientRequestBody.ofBytes(bytes, contentType);

		upload(body, baseURI, overwrite, preserveNodeIds, action, contexts);
	}

	protected void upload(HttpClientRequestBody reqBody, String baseURI, boolean overwrite, boolean preserveNodeIds,
			Action action, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException, UnauthorizedException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		checkRepositoryURL();

		String transactionURL = getTransactionURL();
		boolean useTransaction = transactionURL != null;

		String baseLocation = useTransaction ? transactionURL : Protocol.getStatementsLocation(getQueryURL());
		UriBuilder url = UriBuilder.from(baseLocation);

		// Set relevant query parameters
		for (String encodedContext : Protocol.encodeContexts(contexts)) {
			url.addParameter(Protocol.CONTEXT_PARAM_NAME, encodedContext);
		}
		if (baseURI != null && !baseURI.trim().isEmpty()) {
			String encodedBaseURI = Protocol.encodeValue(SimpleValueFactory.getInstance().createIRI(baseURI));
			url.addParameter(Protocol.BASEURI_PARAM_NAME, encodedBaseURI);
		}
		if (preserveNodeIds) {
			url.addParameter(Protocol.PRESERVE_BNODE_ID_PARAM_NAME, "true");
		}

		if (useTransaction) {
			if (action == null) {
				throw new IllegalArgumentException("action can not be null on transaction operation");
			}
			url.addParameter(Protocol.ACTION_PARAM_NAME, action.toString());
		}

		// Select appropriate HTTP method
		HttpClientRequest method;
		if (overwrite || useTransaction) {
			method = applyAdditionalHeaders(HttpClientRequests.put(url.build()).body(reqBody)).build();
		} else {
			method = applyAdditionalHeaders(HttpClientRequests.post(url.build()).body(reqBody)).build();
		}

		// Send request
		try {
			executeNoContent(method);
		} catch (RepositoryException | RDFParseException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
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
		queryParams.add(NameValuePair.of(Protocol.QUERY_LANGUAGE_PARAM_NAME, ql.getName()));
		queryParams.add(NameValuePair.of(Protocol.QUERY_PARAM_NAME, query));

		if (baseURI != null) {
			queryParams.add(NameValuePair.of(Protocol.BASEURI_PARAM_NAME, baseURI));
		}

		queryParams.add(NameValuePair.of(Protocol.INCLUDE_INFERRED_PARAM_NAME, Boolean.toString(includeInferred)));

		if (maxQueryTime > 0) {
			queryParams.add(NameValuePair.of(Protocol.TIMEOUT_PARAM_NAME, Integer.toString(maxQueryTime)));
		}

		if (dataset != null) {
			for (IRI defaultGraphURI : dataset.getDefaultGraphs()) {
				queryParams.add(
						NameValuePair.of(Protocol.DEFAULT_GRAPH_PARAM_NAME, String.valueOf(defaultGraphURI)));
			}
			for (IRI namedGraphURI : dataset.getNamedGraphs()) {
				queryParams.add(NameValuePair.of(Protocol.NAMED_GRAPH_PARAM_NAME, String.valueOf(namedGraphURI)));
			}
		}

		for (int i = 0; i < bindings.length; i++) {
			String paramName = Protocol.BINDING_PREFIX + bindings[i].getName();
			String paramValue = Protocol.encodeValue(bindings[i].getValue());
			queryParams.add(NameValuePair.of(paramName, paramValue));
		}

		return queryParams;
	}

	@Override
	protected List<NameValuePair> getUpdateMethodParameters(QueryLanguage ql, String update, String baseURI,
			Dataset dataset, boolean includeInferred, int maxQueryTime, Binding... bindings) {
		Objects.requireNonNull(ql, "QueryLanguage may not be null");

		List<NameValuePair> queryParams = new ArrayList<>();

		queryParams.add(NameValuePair.of(Protocol.QUERY_LANGUAGE_PARAM_NAME, ql.getName()));

		if (update != null) {
			queryParams.add(NameValuePair.of(Protocol.UPDATE_PARAM_NAME, update));
		}

		if (baseURI != null) {
			queryParams.add(NameValuePair.of(Protocol.BASEURI_PARAM_NAME, baseURI));
		}

		queryParams.add(NameValuePair.of(Protocol.INCLUDE_INFERRED_PARAM_NAME, Boolean.toString(includeInferred)));

		if (dataset != null) {
			for (IRI graphURI : dataset.getDefaultRemoveGraphs()) {
				queryParams.add(NameValuePair.of(Protocol.REMOVE_GRAPH_PARAM_NAME, String.valueOf(graphURI)));
			}
			if (dataset.getDefaultInsertGraph() != null) {
				queryParams.add(NameValuePair.of(Protocol.INSERT_GRAPH_PARAM_NAME,
						String.valueOf(dataset.getDefaultInsertGraph())));
			}
			for (IRI defaultGraphURI : dataset.getDefaultGraphs()) {
				queryParams.add(NameValuePair.of(Protocol.USING_GRAPH_PARAM_NAME, String.valueOf(defaultGraphURI)));
			}
			for (IRI namedGraphURI : dataset.getNamedGraphs()) {
				queryParams.add(
						NameValuePair.of(Protocol.USING_NAMED_GRAPH_PARAM_NAME, String.valueOf(namedGraphURI)));
			}
		}

		if (maxQueryTime > 0) {
			queryParams.add(NameValuePair.of(Protocol.TIMEOUT_PARAM_NAME, Integer.toString(maxQueryTime)));
		}

		for (int i = 0; i < bindings.length; i++) {
			String paramName = Protocol.BINDING_PREFIX + bindings[i].getName();
			String paramValue = Protocol.encodeValue(bindings[i].getValue());
			queryParams.add(NameValuePair.of(paramName, paramValue));
		}

		return queryParams;
	}

	/**
	 * Applies additional HTTP headers (from {@link #getAdditionalHttpHeaders()}) and the authorization header to the
	 * supplied request builder.
	 *
	 * @param builder the request builder to add headers to
	 * @return the same builder, with headers added
	 */
	protected HttpClientRequest.Builder applyAdditionalHeaders(HttpClientRequest.Builder builder) {
		for (Map.Entry<String, String> additionalHeader : getAdditionalHttpHeaders().entrySet()) {
			builder.header(additionalHeader.getKey(), additionalHeader.getValue());
		}
		String authHeader = getAuthorizationHeader();
		if (authHeader != null) {
			builder.header("Authorization", authHeader);
		}
		return builder;
	}
}
