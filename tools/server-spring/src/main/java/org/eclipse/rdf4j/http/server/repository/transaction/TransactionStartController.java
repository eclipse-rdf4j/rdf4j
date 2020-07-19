/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.common.transaction.TransactionSettingRegistry;
import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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

	private ModelAndView startTransaction(Repository repository, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ClientHTTPException, ServerHTTPException {
		ProtocolUtil.logRequestParameters(request);
		Map<String, Object> model = new HashMap<>();

		ArrayList<TransactionSetting> transactionSettings = new ArrayList<>();

		final IsolationLevel[] isolationLevel = { null };

		// process legacy isolation level param for backward compatibility with older clients
		final String isolationLevelString = request.getParameter(Protocol.ISOLATION_LEVEL_PARAM_NAME);
		if (isolationLevelString != null) {
			final IRI level = SimpleValueFactory.getInstance().createIRI(isolationLevelString);

			for (IsolationLevel standardLevel : IsolationLevels.values()) {
				if (standardLevel.getURI().equals(level)) {
					isolationLevel[0] = standardLevel;
					break;
				}
			}
		}

		request.getParameterMap().forEach((k, v) -> {
			if (k.startsWith(Protocol.TRANSACTION_SETTINGS_PREFIX)) {
				String settingsName = k.replace(Protocol.TRANSACTION_SETTINGS_PREFIX, "");

				// FIXME we should make the isolation level an SPI impl as well so that it will work with non-standard
				// isolation levels
				if (settingsName.equals(IsolationLevels.NONE.getName())) {
					isolationLevel[0] = IsolationLevels.valueOf(v[0]);
					transactionSettings.add(isolationLevel[0]);
				} else {
					TransactionSettingRegistry.getInstance()
							.get(settingsName)
							.flatMap(factory -> factory.getTransactionSetting(v[0]))
							.ifPresent(transactionSettings::add);
				}
			}
		});

		Transaction txn = null;
		boolean allGood = false;
		try {
			txn = new Transaction(repository);

			if (transactionSettings.isEmpty()) {
				if (isolationLevel[0] == null) {
					txn.begin();
				} else {
					txn.begin(isolationLevel[0]);
				}
			} else {
				txn.begin(transactionSettings.toArray(new TransactionSetting[0]));
			}

			UUID txnId = txn.getID();

			model.put(SimpleResponseView.SC_KEY, SC_CREATED);
			final StringBuffer txnURL = request.getRequestURL();
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

}
