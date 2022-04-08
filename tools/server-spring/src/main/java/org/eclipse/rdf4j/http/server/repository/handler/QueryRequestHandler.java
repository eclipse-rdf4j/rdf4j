/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interface used by the {@link org.eclipse.rdf4j.http.server.repository.AbstractRepositoryController} to process a
 * query.
 */
public interface QueryRequestHandler {

	ModelAndView handleQueryRequest(HttpServletRequest request, RequestMethod requestMethod,
			HttpServletResponse response) throws Exception;
}
