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

import static org.eclipse.rdf4j.rio.RDFWriterRegistry.getInstance;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.workbench.base.TupleServlet;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;

import jakarta.servlet.http.HttpServletResponse;

public class ExportServlet extends TupleServlet {

	public ExportServlet() {
		super("export.xsl", "subject", "predicate", "object", "context");
	}

	@Override
	public String[] getCookieNames() {
		return new String[] { ExploreServlet.LIMIT, "Accept" };
	}

	@Override
	protected void service(WorkbenchRequest req, HttpServletResponse resp, String xslPath) throws Exception {
		if (req.isParameterPresent("Accept")) {
			String accept = req.getParameter("Accept");
			RDFFormat format = Rio.getWriterFormatForMIMEType(accept).orElseThrow(Rio.unsupportedFormat(accept));
			Compression compression = Compression.from(req.getParameter("compression"));
			String ext = format.getDefaultFileExtension();
			String attachment = "attachment; filename=" + compression.filename(ext);
			resp.setContentType(compression.contentType(accept));
			resp.setHeader("Content-disposition", attachment);
			try (RepositoryConnection con = repository.getConnection()) {
				con.setParserConfig(NON_VERIFYING_PARSER_CONFIG);
				RDFWriterFactory factory = getInstance().get(format).orElseThrow(Rio.unsupportedFormat(format));
				if (compression == Compression.NONE && format.getCharset() != null) {
					resp.setCharacterEncoding(format.getCharset().name());
				}
				writeExport(con, factory, format, compression, resp.getOutputStream());
			}
		} else {
			super.service(req, resp, xslPath);
		}
	}

	private static void writeExport(RepositoryConnection connection, RDFWriterFactory factory, RDFFormat format,
			Compression compression, OutputStream output) throws Exception {
		switch (compression) {
		case NONE:
			connection.export(factory.getWriter(output));
			break;
		case GZIP:
			try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
				connection.export(factory.getWriter(gzip));
			}
			break;
		case ZIP:
			try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
				zip.putNextEntry(new ZipEntry("export." + format.getDefaultFileExtension()));
				connection.export(factory.getWriter(zip));
				zip.closeEntry();
			}
			break;
		default:
			throw new IllegalStateException("Unsupported export compression: " + compression);
		}
	}

	@Override
	protected void service(WorkbenchRequest req, HttpServletResponse resp, TupleResultBuilder builder,
			RepositoryConnection con) throws Exception {
		int requestedLimit = req.getInt(ExploreServlet.LIMIT);
		int limit = requestedLimit > 0 ? requestedLimit : ExploreServlet.LIMIT_DEFAULT;
		try (RepositoryResult<Statement> result = con.getStatements(null, null, null, false)) {
			for (int i = 0; result.hasNext() && i < limit; i++) {
				Statement st = result.next();
				builder.result(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			}
		}
	}

	private enum Compression {
		NONE("application/octet-stream"),
		GZIP("application/gzip"),
		ZIP("application/zip");

		private final String contentType;

		Compression(String contentType) {
			this.contentType = contentType;
		}

		static Compression from(String value) throws BadRequestException {
			if (value == null || value.isBlank()) {
				return NONE;
			}
			try {
				return valueOf(value.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				throw new BadRequestException("Unsupported export compression: " + value, exception);
			}
		}

		String contentType(String rdfContentType) {
			return this == NONE ? rdfContentType : contentType;
		}

		String filename(String rdfExtension) {
			return switch (this) {
			case NONE -> "export." + rdfExtension;
			case GZIP -> "export." + rdfExtension + ".gz";
			case ZIP -> "export.zip";
			};
		}
	}

}
