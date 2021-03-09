/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.permanentRedirect;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.junit.ClassRule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Unit tests for {@link RDFLoader}.
 * 
 * @author Manuel Fiorelli
 *
 */
public class RDFLoaderTest {

	@ClassRule
	public static WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

	@Test
	public void testTurteDocument() throws Exception {
		stubFor(get("/Socrates.ttl")
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", RDFFormat.TURTLE.getDefaultMIMEType())
						.withBody("<http://example.org/Socrates> a <http://xmlns.com/foaf/0.1/Person> .")));

		RDFLoader rdfLoader = new RDFLoader(new ParserConfig(), SimpleValueFactory.getInstance());

		RDFHandler rdfHandler = mock(RDFHandler.class);

		rdfLoader.load(new URL("http://localhost:" + wireMockRule.port() + "/Socrates.ttl"), null, null, rdfHandler);

		verify(rdfHandler).startRDF();
		verify(rdfHandler)
				.handleStatement(SimpleValueFactory.getInstance()
						.createStatement(SimpleValueFactory.getInstance().createIRI("http://example.org/Socrates"),
								RDF.TYPE,
								FOAF.PERSON));
		verify(rdfHandler).endRDF();
	}

	@Test
	public void testNonInformationResource() throws Exception {
		final SSLSocketFactory toRestoreSocketFactory = disableSSLCertificatCheck();
		try {
			final HostnameVerifier toRestoreHostnameVerifier = disableHostnameVerifier();
			try {
				stubFor(get("/Socrates")
						.willReturn(
								permanentRedirect("https://localhost:" + wireMockRule.httpsPort() + "/Socrates.ttl")));

				stubFor(get("/Socrates.ttl")
						.willReturn(aResponse()
								.withStatus(200)
								.withHeader("Content-Type", RDFFormat.TURTLE.getDefaultMIMEType())
								.withBody("<http://example.org/Socrates> a <http://xmlns.com/foaf/0.1/Person> .")));

				RDFLoader rdfLoader = new RDFLoader(new ParserConfig(), SimpleValueFactory.getInstance());

				RDFHandler rdfHandler = mock(RDFHandler.class);

				rdfLoader.load(new URL("http://localhost:" + wireMockRule.port() + "/Socrates"), null, null,
						rdfHandler);

				verify(rdfHandler).startRDF();
				verify(rdfHandler)
						.handleStatement(SimpleValueFactory.getInstance()
								.createStatement(
										SimpleValueFactory.getInstance().createIRI("http://example.org/Socrates"),
										RDF.TYPE,
										FOAF.PERSON));
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
