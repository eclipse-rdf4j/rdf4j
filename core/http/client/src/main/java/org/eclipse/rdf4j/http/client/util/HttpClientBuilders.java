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
package org.eclipse.rdf4j.http.client.util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClientConfig;

/**
 * Convenience utility class offering helper methods to configure {@link RDF4JHttpClientConfig} instances.
 *
 * @author Andreas Schwarte
 * @see HttpClientDependent
 */
public class HttpClientBuilders {

	/**
	 * Return an {@link RDF4JHttpClientConfig} configured to trust all SSL certificates and skip hostname verification.
	 *
	 * <p>
	 * This installs a no-op {@link javax.net.ssl.TrustManager} that accepts any certificate chain, and disables TLS
	 * endpoint identification so that hostname mismatches are also accepted. Both checks are suppressed, making this
	 * suitable for self-signed certificates in controlled/test environments.
	 *
	 * <p>
	 * <strong>Warning:</strong> this configuration is inherently insecure and must never be used in production.
	 *
	 * @return an {@link RDF4JHttpClientConfig} for <i>SSL trust all</i>
	 */
	public static RDF4JHttpClientConfig getSslTrustAllConfig() {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			} }, null);
			return RDF4JHttpClientConfig.newBuilder().sslContext(sslContext).disableHostnameVerification(true).build();
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}
}
