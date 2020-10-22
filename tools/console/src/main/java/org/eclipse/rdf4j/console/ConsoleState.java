/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.File;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * Console state interface
 *
 * @author Dale Visser
 */
public interface ConsoleState {

	/**
	 * Get application name
	 *
	 * @return application name
	 */
	public String getApplicationName();

	/**
	 * Get repository data directory
	 *
	 * @return directory
	 */
	public File getDataDirectory();

	/**
	 * Get repository manager ID
	 *
	 * @return repository manager ID
	 */
	public String getManagerID();

	/**
	 * Get repository ID
	 *
	 * @return repository ID
	 */
	public String getRepositoryID();

	/**
	 * Get repository manager
	 *
	 * @return repository manager
	 */
	public RepositoryManager getManager();

	/**
	 * Set repository manager
	 *
	 * @param manager repository manager
	 */
	public void setManager(RepositoryManager manager);

	/**
	 * Set repository manager
	 *
	 * @param managerID repository manager ID
	 */
	public void setManagerID(String managerID);

	/**
	 * Get repository
	 *
	 * @return repository
	 */
	public Repository getRepository();

	/**
	 * Set repository ID
	 *
	 * @param repositoryID repository ID
	 */
	public void setRepositoryID(String repositoryID);

	/**
	 * Set repository
	 *
	 * @param repository repository
	 */
	public void setRepository(Repository repository);

}
