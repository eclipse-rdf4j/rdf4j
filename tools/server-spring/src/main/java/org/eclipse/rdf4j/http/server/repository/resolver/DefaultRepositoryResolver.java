/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.resolver;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

public class DefaultRepositoryResolver implements RepositoryResolver {

	private final RepositoryManager repositoryManager;

	public DefaultRepositoryResolver(RepositoryManager repMan) {
		repositoryManager = repMan;
	}

	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}

	public RepositoryConfig getRepositoryConfig(String repId, Model model) {
		return RepositoryConfigUtil.getRepositoryConfig(model, repId);
	}

	public String getRepositoryID(HttpServletRequest request) {
		return RepositoryInterceptor.getRepositoryID(request);
	}

	public RepositoryConnection getRepositoryConnection(HttpServletRequest request, Repository repository) {
		RepositoryConnection conn = repository.getConnection();
		conn.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		conn.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);
		return conn;
	}

	public Repository getRepository(HttpServletRequest request) {
		return RepositoryInterceptor.getRepository(request);
	}

}
