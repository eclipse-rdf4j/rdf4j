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

import java.net.URI;

import org.junit.jupiter.api.Test;

public class UriBuilderTest {

	@Test
	public void testFragmentInBaseUrl_queryInsertedBeforeFragment() {
		URI uri = UriBuilder.from("http://example.com/path#section1")
				.addParameter("foo", "bar")
				.build();

		assertThat(uri.toString()).isEqualTo("http://example.com/path?foo=bar#section1");
		assertThat(uri.getFragment()).isEqualTo("section1");
		assertThat(uri.getQuery()).isEqualTo("foo=bar");
	}

	@Test
	public void testFragmentInBaseUrl_existingQueryAndFragment() {
		URI uri = UriBuilder.from("http://example.com/path?existing=1#section1")
				.addParameter("foo", "bar")
				.build();

		assertThat(uri.toString()).isEqualTo("http://example.com/path?existing=1&foo=bar#section1");
		assertThat(uri.getFragment()).isEqualTo("section1");
		assertThat(uri.getQuery()).isEqualTo("existing=1&foo=bar");
	}

	@Test
	public void testNoFragment_queryAppendedNormally() {
		URI uri = UriBuilder.from("http://example.com/path")
				.addParameter("foo", "bar")
				.build();

		assertThat(uri.toString()).isEqualTo("http://example.com/path?foo=bar");
		assertThat(uri.getFragment()).isNull();
	}

	@Test
	public void testFragmentInBaseUrl_noParams_returnsBaseUrlUnchanged() {
		URI uri = UriBuilder.from("http://example.com/path#section1").build();

		assertThat(uri.toString()).isEqualTo("http://example.com/path#section1");
	}
}
