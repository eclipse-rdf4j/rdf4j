/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.getValueFactory;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.MediaType;

/**
 * Unit tests for {@link RDFLoader}.
 *
 * @author Manuel Fiorelli
 */
@ExtendWith(MockServerExtension.class)
public class RDFLoaderTest {
	@BeforeAll
	static void defineMockServerBehavior(MockServerClient client) {
		client.when(
				request()
						.withMethod("GET")
						.withPath("/Socrates.ttl")
		)
				.respond(
						response()
								.withContentType(MediaType.parse(RDFFormat.TURTLE.getDefaultMIMEType()))
								.withBody("<http://example.org/Socrates> a <http://xmlns.com/foaf/0.1/Person> .")

				);
		client.when(
				request()
						.withMethod("GET")
						.withPath("/Socrates")
		)
				.respond(
						response()
								.withStatusCode(301)
								.withHeader("Location", "/Socrates.ttl")

				);
		client.when(
				request()
						.withMethod("GET")
						.withPath("/Socrates1")
		)
				.respond(
						response()
								.withStatusCode(301)
								.withHeader("Location", "/Socrates2")

				);
		client.when(
				request()
						.withMethod("GET")
						.withPath("/Socrates2")
		)
				.respond(
						response()
								.withStatusCode(301)
								.withHeader("Location", "/Socrates.ttl")

				);
	}

	@Test
	public void testTurtleJavaResource() throws Exception {
		RDFLoader rdfLoader = new RDFLoader(new ParserConfig(), getValueFactory());

		RDFHandler rdfHandler = mock(RDFHandler.class);

		rdfLoader.load(this.getClass().getResource("Socrates.ttl"), null, RDFFormat.TURTLE, rdfHandler);

		verify(rdfHandler).startRDF();
		verify(rdfHandler)
				.handleStatement(statement(iri("http://example.org/Socrates"),
						RDF.TYPE,
						FOAF.PERSON, null));
		verify(rdfHandler).endRDF();
	}

	@Test
	public void testTurtleDocument(MockServerClient client) throws Exception {
		RDFLoader rdfLoader = new RDFLoader(new ParserConfig(), getValueFactory());

		RDFHandler rdfHandler = mock(RDFHandler.class);

		rdfLoader.load(new URL("http://localhost:" + client.getPort() + "/Socrates.ttl"), null, null,
				rdfHandler);

		verify(rdfHandler).startRDF();
		verify(rdfHandler)
				.handleStatement(statement(iri("http://example.org/Socrates"),
						RDF.TYPE,
						FOAF.PERSON, null));
		verify(rdfHandler).endRDF();
	}

	@Test
	public void testMultipleRedirects(MockServerClient client) throws Exception {
		RDFLoader rdfLoader = new RDFLoader(new ParserConfig(), getValueFactory());

		RDFHandler rdfHandler = mock(RDFHandler.class);

		rdfLoader.load(new URL("http://localhost:" + client.getPort() + "/Socrates1"), null, null,
				rdfHandler);

		verify(rdfHandler).startRDF();
		verify(rdfHandler)
				.handleStatement(statement(iri("http://example.org/Socrates"),
						RDF.TYPE,
						FOAF.PERSON, null));
		verify(rdfHandler).endRDF();
	}

	@Test
	public void testAbortOverMaxRedirects(MockServerClient client) throws Exception {
		/* nullable */
		String oldMaxRedirects = System.getProperty("http.maxRedirects");
		try {
			ProtocolException actualException = null;

			System.setProperty("http.maxRedirects", "2"); // http.maxRedirects seems exclusive in http URL

			RDFLoader rdfLoader = new RDFLoader(new ParserConfig(), getValueFactory());

			RDFHandler rdfHandler = mock(RDFHandler.class);
			try {
				rdfLoader.load(new URL("http://localhost:" + client.getPort() + "/Socrates1"), null, null,
						rdfHandler);
			} catch (ProtocolException e) {
				actualException = e;
			}

			assertThat(actualException)
					.hasMessageStartingWith("Server redirected too many times");
		} finally {
			if (oldMaxRedirects != null) {
				System.setProperty("http.maxRedirects", oldMaxRedirects);
			} else {
				System.getProperties().remove("http.maxRedirects");
			}
		}
	}

	@Test
	public void testNonInformationResource(MockServerClient client) throws Exception {
		final SSLSocketFactory toRestoreSocketFactory = disableSSLCertificatCheck();
		try {
			final HostnameVerifier toRestoreHostnameVerifier = disableHostnameVerifier();
			try {
				RDFLoader rdfLoader = new RDFLoader(new ParserConfig(), getValueFactory());

				RDFHandler rdfHandler = mock(RDFHandler.class);

				rdfLoader.load(new URL("http://localhost:" + client.getPort() + "/Socrates"), null, null,
						rdfHandler);

				verify(rdfHandler).startRDF();
				verify(rdfHandler)
						.handleStatement(statement(
								iri("http://example.org/Socrates"),
								RDF.TYPE,
								FOAF.PERSON, null));
				verify(rdfHandler).endRDF();
			} finally {
				restoreHostnameVerifier(toRestoreHostnameVerifier);
			}
		} finally {
			restoreSocketFactory(toRestoreSocketFactory);
		}
	}

	private static HostnameVerifier disableHostnameVerifier() {
		HostnameVerifier replaced = HttpsURLConnection.getDefaultHostnameVerifier();
		// set a hostname verifier that just returns true for every request
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});

		return replaced;
	}

	private static SSLSocketFactory disableSSLCertificatCheck()
			throws KeyManagementException, NoSuchAlgorithmException {
		// set a trust manager that just returns true for every request (this is _very_ unsafe and should only be used
		// in the test environment)

		TrustManager trustManager = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				// do nothing, accept all clients
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				// do nothing accept all servers
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		final SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, new TrustManager[] { trustManager }, SecureRandom.getInstanceStrong());

		SSLSocketFactory replaced = HttpsURLConnection.getDefaultSSLSocketFactory();
		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

		return replaced;
	}

	private static void restoreHostnameVerifier(HostnameVerifier toRestore) {
		HttpsURLConnection.setDefaultHostnameVerifier(toRestore);
	}

	private static void restoreSocketFactory(SSLSocketFactory toRestore) {
		HttpsURLConnection.setDefaultSSLSocketFactory(toRestore);
	}
}
