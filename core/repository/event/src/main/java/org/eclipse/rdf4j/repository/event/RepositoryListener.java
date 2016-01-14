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

/**
 * Listener interface for Repository state changes.
 * 
 * @author James Leigh
 * 
 */
public interface RepositoryListener extends EventListener {

	public abstract void getConnection(NotifyingRepository repo, NotifyingRepositoryConnection conn);

	public abstract void initialize(NotifyingRepository repo);

	public abstract void setDataDir(NotifyingRepository repo, File dataDir);

	public abstract void shutDown(NotifyingRepository repo);

}
