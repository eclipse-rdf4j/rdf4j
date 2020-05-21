/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.event;

import java.io.File;
import java.util.EventListener;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Interceptor interface for Repository state changes.
 *
 * @author Herko ter Horst
 */
public interface RepositoryInterceptor extends EventListener {

	public abstract boolean getConnection(Repository repo, RepositoryConnection conn);

	public abstract boolean initialize(Repository repo);

	public abstract boolean setDataDir(Repository repo, File dataDir);

	public abstract boolean shutDown(Repository repo);

}
