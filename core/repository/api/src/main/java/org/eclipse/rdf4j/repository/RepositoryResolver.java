/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository;

import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

/**
 * Gets local repositories using a simple identifier string.
 *
 * @author Dale Visser
 * @since 2.3
 */
public interface RepositoryResolver {

	Repository getRepository(String memberID) throws RepositoryException, RepositoryConfigException;
}
