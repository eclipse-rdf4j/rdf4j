/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import static org.eclipse.rdf4j.http.protocol.Protocol.ACCEPT_PARAM_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.common.exception.RDF4JConfigException;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.io.ByteSink;
import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.common.io.Sink;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.http.client.shacl.RemoteShaclValidationException;
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

	private HttpClient httpClient;

	private final BackgroundResultExecutor background;

	private final HttpClientContext httpContext;

	private HttpParams params;

	private ParserConfig parserConfig = new ParserConfig();

	private TupleQueryResultFormat preferredTQRFormat = TupleQueryResultFormat.SPARQL;

	private BooleanQueryResultFormat preferredBQRFormat = BooleanQueryResultFormat.TEXT;

	private RDFFormat preferredRDFFormat = RDFFormat.TURTLE;

	private Map<String, String> additionalHttpHeaders = Collections.emptyMap();

	private boolean passThroughEnabled = true;

	public SPARQLProtocolSession(HttpClient client, ExecutorService executor) {
		this.httpClient = client;
		this.httpContext = new HttpClientContext();
		this.background = new BackgroundResultExecutor(executor);
		valueFactory = SimpleValueFactory.getInstance();
		httpContext.setCookieStore(new BasicCookieStore());

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
	public final HttpClient getHttpClient() {
		return httpClient;
	}

	@Override
	public void setHttpClient(HttpClient httpClient) {
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
			java.net.URI requestURI = java.net.URI.create(url);
			String host = requestURI.getHost();
			int port = requestURI.getPort();
			AuthScope scope = new AuthScope(host, port);
			UsernamePasswordCredentials cred = new UsernamePasswordCredentials(username, password);
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(scope, cred);
			httpContext.setCredentialsProvider(credsProvider);
			AuthCache authCache = new BasicAuthCache();
			BasicScheme basicAuth = new BasicScheme();
			HttpHost httpHost = new HttpHost(requestURI.getHost(), requestURI.getPort(), requestURI.getScheme());
			authCache.put(httpHost, basicAuth);
			httpContext.setAuthCache(authCache);
		} else {
			httpContext.removeAttribute(HttpClientContext.AUTH_CACHE);
			httpContext.removeAttribute(HttpClientContext.CREDS_PROVIDER);
		}
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

	public TupleQueryResult sendTupleQuery(QueryLanguage ql, String query, Dataset dataset, boolean includeInferred,
			WeakReference<?> callerRef,
			Binding... bindings) throws IOException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		return sendTupleQuery(ql, query, null, dataset, includeInferred, 0, callerRef, bindings);
	}

	public TupleQueryResult sendTupleQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, WeakReference<?> callerRef, Binding... bindings)
			throws IOException, RepositoryException,
			MalformedQueryException, UnauthorizedException, QueryInterruptedException {
		HttpUriRequest method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
		return getBackgroundTupleQueryResult(method, callerRef);
	}

	public void sendTupleQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset, boolean includeInferred,
			int maxQueryTime, TupleQueryResultHandler handler, Binding... bindings)
			throws IOException, TupleQueryResultHandlerException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		HttpUriRequest method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
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
		HttpUriRequest method = getUpdateMethod(ql, update, baseURI, dataset, includeInferred, maxQueryTime, bindings);

		try {
			executeNoContent(method);
		} catch (RepositoryException | MalformedQueryException | QueryInterruptedException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		}
	}

	public GraphQueryResult sendGraphQuery(QueryLanguage ql, String query, Dataset dataset, boolean includeInferred,
			WeakReference<?> callerRef,
			Binding... bindings) throws IOException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		return sendGraphQuery(ql, query, null, dataset, includeInferred, 0, callerRef, bindings);
	}

	public GraphQueryResult sendGraphQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, WeakReference<?> callerRef, Binding... bindings)
			throws IOException, RepositoryException,
			MalformedQueryException, UnauthorizedException, QueryInterruptedException {
		try {
			HttpUriRequest method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime,
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
		HttpUriRequest method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
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
		HttpUriRequest method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
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

	protected HttpUriRequest getQueryMethod(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings) {
		List<NameValuePair> queryParams = getQueryMethodParameters(ql, query, baseURI, dataset, includeInferred,
				maxQueryTime, bindings);
		HttpUriRequest method;
		String queryUrlWithParams;
		try {
			URIBuilder urib = new URIBuilder(getQueryURL());
			for (NameValuePair nvp : queryParams) {
				urib.addParameter(nvp.getName(), nvp.getValue());
			}
			queryUrlWithParams = urib.toString();
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
		if (shouldUsePost(queryUrlWithParams)) {
			// we just built up a URL for nothing. oh well.
			// It's probably not much overhead against
			// the poor triplestore having to process such as massive query
			HttpPost postMethod = new HttpPost(getQueryURL());
			postMethod.setHeader("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8");
			postMethod.setEntity(new UrlEncodedFormEntity(queryParams, UTF8));
			method = postMethod;
		} else {
			method = new HttpGet(queryUrlWithParams);
		}
		// functionality to provide custom http headers as required by the
		// applications
		for (Map.Entry<String, String> additionalHeader : additionalHttpHeaders.entrySet()) {
			method.addHeader(additionalHeader.getKey(), additionalHeader.getValue());
		}
		return method;
	}

	/**
	 * Return whether the provided query should use POST (otherwise use GET)
	 *
	 * @param fullQueryUrl the complete URL, including hostname and all HTTP query parameters
	 */
	protected boolean shouldUsePost(String fullQueryUrl) {
		return fullQueryUrl.length() > maximumUrlLength;
	}

	protected HttpUriRequest getUpdateMethod(QueryLanguage ql, String update, String baseURI, Dataset dataset,
			boolean includeInferred, Binding... bindings) {
		return getUpdateMethod(ql, update, baseURI, dataset, includeInferred, 0, bindings);
	}

	protected HttpUriRequest getUpdateMethod(QueryLanguage ql, String update, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings) {
		HttpPost method = new HttpPost(getUpdateURL());

		method.setHeader("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8");

		List<NameValuePair> queryParams = getUpdateMethodParameters(ql, update, baseURI, dataset, includeInferred,
				maxQueryTime, bindings);

		method.setEntity(new UrlEncodedFormEntity(queryParams, UTF8));

		if (this.additionalHttpHeaders != null) {
			for (Map.Entry<String, String> additionalHeader : additionalHttpHeaders.entrySet()) {
				method.addHeader(additionalHeader.getKey(), additionalHeader.getValue());
			}
		}

		return method;
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
			queryParams.add(new BasicNameValuePair(Protocol.QUERY_PARAM_NAME, query));
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
			queryParams.add(new BasicNameValuePair(Protocol.UPDATE_PARAM_NAME, update));
			logger.debug("added update string {}", update);
		}

		if (dataset != null) {
			if (dataset.getDefaultRemoveGraphs().size() > 0) {
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
					queryParams.add(new BasicNameValuePair(Protocol.USING_GRAPH_PARAM_NAME, String.valueOf(graphURI)));
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

				queryParams.add(new BasicNameValuePair(Protocol.USING_GRAPH_PARAM_NAME,
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

		return queryParams;
	}

	/*------------------*
	 * Response parsing *
	 *------------------*/

	/**
	 * Parse the response in a background thread. HTTP connections are dealt with in the {@link BackgroundTupleResult}
	 * or (in the error-case) in this method.
	 */
	protected TupleQueryResult getBackgroundTupleQueryResult(HttpUriRequest method, WeakReference<?> callerRef)
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
			tRes = background.parse(parser, response.getEntity().getContent(), callerRef);
			submitted = true;
			return tRes;
		} finally {
			if (!submitted) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}
	}

	/**
	 * Parse the response in this thread using the provided {@link TupleQueryResultHandler}. All HTTP connections are
	 * closed and released in this method
	 */
	protected void getTupleQueryResult(HttpUriRequest method, TupleQueryResultHandler handler)
			throws IOException, TupleQueryResultHandlerException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		// Specify which formats we support
		Set<QueryResultFormat> tqrFormats = TupleQueryResultParserRegistry.getInstance().getKeys();
		if (tqrFormats.isEmpty()) {
			throw new RepositoryException("No tuple query result parsers have been registered");
		}

		// send the tuple query
		HttpResponse response = sendTupleQueryViaHttp(method, tqrFormats);
		try {

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
				parser.parseQueryResult(response.getEntity().getContent());
			} catch (QueryResultParseException e) {
				throw new RepositoryException("Malformed query result from server", e);
			} catch (QueryResultHandlerException e) {
				if (e instanceof TupleQueryResultHandlerException) {
					throw (TupleQueryResultHandlerException) e;
				} else {
					throw new TupleQueryResultHandlerException(e);
				}
			}
		} finally {
			EntityUtils.consumeQuietly(response.getEntity());
		}
	}

	/**
	 * Send the tuple query via HTTP and throws an exception in case anything goes wrong, i.e. only for HTTP 200 the
	 * method returns without exception. If HTTP status code is not equal to 200, the request is aborted, however pooled
	 * connections are not released.
	 *
	 * @param method
	 * @throws RepositoryException
	 * @throws HttpException
	 * @throws IOException
	 * @throws QueryInterruptedException
	 * @throws MalformedQueryException
	 */
	private HttpResponse sendTupleQueryViaHttp(HttpUriRequest method, Set<QueryResultFormat> tqrFormats)
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

		method.addHeader(ACCEPT_PARAM_NAME, String.join(", ", acceptValues));

		try {
			return executeOK(method);
		} catch (RepositoryException | MalformedQueryException | QueryInterruptedException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Parse the response in a background thread. HTTP connections are dealt with in the {@link BackgroundGraphResult}
	 * or (in the error-case) in this method.
	 */
	protected GraphQueryResult getRDFBackground(HttpUriRequest method, boolean requireContext,
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
			HttpEntity entity = response.getEntity();
			if (format.hasCharset() && entity != null && entity.getContentType() != null) {
				// TODO copied from SPARQLGraphQuery repository, is this
				// required?
				try {
					charset = ContentType.parse(entity.getContentType().getValue()).getCharset();
				} catch (IllegalCharsetNameException e) {
					// work around for Joseki-3.2
					// Content-Type: application/rdf+xml;
					// charset=application/rdf+xml
				}
				if (charset == null) {
					charset = UTF8;
				}
			}

			if (entity == null) {
				throw new RepositoryException("Server response was empty.");
			}

			String baseURI = method.getURI().toASCIIString();
			gRes = background.parse(parser, entity.getContent(), charset, baseURI, callerRef);
			submitted = true;
			return gRes;
		} finally {
			if (!submitted) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}

	}

	/**
	 * Parse the response in this thread using the provided {@link RDFHandler}. All HTTP connections are closed and
	 * released in this method
	 */
	protected void getRDF(HttpUriRequest method, RDFHandler handler, boolean requireContext)
			throws IOException, RDFHandlerException, RepositoryException, MalformedQueryException,
			UnauthorizedException, QueryInterruptedException {
		// Specify which formats we support using Accept headers
		Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
		if (rdfFormats.isEmpty()) {
			throw new RepositoryException("No tuple RDF parsers have been registered");
		}

		// send the tuple query
		HttpResponse response = sendGraphQueryViaHttp(method, requireContext, rdfFormats);
		try {

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
				parser.parse(response.getEntity().getContent(), method.getURI().toASCIIString());
			} catch (RDFParseException e) {
				throw new RepositoryException("Malformed query result from server", e);
			}
		} finally {
			EntityUtils.consumeQuietly(response.getEntity());
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
			InputStream in = response.getEntity().getContent();
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

	private HttpResponse sendGraphQueryViaHttp(HttpUriRequest method, boolean requireContext, Set<RDFFormat> rdfFormats)
			throws RepositoryException, IOException, QueryInterruptedException, MalformedQueryException {

		List<String> acceptParams = RDFFormat.getAcceptParams(rdfFormats, requireContext, getPreferredRDFFormat());

		method.addHeader(ACCEPT_PARAM_NAME, String.join(", ", acceptParams));

		try {
			return executeOK(method);
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
	protected boolean getBoolean(HttpUriRequest method) throws IOException, RDF4JException {
		// Specify which formats we support using Accept headers
		Set<QueryResultFormat> booleanFormats = BooleanQueryResultParserRegistry.getInstance().getKeys();
		if (booleanFormats.isEmpty()) {
			throw new RepositoryException("No boolean query result parsers have been registered");
		}

		// send the tuple query
		HttpResponse response = sendBooleanQueryViaHttp(method, booleanFormats);
		try {

			// if we get here, HTTP code is 200
			String mimeType = getResponseMIMEType(response);
			try {
				QueryResultFormat format = BooleanQueryResultFormat.matchMIMEType(mimeType, booleanFormats)
						.orElseThrow(() -> new RepositoryException(
								"Server responded with an unsupported file format: " + mimeType));
				BooleanQueryResultParser parser = QueryResultIO.createBooleanParser(format);
				QueryResultCollector results = new QueryResultCollector();
				parser.setQueryResultHandler(results);
				parser.parseQueryResult(response.getEntity().getContent());
				return results.getBoolean();
			} catch (QueryResultParseException e) {
				throw new RepositoryException("Malformed query result from server", e);
			}
		} finally {
			EntityUtils.consumeQuietly(response.getEntity());
		}

	}

	private HttpResponse sendBooleanQueryViaHttp(HttpUriRequest method, Set<QueryResultFormat> booleanFormats)
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

		method.addHeader(ACCEPT_PARAM_NAME, String.join(", ", acceptValues));

		return executeOK(method);
	}

	/**
	 * Convenience method to deal with HTTP level errors of tuple, graph and boolean queries in the same way. This
	 * method aborts the HTTP connection.
	 *
	 * @param method
	 * @throws RDF4JException
	 */
	protected HttpResponse executeOK(HttpUriRequest method) throws IOException, RDF4JException {
		boolean fail = true;
		HttpResponse response = execute(method);

		try {
			int httpCode = response.getStatusLine().getStatusCode();
			if (httpCode == HttpURLConnection.HTTP_OK || httpCode == HttpURLConnection.HTTP_NOT_AUTHORITATIVE) {
				fail = false;
				return response; // everything OK, control flow can continue
			} else {
				// trying to contact a non-SPARQL server?
				throw new RepositoryException("Request failed with status " + httpCode + ": "
						+ method.getURI().toString());
			}
		} finally {
			if (fail) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}
	}

	protected void executeNoContent(HttpUriRequest method) throws IOException, RDF4JException {
		HttpResponse response = execute(method);
		try {
			if (response.getStatusLine().getStatusCode() >= 300) {
				throw new RepositoryException("Failed to get server protocol; no such resource on this server: "
						+ method.getURI().toString());
			}
		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}

	protected HttpResponse execute(HttpUriRequest method) throws IOException, RDF4JException {
		boolean consume = true;
		if (params != null) {
			method.setParams(params);
		}
		HttpResponse response = httpClient.execute(method, httpContext);

		try {
			int httpCode = response.getStatusLine().getStatusCode();
			if (httpCode >= 200 && httpCode < 300 || httpCode == HttpURLConnection.HTTP_NOT_FOUND) {
				consume = false;
				return response; // everything OK, control flow can continue
			} else {
				switch (httpCode) {
				case HttpURLConnection.HTTP_UNAUTHORIZED: // 401
					throw new UnauthorizedException();
				case HttpURLConnection.HTTP_UNAVAILABLE: // 503
					throw new QueryInterruptedException();
				default:
					ErrorInfo errInfo = getErrorInfo(response);
					// Throw appropriate exception
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

					} else if (errInfo.toString().length() > 0) {
						throw new RepositoryException(errInfo.toString());
					} else {
						throw new RepositoryException(response.getStatusLine().getReasonPhrase());
					}
				}
			}
		} finally {
			if (consume) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}
	}

	static RDFFormat getContentTypeSerialisation(HttpResponse response) {
		Header[] headers = response.getHeaders("Content-Type");

		Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
		if (rdfFormats.isEmpty()) {
			throw new RepositoryException("No tuple RDF parsers have been registered");
		}

		for (Header header : headers) {
			for (HeaderElement element : header.getElements()) {
				// SHACL Validation report Content-Type gets transformed from:
				// application/shacl-validation-report+n-quads => application/n-quads
				// application/shacl-validation-report+ld+json => application/ld+json
				// text/shacl-validation-report+turtle => text/turtle

				String[] split = element.getName().split("\\+");
				StringBuilder serialisation = new StringBuilder(element.getName().split("/")[0] + "/");
				for (int i = 1; i < split.length; i++) {
					serialisation.append(split[i]);
					if (i + 1 < split.length) {
						serialisation.append("+");
					}
				}

				logger.debug("SHACL validation report is serialised as: " + serialisation.toString());

				Optional<RDFFormat> rdfFormat = RDFFormat.matchMIMEType(serialisation.toString(), rdfFormats);

				if (rdfFormat.isPresent()) {
					return rdfFormat.get();
				}
			}
		}

		throw new RepositoryException("Unsupported content-type for SHACL Validation Report: "
				+ Arrays.toString(response.getHeaders("Content-Type"))
				+ "! If the format seems correct, then you may need a maven dependency for that.");

	}

	private static boolean contentTypeIs(HttpResponse response, String contentType) {
		Header[] headers = response.getHeaders("Content-Type");
		if (headers.length == 0) {
			return false;
		}

		for (Header header : headers) {
			for (HeaderElement element : header.getElements()) {
				String name = element.getName().split("\\+")[0];
				if (contentType.equals(name)) {
					return true;
				}
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
	 * @param method The method to get the reponse MIME type from.
	 * @return The response MIME type, or <var>null</var> if not available.
	 */
	protected String getResponseMIMEType(HttpResponse method) throws IOException {
		Header[] headers = method.getHeaders("Content-Type");

		for (Header header : headers) {
			HeaderElement[] headerElements = header.getElements();

			for (HeaderElement headerEl : headerElements) {
				String mimeType = headerEl.getName();
				if (mimeType != null) {
					logger.debug("response MIME type is {}", mimeType);
					return mimeType;
				}
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
		Header[] headers = response.getHeaders("Content-Type");
		for (Header header : headers) {
			HeaderElement[] headerElements = header.getElements();

			for (HeaderElement element : headerElements) {
				NameValuePair charsetParam = element.getParameterByName("charset");
				if (charsetParam != null) {
					try {
						Charset charset = Charset.forName(charsetParam.getValue());
						logger.debug("response charset is {}", charset);
						return Optional.ofNullable(charset);
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
			ErrorInfo errInfo = ErrorInfo.parse(EntityUtils.toString(response.getEntity()));
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
	 * Gets the http connection read timeout in milliseconds.
	 */
	public long getConnectionTimeout() {
		if (params == null) {
			return 0;
		}
		return params.getIntParameter(CoreConnectionPNames.SO_TIMEOUT, 0);
	}

	/**
	 * Sets the http connection read timeout.
	 *
	 * @param timeout timeout in milliseconds. Zero sets to infinity.
	 */
	public void setConnectionTimeout(long timeout) {
		if (params == null) {
			params = new BasicHttpParams();
		}
		params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, (int) timeout);
	}

	/**
	 * Get the {@link HttpContext} used for sending HTTP requests.
	 *
	 * @return the {@link HttpContext} instance used for all protocol session requests.
	 */
	protected HttpContext getHttpContext() {
		return this.httpContext;
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
}
