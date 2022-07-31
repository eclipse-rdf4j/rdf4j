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

package org.eclipse.rdf4j.common.platform.support;

import java.io.File;

import org.eclipse.rdf4j.common.platform.AbstractPlatform;

/**
 * Platform implementation for MS-Windows
 */
public class WindowsPlatform extends AbstractPlatform {

	/** name of the directory containing application data */
	public static final String APPLICATION_DATA = "Application Data";

	/** name of the app data subdirectory containing all RDF4J files * */
	public static final String ADUNA_APPLICATION_DATA = "RDF4J";

	/**
	 * Returns the name of this windows platform.
	 */
	@Override
	public String getName() {
		return "Windows";
	}

	@Override
	public File getUserHome() {
		File result = super.getUserHome();

		String homeDrive = System.getenv("HOMEDRIVE");
		String homePath = System.getenv("HOMEPATH");
		if (homeDrive != null && homePath != null) {
			File homeDir = new File(homeDrive + homePath);
			if (homeDir.isDirectory() && homeDir.canWrite()) {
				result = homeDir;
			}
		} else {
			String userProfile = System.getenv("USERPROFILE");
			if (userProfile != null) {
				File userProfileDir = new File(userProfile);
				if (userProfileDir.isDirectory() && userProfileDir.canWrite()) {
					result = userProfileDir;
				}
			}
		}
		return result;
	}

	/**
	 * Returns an application data directory in the "Application Data" user directory of Windows.
	 *
	 * @return directory
	 */
	@Override
	public File getOSApplicationDataDir() {
		File result = new File(getUserHome(), APPLICATION_DATA);

		// check for the APPDATA environment variable
		String appData = System.getenv("APPDATA");
		if (appData != null) {
			File appDataDir = new File(appData);
			if (appDataDir.isDirectory() && appDataDir.canWrite()) {
				result = appDataDir;
			}
		}

		return new File(result, ADUNA_APPLICATION_DATA);
	}

	/**
	 * Returns the command shell for MS-Windows
	 *
	 * @return name of the command shell
	 */
	public String getCommandShell() {
		return "cmd";
	}

	@Override
	public boolean dataDirPreserveCase() {
		return true;
	}

	@Override
	public boolean dataDirReplaceWhitespace() {
		return false;
	}

	@Override
	public boolean dataDirReplaceColon() {
		return true;
	}
}
