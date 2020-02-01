/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.platform;

import java.io.File;

/**
 * The Platform interface defines methods that are expected to differ slightly between operating systems, e.g. methods
 * for opening local files, storing application data, etc.
 */
public interface Platform {

	public static final String APPDATA_BASEDIR_PROPERTY = "org.eclipse.rdf4j.appdata.basedir";

	@Deprecated
	public static final String OLD_DATADIR_PROPERTY = "info.aduna.platform.appdata.basedir";

	/**
	 * Get a descriptive name for this platform.
	 * 
	 * @return name of the platform
	 */
	public String getName();

	/**
	 * Returns the operating system dependent application data directory.
	 * 
	 * @return application directory
	 */
	public File getOSApplicationDataDir();

	/**
	 * Returns the operating system dependent application data directory. This will be a sub-directory of the directory
	 * returned by the no-argument version of this method.
	 * 
	 * @param applicationName name of the application
	 * @return application directory
	 */
	public File getOSApplicationDataDir(String applicationName);

	/**
	 * Returns the directory for the current user.
	 * 
	 * @return the current user home directory
	 */
	public File getUserHome();

	/**
	 * Returns the directory in which Aduna applications can store their application-dependent data, returns
	 * 'getOSApplicationDataDir' unless the system property "aduna.platform.applicationdata.dir" has been set.
	 * 
	 * @return the Aduna-specific application data directory
	 */
	public File getApplicationDataDir();

	/**
	 * Returns the directory in which a specific application can store all its application-dependent data. This will be
	 * a sub-directory of the directory returned by the no-argument version of this method. Note: the directory might
	 * not exist yet.
	 * 
	 * @see #getApplicationDataDir()
	 * @param applicationName the name of the application for which to determine the directory
	 * @return an application-specific data directory
	 */
	public File getApplicationDataDir(String applicationName);

	/**
	 * Get the directory relative to getApplicationDataDir() for the specified application.
	 * 
	 * @param applicationName the name of the application
	 * @return the directory relative to getApplicationDataDir() for the specified application
	 */
	public String getRelativeApplicationDataDir(String applicationName);

	/**
	 * Check if case is preserved
	 * 
	 * @return boolean
	 */
	public boolean dataDirPreserveCase();

	/**
	 * Check if whitespace is replaced
	 * 
	 * @return boolean
	 */
	public boolean dataDirReplaceWhitespace();

	/**
	 * Check if colon is replaced
	 * 
	 * @return boolean
	 */
	public boolean dataDirReplaceColon();
}
