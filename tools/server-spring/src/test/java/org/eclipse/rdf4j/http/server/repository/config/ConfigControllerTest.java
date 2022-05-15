/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class ConfigControllerTest {

	final String repositoryId = "test-config";
	final ConfigController controller = new ConfigController();

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
	public void getRequestRetrievesConfiguration() throws Exception {
		request.setMethod(HttpMethod.GET.name());
		request.addHeader("Accept", RDFFormat.NTRIPLES.getDefaultMIMEType());

		RepositoryConfig config = new RepositoryConfig(repositoryId, new SailRepositoryConfig(new MemoryStoreConfig()));
		when(manager.getRepositoryConfig(repositoryId)).thenReturn(config);

		ModelAndView result = controller.handleRequest(request, response);

		verify(manager).getRepositoryConfig(repositoryId);
		assertThat(result.getModel().containsKey(ConfigView.CONFIG_DATA_KEY));

		Model resultData = (Model) result.getModel().get(ConfigView.CONFIG_DATA_KEY);
		RepositoryConfig resultConfig = RepositoryConfigUtil.getRepositoryConfig(resultData, repositoryId);
		assertThat(resultConfig).isNotNull();
	}

	@Test
	public void postRequestModifiesConfiguration() throws Exception {
		request.setMethod(HttpMethod.POST.name());
		request.setContentType(RDFFormat.NTRIPLES.getDefaultMIMEType());
		request.setContent(
				("_:node1 <" + RepositoryConfigSchema.REPOSITORYID + "> \"" + repositoryId + "\" .")
						.getBytes(StandardCharsets.UTF_8));

		when(manager.hasRepositoryConfig(repositoryId)).thenReturn(true);

		ArgumentCaptor<RepositoryConfig> config = ArgumentCaptor.forClass(RepositoryConfig.class);

		controller.handleRequest(request, new MockHttpServletResponse());

		verify(manager).addRepositoryConfig(config.capture());
		assertThat(config.getValue().getID()).isEqualTo(repositoryId);
	}
}
