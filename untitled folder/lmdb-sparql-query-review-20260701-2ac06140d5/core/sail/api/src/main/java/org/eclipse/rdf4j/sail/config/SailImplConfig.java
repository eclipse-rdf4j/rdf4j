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
package org.eclipse.rdf4j.sail.config;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

/**
 * Configuration of a Sail implementation.
 *
 * @author Arjohn Kampman
 */
public interface SailImplConfig {

	String getType();

	long getIterationCacheSyncThreshold();

	/**
	 * Validates this configuration. A {@link SailConfigException} is thrown when the configuration is invalid. The
	 * exception should contain an error message that indicates why the configuration is invalid.
	 *
	 * @throws SailConfigException If the configuration is invalid.
	 */
	void validate() throws SailConfigException;

	Resource export(Model graph);

	void parse(Model graph, Resource implNode) throws SailConfigException;
}
