/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.support.connectionfactory;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class DirectRepositoryConnectionFactory implements RepositoryConnectionFactory {
	private final Repository repository;

	public DirectRepositoryConnectionFactory(Repository repository) {
		this.repository = repository;
	}

	@Override
	public RepositoryConnection getConnection() {
		return repository.getConnection();
	}
}
