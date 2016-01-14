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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.BindingAssigner;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.RioSettingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests for transaction creation on a repository.
 * 
 * @since 2.8.0
 * @author Jeen Broekstra
 */
public class TransactionStartController extends AbstractController {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public TransactionStartController()
		throws ApplicationContextException
	{
		setSupportedMethods(new String[] { METHOD_POST });
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
		throws Exception
	{
		ModelAndView result;

		Repository repository = RepositoryInterceptor.getRepository(request);

		String reqMethod = request.getMethod();

		if (METHOD_POST.equals(reqMethod)) {
			logger.info("POST transaction start");
			result = startTransaction(repository, request, response);
			logger.info("transaction started");
		}
		else {
			throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed: "
					+ reqMethod);
		}
		return result;
	}

	private ModelAndView startTransaction(Repository repository, HttpServletRequest request,
			HttpServletResponse response)
		throws IOException, ClientHTTPException, ServerHTTPException
	{
		ProtocolUtil.logRequestParameters(request);
		Map<String, Object> model = new HashMap<String, Object>();

		IsolationLevel isolationLevel = null;
		final String isolationLevelString = request.getParameter(Protocol.ISOLATION_LEVEL_PARAM_NAME);
		if (isolationLevelString != null) {
			final IRI level = SimpleValueFactory.getInstance().createIRI(isolationLevelString);
			
			// FIXME this needs to be adapted to accommodate custom isolation levels
			// from third party stores.
			for (IsolationLevel standardLevel : IsolationLevels.values()) {
				if (standardLevel.getURI().equals(level)) {
					isolationLevel = standardLevel;
					break;
				}
			}
		}

		try {
			RepositoryConnection conn = repository.getConnection();

			ParserConfig config = conn.getParserConfig();
			config.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			config.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
			config.addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);
			conn.begin(isolationLevel);
			UUID txnId = UUID.randomUUID();

			ActiveTransactionRegistry.INSTANCE.register(txnId, conn);
			model.put(SimpleResponseView.SC_KEY, SC_CREATED);
			final StringBuffer txnURL = request.getRequestURL();
			txnURL.append("/" + txnId.toString());
			Map<String, String> customHeaders = new HashMap<String, String>();
			customHeaders.put("Location", txnURL.toString());
			model.put(SimpleResponseView.CUSTOM_HEADERS_KEY, customHeaders);
			return new ModelAndView(SimpleResponseView.getInstance(), model);
		}
		catch (RepositoryException e) {
			throw new ServerHTTPException("Transaction start error: " + e.getMessage(), e);
		}
	}

}
