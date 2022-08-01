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
package org.eclipse.rdf4j.common.io;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.LoggerFactory;

/**
 * Maven-related utility methods.
 *
 * @author Arjohn Kampman
 */
public class MavenUtil {

	/**
	 * Loads the Maven <var>pom.properties</var> for the specified artifact.
	 *
	 * @param groupId    The artifact's group ID.
	 * @param artifactId The artifact's ID.
	 * @return The parsed pom properties, or <var>null</var> if the resource could not be found.
	 * @throws IOException
	 */
	public static Properties loadPomProperties(String groupId, String artifactId) throws IOException {
		String properties = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
		return ResourceUtil.getProperties(properties);
	}

	/**
	 * Loads the version number from the <var>pom.properties</var> file for the specified artifact.
	 *
	 * @param groupId        The artifact's group ID.
	 * @param artifactId     The artifact's ID.
	 * @param defaultVersion The version number to return in case no version number was found.
	 * @return version as a string
	 */
	public static String loadVersion(String groupId, String artifactId, String defaultVersion) {
		String version = null;

		try {
			Properties pom = loadPomProperties(groupId, artifactId);
			if (pom != null) {
				version = pom.getProperty("version");
			}
		} catch (IOException e) {
			LoggerFactory.getLogger(MavenUtil.class).warn("Unable to read version info", e);
		}

		if (version == null) {
			version = defaultVersion;
		}

		return version;
	}
}
