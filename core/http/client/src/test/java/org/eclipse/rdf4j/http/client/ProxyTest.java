/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;
import org.mockserver.model.MediaType;
import org.mockserver.model.NottableString;
import org.mockserver.verify.VerificationTimes;

/**
 * Unit tests for {@link SPARQLProtocolSession} using standard Java properties for proxy configuration.
 *
 * @author Manuel Fiorelli
 */
@ExtendWith(MockServerExtension.class)
public class ProxyTest {

	// the hostname is guaranteed not to exist (https://datatracker.ietf.org/doc/html/rfc6761#section-6.4)
	String serverURL = "http://rdf4j.invalid/rdf4j-server";
	String repositoryID = "test";

	String proxyUser = "proxyUser";
	String proxyPassword = "proxyPassword";

	/* @Nullable */ String proxyHostOld;
	/* @Nullable */ String proxyPortOld;
	/* @Nullable */ String proxyUserOld;
	/* @Nullable */ String proxyPasswordOld;

	RDF4JProtocolSession sparqlSession;

	@BeforeEach
	public void setUp(MockServerClient client) {
		// Set the system properties related to (non-secured) HTTP proxy.
		// Keep a copy of the old value, if any, to restore it after the execution of the test.
		proxyHostOld = System.setProperty("http.proxyHost", "localhost");
		proxyPortOld = System.setProperty("http.proxyPort", String.valueOf(client.getPort()));
		proxyUserOld = System.setProperty("http.proxyUser", proxyUser);
		proxyPasswordOld = System.setProperty("http.proxyPassword", proxyPassword);

		// Instantiate an RDF4JProtocolSession
		sparqlSession = new SharedHttpClientSessionManager().createRDF4JProtocolSession(serverURL);
		sparqlSession.setQueryURL(Protocol.getRepositoryLocation(serverURL, repositoryID));
		sparqlSession.setUpdateURL(
				Protocol.getStatementsLocation(Protocol.getRepositoryLocation(serverURL, repositoryID)));
	}

	@AfterEach
	public void tearDown() {
		// Restore previous value of the system properties, if any
		restoreSystemProperty("http.proxyHost", proxyHostOld);
		restoreSystemProperty("http.proxyPort", proxyPortOld);
		restoreSystemProperty("http.proxyUser", proxyUserOld);
		restoreSystemProperty("http.proxyPassword", proxyPasswordOld);
	}

	void restoreSystemProperty(String key, /* @Nullable */ String value) {
		if (StringUtils.isNotBlank(value)) {
			System.setProperty(key, value);
		} else {
			System.clearProperty(key);
		}
	}

	@Test
	public void testUserNameAndPassword(MockServerClient client) throws Exception {
		String serverUser = "serverUser";
		String serverPassword = "serverPassword";

		String proxyCredentialsEncoded = Base64.getEncoder()
				.encodeToString((proxyUser + ":" + proxyPassword).getBytes(StandardCharsets.US_ASCII));
		String serverCredentialsEncoded = Base64.getEncoder()
				.encodeToString((serverUser + ":" + serverPassword).getBytes(StandardCharsets.US_ASCII));

		// Mock requests to request proxy and server authentication

		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test")
						.withHeader(header(NottableString.not("Proxy-Authorization")))
		)
				.respond(
						response()
								.withStatusCode(407)
								.withHeader("Proxy-Authenticate", "Basic realm=\"rdf4j\"")
				);

		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test")
						.withHeader("Proxy-Authorization", "Basic " + proxyCredentialsEncoded)
						.withHeader(header(NottableString.not("Authorization")))
		)
				.respond(
						response()
								.withStatusCode(401)
								.withHeader("WWW-Authenticate", "Basic realm=\"rdf4j\"")
				);

		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test")
						.withHeader("Proxy-Authorization", "Basic " + proxyCredentialsEncoded)
						.withHeader("Authorization", "Basic " + serverCredentialsEncoded),
				Times.once()
		)
				.respond(
						response()
								.withStatusCode(200)
								.withContentType(MediaType.parse("application/sparql-results+xml;charset=UTF-8"))
								.withBody("<?xml version='1.0' encoding='UTF-8'?>\n" +
										"<sparql xmlns='http://www.w3.org/2005/sparql-results#'>\n" +
										"    <head>\n" +
										"    </head>\n" +
										"    <boolean>true</boolean>\n" +
										"</sparql>")
				);

		// Set server the credentials for the server
		sparqlSession.setUsernameAndPassword(serverUser, serverPassword);

		// Invoke the test
		boolean response = sparqlSession.sendBooleanQuery(QueryLanguage.SPARQL, "ASK {}", new SimpleDataset(), false);

		// Verifications
		assertThat(response).isTrue();
		client.verify(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test")
						.withHeader("Proxy-Authorization", "Basic " + proxyCredentialsEncoded)
						.withHeader("Authorization", "Basic " + serverCredentialsEncoded),
				VerificationTimes.once()
		);

	}

}
