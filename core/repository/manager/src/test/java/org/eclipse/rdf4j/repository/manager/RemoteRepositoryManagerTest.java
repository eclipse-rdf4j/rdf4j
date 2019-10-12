/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Unit tests for {@link RemoteRepositoryManager}
 *
 * @author Jeen Broekstra
 *
 */
public class RemoteRepositoryManagerTest extends RepositoryManagerTest {

	@ClassRule
	public static WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

	@Override
	@Before
	public void setUp() {
		subject = new RemoteRepositoryManager("http://localhost:" + wireMockRule.port() + "/rdf4j-server");
		wireMockRule.resetAll();
	}

	@Test
	public void testAddRepositoryConfig() {
		wireMockRule.stubFor(get(urlEqualTo("/rdf4j-server/protocol"))
				.willReturn(aResponse().withStatus(200).withBody(Protocol.VERSION)));
		wireMockRule
				.stubFor(put(urlEqualTo("/rdf4j-server/repositories/test")).willReturn(aResponse().withStatus(204)));

		RepositoryConfig config = new RepositoryConfig("test");

		subject.addRepositoryConfig(config);

		wireMockRule.verify(
				putRequestedFor(urlEqualTo("/rdf4j-server/repositories/test")).withRequestBody(matching("^BRDF.*"))
						.withHeader("Content-Type", equalTo("application/x-binary-rdf")));
	}

	@Test
	public void testAddRepositoryConfigLegacy() {
		wireMockRule.stubFor(
				get(urlEqualTo("/rdf4j-server/protocol")).willReturn(aResponse().withStatus(200).withBody("8")));
		wireMockRule.stubFor(post(urlPathEqualTo("/rdf4j-server/repositories/SYSTEM/statements"))
				.willReturn(aResponse().withStatus(204)));
		wireMockRule.stubFor(get(urlEqualTo("/rdf4j-server/repositories"))
				.willReturn(aResponse().withHeader("Content-type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBodyFile("repository-list-response.srx")
						.withStatus(200)));

		RepositoryConfig config = new RepositoryConfig("test");

		subject.addRepositoryConfig(config);

		wireMockRule.verify(postRequestedFor(urlPathEqualTo("/rdf4j-server/repositories/SYSTEM/statements"))
				.withRequestBody(matching("^BRDF.*"))
				.withHeader("Content-Type", equalTo("application/x-binary-rdf")));
	}
}
