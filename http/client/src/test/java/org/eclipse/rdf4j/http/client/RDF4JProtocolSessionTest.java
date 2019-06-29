/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link RDF4JProtocolSession}
 * 
 * @author Jeen Broekstra
 */
public class RDF4JProtocolSessionTest {

	private RDF4JProtocolSession subject;
	private HttpClient httpclient;

	private String testHeader = "X-testing-header";
	private String testValue = "foobar";
	private HttpResponse response;

	@Before
	public void setUp() throws Exception {

		httpclient = mock(HttpClient.class);
		response = mock(HttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);

		when(httpclient.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
		when(response.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(200);

		subject = new RDF4JProtocolSession(httpclient, mock(ScheduledExecutorService.class));
		subject.setRepository("http://localhost:1234/rdf4j-server/repositories/test");

		HashMap<String, String> additionalHeaders = new HashMap<>();
		additionalHeaders.put(testHeader, testValue);
		subject.setAdditionalHttpHeaders(additionalHeaders);
	}

	@Test
	public void testSize() throws Exception {
		when(response.getEntity()).thenReturn(new StringEntity("8"));

		assertThat(subject.size()).isEqualTo(8);
		verifyHeaders();
	}

	private void verifyHeaders() throws Exception {
		verify(httpclient).execute(argThat(r -> r.getFirstHeader(testHeader).getValue().equals(testValue)),
				any(HttpContext.class));
	}
}
