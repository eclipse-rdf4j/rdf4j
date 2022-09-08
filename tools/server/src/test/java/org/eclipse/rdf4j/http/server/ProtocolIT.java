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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.opencsv.CSVReader;

public class ProtocolIT {

	private static TestServer server;

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	@BeforeClass
	public static void startServer() throws Exception {
		server = new TestServer();
		try {
			server.start();
		} catch (Exception e) {
			server.stop();
			throw e;
		}
	}

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop();
	}

	/**
	 * Tests the server's methods for updating all data in a repository.
	 */
	@Test
	public void testRepository_PUT() throws Exception {
		putFile(Protocol.getStatementsLocation(TestServer.REPOSITORY_URL), "/testcases/default-graph-1.ttl");
	}

	/**
	 * Tests the server's methods for deleting all data in a repository.
	 */
	@Test
	public void testRepository_DELETE() throws Exception {
		delete(Protocol.getStatementsLocation(TestServer.REPOSITORY_URL));
	}

	/**
	 * Tests the server's methods for updating the data in the default context of a repository.
	 */
	@Test
	public void testNullContext_PUT() throws Exception {
		String location = Protocol.getStatementsLocation(TestServer.REPOSITORY_URL);
		location += "?" + Protocol.CONTEXT_PARAM_NAME + "=" + Protocol.NULL_PARAM_VALUE;
		putFile(location, "/testcases/default-graph-1.ttl");
	}

	/**
	 * Tests the server's methods for deleting the data from the default context of a repository.
	 */
	@Test
	public void testNullContext_DELETE() throws Exception {
		String location = Protocol.getStatementsLocation(TestServer.REPOSITORY_URL);
		location += "?" + Protocol.CONTEXT_PARAM_NAME + "=" + Protocol.NULL_PARAM_VALUE;
		delete(location);
	}

	/**
	 * Tests the server's methods for updating the data in a named context of a repository.
	 */
	@Test
	public void testNamedContext_PUT() throws Exception {
		String location = Protocol.getStatementsLocation(TestServer.REPOSITORY_URL);
		String encContext = Protocol.encodeValue(vf.createIRI("urn:x-local:graph1"));
		location += "?" + Protocol.CONTEXT_PARAM_NAME + "=" + encContext;
		putFile(location, "/testcases/named-graph-1.ttl");
	}

	/**
	 * Tests the server's methods for deleting the data from a named context of a repository.
	 */
	@Test
	public void testNamedContext_DELETE() throws Exception {
		String location = Protocol.getStatementsLocation(TestServer.REPOSITORY_URL);
		String encContext = Protocol.encodeValue(vf.createIRI("urn:x-local:graph1"));
		location += "?" + Protocol.CONTEXT_PARAM_NAME + "=" + encContext;
		delete(location);
	}

	/**
	 * Tests the server's methods for quering a repository using GET requests to send SPARQL-select queries.
	 */
	@Test
	public void testSPARQLselect() throws Exception {
		try (TupleQueryResult queryResult = evaluateTupleQuery(TestServer.REPOSITORY_URL, "select * where{ ?X ?P ?Y }",
				QueryLanguage.SPARQL)) {
			QueryResultIO.writeTuple(queryResult, TupleQueryResultFormat.SPARQL, System.out);
		}
	}

	/**
	 * Checks that the server accepts a direct POST with a content type of "application/sparql-query".
	 */
	@Test
	public void testQueryDirect_POST() throws Exception {
		String query = "DESCRIBE <monkey:pod>";
		String location = TestServer.REPOSITORY_URL;

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost post = new HttpPost(location);
		HttpEntity entity = new StringEntity(query, ContentType.create(Protocol.SPARQL_QUERY_MIME_TYPE));
		post.setEntity(entity);

		CloseableHttpResponse response = httpclient.execute(post);

		System.out.println("Query Direct POST Status: " + response.getStatusLine());
		int statusCode = response.getStatusLine().getStatusCode();
		assertEquals(true, statusCode >= 200 && statusCode < 400);
	}

	/**
	 * Checks that the server accepts a direct POST with a content type of "application/sparql-update".
	 */
	@Test
	public void testUpdateDirect_POST() throws Exception {
		String query = "delete where { <monkey:pod> ?p ?o }";
		String location = Protocol.getStatementsLocation(TestServer.REPOSITORY_URL);

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost post = new HttpPost(location);
		HttpEntity entity = new StringEntity(query, ContentType.create(Protocol.SPARQL_UPDATE_MIME_TYPE));
		post.setEntity(entity);

		CloseableHttpResponse response = httpclient.execute(post);

		System.out.println("Update Direct Post Status: " + response.getStatusLine());
		int statusCode = response.getStatusLine().getStatusCode();
		assertEquals(true, statusCode >= 200 && statusCode < 400);
	}

	/**
	 * Checks that the server accepts a formencoded POST with an update and a timeout parameter.
	 */
	@Test
	public void testUpdateForm_POST() throws Exception {
		String update = "delete where { <monkey:pod> ?p ?o . }";
		String location = Protocol.getStatementsLocation(TestServer.REPOSITORY_URL);
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost post = new HttpPost(location);
		List<NameValuePair> nvps = new ArrayList<>();
		nvps.add(new BasicNameValuePair(Protocol.UPDATE_PARAM_NAME, update));
		nvps.add(new BasicNameValuePair(Protocol.TIMEOUT_PARAM_NAME, "1"));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8);

		post.setEntity(entity);

		CloseableHttpResponse response = httpclient.execute(post);

		System.out.println("Update Form Post Status: " + response.getStatusLine());
		int statusCode = response.getStatusLine().getStatusCode();
		assertEquals(true, statusCode >= 200 && statusCode < 400);
	}

	/**
	 * Checks that the requested content type is returned when accept header explicitly set.
	 */
	@Test
	public void testContentTypeForGraphQuery1_GET() throws Exception {
		String query = "DESCRIBE <foo:bar>";
		String location = TestServer.REPOSITORY_URL;
		location += "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

		URL url = new URL(location);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		// Request RDF/XML formatted results:
		conn.setRequestProperty("Accept", RDFFormat.RDFXML.getDefaultMIMEType());

		conn.connect();

		try {
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				String contentType = conn.getHeaderField("Content-Type");
				assertNotNull(contentType);

				// snip off optional charset declaration
				int charPos = contentType.indexOf(';');
				if (charPos > -1) {
					contentType = contentType.substring(0, charPos);
				}

				assertEquals(RDFFormat.RDFXML.getDefaultMIMEType(), contentType);
			} else {
				String response = "location " + location + " responded: " + conn.getResponseMessage() + " ("
						+ responseCode + ")";
				fail(response);
				throw new RuntimeException(response);
			}
		} finally {
			conn.disconnect();
		}
	}

	/**
	 * Checks that a proper error (HTTP 406) is returned when accept header is set incorrectly on graph query.
	 */
	@Test
	public void testContentTypeForGraphQuery2_GET() throws Exception {
		String query = "DESCRIBE <foo:bar>";
		String location = TestServer.REPOSITORY_URL;
		location += "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

		URL url = new URL(location);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		// incorrect mime-type for graph query results
		conn.setRequestProperty("Accept", TupleQueryResultFormat.SPARQL.getDefaultMIMEType());

		conn.connect();

		try {
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE) {
				// do nothing, expected
			} else {
				String response = "location " + location + " responded: " + conn.getResponseMessage() + " ("
						+ responseCode + ")";
				fail(response);
			}
		} finally {
			conn.disconnect();
		}
	}

	@Test
	public void testQueryResponse_HEAD() throws Exception {
		String query = "DESCRIBE <foo:bar>";
		String location = TestServer.REPOSITORY_URL;
		location += "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

		URL url = new URL(location);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("HEAD");

		// Request RDF/XML formatted results:
		conn.setRequestProperty("Accept", RDFFormat.RDFXML.getDefaultMIMEType());

		conn.connect();

		try {
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				String contentType = conn.getHeaderField("Content-Type");
				assertNotNull(contentType);

				// snip off optional charset declaration
				int charPos = contentType.indexOf(';');
				if (charPos > -1) {
					contentType = contentType.substring(0, charPos);
				}

				assertEquals(RDFFormat.RDFXML.getDefaultMIMEType(), contentType);
				assertEquals(0, conn.getContentLength());
			} else {
				String response = "location " + location + " responded: " + conn.getResponseMessage() + " ("
						+ responseCode + ")";
				fail(response);
				throw new RuntimeException(response);
			}
		} finally {
			conn.disconnect();
		}
	}

	@Test
	public void testUpdateResponse_HEAD() throws Exception {
		String query = "INSERT DATA { <foo:foo> <foo:bar> \"foo\". } ";
		String location = Protocol.getStatementsLocation(TestServer.REPOSITORY_URL);
		location += "?update=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

		URL url = new URL(location);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("HEAD");

		conn.connect();

		try {
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				String contentType = conn.getHeaderField("Content-Type");
				assertNotNull(contentType);

				// snip off optional charset declaration
				int charPos = contentType.indexOf(';');
				if (charPos > -1) {
					contentType = contentType.substring(0, charPos);
				}

				assertEquals(0, conn.getContentLength());
			} else {
				String response = "location " + location + " responded: " + conn.getResponseMessage() + " ("
						+ responseCode + ")";
				fail(response);
				throw new RuntimeException(response);
			}
		} finally {
			conn.disconnect();
		}
	}

	/**
	 * Test for SES-1861
	 *
	 * @throws Exception
	 */
	@Test
	public void testSequentialNamespaceUpdates() throws Exception {
		int limitCount = 1000;
		int limitPrefix = 50;

		Random prng = new Random(234235434);
		// String repositoryLocation =
		// Protocol.getRepositoryLocation("http://localhost:8080/openrdf-sesame",
		// "Test-NativeStore");
		String repositoryLocation = TestServer.REPOSITORY_URL;

		for (int count = 0; count < limitCount; count++) {
			int i = prng.nextInt(limitPrefix);
			String prefix = "prefix" + i;
			String ns = "http://example.org/namespace" + i;

			String location = Protocol.getNamespacePrefixLocation(repositoryLocation, prefix);

			if (count % 2 == 0) {
				putNamespace(location, ns);
			} else {
				deleteNamespace(location);
			}
		}
	}

	/**
	 * Test for GitHub issue #262
	 *
	 * @throws Exception
	 */
	@Test
	public void testPutEmptyPrefix() throws Exception {
		String repositoryLocation = TestServer.REPOSITORY_URL;
		String namespacesLocation = Protocol.getNamespacesLocation(repositoryLocation);
		String emptyPrefixLocation = Protocol.getNamespacePrefixLocation(repositoryLocation, "");

		Set<Namespace> namespacesBefore = getNamespaces(namespacesLocation);

		putNamespace(emptyPrefixLocation, "http://example.org/");

		Set<Namespace> namespacesAfter = getNamespaces(namespacesLocation);

		Set<Namespace> namespaceDeletions = Sets.difference(namespacesBefore, namespacesAfter);
		Set<Namespace> namespaceAdditions = Sets.difference(namespacesAfter, namespacesBefore);

		assertTrue("Some namespaces have been deleted", namespaceDeletions.isEmpty());
		assertEquals(Sets.newHashSet(new SimpleNamespace("", "http://example.org/")), namespaceAdditions);
	}

	/**
	 * Test for SES-1861
	 *
	 * @throws Exception
	 */
	@Test
	public void testConcurrentNamespaceUpdates() throws Exception {
		int limitCount = 1000;
		int limitPrefix = 50;

		Random prng = new Random(234542434);

		// String repositoryLocation =
		// Protocol.getRepositoryLocation("http://localhost:8080/openrdf-sesame",
		// "Test-NativeStore");
		String repositoryLocation = TestServer.REPOSITORY_URL;

		ExecutorService threadPool = Executors.newFixedThreadPool(20,
				new ThreadFactoryBuilder().setNameFormat("rdf4j-protocoltest-%d").build());

		for (int count = 0; count < limitCount; count++) {
			final int number = count;
			final int i = prng.nextInt(limitPrefix);
			final String prefix = "prefix" + i;
			final String ns = "http://example.org/namespace" + i;

			final String location = Protocol.getNamespacePrefixLocation(repositoryLocation, prefix);

			Runnable runner = () -> {
				try {
					if (number % 2 == 0) {
						putNamespace(location, ns);
					} else {
						deleteNamespace(location);
					}
				} catch (Exception e) {
					e.printStackTrace();
					fail("Failed in test: " + number);
				}
			};
			threadPool.execute(runner);
		}
		threadPool.shutdown();
		threadPool.awaitTermination(30000, TimeUnit.MILLISECONDS);
		threadPool.shutdownNow();
	}

	private Set<Namespace> getNamespaces(String location) throws Exception {
		URL url = new URL(location);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		try {
			conn.setRequestProperty("Accept", "text/csv; charset=utf-8");
			conn.connect();

			int responseCode = conn.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				String response = "location " + location + " responded: " + conn.getResponseMessage() + " ("
						+ responseCode + ")";
				fail(response);
			}

			try (CSVReader reader = new CSVReader(
					new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
				String[] headerRow = reader.readNext();

				if (headerRow == null) {
					fail("header not found");
				}

				if (!Arrays.equals(headerRow, new String[] { "prefix", "namespace" })) {
					fail("illegal header row: " + Arrays.toString(headerRow));
				}

				Set<Namespace> namespaces = new HashSet<>();

				String[] aRow;
				while ((aRow = reader.readNext()) != null) {
					String prefix = aRow[0];
					String namespace = aRow[1];

					namespaces.add(new SimpleNamespace(prefix, namespace));
				}
				return namespaces;
			}
		} finally {
			conn.disconnect();
		}
	}

	private void putNamespace(String location, String namespace) throws Exception {
		// System.out.println("Put namespace to " + location);

		URL url = new URL(location);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("PUT");
		conn.setDoOutput(true);

		try (InputStream dataStream = new ByteArrayInputStream(namespace.getBytes(StandardCharsets.UTF_8))) {
			try (OutputStream connOut = conn.getOutputStream()) {
				IOUtil.transfer(dataStream, connOut);
			}
		}

		conn.connect();

		int responseCode = conn.getResponseCode();

		// HTTP 200 or 204
		if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
			String response = "location " + location + " responded: " + conn.getResponseMessage() + " (" + responseCode
					+ ")";
			fail(response);
		}
	}

	private void deleteNamespace(String location) throws Exception {
		// System.out.println("Delete namespace at " + location);

		URL url = new URL(location);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("DELETE");
		conn.setDoOutput(true);

		conn.connect();

		int responseCode = conn.getResponseCode();

		// HTTP 200 or 204
		if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
			String response = "location " + location + " responded: " + conn.getResponseMessage() + " (" + responseCode
					+ ")";
			fail(response);
		}
	}

	private void putFile(String location, String file) throws Exception {
		System.out.println("Put file to " + location);

		URL url = new URL(location);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("PUT");
		conn.setDoOutput(true);

		RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);
		conn.setRequestProperty("Content-Type", dataFormat.getDefaultMIMEType());

		try (InputStream dataStream = ProtocolIT.class.getResourceAsStream(file)) {
			try (OutputStream connOut = conn.getOutputStream()) {
				IOUtil.transfer(Objects.requireNonNull(dataStream), connOut);
			}
		}

		conn.connect();

		int responseCode = conn.getResponseCode();

		// HTTP 200 or 204
		if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
			String response = "location " + location + " responded: " + conn.getResponseMessage() + " (" + responseCode
					+ ")";
			fail(response);
		}
	}

	private void delete(String location) throws Exception {
		URL url = new URL(location);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("DELETE");

		conn.connect();

		int responseCode = conn.getResponseCode();

		// HTTP 200 or 204
		if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
			String response = "location " + location + " responded: " + conn.getResponseMessage() + " (" + responseCode
					+ ")";
			fail(response);
		}
	}

	private TupleQueryResult evaluateTupleQuery(String location, String query, QueryLanguage queryLn) throws Exception {
		location += "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&queryLn=" + queryLn.getName();

		URL url = new URL(location);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		// Request SPARQL-XML formatted results:
		conn.setRequestProperty("Accept", TupleQueryResultFormat.SPARQL.getDefaultMIMEType());

		conn.connect();

		try {
			int responseCode = conn.getResponseCode();
			// HTTP 200
			if (responseCode == HttpURLConnection.HTTP_OK) {
				// Process query results
				return QueryResultIO.parseTuple(conn.getInputStream(), TupleQueryResultFormat.SPARQL, null);
			} else {
				String response = "location " + location + " responded: " + conn.getResponseMessage() + " ("
						+ responseCode + ")";
				fail(response);
				throw new RuntimeException(response);
			}
		} finally {
			conn.disconnect();
		}
	}

}
