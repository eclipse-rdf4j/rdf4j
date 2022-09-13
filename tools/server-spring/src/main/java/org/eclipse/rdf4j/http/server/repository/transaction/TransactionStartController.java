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

import static javax.servlet.http.HttpServletResponse.SC_CREATED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.common.transaction.TransactionSettingRegistry;
import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests for transaction creation on a repository.
 *
 * @author Jeen Broekstra
 */
public class TransactionStartController extends AbstractController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private String externalUrl;

	public TransactionStartController() throws ApplicationContextException {
		setSupportedMethods(METHOD_POST);
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		ModelAndView result;

		Repository repository = RepositoryInterceptor.getRepository(request);

		String reqMethod = request.getMethod();

		if (METHOD_POST.equals(reqMethod)) {
			logger.info("POST transaction start");
			result = startTransaction(repository, request, response);
			logger.info("transaction started");
		} else {
			throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
					"Method not allowed: " + reqMethod);
		}
		return result;
	}

	@Deprecated
	ArrayList<TransactionSetting> getIsolationLevel(HttpServletRequest request) {
		// process legacy isolation level param for backward compatibility with older clients

		ArrayList<TransactionSetting> transactionSettings = new ArrayList<>();
		final String isolationLevelString = request.getParameter(Protocol.ISOLATION_LEVEL_PARAM_NAME);
		if (isolationLevelString != null) {

			for (IsolationLevel standardLevel : IsolationLevels.values()) {
				if (standardLevel.getValue().equals(isolationLevelString)) {
					transactionSettings.add(IsolationLevels.valueOf(isolationLevelString));
					break;
				}
			}
			if (transactionSettings.isEmpty())
				throw new IllegalArgumentException("Unknown isolation-level setting " + isolationLevelString);
		}

		return transactionSettings;
	}

	ArrayList<TransactionSetting> getTransactionSettings(HttpServletRequest request) {
		ArrayList<TransactionSetting> transactionSettings = new ArrayList<>();
		request.getParameterMap().forEach((k, v) -> {
			if (k.startsWith(Protocol.TRANSACTION_SETTINGS_PREFIX)) {
				String settingsName = k.replace(Protocol.TRANSACTION_SETTINGS_PREFIX, "");

				// FIXME we should make the isolation level an SPI impl as well so that it will work with
				// non-standard isolation levels
				if (settingsName.equals(IsolationLevels.NONE.getName())) {
					transactionSettings.add(IsolationLevels.valueOf(v[0]));
				} else {
					TransactionSettingRegistry.getInstance()
							.get(settingsName)
							.flatMap(factory -> factory.getTransactionSetting(v[0]))
							.ifPresent(transactionSettings::add);
				}
			}
		});

		return transactionSettings;
	}

	Transaction createTransaction(Repository repository) throws ExecutionException, InterruptedException {
		return new Transaction(repository);
	}

	private ModelAndView startTransaction(Repository repository, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ClientHTTPException, ServerHTTPException {
		ProtocolUtil.logRequestParameters(request);
		Map<String, Object> model = new HashMap<>();

		ArrayList<TransactionSetting> transactionSettings = getIsolationLevel(request);
		transactionSettings.addAll(getTransactionSettings(request));

		Transaction txn = null;
		boolean allGood = false;
		try {
			txn = createTransaction(repository);

			if (transactionSettings.isEmpty()) {
				txn.begin();
			} else {
				txn.begin(transactionSettings.toArray(new TransactionSetting[0]));
			}

			UUID txnId = txn.getID();

			model.put(SimpleResponseView.SC_KEY, SC_CREATED);
			final StringBuffer txnURL = getUrlBasePath(request);
			txnURL.append("/" + txnId.toString());
			Map<String, String> customHeaders = new HashMap<>();
			customHeaders.put("Location", txnURL.toString());
			model.put(SimpleResponseView.CUSTOM_HEADERS_KEY, customHeaders);

			ModelAndView result = new ModelAndView(SimpleResponseView.getInstance(), model);
			ActiveTransactionRegistry.INSTANCE.register(txn);
			allGood = true;
			return result;
		} catch (RepositoryException | InterruptedException | ExecutionException e) {
			throw new ServerHTTPException("Transaction start error: " + e.getMessage(), e);
		} finally {
			if (!allGood) {
				try {
					txn.close();
				} catch (InterruptedException | ExecutionException e) {
					throw new ServerHTTPException("Transaction start error: " + e.getMessage(), e);
				}
			}
		}
	}

	private StringBuffer getUrlBasePath(final HttpServletRequest request) {
		if (externalUrl == null) {
			return request.getRequestURL();
		}

		final StringBuffer url = new StringBuffer();
		if (externalUrl.endsWith("/")) {
			url.append(externalUrl, 0, externalUrl.length() - 1);
		} else {
			url.append(externalUrl);
		}

		url.append(request.getRequestURI());
		return url;
	}

	public void setExternalUrl(final String externalUrl) {
		this.externalUrl = externalUrl;
	}
}
