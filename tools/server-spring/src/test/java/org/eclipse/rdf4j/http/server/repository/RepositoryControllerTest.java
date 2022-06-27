/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RepositoryControllerTest {

	final String repositoryId = "test-repo";
	final RepositoryController controller = new RepositoryController();

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private RepositoryManager manager;

	@Before
	public void setUp() throws Exception {
		request = new MockHttpServletRequest();
		request.setAttribute("repositoryID", repositoryId);
		response = new MockHttpServletResponse();

		manager = mock(RepositoryManager.class);
		controller.setRepositoryManager(manager);
	}

	@Test
	public void putOnNewRepoSucceeds() throws Exception {
		request.setMethod(HttpMethod.PUT.name());
		request.setContentType(RDFFormat.NTRIPLES.getDefaultMIMEType());
		request.setContent(
				("_:node1 <" + RepositoryConfigSchema.REPOSITORYID + "> \"" + repositoryId + "\" .")
						.getBytes(StandardCharsets.UTF_8));

		when(manager.hasRepositoryConfig(repositoryId)).thenReturn(false);

		ArgumentCaptor<RepositoryConfig> config = ArgumentCaptor.forClass(RepositoryConfig.class);

		controller.handleRequest(request, response);

		verify(manager).addRepositoryConfig(config.capture());
		assertThat(config.getValue().getID()).isEqualTo(repositoryId);
	}

	@Test
	public void putOnExistingRepoFails() throws Exception {
		request.setMethod(HttpMethod.PUT.name());
		request.setContentType(RDFFormat.NTRIPLES.getDefaultMIMEType());
		request.setContent(
				("_:node1 <" + RepositoryConfigSchema.REPOSITORYID + "> \"" + repositoryId + "\" .")
						.getBytes(StandardCharsets.UTF_8));
		when(manager.hasRepositoryConfig(repositoryId)).thenReturn(true);

		try {
			controller.handleRequest(request, response);
			fail("expected exception");
		} catch (ClientHTTPException e) {
			assertThat(e.getStatusCode()).isEqualTo(409);
		}
	}

	@Test
	public void put_errorHandling_MissingConfig() throws Exception {
		request.setMethod(HttpMethod.PUT.name());
		request.setContentType(RDFFormat.NTRIPLES.getDefaultMIMEType());
		request.setContent(("").getBytes(StandardCharsets.UTF_8));

		try {
			controller.handleRequest(request, response);
			fail("expected exception");
		} catch (ClientHTTPException e) {
			assertThat(e.getStatusCode()).isEqualTo(400);
			assertThat(e.getMessage()).startsWith("MALFORMED DATA: Supplied repository configuration is invalid:");
		}
	}

	@Test
	public void put_errorHandling_InvalidConfig() throws Exception {
		request.setMethod(HttpMethod.PUT.name());
		request.setContentType(RDFFormat.NTRIPLES.getDefaultMIMEType());
		request.setContent(("_:node1 <" + RepositoryConfigSchema.REPOSITORYID + "> \"" + repositoryId + "\" .")
				.getBytes(StandardCharsets.UTF_8));
		doThrow(new RepositoryConfigException("stub invalid")).when(manager).addRepositoryConfig(Mockito.any());

		try {
			controller.handleRequest(request, response);
			fail("expected exception");
		} catch (ClientHTTPException e) {
			assertThat(e.getStatusCode()).isEqualTo(400);
			assertThat(e.getMessage())
					.startsWith("MALFORMED DATA: Supplied repository configuration is invalid: stub invalid");
		}
	}
}
