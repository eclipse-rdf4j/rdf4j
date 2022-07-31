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
package org.eclipse.rdf4j.common.app.config;

import java.io.IOException;

/**
 * Application configuration interface
 */
public interface Configuration {

	String DIR = "conf";
	String RESOURCES_LOCATION = "/org/eclipse/rdf4j/common/app/config/";
	String DEFAULT_RESOURCES_LOCATION = RESOURCES_LOCATION + "defaults/";

	/**
	 * Initialize the configuration settings.
	 *
	 * @throws IOException if the configuration settings could not be initialized because of an I/O problem.
	 */
	void init() throws IOException;

	/**
	 * Load the configuration settings. Settings will be loaded from a user and application specific location first. If
	 * no such settings exists, an attempt will be made to retrieve settings from a resource on the classpath. If no
	 * such settings exist either, settings will be loaded from a default resource on the classpath.
	 *
	 * @throws IOException if the configuration settings could not be loaded due to an I/O problem.
	 */
	void load() throws IOException;

	/**
	 * Store configuration settings. Settings will be stored in a user and application specific location.
	 *
	 * @throws IOException if the configuration settings could not be saved due to an I/O problem.
	 */
	void save() throws IOException;

	/**
	 * Clean up configuration resources.
	 *
	 * @throws IOException if one or more resources could not be cleaned up. Implementations should attempt to clean up
	 *                     as many resources as possible before returning or throwing an exception.
	 */
	void destroy() throws IOException;
}
