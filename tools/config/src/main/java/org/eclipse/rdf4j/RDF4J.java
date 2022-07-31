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
package org.eclipse.rdf4j;

import org.eclipse.rdf4j.common.io.MavenUtil;

/**
 * @author Arjohn Kampman
 */
public class RDF4J {

	private static final String VERSION = loadVersion();

	/**
	 * Get version number string
	 *
	 * @return version as string
	 */
	public final static String getVersion() {
		return VERSION;
	}

	/**
	 * Load version string from java package
	 *
	 * @return version string
	 */
	private static String loadVersion() {
		String impl = RDF4J.class.getPackage().getImplementationVersion();
		if (impl == null) {
			return MavenUtil.loadVersion("org.eclipse.rdf4j", "rdf4j-config", "dev");
		} else {
			return impl;
		}
	}
}
