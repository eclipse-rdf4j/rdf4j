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

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.Action;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests for transaction rollbacks on a repository, and provides backward-compatible (deprecated) support for
 * all other transaction operations.
 *
 * @author Jeen Broekstra
 */
public class TransactionController extends AbstractController implements DisposableBean {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public TransactionController() throws ApplicationContextException {
		setSupportedMethods(METHOD_POST, "PUT", "DELETE");
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		ModelAndView result;

		String reqMethod = request.getMethod();
		UUID transactionId = getTransactionID(request);
		logger.debug("transaction id: {}", transactionId);
		logger.debug("request content type: {}", request.getContentType());

		Transaction transaction = ActiveTransactionRegistry.INSTANCE.getTransaction(transactionId);

		if (transaction == null) {
			logger.warn("could not find transaction for transaction id {}", transactionId);
			throw new ClientHTTPException(SC_BAD_REQUEST,
					"unable to find registered transaction for transaction id '" + transactionId + "'");
		}

		if ("DELETE".equals(reqMethod)) {
			logger.info("transaction rollback");
			try {
				transaction.rollback();
			} finally {
				try {
					transaction.close();
				} finally {
					ActiveTransactionRegistry.INSTANCE.deregister(transaction);
				}
			}
			result = new ModelAndView(EmptySuccessView.getInstance());
			logger.info("transaction rollback request finished.");
			if (!(transaction.isClosed() || transaction.isComplete())) {
				ActiveTransactionRegistry.INSTANCE.active(transaction);
			}
			return result;
		}

		if (!("PUT".equals(reqMethod) || METHOD_POST.equals(reqMethod))) {
			throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
					"Method not allowed: " + reqMethod);
		}

		// if no action is specified in the request, it's a rollback
		final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
		final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;

		switch (action) {
		case ADD:
			logger.warn("{} {}: deprecated endpoint. Use {} instead", reqMethod, request.getServletPath(),
					request.getServletPath() + "/" + action.toString().toLowerCase());
			result = new AddController().handleAction(request, response, transaction);
			break;
		case DELETE:
			logger.warn("{} {}: deprecated endpoint. Use {} instead", reqMethod, request.getServletPath(),
					request.getServletPath() + "/" + action.toString().toLowerCase());
			result = new DeleteController().handleAction(request, response, transaction);
			break;
		case UPDATE:
			logger.warn("{} {}: deprecated endpoint. Use {} instead", reqMethod, request.getServletPath(),
					request.getServletPath() + "/" + action.toString().toLowerCase());
			result = new UpdateController().handleAction(request, response, transaction);
			break;
		case PREPARE:
			logger.warn("{} {}: deprecated endpoint. Use {} instead", reqMethod, request.getServletPath(),
					request.getServletPath() + "/" + action.toString().toLowerCase());
			result = new PrepareController().handleAction(request, response, transaction);
			break;
		case QUERY:
			logger.warn("{} {}: deprecated endpoint. Use {} instead", reqMethod, request.getServletPath(),
					request.getServletPath() + "/" + action.toString().toLowerCase());
			result = new QueryController().handleAction(request, response, transaction);
			break;
		case COMMIT:
			logger.warn("{} {}: deprecated endpoint. Use {} instead", reqMethod, request.getServletPath(),
					request.getServletPath() + "/" + action.toString().toLowerCase());
			result = new CommitController().handleAction(request, response, transaction);
			break;
		case GET:
			logger.warn("{} {}: deprecated endpoint", reqMethod, request.getServletPath());
			result = new ExportController().handleAction(request, response, transaction);
			break;
		case SIZE:
			logger.warn("{} {}: deprecated endpoint", reqMethod, request.getServletPath());
			result = new SizeController().handleAction(request, response, transaction);
			break;
		case PING:
			logger.warn("{} {}: deprecated endpoint", reqMethod, request.getServletPath());
			result = new PingController().handleAction(request, response, transaction);
			break;
		default:
			if (action.equals(Action.ROLLBACK) && ("PUT".equals(reqMethod) || METHOD_POST.equals(reqMethod))) {
				logger.warn("{} {}: deprecated verb for rollback action. Use DELETE instead", reqMethod,
						request.getServletPath());
				logger.info("transaction rollback");
				try {
					transaction.rollback();
				} finally {
					try {
						transaction.close();
					} finally {
						ActiveTransactionRegistry.INSTANCE.deregister(transaction);
					}
				}
				result = new ModelAndView(EmptySuccessView.getInstance());
				logger.info("transaction rollback request finished.");
			} else {
				throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
						"Method not allowed: " + reqMethod);
			}
			break;
		}

		if (!(transaction.isClosed() || transaction.isComplete())) {
			ActiveTransactionRegistry.INSTANCE.active(transaction);
		}
		return result;
	}

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

	// Comes from disposableBean interface so to be able to stop the ActiveTransactionRegistry scheduler
	@Override
	public void destroy()
			throws Exception {
		ActiveTransactionRegistry.INSTANCE.destroyScheduler();
	}

}
