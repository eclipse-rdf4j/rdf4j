/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.platform.support;

import java.io.File;

import org.eclipse.rdf4j.common.platform.AbstractPlatform;
import org.eclipse.rdf4j.common.platform.ProcessLauncher;

/**
 * Platform implementation for all Windows' platforms.
 */
public class WindowsPlatform extends AbstractPlatform {

	/** name of the directory containing application data */
	public static final String APPLICATION_DATA = "Application Data";

	/** name of the app data subdirectory containing all RDF4J files * */
	public static final String ADUNA_APPLICATION_DATA = "RDF4J";

	/**
	 * indication whether this is a windows9x platform: 0 means not initialized, -1 means false, 1 means true
	 */
	@Deprecated
	private int isWin9x = 0;

	/**
	 * Returns the name of this windows platform.
	 */
	@Override
	public String getName() {
		if (isWin9x()) {
			return "Windows 9x";
		} else if (isWinNT()) {
			return "Windows NT";
		} else if (isWin2000()) {
			return "Windows 2000";
		} else if (isWinXP()) {
			return "Windows XP";
		} else if (isWin2003()) {
			return "Windows 2003";
		} else if (isWinVista()) {
			return "Windows Vista";
		} else {
			return "Windows";
		}
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
	 * Warns when the platform is not a Windows 9x, NT or 2000 platform.
	 *
	 * @return true when not Win9x/NT/2000
	 */
	@Deprecated
	public boolean warnsWhenOpeningExecutable() {
		return !isWin9x() && !isWinNT() && !isWin2000();
	}

	/**
	 * Check whether this is windows 9x, or windows NT and higher.
	 *
	 * @return true when Win9x or higher
	 */
	@Deprecated
	public boolean isWin9x() {
		if (isWin9x == 0) {
			// let's see if this is windows 9x
			try {
				ProcessLauncher launcher = new ProcessLauncher(new String[] { "cmd", "/c", "echo" });
				launcher.launch();
				isWin9x = -1;
			} catch (ProcessLauncher.CommandNotExistsException nosuchcommand) {
				isWin9x = 1;
			} catch (Exception e) {
				logger.error("Unexpected exception while checking isWin9x", e);
			}
		}
		return isWin9x == 1;
	}

	/**
	 * Check whether this is an Windows NT environment.
	 *
	 * @return true when WinNT
	 */
	@Deprecated
	public boolean isWinNT() {
		return System.getProperty("os.name").toLowerCase().indexOf("nt") >= 0;
	}

	/**
	 * Check whether this is an Windows 2000 environment.
	 *
	 * @return true when Win2000
	 */
	@Deprecated
	public boolean isWin2000() {
		return System.getProperty("os.name").indexOf("2000") >= 0;
	}

	/**
	 * Check whether this is an Windows XP environment.
	 *
	 * @return true wen WinXP
	 */
	@Deprecated
	public boolean isWinXP() {
		return System.getProperty("os.name").toLowerCase().indexOf("xp") >= 0;
	}

	/**
	 * Check whether this is an Windows 2003 environment.
	 *
	 * @return true when Win2003
	 */
	@Deprecated
	public boolean isWin2003() {
		return System.getProperty("os.name").indexOf("2003") >= 0;
	}

	/**
	 * Check whether this is an Windows Vista environment.
	 *
	 * @return true when Vista
	 */
	@Deprecated
	public boolean isWinVista() {
		return System.getProperty("os.name").indexOf("Vista") >= 0;
	}

	/**
	 * Returns appropriate command shell for the current windows shell.
	 *
	 * @return name of the command shell
	 */
	public String getCommandShell() {
		if (isWin9x()) {
			return "command.com";
		} else {
			return "cmd";
		}
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
