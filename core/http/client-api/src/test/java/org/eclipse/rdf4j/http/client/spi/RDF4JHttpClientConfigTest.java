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
package org.eclipse.rdf4j.http.client.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;

class RDF4JHttpClientConfigTest {

	@Test
	void toBuilderCopiesAllFields() {
		RDF4JHttpClientConfig original = RDF4JHttpClientConfig.newBuilder()
				.connectTimeoutMs(1_000)
				.socketTimeoutMs(2_000)
				.connectionRequestTimeoutMs(3_000)
				.maxConnectionsPerRoute(10)
				.maxConnectionsTotal(20)
				.maxRedirects(3)
				.followRedirects(false)
				.idleConnectionTimeoutMs(60_000L)
				.disableHostnameVerification(true)
				.defaultHeaders(List.of(HttpHeader.of("X-Custom", "value")))
				.build();

		RDF4JHttpClientConfig copy = original.toBuilder().build();

		assertThat(copy.getConnectTimeoutMs()).isEqualTo(original.getConnectTimeoutMs());
		assertThat(copy.getSocketTimeoutMs()).isEqualTo(original.getSocketTimeoutMs());
		assertThat(copy.getConnectionRequestTimeoutMs()).isEqualTo(original.getConnectionRequestTimeoutMs());
		assertThat(copy.getMaxConnectionsPerRoute()).isEqualTo(original.getMaxConnectionsPerRoute());
		assertThat(copy.getMaxConnectionsTotal()).isEqualTo(original.getMaxConnectionsTotal());
		assertThat(copy.getMaxRedirects()).isEqualTo(original.getMaxRedirects());
		assertThat(copy.isFollowRedirects()).isEqualTo(original.isFollowRedirects());
		assertThat(copy.getIdleConnectionTimeoutMs()).isEqualTo(original.getIdleConnectionTimeoutMs());
		assertThat(copy.isDisableHostnameVerification()).isEqualTo(original.isDisableHostnameVerification());
		assertThat(copy.getDefaultHeaders())
				.usingRecursiveFieldByFieldElementComparator()
				.isEqualTo(original.getDefaultHeaders());
		assertThat(copy.getSslContext()).isEqualTo(original.getSslContext());
	}

	@Test
	void toBuilderOverrideDoesNotAffectOriginal() {
		RDF4JHttpClientConfig original = RDF4JHttpClientConfig.newBuilder()
				.connectTimeoutMs(5_000)
				.socketTimeoutMs(10_000)
				.connectionRequestTimeoutMs(15_000)
				.defaultHeader("X-Original", "yes")
				.build();

		RDF4JHttpClientConfig modified = original.toBuilder()
				.connectTimeoutMs(1_000)
				.socketTimeoutMs(2_000)
				.connectionRequestTimeoutMs(3_000)
				.defaultHeaders(List.of(HttpHeader.of("X-Modified", "yes")))
				.build();

		// original is unchanged
		assertThat(original.getConnectTimeoutMs()).isEqualTo(5_000);
		assertThat(original.getSocketTimeoutMs()).isEqualTo(10_000);
		assertThat(original.getConnectionRequestTimeoutMs()).isEqualTo(15_000);
		assertThat(original.getDefaultHeaders())
				.extracting(HttpHeader::getName, HttpHeader::getValue)
				.containsExactly(tuple("X-Original", "yes"));

		// modified has new values
		assertThat(modified.getConnectTimeoutMs()).isEqualTo(1_000);
		assertThat(modified.getSocketTimeoutMs()).isEqualTo(2_000);
		assertThat(modified.getConnectionRequestTimeoutMs()).isEqualTo(3_000);
		assertThat(modified.getDefaultHeaders())
				.extracting(HttpHeader::getName, HttpHeader::getValue)
				.containsExactly(tuple("X-Modified", "yes"));
	}

	@Test
	void toBuilderDefaultHeadersListIsIndependent() {
		RDF4JHttpClientConfig original = RDF4JHttpClientConfig.newBuilder()
				.defaultHeader("X-A", "1")
				.build();

		// Adding a header via toBuilder should not affect the original
		RDF4JHttpClientConfig withExtra = original.toBuilder()
				.defaultHeader("X-B", "2")
				.build();

		assertThat(original.getDefaultHeaders()).hasSize(1);
		assertThat(original.getDefaultHeaders())
				.extracting(HttpHeader::getName, HttpHeader::getValue)
				.containsExactly(tuple("X-A", "1"));
		assertThat(withExtra.getDefaultHeaders()).hasSize(2);
	}
}
