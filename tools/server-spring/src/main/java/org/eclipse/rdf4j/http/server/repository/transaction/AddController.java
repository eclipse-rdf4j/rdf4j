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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author jeen
 *
 */
public class AddController extends AbstractActionController {

	@Override
	protected ModelAndView handleAction(HttpServletRequest request, HttpServletResponse response,
			Transaction transaction) throws Exception {

		var baseURI = getBaseURI(request);
		var contexts = getContexts(request);

		boolean preserveNodeIds = ProtocolUtil.parseBooleanParam(request, Protocol.PRESERVE_BNODE_ID_PARAM_NAME, false);
		RDFFormat format = Rio.getParserFormatForMIMEType(request.getContentType())
				.orElseThrow(Rio.unsupportedFormat(request.getContentType()));

		transaction.add(request.getInputStream(), baseURI, format, preserveNodeIds, contexts);

		return emptyOkResponse();
	}

}
