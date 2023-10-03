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
package org.eclipse.rdf4j.http.server.repository.statements;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.springframework.web.servlet.View;

/**
 * View used to export a ValidationException.
 *
 * @author Håvard Ottestad
 */
@InternalUseOnly
public class ValidationExceptionView implements View {

	public static final String FACTORY_KEY = "factory";

	public static final String VALIDATION_EXCEPTION = "validationException";

	private static final ValidationExceptionView INSTANCE = new ValidationExceptionView();

	public static ValidationExceptionView getInstance() {
		return INSTANCE;
	}

	private ValidationExceptionView() {
	}

	@Override
	public String getContentType() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		RDFWriterFactory rdfWriterFactory = (RDFWriterFactory) model.get(FACTORY_KEY);

		RDFFormat rdfFormat = rdfWriterFactory.getRDFFormat();

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			RDFWriter rdfWriter = rdfWriterFactory.getWriter(baos);

			ValidationException validationException = (ValidationException) model.get(VALIDATION_EXCEPTION);

			Model validationReportModel = validationException.validationReportAsModel();

			rdfWriter.startRDF();
			for (Namespace namespace : validationReportModel.getNamespaces()) {
				rdfWriter.handleNamespace(namespace.getPrefix(), namespace.getName());
			}
			for (Statement statement : validationReportModel) {
				rdfWriter.handleStatement(statement);
			}
			rdfWriter.endRDF();

			try (OutputStream out = response.getOutputStream()) {
				response.setStatus((int) model.get(SimpleResponseView.SC_KEY));

				String mimeType = rdfFormat.getDefaultMIMEType();
				if (rdfFormat.hasCharset()) {
					Charset charset = rdfFormat.getCharset();
					mimeType += "; charset=" + charset.name();
				}

				assert mimeType.startsWith("application/");
				response.setContentType("application/shacl-validation-report+" + mimeType.replace("application/", ""));

				out.write(baos.toByteArray());
			}
		}
	}

}
