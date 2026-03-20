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
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpClientResponsesTest {

	private static final String CAFE = "café";

	@Mock
	HttpClientResponse response;

	private void stubResponse(String contentTypeHeader, String body, Charset bodyCharset) throws IOException {
		when(response.getHeader("Content-Type"))
				.thenReturn(contentTypeHeader == null ? Optional.empty() : Optional.of(contentTypeHeader));
		when(response.getBodyAsStream())
				.thenReturn(new ByteArrayInputStream(body.getBytes(bodyCharset)));
	}

	// 1. No Content-Type header → default ISO-8859-1
	@Test
	void noContentTypeHeader_usesIso88591() throws IOException {
		stubResponse(null, CAFE, StandardCharsets.ISO_8859_1);
		assertThat(HttpClientResponses.toString(response)).isEqualTo(CAFE);
	}

	// 2. charset=utf-8 (unquoted, lowercase)
	@Test
	void unquotedLowercaseCharset_decodesWithUtf8() throws IOException {
		stubResponse("text/plain; charset=utf-8", CAFE, StandardCharsets.UTF_8);
		assertThat(HttpClientResponses.toString(response)).isEqualTo(CAFE);
	}

	// 3. charset="UTF-8" (quoted)
	@Test
	void quotedCharset_decodesWithUtf8() throws IOException {
		stubResponse("text/plain; charset=\"UTF-8\"", CAFE, StandardCharsets.UTF_8);
		assertThat(HttpClientResponses.toString(response)).isEqualTo(CAFE);
	}

	// 4. CHARSET= (uppercase parameter name)
	@Test
	void uppercaseCharsetParam_decodesWithUtf8() throws IOException {
		stubResponse("text/plain; CHARSET=utf-8", CAFE, StandardCharsets.UTF_8);
		assertThat(HttpClientResponses.toString(response)).isEqualTo(CAFE);
	}

	// 5. No charset param → falls back to ISO-8859-1
	@Test
	void noCharsetParam_usesIso88591() throws IOException {
		stubResponse("text/plain", CAFE, StandardCharsets.ISO_8859_1);
		assertThat(HttpClientResponses.toString(response)).isEqualTo(CAFE);
	}

	// 6. Unrecognised charset → falls back to ISO-8859-1
	@Test
	void unrecognisedCharset_fallsBackToIso88591() throws IOException {
		stubResponse("text/plain; charset=BOGUS", CAFE, StandardCharsets.ISO_8859_1);
		assertThat(HttpClientResponses.toString(response)).isEqualTo(CAFE);
	}

	// 7. Explicit default UTF-8, no charset in header
	@Test
	void explicitDefaultUtf8_noCharsetInHeader_decodesWithUtf8() throws IOException {
		stubResponse("text/plain", CAFE, StandardCharsets.UTF_8);
		assertThat(HttpClientResponses.toString(response, StandardCharsets.UTF_8)).isEqualTo(CAFE);
	}

	// 8. Header charset overrides explicit default
	@Test
	void headerCharsetOverridesExplicitDefault() throws IOException {
		stubResponse("text/plain; charset=iso-8859-1", CAFE, StandardCharsets.ISO_8859_1);
		assertThat(HttpClientResponses.toString(response, StandardCharsets.UTF_8)).isEqualTo(CAFE);
	}
}
