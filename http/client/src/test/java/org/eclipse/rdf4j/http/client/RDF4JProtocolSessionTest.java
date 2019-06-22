/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link RDF4JProtocolSession}
 * 
 * @author Jeen Broekstra
 *
 */
public class RDF4JProtocolSessionTest {

	private RDF4JProtocolSession subject;
	private HttpClient client;

	@Before
	public void setUp() throws Exception {
		client = mock(HttpClient.class);
		HttpResponse response = mock(HttpResponse.class);

		when(client.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
		when(response.getEntity()).thenReturn(mock(HttpEntity.class));
		when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
		ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		subject = new RDF4JProtocolSession(client, executor);
		subject.setServerURL("http://example.org/");
	}

	@Test
	public void testCreateRepositoryExecutesPut() throws Exception {
		RepositoryConfig config = new RepositoryConfig("test");
		subject.createRepository(config);
		verify(client, times(1)).execute(any(HttpPut.class), any(HttpContext.class));
	}

}
