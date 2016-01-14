/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.repository.manager.SystemRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.runtime.RepositoryManagerFederator;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;

public class CreateServlet extends TransformationServlet {

	private RepositoryManagerFederator rmf;

	@Override
	public void init(final ServletConfig config)
		throws ServletException
	{
		super.init(config);
		this.rmf = new RepositoryManagerFederator(manager);
	}

	/**
	 * POST requests to this servlet come from the various specific create-* form
	 * submissions.
	 */
	@Override
	protected void doPost(final WorkbenchRequest req, final HttpServletResponse resp, final String xslPath)
		throws ServletException
	{
		try {
			resp.sendRedirect("../" + createRepositoryConfig(req) + "/summary");
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * GET requests to this servlet come from the Workbench side bar or from
	 * create.xsl form submissions.
	 * 
	 * @throws RepositoryException
	 * @throws QueryResultHandlerException
	 */
	@Override
	protected void service(final WorkbenchRequest req, final HttpServletResponse resp, final String xslPath)
		throws IOException, RepositoryException, QueryResultHandlerException
	{
		final TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
		boolean federate;
		if (req.isParameterPresent("type")) {
			final String type = req.getTypeParameter();
			federate = "federate".equals(type);
			builder.transform(xslPath, "create-" + type + ".xsl");
		}
		else {
			federate = false;
			builder.transform(xslPath, "create.xsl");
		}
		builder.start(federate ? new String[] { "id", "description", "location" } : new String[] {});
		builder.link(Arrays.asList(INFO));
		if (federate) {
			for (RepositoryInfo info : manager.getAllRepositoryInfos()) {
				String identity = info.getId();
				if (!SystemRepository.ID.equals(identity)) {
					builder.result(identity, info.getDescription(), info.getLocation());
				}
			}
		}
		builder.end();
	}

	private String createRepositoryConfig(final WorkbenchRequest req)
		throws IOException, OpenRDFException
	{
		String type = req.getTypeParameter();
		String newID;
		if ("federate".equals(type)) {
			newID = req.getParameter("Local repository ID");
			rmf.addFed(newID, req.getParameter("Repository title"),
					Arrays.asList(req.getParameterValues("memberID")),
					Boolean.parseBoolean(req.getParameter("readonly")),
					Boolean.parseBoolean(req.getParameter("distinct")));
		}
		else {
			newID = updateRepositoryConfig(getConfigTemplate(type).render(req.getSingleParameterMap())).getID();
		}
		return newID;
	}

	private RepositoryConfig updateRepositoryConfig(final String configString)
		throws IOException, OpenRDFException
	{
		final Repository systemRepo = manager.getSystemRepository();
		final Model graph = new LinkedHashModel();
		final RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, systemRepo.getValueFactory());
		rdfParser.setRDFHandler(new StatementCollector(graph));
		rdfParser.parse(new StringReader(configString), RepositoryConfigSchema.NAMESPACE);

		Resource res = Models.subject(
				graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY)).orElseThrow(
						() -> new RepositoryException("could not find instance of Repository class in config"));
		final RepositoryConfig repConfig = RepositoryConfig.create(graph, res);
		repConfig.validate();
		RepositoryConfigUtil.updateRepositoryConfigs(systemRepo, repConfig);
		return repConfig;
	}

	private ConfigTemplate getConfigTemplate(final String type)
		throws IOException
	{
		final InputStream ttlInput = RepositoryConfig.class.getResourceAsStream(type + ".ttl");
		try {
			final String template = IOUtil.readString(new InputStreamReader(ttlInput, "UTF-8"));
			return new ConfigTemplate(template);
		}
		finally {
			ttlInput.close();
		}
	}
}
