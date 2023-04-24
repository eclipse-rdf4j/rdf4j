/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.transaction;

import static org.eclipse.rdf4j.http.protocol.Protocol.CONTEXT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INCLUDE_INFERRED_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.OBJECT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.PREDICATE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.SUBJECT_PARAM_NAME;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author jeen
 *
 */
public class ExportController extends AbstractActionController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected ModelAndView handleAction(HttpServletRequest request, HttpServletResponse response,
			Transaction transaction) throws Exception {
		logger.info("{} txn get/export statements request", request.getMethod());
		var result = getExportStatementsResult(transaction, request, response);
		logger.info("{} txn get/export statements request finished", request.getMethod());
		return result;
	}

	/**
	 * Get all statements and export them as RDF.
	 *
	 * @return a model and view for exporting the statements.
	 */
	private ModelAndView getExportStatementsResult(Transaction transaction, HttpServletRequest request,
			HttpServletResponse response) throws ClientHTTPException {
		ProtocolUtil.logRequestParameters(request);

		ValueFactory vf = SimpleValueFactory.getInstance();

		Resource subj = ProtocolUtil.parseResourceParam(request, SUBJECT_PARAM_NAME, vf);
		IRI pred = ProtocolUtil.parseURIParam(request, PREDICATE_PARAM_NAME, vf);
		Value obj = ProtocolUtil.parseValueParam(request, OBJECT_PARAM_NAME, vf);
		Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);
		boolean useInferencing = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

		RDFWriterFactory rdfWriterFactory = ProtocolUtil.getAcceptableService(request, response,
				RDFWriterRegistry.getInstance());

		Map<String, Object> model = new HashMap<>();
		model.put(TransactionExportStatementsView.SUBJECT_KEY, subj);
		model.put(TransactionExportStatementsView.PREDICATE_KEY, pred);
		model.put(TransactionExportStatementsView.OBJECT_KEY, obj);
		model.put(TransactionExportStatementsView.CONTEXTS_KEY, contexts);
		model.put(TransactionExportStatementsView.USE_INFERENCING_KEY, Boolean.valueOf(useInferencing));
		model.put(TransactionExportStatementsView.FACTORY_KEY, rdfWriterFactory);
		model.put(TransactionExportStatementsView.HEADERS_ONLY, METHOD_HEAD.equals(request.getMethod()));

		model.put(TransactionExportStatementsView.TRANSACTION_KEY, transaction);
		return new ModelAndView(TransactionExportStatementsView.getInstance(), model);
	}
}
