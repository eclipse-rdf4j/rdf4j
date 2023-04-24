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
package org.eclipse.rdf4j.http.server.repository.transaction;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import static org.eclipse.rdf4j.http.protocol.Protocol.CONTEXT_PARAM_NAME;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests for transaction operations on a repository.
 *
 * @author Jeen Broekstra
 */
public abstract class AbstractActionController extends AbstractController implements DisposableBean {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public AbstractActionController() throws ApplicationContextException {
		setSupportedMethods(new String[] { METHOD_POST, "PUT" });
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		UUID transactionId = getTransactionID(request);
		logger.debug("transaction id: {}", transactionId);
		logger.debug("request content type: {}", request.getContentType());

		Transaction transaction = ActiveTransactionRegistry.INSTANCE.getTransaction(transactionId);

		if (transaction == null) {
			logger.warn("could not find transaction for transaction id {}", transactionId);
			throw new ClientHTTPException(SC_BAD_REQUEST,
					"unable to find registered transaction for transaction id '" + transactionId + "'");
		}

		try {
			var result = handleAction(request, response, transaction);
			if (!(transaction.isClosed() || transaction.isComplete())) {
				ActiveTransactionRegistry.INSTANCE.active(transaction);
			}

			return result;
		} catch (Exception e) {
			if (e instanceof ClientHTTPException) {
				throw (ClientHTTPException) e;
			} else if (e instanceof ServerHTTPException) {
				throw (ServerHTTPException) e;
			} else {
				throw new ServerHTTPException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Transaction handling error: " + e.getMessage(), e);
			}
		}

	}

	// Comes from disposableBean interface so to be able to stop the ActiveTransactionRegistry scheduler
	@Override
	public void destroy() throws Exception {
		ActiveTransactionRegistry.INSTANCE.destroyScheduler();
	}

	/**
	 * Handle the specific action as part of the supplied {@link Transaction} object.
	 *
	 * @param request     the request
	 * @param transaction the transaction on which the action is to be executed
	 * @return result of the action (may not be null)
	 */
	protected abstract ModelAndView handleAction(HttpServletRequest request, HttpServletResponse response,
			Transaction transaction) throws Exception;

	static RDFFormat getRDFFormat(HttpServletRequest request) {
		return Rio.getParserFormatForMIMEType(request.getContentType())
				.orElseThrow(Rio.unsupportedFormat(request.getContentType()));
	}

	static String getBaseURI(HttpServletRequest request) {
		String baseURI = request.getParameter(Protocol.BASEURI_PARAM_NAME);
		return baseURI == null ? "" : baseURI;
	}

	static Resource[] getContexts(HttpServletRequest request) throws ClientHTTPException {
		return ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, SimpleValueFactory.getInstance());
	}

	static Charset getCharset(HttpServletRequest request) {
		return request.getCharacterEncoding() != null ? Charset.forName(request.getCharacterEncoding())
				: StandardCharsets.UTF_8;
	}

	/**
	 * A {@link ModelAndView} for a 200 OK response with an empty body
	 */
	static ModelAndView emptyOkResponse() {
		Map<String, Object> model = new HashMap<>();
		model.put(SimpleResponseView.SC_KEY, HttpServletResponse.SC_OK);
		return new ModelAndView(SimpleResponseView.getInstance(), model);
	}

	/* private methods */

	private UUID getTransactionID(HttpServletRequest request) throws ClientHTTPException {
		String pathInfoStr = request.getPathInfo();

		UUID txnID = null;

		if (pathInfoStr != null && !pathInfoStr.equals("/")) {
			String[] pathInfo = pathInfoStr.substring(1).split("/");
			// should be of the form: /<Repository>/transactions/<txnID>
			if (pathInfo.length == 3) {
				try {
					txnID = UUID.fromString(pathInfo[2]);
					logger.debug("txnID is '{}'", txnID);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "not a valid transaction id: " + pathInfo[2]);
				}
			} else {
				logger.warn("could not determine transaction id from path info {} ", pathInfoStr);
			}
		}

		return txnID;
	}

}
