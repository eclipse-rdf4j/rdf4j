/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.File;

import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * Console state helper class
 *
 * @author Bart Hanssens
 */
class DefaultConsoleState implements ConsoleState {
	private final AppConfiguration cfg;

	private RepositoryManager manager;
	private String managerID;

	private Repository repository;
	private String repositoryID;

	@Override
	public String getApplicationName() {
		return cfg.getFullName();
	}

	@Override
	public File getDataDirectory() {
		return cfg.getDataDir();
	}

	@Override
	public String getManagerID() {
		return this.managerID;
	}

	@Override
	public String getRepositoryID() {
		return this.repositoryID;
	}

	@Override
	public RepositoryManager getManager() {
		return this.manager;
	}

	@Override
	public void setManager(RepositoryManager manager) {
		this.manager = manager;
	}

	@Override
	public void setManagerID(String managerID) {
		this.managerID = managerID;
	}

	@Override
	public Repository getRepository() {
		return this.repository;
	}

	@Override
	public void setRepositoryID(String repositoryID) {
		this.repositoryID = repositoryID;
	}

	@Override
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	/**
	 * Constructor
	 *
	 * @param cfg
	 */
	DefaultConsoleState(AppConfiguration cfg) {
		this.cfg = cfg;
	}
}
