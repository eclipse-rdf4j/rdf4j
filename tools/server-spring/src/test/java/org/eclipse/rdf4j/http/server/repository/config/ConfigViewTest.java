/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositorySchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ConfigViewTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testRender() throws Exception {

		ConfigView configView = ConfigView.getInstance();

		Model configData = new LinkedHashModelFactory().createEmptyModel();
		configData.add(RDF.ALT, RDF.TYPE, RDFS.CLASS);

		Map<Object, Object> map = new LinkedHashMap<>();
		map.put(ConfigView.HEADERS_ONLY, false);
		map.put(ConfigView.CONFIG_DATA_KEY, configData);
		map.put(ConfigView.FORMAT_KEY, RDFFormat.NTRIPLES);

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(HttpMethod.GET.name());
		request.addHeader("Accept", RDFFormat.NTRIPLES.getDefaultMIMEType());

		MockHttpServletResponse response = new MockHttpServletResponse();

		configView.render(map, request, response);

		String ntriplesData = response.getContentAsString();
		Model renderedData = Rio.parse(new StringReader(ntriplesData), "", RDFFormat.NTRIPLES);
		assertThat(renderedData).isNotEmpty();
	}

	@Test
	public void testRenderObfuscation() throws Exception {

		ConfigView configView = ConfigView.getInstance();

		HTTPRepositoryConfig implConfig = new HTTPRepositoryConfig();
		implConfig.setURL("http://localhost:8080/rdf4j-server");
		implConfig.setUsername("admin");
		implConfig.setPassword("password");
		RepositoryConfig config = new RepositoryConfig("test", implConfig);

		Model configData = new DynamicModelFactory().createEmptyModel();

		Resource res = SimpleValueFactory.getInstance().createBNode();
		config.export(configData, res);

		Map<Object, Object> map = new LinkedHashMap<>();
		map.put(ConfigView.HEADERS_ONLY, false);
		map.put(ConfigView.CONFIG_DATA_KEY, configData);
		map.put(ConfigView.FORMAT_KEY, RDFFormat.NTRIPLES);

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(HttpMethod.GET.name());
		request.addHeader("Accept", RDFFormat.NTRIPLES.getDefaultMIMEType());

		MockHttpServletResponse response = new MockHttpServletResponse();

		configView.render(map, request, response);

		String ntriplesData = response.getContentAsString();
		Model renderedData = Rio.parse(new StringReader(ntriplesData), "", RDFFormat.NTRIPLES);
		assertThat(renderedData).isNotEmpty();
		assertThat(renderedData.getStatements(null, HTTPRepositorySchema.PASSWORD, null)).isNotEmpty();
		renderedData.getStatements(null, HTTPRepositorySchema.PASSWORD, null)
				.forEach(pwst -> assertThat(pwst.getObject())
						.isEqualTo(SimpleValueFactory.getInstance().createLiteral("****")));
	}

}
