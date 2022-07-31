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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddServlet extends TransformationServlet {

	private static final String URL = "url";

	private final Logger logger = LoggerFactory.getLogger(AddServlet.class);

	@Override
	protected void doPost(WorkbenchRequest req, HttpServletResponse resp, String xslPath)
			throws IOException, RepositoryException, FileUploadException, QueryResultHandlerException {
		try {
			String baseURI = req.getParameter("baseURI");
			String contentType = req.getParameter("Content-Type");
			if (req.isParameterPresent(CONTEXT)) {
				Resource context = req.getResource(CONTEXT);
				if (req.isParameterPresent(URL)) {
					add(req.getUrl(URL), baseURI, contentType, context);
				} else {
					add(req.getContentParameter(), baseURI, contentType, req.getContentFileName(), context);
				}
			} else {
				if (req.isParameterPresent(URL)) {
					add(req.getUrl(URL), baseURI, contentType);
				} else {
					add(req.getContentParameter(), baseURI, contentType, req.getContentFileName());
				}
			}
			resp.sendRedirect("summary");
		} catch (BadRequestException exc) {
			logger.warn(exc.toString(), exc);
			TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
			builder.transform(xslPath, "add.xsl");
			builder.start("error-message", "baseURI", CONTEXT, "Content-Type");
			builder.link(List.of(INFO));
			String baseURI = req.getParameter("baseURI");
			String context = req.getParameter(CONTEXT);
			String contentType = req.getParameter("Content-Type");
			builder.result(exc.getMessage(), baseURI, context, contentType);
			builder.end();
		}
	}

	private void add(InputStream stream, String baseURI, String contentType, String contentFileName,
			Resource... context) throws BadRequestException, RepositoryException, IOException {
		if (contentType == null) {
			throw new BadRequestException("No Content-Type provided");
		}

		RDFFormat format;
		if ("autodetect".equals(contentType)) {
			format = Rio.getParserFormatForFileName(contentFileName)
					.orElseThrow(() -> new BadRequestException(
							"Could not automatically determine Content-Type for content: " + contentFileName));
		} else {
			format = Rio.getParserFormatForMIMEType(contentType)
					.orElseThrow(() -> new BadRequestException("Unknown Content-Type: " + contentType));
		}

		try (RepositoryConnection con = repository.getConnection()) {
			con.add(stream, baseURI, format, context);
		} catch (RDFParseException | IllegalArgumentException exc) {
			throw new BadRequestException(exc.getMessage(), exc);
		}
	}

	private void add(URL url, String baseURI, String contentType, Resource... context)
			throws BadRequestException, RepositoryException, IOException {
		if (contentType == null) {
			throw new BadRequestException("No Content-Type provided");
		}

		RDFFormat format;
		if ("autodetect".equals(contentType)) {
			format = Rio.getParserFormatForFileName(url.getFile())
					.orElseThrow(() -> new BadRequestException(
							"Could not automatically determine Content-Type for content: " + url.getFile()));
		} else {
			format = Rio.getParserFormatForMIMEType(contentType)
					.orElseThrow(() -> new BadRequestException("Unknown Content-Type: " + contentType));
		}

		try {
			try (RepositoryConnection con = repository.getConnection()) {
				con.add(url, baseURI, format, context);
			}
		} catch (RDFParseException | MalformedURLException | IllegalArgumentException exc) {
			throw new BadRequestException(exc.getMessage(), exc);
		}
	}

	@Override
	public void service(TupleResultBuilder builder, String xslPath)
			throws RepositoryException, QueryResultHandlerException {
		// TupleResultBuilder builder = getTupleResultBuilder(req, resp);
		builder.transform(xslPath, "add.xsl");
		builder.start();
		builder.link(List.of(INFO));
		builder.end();
	}

}
