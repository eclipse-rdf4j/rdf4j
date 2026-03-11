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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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

	private static final String[] FEDERATE_RESULT_VARS = { "id", "description", "location" };

	private static final String[] TYPE_PICKER_RESULT_VARS = { "type", "label" };

	private static final String[] TEMPLATE_RESULT_VARS = { "templateType", "templateLabel", "fieldId",
			"fieldProperty", "fieldRole", "fieldName", "fieldType", "value", "selected", "size", "rows", "cols",
			"placeholder" };

	private static final int FEDERATE_ORDER = 170;

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
		boolean typedTemplate;
		if (req.isParameterPresent("type")) {
			final String type = req.getTypeParameter();
			federate = FEDERATE.equals(type);
			typedTemplate = !federate;
			builder.transform(xslPath, federate ? "create-federate.xsl" : "create-template.xsl");
		} else {
			federate = false;
			typedTemplate = false;
			builder.transform(xslPath, "create.xsl");
		}
		builder.start(federate ? FEDERATE_RESULT_VARS : typedTemplate ? TEMPLATE_RESULT_VARS : TYPE_PICKER_RESULT_VARS);
		builder.link(List.of(INFO));
		if (federate) {
			for (RepositoryInfo info : manager.getAllRepositoryInfos()) {
				String identity = info.getId();
				builder.result(identity, info.getDescription(), info.getLocation());
			}
		} else if (typedTemplate) {
			writeTemplateFields(getCreateTemplate(req.getTypeParameter()), builder);
		} else {
			writeTemplateCatalog(builder);
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
			CreateTemplateConfig template = getCreateTemplate(type);
			newID = updateRepositoryConfig(template.render(getTemplateValues(req, template))).getID();
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

	static List<CreateTemplateConfig> getCreateTemplates() throws IOException {
		return CreateTemplateConfig.loadBuiltin();
	}

	static CreateTemplateConfig getCreateTemplate(String type) throws IOException {
		return CreateTemplateConfig.load(type);
	}

	private void writeTemplateCatalog(TupleResultBuilder builder) throws IOException, QueryResultHandlerException {
		List<TemplateOption> options = new java.util.ArrayList<>();
		for (CreateTemplateConfig template : getCreateTemplates()) {
			options.add(new TemplateOption(template.getType(), template.getLabel(), template.getOrder()));
		}
		options.add(new TemplateOption(FEDERATE, "Federation", FEDERATE_ORDER));
		options.sort(Comparator.comparingInt(TemplateOption::getOrder)
				.thenComparing(TemplateOption::getLabel)
				.thenComparing(TemplateOption::getType));

		for (TemplateOption option : options) {
			builder.result(option.getType(), option.getLabel());
		}
	}

	private void writeTemplateFields(CreateTemplateConfig template, TupleResultBuilder builder)
			throws QueryResultHandlerException {
		for (CreateTemplateConfig.Field field : template.getFields()) {
			if (field.getControl() == CreateTemplateConfig.FieldControl.SELECT
					|| field.getControl() == CreateTemplateConfig.FieldControl.RADIO) {
				for (String value : field.getValues()) {
					writeTemplateField(builder, template, field, value, value.equals(field.getDefaultValue()));
				}
			} else {
				writeTemplateField(builder, template, field, field.getDefaultValue(), true);
			}
		}
	}

	private void writeTemplateField(TupleResultBuilder builder, CreateTemplateConfig template,
			CreateTemplateConfig.Field field, String value, boolean selected) throws QueryResultHandlerException {
		builder.result(template.getType(), template.getLabel(), field.getId(), field.getProperty(), field.getRole(),
				field.getName(), field.getControl().getXslValue(), value, String.valueOf(selected),
				String.valueOf(field.getSize()), String.valueOf(field.getRows()), String.valueOf(field.getCols()),
				field.getPlaceholder());
	}

	private Map<String, String> getTemplateValues(WorkbenchRequest req, CreateTemplateConfig template) {
		Map<String, String> values = new LinkedHashMap<>();
		for (CreateTemplateConfig.Field field : template.getFields()) {
			String value = req.getParameter(field.getName());
			if (value != null) {
				values.put(field.getName(), value);
			}
		}
		return values;
	}

	private static final class TemplateOption {
		private final String type;
		private final String label;
		private final int order;

		private TemplateOption(String type, String label, int order) {
			this.type = type;
			this.label = label;
			this.order = order;
		}

		private String getType() {
			return type;
		}

		private String getLabel() {
			return label;
		}

		private int getOrder() {
			return order;
		}
	}
}
