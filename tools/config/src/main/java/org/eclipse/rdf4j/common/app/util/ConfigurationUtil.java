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
package org.eclipse.rdf4j.common.app.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.rdf4j.common.app.config.Configuration;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.io.ResourceUtil;

/**
 * Configuration helper class
 */
public class ConfigurationUtil {

	/**
	 * Load configuration settings from the specified file.
	 *
	 * @param file the file to load from
	 * @return the contents of the file as a String, or null if the file did not exist
	 * @throws IOException if the contents of the file could not be read due to an I/O problem
	 */
	public static String loadConfigurationContents(File file) throws IOException {
		String result = null;
		if (file.exists()) {
			result = IOUtil.readString(file);
		}
		return result;
	}

	/**
	 * Load configuration settings from a resource on the classpath.
	 *
	 * @param resourceName the name of the resource
	 * @return the contents of the resources as a String, or null if the resource, nor its default, could be found
	 * @throws IOException if the resource could not be read due to an I/O problem
	 */
	public static String loadConfigurationContents(String resourceName) throws IOException {
		InputStream in = null;
		try {
			in = ResourceUtil.getInputStream(getResourceName(resourceName));
			if (in == null) {
				in = ResourceUtil.getInputStream(getDefaultResourceName(resourceName));
			}
			if (in != null) {
				return IOUtil.readString(in);
			}
			return null;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * Load configuration properties from the specified file.
	 *
	 * @param file     the file to load from
	 * @param defaults default properties
	 * @return the contents of the file as Properties, or null if the file did not exist
	 * @throws IOException if the contents of the file could not be read due to an I/O problem
	 */
	public static Properties loadConfigurationProperties(File file, Properties defaults) throws IOException {
		Properties result;
		if (file.exists()) {
			result = IOUtil.readProperties(file, defaults);
		} else {
			result = new Properties(defaults);
		}
		return result;
	}

	/**
	 * Load configuration properties from a resource on the classpath.
	 *
	 * @param resourceName the name of the resource
	 * @param defaults     default properties
	 * @return the contents of the resource as Properties
	 * @throws IOException if the resource could not be read due to an I/O problem
	 */
	public static Properties loadConfigurationProperties(String resourceName, Properties defaults) throws IOException {
		Properties result;

		String defaultResourceName = getDefaultResourceName(resourceName);

		Properties defaultResult;
		InputStream in = ResourceUtil.getInputStream(defaultResourceName);
		if (in != null) {
			defaultResult = IOUtil.readProperties(in, defaults);
		} else {
			defaultResult = new Properties(defaults);
		}

		// load application-specific overrides
		in = ResourceUtil.getInputStream(getResourceName(resourceName));
		if (in != null) {
			result = IOUtil.readProperties(in, defaultResult);
		} else {
			result = new Properties(defaultResult);
		}

		return result;
	}

	/**
	 * Get full resource name
	 *
	 * @param resourceName relative resource name
	 * @return full resource location
	 */
	private static String getResourceName(String resourceName) {
		StringBuilder result = new StringBuilder(Configuration.RESOURCES_LOCATION);
		if (resourceName.startsWith("/")) {
			resourceName = resourceName.substring(1);
		}
		result.append(resourceName);
		return result.toString();
	}

	/**
	 * Get full resource name from default location
	 *
	 * @param resourceName relative resource name
	 * @return full default resource location
	 */
	private static String getDefaultResourceName(String resourceName) {
		StringBuilder result = new StringBuilder(Configuration.DEFAULT_RESOURCES_LOCATION);
		if (resourceName.startsWith("/")) {
			resourceName = resourceName.substring(1);
		}
		result.append(resourceName);
		return result.toString();
	}

	/**
	 * Save configuration settings to a file.
	 *
	 * @param contents the configuration settings
	 * @param file     the file to write to
	 * @throws IOException if the settings could not be saved because of an I/O problem
	 */
	public static void saveConfigurationContents(String contents, File file) throws IOException {
		if (file.getParentFile().mkdirs() || file.getParentFile().canWrite()) {
			IOUtil.writeString(contents, file);
		}
	}

	/**
	 * Save configuration properties to a file.
	 *
	 * @param props           the configuration properties
	 * @param file            the file to write to
	 * @param includeDefaults
	 * @throws IOException if the settings could not be saved because of an I/O problem
	 */
	public static void saveConfigurationProperties(Properties props, File file, boolean includeDefaults)
			throws IOException {
		if (file.getParentFile().mkdirs() || file.getParentFile().canWrite()) {
			IOUtil.writeProperties(props, file, includeDefaults);
		}
	}
}
