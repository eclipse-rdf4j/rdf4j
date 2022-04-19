/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.namespaces;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests for manipulating a specific namespace definition in a repository.
 *
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class NamespaceController extends AbstractController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public NamespaceController() throws ApplicationContextException {
		setSupportedMethods(new String[] { METHOD_GET, METHOD_HEAD, "PUT", "DELETE" });
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String pathInfoStr = request.getPathInfo();
		String prefix = pathInfoStr.substring(pathInfoStr.lastIndexOf('/') + 1);

		String reqMethod = request.getMethod();

		if (METHOD_HEAD.equals(reqMethod)) {
			logger.info("HEAD namespace for prefix {}", prefix);

			Map<String, Object> model = new HashMap<>();
			return new ModelAndView(SimpleResponseView.getInstance(), model);
		}

		if (METHOD_GET.equals(reqMethod)) {
			logger.info("GET namespace for prefix {}", prefix);
			return getExportNamespaceResult(request, prefix);
		} else if ("PUT".equals(reqMethod)) {
			logger.info("PUT prefix {}", prefix);
			return getUpdateNamespaceResult(request, prefix);
		} else if ("DELETE".equals(reqMethod)) {
			logger.info("DELETE prefix {}", prefix);
			return getRemoveNamespaceResult(request, prefix);
		} else {
			throw new ServerHTTPException("Unexpected request method: " + reqMethod);
		}
	}

	private ModelAndView getExportNamespaceResult(HttpServletRequest request, String prefix)
			throws ServerHTTPException, ClientHTTPException {
		try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
			String namespace = repositoryCon.getNamespace(prefix);

			if (namespace == null) {
				throw new ClientHTTPException(SC_NOT_FOUND, "Undefined prefix: " + prefix);
			}

			Map<String, Object> model = new HashMap<>();
			model.put(SimpleResponseView.CONTENT_KEY, namespace);

			return new ModelAndView(SimpleResponseView.getInstance(), model);
		} catch (RepositoryException e) {
			throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
		}
	}

	private ModelAndView getUpdateNamespaceResult(HttpServletRequest request, String prefix)
			throws IOException, ClientHTTPException, ServerHTTPException {
		String namespace = IOUtil.readString(request.getReader());
		namespace = namespace.trim();

		if (namespace.length() == 0) {
			throw new ClientHTTPException(SC_BAD_REQUEST, "No namespace name found in request body");
		}
		// FIXME: perform some sanity checks on the namespace string

		try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
			repositoryCon.setNamespace(prefix, namespace);
		} catch (RepositoryException e) {
			throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
		}

		return new ModelAndView(EmptySuccessView.getInstance());
	}

	private ModelAndView getRemoveNamespaceResult(HttpServletRequest request, String prefix)
			throws ServerHTTPException {
		try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
			repositoryCon.removeNamespace(prefix);
		} catch (RepositoryException e) {
			throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
		}

		return new ModelAndView(EmptySuccessView.getInstance());
	}
}
