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

import static org.eclipse.rdf4j.http.protocol.Protocol.ACCEPT_PARAM_NAME;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.exception.RDF4JConfigException;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.io.ByteSink;
import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.common.io.Sink;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.http.client.shacl.RemoteShaclValidationException;
import org.eclipse.rdf4j.http.client.spi.AuthenticationHandler;
import org.eclipse.rdf4j.http.client.spi.BasicAuthenticationHandler;
import org.eclipse.rdf4j.http.client.spi.HttpHeader;
import org.eclipse.rdf4j.http.client.spi.HttpRequest;
import org.eclipse.rdf4j.http.client.spi.HttpRequestBody;
import org.eclipse.rdf4j.http.client.spi.HttpRequests;
import org.eclipse.rdf4j.http.client.spi.HttpResponse;
import org.eclipse.rdf4j.http.client.spi.HttpUtils;
import org.eclipse.rdf4j.http.client.spi.NameValuePair;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.http.client.spi.UriBuilder;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.impl.BackgroundGraphResult;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParser;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParserRegistry;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserRegistry;
import org.eclipse.rdf4j.query.resultio.helpers.BackgroundTupleResult;
import org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SPARQLProtocolSession provides low level HTTP methods for communication with SPARQL endpoints. All methods are
 * compliant to the <a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol W3C Recommendation</a>.
 * <p/>
 * For both Tuple and Graph queries there is a variant which parses the result in the background, see
 * {@link BackgroundTupleResult} and {@link BackgroundGraphResult}. For boolean queries the result is parsed in the
 * current thread. All methods in this class guarantee that HTTP connections are closed properly and returned to the
 * connection pool. The methods in this class are not guaranteed to be thread-safe.
 * <p/>
 * Functionality specific to the RDF4J HTTP protocol can be found in {@link RDF4JProtocolSession} (which is used by
 * HTTPRepository).
 *
 * @author Herko ter Horst
 * @author Arjohn Kampman
 * @author Andreas Schwarte
 * @author Jeen Broekstra
 * @see RDF4JProtocolSession
 * @see <a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol (W3C Recommendation)</a>
 */
public class SPARQLProtocolSession implements HttpClientDependent, AutoCloseable {

	protected static final Charset UTF8 = StandardCharsets.UTF_8;

	/**
	 * The default value of the threshold for URL length, beyond which we use the POST method for SPARQL query requests.
	 * The default is based on the lowest common denominator for various web servers.
	 */
	public static final int DEFAULT_MAXIMUM_URL_LENGTH = 4083;

	/**
	 * @deprecated use {@link #DEFAULT_MAXIMUM_URL_LENGTH} instead.
	 */
	@Deprecated
	public static final int MAXIMUM_URL_LENGTH = DEFAULT_MAXIMUM_URL_LENGTH;

	/**
	 * System property for configuration of URL length threshold: {@code rdf4j.sparql.url.maxlength}. A threshold of 0
	 * (or a negative value) means that the POST method is used for <strong>every</strong> SPARQL query request.
	 */
	public static final String MAXIMUM_URL_LENGTH_PARAM = "rdf4j.sparql.url.maxlength";

	/**
	 * The threshold for URL length, beyond which we use the POST method. A threshold of 0 (or a negative value) means
	 * that the POST method is used for <strong>every</strong> SPARQL query request.
	 */
	private final int maximumUrlLength;

	final static Logger logger = LoggerFactory.getLogger(SPARQLProtocolSession.class);

	private ValueFactory valueFactory;

	private String queryURL;

	private String updateURL;

	private RDF4JHttpClient httpClient;

	private final BackgroundResultExecutor background;

	private AuthenticationHandler authenticationHandler;

	private ParserConfig parserConfig = new ParserConfig();

	private TupleQueryResultFormat preferredTQRFormat = TupleQueryResultFormat.SPARQL;

	private BooleanQueryResultFormat preferredBQRFormat = BooleanQueryResultFormat.TEXT;

	private RDFFormat preferredRDFFormat = RDFFormat.TURTLE;

	private Map<String, String> additionalHttpHeaders = Collections.emptyMap();

	private boolean passThroughEnabled = true;

	public SPARQLProtocolSession(RDF4JHttpClient client, ExecutorService executor) {
		this.httpClient = client;
		this.background = new BackgroundResultExecutor(executor);
		valueFactory = SimpleValueFactory.getInstance();

		// parser used for processing server response data should be lenient
		parserConfig.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		parserConfig.addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);

