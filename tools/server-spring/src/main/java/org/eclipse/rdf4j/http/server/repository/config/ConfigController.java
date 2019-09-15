/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.config;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests related to repository configuration.
 * 
 * @author Jeen Broekstra
 */
public class ConfigController extends AbstractController {

	private RepositoryManager repositoryManager;

	private ModelFactory modelFactory = new LinkedHashModelFactory();

	public ConfigController() throws ApplicationContextException {
		setSupportedMethods(new String[] { METHOD_GET, METHOD_HEAD });
	}

	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		RDFWriterFactory rdfWriterFactory = ProtocolUtil.getAcceptableService(request, response,
				RDFWriterRegistry.getInstance());

		RepositoryConfig repositoryConfig = repositoryManager
				.getRepositoryConfig(RepositoryInterceptor.getRepositoryID(request));

		Model configData = modelFactory.createEmptyModel();
		String baseURI = request.getRequestURL().toString();
		Resource ctx = SimpleValueFactory.getInstance().createIRI(baseURI + "#" + repositoryConfig.getID());

		repositoryConfig.export(configData, ctx);
		Map<String, Object> model = new HashMap<>();
		model.put(ConfigView.FORMAT_KEY, rdfWriterFactory.getRDFFormat());
		model.put(ConfigView.CONFIG_DATA_KEY, configData);
		model.put(ConfigView.HEADERS_ONLY, METHOD_HEAD.equals(request.getMethod()));
		return new ModelAndView(ConfigView.getInstance(), model);
	}

}
