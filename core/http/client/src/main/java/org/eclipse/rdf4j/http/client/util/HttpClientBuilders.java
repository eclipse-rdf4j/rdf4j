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

import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.eclipse.rdf4j.http.client.HttpClientDependent;

/**
 * Convenience utility class offering helper methods to configure {@link HttpClient}s and {@link HttpClientBuilders}.
 *
 * @author Andreas Schwarte
 * @see HttpClientDependent
 */
public class HttpClientBuilders {

	/**
	 * Return an {@link HttpClientBuilder} that can be used to build an {@link HttpClient} which trusts all certificates
	 * (particularly including self-signed certificates).
	 *
	 * @return a {@link HttpClientBuilder} for <i>SSL trust all</i>
	 */
	public static HttpClientBuilder getSSLTrustAllHttpClientBuilder() {
		try {
			SSLContextBuilder builder = new SSLContextBuilder();
			builder.loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true);

			HostnameVerifier hostNameVerifier = (String hostname, SSLSession session) -> true;
			SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(builder.build(), hostNameVerifier);

			return HttpClients.custom().setSSLSocketFactory(sslSF).useSystemProperties();
		} catch (Exception e) {
			// key management exception, etc.
			throw new RuntimeException(e);
		}
	}
}
