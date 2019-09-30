/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.util;

import java.io.File;

import org.eclipse.rdf4j.federated.Config;

/**
 * Utility function for files
 * 
 * @author Andreas Schwarte
 *
 */
public class FileUtil {

	/**
	 * Returns the File representing the location.
	 * 
	 * <p>
	 * If the specified path is absolute, it is returned as is, otherwise a location relative to
	 * {@link Config#getBaseDir()} is returned
	 * </p>
	 * 
	 * @param path
	 * @return the file corresponding to the abstract path
	 */
	public static File getFileLocation(String path) {

		// check if path is an absolute path that already exists
		File f = new File(path);

		if (f.isAbsolute())
			return f;

		return fileInBaseDir(path);
	}

	/**
	 * Returns a File relative to the configured {@link Config#getBaseDir()}.
	 * 
	 * @param relPath
	 * @return the file
	 */
	public static File fileInBaseDir(String relPath) {
		String baseDir = Config.getConfig().getBaseDir();
		if (baseDir == null) {
			baseDir = ".";
		}
		return new File(Config.getConfig().getBaseDir(), relPath);
	}

	/**
	 * Uses {@link File#mkdirs()} to create the directory (if necessary)
	 * 
	 * @param dir
	 * @throws IllegalStateException if the directories cannot be created
	 */
	public static void mkdirs(File dir) {
		if (dir.isDirectory()) {
			return;
		}
		if (!dir.mkdirs()) {
			throw new IllegalStateException("Failed to create directories at " + dir);
		}
	}

}
