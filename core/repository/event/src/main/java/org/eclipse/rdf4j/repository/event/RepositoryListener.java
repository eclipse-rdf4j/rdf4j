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
package org.eclipse.rdf4j.repository.event;

import java.io.File;
import java.util.EventListener;

/**
 * Listener interface for Repository state changes.
 *
 * @author James Leigh
 */
public interface RepositoryListener extends EventListener {

	void getConnection(NotifyingRepository repo, NotifyingRepositoryConnection conn);

	void init(NotifyingRepository repo);

	void setDataDir(NotifyingRepository repo, File dataDir);

	void shutDown(NotifyingRepository repo);

}
