/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.federated.repository.FedXRepositoryConfigBuilder;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;

public class CreateServlet extends TransformationServlet {

	private static final String FEDERATE = "federate";

	private static final String LMDB = "lmdb";

	private static final String[] FEDERATE_RESULT_VARS = { "id", "description", "location" };

	private static final String[] LMDB_RESULT_VARS = { "fieldId", "fieldName", "fieldType", "value", "selected" };

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
	}

	/**
	 * POST requests to this servlet come from the various specific create-* form submissions.
	 */
	@Override
	protected void doPost(final WorkbenchRequest req, final HttpServletResponse resp, final String xslPath)
			throws ServletException {
		try {
			resp.sendRedirect("../" + createRepositoryConfig(req) + "/summary");
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * GET requests to this servlet come from the Workbench side bar or from create.xsl form submissions.
	 *
	 * @throws RepositoryException
	 * @throws QueryResultHandlerException
	 */
	@Override
	protected void service(final WorkbenchRequest req, final HttpServletResponse resp, final String xslPath)
			throws IOException, RepositoryException, QueryResultHandlerException {
		final TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
		boolean federate;
		boolean lmdb;
		if (req.isParameterPresent("type")) {
			final String type = req.getTypeParameter();
			federate = FEDERATE.equals(type);
			lmdb = LMDB.equals(type);
			builder.transform(xslPath, "create-" + type + ".xsl");
		} else {
			federate = false;
			lmdb = false;
			builder.transform(xslPath, "create.xsl");
		}
		builder.start(federate ? FEDERATE_RESULT_VARS : lmdb ? LMDB_RESULT_VARS : new String[] {});
		builder.link(List.of(INFO));
		if (federate) {
			for (RepositoryInfo info : manager.getAllRepositoryInfos()) {
				String identity = info.getId();
				builder.result(identity, info.getDescription(), info.getLocation());
			}
		} else if (lmdb) {
			writeLmdbConfigFields(builder);
		}
		builder.end();
	}

	private String createRepositoryConfig(final WorkbenchRequest req) throws IOException, RDF4JException {
		String type = req.getTypeParameter();
		String newID;
		if (FEDERATE.equals(type)) {
			newID = req.getParameter("Local repository ID");
			addFederated(newID, req.getParameter("Repository title"),
					Arrays.asList(req.getParameterValues("memberID")));
		} else {
			newID = updateRepositoryConfig(getConfigTemplate(type).render(req.getSingleParameterMap())).getID();
		}
		return newID;
	}

	RepositoryConfig updateRepositoryConfig(final String configString) throws IOException, RDF4JException {
		final Model graph = new LinkedHashModel();
		final RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, SimpleValueFactory.getInstance());
		rdfParser.setRDFHandler(new StatementCollector(graph));
		rdfParser.parse(new StringReader(configString), CONFIG.NAMESPACE);

		Resource res = Models.subject(graph.getStatements(null, RDF.TYPE, CONFIG.Rep.Repository))
				.orElseGet(() -> Models.subject(graph.getStatements(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY))
						.orElseThrow(() -> new RepositoryException(
								"could not find instance of Repository class in config")));
		final RepositoryConfig repConfig = RepositoryConfig.create(graph, res);
		repConfig.validate();
		manager.addRepositoryConfig(repConfig);
		return repConfig;
	}

	private void addFederated(String repositoryId, String repositoryTitle, List<String> memberIds) {

		RepositoryConfig repoConfig = FedXRepositoryConfigBuilder.create()
				.withResolvableEndpoint(memberIds)
				.build(repositoryId, repositoryTitle);
		repoConfig.validate();
		manager.addRepositoryConfig(repoConfig);
	}

	static ConfigTemplate getConfigTemplate(final String type) throws IOException {
		try (InputStream ttlInput = RepositoryConfig.class.getResourceAsStream(type + ".ttl")) {
			final String template = IOUtil.readString(new InputStreamReader(ttlInput, StandardCharsets.UTF_8));
			return new ConfigTemplate(template);
		}
	}

	private void writeLmdbConfigFields(TupleResultBuilder builder) throws IOException, QueryResultHandlerException {
		for (Map.Entry<String, List<String>> entry : getConfigTemplate(LMDB).getVariableMap().entrySet()) {
			String fieldName = entry.getKey();
			List<String> values = entry.getValue();
			String fieldId = toLmdbFieldId(fieldName);
			String fieldType = values.size() > 1 ? "select" : "text";
			String defaultValue = values.isEmpty() ? "" : values.get(0);

			if ("select".equals(fieldType)) {
				for (String value : values) {
					builder.result(fieldId, fieldName, fieldType, value, String.valueOf(value.equals(defaultValue)));
				}
			} else {
				builder.result(fieldId, fieldName, fieldType, defaultValue, Boolean.TRUE.toString());
			}
		}
	}

	private static String toLmdbFieldId(String fieldName) {
		switch (fieldName) {
		case "Repository ID":
			return "id";
		case "Repository title":
			return "title";
		case "Triple indexes":
			return "indexes";
		case "Query Iteration Cache sync threshold":
			return "iterationCacheSyncThreshold";
		case "Query Evaluation Mode":
			return "queryEvalMode";
		default:
			String[] words = fieldName.replaceAll("[^A-Za-z0-9]+", " ").trim().split("\\s+");
			if (words.length == 0) {
				throw new IllegalArgumentException("Cannot derive LMDB field id from empty field name");
			}
			StringBuilder fieldId = new StringBuilder(words[0].toLowerCase(Locale.ENGLISH));
			for (int i = 1; i < words.length; i++) {
				String word = words[i];
				if (word.equals(word.toUpperCase(Locale.ENGLISH)) && word.length() <= 3) {
					fieldId.append(word);
				} else {
					fieldId.append(Character.toUpperCase(word.charAt(0)))
							.append(word.substring(1).toLowerCase(Locale.ENGLISH));
				}
			}
			return fieldId.toString();
		}
	}
}
