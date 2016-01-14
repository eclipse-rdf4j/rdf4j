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
 * @author Dale Visser
 */
public interface ConsoleState {

	String getApplicationName();

	File getDataDirectory();

	String getManagerID();

	String getRepositoryID();

	RepositoryManager getManager();

	void setManager(RepositoryManager manager);

	void setManagerID(String managerID);

	Repository getRepository();

	void setRepositoryID(String repositoryID);

	void setRepository(Repository repository);
}
