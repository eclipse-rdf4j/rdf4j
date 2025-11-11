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

import static jakarta.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.View;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Streams statements as RDF in the format requested by the client.
 *
 * @author Herko ter Horst
 */
public class ExportStatementsView implements View {

	private static final Logger logger = LoggerFactory.getLogger(ExportStatementsView.class);

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
	public static int MAX_NUMBER_OF_STATEMENTS_WHEN_TESTING_FOR_POSSIBLE_EXCEPTIONS;

	static {
		int max = 1024; // default value
		String maxStatements = System.getProperty(
				"org.eclipse.rdf4j.http.server.repository.statements.ExportStatementsView.MAX_NUMBER_OF_STATEMENTS_WHEN_TESTING_FOR_POSSIBLE_EXCEPTIONS");
		if (maxStatements != null) {
			try {
				int userMax = Integer.parseInt(maxStatements);
				if (userMax >= -1) {
					max = userMax;
				} else {
					logger.warn(
							"Invalid value for MAX_NUMBER_OF_STATEMENTS_WHEN_TESTING_FOR_POSSIBLE_EXCEPTIONS: {}, must be >= -1, using default value of {}.",
							maxStatements, max);
				}
			} catch (NumberFormatException e) {
				logger.warn("Invalid value for MAX_NUMBER_OF_STATEMENTS_WHEN_TESTING_FOR_POSSIBLE_EXCEPTIONS: "
						+ maxStatements, e);
			}
		}
		MAX_NUMBER_OF_STATEMENTS_WHEN_TESTING_FOR_POSSIBLE_EXCEPTIONS = max;
	}

	public static ExportStatementsView getInstance() {
		return INSTANCE;
	}

	private ExportStatementsView() {
	}

	@Override
	public String getContentType() {
		// Spring ignores this for View implementations; we set it in render().
		return null;
	}

	@Override
	public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		response.setBufferSize(1024 * 1024); // 1MB

		Resource subj = (Resource) Objects.requireNonNull(model, "model should not be null").get(SUBJECT_KEY);
		IRI pred = (IRI) model.get(PREDICATE_KEY);
		Value obj = (Value) model.get(OBJECT_KEY);
		Resource[] contexts = (Resource[]) model.get(CONTEXTS_KEY);
		boolean useInferencing = Boolean.TRUE.equals(model.get(USE_INFERENCING_KEY));
		boolean headersOnly = Boolean.TRUE.equals(model.get(HEADERS_ONLY));

		RDFWriterFactory factory = (RDFWriterFactory) model.get(FACTORY_KEY);
		RDFFormat rdfFormat = factory.getRDFFormat();

		attemptToDetectExceptions(request, factory, headersOnly, subj, pred, obj, useInferencing, contexts);

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

		if (headersOnly) {
			response.setContentLength(0);
			response.flushBuffer();
			return;
		}

		try (OutputStream out = response.getOutputStream()) {
			RDFWriter writer = factory.getWriter(out);
			try (RepositoryConnection conn = RepositoryInterceptor.getRepositoryConnection(request)) {
				conn.exportStatements(subj, pred, obj, useInferencing, writer, contexts);
				out.flush();
				response.flushBuffer();
			} catch (RDFHandlerException e) {
				var serverHTTPException = new ServerHTTPException("Serialization error: " + e.getMessage(), e);
				if (!response.isCommitted()) {
					response.reset();
				}
				throw serverHTTPException;
			} catch (RepositoryException e) {
				var serverHTTPException = new ServerHTTPException("Repository error: " + e.getMessage(), e);
				if (!response.isCommitted()) {
					response.reset();
				}
				throw serverHTTPException;
			} catch (Throwable e) {
				if (!response.isCommitted()) {
					response.reset();
				}
				throw e;
			}

		}

	}

	private static void attemptToDetectExceptions(HttpServletRequest request, RDFWriterFactory rdfWriterFactory,
			boolean headersOnly, Resource subj, IRI pred, Value obj, boolean useInferencing, Resource[] contexts)
			throws IOException, ServerHTTPException {
		if (MAX_NUMBER_OF_STATEMENTS_WHEN_TESTING_FOR_POSSIBLE_EXCEPTIONS == 0) {
			return;
		}

		try (OutputStream out = OutputStream.nullOutputStream()) {
			RDFHandler rdfWriter = new LimitedSizeRDFHandler(rdfWriterFactory.getWriter(out),
					MAX_NUMBER_OF_STATEMENTS_WHEN_TESTING_FOR_POSSIBLE_EXCEPTIONS);
			if (!headersOnly) {
				try (RepositoryConnection conn = RepositoryInterceptor.getRepositoryConnection(request)) {
					conn.exportStatements(subj, pred, obj, useInferencing, rdfWriter, contexts);
				} catch (RDFHandlerException e) {
					throw new ServerHTTPException("Serialization error: " + e.getMessage(), e);
				} catch (RepositoryException e) {
					throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
				} catch (ClientHTTPException e) {
					throw new ServerHTTPException("Client error: " + e.getMessage(), e);
				} catch (LimitedSizeReachedException ignored) {
				}
			}
		}
	}

	private static class LimitedSizeRDFHandler implements RDFHandler {

		private final RDFHandler delegate;
		private final long maxSize;
		private long currentSize = 0;

		public LimitedSizeRDFHandler(RDFHandler delegate, long maxSize) {
			this.delegate = delegate;
			this.maxSize = maxSize;
		}

		@Override
		public void startRDF() throws RDFHandlerException {
			delegate.startRDF();
		}

		@Override
		public void endRDF() throws RDFHandlerException {
			delegate.endRDF();
		}

		@Override
		public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
			delegate.handleNamespace(prefix, uri);
			incrementCurrentSize();
		}

		@Override
		public void handleStatement(Statement st) throws RDFHandlerException {
			delegate.handleStatement(st);
			incrementCurrentSize();
		}

		@Override
		public void handleComment(String comment) throws RDFHandlerException {
			delegate.handleComment(comment);
			incrementCurrentSize();
		}

		private void incrementCurrentSize() {
			currentSize++;
			if (maxSize >= 0 && currentSize > maxSize) {
				endRDF();
				logger.trace(
						"Limited size reached, throwing LimitedSizeReachedException to signal that we are done testing the export of statements for exceptions.");
				throw new LimitedSizeReachedException();
			}
		}
	}

	private static class LimitedSizeReachedException extends RuntimeException {
		@Override
		public Throwable fillInStackTrace() {
			// Do not fill in the stack trace to avoid performance overhead
			return this;
		}
	}

}
