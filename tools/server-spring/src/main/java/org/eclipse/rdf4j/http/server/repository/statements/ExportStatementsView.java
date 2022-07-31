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

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.springframework.web.servlet.View;

/**
 * View used to export statements. Renders the statements as RDF using a serialization specified using a parameter or
 * Accept header.
 *
 * @author Herko ter Horst
 */
public class ExportStatementsView implements View {

	public static final String SUBJECT_KEY = "subject";

	public static final String PREDICATE_KEY = "predicate";

	public static final String OBJECT_KEY = "object";

	public static final String CONTEXTS_KEY = "contexts";

	public static final String USE_INFERENCING_KEY = "useInferencing";

	public static final String CONNECTION_KEY = "connection";

	public static final String TRANSACTION_ID_KEY = "transactionID";

	public static final String FACTORY_KEY = "factory";

	public static final String HEADERS_ONLY = "headersOnly";

	private static final ExportStatementsView INSTANCE = new ExportStatementsView();

	public static ExportStatementsView getInstance() {
		return INSTANCE;
	}

	private ExportStatementsView() {
	}

	@Override
	public String getContentType() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Resource subj = (Resource) model.get(SUBJECT_KEY);
		IRI pred = (IRI) model.get(PREDICATE_KEY);
		Value obj = (Value) model.get(OBJECT_KEY);
		Resource[] contexts = (Resource[]) model.get(CONTEXTS_KEY);
		boolean useInferencing = (Boolean) model.get(USE_INFERENCING_KEY);

		boolean headersOnly = (Boolean) model.get(HEADERS_ONLY);

		RDFWriterFactory rdfWriterFactory = (RDFWriterFactory) model.get(FACTORY_KEY);

		RDFFormat rdfFormat = rdfWriterFactory.getRDFFormat();

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			RDFWriter rdfWriter = rdfWriterFactory.getWriter(baos);
			if (!headersOnly) {
				try (RepositoryConnection conn = RepositoryInterceptor.getRepositoryConnection(request)) {
					conn.exportStatements(subj, pred, obj, useInferencing, rdfWriter, contexts);
				} catch (RDFHandlerException e) {
					throw new ServerHTTPException("Serialization error: " + e.getMessage(), e);
				} catch (RepositoryException e) {
					throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
				}
			}
			try (OutputStream out = response.getOutputStream()) {
				response.setStatus(SC_OK);

				String mimeType = rdfFormat.getDefaultMIMEType();
				if (rdfFormat.hasCharset()) {
					Charset charset = rdfFormat.getCharset();
					mimeType += "; charset=" + charset.name();
				}
				response.setContentType(mimeType);

				String filename = "statements";
				if (rdfFormat.getDefaultFileExtension() != null) {
					filename += "." + rdfFormat.getDefaultFileExtension();
				}
				response.setHeader("Content-Disposition", "attachment; filename=" + filename);
				out.write(baos.toByteArray());
			}
		}
	}

}
