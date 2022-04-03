/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.handler;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.ModelAndView;

public interface RepositoryRequestHandler {

	ModelAndView handleCreateNewRepositoryRequest(HttpServletRequest request) throws Exception;

	ModelAndView handleDeleteRepositoryRequest(HttpServletRequest request) throws Exception;

}
