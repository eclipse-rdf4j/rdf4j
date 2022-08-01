/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
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

	private final ModelFactory modelFactory = new LinkedHashModelFactory();

	public ConfigController() throws ApplicationContextException {
		setSupportedMethods(new String[] { METHOD_GET, METHOD_POST, METHOD_HEAD });
	}

	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		switch (request.getMethod()) {
		case METHOD_GET:
		case METHOD_HEAD:
			return handleQuery(request, response);
		case METHOD_POST:
			return handleUpdate(request, response);
		default:
			throw new ClientHTTPException("unrecognized method " + request.getMethod());
		}
	}

	private ModelAndView handleQuery(HttpServletRequest request, HttpServletResponse response)
			throws ClientHTTPException {

		RDFWriterFactory rdfWriterFactory = ProtocolUtil.getAcceptableService(request, response,
				RDFWriterRegistry.getInstance());
		String repId = RepositoryInterceptor.getRepositoryID(request);
		RepositoryConfig repositoryConfig = repositoryManager.getRepositoryConfig(repId);

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

	private ModelAndView handleUpdate(HttpServletRequest request, HttpServletResponse response)
			throws RDFParseException, UnsupportedRDFormatException, IOException, HTTPException {
		String repId = RepositoryInterceptor.getRepositoryID(request);
		Model model = Rio.parse(request.getInputStream(), "",
				Rio.getParserFormatForMIMEType(request.getContentType())
						.orElseThrow(() -> new HTTPException(HttpStatus.SC_BAD_REQUEST,
								"unrecognized content type " + request.getContentType())));
		RepositoryConfig config = RepositoryConfigUtil.getRepositoryConfig(model, repId);
		repositoryManager.addRepositoryConfig(config);
		return new ModelAndView(EmptySuccessView.getInstance());

	}

}
