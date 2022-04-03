/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.resolver;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

public interface RepositoryResolver {

	RepositoryManager getRepositoryManager();

	String getRepositoryID(HttpServletRequest request);

	RepositoryConfig getRepositoryConfig(String repId, Model model);

	RepositoryConnection getRepositoryConnection(HttpServletRequest request, Repository repository);

	Repository getRepository(HttpServletRequest request);

}