		// configure the maximum url length for SPARQL query GET requests
		int maximumUrlLength = DEFAULT_MAXIMUM_URL_LENGTH;
		String propertyValue = System.getProperty(MAXIMUM_URL_LENGTH_PARAM);
		if (propertyValue != null) {
			try {
				maximumUrlLength = Integer.parseInt(propertyValue);
			} catch (NumberFormatException e) {
				throw new RDF4JConfigException("integer value expected for property " + MAXIMUM_URL_LENGTH_PARAM, e);
			}
		}
		this.maximumUrlLength = maximumUrlLength;
	}

	@Override
	public final RDF4JHttpClient getHttpClient() {
		return httpClient;
	}

	@Override
	public void setHttpClient(RDF4JHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public void setValueFactory(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	public ValueFactory getValueFactory() {
		return valueFactory;
	}

	protected void setQueryURL(String queryURL) {
		if (queryURL == null) {
			throw new IllegalArgumentException("queryURL must not be null");
		}
		this.queryURL = queryURL;
	}

	protected void setUpdateURL(String updateURL) {
		if (updateURL == null) {
			throw new IllegalArgumentException("updateURL must not be null");
		}
		this.updateURL = updateURL;
	}

	/**
	 * Sets the preferred format for encoding tuple query results.
	 *
	 * @param format The preferred {@link TupleQueryResultFormat}, or <var>null</var> to indicate no specific format is
	 *               preferred.
	 */
	public void setPreferredTupleQueryResultFormat(TupleQueryResultFormat format) {
		preferredTQRFormat = format;
	}

	/**
	 * Gets the preferred {@link TupleQueryResultFormat} for encoding tuple query results. The
	 * {@link TupleQueryResultFormat#SPARQL SPARQL/XML} format is preferred by default.
	 *
	 * @return The preferred format, of <var>null</var> if no specific format is preferred.
	 */
	public TupleQueryResultFormat getPreferredTupleQueryResultFormat() {
		return preferredTQRFormat;
	}

	/**
	 * Sets the preferred format for encoding RDF documents.
	 *
	 * @param format The preferred {@link RDFFormat}, or <var>null</var> to indicate no specific format is preferred.
	 */
	public void setPreferredRDFFormat(RDFFormat format) {
		preferredRDFFormat = format;
	}

	/**
	 * Gets the preferred {@link RDFFormat} for encoding RDF documents. The {@link RDFFormat#TURTLE Turtle} format is
	 * preferred by default.
	 *
	 * @return The preferred format, of <var>null</var> if no specific format is preferred.
	 */
	public RDFFormat getPreferredRDFFormat() {
		return preferredRDFFormat;
	}

	/**
	 * Sets the preferred format for encoding boolean query results.
	 *
	 * @param format The preferred {@link BooleanQueryResultFormat}, or <var>null</var> to indicate no specific format
	 *               is preferred.
	 */
	public void setPreferredBooleanQueryResultFormat(BooleanQueryResultFormat format) {
		preferredBQRFormat = format;
	}

	/**
	 * Gets the preferred {@link BooleanQueryResultFormat} for encoding boolean query results. The
	 * {@link BooleanQueryResultFormat#TEXT binary} format is preferred by default.
	 *
	 * @return The preferred format, of <var>null</var> if no specific format is preferred.
	 */
	public BooleanQueryResultFormat getPreferredBooleanQueryResultFormat() {
		return preferredBQRFormat;
	}

	/**
	 * Set the username and password for authentication with the remote server.
	 *
	 * @param username the username
	 * @param password the password
	 */
	public void setUsernameAndPassword(String username, String password) {
		setUsernameAndPasswordForUrl(username, password, getQueryURL());
	}

	protected void setUsernameAndPasswordForUrl(String username, String password, String url) {
		if (username != null && password != null) {
			logger.debug("Setting username '{}' and password for server at {}.", username, url);
			this.authenticationHandler = new BasicAuthenticationHandler(username, password);
		} else {
			this.authenticationHandler = null;
		}
	}

	/**
	 * Sets an {@link AuthenticationHandler} that will be applied to every outgoing request. The handler may modify the
	 * request, for example by adding an {@code Authorization} header.
	 *
	 * @param handler the authentication handler to use, or {@code null} to remove authentication
	 */
	public void setAuthenticationHandler(AuthenticationHandler handler) {
		this.authenticationHandler = handler;
	}

	/**
	 * Returns the current {@link AuthenticationHandler}, or {@code null} if none has been set.
	 *
	 * @return the authentication handler, or {@code null}
	 */
	public AuthenticationHandler getAuthenticationHandler() {
		return authenticationHandler;
	}

	public String getQueryURL() {
		return queryURL;
	}

	public String getUpdateURL() {
		return updateURL;
	}

	@Override
	public void close() {
		background.close();
	}

	/*------------------*
	 * Query evaluation *
	 *------------------*/

	/**
	 * @deprecated WeakReference<?> callerRef argument will be removed
	 */
	@Deprecated(since = "4.1.2")
	public TupleQueryResult sendTupleQuery(QueryLanguage ql, String query, Dataset dataset, boolean includeInferred,
			WeakReference<?> callerRef,
			Binding... bindings) throws IOException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		return sendTupleQuery(ql, query, null, dataset, includeInferred, 0, callerRef, bindings);
	}

	/**
	 * @deprecated WeakReference<?> callerRef argument will be removed
	 */
	@Deprecated(since = "4.1.2")
	public TupleQueryResult sendTupleQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, WeakReference<?> callerRef, Binding... bindings)
			throws IOException, RepositoryException,
			MalformedQueryException, UnauthorizedException, QueryInterruptedException {
		HttpRequest method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
		return getBackgroundTupleQueryResult(method, callerRef);
	}

	public void sendTupleQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset, boolean includeInferred,
			int maxQueryTime, TupleQueryResultHandler handler, Binding... bindings)
			throws IOException, TupleQueryResultHandlerException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		HttpRequest method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
		getTupleQueryResult(method, handler);
	}

	public void sendUpdate(QueryLanguage ql, String update, String baseURI, Dataset dataset, boolean includeInferred,
			Binding... bindings) throws IOException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		sendUpdate(ql, update, baseURI, dataset, includeInferred, 0, bindings);
	}

	public void sendUpdate(QueryLanguage ql, String update, String baseURI, Dataset dataset, boolean includeInferred,
			int maxQueryTime, Binding... bindings) throws IOException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		HttpRequest method = getUpdateMethod(ql, update, baseURI, dataset, includeInferred, maxQueryTime,
				bindings);

		try {
			executeNoContent(method);
		} catch (RepositoryException | MalformedQueryException | QueryInterruptedException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * @deprecated WeakReference<?> callerRef argument will be removed
	 */
	@Deprecated(since = "4.1.2")
	public GraphQueryResult sendGraphQuery(QueryLanguage ql, String query, Dataset dataset, boolean includeInferred,
			WeakReference<?> callerRef,
			Binding... bindings) throws IOException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		return sendGraphQuery(ql, query, null, dataset, includeInferred, 0, callerRef, bindings);
	}

	/**
	 * @deprecated WeakReference<?> callerRef argument will be removed
	 */
	@Deprecated(since = "4.1.2")
	public GraphQueryResult sendGraphQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, WeakReference<?> callerRef, Binding... bindings)
			throws IOException, RepositoryException,
			MalformedQueryException, UnauthorizedException, QueryInterruptedException {
		try {
			HttpRequest method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime,
					bindings);
			return getRDFBackground(method, false, callerRef);
		} catch (RDFHandlerException e) {
			// Found a bug in TupleQueryResultBuilder?
			throw new RepositoryException(e);
		}
	}

	public void sendGraphQuery(QueryLanguage ql, String query, Dataset dataset, boolean includeInferred,
			RDFHandler handler, Binding... bindings) throws IOException, RDFHandlerException, RepositoryException,
			MalformedQueryException, UnauthorizedException, QueryInterruptedException {
		sendGraphQuery(ql, query, null, dataset, includeInferred, 0, handler, bindings);
	}

	public void sendGraphQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset, boolean includeInferred,
			int maxQueryTime, RDFHandler handler, Binding... bindings) throws IOException, RDFHandlerException,
			RepositoryException, MalformedQueryException, UnauthorizedException, QueryInterruptedException {
		HttpRequest method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
		getRDF(method, handler, false);
	}

	public boolean sendBooleanQuery(QueryLanguage ql, String query, Dataset dataset, boolean includeInferred,
			Binding... bindings) throws IOException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		return sendBooleanQuery(ql, query, null, dataset, includeInferred, 0, bindings);
	}

	public boolean sendBooleanQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings) throws IOException, RepositoryException,
			MalformedQueryException, UnauthorizedException, QueryInterruptedException {
		HttpRequest method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
		try {
			return getBoolean(method);
		} catch (RepositoryException | MalformedQueryException | QueryInterruptedException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
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
	 * unusual server configurations.
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

	protected HttpRequest getQueryMethod(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings) {
		List<NameValuePair> queryParams = getQueryMethodParameters(ql, query, baseURI, dataset, includeInferred,
				maxQueryTime, bindings);

		UriBuilder urib = UriBuilder.from(getQueryURL());
		for (NameValuePair nvp : queryParams) {
			urib.addParameter(nvp.getName(), nvp.getValue());
		}
		String queryUrlWithParams = urib.toString();

		HttpRequest.Builder builder;
		if (shouldUsePost(queryUrlWithParams)) {
			// we just built up a URL for nothing. oh well.
			// It's probably not much overhead against
			// the poor triplestore having to process such as massive query
			builder = HttpRequests.post(getQueryURL())
					.header("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8")
					.body(HttpRequestBody.ofFormData(queryParams));
		} else {
			builder = HttpRequests.get(queryUrlWithParams);
		}

		// functionality to provide custom http headers as required by the applications
		for (Map.Entry<String, String> additionalHeader : additionalHttpHeaders.entrySet()) {
			builder.header(additionalHeader.getKey(), additionalHeader.getValue());
		}
		return builder.build();
	}

	/**
	 * Return whether the provided query should use POST (otherwise use GET)
	 *
	 * @param fullQueryUrl the complete URL, including hostname and all HTTP query parameters
	 */
	protected boolean shouldUsePost(String fullQueryUrl) {
		return fullQueryUrl.length() > maximumUrlLength;
	}

	protected HttpRequest getUpdateMethod(QueryLanguage ql, String update, String baseURI, Dataset dataset,
			boolean includeInferred, Binding... bindings) {
		return getUpdateMethod(ql, update, baseURI, dataset, includeInferred, 0, bindings);
	}

	protected HttpRequest getUpdateMethod(QueryLanguage ql, String update, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings) {
		List<NameValuePair> queryParams = getUpdateMethodParameters(ql, update, baseURI, dataset, includeInferred,
				maxQueryTime, bindings);

		HttpRequest.Builder builder = HttpRequests.post(getUpdateURL())
				.header("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8")
				.body(HttpRequestBody.ofFormData(queryParams));

		if (this.additionalHttpHeaders != null) {
			for (Map.Entry<String, String> additionalHeader : additionalHttpHeaders.entrySet()) {
				builder.header(additionalHeader.getKey(), additionalHeader.getValue());
			}
		}
		return builder.build();
	}

	protected List<NameValuePair> getQueryMethodParameters(QueryLanguage ql, String query, String baseURI,
			Dataset dataset, boolean includeInferred, int maxQueryTime, Binding... bindings) {
		List<NameValuePair> queryParams = new ArrayList<>();

		/*
		 * Only query, default-graph-uri, and named-graph-uri are standard parameters in SPARQL Protocol 1.1.
		 */

		if (query != null) {
			if (baseURI != null && !baseURI.isEmpty()) {
				// prepend query string with base URI declaration
				query = "BASE <" + baseURI + "> \n" + query;
			}
			queryParams.add(NameValuePair.of(Protocol.QUERY_PARAM_NAME, query));
		}

		if (dataset != null) {
			for (IRI defaultGraphURI : dataset.getDefaultGraphs()) {
				queryParams.add(NameValuePair.of(Protocol.DEFAULT_GRAPH_PARAM_NAME, String.valueOf(defaultGraphURI)));
			}
			for (IRI namedGraphURI : dataset.getNamedGraphs()) {
				queryParams.add(NameValuePair.of(Protocol.NAMED_GRAPH_PARAM_NAME, String.valueOf(namedGraphURI)));
			}
		}

		return queryParams;
	}

	protected List<NameValuePair> getUpdateMethodParameters(QueryLanguage ql, String update, String baseURI,
			Dataset dataset, boolean includeInferred, Binding... bindings) {
		return getUpdateMethodParameters(ql, update, baseURI, dataset, includeInferred, 0, bindings);
	}

	protected List<NameValuePair> getUpdateMethodParameters(QueryLanguage ql, String update, String baseURI,
			Dataset dataset, boolean includeInferred, int maxQueryTime, Binding... bindings) {

		List<NameValuePair> queryParams = new ArrayList<>();

		if (update != null) {
			if (baseURI != null && !baseURI.isEmpty()) {
				// prepend update string with base URI declaration
				update = "BASE <" + baseURI + "> \n" + update;
			}
			queryParams.add(NameValuePair.of(Protocol.UPDATE_PARAM_NAME, update));
			logger.debug("added update string {}", update);
		}

		if (dataset != null) {
			if (!dataset.getDefaultRemoveGraphs().isEmpty()) {
				if (!(dataset.getDefaultRemoveGraphs().equals(dataset.getDefaultGraphs()))) {
					logger.warn(
							"ambiguous dataset spec for SPARQL endpoint: default graphs and default remove graphs both defined but not equal");
				}
				for (IRI graphURI : dataset.getDefaultRemoveGraphs()) {
					if (dataset.getDefaultInsertGraph() != null) {
						if (!dataset.getDefaultInsertGraph().equals(graphURI)) {
							logger.warn(
									"ambiguous dataset spec for SPARQL endpoint: default insert graph ({}) and default remove graph ({}) both defined but not equal. ",
									dataset.getDefaultInsertGraph(), graphURI);
						}
					}
					queryParams.add(NameValuePair.of(Protocol.USING_GRAPH_PARAM_NAME, String.valueOf(graphURI)));
				}
			}

			if (dataset.getDefaultInsertGraph() != null) {
				if (!dataset.getDefaultGraphs().isEmpty()) {
					if (!(dataset.getDefaultGraphs().size() == 1
							&& dataset.getDefaultGraphs().contains(dataset.getDefaultInsertGraph()))) {
						logger.warn(
								"ambiguous dataset spec for SPARQL endpoint: default insert graph ({}) and default graphs both defined but not equal. ",
								dataset.getDefaultInsertGraph());
					}
				}

				queryParams.add(NameValuePair.of(Protocol.USING_GRAPH_PARAM_NAME,
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

		return queryParams;
	}

	/*------------------*
	 * Response parsing *
	 *------------------*/

	/**
	 * Parse the response in a background thread. HTTP connections are dealt with in the {@link BackgroundTupleResult}
	 * or (in the error-case) in this method.
	 *
	 * @deprecated WeakReference<?> callerRef argument will be removed
	 */
	@Deprecated(since = "4.1.2")
	protected TupleQueryResult getBackgroundTupleQueryResult(HttpRequest method, WeakReference<?> callerRef)
			throws RepositoryException, QueryInterruptedException, MalformedQueryException, IOException {

		boolean submitted = false;

		// Specify which formats we support
		Set<QueryResultFormat> tqrFormats = TupleQueryResultParserRegistry.getInstance().getKeys();
		if (tqrFormats.isEmpty()) {
			throw new RepositoryException("No tuple query result parsers have been registered");
		}

		TupleQueryResult tRes;
		// send the tuple query
		HttpResponse response = sendTupleQueryViaHttp(method, tqrFormats);
		try {

			// if we get here, HTTP code is 200
			String mimeType = getResponseMIMEType(response);
			QueryResultFormat format = TupleQueryResultFormat.matchMIMEType(mimeType, tqrFormats)
					.orElseThrow(() -> new RepositoryException(
							"Server responded with an unsupported file format: " + mimeType));
			TupleQueryResultParser parser = QueryResultIO.createTupleParser(format, getValueFactory());
			tRes = background.parse(parser, responseClosingStream(response), callerRef);
			submitted = true;
			return tRes;
		} finally {
			if (!submitted) {
				response.discardAndClose();
			}
		}
	}

	/**
	 * Parse the response in this thread using the provided {@link TupleQueryResultHandler}. All HTTP connections are
	 * closed and released in this method
	 */
	protected void getTupleQueryResult(HttpRequest method, TupleQueryResultHandler handler)
			throws IOException, TupleQueryResultHandlerException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		// Specify which formats we support
		Set<QueryResultFormat> tqrFormats = TupleQueryResultParserRegistry.getInstance().getKeys();
		if (tqrFormats.isEmpty()) {
			throw new RepositoryException("No tuple query result parsers have been registered");
		}

		// send the tuple query
		try (HttpResponse response = sendTupleQueryViaHttp(method, tqrFormats)) {

			// if we get here, HTTP code is 200
			String mimeType = getResponseMIMEType(response);
			try {
				QueryResultFormat format = TupleQueryResultFormat.matchMIMEType(mimeType, tqrFormats)
						.orElseThrow(() -> new RepositoryException(
								"Server responded with an unsupported file format: " + mimeType));

				// Check if we can pass through to the writer directly
				if (handler instanceof Sink && passThrough(response, format, ((Sink) handler))) {
					return;
				}

				// we need to parse the result and re-serialize.
				TupleQueryResultParser parser = QueryResultIO.createTupleParser(format, getValueFactory());
				parser.setQueryResultHandler(handler);
				parser.parseQueryResult(response.getBodyAsStream());
			} catch (QueryResultParseException e) {
				throw new RepositoryException("Malformed query result from server", e);
			} catch (QueryResultHandlerException e) {
				if (e instanceof TupleQueryResultHandlerException) {
					throw (TupleQueryResultHandlerException) e;
				} else {
					throw new TupleQueryResultHandlerException(e);
				}
			}
		}
	}

	/**
	 * Send the tuple query via HTTP and throws an exception in case anything goes wrong, i.e. only for HTTP 200 the
	 * method returns without exception. If HTTP status code is not equal to 200, the request is aborted, however pooled
	 * connections are not released.
	 *
	 * @param method
	 * @throws RepositoryException
	 * @throws IOException
	 * @throws QueryInterruptedException
	 * @throws MalformedQueryException
	 */
	private HttpResponse sendTupleQueryViaHttp(HttpRequest method, Set<QueryResultFormat> tqrFormats)
			throws RepositoryException, IOException, QueryInterruptedException, MalformedQueryException {

		final List<String> acceptValues = new ArrayList<>(tqrFormats.size());
		for (QueryResultFormat format : tqrFormats) {

			// Determine a q-value that reflects the user specified preference
			int qValue = 10;

			if (preferredTQRFormat != null && !preferredTQRFormat.equals(format)) {
				// Prefer specified format over other formats
				qValue -= 2;
			}

			for (String mimeType : format.getMIMETypes()) {
				String acceptParam = mimeType;

				if (qValue < 10) {
					acceptParam += ";q=0." + qValue;
				}
				acceptValues.add(acceptParam);
			}
		}

		HttpRequest methodWithAccept = withHeader(method, ACCEPT_PARAM_NAME,
				String.join(", ", acceptValues));

		try {
			return executeOK(methodWithAccept);
		} catch (RepositoryException | MalformedQueryException | QueryInterruptedException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Parse the response in a background thread. HTTP connections are dealt with in the {@link BackgroundGraphResult}
	 * or (in the error-case) in this method.
	 *
	 * @deprecated WeakReference<?> callerRef argument will be removed
	 */
	@Deprecated(since = "4.1.2")
	protected GraphQueryResult getRDFBackground(HttpRequest method, boolean requireContext,
			WeakReference<?> callerRef)
			throws IOException, RDFHandlerException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {

		boolean submitted = false;

		// Specify which formats we support using Accept headers
		Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
		if (rdfFormats.isEmpty()) {
			throw new RepositoryException("No tuple RDF parsers have been registered");
		}

		GraphQueryResult gRes;
		// send the tuple query
		HttpResponse response = sendGraphQueryViaHttp(method, requireContext, rdfFormats);
		try {

			// if we get here, HTTP code is 200
			String mimeType = getResponseMIMEType(response);
			RDFFormat format = RDFFormat.matchMIMEType(mimeType, rdfFormats)
					.orElseThrow(() -> new RepositoryException(
							"Server responded with an unsupported file format: " + mimeType));
			RDFParser parser = Rio.createParser(format, getValueFactory());
			parser.setParserConfig(getParserConfig());
			parser.setParseErrorListener(new ParseErrorLogger());

			Charset charset = null;

			// SES-1793 : Do not attempt to check for a charset if the format is
			// defined not to have a charset
			// This prevents errors caused by people erroneously attaching a
			// charset to a binary formatted document
			if (format.hasCharset()) {
				charset = getResponseCharset(response).orElse(null);
				if (charset == null) {
					charset = UTF8;
				}
			}

			String baseURI = method.getUri().toASCIIString();
			gRes = background.parse(parser, responseClosingStream(response), charset, baseURI, callerRef);
			submitted = true;
			return gRes;
		} finally {
			if (!submitted) {
				response.discardAndClose();
			}
		}

	}

	/**
	 * Parse the response in this thread using the provided {@link RDFHandler}. All HTTP connections are closed and
	 * released in this method
	 */
	protected void getRDF(HttpRequest method, RDFHandler handler, boolean requireContext)
			throws IOException, RDFHandlerException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		// Specify which formats we support using Accept headers
		Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
		if (rdfFormats.isEmpty()) {
			throw new RepositoryException("No tuple RDF parsers have been registered");
		}

		// send the tuple query
		try (HttpResponse response = sendGraphQueryViaHttp(method, requireContext, rdfFormats)) {

			String mimeType = getResponseMIMEType(response);
			try {
				RDFFormat format = RDFFormat.matchMIMEType(mimeType, rdfFormats)
						.orElseThrow(() -> new RepositoryException(
								"Server responded with an unsupported file format: " + mimeType));

				// Check if we can pass through to the writer directly
				if (handler instanceof Sink && passThrough(response, format, ((Sink) handler))) {
					return;
				}

				// we need to parse the result and re-serialize.
				RDFParser parser = Rio.createParser(format, getValueFactory());
				parser.setParserConfig(getParserConfig());
				parser.setParseErrorListener(new ParseErrorLogger());
				parser.setRDFHandler(handler);
				parser.parse(response.getBodyAsStream(), method.getUri().toASCIIString());
			} catch (RDFParseException e) {
				throw new RepositoryException("Malformed query result from server", e);
			}
		}
	}

	/**
	 * Pass through response content directly to the supplied sink if possible.
	 *
	 * @param response       the {@link HttpResponse} with the content.
	 * @param responseFormat the format of the response.
	 * @param sink           the {@link Sink} to pass the content through to.
	 * @return {@code true} if the content was passed through, {@code false} otherwise.
	 * @throws IOException
	 */
	private boolean passThrough(HttpResponse response, FileFormat responseFormat, Sink sink)
			throws IOException {
		if (!isPassThroughEnabled()) {
			return false;
		}
		if (sink.acceptsFileFormat(responseFormat)) {
			InputStream in = response.getBodyAsStream();
			if (sink instanceof CharSink) {
				Writer out = ((CharSink) sink).getWriter();
				IOUtils.copy(in, out,
						getResponseCharset(response).orElse(responseFormat.getCharset()));
				out.flush();
				return true;
			} else if (sink instanceof ByteSink) {
				OutputStream out = ((ByteSink) sink).getOutputStream();
				IOUtils.copy(in, out);
				out.flush();
				return true;
			}

		}
		return false;
	}

	private HttpResponse sendGraphQueryViaHttp(HttpRequest method, boolean requireContext,
			Set<RDFFormat> rdfFormats)
			throws RepositoryException, IOException, QueryInterruptedException, MalformedQueryException {

		List<String> acceptParams = RDFFormat.getAcceptParams(rdfFormats, requireContext, getPreferredRDFFormat());

		HttpRequest methodWithAccept = withHeader(method, ACCEPT_PARAM_NAME,
				String.join(", ", acceptParams));

		try {
			return executeOK(methodWithAccept);
		} catch (RepositoryException | MalformedQueryException | QueryInterruptedException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Parse the response in this thread using a suitable {@link BooleanQueryResultParser}. All HTTP connections are
	 * closed and released in this method
	 *
	 * @throws RDF4JException
	 */
	protected boolean getBoolean(HttpRequest method) throws IOException, RDF4JException {
		// Specify which formats we support using Accept headers
		Set<QueryResultFormat> booleanFormats = BooleanQueryResultParserRegistry.getInstance().getKeys();
		if (booleanFormats.isEmpty()) {
			throw new RepositoryException("No boolean query result parsers have been registered");
		}

		// send the tuple query
		try (HttpResponse response = sendBooleanQueryViaHttp(method, booleanFormats)) {

			// if we get here, HTTP code is 200
			String mimeType = getResponseMIMEType(response);
			try {
				QueryResultFormat format = BooleanQueryResultFormat.matchMIMEType(mimeType, booleanFormats)
						.orElseThrow(() -> new RepositoryException(
								"Server responded with an unsupported file format: " + mimeType));
				BooleanQueryResultParser parser = QueryResultIO.createBooleanParser(format);
				QueryResultCollector results = new QueryResultCollector();
				parser.setQueryResultHandler(results);
				parser.parseQueryResult(response.getBodyAsStream());
				return results.getBoolean();
			} catch (QueryResultParseException e) {
				throw new RepositoryException("Malformed query result from server", e);
			}
		}

	}

	private HttpResponse sendBooleanQueryViaHttp(HttpRequest method, Set<QueryResultFormat> booleanFormats)
			throws IOException, RDF4JException {

		final List<String> acceptValues = new ArrayList<>(booleanFormats.size());

		for (QueryResultFormat format : booleanFormats) {
			// Determine a q-value that reflects the user specified preference
			int qValue = 10;

			if (preferredBQRFormat != null && !preferredBQRFormat.equals(format)) {
				// Prefer specified format over other formats
				qValue -= 2;
			}

			for (String mimeType : format.getMIMETypes()) {
				String acceptParam = mimeType;

				if (qValue < 10) {
					acceptParam += ";q=0." + qValue;
				}

				acceptValues.add(acceptParam);
			}
		}

		HttpRequest methodWithAccept = withHeader(method, ACCEPT_PARAM_NAME,
				String.join(", ", acceptValues));

		return executeOK(methodWithAccept);
	}

	/**
	 * Convenience method to deal with HTTP level errors of tuple, graph and boolean queries in the same way. This
	 * method aborts the HTTP connection.
	 *
	 * @param method
	 * @throws RDF4JException
	 */
	protected HttpResponse executeOK(HttpRequest method) throws IOException, RDF4JException {
		boolean fail = true;
		HttpResponse response = execute(method);

		try {
			int httpCode = response.getStatusCode();
			if (httpCode == HttpURLConnection.HTTP_OK || httpCode == HttpURLConnection.HTTP_NOT_AUTHORITATIVE) {
				fail = false;
				return response; // everything OK, control flow can continue
			} else {
				// trying to contact a non-SPARQL server?
				throw new RepositoryException("Request failed with status " + httpCode + ": "
						+ method.getUri().toString());
			}
		} finally {
			if (fail) {
				response.discardAndClose();
			}
		}
	}

	protected void executeNoContent(HttpRequest method) throws IOException, RDF4JException {
		try (HttpResponse response = execute(method)) {
			if (response.getStatusCode() >= 300) {
				throw new RepositoryException("Failed to get server protocol; no such resource on this server: "
						+ method.getUri().toString());
			}
		}
	}

	/**
	 * Executes the given HTTP request and returns the response on success (2xx or 404).
	 * <p>
	 * The caller is responsible for closing the returned {@link HttpResponse}. On error (non-2xx other than 404) the
	 * response is consumed and closed internally and an appropriate exception is thrown.
	 *
	 * @param request the request to execute
	 * @return the response; the caller must close it after use
	 * @throws IOException    if a network or I/O error occurs
	 * @throws RDF4JException if the server reports a protocol-level error
	 */
	protected HttpResponse execute(HttpRequest request) throws IOException, RDF4JException {

		if (authenticationHandler != null) {
			authenticationHandler.authenticate(request);
		}
		HttpResponse response = httpClient.execute(request);
		int httpCode = response.getStatusCode();
		if (httpCode >= 200 && httpCode < 300 || httpCode == HttpURLConnection.HTTP_NOT_FOUND) {
			return response;
		}
		boolean consumed = false;
		try {
			switch (httpCode) {
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				throw new UnauthorizedException();
			case HttpURLConnection.HTTP_UNAVAILABLE:
				throw new QueryInterruptedException();
			default:
				ErrorInfo errInfo = getErrorInfo(response);
				consumed = true;
				if (errInfo.getErrorType() == ErrorType.MALFORMED_DATA) {
					throw new RDFParseException(errInfo.getErrorMessage());
				} else if (errInfo.getErrorType() == ErrorType.UNSUPPORTED_FILE_FORMAT) {
					throw new UnsupportedRDFormatException(errInfo.getErrorMessage());
				} else if (errInfo.getErrorType() == ErrorType.MALFORMED_QUERY) {
					throw new MalformedQueryException(errInfo.getErrorMessage());
				} else if (errInfo.getErrorType() == ErrorType.UNSUPPORTED_QUERY_LANGUAGE) {
					throw new UnsupportedQueryLanguageException(errInfo.getErrorMessage());
				} else if (contentTypeIs(response, "application/shacl-validation-report")) {
					RDFFormat format = getContentTypeSerialisation(response);
					throw new RepositoryException(new RemoteShaclValidationException(
							new StringReader(errInfo.toString()), "", format));
				} else if (!errInfo.toString().isEmpty()) {
					throw new RepositoryException(errInfo.toString());
				} else {
					throw new RepositoryException(response.getReasonPhrase());
				}
			}
		} finally {
			if (!consumed) {
				response.discardQuietly();
			}
			response.close();
		}
	}

	static RDFFormat getContentTypeSerialisation(HttpResponse response) {
		Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
		if (rdfFormats.isEmpty()) {
			throw new RepositoryException("No tuple RDF parsers have been registered");
		}

		for (HttpHeader header : response.getHeaders("Content-Type")) {
			String headerValue = header.getValue();
			// get the mime type part (before ';')
			String mimeType = headerValue.split(";")[0].trim();
			// SHACL Validation report Content-Type gets transformed from:
			// application/shacl-validation-report+n-quads => application/n-quads
			// application/shacl-validation-report+ld+json => application/ld+json
			// text/shacl-validation-report+turtle => text/turtle

			String[] split = mimeType.split("\\+");
			StringBuilder serialisation = new StringBuilder(mimeType.split("/")[0] + "/");
			for (int i = 1; i < split.length; i++) {
				serialisation.append(split[i]);
				if (i + 1 < split.length) {
					serialisation.append("+");
				}
			}
			logger.debug("SHACL validation report is serialised as: " + serialisation);

			Optional<RDFFormat> rdfFormat = RDFFormat.matchMIMEType(serialisation.toString(), rdfFormats);

			if (rdfFormat.isPresent()) {
				return rdfFormat.get();
			}
		}

		throw new RepositoryException("Unsupported content-type for SHACL Validation Report: "
				+ response.getHeaders("Content-Type")
				+ "! If the format seems correct, then you may need a maven dependency for that.");

	}

	/**
	 * Returns an {@link InputStream} over the response body that also closes the {@link HttpResponse} when the stream
	 * is closed. This is used when the response body is consumed by a background thread: the background thread closes
	 * the stream when parsing completes, which in turn releases the underlying HTTP connection.
	 */
	private static InputStream responseClosingStream(HttpResponse response) throws IOException {
		return new FilterInputStream(response.getBodyAsStream()) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					response.close();
				}
			}
		};
	}

	private static boolean contentTypeIs(HttpResponse response, String contentType) {
		List<HttpHeader> headers = response.getHeaders("Content-Type");
		if (headers.isEmpty()) {
			return false;
		}
		for (HttpHeader header : headers) {
			String mimeType = header.getValue().split(";")[0].trim();
			String name = mimeType.split("\\+")[0];
			if (contentType.equals(name)) {
				return true;
			}
		}
		return false;
	}

	/*-------------------------*
	 * General utility methods *
	 *-------------------------*/

	/**
	 * Gets the MIME type specified in the response headers of the supplied method, if any. For example, if the response
	 * headers contain <var>Content-Type: application/xml;charset=UTF-8</var>, this method will return
	 * <var>application/xml</var> as the MIME type.
	 *
	 * @param response The response to get the MIME type from.
	 * @return The response MIME type, or <var>null</var> if not available.
	 */
	protected String getResponseMIMEType(HttpResponse response) throws IOException {
		List<HttpHeader> headers = response.getHeaders("Content-Type");
		for (HttpHeader header : headers) {
			String value = header.getValue().split(";")[0].trim();
			if (!value.isEmpty()) {
				logger.debug("response MIME type is {}", value);
				return value;
			}
		}
		return null;
	}

	/**
	 * Gets the character encoding specified in the HTTP headers of the supplied response, if any. For example, if the
	 * response headers contain <var>Content-Type: application/xml;charset=UTF-8</var>, this method will return
	 * {@link StandardCharsets#UTF_8 UTF-8} as the character encoding.
	 *
	 * @param response the response to get the character encoding from.
	 * @return the response character encoding, {@link Optional#empty()} if it can not be determined.
	 */
	Optional<Charset> getResponseCharset(HttpResponse response) {
		List<HttpHeader> headers = response.getHeaders("Content-Type");
		for (HttpHeader header : headers) {
			for (String part : header.getValue().split(";")) {
				String trimmed = part.trim();
				if (trimmed.regionMatches(true, 0, "charset=", 0, "charset=".length())) {
					String charsetName = trimmed.substring("charset=".length()).trim();
					// Strip optional surrounding quotes (e.g. charset="UTF-8")
					if (charsetName.length() >= 2
							&& charsetName.charAt(0) == '"'
							&& charsetName.charAt(charsetName.length() - 1) == '"') {
						charsetName = charsetName.substring(1, charsetName.length() - 1);
					}
					try {
						Charset charset = Charset.forName(charsetName);
						logger.debug("response charset is {}", charset);
						return Optional.of(charset);
					} catch (IllegalArgumentException e) {
						// continue
					}
				}
			}
		}
		return Optional.empty();
	}

	protected ErrorInfo getErrorInfo(HttpResponse response) throws RepositoryException {
		try {
			String body = HttpUtils.toString(response);
			ErrorInfo errInfo = ErrorInfo.parse(body);
			logger.warn("Server reports problem: {} (enable debug logging for full details)", errInfo.getErrorType());
			logger.debug("full error message: {}", errInfo.getErrorMessage());
			return errInfo;
		} catch (IOException e) {
			logger.warn("Unable to retrieve error info from server");
			throw new RepositoryException("Unable to retrieve error info from server", e);
		}
	}

	/**
	 * Sets the parser configuration used to process HTTP response data.
	 *
	 * @param parserConfig The parserConfig to set.
	 */
	public void setParserConfig(ParserConfig parserConfig) {
		this.parserConfig = parserConfig;
	}

	/**
	 * @return Returns the parser configuration used to process HTTP response data.
	 */
	public ParserConfig getParserConfig() {
		return parserConfig;
	}

	/**
	 * Indicates if direct pass-through of the endpoint result to the supplied {@link Sink} is enabled.
	 *
	 * @return the passThroughEnabled setting.
	 */
	public boolean isPassThroughEnabled() {
		return passThroughEnabled;
	}

	/**
	 * Configure direct pass-through of the endpoint result to the supplied {@link Sink}.
	 * <p>
	 * If not explicitly configured, the setting defaults to {@code true}.
	 *
	 * @param passThroughEnabled the passThroughEnabled to set.
	 */
	public void setPassThroughEnabled(boolean passThroughEnabled) {
		this.passThroughEnabled = passThroughEnabled;
	}

	/**
	 * Creates a new {@link HttpRequest} with an additional header added to the existing request.
	 *
	 * @param request the original request
	 * @param name    the header name
	 * @param value   the header value
	 * @return a new request with the additional header
	 */
	private static HttpRequest withHeader(HttpRequest request, String name, String value) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(request.getMethod(), request.getUri())
				.headers(request.getHeaders())
				.header(name, value);
		request.getBody().ifPresent(builder::body);
		return builder.build();
	}
}
