/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.client.jdk;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

import javax.net.ssl.SSLParameters;

import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClientConfig;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClientFactory;

/**
 * {@link RDF4JHttpClientFactory} implementation that uses the JDK built-in {@code java.net.http.HttpClient}.
 */
public class JdkRDF4JHttpClientFactory implements RDF4JHttpClientFactory {

	@Override
	public String getName() {
		return "jdk";
	}

	@Override
	public RDF4JHttpClient create() {
		return create(RDF4JHttpClientConfig.defaultConfig());
	}

	@Override
	public RDF4JHttpClient create(RDF4JHttpClientConfig config) {
		HttpClient.Builder builder = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
				.followRedirects(config.isFollowRedirects()
						? HttpClient.Redirect.ALWAYS
						: HttpClient.Redirect.NEVER)
				.proxy(ProxySelector.getDefault());

		config.getSslContext().ifPresent(builder::sslContext);

		if (config.isDisableHostnameVerification()) {
			SSLParameters sslParameters = new SSLParameters();
			sslParameters.setEndpointIdentificationAlgorithm(null);
			builder.sslParameters(sslParameters);
		}

		builder.authenticator(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				if (getRequestorType() == RequestorType.PROXY) {
					String user = System.getProperty("http.proxyUser");
					String pass = System.getProperty("http.proxyPassword");
					if (user != null && pass != null) {
						return new PasswordAuthentication(user, pass.toCharArray());
					}
				}
				return null;
			}
		});

		return new JdkRDF4JHttpClient(builder.build(), config);
	}
}
