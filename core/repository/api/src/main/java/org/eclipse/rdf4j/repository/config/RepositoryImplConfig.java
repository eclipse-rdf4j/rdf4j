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
package org.eclipse.rdf4j.repository.config;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

/**
 * @author Arjohn Kampman
 */
public interface RepositoryImplConfig {

	String getType();

	/**
	 * Validates this configuration. A {@link RepositoryConfigException} is thrown when the configuration is invalid.
	 * The exception should contain an error message that indicates why the configuration is invalid.
	 *
	 * @throws RepositoryConfigException If the configuration is invalid.
	 */
	void validate() throws RepositoryConfigException;

	/**
	 * Export this {@link RepositoryImplConfig} to its RDF representation
	 *
	 * @param model a {@link Model} object. After successful completion of this method this Model will contain the RDF
	 *              representation of this {@link RepositoryImplConfig}.
	 * @return the subject {@link Resource} that identifies this {@link RepositoryImplConfig} in the Model.
	 */
	Resource export(Model model);

	/**
	 * Reads the properties of this {@link RepositoryImplConfig} from the supplied Model and sets them accordingly.
	 *
	 * @param model    a {@link Model} containing repository configuration data.
	 * @param resource the subject {@link Resource} that identifies the {@link RepositoryImplConfig} in the Model.
	 * @throws RepositoryConfigException if the configuration data could not be read from the supplied Model.
	 */
	void parse(Model model, Resource resource) throws RepositoryConfigException;

}
