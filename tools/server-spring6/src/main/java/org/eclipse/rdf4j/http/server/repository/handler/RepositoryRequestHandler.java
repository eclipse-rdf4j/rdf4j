/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.handler;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.ModelAndView;

/**
 * An interface used by {@link org.eclipse.rdf4j.http.server.repository.AbstractRepositoryController} to process HTTP
 * request for creating or deleting a repository.
 */
public interface RepositoryRequestHandler {

	ModelAndView handleCreateOrUpdateRepositoryRequest(HttpServletRequest request) throws Exception;

	ModelAndView handleDeleteRepositoryRequest(HttpServletRequest request) throws Exception;

}
