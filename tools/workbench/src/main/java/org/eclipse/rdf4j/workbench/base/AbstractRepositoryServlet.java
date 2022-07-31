/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.base;

import java.net.MalformedURLException;
import java.net.URL;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.workbench.RepositoryServlet;
import org.eclipse.rdf4j.workbench.exceptions.MissingInitParameterException;

public abstract class AbstractRepositoryServlet extends AbstractServlet implements RepositoryServlet {

	public static final String REPOSITORY_PARAM = "repository";

	public static final String MANAGER_PARAM = "repository-manager";

	protected RepositoryManager manager;

	protected RepositoryInfo info;

	protected Repository repository;

	protected ValueFactory vf;

	@Override
	public void setRepositoryManager(RepositoryManager manager) {
		this.manager = manager;
	}

	@Override
	public void setRepositoryInfo(RepositoryInfo info) {
		this.info = info;
	}

	@Override
	public void setRepository(Repository repository) {
		if (repository == null) {
			this.vf = SimpleValueFactory.getInstance();
		} else {
			this.repository = repository;
			this.vf = repository.getValueFactory();

			if (this.repository instanceof HTTPRepository) {
				((HTTPRepository) this.repository).setPreferredRDFFormat(RDFFormat.BINARY);
			}
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		if (repository == null) {
			if (config.getInitParameter(REPOSITORY_PARAM) != null) {
				setRepository((Repository) lookup(config, REPOSITORY_PARAM));
			}
		}
		if (manager == null) {
			if (config.getInitParameter(MANAGER_PARAM) == null) {
				throw new MissingInitParameterException(MANAGER_PARAM);
			}
			manager = (RepositoryManager) lookup(config, MANAGER_PARAM);
		}
		if (info == null) {
			info = new RepositoryInfo();
			info.setId(config.getInitParameter("id"));
			info.setDescription(config.getInitParameter("description"));
			try {
				if (repository == null) {
					info.setReadable(false);
					info.setWritable(false);
				} else {
					info.setReadable(true);
					info.setWritable(repository.isWritable());
				}
				String location = config.getInitParameter("location");
				if (location != null && location.trim().length() > 0) {
					info.setLocation(new URL(location));
				}
			} catch (MalformedURLException | RepositoryException e) {
				throw new ServletException(e);
			}
		}
	}

	private Object lookup(ServletConfig config, String name) throws ServletException {
		String param = config.getInitParameter(name);
		try {
			InitialContext ctx = new InitialContext();
			return ctx.lookup(param);
		} catch (NamingException e) {
			throw new ServletException(e);
		}
	}
}
