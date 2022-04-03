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

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

public interface QueryRequestHandler {

	ModelAndView handleQueryRequest(HttpServletRequest request, RequestMethod requestMethod,
			HttpServletResponse response) throws Exception;

	View getViewFor(Query query);

	FileFormatServiceRegistry<? extends FileFormat, ?> getResultWriterFor(Query query);

	String getQueryString(HttpServletRequest request, RequestMethod requestMethod) throws Exception;

	Query getQuery(HttpServletRequest request, RepositoryConnection repositoryCon, String queryString) throws Exception;

}
