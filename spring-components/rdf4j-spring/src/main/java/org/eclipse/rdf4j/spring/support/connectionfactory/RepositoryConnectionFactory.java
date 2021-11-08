/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.support.connectionfactory;

import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public interface RepositoryConnectionFactory {
	RepositoryConnection getConnection();
}
