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
package org.eclipse.rdf4j.http.server.repository.graph;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.webapp.util.HttpServerUtil;
import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.http.server.repository.statements.ExportStatementsView;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests for manipulating the named graphs in a repository.
 *
 * @author Jeen Broekstra
 */
public class GraphController extends AbstractController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public GraphController() throws ApplicationContextException {
		setSupportedMethods(new String[] { METHOD_GET, METHOD_HEAD, METHOD_POST, "PUT", "DELETE" });
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		ModelAndView result;

		Repository repository = RepositoryInterceptor.getRepository(request);

		String reqMethod = request.getMethod();

		if (METHOD_GET.equals(reqMethod)) {
			logger.info("GET graph");
			result = getExportStatementsResult(repository, request, response);
			logger.info("GET graph request finished.");
		} else if (METHOD_HEAD.equals(reqMethod)) {
			logger.info("HEAD graph");
			result = getExportStatementsResult(repository, request, response);
			logger.info("HEAD graph request finished.");
		} else if (METHOD_POST.equals(reqMethod)) {
			logger.info("POST data to graph");
			result = getAddDataResult(repository, request, response, false);
			logger.info("POST data request finished.");
		} else if ("PUT".equals(reqMethod)) {
			logger.info("PUT data in graph");
			result = getAddDataResult(repository, request, response, true);
			logger.info("PUT data request finished.");
		} else if ("DELETE".equals(reqMethod)) {
			logger.info("DELETE data from graph");
			result = getDeleteDataResult(repository, request, response);
			logger.info("DELETE data request finished.");
		} else {
			throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
					"Method not allowed: " + reqMethod);
		}
		return result;
	}

	private IRI getGraphName(HttpServletRequest request, ValueFactory vf) throws ClientHTTPException {
		String requestURL = request.getRequestURL().toString();
		boolean isServiceRequest = requestURL.endsWith("/service");

		String queryString = request.getQueryString();

		if (isServiceRequest) {
			if (!"default".equalsIgnoreCase(queryString)) {
				IRI graph = ProtocolUtil.parseGraphParam(request, vf);
				if (graph == null) {
					throw new ClientHTTPException(HttpServletResponse.SC_BAD_REQUEST,
							"Named or default graph expected for indirect reference request.");
				}
				return graph;
			}
			return null;
		} else {
			if (queryString != null) {
				throw new ClientHTTPException(HttpServletResponse.SC_BAD_REQUEST,
						"No parameters epxected for direct reference request.");
			}
			return vf.createIRI(requestURL);
		}
	}

	/**
	 * Get all statements and export them as RDF.
	 *
	 * @return a model and view for exporting the statements.
	 */
	private ModelAndView getExportStatementsResult(Repository repository, HttpServletRequest request,
			HttpServletResponse response) throws ClientHTTPException {
		ProtocolUtil.logRequestParameters(request);

		ValueFactory vf = repository.getValueFactory();

		IRI graph = getGraphName(request, vf);

		RDFWriterFactory rdfWriterFactory = ProtocolUtil.getAcceptableService(request, response,
				RDFWriterRegistry.getInstance());

		Map<String, Object> model = new HashMap<>();

		model.put(ExportStatementsView.CONTEXTS_KEY, new Resource[] { graph });
		model.put(ExportStatementsView.FACTORY_KEY, rdfWriterFactory);
		model.put(ExportStatementsView.USE_INFERENCING_KEY, true);
		model.put(ExportStatementsView.HEADERS_ONLY, METHOD_HEAD.equals(request.getMethod()));
		return new ModelAndView(ExportStatementsView.getInstance(), model);
	}

	/**
	 * Upload data to the graph.
	 */
	private ModelAndView getAddDataResult(Repository repository, HttpServletRequest request,
			HttpServletResponse response, boolean replaceCurrent)
			throws IOException, ClientHTTPException, ServerHTTPException {
		ProtocolUtil.logRequestParameters(request);

		String mimeType = HttpServerUtil.getMIMEType(request.getContentType());

		RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(mimeType)
				.orElseThrow(
						() -> new ClientHTTPException(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported MIME type: " + mimeType));

		ValueFactory vf = repository.getValueFactory();
		final IRI graph = getGraphName(request, vf);

		IRI baseURI = ProtocolUtil.parseURIParam(request, Protocol.BASEURI_PARAM_NAME, vf);
		if (baseURI == null) {
			baseURI = graph != null ? graph : vf.createIRI("foo:bar");
			logger.info("no base URI specified, using '{}'", baseURI);
		}

		InputStream in = request.getInputStream();
		try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
			boolean localTransaction = !repositoryCon.isActive();

			if (localTransaction) {
				repositoryCon.begin();
			}

			if (replaceCurrent) {
				repositoryCon.clear(graph);
			}
			repositoryCon.add(in, baseURI.stringValue(), rdfFormat, graph);

			if (localTransaction) {
				repositoryCon.commit();
			}

			return new ModelAndView(EmptySuccessView.getInstance());
		} catch (UnsupportedRDFormatException e) {
			throw new ClientHTTPException(SC_UNSUPPORTED_MEDIA_TYPE,
					"No RDF parser available for format " + rdfFormat.getName());
		} catch (RDFParseException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_DATA, e.getMessage());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		} catch (IOException e) {
			throw new ServerHTTPException("Failed to read data: " + e.getMessage(), e);
		} catch (RepositoryException e) {
			throw new ServerHTTPException("Repository update error: " + e.getMessage(), e);
		}
	}

	/**
	 * Delete data from the graph.
	 */
	private ModelAndView getDeleteDataResult(Repository repository, HttpServletRequest request,
			HttpServletResponse response) throws ClientHTTPException, ServerHTTPException {
		ProtocolUtil.logRequestParameters(request);

		ValueFactory vf = repository.getValueFactory();

		IRI graph = getGraphName(request, vf);

		try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
			repositoryCon.clear(graph);

			return new ModelAndView(EmptySuccessView.getInstance());
		} catch (RepositoryException e) {
			throw new ServerHTTPException("Repository update error: " + e.getMessage(), e);
		}
	}
}
